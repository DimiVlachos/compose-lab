package dev.dimvlachos.lab.core.presentation.canvas2d

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual class PixelSurface
actual constructor(actual val width: Int, actual val height: Int) {
    actual val pixels = IntArray(width * height)
    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val image = bitmap.asImageBitmap()

    // setPixels marks the bitmap changed, so the GPU's copy of it is uploaded again.
    actual fun commit(): ImageBitmap {
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return image
    }
}
