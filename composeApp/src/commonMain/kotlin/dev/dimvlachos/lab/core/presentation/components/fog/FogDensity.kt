package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.random.Random

/**
 * Where the fog is thicker and thinner: a small map of soft white blobs of varying strength, drawn
 * once and stretched over the glass, where its smoothing keeps the patches soft. Real condensation
 * is never an even film.
 */
internal fun fogDensityMap(
    width: Int = FogDimens.DensityMapWidth,
    height: Int = FogDimens.DensityMapHeight,
    seed: Int = 11,
): ImageBitmap {
    val map = ImageBitmap(width, height)
    val random = Random(seed)
    CanvasDrawScope().draw(
        Density(1f),
        LayoutDirection.Ltr,
        Canvas(map),
        Size(width.toFloat(), height.toFloat()),
    ) {
        repeat(FogDimens.DensityBlobs) {
            val centre = Offset(random.nextFloat() * width, random.nextFloat() * height)
            val radius = (0.15f + random.nextFloat() * 0.35f) * width
            val strength = 0.1f + random.nextFloat() * (FogDimens.DensityMaxAlpha - 0.1f)
            drawCircle(
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = strength),
                    1f to Color.Transparent,
                    center = centre,
                    radius = radius,
                ),
                radius,
                centre,
            )
        }
    }
    return map
}

/**
 * What fog does to the scene behind it: most of the colour gone, the contrast pressed toward a
 * light grey. Offsets are on the 0-255 scale.
 */
internal fun milkyColorMatrix(
    saturation: Float = FogDimens.MilkySaturation,
    contrast: Float = FogDimens.MilkyContrast,
    lift: Float = FogDimens.MilkyLift,
): ColorMatrix {
    // Luminance weights, then each channel mixed toward that grey by the saturation.
    val r = 0.2126f
    val g = 0.7152f
    val b = 0.0722f
    fun row(own: Float, weight: Float) = own * saturation + weight * (1 - saturation)
    val offset = lift * 255f
    return ColorMatrix(
        floatArrayOf(
            contrast * row(1f, r),
            contrast * row(0f, g),
            contrast * row(0f, b),
            0f,
            offset,
            contrast * row(0f, r),
            contrast * row(1f, g),
            contrast * row(0f, b),
            0f,
            offset,
            contrast * row(0f, r),
            contrast * row(0f, g),
            contrast * row(1f, b),
            0f,
            offset,
            0f,
            0f,
            0f,
            1f,
            0f,
        )
    )
}
