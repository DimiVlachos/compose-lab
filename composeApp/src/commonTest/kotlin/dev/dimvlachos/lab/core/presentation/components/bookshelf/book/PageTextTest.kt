package dev.dimvlachos.lab.core.presentation.components.bookshelf.book

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PageTextTest {

    private val page =
        PageText(
            paragraphs = listOf(listOf("Call", "me", "Ishmael."), listOf("Some", "years", "ago.")),
            firstParagraph = 4,
        )

    @Test
    fun paragraphsAreJoinedByNewlinesAndWordsBySpaces() {
        assertEquals("Call me Ishmael.\nSome years ago.", page.text)
    }

    @Test
    fun aWordIsLocatedByAbsoluteParagraphAndItsCharacters() {
        assertEquals(8 until 16, page.rangeOf(WordRef(4, 2)))
        assertEquals(22 until 27, page.rangeOf(WordRef(5, 1)))
        assertNull(page.rangeOf(WordRef(3, 0)))
    }

    @Test
    fun anOffsetMapsBackToTheWordUnderIt() {
        assertEquals(WordRef(4, 1), page.wordAt(6))
        assertEquals(WordRef(5, 0), page.wordAt(17))
        assertEquals(WordRef(5, 2), page.wordAt(29))
    }

    @Test
    fun aSelectionStaysWithinTheParagraphItStartsIn() {
        assertEquals(PageSelection(4, 1, 2), page.select(5, 24))
        assertEquals(PageSelection(5, 1, 1), page.select(23, 25))
        assertNull(page.select(10, 10))
    }
}
