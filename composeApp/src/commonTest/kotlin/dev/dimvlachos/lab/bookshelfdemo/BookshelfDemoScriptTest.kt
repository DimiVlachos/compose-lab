@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.bookshelfdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class BookshelfDemoScriptTest {
    private val demo = BookshelfDemos.all.single()

    @Test
    fun itIsATallDemoThatPlaysByItself() {
        assertEquals("bookshelf.reading", demo.id)
        assertTrue(demo.tall)
        assertTrue(demo.autoplay)
    }

    @Test
    fun theClipReadsABookOffTheRingThenOneOffThePlanksAndPutsEverythingBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)

        assertEquals(
            listOf(
                "tapControl(${BookshelfDemos.SpinOn})",
                "tapControl(${BookshelfDemos.SpinBack})",
                "tapControl(${BookshelfDemos.TakeOut})",
                "tapControl(${BookshelfDemos.TurnOn})",
                "tapControl(${BookshelfDemos.TurnOn})",
                "tapControl(${BookshelfDemos.TurnBack})",
                "tapControl(${BookshelfDemos.PutBack})",
                "tapControl(${BookshelfDemos.ToPlanks})",
                "tapControl(${BookshelfDemos.TakeOutPlank})",
                "tapControl(${BookshelfDemos.PutBack})",
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
