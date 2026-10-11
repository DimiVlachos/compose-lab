package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * How a photo card's shadow falls, by how far the card is lifted off the table, 0 lying on it to 1
 * held in a finger: a tight, darker contact shadow just under its edges and a wider, fainter one
 * round it, both cast a little down and to the right, as by a light above and behind the viewer.
 * Lifted, the shadows spread, fall further from the card and fade; the contact one all but goes, as
 * nothing touches the table any more. Sizes in dp for a card of its own size, alphas as shares of
 * the theme's card shadow.
 */
internal object CardShadow {
    fun contactBlur(lift: Float): Float = lerp(2f, 5f, lift)

    fun contactDrop(lift: Float): Float = lerp(1.5f, 4f, lift)

    fun contactAlpha(lift: Float): Float = lerp(1.6f, 0.3f, lift)

    fun ambientBlur(lift: Float): Float = lerp(5f, 13f, lift)

    fun ambientDrop(lift: Float): Float = lerp(3.5f, 11f, lift)

    fun ambientAlpha(lift: Float): Float = lerp(1.4f, 1f, lift)

    /** How much bigger than the card its wide shadow is on each side, as a lifted card's is. */
    fun ambientSpread(lift: Float): Float = lerp(0f, 2f, lift)

    /** How far right of straight down the light casts the shadows, as a share of their drop. */
    const val Slant = 0.25f
}

/**
 * Draws soft, blurred shadows of rounded rectangles from one small picture of a blurred corner,
 * made once and stretched over any size: nine pieces, the corners scaled to the blur, the edges and
 * middle stretched, so a frame of moving cards draws its shadows without making anything.
 */
internal class SoftShadow {
    private var texture: ImageBitmap? = null

    /**
     * The shadow of the rectangle [left], [top], [right], [bottom], in px, blurred over [blur] px
     * either side of its edges, its corners rounded as far, in black at [alpha].
     */
    fun DrawScope.drawSoftShadow(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        blur: Float,
        alpha: Float,
    ) {
        if (alpha <= 0f || right <= left || bottom <= top) return
        val image = texture ?: blurredCorner().also { texture = it }
        val outerL = left - blur
        val outerT = top - blur
        val outerR = right + blur
        val outerB = bottom + blur
        // A corner piece covers the blur either side of the edge, or half the shadow if less.
        val piece = min(blur * 2f, min((outerR - outerL) / 2f, (outerB - outerT) / 2f))
        val x0 = outerL.roundToInt()
        val x1 = (outerL + piece).roundToInt()
        val x2 = (outerR - piece).roundToInt()
        val x3 = outerR.roundToInt()
        val y0 = outerT.roundToInt()
        val y1 = (outerT + piece).roundToInt()
        val y2 = (outerB - piece).roundToInt()
        val y3 = outerB.roundToInt()
        val a = alpha.coerceAtMost(1f)
        piece(image, 0, 0, x0, y0, x1, y1, a)
        piece(image, 1, 0, x1, y0, x2, y1, a)
        piece(image, 2, 0, x2, y0, x3, y1, a)
        piece(image, 0, 1, x0, y1, x1, y2, a)
        piece(image, 1, 1, x1, y1, x2, y2, a)
        piece(image, 2, 1, x2, y1, x3, y2, a)
        piece(image, 0, 2, x0, y2, x1, y3, a)
        piece(image, 1, 2, x1, y2, x2, y3, a)
        piece(image, 2, 2, x2, y2, x3, y3, a)
    }

    // One of the nine pieces: the [column]th and [row]th of the picture, drawn from x0, y0 to x1,
    // y1.
    private fun DrawScope.piece(
        image: ImageBitmap,
        column: Int,
        row: Int,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        alpha: Float,
    ) {
        if (x1 <= x0 || y1 <= y0) return
        drawImage(
            image,
            srcOffset = IntOffset(SliceStart[column], SliceStart[row]),
            srcSize = IntSize(SliceSize[column], SliceSize[row]),
            dstOffset = IntOffset(x0, y0),
            dstSize = IntSize(x1 - x0, y1 - y0),
            alpha = alpha,
        )
    }

    private companion object {
        // The picture's blur, in px: its corner pieces are twice this across, and between them
        // lies a strip Middle px wide that is stretched.
        const val Blur = 24
        const val Middle = 2
        const val Side = Blur * 4 + Middle
        val SliceStart = intArrayOf(0, Blur * 2, Blur * 2 + Middle)
        val SliceSize = intArrayOf(Blur * 2, Middle, Blur * 2)

        // A rounded square blurred as by a Gaussian a fifth as wide as the blur either side,
        // built up from rings drawn outside in, each just dark enough that together they make
        // the curve: from nothing at the blur's outer reach to solid at its inner.
        fun blurredCorner(): ImageBitmap {
            val image = ImageBitmap(Side, Side)
            val canvas = Canvas(image)
            val paint = Paint()
            val sigma = Blur / 2.5f
            var covered = 0f
            for (d in Blur downTo -Blur) {
                val target = if (d == -Blur) 1f else coverage(-d / sigma)
                val a = if (covered >= 1f) 0f else 1f - (1f - target) / (1f - covered)
                covered = target
                if (a <= 0f) continue
                paint.color = Color.Black.copy(alpha = a.coerceIn(0f, 1f))
                val inset = (Blur - d).toFloat()
                val radius = max(0f, (Blur + d).toFloat())
                canvas.drawRoundRect(
                    inset,
                    inset,
                    Side - inset,
                    Side - inset,
                    radius,
                    radius,
                    paint,
                )
            }
            return image
        }

        // The share of a Gaussian below [x] standard deviations.
        fun coverage(x: Float): Float = 0.5f * (1f + erf(x / sqrt(2f)))

        // Abramowitz and Stegun's 7.1.26, good to a few parts in ten million.
        fun erf(x: Float): Float {
            val t = 1f / (1f + 0.3275911f * abs(x))
            val y =
                1f -
                    (((((1.0614054f * t - 1.4531521f) * t) + 1.4214138f) * t - 0.28449672f) * t +
                        0.2548296f) * t * exp(-x * x)
            return if (x >= 0f) y else -y
        }
    }
}
