package dev.dimvlachos.lab.fogdemo

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeartArrowTest {
    private val strokes = heartWithArrow()
    private val heart = strokes[0].path
    private val shaft = strokes[1].path

    @Test
    fun aHeartThenAShaftThenTwoHeadStrokesThenTwoFeathers() {
        assertEquals(6, strokes.size)
    }

    @Test
    fun theHeartIsOneClosedStrokeFromTheDipAtTheTop() {
        assertTrue(
            (heart.first() - heart.last()).getDistance() < 0.02f,
            "closed: ${heart.first()} ${heart.last()}",
        )
        val bottom = heart.maxBy { it.y }
        assertTrue(abs(bottom.x - heart.first().x) < 0.03f, "the point sits under the dip")
        assertTrue(heart.first().y < bottom.y - 0.2f, "the dip is at the top")
    }

    @Test
    fun theHeartHasALobeEachSideOfTheDip() {
        val dip = heart.first().x
        assertTrue(heart.any { it.x < dip - 0.15f } && heart.any { it.x > dip + 0.15f })
    }

    @Test
    fun theShaftRunsFromLowerLeftToUpperRightThroughTheHeart() {
        assertTrue(shaft.first().x < shaft.last().x && shaft.first().y > shaft.last().y)
        val left = heart.minOf { it.x }
        val right = heart.maxOf { it.x }
        assertTrue(
            shaft.first().x < left && shaft.last().x > right,
            "it pierces the heart side to side",
        )
    }

    @Test
    fun theHeadIsAtTheTipAndTheFeathersAtTheTail() {
        val tip = shaft.last()
        val tail = shaft.first()
        strokes.subList(2, 4).forEach {
            assertTrue((it.path.last() - tip).getDistance() < 0.02f, "head ends at the tip")
        }
        strokes.subList(4, 6).forEach {
            assertTrue((it.path.first() - tail).getDistance() < 0.12f, "feathers near the tail")
        }
    }

    @Test
    fun everythingStaysOnTheGlassAndTakesAboutFiveSeconds() {
        assertTrue(strokes.flatMap { it.path }.all { it.x in 0.05f..0.95f && it.y in 0.05f..0.95f })
        val seconds = strokes.sumOf { it.duration.inWholeMilliseconds } / 1000f
        assertTrue(seconds in 3.5f..6f, "$seconds s")
    }

    @Test
    fun theSameDrawingEveryTime() {
        assertEquals(heart, heartWithArrow()[0].path)
    }
}
