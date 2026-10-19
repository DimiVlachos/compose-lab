package dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
class PageTurnStateTest {

    private val page = 400f
    private val height = 520f
    private val spine = 420f
    private val middle = height / 2f
    private val rightEdge = spine + 0.9f * page
    private val leftEdge = spine - 0.9f * page
    private val layout =
        BookLayout(
            left = 0f,
            top = 0f,
            geometry =
                PageGeometry(
                    spineX = spine,
                    originY = height * PageTurnDimens.OriginYFraction,
                    perspectivePx = 2250f,
                    page = page,
                    height = height,
                ),
        )

    private fun TestScope.book(count: Int = 6, live: Int = 2): Pair<PageTurnState, Job> {
        val job = Job(backgroundScope.coroutineContext[Job])
        val scope = CoroutineScope(coroutineContext + FrameClock(testScheduler) + job)
        val state = PageTurnState(scope)
        state.layout = layout
        state.sync(key = 1, count = count, live = live)
        return state to job
    }

    private fun TestScope.frame(ms: Long = 16) {
        advanceTimeBy(ms)
        runCurrent()
    }

    private fun TestScope.settle() = frame(3_000)

    private fun PageTurnState.take(x: Float, leftwards: Boolean): Boolean =
        dragStart(Offset(x, middle), Offset(x + if (leftwards) -12f else 12f, middle))

    private fun TestScope.moveTo(state: PageTurnState, from: Float, to: Float, steps: Int = 6) {
        for (i in 1..steps) {
            state.dragTo(Offset(from + (to - from) * i / steps, middle))
            frame()
        }
    }

    private val PageTurnState.held: Float
        get() = flights.last().t

    @Test
    fun openingAChapterShowsTheNarratedPageAndTurnsWithTheNarration() = runTest {
        val (state, job) = book(live = 2)
        assertEquals(2, state.page)

        state.sync(key = 1, count = 6, live = 3)
        assertTrue(state.isTurning)
        settle()

        assertEquals(3, state.page)
        assertFalse(state.isTurning)
        job.cancel()
    }

    @Test
    fun aTapBrowsesAwayFromTheNarrationAndBackToNarrationReturns() = runTest {
        val (state, job) = book(live = 2)

        state.tap(forward = true)
        settle()
        assertEquals(3, state.page)
        assertTrue(state.isBrowsing)

        state.sync(key = 1, count = 6, live = 4)
        settle()
        assertEquals(3, state.page)

        state.backToNarration()
        settle()
        assertEquals(4, state.page)
        assertFalse(state.isBrowsing)
        job.cancel()
    }

    @Test
    fun theNarrationFarAwayIsReachedWithOneTurn() = runTest {
        val (state, job) = book(count = 20, live = 2)

        state.sync(key = 1, count = 20, live = 12)
        assertEquals(1, state.flights.size)
        settle()

        assertEquals(12, state.page)
        job.cancel()
    }

    @Test
    fun facesThatAreNotReadyBlockTurnsAndTheNarrationSnaps() = runTest {
        val (state, job) = book(live = 2)
        state.canTurn = { false }

        assertFalse(state.take(rightEdge, leftwards = true))
        state.tap(forward = true)
        assertEquals(2, state.page)

        state.sync(key = 1, count = 6, live = 3)
        assertEquals(3, state.page)
        assertFalse(state.isTurning)
        job.cancel()
    }

    @Test
    fun thereIsNoPageBeforeTheFirstOrAfterTheLast() = runTest {
        val (state, job) = book(count = 2, live = 0)

        assertFalse(state.take(leftEdge, leftwards = false))
        state.tap(forward = true)
        settle()
        assertEquals(1, state.page)
        assertFalse(state.take(rightEdge, leftwards = true))
        job.cancel()
    }

    @Test
    fun theLeafFollowsTheFingerAllTheWayOverWithoutJumping() = runTest {
        val (state, job) = book()
        assertTrue(state.take(rightEdge, leftwards = true))
        var last = state.held
        val end = leftEdge + 0.15f * page
        for (i in 1..40) {
            state.dragTo(Offset(rightEdge + (end - rightEdge) * i / 40f, middle))
            frame()
            val now = state.held
            assertTrue(now >= last - 0.001f, "move $i: $last then $now")
            assertTrue(now - last < 0.15f, "move $i jumped: $last then $now")
            last = now
        }
        assertTrue(last > 0.75f, "over on the left: $last")
        job.cancel()
    }

    @Test
    fun aShortSlowDragFallsBackAndAFlickFinishes() = runTest {
        val (state, job) = book()
        state.take(rightEdge, leftwards = true)
        moveTo(state, rightEdge, rightEdge - 0.3f * page, steps = 10)
        state.dragEnd(Offset(-100f, 0f))
        settle()
        assertEquals(2, state.page)

        state.take(rightEdge, leftwards = true)
        moveTo(state, rightEdge, rightEdge - 0.2f * page)
        state.dragEnd(Offset(-3_000f, 0f))
        settle()
        assertEquals(3, state.page)
        assertFalse(state.following)
        job.cancel()
    }

