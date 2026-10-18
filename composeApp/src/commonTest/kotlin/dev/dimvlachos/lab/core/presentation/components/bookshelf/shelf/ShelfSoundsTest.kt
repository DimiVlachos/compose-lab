package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.platform.Sound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShelfSoundsTest {
    @Test
    fun aBookSlidesOutAndBack() {
        assertEquals(Sound.BookSlide, shelfSoundFor(ShelfMode.Tip, ShelfMode.Out))
        assertEquals(Sound.BookSlide, shelfSoundFor(ShelfMode.Shelf, ShelfMode.Out))
        assertEquals(Sound.BookSlide, shelfSoundFor(ShelfMode.Open, ShelfMode.Back))
    }

    @Test
    fun theCoverOpens() {
        assertEquals(Sound.BookOpen, shelfSoundFor(ShelfMode.Out, ShelfMode.Open))
    }

    @Test
    fun aTipThatFallsBackIsQuiet() {
        assertNull(shelfSoundFor(ShelfMode.Tip, ShelfMode.Settle))
        assertNull(shelfSoundFor(ShelfMode.Shelf, ShelfMode.Tip))
        assertNull(shelfSoundFor(ShelfMode.Back, ShelfMode.Shelf))
        assertNull(shelfSoundFor(ShelfMode.Open, ShelfMode.Open))
    }
}
