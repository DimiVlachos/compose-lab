@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.frostdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FrostDemosTest {
    @Test
    fun theClipsBreathFrostsTheWholeGlassSoTheLoopNeedsNoReset() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        FrostDemos.all.single().script.play(controller)
        val (duration, peak) = controller.breaths.single()

        val frost = FrostState()
        frost.beginStroke(Offset(0.5f, 0.5f))
        val driver = BreathDriver(frost)
        val steps = (duration.inWholeMilliseconds / 16).toInt()
        for (i in 1..steps) driver.advance(
            scriptedBreathStrength(i / steps.toFloat(), peak),
            0.016f,
        )

        // Fog carried on past the top may linger over the fresh frost, unseen; no wipe may.
        assertTrue(frost.strokes.isEmpty(), "fog left the glass partly clear: ${frost.marks}")
    }
}
