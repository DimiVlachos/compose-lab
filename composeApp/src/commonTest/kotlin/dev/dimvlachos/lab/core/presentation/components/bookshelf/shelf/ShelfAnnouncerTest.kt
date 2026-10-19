package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShelfAnnouncerTest {
    @Test
    fun aBookThatOpensIsTheOneAnnouncedWhenItGoesBack() {
        val opened = announcedTitle(null, ShelfMode.Open, "Bayou Lanterns")

        assertEquals("Bayou Lanterns", opened)
        assertEquals("Bayou Lanterns", announcedTitle(opened, ShelfMode.Back, "Bayou Lanterns"))
        assertEquals("Bayou Lanterns", announcedTitle(opened, ShelfMode.Shelf, null))
    }

    @Test
    fun aLaterTipThatFallsBackAnnouncesNoOldBook() {
        val tipped = announcedTitle("Bayou Lanterns", ShelfMode.Tip, "Cotton and Iron")

        assertNull(tipped)
        assertNull(announcedTitle(tipped, ShelfMode.Settle, "Cotton and Iron"))
    }
}