    @Test
    fun aBackwardDragTurnsThePreviousLeafBack() = runTest {
        val (state, job) = book(live = 2)
        state.take(leftEdge, leftwards = false)
        moveTo(state, leftEdge, spine + 0.2f * page, steps = 10)
        state.dragEnd(Offset.Zero)
        settle()
        assertEquals(1, state.page)
        job.cancel()
    }

    @Test
    fun aCornerHeldNearTheTopPeelsThatCornerFirst() = runTest {
        val (state, job) = book()
        state.dragStart(Offset(rightEdge, 0.05f * height), Offset(rightEdge - 12f, 0.05f * height))
        for (i in 1..20) {
            state.dragTo(Offset(rightEdge - 0.4f * page * i / 20, 0.05f * height))
            frame()
        }
        assertTrue(
            state.flights.last().tilt > 0f,
            "leaning to the top: ${state.flights.last().tilt}",
        )
        job.cancel()
    }

    @Test
    fun aSecondSwipeWhileAPageLandsTurnsTheNextLeaf() = runTest {
        val (state, job) = book(live = 1)
        state.tap(forward = true)
        frame(48)
        state.tap(forward = true)
        assertEquals(2, state.flights.size)
        settle()
        assertEquals(3, state.page)
        assertFalse(state.isTurning)
        job.cancel()
    }

    @Test
    fun aHardFlickStopsAtTheSpine() = runTest {
        val (state, job) = book()
        state.take(rightEdge, leftwards = true)
        moveTo(state, rightEdge, rightEdge - 0.2f * page)
        state.dragEnd(Offset(-20_000f, 0f))
        var furthest = 0f
        repeat(60) {
            frame()
            furthest = maxOf(furthest, state.flights.lastOrNull()?.t ?: 0f)
        }
        assertTrue(furthest <= 1f, "reached $furthest")
        assertEquals(3, state.page)
        job.cancel()
    }

    @Test
    fun aNewChapterResetsToItsNarratedPage() = runTest {
        val (state, job) = book(live = 2)
        state.tap(forward = true)
        frame()

        state.sync(key = 2, count = 4, live = 0)
        assertEquals(0, state.page)
        assertFalse(state.isTurning)
        assertTrue(state.following)
        job.cancel()
    }

    @Test
    fun aFarNarrationHandsTheJumpToTheReaderInsteadOfSnapping() = runTest {
        val (state, job) = book(count = 20, live = 2)
        var jumped: Pair<Int, Int>? = null
        state.onJump = { from, to -> jumped = from to to }

        state.sync(key = 1, count = 20, live = 12)

        assertEquals(2 to 12, jumped)
        assertEquals(2, state.page)
        assertFalse(state.isTurning)
        job.cancel()
    }

    @Test
    fun aRiffleTurnsLeafAfterLeafAndLandsOnItsTarget() = runTest {
        val (state, job) = book(count = 6, live = 0)
        var landed = false

        state.riffle(key = 9, count = 6, from = 0, to = 4) { landed = true }
        frame(200)
        assertTrue(state.isRiffling)
        assertTrue(state.flights.size > 1)
        settle()

        assertTrue(landed)
        assertFalse(state.isRiffling)
        assertEquals(4, state.page)
        job.cancel()
    }

    @Test
    fun aRiffleBackwardsLandsOnTheFirstPage() = runTest {
        val (state, job) = book(count = 6, live = 0)

        state.riffle(key = 9, count = 6, from = 5, to = 0) {}
        settle()

        assertEquals(0, state.page)
        job.cancel()
    }

    @Test
    fun aRiffleWhoseFacesNeverBakeSnapsToItsTarget() = runTest {
        val (state, job) = book(count = 6, live = 0)
        state.canTurn = { false }
        var landed = false

        state.riffle(key = 9, count = 6, from = 0, to = 4) { landed = true }
        settle()

        assertTrue(landed)
        assertEquals(4, state.page)
        job.cancel()
    }

    @Test
    fun tapsAreIgnoredWhileARiffleRuns() = runTest {
        val (state, job) = book(count = 6, live = 0)

        state.riffle(key = 9, count = 6, from = 0, to = 3) {}
        state.tap(forward = false)
        settle()

        assertEquals(3, state.page)
        assertTrue(state.following)
        job.cancel()
    }

    private class FrameClock(private val scheduler: TestCoroutineScheduler) : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            delay(FRAME_MS)
            return onFrame(scheduler.currentTime * 1_000_000L)
        }
    }

    private companion object {
        const val FRAME_MS = 16L
    }
}
