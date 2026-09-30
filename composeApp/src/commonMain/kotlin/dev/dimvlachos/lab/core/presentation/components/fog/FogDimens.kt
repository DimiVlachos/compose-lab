package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.unit.dp

internal object FogDimens {
    val BrushRadius = 28.dp
    val FogBlur = 28.dp

    /** Dabs this far apart, as a share of the brush radius, read as one continuous stroke. */
    const val DabSpacingRatio = 0.25f

    const val NoiseTileSize = 96
    const val NoiseMaxAlpha = 0.22f

    /** The fog's soft edge, as a share of the window height. */
    const val FogEdge = 0.12f

    /** How long a press must stay still to breathe rather than wipe. */
    const val HoldDelayMillis = 400L

    // What fog does to the scene behind: little colour left, contrast pressed toward light grey.
    const val MilkySaturation = 0.25f
    const val MilkyContrast = 0.5f
    const val MilkyLift = 0.38f

    /** The even part of the milky film; the density map adds thicker patches on top. */
    const val FilmAlpha = 0.18f

    const val DensityMapWidth = 48
    const val DensityMapHeight = 64
    const val DensityBlobs = 16
    const val DensityMaxAlpha = 0.45f

    /** The soft brightness where the light comes through, toward the top right. */
    const val GlowAlpha = 0.22f
}
