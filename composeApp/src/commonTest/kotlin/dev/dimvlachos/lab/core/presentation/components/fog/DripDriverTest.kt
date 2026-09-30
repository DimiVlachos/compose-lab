package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Condensation on a 400 × 800 dp mirror, stepped at 60 frames a second.
class DripDriverTest {
    private val step = 1 / 60f

    private fun driver(fog: FogState = FogState(), seed: Int = 1, randomStarts: Boolean = false) =
        DripDriver(fog, Random(seed)).apply {
            glass = DpSize(400.dp, 800.dp)
            this.randomStarts = randomStarts
        }

    private fun DripDriver.run(seconds: Float, each: (Float) -> Unit = {}) {
        var time = 0f
        while (time < seconds) {
            advance(step)
            time += step
            each(time)
        }
    }

    private fun FogState.streaks() = marks.filterIsInstance<WipeStroke>().filter { it.clarity < 1f }

    @Test
    fun dropsStartByThemselvesFourToEightSecondsApart() {
        val drips = driver(randomStarts = true)
        val starts = mutableListOf<Float>()
        var running = 0
        drips.run(60f) { time ->
            val now = drips.beads.count { !it.resting }
            if (now > running) starts += time
            running = now
        }
        assertTrue(starts.size >= 6, "starts: $starts")
        for ((a, b) in starts.zipWithNext()) {
            assertTrue(b - a in 3.95f..8.05f, "a gap of ${b - a} s in $starts")
        }
    }

