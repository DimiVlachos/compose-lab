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
    fun theClipGoesEveryWayInAndOutThenEndsAsItBegan() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(hold)",
                "tapControl(peek)",
                "tapControl(thaw)",
                "tapControl(freeze)",
                "tapControl(unfreeze)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theCardIsWarmAgainBeforeTheReset() {
        // The hold freezes the card 450 ms after the thumb lands, and the frost then takes 2.8 s;
        // a peek's hole closes in about 2 s; a held thumb clears the card in about 1.5 s; switched
        // off, the card thaws from the middle in about 1.5 s, and the melt runs off in about 2.
        val d = FrostCardDemos
        val frozen = .9 + (200 + d.ToCardMs) / 1000.0 + .45 + d.FrostSeconds
        val closed = frozen + (200 + d.ToCardMs + d.PeekMs) / 1000.0 + d.CloseSeconds
        val clear = closed + (d.SeeClosedMs + 200 + d.ToCardMs) / 1000.0 + 1.5
        val lifted = clear + d.LiftAsideMs / 1000.0
        val refrozen = lifted + (200 + d.ToSwitchMs + d.PressMs) / 1000.0 + d.FrostSeconds
        val warm = refrozen + (d.SeeFrozenMs + 200 + d.ToSwitchMs + d.PressMs) / 1000.0 + 1.5 + 2
        assertTrue(warm < .9 + 22, "$warm")
    }

    @Test
    fun theClipFitsInFortySeconds() {
        assertTrue(demo.script.nominalDuration <= 40.seconds, "${demo.script.nominalDuration}")
    }
}
