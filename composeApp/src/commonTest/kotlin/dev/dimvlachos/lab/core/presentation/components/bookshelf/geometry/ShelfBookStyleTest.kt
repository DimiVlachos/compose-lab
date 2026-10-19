package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShelfBookStyleTest {

    @Test
    fun theSameBookAlwaysGetsTheSameSpine() {
        assertEquals(shelfBookStyle(42, "#245862"), shelfBookStyle(42, "#245862"))
    }

    @Test
    fun bothBindingsAppearAcrossALibrary() {
        val kinds = (1..40).map { shelfBookStyle(it, "#A84E34").binding }.toSet()

        assertEquals(setOf(Binding.Cloth, Binding.Paper), kinds)
    }

    @Test
    fun clothIsARichDarkShadeAndPaperIsALightStock() {
        val styles = (1..40).map { shelfBookStyle(it, "#D49E34") }

        styles
            .filter { it.binding == Binding.Cloth }
            .forEach { assertTrue(relativeLuminance(it.base) < 0.2f) }
        styles
            .filter { it.binding == Binding.Paper }
            .forEach { assertTrue(relativeLuminance(it.base) > 0.6f) }
    }

    @Test
    fun inkAlwaysContrastsWithItsStock() {
        (1..40)
            .map { shelfBookStyle(it, "#F4F1EA") }
            .forEach { assertTrue(contrastRatio(it.base, it.ink) >= 4.5f) }
    }

    @Test
    fun aMissingOrMalformedColourFallsBackToANeutralCloth() {
        assertEquals(shelfBookStyle(7, null).base, shelfBookStyle(7, "nonsense").base)
    }

    @Test
    fun hexParsingAcceptsWithOrWithoutAHashAndIgnoresAlpha() {
        assertEquals(0x245862, parseRgb("#245862"))
        assertEquals(0x245862, parseRgb("245862"))
        assertEquals(0x245862, parseRgb("#FF245862"))
        assertEquals(null, parseRgb("#12"))
    }
}
