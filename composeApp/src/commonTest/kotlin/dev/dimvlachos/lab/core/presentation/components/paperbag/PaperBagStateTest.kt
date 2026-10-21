package dev.dimvlachos.lab.core.presentation.components.paperbag

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaperBagStateTest {
    private fun run(state: PaperBagState, seconds: Float) {
        repeat((seconds * 60).toInt()) { state.tick(1 / 60f) }
    }

    @Test
    fun aLandingSquashesTheBagAndItSettles() {
        val bag = PaperBagState(Random(1))
        bag.landed(1)
        run(bag, .05f)
        assertTrue(bag.pose(emptyList()).sq > .05f, "squashed")
        run(bag, 2f)
        assertFalse(bag.moving)
        assertEquals(0f, bag.pose(emptyList()).sq, .001f)
    }

    @Test
    fun theNumberIsWrittenInOverAboutAQuarterSecond() {
        val bag = PaperBagState(Random(1))
        bag.landed(1)
        assertEquals(0f, bag.pose(emptyList()).numP)
        run(bag, .3f)
        assertEquals(1f, bag.pose(emptyList()).numP)
        assertEquals(0, bag.pose(emptyList()).prevCount)
        bag.landed(2)
        assertEquals(1, bag.pose(emptyList()).prevCount)
    }

    @Test
    fun itBulgesAsItFills() {
        val bag = PaperBagState(Random(1))
        for (n in 1..7) bag.landed(n)
        run(bag, 2f)
        assertEquals(1f, bag.pose(emptyList()).bulge, .01f)
    }

    @Test
    fun itOpensAndClosesWithTheTip() {
        val bag = PaperBagState(Random(1))
        bag.openTo(true)
        run(bag, 1.2f)
        assertEquals(1f, bag.open)
        assertEquals(1f, bag.pose(emptyList()).tip)
        bag.openTo(false)
        run(bag, 1.2f)
        assertEquals(0f, bag.open)
    }

    @Test
    fun aLiftRisesOverItsTimeAndEndsWhenLifted() {
        val bag = PaperBagState(Random(1))
        bag.landed(1)
        bag.lift(4)
        run(bag, .31f)
        assertTrue(bag.rise(4) in .4f..1f)
        run(bag, .4f)
        assertEquals(1.3f, bag.rise(4), .001f)
        bag.lifted(4, 0)
        assertEquals(0f, bag.rise(4))
    }

    @Test
    fun reducedMotionOpensAtOnceAndNeverSprings() {
        val bag = PaperBagState(Random(1))
        bag.reducedMotion = true
        bag.landed(1)
        bag.openTo(true)
        assertEquals(1f, bag.open)
        run(bag, .02f)
        assertEquals(0f, bag.pose(emptyList()).sq)
    }
}
