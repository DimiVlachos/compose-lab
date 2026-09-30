@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.fogdemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.fog.Evaporation
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
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
        FogDemos.all.single().script.play(controller)
        val (duration, peak) = controller.breaths.single()

        val fog = newFogDemoState()
        for (path in controller.wipes) {
            val stroke = fog.beginStroke(path.first())
            path.drop(1).forEach { fog.extendStroke(stroke, it) }
        }
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
    }

    @Test
    fun theBreathComesAfterTheDrawing() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.all.single().script.play(controller)

        val calls = controller.calls.map { it.second }
        assertTrue(calls.last().startsWith("breathe"), "calls: $calls")
        assertEquals(heartWithArrow().size, calls.count { it.startsWith("wipe") })
        assertTrue(calls.none { it.startsWith("select") }, "calls: $calls")
    }

    @Test
    fun theDemoOpensFullyFogged() {
        assertTrue(newFogDemoState().marks.isEmpty())
    }

    @Test
    fun onThePhoneTheDemoWaitsToBePlayedWith() {
        assertFalse(FogDemos.all.single().autoplay)
    }
}
