package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.ui.unit.dp

internal object FrostDimens {
    val BrushRadius = 28.dp
    val FrostBlur = 18.dp

    /** Dabs this far apart, as a share of the brush radius, read as one continuous stroke. */
    const val DabSpacingRatio = 0.25f

    /** The pale film over the blurred photo. */
    const val TintAlpha = 0.35f

    const val NoiseTileSize = 96
    const val NoiseMaxAlpha = 0.22f
}
