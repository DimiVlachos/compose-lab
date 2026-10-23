@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.locketdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class LocketDemosTest {
    private val demo = LocketDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("locket.avatar", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipOpensTheLocketDragsAPhotoInPushesItShutSwingsItAndPutsItBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(open-catch)",
                "tapControl(drag-photo)",
                "tapControl(push-shut)",
                "tapControl(swing)",
                "tapControl(leave)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theStepsDoNotOverlapAndTheClipIsShort() {
        val steps = demo.script.steps.sortedBy { it.at }
        for (i in 1 until steps.size) {
            assertTrue(steps[i - 1].at + steps[i - 1].lasts <= steps[i].at, "step $i overlaps")
        }
        assertTrue(demo.script.nominalDuration <= 14.seconds, "${demo.script.nominalDuration}")
    }
}
