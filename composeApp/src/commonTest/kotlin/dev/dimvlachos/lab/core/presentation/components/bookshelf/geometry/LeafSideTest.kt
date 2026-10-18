package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LeafSideTest {

    private fun spread(index: Int, pages: Int) =
        leafSide(index - 1, right = false, pages, spread = true) to
            leafSide(index, right = true, pages, spread = true)

    @Test
    fun aSinglePageShowsThePreviousPageOnTheLeft() {
        assertEquals(LeafSide.Page(3), leafSide(3, right = true, pages = 10, spread = false))
        assertEquals(LeafSide.Page(2), leafSide(2, right = false, pages = 10, spread = false))
        assertEquals(LeafSide.Endpaper, leafSide(-1, right = false, pages = 10, spread = false))
        assertNull(leafSide(10, right = true, pages = 10, spread = false))
    }

    @Test
    fun aSpreadShowsTwoConsecutivePagesTheEvenOneOnTheLeft() {
        assertEquals(LeafSide.Page(0) to LeafSide.Page(1), spread(0, pages = 6))
        assertEquals(LeafSide.Page(4) to LeafSide.Page(5), spread(2, pages = 6))
    }

    @Test
    fun aChapterEndingOnALeftPageLeavesTheRightPageBlank() {
        assertEquals(3, leafCount(pages = 5, spread = true))
        assertEquals(LeafSide.Page(4) to LeafSide.Blank, spread(2, pages = 5))
        assertNull(leafSide(3, right = true, pages = 5, spread = true))
        assertNull(leafSide(2, right = false, pages = 5, spread = true))
    }

    @Test
    fun aTurnedLeafCarriesTheNextSpreadSLeftPageOnItsBack() {
        assertEquals(LeafSide.Page(1), leafSide(0, right = true, pages = 6, spread = true))
        assertEquals(LeafSide.Page(2), leafSide(0, right = false, pages = 6, spread = true))
    }

    @Test
    fun theNarratedPageMapsToItsSpread() {
        assertEquals(0, leafOfPage(1, spread = true))
        assertEquals(2, leafOfPage(4, spread = true))
        assertEquals(4, leafOfPage(4, spread = false))
    }
}
