package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.unit.dp

internal object FogDimens {
    val BrushRadius = 28.dp
    val FogBlur = 28.dp

    /** Dabs this far apart, as a share of the brush radius, read as one continuous stroke. */
    const val DabSpacingRatio = 0.25f

    /** The fog's soft edge, as a share of the window height. */
    const val FogEdge = 0.12f

    /** How long a press must stay still to breathe rather than wipe. */
    const val HoldDelayMillis = 400L

    // What fog does to the scene behind: little colour left, contrast pressed toward light grey.
    const val MilkySaturation = 0.2f
    const val MilkyContrast = 0.7f
    const val MilkyLift = 0.2f

    /** The milky film over the blurred scene, under the droplets. */
    const val FilmAlpha = 0.1f
}
