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
    fun theClipBuriesATaskThenUndoesItAndEndsAsItBegan() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf("tapControl(bury)", "tapControl(undo)", "resetFeed()"),
            controller.calls.map { it.second },
        )
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
    fun theClipFitsInSixteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 16.seconds, "${demo.script.nominalDuration}")
    }
}
