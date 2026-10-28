@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.graveundodemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class GraveUndoDemosTest {
    private val demo = GraveUndoDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("graveundo.undo", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipShowsEveryWayInAndOutAndEndsAsItBegan() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(bury)",
                "tapControl(undo-at-grave)",
                "tapControl(bury)",
                "tapControl(bury)",
                "tapControl(undo)",
                "tapControl(bury-second)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theLastTaskIsLeftLongEnoughForItsHandToSinkAndItsGraveToGo() {
        // The soft timings: a task flies 1.95 s, rests 0.55 and the hand rises 1.69; an undo flies
        // home 1.82 and lands 0.26; a hand left alone holds 6.5 s, and the grave slides away 1.02
        // after the hand is 62% of its 1.95 s down. Each swipe lets go 2.2 s after it starts, once
        // nothing is flying.
        val letGo =
            (GraveUndoDemos.ToRowMs +
                GraveUndoDemos.GripMs +
                GraveUndoDemos.DragMs +
                GraveUndoDemos.HoldMs) / 1000.0
        val pressDelay = (GraveUndoDemos.ToHandMs + GraveUndoDemos.PressMs) / 1000.0
        val pureeHeld = 12.8 + letGo + 1.95 + .55 + 1.69
        val pureeHome = pureeHeld + GraveUndoDemos.ShowHandMs / 1000.0 + pressDelay + 1.82 + .26
        val graveGone = pureeHome + letGo + 1.95 + .55 + 1.69 + 6.5 + 1.95 * .62 + 1.015
        assertTrue(graveGone < 38.3, "$graveGone")
    }

    @Test
    fun theUndoComesAfterTheHandIsUp() {
        // Let go at about 2.2 s into the bury, then the flight, the rest and the rise: 4.2 s.
        val heldAt =
            (GraveUndoDemos.ToRowMs +
                GraveUndoDemos.GripMs +
                GraveUndoDemos.DragMs +
                GraveUndoDemos.HoldMs +
                1950 +
                550 +
                1690) / 1000.0
        assertTrue(heldAt < 6.5, "$heldAt")
    }

    @Test
    fun theClipFitsInFortySeconds() {
        assertTrue(demo.script.nominalDuration <= 40.seconds, "${demo.script.nominalDuration}")
    }
}
