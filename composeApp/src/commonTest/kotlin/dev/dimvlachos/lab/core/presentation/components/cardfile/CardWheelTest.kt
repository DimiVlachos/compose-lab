package dev.dimvlachos.lab.core.presentation.components.cardfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardWheelTest {
    @Test
    fun aSlowReleaseRestsOnTheNearestCard() {
        assertEquals(4, CardWheel.restingCard(4.3f, 0f, count = 30))
        assertEquals(5, CardWheel.restingCard(4.6f, 0f, count = 30))
    }

    @Test
    fun aFlingCarriesOnForThreeTenthsOfASecond() {
        assertEquals(10, CardWheel.restingCard(4f, 20f, 30))
        assertEquals(-2 + 4, CardWheel.restingCard(4f, -7f, 30))
    }

    @Test
    fun aReleaseSlowerThanOneCardAndAFifthASecondRestsOnTheNearestCard() {
        assertEquals(4, CardWheel.restingCard(4.3f, 1.1f, 30))
        assertEquals(5, CardWheel.restingCard(4.3f, 1.3f, 30))
    }

    @Test
    fun aHugeFlingStopsOnTheLastCard() {
        assertEquals(29, CardWheel.restingCard(20f, 500f, 30))
        assertEquals(0, CardWheel.restingCard(3f, -500f, 30))
        assertEquals(0, CardWheel.restingCard(0f, 10f, 1))
        assertEquals(0, CardWheel.restingCard(0f, 10f, 0))
    }

    @Test
    fun pastTheEndsTheWheelGivesLessThanTheDrag() {
        assertEquals(10f, CardWheel.banded(10f, 30))
        // The prototype's soft band: an overshoot o goes o / (1 + 1.5 o).
        assertEquals(29f + 1f / 2.5f, CardWheel.banded(30f, 30), 1e-4f)
        assertEquals(-3f / 5.5f, CardWheel.banded(-3f, 30), 1e-4f)
    }

    @Test
    fun aCardIsCountedAsItCrossesTheTop() {
        assertEquals(0, CardWheel.cardsCrossed(4f, 4.4f))
        assertEquals(1, CardWheel.cardsCrossed(4.4f, 4.6f))
        assertEquals(3, CardWheel.cardsCrossed(4f, 7f))
        assertEquals(3, CardWheel.cardsCrossed(7f, 4f))
    }

    @Test
    fun ticksAreAtLeastAFrameApart() {
        val throttle = TickThrottle(CardFileDimens.TickGapMs)
        assertTrue(throttle.shouldTick(1_000))
        assertTrue(!throttle.shouldTick(1_010))
        assertTrue(!throttle.shouldTick(1_015))
        assertTrue(throttle.shouldTick(1_016))
    }

    @Test
    fun unbandingUndoesTheBand() {
        for (raw in listOf(-6f, -1f, -0.2f, 0f, 12f, 29f, 29.4f, 31f, 35f)) {
            assertEquals(raw, CardWheel.unbanded(CardWheel.banded(raw, 30), 30), 1e-2f)
        }
    }

    @Test
    fun unbandingAWheelPastTheBandsReachSaturates() {
        // A wheel that springs further past an end than any drag could take it, at or beyond the
        // band's asymptote, unbands to a raw drag a few cards out, not a thousand.
        val reach = 1f / CardFileDimens.RubberStiffness
        for (over in listOf(reach - 0.01f, reach, reach + 3f, 50f)) {
            val past = CardWheel.unbanded(29f + over, 30) - 29f
            assertTrue(past > 0f && past <= 30f, "$over past the end unbands to $past")
            val before = CardWheel.unbanded(-over, 30)
            assertTrue(before < 0f && before >= -30f, "$over before the start unbands to $before")
        }
    }
}
