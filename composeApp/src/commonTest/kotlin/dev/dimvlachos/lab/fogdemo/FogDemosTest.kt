@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.fogdemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.fog.Evaporation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FogDemosTest {
    @Test
    fun theClipsBreathFogsTheWholeGlassSoTheLoopNeedsNoReset() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FogDemos.all.single().script.play(controller)
        val (duration, peak) = controller.breaths.single()

        val fog = newFogDemoState()
        val driver = BreathDriver(fog)
        val steps = (duration.inWholeMilliseconds / 16).toInt()
        for (i in 1..steps) driver.advance(
            scriptedBreathStrength(i / steps.toFloat(), peak),
            0.016f,
        )

        // Fog carried on past the top may linger over the fresh fog, unseen; the clear glass may
        // not.
        assertTrue(
            fog.marks.none { it is Evaporation },
            "fog left the glass partly clear: ${fog.marks}",
        )
    }

    @Test
    fun theDemoOpensOnClearGlass() {
        assertEquals(1f, (newFogDemoState().marks.single() as Evaporation).amount)
    }

    @Test
    fun onThePhoneTheDemoWaitsToBePlayedWith() {
        assertFalse(FogDemos.all.single().autoplay)
    }
}
