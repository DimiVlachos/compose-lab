package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeClosesTest {
    private val still = Offset.Zero

    @Test
    fun aSwipeDownPutsTheBookBack() {
        assertTrue(swipeCloses(Offset(10f, 90f), still, atFirstPage = false))
        assertTrue(
            swipeCloses(Offset(4f, 30f), Offset(0f, 900f), atFirstPage = false),
            "a flick down",
        )
    }

    @Test
    fun aShortOrSidelongDragDownDoesNot() {
        assertFalse(swipeCloses(Offset(0f, 40f), still, atFirstPage = false))
        assertFalse(swipeCloses(Offset(90f, 60f), still, atFirstPage = false))
        assertFalse(swipeCloses(Offset(0f, -120f), Offset(0f, -900f), atFirstPage = false), "up")
    }

    @Test
    fun aSwipeRightOnTheFirstPagePutsTheBookBack() {
        assertTrue(swipeCloses(Offset(110f, 12f), still, atFirstPage = true))
        assertTrue(swipeCloses(Offset(30f, 0f), Offset(900f, 0f), atFirstPage = true), "a flick")
    }

    @Test
    fun aSwipeRightFurtherInIsAPageTurnNotAClose() {
        assertFalse(swipeCloses(Offset(110f, 12f), still, atFirstPage = false))
        assertFalse(swipeCloses(Offset(-110f, 12f), still, atFirstPage = true), "left turns on")
    }
}
