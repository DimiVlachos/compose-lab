@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.pencilcupdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PencilCupDemosTest {
    private val demo = PencilCupDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("pencilcup.fab", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipTapsTheChecklistSlidesOntoThePencilTapsTheScanAndResets() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(tap-cup)",
                "tapControl(tap-list)",
                "tapControl(type-item)",
                "tapControl(done)",
                "tapControl(slide-to-note)",
                "tapControl(type-title)",
                "tapControl(done)",
                "tapControl(tap-cup)",
                "tapControl(tap-scan)",
                "tapControl(done)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theStepsDoNotOverlap() {
        val steps = demo.script.steps.sortedBy { it.at }
        for (i in 1 until steps.size) {
            assertTrue(steps[i - 1].at + steps[i - 1].lasts <= steps[i].at, "step $i overlaps")
        }
    }

    @Test
    fun theClipFitsInTwentyTwoSeconds() {
        assertTrue(demo.script.nominalDuration <= 22.seconds, "${demo.script.nominalDuration}")
    }

    @Test
    fun theListStartsWithSixNotes() {
        assertEquals(6, StartingNotes.size)
        assertEquals(StartingNotes.size, StartingNotes.map { it.id }.toSet().size)
    }
}