    @Test
    fun aDropGrowsInPlaceBeforeItRuns() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.3f)
        drips.run(0.4f)

        val bead = drips.beads.single()
        assertEquals(Offset(0.5f, 0.2f), bead.at)
        assertTrue(fog.streaks().isEmpty())
        drips.run(0.3f)
        assertTrue(bead.radius < drips.beads.single().radius, "still swelling: ${bead.radius}")
    }

    @Test
    fun aDropOnlyRunsDownAndStaysOnItsLine() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.2f), 0.3f)
        var lastY = 0.2f
        drips.run(12f) {
            val at = drips.beads.single().at
            assertTrue(at.y >= lastY, "it never climbs: ${at.y} after $lastY")
            assertTrue(abs(at.x - 0.5f) * 400 < 3f, "it drifts under 3 dp: ${at.x}")
            lastY = at.y
        }
        val bead = drips.beads.single()
        assertTrue(bead.resting)
        assertTrue(abs(bead.at.y - 0.5f) < 0.001f, "it runs its whole length: ${bead.at.y}")
    }

    @Test
    fun aDropStopsAtTheBottomEdge() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.9f), 0.4f)
        drips.run(12f)
        assertTrue(drips.beads.single().at.y <= 1f)
    }

    @Test
    fun itsStreakIsAThinWetStrokeNarrowerAtTheTop() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.3f)
        drips.run(12f)

        val streaks = fog.streaks()
        assertTrue(streaks.size >= 2, "one stroke per burst: ${streaks.size}")
        assertTrue(streaks.all { it.clarity == 0.9f })
        val bead = drips.beads.single().radius
        assertTrue(streaks.first().radius!! < streaks[1].radius!!, "narrower at the top")
        assertTrue(streaks.all { it.radius!! < bead }, "a streak is thinner than its drop")
    }

    @Test
    fun noDropStartsOnClearGlass() {
        val drips = driver(FogState(startClear = true), randomStarts = true)
        drips.run(30f)
        assertTrue(drips.beads.isEmpty())
    }

    @Test
    fun noDropStartsOnAWipedPatch() {
        val fog = FogState()
        // One enormous wipe over the whole mirror.
        fog.beginStroke(Offset(0.5f, 0.5f), radius = 2000.dp)
        val drips = driver(fog, randomStarts = true)
        drips.run(30f)
        assertTrue(drips.beads.isEmpty())
    }

    @Test
    fun atMostTwoDropsRunAtOnce() {
        // A tall mirror, so two long drops are still running when the next start comes due.
        val drips = driver(randomStarts = true).apply { glass = DpSize(400.dp, 8000.dp) }
        drips.drip(Offset(0.3f, 0.1f), 0.4f)
        drips.drip(Offset(0.7f, 0.1f), 0.4f)
        drips.run(9f) {
            assertTrue(drips.beads.count { !it.resting } <= 2, "${drips.beads}")
        }
    }

    @Test
    fun aFullBreathFogsOverARestingDrop() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        drips.run(10f)
        assertTrue(drips.beads.single().resting)

        fog.setBreathLevel(fog.beginBreath(), 1f)

        // Fogged over already as the breath rose: nothing left to see, and soon nothing at all.
        assertTrue(drips.beads.none { it.alpha > 0.01f }, "${drips.beads}")
        drips.run(1f)
        assertTrue(drips.beads.isEmpty())
    }

    @Test
    fun aWipeOverARestingDropClearsIt() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        drips.run(10f)
        val at = drips.beads.single().at

        fog.beginStroke(at).also { fog.extendStroke(it, at + Offset(0.01f, 0f)) }

        // Smeared away quickly, not switched off.
        drips.run(0.08f)
        val fading = drips.beads.single().alpha
        assertTrue(fading in 0.1f..0.9f, "fading: $fading")
        drips.run(0.4f)
        assertTrue(drips.beads.isEmpty())
    }

    @Test
    fun aDropRunningThroughAFullBreathCarriesOnWithAFreshStreak() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.1f), 0.4f)
        drips.run(1.2f)
        val before = drips.beads.single().at.y

        fog.setBreathLevel(fog.beginBreath(), 1f)
        drips.run(10f)

        assertTrue(drips.beads.single().at.y > before, "still running")
        assertTrue(fog.streaks().isNotEmpty(), "a fresh streak through the new fog")
    }

    @Test
    fun aLongGapBetweenFramesDoesNotTeleportADrop() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.1f), 0.4f)
        drips.run(0.7f) // grown, just starting to run
        val before = drips.beads.single().at.y

        drips.advance(5f)

        // 0.1 s at most, at no more than 190 dp/s, on an 800 dp mirror.
        assertTrue(drips.beads.single().at.y - before <= 19f / 800f + 1e-4f)
    }

    @Test
    fun restingDropsAreCappedAtTwelve() {
        val drips = driver()
        repeat(20) { drips.drip(Offset(0.05f + it * 0.045f, 0.1f), 0.15f) }
        drips.run(15f)
        assertEquals(12, drips.beads.count { it.resting })
    }

    @Test
    fun resizingTheGlassMidRunKeepsTheDropOnIt() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.6f), 0.4f)
        drips.run(1.5f)
        drips.glass = DpSize(800.dp, 400.dp)
        drips.run(12f) {
            val at = drips.beads.single().at
            assertTrue(at.x in 0f..1f && at.y in 0f..1f, "$at")
        }
    }

    @Test
    fun nothingDripsBeforeTheGlassHasASize() {
        val drips = DripDriver(FogState(), Random(1))
        drips.run(20f)
        assertTrue(drips.beads.isEmpty())
        assertFalse(drips.moving)
    }

    @Test
    fun itKnowsWhenTheNextDropIsDue() {
        val drips = driver(randomStarts = true)
        assertTrue(drips.secondsToNextStart!! in 4f..8f)
        drips.randomStarts = false
        assertEquals(null, drips.secondsToNextStart)
    }

    @Test
    fun aDropRunsOnThroughAWipedPatch() {
        val fog = FogState()
        // A wiped band across the mirror, below where the drop starts.
        fog.beginStroke(Offset(0f, 0.3f), radius = 20.dp).also {
            fog.extendStroke(it, Offset(1f, 0.3f))
        }
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.3f)
        drips.run(12f)

        val bead = drips.beads.single()
        assertTrue(bead.at.y > 0.45f, "it ran on past the wiped band: ${bead.at}")
    }

    @Test
    fun aDropStillGrowingWhenWipedIsGone() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.1f)
        drips.run(0.3f)
        fog.beginStroke(Offset(0.3f, 0.2f)).also { fog.extendStroke(it, Offset(0.7f, 0.2f)) }

        drips.run(0.5f)
        assertTrue(drips.beads.isEmpty(), "smeared away: ${drips.beads}")
        drips.run(3f)
        assertTrue(drips.beads.isEmpty(), "and stays gone: ${drips.beads}")
        assertFalse(drips.moving)
    }

    @Test
    fun aDropNeverStartsAtTheSoftEdgeOfAWipe() {
        val fog = FogState()
        val wipe = FogDimens.BrushRadius.value
        fog.beginStroke(Offset(0.5f, 0.5f))
        val glass = DpSize(400.dp, 800.dp)
        // Just outside the brush's reach the glass still looks wiped; a little further it is fog.
        val nearEdge = Offset(0.5f + (wipe + 5f) / 400f, 0.5f)
        val wellClear = Offset(0.5f + (wipe + 20f) / 400f, 0.5f)
        assertFalse(fog.isFoggedAt(nearEdge, glass, FogDimens.BrushRadius))
        assertTrue(fog.isFoggedAt(wellClear, glass, FogDimens.BrushRadius))
    }

    @Test
    fun onlyTheLastTwelveDropsLeaveTheirStreaksOnTheGlass() {
        val fog = FogState()
        val drips = driver(fog)
        // One after another, so the order they finish in is the order they started.
        repeat(20) {
            drips.drip(Offset(0.05f + it * 0.045f, 0.1f), 0.15f)
            drips.run(6f)
        }
        // Each drop's streaks start within a few dp of its own spot, 0.045 apart from the next.
        val xs = fog.streaks().map { ((it.points.first().x - 0.05f) / 0.045f).roundToInt() }.toSet()
        assertTrue(xs.size <= 12, "streaks from ${xs.size} drops")
        assertTrue(
            fog.streaks().none { it.points.first().x < 0.05f + 8 * 0.045f - 0.01f },
            "the oldest drops' streaks are gone",
        )
    }

    @Test
    fun theDriverForgetsStreaksAFullBreathHasFoggedOver() {
        val fog = FogState()
        val drips = driver(fog)
        repeat(3) { drips.drip(Offset(0.2f + it * 0.3f, 0.1f), 0.2f) }
        drips.run(10f)
        fog.setBreathLevel(fog.beginBreath(), 1f)
        drips.run(0.1f)
        assertEquals(0, drips.trackedStreaks)
    }

    @Test
    fun aDropKeepsItsSizeWhenItStops() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        drips.run(1f)
        val running = drips.beads.single()
        assertFalse(running.resting)
        drips.run(10f)
        val resting = drips.beads.single()
        assertTrue(resting.resting)
        assertEquals(running.radius, resting.radius)
    }

    @Test
    fun dropsComeInManySizesMostlySmall() {
        val drips = driver()
        repeat(40) { drips.drip(Offset(0.05f + it * 0.022f, 0.1f), 0.05f) }
        drips.run(1f)
        val radii = drips.beads.map { it.radius.value }
        assertTrue(radii.min() < 4f, "some small: ${radii.min()}")
        assertTrue(radii.max() > 6.5f, "some big: ${radii.max()}")
        assertTrue(radii.count { it < 5f } > radii.size / 2, "mostly small: $radii")
    }

    @Test
    fun aRunningDropStretchesIntoATeardropAndSagsWhenItStops() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.1f), 0.3f)
        drips.run(0.3f)
        assertEquals(0f, drips.beads.single().stretch, "round while it gathers")

        var stretched = 0f
        drips.run(12f) {
            val bead = drips.beads.single()
            if (!bead.resting) stretched = maxOf(stretched, bead.stretch)
        }
        assertTrue(stretched > 0.5f, "pulled long as it runs: $stretched")
        val resting = drips.beads.single()
        assertTrue(resting.resting)
        assertTrue(resting.stretch in 0.05f..0.35f, "a slight sag at rest: ${resting.stretch}")
    }

    @Test
    fun aLongScrubThatStartedBeforeADropStoppedStillClearsIt() {
        val fog = FogState()
        val drips = driver(fog)
        // The finger goes down far away and keeps scrubbing while the drop runs and stops.
        val scrub = fog.beginStroke(Offset(0.1f, 0.9f))
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        drips.run(10f)
        val at = drips.beads.single().at
        assertTrue(drips.beads.single().resting)

        fog.extendStroke(scrub, at + Offset(-0.05f, 0f))
        fog.extendStroke(scrub, at + Offset(0.05f, 0f))

        drips.run(0.5f)
        assertTrue(drips.beads.isEmpty(), "wiped away: ${drips.beads}")
    }

    @Test
    fun aDropThatJustStoppedStillNeedsFramesUntilItHasRelaxed() {
        val drips = driver()
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        while (!drips.beads.single().resting) drips.advance(step)
        assertFalse(drips.moving)
        assertTrue(drips.needsFrames, "still pulled long: ${drips.beads.single().stretch}")

        drips.run(3f)
        assertFalse(drips.needsFrames)
        assertTrue(abs(drips.beads.single().stretch - 0.2f) < 0.02f)
    }

    @Test
    fun aRisingBreathFogsARestingDropOverAsItsFrontPasses() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        drips.run(10f)
        val y = drips.beads.single().at.y
        val breath = fog.beginBreath()

        var last = 1f
        var level = 0f
        while (level < 1f) {
            level += 0.01f
            fog.setBreathLevel(breath, level.coerceAtMost(0.999f))
            val alpha = drips.beads.singleOrNull()?.alpha ?: 0f
            assertTrue(alpha <= last + 1e-4f, "never brighter as the fog rises: $alpha after $last")
            if (level < 1f - y - 0.13f) assertEquals(1f, alpha, "untouched below the front")
            if (level > 1f - y + 0.01f) assertEquals(0f, alpha, "covered once the front is past")
            last = alpha
        }
    }

    @Test
    fun aDropPushedOutByTheLimitFadesRatherThanVanishing() {
        val drips = driver()
        repeat(12) { drips.drip(Offset(0.05f + it * 0.07f, 0.1f), 0.1f) }
        drips.run(10f)
        drips.drip(Offset(0.5f, 0.5f), 0.1f)
        while (
            drips.beads.count { it.resting && it.alpha == 1f } < 13 &&
                drips.beads.none { it.alpha < 1f }
        ) {
            drips.advance(step)
        }
        val oldest = drips.beads.minBy { it.alpha }
        assertTrue(oldest.alpha < 1f && oldest.alpha > 0.5f, "starting to fade: ${oldest.alpha}")
        drips.run(1.5f)
        assertEquals(12, drips.beads.size)
    }

    @Test
    fun aFadingDropKeepsTheGlassAskingForFrames() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        drips.run(10f)
        assertFalse(drips.needsFrames)
        val at = drips.beads.single().at

        fog.beginStroke(at).also { fog.extendStroke(it, at + Offset(0.01f, 0f)) }

        assertTrue(drips.wantsFrames, "a wipe over a resting drop wakes the glass")
        drips.advance(step)
        assertTrue(drips.needsFrames, "while it fades")
    }
}
