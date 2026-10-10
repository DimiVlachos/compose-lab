package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Paper art turned into bitmaps once, so a frame only draws them. */
internal object PopUpArt {
    /** [painter] drawn into a bitmap [widthPx] by [heightPx]. */
    fun raster(painter: Painter, widthPx: Int, heightPx: Int, density: Density): ImageBitmap {
        val bitmap = ImageBitmap(max(1, widthPx), max(1, heightPx))
        CanvasDrawScope().draw(
            density,
            LayoutDirection.Ltr,
            Canvas(bitmap),
            Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        ) {
            with(painter) { draw(size) }
        }
        return bitmap
    }

    /** The art's shape filled with [color]: a piece's plain paper back. */
    fun silhouette(art: ImageBitmap, color: Color): ImageBitmap {
        val bitmap = ImageBitmap(art.width, art.height)
        Canvas(bitmap)
            .drawImage(
                art,
                Offset.Zero,
                Paint().apply { colorFilter = ColorFilter.tint(color, BlendMode.SrcIn) },
            )
        return bitmap
    }

    /**
     * The art's shape in black, softened by drawing it many times over a disc [radiusPx] wide, and
     * padded by that radius on every side. Done once, so no platform blur is needed.
     */
    fun softShadow(art: ImageBitmap, radiusPx: Float): ImageBitmap {
        val pad = radiusPx.toInt() + 1
        val bitmap = ImageBitmap(art.width + 2 * pad, art.height + 2 * pad)
        val canvas = Canvas(bitmap)
        val paint =
            Paint().apply {
                colorFilter = ColorFilter.tint(Color.Black, BlendMode.SrcIn)
                alpha = TapAlpha
            }
        for (i in 0 until Taps) {
            // A centre tap, then rings half way out and all the way out.
            val ring = if (i == 0) 0f else if (i <= 5) 0.5f else 1f
            val turn = 2.0 * PI * i / (if (i <= 5) 5 else 10)
            val x = pad + radiusPx * ring * cos(turn).toFloat()
            val y = pad + radiusPx * ring * sin(turn).toFloat()
            canvas.drawImage(art, Offset(x, y), paint)
        }
        return bitmap
    }

    private const val Taps = 16
    private const val TapAlpha = 0.15f
}
