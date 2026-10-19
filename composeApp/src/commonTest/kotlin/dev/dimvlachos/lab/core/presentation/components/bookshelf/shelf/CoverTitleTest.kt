package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverTitleTest {
    // Every letter is half its size wide, so a line's width is easy to reckon.
    private val measure = { text: String, size: Float -> text.length * size * 0.5f }

    private fun fit(title: String, maxLines: Int = 2) =
        fitCoverTitle(title, maxWidth = 50f, maxLines, size = 10f, minSize = 7f, measure)

    @Test
    fun aTitleThatFitsKeepsItsSizeAndItsLines() {
        val fitted = fit("THE OZARK FIDDLER")

        assertEquals(10f, fitted.size)
        assertEquals(listOf("THE OZARK", "FIDDLER"), fitted.lines)
    }

    @Test
    fun aTitleTooLongForItsLinesIsSetSmallerToKeepEveryWord() {
        val fitted = fit("THE FERRYMAN OF PUGET SOUND")

        assertTrue(fitted.size < 10f && fitted.size >= 7f, "size ${fitted.size}")
        assertEquals("THE FERRYMAN OF PUGET SOUND", fitted.lines.joinToString(" "))
        assertTrue(fitted.lines.size <= 2)
        assertTrue(fitted.lines.all { measure(it, fitted.size) <= 50f }, "${fitted.lines}")
    }

    @Test
    fun aTitleTooLongEvenAtTheSmallestSizeEndsInAnEllipsis() {
        val fitted = fit("A VERY LONG TITLE ABOUT THE GREAT RAILROADS OF THE AMERICAN WEST")

        assertEquals(7f, fitted.size)
        assertEquals(2, fitted.lines.size)
        assertTrue(fitted.lines.last().endsWith("…"), "${fitted.lines}")
        assertTrue(fitted.lines.all { measure(it, fitted.size) <= 50f }, "${fitted.lines}")
    }

    @Test
    fun aWordWiderThanTheCoverIsSetSmallerUntilItFits() {
        val fitted = fit("UNSELFCONSCIOUSNESS", maxLines = 1)

        assertTrue(measure(fitted.lines.single().removeSuffix("…"), fitted.size) <= 50f)
    }
}
