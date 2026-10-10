package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PopUpBookStateTest {
    private val pi = PI.toFloat()

    // Spreads 2 and 3 (1-based) have tabs, at these points on the screen.
    private val tabs = mapOf(1 to Offset(330f, 420f), 2 to Offset(330f, 420f))

    private val state =
        PopUpBookState(spreadCount = 3).apply {
            layout =
                BookLayout(
                    camera = BookCamera(372f, 600f),
                    gutterY = 300f,
                    span = 400f,
                    touchSlop = 6f,
                    tabReach = 34f,
                    tabPoint = { tabs[it] },
                    tabTravel = { if (it in tabs) 58f else 0f },
                )
        }

    private fun step(times: Int = 1) = repeat(times) { state.advance(1f / 120f) }

    private fun settle() = step(600)

    private fun openTo(spread: Int) {
        repeat(spread) { state.next() }
        settle()
    }

    private fun tabPoint(spread: Int): Offset = tabs.getValue(spread - 1)

    @Test
    fun nextTurnsTheCoverOpen() {
        state.next()
        settle()
        assertEquals(1, state.spread)
        assertEquals(pi, state.angles[0], 1e-3f)
    }

    @Test
    fun previousTurnsItBack() {
        openTo(2)
        state.previous()
        settle()
        assertEquals(1, state.spread)
    }

    @Test
    fun nextAtTheEndIsANoOp() {
        openTo(3)
        state.next()
        assertFalse(state.awake)
        assertEquals(3, state.spread)
    }

    @Test
    fun previousOnTheCoverIsANoOp() {
        state.previous()
        assertFalse(state.awake)
        assertEquals(0, state.spread)
    }

    @Test
    fun aDragPastHalfwayCommits() {
        state.dragStart(Offset(100f, 400f))
        state.dragTo(Offset(100f, 400f - 220f))
        state.dragEnd(Velocity.Zero)
        settle()
        assertEquals(1, state.spread)
    }

    @Test
    fun aHeldLeafFollowsTheFinger() {
        state.dragStart(Offset(100f, 400f))
        state.dragTo(Offset(100f, 300f))
        step(30)
        assertEquals(pi / 4f, state.angles[0], 1e-3f)
    }

    @Test
    fun aShortDragFallsBack() {
        state.dragStart(Offset(100f, 400f))
        state.dragTo(Offset(100f, 330f))
        state.dragEnd(Velocity.Zero)
        settle()
        assertEquals(0, state.spread)
        assertEquals(0f, state.angles[0], 1e-3f)
    }

    @Test
    fun aFlickCommits() {
        state.dragStart(Offset(100f, 400f))
        state.dragTo(Offset(100f, 330f))
        state.dragEnd(Velocity(0f, -3000f))
        settle()
        assertEquals(1, state.spread)
    }

    @Test
    fun aTapBelowTheGutterTurnsForward() {
        state.dragStart(Offset(100f, 400f))
        state.dragEnd(Velocity.Zero)
        settle()
        assertEquals(1, state.spread)
    }

    @Test
    fun aTapAboveTheGutterTurnsBack() {
        openTo(2)
        state.dragStart(Offset(100f, 200f))
        state.dragEnd(Velocity.Zero)
        settle()
        assertEquals(1, state.spread)
    }

    @Test
    fun aDragWithNothingToTurnIsRefused() {
        assertFalse(state.dragStart(Offset(100f, 200f)))
    }

    @Test
    fun leavesNeverCrossWhenTurnedQuickly() {
        state.next()
        state.next()
        repeat(600) {
            step()
            assertTrue(state.angles[0] >= state.angles[1] - 1e-4f)
        }
        state.previous()
        state.previous()
        repeat(600) {
            step()
            assertTrue(state.angles[0] >= state.angles[1] - 1e-4f)
        }
    }

    @Test
    fun grabbingALandingLeafKeepsItsAngle() {
        state.next()
        step(10)
        val angle = state.angles[0]
        state.dragStart(Offset(100f, 250f))
        step()
        assertEquals(angle, state.angles[0], 1e-4f)
    }

    @Test
    fun aTabIsDeadUntilItsSpreadIsOpen() {
        openTo(1)
        state.next()
        step(5)
        assertFalse(state.tabStart(tabPoint(2)))
        settle()
        assertTrue(state.tabStart(tabPoint(2)))
    }

    @Test
    fun aSpreadWithoutATabHasNone() {
        openTo(1)
        assertFalse(state.tabStart(tabPoint(2)))
    }

    @Test
    fun aPulledTabFollowsTheFingerUpToItsTravel() {
        openTo(2)
        state.tabStart(tabPoint(2))
        state.tabTo(tabPoint(2) + Offset(20f, 0f))
        assertEquals(20f, state.tabTravel[1], 1e-3f)
        state.tabTo(tabPoint(2) + Offset(200f, 0f))
        assertEquals(58f, state.tabTravel[1], 1e-3f)
    }

    @Test
    fun aPulledTabSpringsBackIn() {
        openTo(2)
        state.tabStart(tabPoint(2))
        state.tabTo(tabPoint(2) + Offset(80f, 0f))
        state.tabEnd()
        settle()
        assertEquals(0f, state.tabTravel[1])
        assertFalse(state.awake)
    }

    @Test
    fun sailsSpinOnlyOnAnOutwardPullAndCoastDown() {
        openTo(3)
        state.tabStart(tabPoint(3))
        state.tabTo(tabPoint(3) + Offset(80f, 0f))
        step()
        val spun = state.sailAngle[2]
        assertTrue(spun > 0f)
        state.tabTo(tabPoint(3))
        step()
        state.tabEnd()
        var last = state.sailAngle[2]
        // Sails coast for a long while, as real ones do.
        repeat(2400) {
            step()
            assertTrue(state.sailAngle[2] >= last)
            last = state.sailAngle[2]
        }
        assertFalse(state.awake)
    }

    @Test
    fun theFrameCountsAdvances() {
        val before = state.frame
        state.next()
        step()
        assertTrue(state.frame > before)
    }
}
