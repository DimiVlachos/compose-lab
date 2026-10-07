@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.fog.DripDriver
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
import dev.dimvlachos.lab.fogdemo.presentation.components.wallGlassOn
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FogDemosTest {
    @Test
    fun theDemoOpensFullyFogged() {
        assertTrue(newFogDemoState().marks.isEmpty())
    }

    @Test
    fun onThePhoneTheDemoWaitsToBePlayedWith() {
        assertFalse(FogDemos.bathroom.autoplay)
    }

    @Test
    fun theClipsLengthCountsItsMistToTheEnd() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        val script = FogDemos.bathroom.script
        script.play(controller)
        val mistAt = controller.calls.first { it.second.startsWith("mist") }.first.milliseconds
        assertEquals(mistAt + controller.mists.single() + script.holdEnd, script.nominalDuration)
    }

    // Plays the clip both versions share, then runs [check] on what it did.
    private fun playTheClip(check: (FakeController) -> Unit) =
        runTest(timeout = SlowFogTest) {
            val controller = FakeController { testScheduler.currentTime }
            FogDemos.bathroom.script.play(controller)
            check(controller)
        }

    @Test
    fun bothVersionsPlayTheSameClip() {
        val (bathroom, camera) = FogDemos.all
        assertEquals("fog.mirror.bathroom", bathroom.id)
        assertEquals("fog.mirror.camera", camera.id)
        assertSame(bathroom.script, camera.script)
        assertFalse(camera.autoplay)
    }

    @Test
    fun theClipShowsDropsRunningBeforeTheWipe() = playTheClip { controller ->
        val wipeAt = controller.calls.first { it.second.startsWith("wipe") }.first
        val before = controller.calls.filter { it.second.startsWith("drip") && it.first < wipeAt }
        assertTrue(before.size >= 2, "drops before the wipe: ${controller.calls}")

        // Every one of them has run and stopped before the finger comes, whatever their sizes.
        for (seed in 1..ClipSeeds) {
            val drips = clipDrips(seed)
            playDrips(drips, controller, until = wipeAt)
            assertTrue(drips.beads.size >= 2, "seed $seed: ${drips.beads}")
            assertTrue(!drips.moving, "seed $seed: a drop still running as the wipe starts")
        }
    }

    @Test
    fun theClipsFirstDropsStayClearOfTheWipeButOne() = playTheClip { controller ->
        val wipeAt = controller.calls.first { it.second.startsWith("wipe") }.first
        val before =
            controller.drips
                .zip(controller.calls.filter { it.second.startsWith("drip") })
                .filter { (_, call) -> call.first < wipeAt }
                .map { it.first }
        fun dp(point: Offset) =
            Offset(point.x * ClipGlass.width.value, point.y * ClipGlass.height.value)
        val scrub = FogDemos.porthole.path().map(::dp)
        val clearance = FogDemos.FingerBrush.value + MaxDropRadius
        // How near each drop's run comes to the hand's path.
        val nearest = before.map { (at, length) ->
            generateSequence(at.y) { it + 0.005f }
                .takeWhile { it <= at.y + length }
                .minOf { y ->
                    val point = dp(Offset(at.x, y))
                    scrub.zipWithNext().minOf { (a, b) -> distanceToSegment(point, a, b) }
                }
        }
        // All but one stay to watch; that one sits where the hand will pass, deep in its path.
        assertEquals(1, nearest.count { it <= clearance }, "in the hand's path: $nearest")
        assertTrue(nearest.min() < FogDemos.FingerBrush.value / 2, "deep in it: $nearest")
    }

    @Test
    fun theClipsHandSmearsAwayTheDropInItsPath() = playTheClip { controller ->
        val wipeAt = controller.calls.first { it.second.startsWith("wipe") }.first
        for (seed in 1..ClipSeeds) {
            val fog = newFogDemoState()
            val drips = clipDrips(seed, fog)
            playDrips(drips, controller, until = wipeAt)
            val resting = drips.beads.count { it.alpha > 0.9f }
            wipeOnto(fog, controller)
            repeat(30) { drips.advance(1 / 60f) }
            assertEquals(resting - 1, drips.beads.size, "seed $seed: one smeared away")
            // The one in the hand's path, the only one on its line.
            assertTrue(
                drips.beads.none { kotlin.math.abs(it.at.x - InPath) < 0.05f },
                "seed $seed: ${drips.beads}",
            )
        }
    }

    @Test
    fun theClipRunsADropIntoTheWipeWhereItSpreadsAwayBeforeTheEnd() = playTheClip { controller ->
        val wipeAt = controller.calls.first { it.second.startsWith("wipe") }.first
        val mistAt = controller.calls.first { it.second.startsWith("mist") }.first
        val calls = controller.calls.filter { it.second.startsWith("drip") }
        val intoWipe = controller.drips.zip(calls).filter { (_, call) -> call.first > wipeAt }
        assertEquals(1, intoWipe.size, "one drop after the wipe: ${controller.calls}")
        val (drip, call) = intoWipe.single()

        for (seed in 1..ClipSeeds) {
            val fog = newFogDemoState()
            wipeOnto(fog, controller)
            val drips = clipDrips(seed, fog)
            drips.drip(drip.first, drip.second)
            var spread = 0f
            var now = call.first
            while (now < mistAt) {
                drips.advance(0.016f)
                spread = maxOf(spread, drips.beads.maxOfOrNull { it.spread } ?: 0f)
                now += 16
            }
            assertTrue(spread > 0.9f, "seed $seed: it spread right out, $spread")
            assertTrue(drips.beads.isEmpty(), "seed $seed: gone before the end: ${drips.beads}")
        }
    }

    @Test
    fun theClipMistsBackOverByItselfSoTheLoopNeedsNoReset() = playTheClip { controller ->
        val calls = controller.calls.map { it.second }
        assertTrue(calls.last().startsWith("mist"), "calls: $calls")
        assertEquals(1, calls.count { it.startsWith("wipe") })
        assertTrue(calls.none { it.startsWith("select") }, "calls: $calls")

        val fog = newFogDemoState()
        wipeOnto(fog, controller)
        val drips = clipDrips(1, fog)
        for ((at, length) in controller.drips) drips.drip(at, length)
        repeat(900) { drips.advance(1 / 60f) }
        val mist = fog.beginMist()
        fog.setMistAmount(mist, 1f)
        repeat(60) { drips.advance(1 / 60f) }

        assertTrue(
            fog.marks.none { it is WipeStroke },
            "the wipe is still on the glass: ${fog.marks}",
        )
        assertTrue(drips.beads.none { it.alpha > 0.01f }, "a drop still showing: ${drips.beads}")
    }
}

