@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.crtpipdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class CrtPipDemosTest {
    private val demo = CrtPipDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("crtpip.minimize", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipSwipesDownBrowsesSlidesTheSetTapsItAndResets() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(swipe-down)",
                "tapControl(browse-down)",
                "tapControl(browse-up)",
                "tapControl(slide-set)",
                "tapControl(tap-set)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theStepsDoNotOverlap() {
        val steps = demo.script.steps.sortedBy { it.at }
        for (i in 1 until steps.size) {
            assertTrue(steps[i - 1].at + steps[i - 1].lasts <= steps[i].at, "step $i overlaps")
        }
    }

    @Test
    fun theClipFitsInFifteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 15.seconds, "${demo.script.nominalDuration}")
    }
}
