package dev.dimvlachos.lab.core.presentation.canvas2d

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

// BGRA, unpremultiplied, set explicitly rather than the platform's N32, so each pixel is written
// byte by byte into a buffer kept for the surface's life; installPixels copies it into the bitmap.
internal actual class PixelSurface
actual constructor(actual val width: Int, actual val height: Int) {
    actual val pixels = IntArray(width * height)
    private val bytes = ByteArray(width * height * 4)
    // Not in an apply block: there, width and height would be the empty bitmap's own.
    private val bitmap = Bitmap()

    init {
        bitmap.allocPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL))
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
