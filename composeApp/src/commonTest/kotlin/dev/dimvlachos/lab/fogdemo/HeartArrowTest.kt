package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeartArrowTest {
    private val strokes = heartWithArrow()
    private val heart = strokes[0].path
    private val shaftIn = strokes[1].path
    private val shaftOut = strokes[2].path

    private fun distanceToHeart(point: Offset) = heart.minOf { (it - point).getDistance() }

    // Even-odd ray cast against the heart's outline.
    private fun insideHeart(point: Offset): Boolean {
        var inside = false
        for (i in heart.indices) {
            val a = heart[i]
            val b = heart[(i + 1) % heart.size]
            if ((a.y > point.y) != (b.y > point.y)) {
                val x = a.x + (point.y - a.y) / (b.y - a.y) * (b.x - a.x)
                if (x > point.x) inside = !inside
            }
        }
        return inside
    }

    @Test
    fun aHeartThenTheShaftInTwoPiecesThenTwoHeadStrokesThenTwoFeathers() {
        assertEquals(7, strokes.size)
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
    fun theArrowPiercesTheHeartGoingInOneSideAndOutTheOther() {
        // Lower left in, upper right out; nothing of the shaft drawn inside the heart.
        assertTrue(shaftIn.first().x < shaftOut.last().x && shaftIn.first().y > shaftOut.last().y)
        assertTrue(distanceToHeart(shaftIn.last()) < 0.015f, "the shaft goes in at the outline")
        assertTrue(distanceToHeart(shaftOut.first()) < 0.015f, "and comes out at the outline")
        val drawn = shaftIn.dropLast(2) + shaftOut.drop(2)
        assertTrue(drawn.none { insideHeart(it) }, "no shaft inside the heart")
        assertTrue(
            !insideHeart(shaftIn.first()) && !insideHeart(shaftOut.last()),
            "tail and tip outside it",
        )
    }

    @Test
    fun theHeadIsAtTheTipAndTheFeathersAtTheTail() {
        val tip = shaftOut.last()
        val tail = shaftIn.first()
        strokes.subList(3, 5).forEach {
            assertTrue((it.path.last() - tip).getDistance() < 0.02f, "head ends at the tip")
        }
        strokes.subList(5, 7).forEach {
            assertTrue((it.path.first() - tail).getDistance() < 0.15f, "feathers near the tail")
        }
    }

    @Test
    fun theFeathersAreBigEnoughToSee() {
        strokes.subList(5, 7).forEach { feather ->
            val span = (feather.path.first() - feather.path.last()).getDistance()
            assertTrue(span >= 0.1f, "each feather spans a tenth of the frame: $span")
        }
    }

    @Test
    fun everythingStaysOnTheGlassAndTakesAboutFiveSeconds() {
        assertTrue(strokes.flatMap { it.path }.all { it.x in 0.05f..0.95f && it.y in 0.05f..0.95f })
        val seconds = strokes.sumOf { it.duration.inWholeMilliseconds } / 1000f
        assertTrue(seconds in 3.5f..6.5f, "$seconds s")
    }

    @Test
    fun theSameDrawingEveryTime() {
        assertEquals(heart, heartWithArrow()[0].path)
    }
}
