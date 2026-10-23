@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.stickynotedemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class StickyNoteDemosTest {
    private val demo = StickyNoteDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("stickynote.swipe", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipPullsArchivesSnoozesAndPutsBothBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(short-pull)",
                "tapControl(archive)",
                "tapControl(snooze)",
                "tapControl(tap-later)",
                "tapControl(tap-done)",
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
