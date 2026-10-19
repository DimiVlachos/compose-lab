package dev.dimvlachos.lab.core.presentation.canvas2d

import kotlin.test.Test
import kotlin.test.assertEquals

class PixelBitmapTest {
    @Test
    fun pixelsComeBackAsTheyWentIn() {
        val pixels =
            intArrayOf(0xFF102030.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0x00000000)

        val image = imageBitmapOf(pixels, 2, 2)
        val back = IntArray(4).also { image.readPixels(it, width = 2, height = 2) }

        assertEquals(2, image.width)
        assertEquals(2, image.height)
        assertEquals(0xFF102030.toInt(), back[0])
        assertEquals(0xFFFFFFFF.toInt(), back[1])
        assertEquals(0xFF000000.toInt(), back[2])
        assertEquals(0, back[3] ushr 24)
    }

    @Test
    fun aHalfTransparentPixelKeepsItsAlpha() {
        val image = imageBitmapOf(intArrayOf(0x80FF0000.toInt()), 1, 1)
        val back = IntArray(1).also { image.readPixels(it, width = 1, height = 1) }

        assertEquals(0x80, back[0] ushr 24)
    }
}
