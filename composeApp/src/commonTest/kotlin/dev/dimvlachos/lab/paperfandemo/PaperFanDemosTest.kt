@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.paperfandemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PaperFanDemosTest {
    private val demo = PaperFanDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("paperfan.faq", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipOpensOneAnswerThenAnotherAndSweepsItShutThenFoldsAll() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf("tapControl(tap:1)", "tapControl(tap:3)", "tapControl(fold:3)", "resetFeed()"),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theTapsLandWhenThePrototypesDo() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        // Each tap's start plus its glide and press: the prototype's 1.53 s and 4.76 s.
        val press = PaperFanDemos.GlideMs + PaperFanDemos.PressMs
        assertEquals(listOf(1530L, 4760L), controller.calls.take(2).map { it.first + press })
    }

    @Test
    fun controlsRoundTrip() {
        assertEquals(3, PaperFanDemos.tapped(PaperFanDemos.tap(3)))
        assertEquals(2, PaperFanDemos.folded(PaperFanDemos.fold(2)))
        assertNull(PaperFanDemos.tapped(PaperFanDemos.fold(1)))
        assertNull(PaperFanDemos.folded(PaperFanDemos.tap(1)))
    }

    @Test
    fun theClipFitsInTwelveSeconds() {
        assertTrue(demo.script.nominalDuration <= 12.seconds, "${demo.script.nominalDuration}")
    }
}
