package dev.dimvlachos.lab.core.presentation.canvas2d

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A bitmap that is painted again and again a pixel at a time, as a canvas's `putImageData` is:
 * write [pixels] (ARGB packed as `0xAARRGGBB`, not premultiplied, row by row, [width] to a row),
 * then [commit] them and draw what it returns. Unlike [imageBitmapOf], the bitmap is made once and
 * reused, so a frame makes no new bitmap.
 */
internal expect class PixelSurface(width: Int, height: Int) {
    val width: Int
    val height: Int
    val pixels: IntArray

    /** Copies [pixels] into the bitmap and returns it, the same bitmap every time. */
    fun commit(): ImageBitmap
}
