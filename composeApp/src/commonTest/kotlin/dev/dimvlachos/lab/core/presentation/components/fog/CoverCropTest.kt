package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class CoverCropTest {
    private val texture = IntSize(1296, 1620) // 4:5

    @Test
    fun onTheClipsOwnFrameTheWholeTextureShows() {
        assertEquals(IntOffset(0, 0) to texture, coverCrop(texture, Size(400f, 500f)))
    }

    @Test
    fun onATallPhoneTheSidesAreCroppedNotSquashed() {
        // A 400 x 1000 window takes a 648 x 1620 slice from the middle.
        assertEquals(IntOffset(324, 0) to IntSize(648, 1620), coverCrop(texture, Size(400f, 1000f)))
    }

    @Test
    fun onAWideWindowTheTopAndBottomAreCropped() {
        // A 1000 x 400 window takes a 1296 x 518 slice from the middle.
        assertEquals(IntOffset(0, 551) to IntSize(1296, 518), coverCrop(texture, Size(1000f, 400f)))
    }

    @Test
    fun anEmptyWindowTakesTheWholeTextureRatherThanCrashing() {
        assertEquals(IntOffset(0, 0) to texture, coverCrop(texture, Size(0f, 0f)))
    }
}
