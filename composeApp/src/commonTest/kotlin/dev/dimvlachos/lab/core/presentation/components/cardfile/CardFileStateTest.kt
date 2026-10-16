@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.runtime.MonotonicFrameClock
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

/** A frame every 16 ms of virtual time. */
private class StepClock : MonotonicFrameClock {
    private var nanos = 0L

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        kotlinx.coroutines.delay(16)
        nanos += 16_000_000L
        return onFrame(nanos)
    }
}

class CardFileStateTest {
    private suspend fun file(count: Int = 30, now: () -> Long = { 0L }) =
        CardFileState(now = now).also { it.updateCount(count) }

    private fun clocked(block: suspend TestScope.() -> Unit) = runTest {
        withContext(StepClock()) { block() }
    }

    @Test
    fun aSlowDragAndReleaseSettlesOnAWholeCard() = clocked {
        val s = file()
        s.dragBy(2.3f)
        s.release(0f)
        assertEquals(2f, s.position, 1e-3f)
        assertEquals(2, s.front)
    }

    /** The furthest the wheel goes either way while [block] runs. */
    private suspend fun TestScope.span(
        s: CardFileState,
        block: suspend () -> Unit,
    ): Pair<Float, Float> {
        var low = s.position
        var high = s.position
        val sampler = launch {
            while (true) {
                low = minOf(low, s.position)
                high = maxOf(high, s.position)
                delay(1)
            }
        }
        block()
        sampler.cancel()
        return low to high
    }

    @Test
    fun aFlingPastTheEndSettlesOnTheLastCard() = clocked {
        val s = file()
        s.dragBy(25f)
        val (_, high) = span(s) { s.release(400f) }
        assertEquals(29f, s.position, 1e-3f)
        assertTrue(high <= 29.05f, "swung out to $high past the last card")
    }

    @Test
    fun aFlingAtTheLastCardDoesNotSwingPastIt() = clocked {
        for (v in listOf(40f, 400f, 4000f)) {
            val s = file()
            s.spinTo(28)
            val (_, high) = span(s) { s.release(v) }
            assertEquals(29f, s.position, 1e-3f)
            assertTrue(high <= 29.05f, "a fling of $v swung out to $high")
        }
    }

    @Test
    fun aFlingAtTheFirstCardDoesNotSwingPastIt() = clocked {
        for (v in listOf(-40f, -400f, -4000f)) {
            val s = file()
            s.spinTo(2)
            val (low, _) = span(s) { s.release(v) }
            assertEquals(0f, s.position, 1e-3f)
            assertTrue(low >= -0.05f, "a fling of $v swung out to $low")
        }
    }

    @Test
    fun aFlingMidDeckStillCarriesOnItsWay() = clocked {
        val s = file()
        s.spinTo(5)
        s.release(20f)
        assertEquals(11f, s.position, 1e-3f)
    }

    @Test
    fun spinToLandsExactlyAndTicksOnTheWay() = clocked {
        val s = file(now = { testScheduler.currentTime })
        var ticks = 0
        s.onTick = { ticks++ }
        s.spinTo(6)
        assertEquals(6f, s.position, 1e-3f)
        assertTrue(ticks in 1..6, "$ticks ticks")
    }

    @Test
    fun aCardPullsOutAndFilesBack() = clocked {
        val s = file()
        s.spinTo(3)
        assertTrue(s.pull())
        assertEquals(3, s.pulled)
        assertEquals(1f, s.pullProgress, 1e-3f)
        s.file()
        assertNull(s.pulled)
        assertEquals(0f, s.pullProgress, 1e-3f)
    }

    @Test
    fun spinningIsIgnoredWhileACardIsOut() = clocked {
        val s = file()
        s.pull()
        s.dragBy(4f)
        s.release(30f)
        s.spinTo(9)
        assertEquals(0f, s.position, 1e-3f)
    }

    @Test
    fun aShortPushSpringsBackAndOneThatTakesTheCardMostOfTheWayInFilesIt() = clocked {
        val s = file()
        s.pushTravel = 100f
        s.pull()
        // Pushed down 20 of the 100 it travels, the card is still 80% out: it comes back out.
        s.pushTo(20f)
        assertEquals(.8f, s.pullProgress, 1e-3f)
        s.releasePush(0f)
        assertEquals(0, s.pulled)
        assertEquals(1f, s.pullProgress, 1e-3f)
        // Pushed 30 down, it is under 72% out: it files.
        s.pushTo(30f)
        s.releasePush(0f)
        assertNull(s.pulled)
    }

    @Test
    fun aFlickDownFasterThanSevenHundredFilesItToo() = clocked {
        val s = file()
        s.pushTravel = 100f
        s.pull()
        s.pushTo(5f)
        s.releasePush(600f)
        assertEquals(0, s.pulled, "a slow push springs back")
        s.pushTo(5f)
        s.releasePush(800f)
        assertNull(s.pulled)
    }

    @Test
    fun aGentleReleaseRestsOnTheNearestCard() = clocked {
        val s = file()
        s.dragBy(4.3f)
        s.release(1f)
        assertEquals(4f, s.position, 1e-3f)
    }

    @Test
    fun aDividerCantBePulled() = clocked {
        val s = file()
        s.canPull = { it != 0 }
        assertFalse(s.pull())
        assertNull(s.pulled)
    }

    @Test
    fun aOneCardDeckDoesNotSpinButPulls() = clocked {
        val s = file(count = 1)
        s.dragBy(3f)
        s.release(20f)
        assertEquals(0f, s.position, 1e-3f)
        assertTrue(s.pull())
    }

