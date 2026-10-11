package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StageShapeTest {
    @Test
    fun aClipsPortraitFramesAreNeverWide() {
        for (width in 320..480 step 8) {
            val w = width.dp
            assertFalse(isWideStage(w, w * 5f / 4f), "4:5 at $w")
            assertFalse(isWideStage(w, w * 16f / 9f), "9:16 at $w")
        }
    }

    @Test
    fun aPhoneOnItsSideIsWide() {
        assertTrue(isWideStage(800.dp, 300.dp))
    }

    @Test
    fun aSquareIsNotWide() {
        assertFalse(isWideStage(400.dp, 400.dp))
    }
}
