@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.photocopierdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PhotocopierDemosTest {
    private val demo = PhotocopierDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("photocopier.duplicate", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipHoldsDuplicatesFeedsTheCopyBackAndResets() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(long-press)",
                "tapControl(tap-duplicate)",
                "tapControl(feed-back)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInSixteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 16.seconds, "${demo.script.nominalDuration}")
    }
}
