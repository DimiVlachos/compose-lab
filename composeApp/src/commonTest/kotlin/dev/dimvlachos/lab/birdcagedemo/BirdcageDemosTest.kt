@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.birdcagedemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class BirdcageDemosTest {
    private val demo = BirdcageDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("birdcage.mute", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipSingsMutesStirsLiftsAndSingsAgainThenResets() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(message)",
                "tapControl(cover-down)",
                "tapControl(message)",
                "tapControl(cover-up)",
                "tapControl(message)",
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
