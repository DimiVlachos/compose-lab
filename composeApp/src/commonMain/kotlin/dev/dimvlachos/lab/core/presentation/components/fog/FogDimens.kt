package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.unit.dp

internal object FogDimens {
    val BrushRadius = 28.dp
    val FogBlur = 18.dp

    /** Dabs this far apart, as a share of the brush radius, read as one continuous stroke. */
    const val DabSpacingRatio = 0.25f

    /** The pale film over the blurred photo. */
    const val TintAlpha = 0.35f

    const val NoiseTileSize = 96
    const val NoiseMaxAlpha = 0.22f

    /** The fog's soft edge, as a share of the window height. */
    const val FogEdge = 0.12f

    /** How long a press must stay still to breathe rather than wipe. */
    const val HoldDelayMillis = 400L
}
