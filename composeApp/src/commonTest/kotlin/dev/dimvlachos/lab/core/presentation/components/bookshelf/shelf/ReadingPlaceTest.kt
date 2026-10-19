package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingPlaceTest {
    // A chapter of 6 paragraphs in pages of two paragraphs each in one layout, three in another.
    private fun pages(perPage: Int) =
        (0 until 6 step perPage).map { BookPage(0, it, it + perPage - 1, 0, 2 * perPage, it == 0) }

    private val narrow = pages(2)
    private val wide = pages(3)

    private fun pageIn(pages: List<BookPage>) = { paragraph: Int ->
        pages.indexOfLast { it.firstParagraph <= paragraph }
    }

    @Test
    fun aBookOpensAtItsBookmark() {
        val place = ReadingPlace()

        assertEquals(
            1,
            place.pageFor(
                1,
                spread = false,
                narrow,
                shownLeaf = 0,
                bookmarkPage = 1,
                pageIn(narrow),
            ),
        )
    }

    @Test
    fun aBookPutBackOpensAtItsBookmarkAgain() {
        val place = ReadingPlace()
        place.pageFor(1, spread = false, narrow, shownLeaf = 0, bookmarkPage = 1, pageIn(narrow))
        // The screen turned while it was being read, at its last page.
        place.pageFor(1, spread = true, wide, shownLeaf = 2, bookmarkPage = 1, pageIn(wide))

        place.forget()

        assertEquals(
            1,
            place.pageFor(
                1,
                spread = true,
                wide,
                shownLeaf = 0,
                bookmarkPage = 1,
                pageIn(wide),
            ),
        )
    }

    @Test
    fun turningASpreadBackToSinglePagesKeepsThePlaceToo() {
        // Twelve paragraphs: three to a wide page, two to a narrow one.
        fun pagesOf(perPage: Int) =
            (0 until 12 step perPage).map {
                BookPage(0, it, it + perPage - 1, 0, 2 * perPage, it == 0)
            }
        val wide = pagesOf(3)
        val narrow = pagesOf(2)
        val place = ReadingPlace()
        place.pageFor(1, spread = true, wide, shownLeaf = 0, bookmarkPage = 0, pageIn(wide))

        // The second spread shows wide pages 2 and 3, from paragraph 6; turned upright, the
        // narrow page that paragraph begins is the fourth.
        assertEquals(
            3,
            place.pageFor(
                1,
                spread = false,
                narrow,
                shownLeaf = 1,
                bookmarkPage = 0,
                pageIn(narrow),
            ),
        )
    }

    @Test
    fun turningTheScreenKeepsThePlaceInTheText() {
        val place = ReadingPlace()
        place.pageFor(1, spread = false, narrow, shownLeaf = 0, bookmarkPage = 0, pageIn(narrow))

        // Read on to the third narrow page (paragraphs 4 and 5), then the phone turns on its
        // side: the wide layout's second page holds paragraphs 3 to 5.
        assertEquals(
            1,
            place.pageFor(1, spread = true, wide, shownLeaf = 2, bookmarkPage = 0, pageIn(wide)),
        )
    }

    @Test
    fun anotherBookOpensAtItsOwnBookmark() {
        val place = ReadingPlace()
        place.pageFor(1, spread = false, narrow, shownLeaf = 0, bookmarkPage = 0, pageIn(narrow))
        place.pageFor(1, spread = true, wide, shownLeaf = 2, bookmarkPage = 0, pageIn(wide))

        place.pageFor(null, spread = true, wide, shownLeaf = 0, bookmarkPage = 0, pageIn(wide))
        assertEquals(
            1,
            place.pageFor(2, spread = true, wide, shownLeaf = 0, bookmarkPage = 1, pageIn(wide)),
        )
    }

    @Test
    fun aPageTurnSoundsOnlyForATurnInAnOpenBook() {
        val cue = PageFlipCue()

        assertFalse(cue.next(open = false, page = 0))
        assertFalse(cue.next(open = true, page = 4), "the jump to the bookmark as it opens")
        assertTrue(cue.next(open = true, page = 5))
        assertFalse(cue.next(open = true, page = 5))
        assertFalse(cue.next(open = false, page = 5))
        assertFalse(cue.next(open = true, page = 4), "reopened at its bookmark")
        assertTrue(cue.next(open = true, page = 3))
    }
}
