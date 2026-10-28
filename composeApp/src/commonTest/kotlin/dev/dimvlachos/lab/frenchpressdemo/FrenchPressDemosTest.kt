@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.frenchpressdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FrenchPressDemosTest {
    private val demo = FrenchPressDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("frenchpress.summary", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipShowsEveryWayToSummarizeAndGiveTheStoryBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(push-early)",
                "tapControl(push)",
                "tapControl(full)",
                "tapControl(tap)",
                "tapControl(pull-up)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInThirtyTwoSeconds() {
        assertTrue(demo.script.nominalDuration <= 32.seconds, "${demo.script.nominalDuration}")
    }
}
