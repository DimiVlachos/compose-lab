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
}
