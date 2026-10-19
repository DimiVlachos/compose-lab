package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookPage
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun aLeafWhooshesAsItCrossesTheSpineAndPatsAsItLiesDown() {
        val cue = LeafSoundCue()

        assertEquals(emptyList(), cue.next(open = true, listOf(4 to 0f), atMillis = 0))
        assertEquals(emptyList(), cue.next(open = true, listOf(4 to 0.4f), atMillis = 16))
        assertEquals(
            listOf(LeafSound.Turn),
            cue.next(open = true, listOf(4 to 0.6f), atMillis = 33),
        )
        assertEquals(emptyList(), cue.next(open = true, listOf(4 to 0.9f), atMillis = 50))
        assertEquals(
            listOf(LeafSound.Land),
            cue.next(open = true, listOf(4 to 0.99f), atMillis = 66),
            "flat to the eye, though its spring is still settling",
        )
        assertEquals(emptyList(), cue.next(open = true, listOf(4 to 1f), atMillis = 83))
        assertEquals(emptyList(), cue.next(open = true, emptyList(), atMillis = 100))
    }

    @Test
    fun aLeafLetFallBackBeforeItCrossesMakesNoSound() {
        val cue = LeafSoundCue()

        listOf(0f, 0.3f, 0.45f, 0.2f, 0f).forEachIndexed { frame, t ->
            assertEquals(emptyList(), cue.next(open = true, listOf(2 to t), atMillis = frame * 16L))
        }
        assertEquals(emptyList(), cue.next(open = true, emptyList(), atMillis = 100))
    }

    @Test
    fun aLeafTurnedBackLandsWhereItStarted() {
        val cue = LeafSoundCue()

        cue.next(open = true, listOf(3 to 1f), atMillis = 0)
        assertEquals(
            listOf(LeafSound.Turn),
            cue.next(open = true, listOf(3 to 0.4f), atMillis = 16),
        )
        assertEquals(listOf(LeafSound.Land), cue.next(open = true, listOf(3 to 0f), atMillis = 33))
    }

    @Test
    fun aLeafThatLeavesFlightAfterCrossingStillLands() {
        val cue = LeafSoundCue()

        cue.next(open = true, listOf(1 to 0.2f), atMillis = 0)
        assertEquals(
            listOf(LeafSound.Turn),
            cue.next(open = true, listOf(1 to 0.8f), atMillis = 16),
        )
        assertEquals(listOf(LeafSound.Land), cue.next(open = true, emptyList(), atMillis = 33))
    }

    @Test
    fun aRiffleWhooshesAtMostOnceInAShortWhileButPatsEveryLeaf() {
        val cue = LeafSoundCue()

        cue.next(open = true, listOf(0 to 0f, 1 to 0f), atMillis = 0)
        assertEquals(
            listOf(LeafSound.Turn),
            cue.next(open = true, listOf(0 to 0.7f, 1 to 0.6f), atMillis = 16),
        )
        assertEquals(
            listOf(LeafSound.Land, LeafSound.Land),
            cue.next(open = true, listOf(0 to 1f, 1 to 1f), atMillis = 33),
        )
    }

    @Test
    fun leavesMakeNoSoundUnlessTheBookIsOpen() {
        val cue = LeafSoundCue()

        cue.next(open = false, listOf(0 to 0f), atMillis = 0)
        assertEquals(emptyList(), cue.next(open = false, listOf(0 to 0.8f), atMillis = 16))
        assertEquals(emptyList(), cue.next(open = true, listOf(0 to 1f), atMillis = 33))
    }
}
