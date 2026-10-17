package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.GradientStyle

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
    org.jetbrains.skia.Shader.makeTwoPointConicalGradient(
            x0,
            y0,
            r0,
            x1,
            y1,
            r1,
            IntArray(colors.size) { colors[it].toArgb() },
            offsets.toFloatArray(),
            GradientStyle.DEFAULT,
        )
        .asComposeShader()
