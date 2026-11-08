@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.sandmandalademo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class SandMandalaDemosTest {
    private val demo = SandMandalaDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("sandmandala.history", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipClearsSweepsAndUndoesThenEndsAsItBegan() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf("tapControl(clear)", "tapControl(sweep)", "tapControl(undo)", "resetFeed()"),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theSandIsBackInItsRowsBeforeTheReset() {
        // On a phone the gather takes about 3.3 s, the sweep's spiral 2.2, the heap's hold 0.45,
        // the pour 2.6 and the mandala's hold on the way back 0.75. The sweep lets go about 2.3 s
        // into its strokes, and Undo is tapped 1.4 s after the history is cleared.
        val formed =
            .9 +
                (SandMandalaDemos.ShowMs + SandMandalaDemos.ToButtonMs + SandMandalaDemos.PressMs) /
                    1000.0 +
                3.3
        val letGo =
            formed +
                (SandMandalaDemos.SeeMandalaMs +
                    SandMandalaDemos.ShowMs +
                    SandMandalaDemos.RestMs +
                    SandMandalaDemos.GripMs +
                    3 * SandMandalaDemos.LegMs) / 1000.0
        val cleared = letGo + 2.2 + .45 + 2.6
        val undone =
            cleared +
                (SandMandalaDemos.SeeClearedMs +
                    SandMandalaDemos.ShowMs +
                    SandMandalaDemos.ToButtonMs +
                    SandMandalaDemos.PressMs) / 1000.0
        val rows = undone + 2.6 + 2.2 + .75 + 3.3
        assertTrue(rows < .9 + 27, "$rows")
    }

    @Test
    fun theClipFitsInThirtySeconds() {
        assertTrue(demo.script.nominalDuration <= 30.seconds, "${demo.script.nominalDuration}")
    }
}