    @Test
    fun aScriptedSpinTakesItsTime() = clocked {
        val s = file()
        val start = testScheduler.currentTime
        s.spinBy(3, 600.milliseconds)
        assertEquals(3f, s.position, 1e-3f)
        assertTrue(abs((testScheduler.currentTime - start) - 600) <= 40)
    }

    @Test
    fun withReducedMotionAReleaseSnapsToTheCard() = clocked {
        val s = file()
        s.reducedMotion = true
        s.dragBy(1.4f)
        val start = testScheduler.currentTime
        s.release(0f)
        assertEquals(1f, s.position)
        assertEquals(start, testScheduler.currentTime)
    }

    @Test
    fun grabbingASpinningWheelCarriesOnFromWhereItIs() = clocked {
        val s = file()
        val job = launch(StepClock()) { s.release(30f) }
        delay(100)
        val before = s.position
        assertTrue(before > 0.5f, "$before")
        s.beginDrag()
        s.dragBy(0.1f)
        assertEquals(before + 0.1f, s.position, 0.05f)
        job.cancel()
    }

    @Test
    fun draggingWithoutBeginDragStillCatchesASpinningWheel() = clocked {
        val s = file()
        val job = launch(StepClock()) { s.release(30f) }
        delay(100)
        val before = s.position
        s.dragBy(0.1f)
        assertEquals(before + 0.1f, s.position, 0.05f)
        job.cancel()
    }

    @Test
    fun grabbingTheWheelPastAnEndCarriesOnFromWhereItIs() = clocked {
        val s = file()
        s.dragBy(-2f)
        val before = s.position
        s.beginDrag()
        s.dragBy(-0.1f)
        assertTrue(s.position < before && before - s.position < 0.1f, "${s.position} from $before")
    }

    @Test
    fun pullingMidSpinStopsOnTheCardPulled() = clocked {
        val s = file()
        val job = launch(StepClock()) { s.spinTo(6) }
        delay(100)
        assertTrue(s.pull())
        job.cancel()
        val card = s.pulled
        assertEquals(card!!.toFloat(), s.position, 1e-3f)
        assertEquals(card, s.front)
    }

    @Test
    fun aScriptedSpinCutShortSettlesOnAWholeCard() = clocked {
        val s = file()
        val job = launch(StepClock()) { s.spinBy(1, 600.milliseconds) }
        delay(150)
        job.cancel()
        val stopped = s.position
        assertTrue(stopped > 0.05f && stopped < 0.95f, "stopped between cards at $stopped")
        s.settle()
        assertEquals(s.front.toFloat(), s.position, 1e-3f)
    }

    @Test
    fun aPullCutShortComesAllTheWayOut() = clocked {
        val s = file()
        val job = launch(StepClock()) { s.pull() }
        delay(100)
        job.cancel()
        assertTrue(s.pullProgress < 1f)
        s.settle()
        assertEquals(0, s.pulled)
        assertEquals(1f, s.pullProgress, 1e-3f)
    }

    @Test
    fun withReducedMotionAScriptedSpinSnaps() = clocked {
        val s = file()
        s.reducedMotion = true
        val start = testScheduler.currentTime
        s.spinBy(3, 600.milliseconds)
        assertEquals(3f, s.position)
        assertEquals(start, testScheduler.currentTime)
    }

    @Test
    fun aShrinkingDeckPullsTheWheelBackOntoItsLastCard() = clocked {
        val s = file()
        s.spinTo(20)
        s.updateCount(10)
        assertEquals(9f, s.position, 1e-3f)
        assertEquals(9, s.front)
    }

    @Test
    fun aFastSpinDropsTicksInsteadOfQueuingThem() = clocked {
        val s = file(now = { testScheduler.currentTime })
        var ticks = 0
        s.onTick = { ticks++ }
        s.spinBy(20, 300.milliseconds)
        assertEquals(20f, s.position, 1e-3f)
        assertTrue(ticks in 1..(300 / 16 + 1), "$ticks ticks for 20 cards in 300 ms")
    }

    @Test
    fun twoQuickStepsMoveTwoCards() = clocked {
        val s = file()
        s.spinTo(3)
        val first = launch(StepClock()) { s.step(1) }
        delay(32)
        assertTrue(s.position < 3.5f, "still near 3 at ${s.position}")
        val second = launch(StepClock()) { s.step(1) }
        first.join()
        second.join()
        assertEquals(5f, s.position, 1e-3f)
        s.step(-1)
        assertEquals(4f, s.position, 1e-3f)
    }

    @Test
    fun draggingPastTheEndsTicksNoPhantomCards() = clocked {
        val s = file(now = { testScheduler.currentTime })
        var ticks = 0
        s.onTick = { ticks++ }
        s.dragBy(-3f)
        delay(100)
        s.dragBy(3f)
        assertEquals(0, ticks, "no card crossed going past the first card and back")
        s.spinTo(29)
        delay(100)
        ticks = 0
        s.dragBy(3f)
        delay(100)
        s.dragBy(-3f)
        assertEquals(0, ticks, "no card crossed going past the last card and back")
    }

    @Test
    fun aStoppedWheelRestsWhereItWasCaught() = clocked {
        val s = file()
        val job = launch(StepClock()) { s.spinTo(8) }
        delay(100)
        assertTrue(s.isSpinning)
        s.stop()
        val caught = s.position
        delay(200)
        assertEquals(caught, s.position)
        assertFalse(s.isSpinning)
        job.cancel()
    }
}
