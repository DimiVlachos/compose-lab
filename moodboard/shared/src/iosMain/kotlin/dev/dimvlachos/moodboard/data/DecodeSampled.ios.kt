package dev.dimvlachos.moodboard.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import kotlin.math.max
import kotlin.math.roundToInt
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

// Skia's buffers live outside Kotlin's heap, where the GC can't feel them, so every intermediate
// is closed here instead of waiting for a finalizer. The result is a pixel bitmap, decoded now on
// the background dispatcher, never lazily on the first draw.
internal actual fun decodeSampled(bytes: ByteArray, maxPixel: Int): ImageBitmap? {
    val image = runCatching { Image.makeFromEncoded(bytes) }.getOrNull() ?: return null
    try {
        val longest = max(image.width, image.height)
        if (longest <= maxPixel) return Bitmap.makeFromImage(image).asComposeImageBitmap()
        val scale = maxPixel.toFloat() / longest
        val width = (image.width * scale).roundToInt()
        val height = (image.height * scale).roundToInt()
        val surface = Surface.makeRasterN32Premul(width, height)
        val paint = Paint()
        try {
            surface.canvas.drawImageRect(
                image,
                Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                Rect.makeWH(width.toFloat(), height.toFloat()),
                FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
                paint,
                true,
            )
            val snapshot = surface.makeImageSnapshot()
            try {
                return Bitmap.makeFromImage(snapshot).asComposeImageBitmap()
            } finally {
                snapshot.close()
            }
        } finally {
            paint.close()
            surface.close()
        }
    } finally {
        image.close()
    }
}
