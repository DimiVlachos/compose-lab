@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.frostdemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.frost.Thaw
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FrostDemosTest {
    @Test
    fun theClipsBreathFrostsTheWholeGlassSoTheLoopNeedsNoReset() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FrostDemos.all.single().script.play(controller)
        val (duration, peak) = controller.breaths.single()

        val frost = newFrostDemoState()
        val driver = BreathDriver(frost)
        val steps = (duration.inWholeMilliseconds / 16).toInt()
        for (i in 1..steps) driver.advance(
            scriptedBreathStrength(i / steps.toFloat(), peak),
            0.016f,
        )

        // Fog carried on past the top may linger over the fresh frost, unseen; the clear glass may
        // not.
        assertTrue(
            frost.marks.none { it is Thaw },
            "fog left the glass partly clear: ${frost.marks}",
        )
    }

    @Test
    fun theDemoOpensOnClearGlass() {
        assertEquals(1f, (newFrostDemoState().marks.single() as Thaw).amount)
    }

    @Test
    fun onThePhoneTheDemoWaitsToBePlayedWith() {
        assertFalse(FrostDemos.all.single().autoplay)
    }
}
