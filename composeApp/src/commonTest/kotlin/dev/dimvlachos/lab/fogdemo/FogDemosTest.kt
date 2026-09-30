@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.fog.DripDriver
import dev.dimvlachos.lab.core.presentation.components.fog.Evaporation
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
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
        FogDemos.bathroom.script.play(controller)
        val (duration, peak) = controller.breaths.single()

        val fog = newFogDemoState()
        for (path in controller.wipes) {
            val stroke = fog.beginStroke(path.first())
            path.drop(1).forEach { fog.extendStroke(stroke, it) }
        }
        val dripper =
            DripDriver(fog, Random(1)).apply {
                glass = DpSize(400.dp, 500.dp)
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
        FogDemos.bathroom.script.play(controller)

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
        FogDemos.bathroom.script.play(controller)

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
        FogDemos.bathroom.script.play(controller)
        val drawing = FogDemos.porthole.path()

        for ((at, length) in controller.drips) {
            var y = at.y
            while (y <= at.y + length) {
                val point = Offset(at.x, y)
                val nearest = drawing.minOf { (it - point).getDistance() }
                // Clear of the scrub by more than the finger's width.
                assertTrue(nearest > 0.12f, "a drip at $point comes $nearest from the porthole")
                y += 0.01f
            }
        }
    }

    @Test
    fun theClipsDripsHaveStoppedBeforeTheBreath() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.bathroom.script.play(controller)
        val times = controller.calls.filter { it.second.startsWith("drip") }.map { it.first }
        val breathAt = controller.calls.first { it.second.startsWith("breathe") }.first

        // Whatever the drops' own randomness, over many seeds, on the clip's 400 × 500 frame.
        for (seed in 1..30) {
            val drips =
                DripDriver(newFogDemoState(), Random(seed)).apply {
                    glass = DpSize(400.dp, 500.dp)
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
    fun bothVersionsPlayTheSameClip() {
        val (bathroom, camera) = FogDemos.all
        assertEquals("fog.mirror.bathroom", bathroom.id)
        assertEquals("fog.mirror.camera", camera.id)
        assertTrue(bathroom.script === camera.script)
        assertFalse(camera.autoplay)
    }
}
