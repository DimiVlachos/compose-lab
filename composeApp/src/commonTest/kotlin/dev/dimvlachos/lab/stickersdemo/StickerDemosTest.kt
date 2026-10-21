@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.stickersdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class StickerDemosTest {
    private val demo = StickerDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("stickers.rating", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipDragsToFiveBackToThreeTapsOneAndFiveAndClearsTheRow() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(drag-all)",
                "tapControl(drag-back)",
                "tapControl(tap-first)",
                "tapControl(tap-fifth)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInFifteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 15.seconds, "${demo.script.nominalDuration}")
    }
}
