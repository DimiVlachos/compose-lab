package dev.dimvlachos.lab.core.presentation.canvas2d

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PixelSurfaceTest {
    @Test
    fun aCommitShowsThePixelsAsTheyAreNow() {
        val surface = PixelSurface(2, 1)
        surface.pixels[0] = 0xFF102030.toInt()
        surface.pixels[1] = 0x80FF0000.toInt()
        val image = surface.commit()
        val back = IntArray(2).also { image.readPixels(it, width = 2, height = 1) }

        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertEquals(0xFF102030.toInt(), back[0])
        assertEquals(0x80, back[1] ushr 24)

        surface.pixels[0] = 0xFFFFFFFF.toInt()
        val again = surface.commit()
        again.readPixels(back, width = 2, height = 1)

        assertSame(image, again)
        assertEquals(0xFFFFFFFF.toInt(), back[0])
    }
}
