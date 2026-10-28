@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.rubberbanddemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class RubberBandDemosTest {
    private val demo = RubberBandDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("rubberband.unsend", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipSendsPullsBackFixesSendsAgainTwangsAndFadesBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(type-first)",
                "tapControl(send)",
                "tapControl(pull-back)",
                "tapControl(fix)",
                "tapControl(send-again)",
                "tapControl(twang)",
                "tapControl(reply)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun jessRepliesOnlyOnceTheSecondBandHasSnapped() {
        val steps = demo.script.steps
        val sendAgain = steps[4].at
        val reply = steps[6].at // after the twang
        // Sent at the end of its tap, landed 420 ms later, then nine seconds of window.
        val snaps =
            sendAgain + (RubberBandDemos.TapMs + RubberBandDemos.HoldMs + 420 + 9000).milliseconds
        assertTrue(reply > snaps, "$reply vs $snaps")
    }

    @Test
    fun theClipFitsUnderTwentySixSeconds() {
        assertTrue(demo.script.nominalDuration <= 26.seconds, "${demo.script.nominalDuration}")
    }
}
