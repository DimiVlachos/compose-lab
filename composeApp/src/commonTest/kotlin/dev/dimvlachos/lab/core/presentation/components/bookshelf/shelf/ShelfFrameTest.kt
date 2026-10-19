package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlin.test.Test
import kotlin.test.assertEquals

class ShelfFrameTest {
    @Test
    fun withReducedMotionTheRibbonStillSettlesButTheLeavesStayStill() {
        val ticks =
            shelfFrameTicks(moving = true, shelfShown = true, frame = 4L, reducedMotion = true)

        assertEquals(ShelfFrameTicks(physics = true, sway = false), ticks)
    }

    @Test
    fun theLeavesSwayEveryFewFramesWhileTheShelfShows() {
        val shown = (1L..8L).map { shelfFrameTicks(false, true, it, reducedMotion = false).sway }
        val covered = (1L..8L).map { shelfFrameTicks(false, false, it, reducedMotion = false).sway }

        assertEquals(8 / SWAY_FRAME_STRIDE.toInt(), shown.count { it })
        assertEquals(0, covered.count { it })
    }
}
