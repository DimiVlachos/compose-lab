package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.dp

// Ported from compose-lab's imagemorph; see that package for the history behind each value.
internal object MorphDimens {
    val CardRadius = 16.dp
    val DetailRadius = 0.dp

    // Material's emphasized curve; no springs, so the clip and crop never overshoot.
    const val OpenMs = 500
    const val CloseMs = 400
    val MorphEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // A rising radius outruns the bounds, so a card never lands square.
    const val RiseMs = 200
}