private fun clipDrips(seed: Int, fog: FogState = newFogDemoState()) =
    DripDriver(fog, Random(seed), wipeRadius = FogDemos.FingerBrush).apply {
        glass = ClipGlass
        randomStarts = false
    }

// Starts the clip's drops at their times, running them until [until], in ms.
private fun playDrips(drips: DripDriver, controller: FakeController, until: Long) {
    val times = controller.calls.filter { it.second.startsWith("drip") }.map { it.first }
    val pending = controller.drips.zip(times).toMutableList()
    var now = times.first()
    while (now < until) {
        pending.removeAll { (drip, at) ->
            (at <= now).also { if (it) drips.drip(drip.first, drip.second) }
        }
        drips.advance(0.016f)
        now += 16
    }
}

private fun wipeOnto(fog: FogState, controller: FakeController) {
    for (path in controller.wipes) {
        val stroke = fog.beginStroke(path.first(), radius = FogDemos.FingerBrush)
        path.drop(1).forEach { fog.extendStroke(stroke, it) }
    }
}

// The mirror's glass in the clip's 400 × 500 dp frame: the stage the drops run on.
private val ClipGlass = wallGlassOn(Size(400f, 500f)).let { DpSize(it.width.dp, it.height.dp) }

// Whatever the drops' own randomness: as many runs as a phone might give.
private const val ClipSeeds = 300

// Where the clip's drop in the hand's path runs down, across the glass.
private const val InPath = 0.62f

// The biggest drop's radius, in dp.
private const val MaxDropRadius = 5.5f

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val length = ab.getDistanceSquared()
    if (length == 0f) return (p - a).getDistance()
    val t = (((p - a).x * ab.x + (p - a).y * ab.y) / length).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}
