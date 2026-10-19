package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenPlacementTest {

    private val depth = 150f
    private val height = 220f

    @Test
    fun aSinglePageIsCentredInTheBandAndKeepsTheBookSProportions() {
        val placed =
            OpenBand(top = 100f, bottom = 540f, left = 0f, right = 800f).place(depth, height)

        assertEquals(400f, placed.centerX, 0.01f)
        assertEquals(320f, placed.centerY, 0.01f)
        assertEquals(440f * depth / height, placed.pageWidth, 0.01f)
    }

    @Test
    fun aSinglePageNeverTakesMoreThanMostOfANarrowBand() {
        val placed = OpenBand(top = 0f, bottom = 900f, left = 0f, right = 400f).place(depth, height)

        assertEquals(320f, placed.pageWidth, 0.01f)
    }

    @Test
    fun aSpreadPutsTheSpineOnTheCentreOfTheBand() {
        val placed =
            OpenBand(top = 0f, bottom = 440f, left = 0f, right = 1000f, spread = true)
                .place(depth, height)

        assertEquals(500f, placed.centerX - placed.pageWidth / 2f, 0.01f)
        assertEquals(300f, placed.pageWidth, 0.01f)
    }

    @Test
    fun aSpreadPutsTheSpineOnTheGutterAndFitsTheShorterSide() {
        val placed =
            OpenBand(
                    top = 0f,
                    bottom = 1000f,
                    left = 0f,
                    right = 1000f,
                    spread = true,
                    gutter = 420f,
                )
                .place(depth, height)

        assertEquals(420f, placed.centerX - placed.pageWidth / 2f, 0.01f)
        assertEquals(420f * 0.94f, placed.pageWidth, 0.01f)
        assertTrue(placed.centerX + placed.pageWidth / 2f <= 1000f)
    }

    @Test
    fun aGutterOutsideTheBandIsIgnored() {
        val placed =
            OpenBand(
                    top = 0f,
                    bottom = 440f,
                    left = 100f,
                    right = 900f,
                    spread = true,
                    gutter = 5000f,
                )
                .place(depth, height)

        assertEquals(500f, placed.centerX - placed.pageWidth / 2f, 0.01f)
        assertEquals(300f, placed.pageWidth, 0.01f)
    }
}
