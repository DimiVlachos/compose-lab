package dev.dimvlachos.lab.core.presentation.components.snowglobe

import android.graphics.RadialGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader

internal actual fun twoPointConicalShader(
    x0: Float,
    y0: Float,
    r0: Float,
    x1: Float,
    y1: Float,
    r1: Float,
    colors: List<Color>,
    offsets: List<Float>,
): Shader =
    RadialGradient(
        x0,
        y0,
        r0,
        x1,
        y1,
        r1,
        // The two-circle constructor takes colour longs, which Compose's Color packs the same way.
        LongArray(colors.size) { colors[it].value.toLong() },
        offsets.toFloatArray(),
        android.graphics.Shader.TileMode.CLAMP,
    )
