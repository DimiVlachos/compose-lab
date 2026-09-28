package dev.dimvlachos.lab.core.presentation.components.gallery

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
}
