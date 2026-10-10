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
    fun aLeafEasesUpSwingsOverAndSettlesWithoutASnap() {
        state.next()
        // A hand lifts a page gently: barely moving after 0.15 s...
        step(18)
        assertTrue(state.angles[0] < pi * 25f / 180f, "${state.angles[0] * 180f / pi}° at 0.15 s")
        // ...still well short of lying flat 0.4 s in...
        step(30)
        assertTrue(state.angles[0] < pi * 150f / 180f, "${state.angles[0] * 180f / pi}° at 0.4 s")
        // ...and down within about a second and a half.
        step(132)
        assertEquals(1, state.spread)
        settle()
        assertFalse(state.awake)
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
    fun aCancelledTouchTurnsNothing() {
        state.dragStart(Offset(100f, 400f))
        state.dragCancel()
        settle()
        assertEquals(0, state.spread)
        assertFalse(state.awake)
    }

    @Test
    fun aCancelledDragLetsTheLeafFallWhereItWasHeaded() {
        state.dragStart(Offset(100f, 400f))
        state.dragTo(Offset(100f, 400f - 260f))
        state.dragCancel()
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
    fun aHeldLeafPushesTheLeavesAboveItWithoutPassingThem() {
        // Two leaves in flight, then the third dragged quickly far over them.
        state.next()
        state.next()
        step(8)
        state.dragStart(Offset(100f, 400f))
        state.dragTo(Offset(100f, 80f))
        repeat(30) {
            step()
            assertTrue(state.angles[0] >= state.angles[1] - 1e-4f, "${state.angles.toList()}")
            assertTrue(state.angles[1] >= state.angles[2] - 1e-4f, "${state.angles.toList()}")
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
    fun aBookRestoredOnARestlessSpreadKeepsMoving() {
        // Made again on the boat's spread, as after a rotation: the boat bobs on at once.
        val restored = PopUpBookState(spreadCount = 3, opened = 2)
        restored.restless = booleanArrayOf(false, true, false)
        assertTrue(restored.awake)
    }

    @Test
    fun backTappedWhileAPageIsHeldForwardStillSettles() {
        openTo(2)
        // A thumb holds leaf 2 forward; the other taps Back, shutting leaf 1; the page is let go
        // past halfway.
        state.dragStart(Offset(100f, 400f))
        state.previous()
        state.dragTo(Offset(100f, 400f - 260f))
        state.dragEnd(Velocity.Zero)
        settle()
        assertFalse(state.awake, "angles ${state.angles.toList()}")
        assertEquals(state.destination, state.spread)
    }

    @Test
    fun nextTappedWhileAPageIsHeldBackStillSettles() {
        openTo(2)
        // Leaf 1 held back, Next opens leaf 2, and leaf 1 is let fall back.
        state.dragStart(Offset(100f, 200f))
        state.next()
        state.dragTo(Offset(100f, 200f + 30f))
        state.dragEnd(Velocity.Zero)
        settle()
        assertFalse(state.awake, "angles ${state.angles.toList()}")
        assertEquals(state.destination, state.spread)
    }

    @Test
    fun aSecondFingerCannotTakeAHeldPage() {
        assertTrue(state.dragStart(Offset(100f, 400f)))
        assertFalse(state.dragStart(Offset(120f, 420f)))
    }

    @Test
    fun theFrameCountsAdvances() {
        val before = state.frame
        state.next()
        step()
        assertTrue(state.frame > before)
    }
}
