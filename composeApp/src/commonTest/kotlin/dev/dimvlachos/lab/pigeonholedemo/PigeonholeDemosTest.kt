@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.pigeonholedemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PigeonholeDemosTest {
    private val demo = PigeonholeDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("pigeonhole.move", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipCallsOffAPostThenPostsOpensGoesBackAndPullsTheReceiptOut() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(post-cancel)",
                "tapControl(post)",
                "tapControl(open-rack)",
                "tapControl(open-receipts)",
                "tapControl(back)",
                "tapControl(open-rack)",
                "tapControl(pull-back)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInTwentyFourSeconds() {
        assertTrue(demo.script.nominalDuration <= 24.seconds, "${demo.script.nominalDuration}")
    }
}
