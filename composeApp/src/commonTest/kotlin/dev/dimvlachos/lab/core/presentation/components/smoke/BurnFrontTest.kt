package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BurnFrontTest {
    private val columns = SmokeDimens.Columns

    @Test
    fun aRowSwipedLeftBurnsFromItsRightEdge() {
        val front = BurnFront(fromRight = true)
        front.moveTo(0.25f, now = 1f, seconds = 0.1f)
        // The last quarter of the columns, and only those, have been crossed.
        for (c in 0 until columns) {
            assertEquals(c >= columns * 3 / 4, front.burning(c), "column $c")
        }
        assertEquals(0.75f, front.edge, 1e-6f)
    }

    @Test
    fun aRowSwipedRightBurnsFromItsLeftEdge() {
        val front = BurnFront(fromRight = false)
        front.moveTo(0.5f, now = 1f, seconds = 0.1f)
        assertTrue(front.burning(0))
        assertFalse(front.burning(columns - 1))
        assertEquals(BurnFront.Uncrossed, front.crossing(columns - 1))
        assertEquals(0.5f, front.edge, 1e-6f)
    }

    @Test
    fun aFastFrontStillCrossesItsColumnsOneAfterAnother() {
        val front = BurnFront(fromRight = true)
        front.moveTo(1f, now = 2f, seconds = 0.5f)
        // The edge it burns from goes first: crossing times rise right to left, within the step.
        for (c in columns - 1 downTo 1) {
            assertTrue(front.crossing(c) <= front.crossing(c - 1), "column $c before ${c - 1}")
        }
        assertTrue(front.crossing(columns - 1) >= 1.5f)
        assertTrue(front.crossing(0) <= 2f)
        assertTrue(front.done)
    }

    @Test
    fun aColumnAgesASecondASecondWhileItBurns() {
        val front = BurnFront(fromRight = false)
        front.moveTo(0.5f, now = 1f, seconds = 0f)
        assertEquals(0f, front.ageOf(0, 1f))
        assertEquals(2.5f, front.ageOf(0, 3.5f))
        assertEquals(Float.NEGATIVE_INFINITY, front.ageOf(columns - 1, 3.5f), "whole")
    }

    @Test
    fun theFrontFollowsTheFingerBackAndWindsTheColumnsItLeavesBack() {
        val front = BurnFront(fromRight = true)
        front.moveTo(0.5f, now = 1f, seconds = 0f)
        front.moveTo(0.2f, now = 2f, seconds = 0f)
        assertEquals(0.2f, front.burnt)
        val left = columns / 2 + 2 // well inside the stretch the front drew back off
        val kept = columns - 2 // still under the front
        assertFalse(front.burning(left))
        assertTrue(front.burning(kept))
        // A second old as the front left it, it is wound back, faster than it rose, to nothing.
        assertEquals(1f, front.ageOf(left, 2f), 1e-5f)
        val rate = maxOf(SmokeDimens.RewindRate, 1f / SmokeDimens.RewindMost)
        assertEquals(1f - rate * 0.1f, front.ageOf(left, 2.1f), 1e-5f)
        assertEquals(0f, front.ageOf(left, 2f + 1f / rate + 0.01f))
        assertEquals(1f, front.woundBack(left, 2f + 1f / rate + 0.01f))
        // The one under the front burns on.
        assertEquals(2f, front.ageOf(kept, 3f), 1e-5f)
    }

    @Test
    fun smokeLongGoneLandsWithinRewindMost() {
        val front = BurnFront(fromRight = true)
        front.moveTo(0.5f, now = 1f, seconds = 0f)
        front.moveTo(0f, now = 30f, seconds = 0f)
        assertFalse(front.wholeAt(30f + SmokeDimens.RewindMost - 0.01f))
        assertTrue(front.wholeAt(30f + SmokeDimens.RewindMost + 0.001f))
    }

    @Test
    fun aColumnBurntAgainCarriesOnFromWhereItsSmokeIs() {
        val front = BurnFront(fromRight = false)
        front.moveTo(0.5f, now = 1f, seconds = 0f)
        front.moveTo(0f, now = 2f, seconds = 0f)
        val c = 3
        val partway = front.ageOf(c, 2.2f)
        assertTrue(partway > 0f && partway < 1f, "wound partway back: $partway")
        front.moveTo(0.5f, now = 2.2f, seconds = 0f)
        assertTrue(front.burning(c))
        assertEquals(partway, front.ageOf(c, 2.2f), 1e-5f)
        assertEquals(partway + 0.5f, front.ageOf(c, 2.7f), 1e-5f)
    }

    @Test
    fun eachColumnKeepsWhereTheRowWasUntilItsSmokeHasLanded() {
        val front = BurnFront(fromRight = false)
        front.moveTo(0.25f, now = 1f, seconds = 0.1f, shownTop = 10f, shownBottom = 50f)
        // The list scrolls the row up 30 px, all of it showing, and the front burns on.
        front.moveTo(0.5f, now = 2f, seconds = 0.1f, movedY = -30f)
        val first = 0
        val later = columns * 3 / 8
        assertEquals(0f, front.movedYOf(first))
        assertEquals(-30f, front.movedYOf(later))
        assertEquals(10f, front.shownTopOf(first))
        // Burnt on, nothing still burning is stamped anew.
        front.moveTo(0.75f, now = 3f, seconds = 0.1f, movedY = -90f)
        assertEquals(-30f, front.movedYOf(later))
        // Drawn back, and burnt again mid-flight: the smoke carries on from the same place.
        front.moveTo(0.3f, now = 3.1f, seconds = 0f)
        front.moveTo(0.75f, now = 3.15f, seconds = 0f, movedY = -120f)
        assertEquals(-30f, front.movedYOf(later))
        // Drawn back and landed, and burnt again: it leaves from where the row is now.
        front.moveTo(0.3f, now = 4f, seconds = 0f)
        front.moveTo(0.75f, now = 6f, seconds = 0f, movedY = -150f)
        assertEquals(-150f, front.movedYOf(later))
    }

    @Test
    fun theRowIsCutAtTheFrontAndComesBackBehindItAsItsSmokeLands() {
        val front = BurnFront(fromRight = true)
        front.moveTo(0.6f, now = 1f, seconds = 0.016f)
        assertEquals(0.6f, front.shownFrom(1f), 1e-6f, "on, right under the front")
        front.moveTo(0.2f, now = 2f, seconds = 0.016f)
        // Just drawn back, the row hasn't come back yet; it does as the smoke lands.
        assertTrue(front.shownFrom(2f) > 0.55f)
        var last = front.shownFrom(2f)
        var t = 2f
        while (t < 2f + SmokeDimens.RewindMost + 0.1f) {
            t += 0.01f
            val shown = front.shownFrom(t)
            assertTrue(shown <= last + 1e-6f, "the row only comes back: $shown after $last")
            last = shown
        }
        assertEquals(0.2f, front.shownFrom(t), 1e-6f, "landed, it is cut at the front again")
    }

    // Moves [front] back and forth as a finger would, a frame at a time, from [seed]; returns
    // when it stopped.
    private fun scrub(front: BurnFront, seed: Int, frames: Int): Float {
        val random = Random(seed)
        var now = 0f
        var at = 0f
        var heading = 1f
        repeat(frames) {
            now += 1f / 60f
            if (random.nextFloat() < 0.04f) heading = -heading
            at = (at + heading * random.nextFloat() * 0.03f).coerceIn(0f, 0.9f)
            front.moveTo(at, now, 1f / 60f)
        }
        return now
    }

    @Test
    fun scrubbingBackAndForthIsTheSameEveryTimeAndStaysInBounds() {
        val one = BurnFront(fromRight = true)
        val two = BurnFront(fromRight = true)
        val now = scrub(one, seed = 7, frames = 6_000)
        scrub(two, seed = 7, frames = 6_000)
        for (c in 0 until columns) {
            assertEquals(one.ageOf(c, now), two.ageOf(c, now), "column $c")
            assertEquals(one.burning(c), two.burning(c))
            val age = one.ageOf(c, now)
            assertTrue(age == Float.NEGATIVE_INFINITY || age in 0f..now, "column $c is $age old")
        }
        // Drawn back to the edge, every column lands, however long it was scrubbed.
        one.moveTo(0f, now + 1f / 60f, 1f / 60f)
        assertTrue(one.wholeAt(now + 1f / 60f + SmokeDimens.RewindMost + 0.01f))
    }

    @Test
    fun scrubbingNeverMakesTheSmokeJump() {
        val front = BurnFront(fromRight = false)
        val random = Random(3)
        val dt = 1f / 60f
        // Within a frame a column turns at most once, so its age moves by at most the fastest
        // winding back plus a second a second, over the frame.
        val most = dt * (1f + SmokeMotion.longestLife / SmokeDimens.RewindMost) + 1e-4f
        val before = FloatArray(columns)
        var now = 0f
        var at = 0f
        var heading = 1f
        repeat(3_000) {
            for (c in 0 until columns) before[c] = front.ageOf(c, now)
            now += dt
            if (random.nextFloat() < 0.05f) heading = -heading
            at = (at + heading * random.nextFloat() * 0.04f).coerceIn(0f, 1f)
            front.moveTo(at, now, dt)
            for (c in 0 until columns) {
                val was = before[c]
                val age = front.ageOf(c, now)
                // A whole column just crossed starts from nothing, and smoke long gone may be
                // taken back to the longest life: neither shows.
                if (was == Float.NEGATIVE_INFINITY || was >= SmokeMotion.longestLife) continue
                assertTrue(abs(age - was) <= most, "column $c went from $was to $age")
            }
        }
    }
}
