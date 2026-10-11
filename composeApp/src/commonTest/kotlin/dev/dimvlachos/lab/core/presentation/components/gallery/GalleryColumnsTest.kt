package dev.dimvlachos.lab.core.presentation.components.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryColumnsTest {
    // At density 1, so pixels are dp: the grid sits inside the screen's 20 dp side padding and
    // keeps a 12 dp gap, with cards at least 160 dp wide.
    private fun columnsOnStage(stageWidth: Int): Int =
        galleryColumns(widthPx = stageWidth - 2 * 20, minCellPx = 160, gapPx = 12)

    @Test
    fun everyPhoneHeldUprightKeepsTwoColumns() {
        for (stage in 320..440) assertEquals(2, columnsOnStage(stage), "stage $stage dp")
    }

    @Test
    fun aPhoneOnItsSideFitsFourColumns() {
        assertEquals(4, columnsOnStage(820))
        assertEquals(4, galleryColumns(widthPx = 820, minCellPx = 160, gapPx = 12))
    }

    @Test
    fun aThirdColumnArrivesOnceThreeMinimumCardsFit() {
        // 3 × 160 + 2 × 12 = 504 dp of grid, so a 544 dp stage.
        assertEquals(2, columnsOnStage(543))
        assertEquals(3, columnsOnStage(544))
    }

    @Test
    fun neverFewerThanTwo() {
        assertEquals(2, galleryColumns(widthPx = 100, minCellPx = 160, gapPx = 12))
        assertEquals(2, galleryColumns(widthPx = 0, minCellPx = 160, gapPx = 12))
    }
}
