package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlin.random.Random

/**
 * A square tile of grain: light and dark specks at random strengths, to repeat over the frost.
 * Drawn once, pixel by pixel, in common code, so both platforms get the same grain without a
 * shader.
 */
internal fun frostNoiseTile(
    size: Int = FrostDimens.NoiseTileSize,
    maxAlpha: Float = FrostDimens.NoiseMaxAlpha,
    seed: Int = 7,
): ImageBitmap {
    val tile = ImageBitmap(size, size)
    val canvas = Canvas(tile)
    val paint = Paint()
    val random = Random(seed)
    val pixel = Size(1f, 1f)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val speck = if (random.nextBoolean()) Color.White else Color.Black
            paint.color = speck.copy(alpha = random.nextFloat() * maxAlpha)
            canvas.drawRect(Rect(Offset(x.toFloat(), y.toFloat()), pixel), paint)
        }
    }
    return tile
}
