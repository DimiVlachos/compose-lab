package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PageTurnStateTest {
    // A drag of DragSpan x this is a full turn: 620 px.
    private val bookWidth = 1000f

    private fun ComposeUiTest.book(spreads: Int = 4, initialSpread: Int = 0): PageTurnState {
        mainClock.autoAdvance = false
        lateinit var state: PageTurnState
        setContent { state = rememberPageTurnState(spreads, initialSpread) }
        state.bookWidthPx = bookWidth
        return state
    }

    private fun ComposeUiTest.act(ms: Long = 16, block: () -> Unit) {
        runOnUiThread(block)
        mainClock.advanceTimeBy(ms)
    }

    private fun ComposeUiTest.settle() = mainClock.advanceTimeBy(3_000)

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
        assertFalse(state.dragStart(dx = 10f))
        act { state.next() }
        settle()
        act { state.next() }
        settle()
        assertEquals(1, state.spread)
        assertFalse(state.dragStart(dx = -10f))
    }

    @Test
    fun aDragTurnsTheLeafUnderTheFinger() = runComposeUiTest {
        val state = book()
        act {
            assertTrue(state.dragStart(dx = -10f))
            state.dragBy(-310f)
        }
        assertEquals(0, state.pair?.leaf)
        assertEquals(0.5f, state.leafProgress, 0.001f)
        assertEquals(1f, state.bendDirection)
    }

    @Test
    fun aBackwardDragTurnsThePreviousLeafBack() = runComposeUiTest {
        val state = book(initialSpread = 2)
        act {
            state.dragStart(dx = 10f)
            state.dragBy(155f)
        }
        assertEquals(1, state.pair?.leaf)
        assertEquals(0.75f, state.leafProgress, 0.001f)
        act { state.dragEnd(velocityPxPerSecond = 0f) }
        settle()
        assertEquals(2, state.spread)
    }

    @Test
    fun aShortSlowDragFallsBack() = runComposeUiTest {
        val state = book()
        act {
            state.dragStart(dx = -10f)
            state.dragBy(-200f)
        }
        act { state.dragEnd(velocityPxPerSecond = -100f) }
        assertIs<TurnPhase.Settling>(state.phase)
        settle()
        assertEquals(0, state.spread)
        assertEquals(TurnPhase.Idle, state.phase)
        assertEquals(-1f, state.bendDirection, 0.001f)
    }

    @Test
    fun aDragPastTheThresholdFinishes() = runComposeUiTest {
        val state = book()
        act {
            state.dragStart(dx = -10f)
            state.dragBy(-300f)
        }
        act { state.dragEnd(velocityPxPerSecond = 0f) }
        settle()
        assertEquals(1, state.spread)
    }

    @Test
    fun aShortFlickFinishesTheTurn() = runComposeUiTest {
        val state = book()
        act {
            state.dragStart(dx = -10f)
            state.dragBy(-100f)
        }
        act { state.dragEnd(velocityPxPerSecond = -3_000f) }
        settle()
        assertEquals(1, state.spread)
    }

    @Test
    fun reversingADragFlipsTheBowAndFallsBack() = runComposeUiTest {
        val state = book()
        act {
            state.dragStart(dx = -10f)
            state.dragBy(-300f)
        }
        assertEquals(1f, state.bendDirection)
        repeat(4) { act { state.dragBy(40f) } }
        assertEquals(-1f, state.bendDirection, 0.001f)
        act { state.dragEnd(velocityPxPerSecond = 0f) }
        settle()
        assertEquals(0, state.spread)
    }

    @Test
    fun aPageCaughtWhileLandingFollowsTheFinger() = runComposeUiTest {
        val state = book()
        act(ms = 120) { state.next() }
        val caughtAt = state.leafProgress
        assertTrue(caughtAt in 0.05f..0.95f, "caught at $caughtAt")
        act {
            // A rightwards drag grabs the forward turn in flight and pulls it back.
            assertTrue(state.dragStart(dx = 10f))
            state.dragBy(620f)
        }
        val caught = assertIs<TurnPhase.Dragging>(state.phase)
        assertTrue(caught.pair.forward)
        assertEquals(0f, state.leafProgress, 0.001f)
        act { state.dragEnd(velocityPxPerSecond = 0f) }
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
    fun aTapWhileAPageLandsTurnsTheNextOne() = runComposeUiTest {
        val state = book()
        act(ms = 80) { state.next() }
        act { state.next() }
        settle()
        assertEquals(2, state.spread)
    }
}
