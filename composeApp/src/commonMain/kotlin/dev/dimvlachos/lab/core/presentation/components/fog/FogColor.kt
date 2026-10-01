package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.graphics.ColorMatrix

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
