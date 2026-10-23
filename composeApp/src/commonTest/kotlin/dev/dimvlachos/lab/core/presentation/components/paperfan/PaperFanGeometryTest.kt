package dev.dimvlachos.lab.core.presentation.components.paperfan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaperFanGeometryTest {
    @Test
    fun theFanSpansTheCardWithinItsLimits() {
        assertEquals(172f, fanRadius(382f))
        assertEquals(167f, fanRadius(358f))
        assertEquals(110f, fanRadius(200f))
        assertEquals(204f, fanOpenHeight(172f))
    }

    // About Georgia's average advance at 13 px.
    private val measure: (String) -> Float = { it.length * 6.2f }

    @Test
    fun linesAreShortNearTheTopOfTheFanAndFitTheHalfRing() {
        val r = 172f
        val text =
            "Send anything back within 60 days, worn or not. Print a free label from your " +
                "order page or drop it at any store."
        val lines = assertNotNull(fitLines(measure, text, 13f, r, r * .3f))
        assertEquals(text, lines.joinToString(" ") { it.text })
        // Baselines step down by the line height.
        for (k in 1 until lines.size) assertEquals(13f * 1.3f, lines[k].y - lines[k - 1].y, 1e-3f)
        // Every line sits above the inner ring, inside the outer one.
        for (l in lines) {
            assertTrue(l.y < -r * .3f - 6f + 13f * 1.3f, "${l.y}")
            assertTrue(l.y > -(r - 17f), "${l.y}")
        }
    }

    @Test
    fun anAnswerTooLongForTheFanDoesNotFit() {
        assertNull(fitLines(measure, List(80) { "word" }.joinToString(" "), 13f, 110f, 33f))
    }
}
