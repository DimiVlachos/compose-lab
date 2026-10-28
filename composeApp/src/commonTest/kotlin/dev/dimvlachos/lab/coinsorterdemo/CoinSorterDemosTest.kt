@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.coinsorterdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class CoinSorterDemosTest {
    private val demo = CoinSorterDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("coinsorter.sort", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipSortsEveryWayThenBackToBestMatch() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(sort-low)",
                "tapControl(sort-high)",
                "tapControl(sort-top)",
                "tapControl(sort-best)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInTwentyEightSeconds() {
        assertTrue(demo.script.nominalDuration <= 28.seconds, "${demo.script.nominalDuration}")
    }

    @Test
    fun theResultsAreThePrototypesEight() {
        assertEquals(8, LanternResults.size)
        assertEquals("Pocket Glow Mini, 2-pack", LanternResults.minBy { it.cents }.name)
        assertTrue(LanternResults.all { '\\' !in it.name })
    }
}
