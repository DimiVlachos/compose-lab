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
    fun theClipOpensClosesSlidesOntoThePencilTapsDoneAndResets() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(tap-cup)",
                "tapControl(tap-cup)",
                "tapControl(slide-to-note)",
                "tapControl(done)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
        // The prototype's timeline: the first tap after 0.9 s, Done about 8 s in.
        assertEquals(listOf(1500L, 3590L, 5480L, 8610L, 10820L), controller.calls.map { it.first })
    }

    @Test
    fun theClipFitsInFourteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 14.seconds, "${demo.script.nominalDuration}")
    }

    @Test
    fun theListStartsWithSixNotes() {
        assertEquals(6, StartingNotes.size)
        assertEquals(StartingNotes.size, StartingNotes.map { it.id }.toSet().size)
    }
}
