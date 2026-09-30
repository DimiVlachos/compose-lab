@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.fog.DripDriver
import dev.dimvlachos.lab.core.presentation.components.fog.Evaporation
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
import dev.dimvlachos.lab.fogdemo.presentation.components.wallGlassOn
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FogDemosTest {
    @Test
    fun theClipsBreathFogsOverTheDrawingSoTheLoopNeedsNoReset() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.reflection.script.play(controller)
        val (duration, peak) = controller.breaths.single()

        val fog = newFogDemoState()
        for (path in controller.wipes) {
            val stroke = fog.beginStroke(path.first())
            path.drop(1).forEach { fog.extendStroke(stroke, it) }
        }
        val dripper =
            DripDriver(fog, Random(1)).apply {
                glass = ClipGlass
                randomStarts = false
            }
        for ((at, length) in controller.drips) dripper.drip(at, length)
        repeat(600) { dripper.advance(1 / 60f) }
        val driver = BreathDriver(fog)
        val steps = (duration.inWholeMilliseconds / 16).toInt()
        for (i in 1..steps) driver.advance(
            scriptedBreathStrength(i / steps.toFloat(), peak),
            0.016f,
        )

        assertTrue(
            fog.marks.none { it is WipeStroke || it is Evaporation },
            "the drawing is still on the glass: ${fog.marks}",
        )
        assertTrue(
            dripper.beads.none { it.alpha > 0.01f },
            "a drop still showing on the fresh fog: ${dripper.beads}",
        )
    }

    @Test
    fun theBreathComesAfterTheDrawing() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.reflection.script.play(controller)

        val calls = controller.calls.map { it.second }
        assertTrue(calls.last().startsWith("breathe"), "calls: $calls")
        assertEquals(1, calls.count { it.startsWith("wipe") })
        assertTrue(calls.none { it.startsWith("select") }, "calls: $calls")
    }

    @Test
    fun theDemoOpensFullyFogged() {
        assertTrue(newFogDemoState().marks.isEmpty())
    }

    @Test
    fun onThePhoneTheDemoWaitsToBePlayedWith() {
        assertFalse(FogDemos.bathroom.autoplay)
    }

    @Test
    fun theClipDripsTwiceBetweenTheDrawingAndTheBreath() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.reflection.script.play(controller)

        val calls = controller.calls.map { it.second }
        val lastWipe = calls.indexOfLast { it.startsWith("wipe") }
        val breath = calls.indexOfFirst { it.startsWith("breathe") }
        val drips = calls.indices.filter { calls[it].startsWith("drip") }
        assertEquals(2, drips.size, "$calls")
        assertTrue(drips.all { it in lastWipe..breath }, "$calls")
    }

    @Test
    fun theClipsDripsRunBesideThePorthole() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.reflection.script.play(controller)
        // In dp on the clip's glass, measured to the scrub's segments: a drop that
        // came within the finger's reach of them would stop and blend in.
        fun dp(point: Offset) =
            Offset(point.x * ClipGlass.width.value, point.y * ClipGlass.height.value)
        val scrub = FogDemos.porthole.path().map(::dp)
        val clearance = FogDemos.FingerBrush.value + MaxDropRadius

        for ((at, length) in controller.drips) {
            var y = at.y
            while (y <= at.y + length) {
                val point = dp(Offset(at.x, y))
                val nearest = scrub.zipWithNext().minOf { (a, b) -> distanceToSegment(point, a, b) }
                assertTrue(nearest > clearance, "a drip at $point comes $nearest dp from the scrub")
                y += 0.005f
            }
        }
    }

    @Test
    fun theClipsDripsHaveStoppedBeforeTheBreath() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.reflection.script.play(controller)
        val times = controller.calls.filter { it.second.startsWith("drip") }.map { it.first }
        val breathAt = controller.calls.first { it.second.startsWith("breathe") }.first

        // Whatever the drops' own randomness, over many seeds, on the clip's glass.
        for (seed in 1..30) {
            val drips =
                DripDriver(newFogDemoState(), Random(seed)).apply {
                    glass = ClipGlass
                    randomStarts = false
                }
            var now = times.first()
            val pending = controller.drips.zip(times).toMutableList()
            while (now < breathAt) {
                pending.removeAll { (drip, at) ->
                    (at <= now).also { if (it) drips.drip(drip.first, drip.second) }
                }
                drips.advance(0.016f)
                now += 16
            }
            assertTrue(!drips.moving, "seed $seed: a drop still running at the breath")
        }
    }

    @Test
    fun eachVersionHasItsOwnClip() {
        val (bathroom, camera) = FogDemos.all
        assertEquals("fog.mirror.bathroom", bathroom.id)
        assertEquals("fog.mirror.camera", camera.id)
        assertTrue(bathroom.script !== camera.script)
        assertFalse(camera.autoplay)
    }

    @Test
    fun theBathroomClipShowsDropsRunningBeforeTheWipe() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.bathroom.script.play(controller)
        val wipeAt = controller.calls.first { it.second.startsWith("wipe") }.first
        val before = controller.calls.filter { it.second.startsWith("drip") && it.first < wipeAt }
        assertTrue(before.size >= 2, "drops before the wipe: ${controller.calls}")

        // Every one of them has run and stopped before the finger comes, whatever their sizes.
        for (seed in 1..30) {
            val drips = clipDrips(seed)
            playDrips(drips, controller, until = wipeAt)
            assertTrue(drips.beads.size >= 2, "seed $seed: ${drips.beads}")
            assertTrue(!drips.moving, "seed $seed: a drop still running as the wipe starts")
        }
    }

    @Test
    fun theBathroomClipsFirstDropsStayClearOfTheWipe() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.bathroom.script.play(controller)
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
        for ((at, length) in before) {
            var y = at.y
            while (y <= at.y + length) {
                val point = dp(Offset(at.x, y))
                val nearest = scrub.zipWithNext().minOf { (a, b) -> distanceToSegment(point, a, b) }
                assertTrue(nearest > clearance, "a drip at $point comes $nearest dp from the scrub")
                y += 0.005f
            }
        }
    }

    @Test
    fun theBathroomClipRunsADropIntoTheWipeWhereItSpreadsAway() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.bathroom.script.play(controller)
        val wipeAt = controller.calls.first { it.second.startsWith("wipe") }.first
        val mistAt = controller.calls.first { it.second.startsWith("mist") }.first
        val calls = controller.calls.filter { it.second.startsWith("drip") }
        val intoWipe = controller.drips.zip(calls).filter { (_, call) -> call.first > wipeAt }
        assertEquals(1, intoWipe.size, "one drop after the wipe: ${controller.calls}")
        val (drip, call) = intoWipe.single()

        for (seed in 1..30) {
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
            assertTrue(drips.beads.isEmpty(), "seed $seed: gone before the mist: ${drips.beads}")
        }
    }

    @Test
    fun theBathroomClipMistsBackOverByItselfSoTheLoopNeedsNoReset() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.bathroom.script.play(controller)
        val calls = controller.calls.map { it.second }
        assertTrue(calls.last().startsWith("mist"), "calls: $calls")
        assertTrue(calls.none { it.startsWith("breathe") }, "no breath in the bathroom: $calls")
        assertEquals(1, calls.count { it.startsWith("wipe") })

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

// The biggest drop's radius, in dp.
private const val MaxDropRadius = 5.5f

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val length = ab.getDistanceSquared()
    if (length == 0f) return (p - a).getDistance()
    val t = (((p - a).x * ab.x + (p - a).y * ab.y) / length).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}
