package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookPagesTest {

    @Test
    fun pagesCoverEveryLineExactlyOnceTheOpeningPageHoldingFewer() {
        val counts = listOf(3, 5, 2, 7, 1)

        val pages = paginateChapter(chapter = 0, counts, linesPerPage = 6, openingLines = 4)

        assertEquals(counts.sum(), pages.sumOf { it.lineCount })
        assertEquals(4, pages.first().lineCount)
        assertTrue(pages.first().opensChapter)
        assertTrue(pages.drop(1).none { it.opensChapter })
        assertTrue(pages.dropLast(1).drop(1).all { it.lineCount == 6 })
    }

    @Test
    fun aPageNamesTheParagraphsItTouchesAndWhereItStartsInsideTheFirst() {
        val pages =
            paginateChapter(chapter = 2, listOf(3, 5, 2), linesPerPage = 4, openingLines = 4)

        assertEquals(BookPage(2, 0, 1, 0, 4, true), pages[0])
        assertEquals(BookPage(2, 1, 1, 1, 4, false), pages[1])
        assertEquals(BookPage(2, 2, 2, 0, 2, false), pages[2])
    }

    @Test
    fun aLineIsFoundOnThePageThatShowsIt() {
        val counts = listOf(3, 5, 2)
        val lines = paragraphLines(counts)
        val pages = paginateChapter(0, counts, linesPerPage = 4, openingLines = 4)

        assertEquals(0, pages.indexAtLine(3, lines))
        assertEquals(1, pages.indexAtLine(4, lines))
        assertEquals(2, pages.indexAtLine(9, lines))
    }

    @Test
    fun anEmptyChapterStillGetsItsOpeningPage() {
        val pages = paginateChapter(1, emptyList(), linesPerPage = 4, openingLines = 3)

        assertEquals(1, pages.size)
        assertTrue(pages.single().opensChapter)
    }
}
