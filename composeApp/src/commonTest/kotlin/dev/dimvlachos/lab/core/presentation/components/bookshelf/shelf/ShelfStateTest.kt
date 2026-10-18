package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfStateTest {

    private val opened = mutableListOf<Int>()
    private val closed = mutableListOf<Int>()

    private val drawnTilts = mutableListOf<Float>()
    private var drawnState: ShelfState? = null

    private fun TestScope.shelf(): Pair<ShelfState, Job> {
        val job = Job()
        val clock =
            FrameClock(testScheduler) { drawnState?.let { drawnTilts += it.motion.value.tilt } }
        val scope = CoroutineScope(coroutineContext + clock + job)
        return ShelfState(scope, { opened += it }, { closed += it }).also { drawnState = it } to job
    }

    private fun TestScope.settle(millis: Long = 4_000) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun ShelfState.pagesReady(bookId: Int) {
        pagesReadyFor = bookId
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun aTipPastTheThresholdTakesTheBookOutAndOpensIt() = runTest {
        val (state, job) = shelf()
        state.pagesReady(3)

        state.beginTip(3)
        state.dragTip(dragY = 45f, grabHeight = 100f)
        state.releaseTip(velocityY = 0f)
        settle()

        assertEquals(ShelfMode.Open, state.mode)
        assertEquals(1f, state.motion.value.travel, 1e-3f)
        assertEquals(1f, state.motion.value.open, 1e-3f)
        assertEquals(listOf(3), opened)
        job.cancel()
    }

    @Test
    fun anEarlyReleaseRocksTheBookBackIntoTheRow() = runTest {
        val (state, job) = shelf()

        state.beginTip(3)
        state.dragTip(dragY = 3f, grabHeight = 100f)
        state.releaseTip(velocityY = 0f)
        assertEquals(ShelfMode.Settle, state.mode)
        settle()

        assertEquals(ShelfMode.Shelf, state.mode)
        assertNull(state.activeBookId)
        assertTrue(opened.isEmpty())
        job.cancel()
    }

    @Test
    fun puttingAnOpenBookBackReturnsItToTheShelf() = runTest {
        val (state, job) = shelf()
        state.pagesReady(3)
        state.openDirect(3)
        settle()

        state.putBack()
        settle()

        assertEquals(ShelfMode.Shelf, state.mode)
        assertEquals(0f, state.motion.value.travel, 1e-3f)
        assertEquals(listOf(3), closed)
        job.cancel()
    }

    @Test
    fun puttingBackMidSlideReversesFromWhereTheBookIs() = runTest {
        val (state, job) = shelf()
        state.pagesReady(3)
        state.openDirect(3)
        advanceTimeBy(400)
        runCurrent()
        val midway = state.motion.value.travel
        assertTrue(midway in 0.2f..0.8f)

        state.putBack()
        advanceTimeBy(150)
        runCurrent()
        assertTrue(state.motion.value.travel < midway)
        settle()

        assertEquals(ShelfMode.Shelf, state.mode)
        assertTrue(opened.isEmpty())
        job.cancel()
    }

    @Test
    fun theBookOnlyOpensOnceItsPagesAreReady() = runTest {
        val (state, job) = shelf()
        state.openDirect(3)
        settle(2_000)
        assertEquals(ShelfMode.Out, state.mode)
        assertEquals(0f, state.motion.value.open, 1e-3f)

        state.pagesReady(3)
        settle()

        assertEquals(ShelfMode.Open, state.mode)
        job.cancel()
    }

    @Test
    fun aLowGrabOnlyNudgesTheBookInPlace() = runTest {
        val (state, job) = shelf()

        state.nudge(5)
        assertEquals(ShelfMode.Settle, state.mode)
        assertEquals(5, state.activeBookId)
        settle()

        assertEquals(ShelfMode.Shelf, state.mode)
        assertEquals(0f, state.motion.value.travel, 1e-3f)
        job.cancel()
    }

    @Test
    fun puttingBackNeverDrawsTheTiltTheBookWasPulledOutWith() = runTest {
        val (state, job) = shelf()
        state.pagesReady(3)
        state.beginTip(3)
        state.dragTip(dragY = 45f, grabHeight = 100f)
        state.releaseTip(velocityY = 0f)
        settle()
        drawnTilts.clear()

        state.putBack()
        settle()

        assertEquals(ShelfMode.Shelf, state.mode)
        val steepest = drawnTilts.maxOf { kotlin.math.abs(it) }
        assertTrue(steepest <= 0.31f, "steepest drawn tilt while returning was $steepest")
        job.cancel()
    }

    @Test
    fun listeningClosesTheOpenBookAndFadesItAway() = runTest {
        val (state, job) = shelf()
        state.pagesReady(3)
        state.openDirect(3)
        settle()

        state.listen()
        settle()

        assertEquals(ShelfMode.Open, state.mode)
        assertTrue(state.listening)
        assertEquals(0f, state.motion.value.open, 1e-3f)
        assertEquals(1f, state.motion.value.vanish, 1e-3f)
        job.cancel()
    }

    @Test
    fun readingAgainBringsTheBookBackAndOpensIt() = runTest {
        val (state, job) = shelf()
        state.pagesReady(3)
        state.openDirect(3)
        settle()
        state.listen()
        settle()

        state.read()
        settle()

        assertFalse(state.listening)
        assertEquals(1f, state.motion.value.open, 1e-3f)
        assertEquals(0f, state.motion.value.vanish, 1e-3f)
        job.cancel()
    }

    @Test
    fun puttingBackFromListeningShowsTheBookBeforeItReturns() = runTest {
        val (state, job) = shelf()
        state.pagesReady(3)
        state.openDirect(3)
        settle()
        state.listen()
        settle()

        state.putBack()
        advanceTimeBy(200)
        runCurrent()
        assertTrue(state.listening)
        assertEquals(1f, state.motion.value.travel, 1e-3f)
        assertTrue(state.motion.value.vanish < 1f)
        settle()

        assertEquals(ShelfMode.Shelf, state.mode)
        assertFalse(state.listening)
        assertEquals(0f, state.motion.value.vanish, 1e-3f)
        assertEquals(listOf(3), closed)
        job.cancel()
    }

    @Test
    fun takingABookOutAfterListeningOpensItToRead() = runTest {
        val (state, job) = shelf()
        state.pagesReady(3)
        state.openDirect(3)
        settle()
        state.listen()
        settle()
        state.putBack()
        settle()

        state.openDirect(3)
        settle()

        assertFalse(state.listening)
        assertEquals(1f, state.motion.value.open, 1e-3f)
        assertEquals(0f, state.motion.value.vanish, 1e-3f)
        job.cancel()
    }

    private class FrameClock(
        private val scheduler: TestCoroutineScheduler,
        private val onDrawn: () -> Unit = {},
    ) : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            delay(FRAME_MS)
            return onFrame(scheduler.currentTime * 1_000_000L).also { onDrawn() }
        }
    }

    private companion object {
        const val FRAME_MS = 16L
    }
}
