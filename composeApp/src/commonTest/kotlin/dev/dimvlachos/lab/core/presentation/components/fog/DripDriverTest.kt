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
        assertTrue(bead.radius < 3.5.dp, "still swelling: ${bead.radius}")
        assertTrue(fog.streaks().isEmpty())
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
        assertEquals(2.dp, bead.radius)
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
        assertTrue(streaks.all { it.clarity == 0.85f })
        assertEquals(3.dp, streaks.first().radius)
        assertTrue(streaks.drop(1).all { it.radius == 4.dp })
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

        assertTrue(drips.beads.isEmpty(), "gone at once, before the next step")
    }

    @Test
    fun aWipeOverARestingDropClearsIt() {
        val fog = FogState()
        val drips = driver(fog)
        drips.drip(Offset(0.5f, 0.2f), 0.2f)
        drips.run(10f)
        val at = drips.beads.single().at

        fog.beginStroke(at).also { fog.extendStroke(it, at + Offset(0.01f, 0f)) }

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

        // 0.1 s at most, at no more than 150 dp/s, on an 800 dp mirror.
        assertTrue(drips.beads.single().at.y - before <= 15f / 800f + 1e-4f)
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

        assertTrue(drips.beads.isEmpty(), "gone at once: ${drips.beads}")
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
}
