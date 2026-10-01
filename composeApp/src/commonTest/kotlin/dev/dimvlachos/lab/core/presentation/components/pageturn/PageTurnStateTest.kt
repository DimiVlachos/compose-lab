package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PageTurnStateTest {
    // At 1.5 the eye stands as far from a page, for its size, as on a phone.
    private val layout = bookLayout(Size(1000f, 1000f / BookAspect), Density(1.5f))
    private val page = layout.geometry.page
    private val spine = layout.left + layout.geometry.spineX
    private val middle = layout.size.height / 2f
    private val top = layout.top + 0.04f * layout.geometry.height
    private val bottom = layout.top + 0.96f * layout.geometry.height

    // Out on the outer part of each page, where a thumb takes it.
    private val rightEdge = spine + 0.9f * page
    private val leftEdge = spine - 0.9f * page

    private fun ComposeUiTest.book(spreads: Int = 4, initialSpread: Int = 0): PageTurnState {
        mainClock.autoAdvance = false
        lateinit var state: PageTurnState
        setContent { state = rememberPageTurnState(spreads, initialSpread) }
        state.layout = layout
        return state
    }

    private fun ComposeUiTest.act(ms: Long = 16, block: () -> Unit) {
        runOnUiThread(block)
        mainClock.advanceTimeBy(ms)
    }

    private fun ComposeUiTest.settle() = mainClock.advanceTimeBy(3_000)

    // A finger down at ([x], [y]) that sets off sideways ([leftwards] or not): true if it took a
    // page.
    private fun PageTurnState.take(x: Float, y: Float = middle, leftwards: Boolean): Boolean {
        val down = Offset(x, y)
        return dragStart(down, down + Offset(if (leftwards) -10f else 10f, 0f))
    }

    // The finger moved in a straight line from ([from], [fromY]) to ([x], [y]) in [steps] frames.
    private fun ComposeUiTest.moveTo(
        state: PageTurnState,
        from: Float,
        x: Float,
        y: Float = middle,
        steps: Int = 1,
        fromY: Float = y,
    ) {
        for (i in 1..steps) {
            act {
                state.dragTo(Offset(from + (x - from) * i / steps, fromY + (y - fromY) * i / steps))
            }
        }
    }

    @Test
    fun nextAndPreviousTurnOneSpreadAndLand() = runComposeUiTest {
        val state = book()
        act { state.next() }
        settle()
        assertEquals(1, state.spread)
        assertEquals(TurnPhase.Idle, state.phase)
        act { state.previous() }
        settle()
        assertEquals(0, state.spread)
    }

    @Test
    fun thereIsNoPageBeforeTheFirstSpreadOrAfterTheLast() = runComposeUiTest {
        val state = book(spreads = 2)
        act { state.previous() }
        settle()
        assertEquals(0, state.spread)
        assertFalse(state.take(leftEdge, leftwards = false))
        act { state.next() }
        settle()
        act { state.next() }
        settle()
        assertEquals(1, state.spread)
        assertFalse(state.take(rightEdge, leftwards = true))
    }

    @Test
    fun theEdgeHeldAtTheSpineStandsTheLeafUp() = runComposeUiTest {
        val state = book()
        act { assertTrue(state.take(rightEdge, leftwards = true)) }
        moveTo(state, rightEdge, spine, steps = 8)
        assertEquals(0, state.pair?.leaf)
        assertTrue(state.leafProgress in 0.35f..0.65f, "standing up: ${state.leafProgress}")
        assertTrue(state.bendDirection < 0f, "the hand leads: ${state.bendDirection}")
    }

    @Test
    fun theLeafFollowsTheFingerAllTheWayOver() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        var last = state.leafProgress
        // Every move on, the leaf turns on; none of them snaps it a long way at once.
        // To just short of where the edge lies on the left, beyond which there is no more turn.
        val end = leftEdge + 0.15f * page
        for (i in 1..40) {
            act { state.dragTo(Offset(rightEdge + (end - rightEdge) * i / 40f, middle)) }
            val now = state.leafProgress
            assertTrue(now >= last - 0.001f, "move $i: $last then $now")
            assertTrue(now - last < 0.15f, "move $i jumped: $last then $now")
            last = now
        }
        assertTrue(last > 0.8f, "over on the left: $last")
    }

    @Test
    fun aPageLiftsOffItsStackGentlyAtTheFirstPull() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        // The slop alone barely stirs it.
        assertTrue(state.leafProgress < 0.03f, "taken: ${state.leafProgress}")
        // Equal steps in: the first lifts it no more than a later one does.
        val step = 0.02f * page
        val lifts =
            (1..6).map { i ->
                val before = state.leafProgress
                act { state.dragTo(Offset(rightEdge - step * i, middle)) }
                state.leafProgress - before
            }
        assertTrue(lifts.first() <= lifts.drop(1).max() * 1.2f, "lifts $lifts")
    }

    @Test
    fun aBackwardDragTurnsThePreviousLeafBack() = runComposeUiTest {
        val state = book(initialSpread = 2)
        act { state.take(leftEdge, leftwards = false) }
        moveTo(state, leftEdge, leftEdge + 0.3f * page, steps = 4)
        assertEquals(1, state.pair?.leaf)
        assertTrue(state.leafProgress in 0.6f..0.99f, "lifted off the left: ${state.leafProgress}")
        act { state.dragEnd(Offset.Zero) }
        settle()
        assertEquals(2, state.spread)
    }

    @Test
    fun aShortSlowDragFallsBack() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        moveTo(state, rightEdge, rightEdge - 0.3f * page, steps = 10)
        act { state.dragEnd(Offset(-100f, 0f)) }
        assertIs<TurnPhase.Settling>(state.phase)
        settle()
        assertEquals(0, state.spread)
        assertEquals(TurnPhase.Idle, state.phase)
    }

    @Test
    fun aDragPastTheThresholdFinishes() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        moveTo(state, rightEdge, spine - 0.2f * page, steps = 10)
        act { state.dragEnd(Offset.Zero) }
        settle()
        assertEquals(1, state.spread)
    }

    @Test
    fun aShortFlickFinishesTheTurn() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        moveTo(state, rightEdge, rightEdge - 0.2f * page)
        act { state.dragEnd(Offset(-3_000f, 0f)) }
        settle()
        assertEquals(1, state.spread)
    }

    @Test
    fun reversingADragSwingsTheBowOverAndFallsBack() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        moveTo(state, rightEdge, spine + 0.2f * page, steps = 12)
        assertTrue(state.bendDirection < 0f, "the hand leads on: ${state.bendDirection}")
        moveTo(state, spine + 0.2f * page, rightEdge - 0.1f * page, steps = 20)
        assertTrue(state.bendDirection > 0f, "and leads back: ${state.bendDirection}")
        act { state.dragEnd(Offset.Zero) }
        settle()
        assertEquals(0, state.spread)
    }

    @Test
    fun aQuickPullBillowsTheLeafAndHeldStillItSettles() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        moveTo(state, rightEdge, spine + 0.3f * page, steps = 6)
        // The air pushes the paper back against the hand's lead...
        val pulled = state.bendDirection
        assertTrue(pulled > -PageTurnDimens.HoldBow + 0.05f, "billowed: $pulled")
        // ...and held still, it lets go and the paper sags back behind the hand.
        repeat(60) { act {} }
        assertTrue(
            abs(state.bendDirection + PageTurnDimens.HoldBow) < 0.1f,
            "settled from $pulled to ${state.bendDirection}",
        )
    }

    @Test
    fun letGoTheFreeEdgeWhipsOverToTrail() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        moveTo(state, rightEdge, spine - 0.1f * page, steps = 10)
        assertTrue(state.bendDirection < 0f)
        act { state.dragEnd(Offset(-1_000f, 0f)) }
        mainClock.advanceTimeBy(200)
        val bow = state.flights.single().bend
        assertTrue(bow > 0f, "trailing: $bow")
    }

    @Test
    fun theTopCornerPeelsFirst() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, top, leftwards = true) }
        moveTo(state, rightEdge, rightEdge - 0.35f * page, top, steps = 8)
        assertTrue(state.tilt > 0.1f, "the top corner leads: ${state.tilt}")
    }

    @Test
    fun aPullOnASlantFoldsSquareToIt() = runComposeUiTest {
        val state = book()
        // Taken at the middle of the edge, where a straight pull turns it straight...
        act { state.take(rightEdge, middle, leftwards = true) }
        // ...and pulled down and in at 45 degrees: the fold lies across the pull, top corner first.
        moveTo(
            state,
            rightEdge,
            rightEdge - 0.3f * page,
            middle + 0.3f * page,
            steps = 10,
            fromY = middle,
        )
        repeat(20) { act {} }
        assertTrue(state.tilt > 0.2f, "down and in leans to the top: ${state.tilt}")
    }

    @Test
    fun aPullUpOnASlantPeelsTheBottomCorner() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, middle, leftwards = true) }
        moveTo(
            state,
            rightEdge,
            rightEdge - 0.3f * page,
            middle - 0.3f * page,
            steps = 10,
            fromY = middle,
        )
        repeat(20) { act {} }
        assertTrue(state.tilt < -0.2f, "up and in leans to the bottom: ${state.tilt}")
    }

    @Test
    fun theBottomCornerPeelsTheOtherWay() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, bottom, leftwards = true) }
        moveTo(state, rightEdge, rightEdge - 0.35f * page, bottom, steps = 8)
        assertTrue(state.tilt < -0.1f, "the bottom corner leads: ${state.tilt}")
    }

    @Test
    fun aHoldAtTheMiddleOfTheEdgeTurnsTheLeafStraight() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, middle, leftwards = true) }
        moveTo(state, rightEdge, rightEdge - 0.35f * page, steps = 8)
        assertTrue(abs(state.tilt) < 0.03f, "straight: ${state.tilt}")
    }

    @Test
    fun movingTheFingerDownTheEdgeLeansTheLeafAfterIt() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, middle, leftwards = true) }
        moveTo(state, rightEdge, spine + 0.2f * page, steps = 10)
        val level = state.tilt
        moveTo(state, spine + 0.2f * page, spine + 0.2f * page, bottom, steps = 10)
        repeat(30) { act {} }
        assertTrue(abs(state.tilt - level) > 0.05f, "leant from $level to ${state.tilt}")
    }

    @Test
    fun takenByTheTopCornerAndMovedToTheBottomTheBottomCornerLeads() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, top, leftwards = true) }
        val across = spine + 0.45f * page
        moveTo(state, rightEdge, across, top, steps = 10)
        repeat(20) { act {} }
        assertTrue(state.tilt > 0.1f, "the top corner leads: ${state.tilt}")
        // Down to the bottom corner while still dragging on, then on across from there.
        val lower = spine + 0.25f * page
        var lastT = state.leafProgress
        var lastTilt = state.tilt
        for (i in 1..30) {
            act {
                state.dragTo(
                    Offset(across + (lower - across) * i / 30f, top + (bottom - top) * i / 30f)
                )
            }
            // The leaf neither jumps nor twists away while the finger goes down the edge.
            assertTrue(abs(state.leafProgress - lastT) < 0.05f, "step $i turn jumped")
            assertTrue(abs(state.tilt - lastTilt) < 0.08f, "step $i lean jumped")
            lastT = state.leafProgress
            lastTilt = state.tilt
        }
        moveTo(state, lower, spine + 0.05f * page, bottom, steps = 15)
        repeat(30) { act {} }
        assertTrue(state.tilt < -0.1f, "the bottom corner leads now: ${state.tilt}")
        // However the finger went, the leaf stays in the book: no corner reaches further out than
        // the same leaf's turning straight, which perspective draws a little past it.
        val flight = state.flights.single()
        fun corner(y: Float, tilt: Float) =
            turnFrame(
                    flight.t,
                    page,
                    flight.bend,
                    height = layout.geometry.height,
                    tilt = tilt,
                    grip = flight.grip,
                )
                .project(page, y, layout.geometry)
                .y
        val tolerance = 0.05f * page
        assertTrue(corner(0f, flight.tilt) > corner(0f, 0f) - tolerance, "top corner out")
        val bottomEdge = layout.geometry.height
        assertTrue(
            corner(bottomEdge, flight.tilt) < corner(bottomEdge, 0f) + tolerance,
            "bottom corner out",
        )
    }

    @Test
    fun aFingerWobblingUpAndDownLeavesTheLeafSteady() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, middle, leftwards = true) }
        val across = spine + 0.3f * page
        moveTo(state, rightEdge, across, steps = 10)
        repeat(30) { act {} }
        val bow = state.bendDirection
        var lastTilt = state.tilt
        // Up and down the edge and back, with a pixel or two of sideways wobble.
        var lastStep = 0f
        var reversals = 0
        for (i in 1..80) {
            val y = middle + 0.3f * page * kotlin.math.sin(i / 80f * 2f * kotlin.math.PI.toFloat())
            val wobble = if (i % 2 == 0) 2f else -2f
            act { state.dragTo(Offset(across + wobble, y)) }
            assertTrue(state.bendDirection * bow > 0f, "step $i: the bow flipped")
            val step = state.tilt - lastTilt
            assertTrue(abs(step) < 0.08f, "step $i: lean jumped $lastTilt to ${state.tilt}")
            // Back and forth from one frame to the next is the wobble showing.
            if (abs(step) > 0.002f && abs(lastStep) > 0.002f && step * lastStep < 0f) reversals++
            if (abs(step) > 0.002f) lastStep = step
            lastTilt = state.tilt
        }
        assertTrue(reversals <= 6, "the lean went back and forth $reversals times")
    }

    @Test
    fun aPeeledLeafLetGoStraightensAsItFalls() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, top, leftwards = true) }
        moveTo(state, rightEdge, rightEdge - 0.35f * page, top, steps = 8)
        val held = state.tilt
        act { state.dragEnd(Offset.Zero) }
        mainClock.advanceTimeBy(250)
        val falling = state.flights.single().tilt
        assertTrue(falling < held * 0.7f, "straightening: $held then $falling")
        settle()
        assertEquals(0, state.spread)
        assertFalse(state.isTurning)
    }

    @Test
    fun aPageCaughtWhileLandingFollowsTheFinger() = runComposeUiTest {
        val state = book()
        act(ms = 120) { state.next() }
        val caughtAt = state.leafProgress
        assertTrue(caughtAt in 0.05f..0.95f, "caught at $caughtAt")
        // A rightwards drag grabs the forward turn in flight and pulls it back down.
        act { assertTrue(state.take(spine + 0.3f * page, leftwards = false)) }
        val caught = assertIs<TurnPhase.Dragging>(state.phase)
        assertTrue(caught.pair.forward)
        moveTo(state, spine + 0.3f * page, layout.size.width, steps = 6)
        assertTrue(state.leafProgress < caughtAt, "pulled back: ${state.leafProgress}")
        act { state.dragEnd(Offset.Zero) }
        settle()
        assertEquals(0, state.spread)
    }

    @Test
    fun aTapTakesTheBookAtOnceSoNothingSlipsInBeforeItsTurnStarts() = runComposeUiTest {
        val state = book()
        runOnUiThread {
            state.next()
            // Before any frame: the turn is already the book's, and it starts from the page.
            assertTrue(state.isTurning)
            assertEquals(0f, state.leafProgress)
        }
        settle()
        assertEquals(1, state.spread)
        assertFalse(state.isTurning)
    }

    @Test
    fun aHardFlickStopsAtTheSpineInsteadOfSailingPastIt() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        moveTo(state, rightEdge, rightEdge - 0.2f * page)
        act { state.dragEnd(Offset(-20_000f, 0f)) }
        var furthest = 0f
        repeat(40) {
            mainClock.advanceTimeBy(16)
            furthest = maxOf(furthest, state.leafProgress)
        }
        assertTrue(furthest <= 1f, "reached $furthest")
        assertEquals(1, state.spread)
        assertFalse(state.isTurning)
    }

    @Test
    fun aSwipeTheSameWayWhileAPageLandsTurnsTheNextLeaf() = runComposeUiTest {
        val state = book()
        act { state.take(rightEdge, leftwards = true) }
        moveTo(state, rightEdge, rightEdge - 0.2f * page)
        act(ms = 32) { state.dragEnd(Offset(-1_500f, 0f)) }
        assertIs<TurnPhase.Settling>(state.phase)
        act { assertTrue(state.take(rightEdge, leftwards = true)) }
        val next = assertIs<TurnPhase.Dragging>(state.phase)
        assertEquals(TurnPair(forward = true, from = 1, to = 2), next.pair)
        assertEquals(1, state.spread, "the spread moves on as the second page is taken")
        // The first page is not snapped down: it is still in the air, landing by itself.
        val landing = state.flights.first { it.leaf == 0 }
        assertTrue(landing.t in 0.2f..0.99f, "first page at ${landing.t}")
        // The new leaf follows the finger straight away.
        var last = state.leafProgress
        for (i in 1..3) {
            act { state.dragTo(Offset(rightEdge - 0.15f * page * i, middle)) }
            assertTrue(state.leafProgress > last, "move $i: $last then ${state.leafProgress}")
            last = state.leafProgress
        }
        act { state.dragEnd(Offset(-1_500f, 0f)) }
        settle()
        assertEquals(2, state.spread)
    }

    @Test
    fun aPageLeftToLandKeepsFallingAndLandsOnItsOwn() = runComposeUiTest {
        val state = book()
        act { state.next() }
        act { state.next() } // the first is still landing
        val first = state.flights.first { it.leaf == 0 }.t
        mainClock.advanceTimeBy(48)
        val later = state.flights.firstOrNull { it.leaf == 0 }?.t ?: 1f
        assertTrue(later > first, "the first page goes on falling: $first then $later")
        assertEquals(2, state.flights.size, "two leaves in the air")
        settle()
        assertEquals(2, state.spread)
        assertTrue(state.flights.isEmpty())
        assertFalse(state.isTurning)
    }

    @Test
    fun aSwipeBackCatchesTheLeafStillComingDown() = runComposeUiTest {
        val state = book()
        act { state.next() }
        act { state.next() } // leaf 0 is left to land, leaf 1 is in hand
        settle()
        act { state.next() }
        act { state.next() } // leaf 2 left to land, leaf 3 in hand, spread at 3
        settle()
        assertEquals(4 - 1, state.spread)
        // Leaf 2 lands; then, while it is still falling, swipe back onto it.
        act { state.previous() }
        assertEquals(TurnPair(forward = false, from = 3, to = 2), state.pair)
        settle()
        assertEquals(2, state.spread)
    }

    @Test
    fun quickSwipesThereAndBackComeHomeOnePageEach() = runComposeUiTest {
        val state = book(spreads = 12, initialSpread = 3)
        fun swipe(forward: Boolean) {
            val from = if (forward) rightEdge else leftEdge
            val sign = if (forward) -1f else 1f
            act(ms = 48) {
                state.take(from, leftwards = forward)
                state.dragTo(Offset(from + sign * 0.3f * page, middle))
            }
            act(ms = 80) { state.dragEnd(Offset(sign * 3_000f, 0f)) }
        }
        repeat(5) { swipe(forward = true) }
        repeat(5) { swipe(forward = false) }
        settle()
        assertEquals(3, state.spread)
        assertFalse(state.isTurning)
    }

    @Test
    fun aDragFollowsEveryMoveOfAQuickFinger() = runComposeUiTest {
        val state = book()
        // Moves land back to back, faster than frames: each one still shows.
        runOnUiThread {
            state.take(rightEdge, leftwards = true)
            var last = state.leafProgress
            for (i in 1..3) {
                state.dragTo(Offset(rightEdge - 0.2f * page * i, middle))
                assertTrue(state.leafProgress > last, "move $i")
                last = state.leafProgress
            }
        }
    }

    @Test
    fun aTapWhileAPageLandsTurnsTheNextOne() = runComposeUiTest {
        val state = book()
        act(ms = 80) { state.next() }
        act { state.next() }
        settle()
        assertEquals(2, state.spread)
    }
}
