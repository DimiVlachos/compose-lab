@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.paperbagdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PaperBagDemoScriptTest {
    private val demo = PaperBagDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("paperbag.cart", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipFillsTheBagTakesTheApplesBackOutAndEndsEmpty() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)

        assertEquals(
            listOf(
                "tapControl(add-bread)",
                "tapControl(add-celery)",
                "tapControl(add-apples)",
                "tapControl(add-flowers)",
                "tapControl(bag)",
                "tapControl(remove-apples)",
                "tapControl(done)",
                "tapControl(bag)",
                "resetFeed()",
                "tapControl(done)",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun everyProductTheScriptAddsIsOnTheShelf() {
        for (id in listOf("bread", "celery", "apples", "flowers")) Groceries.byId(id)
    }

    @Test
    fun theClipFitsInTwentySixSeconds() {
        assertTrue(demo.script.nominalDuration <= 26.seconds, "${demo.script.nominalDuration}")
    }
}
