package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A text's letters, drawn the once into a picture of the text as it is laid out, to draw each from
 * as it moves: drawn from the layout itself, every letter would set the whole text again, every
 * frame.
 */
internal class LetterAtlas(private val text: TextLayoutResult) {
    private var picture: ImageBitmap? = null

    /** The letter in [box] of the text, at its place in the text laid out from the origin. */
    fun DrawScope.drawLetter(box: Rect, alpha: Float) {
        val image = picture ?: drawn().also { picture = it }
        val left = floor(box.left).toInt().coerceIn(0, image.width)
        val top = floor(box.top).toInt().coerceIn(0, image.height)
        val right = ceil(box.right).toInt().coerceIn(left, image.width)
        val bottom = ceil(box.bottom).toInt().coerceIn(top, image.height)
        if (right == left || bottom == top) return
        val at = IntOffset(left, top)
        val size = IntSize(right - left, bottom - top)
        drawImage(image, at, size, at, size, alpha)
    }

    private fun DrawScope.drawn(): ImageBitmap {
        val size = text.size
        val image = ImageBitmap(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1))
        CanvasDrawScope().draw(
            this,
            layoutDirection,
            Canvas(image),
            Size(image.width.toFloat(), image.height.toFloat()),
        ) {
            drawText(text)
        }
        return image
    }
}
