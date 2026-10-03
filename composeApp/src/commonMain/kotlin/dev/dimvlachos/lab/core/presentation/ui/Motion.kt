package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

@Immutable
data class Motion(
    val ease: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
    val fastMs: Int = 200,
    val mediumMs: Int = 300,
    val slowMs: Int = 400,
)
