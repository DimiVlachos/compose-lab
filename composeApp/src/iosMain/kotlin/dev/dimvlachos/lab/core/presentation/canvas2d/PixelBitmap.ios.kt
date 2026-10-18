package dev.dimvlachos.lab.core.presentation.canvas2d

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo

// Skia's N32 on iOS is BGRA, unpremultiplied here, so each pixel is written byte by byte.
internal actual fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bytes = ByteArray(width * height * 4)
    for (i in 0 until width * height) {
        val p = pixels[i]
        bytes[4 * i] = p.toByte()
        bytes[4 * i + 1] = (p shr 8).toByte()
        bytes[4 * i + 2] = (p shr 16).toByte()
        bytes[4 * i + 3] = (p ushr 24).toByte()
    }
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.UNPREMUL))
    bitmap.installPixels(bytes)
    return bitmap.asComposeImageBitmap()
}
