@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.paintrollerdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PaintRollerDemosTest {
    private val demo = PaintRollerDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("paintroller.accent", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipPaintsSedonaThenTapsEvergreenUndoingEach() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(paint-sedona)",
                "tapControl(undo)",
                "tapControl(tap-evergreen)",
                "tapControl(undo)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInTwentySixSeconds() {
        assertTrue(demo.script.nominalDuration <= 26.seconds, "${demo.script.nominalDuration}")
    }
}
