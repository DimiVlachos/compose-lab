package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlin.test.Test
import kotlin.test.assertEquals

class BridgePagesTest {

    private fun chapter(name: String, count: Int): (Int, Boolean) -> PageContent? = { index, _ ->
        when (index) {
            -1 -> PageContent.Endpaper
            in 0 until count -> page(name, index)
            else -> null
        }
    }

    private fun page(name: String, index: Int) = PageContent.Passage(name, listOf("$index"), null)

    private fun fronts(
        earlier: (Int, Boolean) -> PageContent?,
        at: Int,
        later: (Int, Boolean) -> PageContent?,
        to: Int,
    ): List<PageContent> = bridgePages(earlier, at, later, to).map { it.second }

    @Test
    fun aMissingBackOfABridgedLeafTurnsAsABlankPage() {
        val spread: (Int, Boolean) -> PageContent? = { index, right ->
            if (right || index < 1) page("a", index) else null
        }

        val leaves = bridgePages(spread, 1, chapter("b", 4), 2)

        assertEquals(PageContent.Blank, leaves[1].first)
        assertEquals(page("a", 1), leaves[1].second)
    }

    @Test
    fun aBridgeStartsOnTheCurrentSpreadAndEndsOnTheTargetSpread() {
        val pages = fronts(chapter("a", 10), 4, chapter("b", 10), 6)

        assertEquals(
            listOf(
                page("a", 3),
                page("a", 4),
                page("a", 5),
                page("b", 4),
                page("b", 5),
                page("b", 6),
            ),
            pages,
        )
    }

    @Test
    fun aChapterOpeningIsReachedThroughItsEndpaper() {
        val pages = fronts(chapter("a", 10), 0, chapter("b", 10), 0)

        assertEquals(
            listOf(
                PageContent.Endpaper,
                page("a", 0),
                page("a", 1),
                PageContent.Endpaper,
                page("b", 0),
            ),
            pages,
        )
    }

    @Test
    fun leavingFromTheLastPageSkipsThePageAfterIt() {
        val pages = fronts(chapter("a", 3), 2, chapter("b", 10), 5)

        assertEquals(
            listOf(page("a", 1), page("a", 2), page("b", 3), page("b", 4), page("b", 5)),
            pages,
        )
    }
}
