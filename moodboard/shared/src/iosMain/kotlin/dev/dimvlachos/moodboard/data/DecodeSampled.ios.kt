package dev.dimvlachos.moodboard.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.math.max
import kotlin.math.roundToInt
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

internal actual fun decodeSampled(bytes: ByteArray, maxPixel: Int): ImageBitmap? {
    val image = runCatching { Image.makeFromEncoded(bytes) }.getOrNull() ?: return null
    val longest = max(image.width, image.height)
    if (longest <= maxPixel) return image.toComposeImageBitmap()
    val scale = maxPixel.toFloat() / longest
    val width = (image.width * scale).roundToInt()
    val height = (image.height * scale).roundToInt()
    val surface = Surface.makeRasterN32Premul(width, height)
    surface.canvas.drawImageRect(
        image,
        Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
        Rect.makeWH(width.toFloat(), height.toFloat()),
        FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
        Paint(),
        true,
    )
    return surface.makeImageSnapshot().toComposeImageBitmap()
}
