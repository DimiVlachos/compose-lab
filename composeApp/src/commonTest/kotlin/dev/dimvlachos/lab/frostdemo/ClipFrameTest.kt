package dev.dimvlachos.lab.frostdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class ClipFrameTest {
    @Test
    fun onTheClipsOwnFrameNothingMoves() {
        assertEquals(Offset(0.3f, 0.6f), clipFrameToWindow(Offset(0.3f, 0.6f), Size(400f, 500f)))
    }

    @Test
    fun onATallPhoneTheFrameSitsCentredAcrossTheFullWidth() {
        // A 400 x 1000 window holds a 400 x 500 frame from y = 250 to 750.
        assertEquals(Offset(0.5f, 0.25f), clipFrameToWindow(Offset(0.5f, 0f), Size(400f, 1000f)))
        assertEquals(Offset(0.5f, 0.5f), clipFrameToWindow(Offset(0.5f, 0.5f), Size(400f, 1000f)))
        assertEquals(Offset(0.5f, 0.75f), clipFrameToWindow(Offset(0.5f, 1f), Size(400f, 1000f)))
    }

    @Test
    fun onAWideWindowTheFrameSitsCentredAcrossTheFullHeight() {
        // A 1000 x 500 window holds a 400 x 500 frame from x = 300 to 700.
        assertEquals(Offset(0.3f, 0.5f), clipFrameToWindow(Offset(0f, 0.5f), Size(1000f, 500f)))
        assertEquals(Offset(0.7f, 0.5f), clipFrameToWindow(Offset(1f, 0.5f), Size(1000f, 500f)))
    }
}
