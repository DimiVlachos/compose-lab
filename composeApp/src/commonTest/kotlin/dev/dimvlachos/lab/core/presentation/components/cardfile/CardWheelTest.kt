package dev.dimvlachos.lab.core.presentation.components.cardfile

import kotlin.math.abs
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
    fun aFlingCarriesOnItsWay() {
        val still = CardWheel.restingCard(4f, 0f, 30)
        assertTrue(CardWheel.restingCard(4f, 20f, 30) > still + 2)
        assertTrue(CardWheel.restingCard(4f, -20f, 30) < still - 2)
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
        val over = CardWheel.banded(32f, 30) - 29f
        assertTrue(over > 0f && over < 3f, "$over")
        val under = CardWheel.banded(-3f, 30)
        assertTrue(under < 0f && under > -3f, "$under")
    }

    @Test
    fun aCardIsCountedAsItCrossesTheTop() {
        assertEquals(0, CardWheel.cardsCrossed(4f, 4.4f))
        assertEquals(1, CardWheel.cardsCrossed(4.4f, 4.6f))
        assertEquals(3, CardWheel.cardsCrossed(4f, 7f))
        assertEquals(3, CardWheel.cardsCrossed(7f, 4f))
    }

    @Test
    fun theFrontCardStandsUpAndTheOthersLeanAway() {
        assertEquals(0f, CardWheel.cardAngle(5, 5f))
        assertTrue(CardWheel.cardAngle(6, 5f) > 0f, "cards behind lean back")
        assertTrue(CardWheel.cardAngle(4, 5f) < 0f, "flipped cards fall forward")
        assertTrue(CardWheel.cardAngle(8, 5f) >= CardWheel.cardAngle(6, 5f))
        assertTrue(abs(CardWheel.cardAngle(0, 5f)) <= CardFileDimens.FlippedAngle)
        assertTrue(CardWheel.cardAngle(20, 5f) <= CardFileDimens.BehindAngle)
    }

    @Test
    fun flippedCardsFallPastLevelOntoAPileThatLiesFurtherOverDownIt() {
        assertTrue(CardWheel.cardAngle(4, 5f) < -90f, "a flipped card lies past level")
        assertTrue(CardWheel.cardAngle(3, 5f) < CardWheel.cardAngle(4, 5f))
        assertTrue(CardWheel.cardAngle(4, 4.5f) > -90f, "halfway over, it hasn't fallen yet")
        assertEquals(-CardFileDimens.FlippedAngle, CardWheel.cardAngle(0, 20f))
    }

    @Test
    fun onlyTheCardsAroundTheFrontAreComposed() {
        assertEquals(1..9, CardWheel.window(5.2f, 30))
        assertEquals(0..4, CardWheel.window(0f, 30))
        assertEquals(25..29, CardWheel.window(29f, 30))
        assertTrue(CardWheel.window(0f, 0).isEmpty())
    }

    @Test
    fun ticksAreAtLeastFortyFiveMillisecondsApart() {
        val throttle = TickThrottle(CardFileDimens.TickGapMs)
        assertTrue(throttle.shouldTick(1_000))
        assertTrue(!throttle.shouldTick(1_020))
        assertTrue(!throttle.shouldTick(1_044))
        assertTrue(throttle.shouldTick(1_045))
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
