package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

/**
 * The lab's animation timing: the standard [ease], quick to start and gentle to land, and a short,
 * a medium and a long duration in ms. A screen slides in over [slowMs] on [ease].
 */
@Immutable
public data class Motion(
    val ease: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
    val fastMs: Int = 200,
    val mediumMs: Int = 300,
    val slowMs: Int = 400,
)
