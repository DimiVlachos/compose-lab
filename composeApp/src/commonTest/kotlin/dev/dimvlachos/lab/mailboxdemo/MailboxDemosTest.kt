@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.mailboxdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class MailboxDemosTest {
    private val demo = MailboxDemos.all.single()

    @Test
    fun itIsATallScreen() {
        assertEquals("mailbox.badge", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipDeliversTwiceOpensReadsMarksAllReadAndGoesBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(arrive)",
                "tapControl(arrive)",
                "tapControl(mailbox)",
                "tapControl(read-first)",
                "tapControl(mark-all-read)",
                "tapControl(back)",
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
