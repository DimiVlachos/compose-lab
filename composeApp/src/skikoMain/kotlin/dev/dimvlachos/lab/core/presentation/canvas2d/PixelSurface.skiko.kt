package dev.dimvlachos.lab.core.presentation.canvas2d

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo

// Skia's N32 is BGRA here, unpremultiplied, so each pixel is written byte by byte, into one buffer
// kept for the surface's life.
internal actual class PixelSurface
actual constructor(actual val width: Int, actual val height: Int) {
    actual val pixels = IntArray(width * height)
    private val bytes = ByteArray(width * height * 4)
    // Not in an apply block: there, width and height would be the empty bitmap's own.
    private val bitmap = Bitmap()

    init {
        bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.UNPREMUL))
    }

    private val image = bitmap.asComposeImageBitmap()

    actual fun commit(): ImageBitmap {
        for (i in pixels.indices) {
            val p = pixels[i]
            bytes[4 * i] = p.toByte()
            bytes[4 * i + 1] = (p shr 8).toByte()
            bytes[4 * i + 2] = (p shr 16).toByte()
            bytes[4 * i + 3] = (p ushr 24).toByte()
        }
        bitmap.installPixels(bytes)
        bitmap.notifyPixelsChanged()
        return image
    }
}
