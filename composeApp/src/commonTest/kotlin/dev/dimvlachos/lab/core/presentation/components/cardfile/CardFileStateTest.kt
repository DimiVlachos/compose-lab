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

    @Test
    fun aFlingPastTheEndSettlesOnTheLastCard() = clocked {
        val s = file()
        s.dragBy(25f)
        s.release(400f)
        assertEquals(29f, s.position, 1e-3f)
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
    fun aPushPastAThirdFilesTheCardAndAShortOneSpringsBack() = clocked {
        val s = file()
        s.pull()
        s.pushBy(0.15f)
        s.releasePush(0f)
        assertEquals(0, s.pulled)
        assertEquals(0f, s.pushDown, 1e-3f)
        s.pushBy(0.4f)
        s.releasePush(0f)
        assertNull(s.pulled)
    }

    @Test
    fun aDownwardFlickFilesItToo() = clocked {
        val s = file()
        s.pull()
        s.pushBy(0.05f)
        s.releasePush(3f)
        assertNull(s.pulled)
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
        assertTrue(ticks in 1 until 20, "$ticks ticks for 20 cards")
    }
}
