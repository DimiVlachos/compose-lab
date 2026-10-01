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

    // The fog clear glass keeps round its edges, as shares of the window: shallow at the top,
    // deepest along the bottom, where condensation collects.
    const val EdgeDepthTop = 0.035f
    const val EdgeDepthSide = 0.07f
    const val EdgeDepthBottom = 0.09f
    const val EdgeRag = 0.03f
    const val EdgeWobbleKnots = 7
    const val EdgeGrainKnots = 9
    const val EdgeMaskWidth = 72
    const val EdgeMaskHeight = 128

    /** How much a drop darkens the glass behind it. */
    const val BeadShade = 0.2f

    /** The darker rim along a drop's lower edge. */
    const val BeadRim = 0.45f

    /** The light caught along a drop's upper edge. */
    const val BeadEdgeLight = 0.55f

    /** The bright glint near a drop's top. */
    const val BeadHighlight = 0.9f
}
