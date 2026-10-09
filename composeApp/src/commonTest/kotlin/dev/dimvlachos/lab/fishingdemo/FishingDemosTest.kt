@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.fishingdemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingDimens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FishingDemosTest {
    private val demo = FishingDemos.all.single()

    @Test
    fun onThePhoneTheFeedWaitsToBePulled() {
        assertEquals("feed.fishing", demo.id)
        assertFalse(demo.autoplay)
    }

    @Test
    fun theClipPullsFourTimesPastTheThreshold() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(4, controller.refreshPulls.size)
        // Material's pull gives half of the finger's travel to the list.
        assertTrue(FishingDemos.Pull / 2 > FishingDimens.Band, "the pulls must pass the threshold")
    }

    @Test
    fun theClipEndsWithTheFeedPutBackSoItLoops() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        val last = controller.calls.last()
        assertEquals("resetFeed()", last.second)
        // After the last refresh has played out and the water has closed.
        assertTrue(last.first >= 23_000, "reset at ${last.first} ms")
    }

    @Test
    fun theClipFitsInTwentySixSeconds() {
        assertTrue(demo.script.nominalDuration <= 26.seconds, "${demo.script.nominalDuration}")
    }

    @Test
    fun theFeedsTurnsCatchTwoFindNothingFailThenCatchOne() {
        val turns = IslandFeedTurns.scripted
        assertEquals(4, turns.size)
        assertEquals(2, (turns[0] as FeedTurn.Catch).islands.size)
        assertTrue(turns[1] is FeedTurn.Empty)
        assertTrue(turns[2] is FeedTurn.Fail)
        assertEquals(1, (turns[3] as FeedTurn.Catch).islands.size)
        // A fast answer, to show the bobber floats a moment however quick the refresh is.
        assertTrue(turns[1].after < FishingDimens.MinWaitSeconds.toDouble().seconds)
    }

    @Test
    fun everyIslandIsInTheFeedOnceAfterTheScript() {
        val caught =
            IslandFeedTurns.scripted.filterIsInstance<FeedTurn.Catch>().flatMap {
                it.islands
            }
        val all = IslandFeedTurns.start + caught
        assertEquals(12, all.size)
        assertEquals(all.size, all.map { it.id }.toSet().size, "an island twice would clash")
    }
}
