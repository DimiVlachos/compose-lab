package dev.dimvlachos.lab.core.presentation.components.gallery

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryHeaderTest {
    @Test
    fun theHeaderCollapsesWithTheScrollOverItsRange() {
        assertEquals(0f, headerCollapse(scrollPx = 0, rangePx = 60f))
        assertEquals(0.5f, headerCollapse(scrollPx = 30, rangePx = 60f))
        assertEquals(1f, headerCollapse(scrollPx = 60, rangePx = 60f))
        assertEquals(1f, headerCollapse(scrollPx = 900, rangePx = 60f))
    }

    @Test
    fun theShadowShowsOnlyOnceFullyCollapsed() {
        assertFalse(headerShadowShows(collapse = 0f))
        assertFalse(headerShadowShows(collapse = 0.95f))
        assertTrue(headerShadowShows(collapse = 1f))
    }

    @Test
    fun theGridIsCutEightDpBelowTheHeaderAtEveryCollapse() {
        assertEquals(HeaderExpandedHeight + GridTopGap, gridClipTop(collapse = 0f))
        assertEquals(HeaderCollapsedHeight + 8.dp, gridClipTop(collapse = 1f))
        assertEquals(
            (HeaderExpandedHeight + HeaderCollapsedHeight) / 2 + 8.dp,
            gridClipTop(collapse = 0.5f),
        )
    }

    @Test
    fun onTheLightPaletteTheShadowThinsWithTheBackground() {
        assertEquals(1f, headerShadowStrength(fade = 1f, collapse = 1f, dark = false))
        assertEquals(0f, headerShadowStrength(fade = 1f, collapse = 0.9f, dark = false))
        assertEquals(0f, headerShadowStrength(fade = 1f, collapse = 0.5f, dark = false))
        val opening = (0..20).map { headerShadowStrength(1f, 1f - it / 200f, dark = false) }
        opening.zipWithNext().forEach { (a, b) -> assertTrue(b <= a, "$a then $b") }
        assertTrue(opening.zipWithNext().all { (a, b) -> a - b < 0.15f }, "no jump: $opening")
    }

    @Test
    fun theTimedFadeStillLeadsAndTheDarkPaletteKeepsItAlone() {
        assertEquals(0.5f, headerShadowStrength(fade = 0.5f, collapse = 1f, dark = false))
        assertEquals(0.5f, headerShadowStrength(fade = 0.5f, collapse = 0.2f, dark = true))
    }
}
