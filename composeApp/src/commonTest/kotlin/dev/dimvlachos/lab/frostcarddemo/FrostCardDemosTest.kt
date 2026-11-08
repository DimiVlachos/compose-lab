@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.frostcarddemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FrostCardDemosTest {
    private val demo = FrostCardDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("frostcard.freeze", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipFreezesTheCardThenThawsItAndEndsAsItBegan() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf("tapControl(freeze)", "tapControl(thaw)", "resetFeed()"),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theThumbComesOnlyOnceTheFrostHasCoveredTheCard() {
        // The switch flips once the finger reaches it and presses, and the frost takes 2.8 s to
        // cover the card; the thumb starts a moment after that.
        val flip = (FrostCardDemos.ToSwitchMs + FrostCardDemos.PressMs) / 1000.0
        assertTrue(flip + 2.8 < 5.4, "the thaw starts at 5.4 s")
        assertTrue(demo.script.nominalDuration <= 20.seconds)
    }
}
