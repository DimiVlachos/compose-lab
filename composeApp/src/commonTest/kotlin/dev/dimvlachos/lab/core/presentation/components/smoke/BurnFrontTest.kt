package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BurnFrontTest {
    @Test
    fun aRowSwipedLeftBurnsFromItsRightEdge() {
        val front = BurnFront(fromRight = true)
        front.advanceTo(0.25f, now = 1f, seconds = 0.1f)
        val columns = SmokeDimens.Columns
        // The last quarter of the columns, and only those, have been crossed.
        for (c in 0 until columns) {
            assertEquals(
                c >= columns * 3 / 4,
                front.crossing(c) != BurnFront.Uncrossed,
                "column $c",
            )
        }
        assertEquals(0.75f, front.edge, 1e-6f)
    }

    @Test
    fun aRowSwipedRightBurnsFromItsLeftEdge() {
        val front = BurnFront(fromRight = false)
        front.advanceTo(0.5f, now = 1f, seconds = 0.1f)
        assertTrue(front.crossing(0) != BurnFront.Uncrossed)
        assertEquals(BurnFront.Uncrossed, front.crossing(SmokeDimens.Columns - 1))
        assertEquals(0.5f, front.edge, 1e-6f)
    }

    @Test
    fun aFastFrontStillCrossesItsColumnsOneAfterAnother() {
        val front = BurnFront(fromRight = true)
        front.advanceTo(1f, now = 2f, seconds = 0.5f)
        val columns = SmokeDimens.Columns
        // The edge it burns from goes first: crossing times rise right to left, within the step.
        for (c in columns - 1 downTo 1) {
            assertTrue(front.crossing(c) <= front.crossing(c - 1), "column $c before ${c - 1}")
        }
        assertTrue(front.crossing(columns - 1) >= 1.5f)
        assertTrue(front.crossing(0) <= 2f)
        assertTrue(front.done)
    }

    @Test
    fun theFrontNeverMovesBackAndKeepsWhenItCrossed() {
        val front = BurnFront(fromRight = true)
        front.advanceTo(0.5f, now = 1f, seconds = 0.1f)
        val stamped = front.crossing(SmokeDimens.Columns - 1)
        front.advanceTo(0.2f, now = 2f, seconds = 0.1f)
        assertEquals(0.5f, front.burnt)
        front.advanceTo(0.7f, now = 3f, seconds = 0.1f)
        assertEquals(stamped, front.crossing(SmokeDimens.Columns - 1))
        assertEquals(3f, front.lastCross)
    }
}
