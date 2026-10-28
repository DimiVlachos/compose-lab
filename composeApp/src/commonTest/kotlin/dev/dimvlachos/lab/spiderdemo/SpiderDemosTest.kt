@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.spiderdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class SpiderDemosTest {
    private val demo = SpiderDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("spider.refresh", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipPullsShortThenFullThenTapsTheButtonAndResets() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(pull-short)",
                "tapControl(pull)",
                "tapControl(button)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInTwentyFiveSeconds() {
        assertTrue(demo.script.nominalDuration <= 25.seconds, "${demo.script.nominalDuration}")
    }
}
