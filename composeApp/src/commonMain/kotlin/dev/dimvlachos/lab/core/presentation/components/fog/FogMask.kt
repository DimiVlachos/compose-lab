package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

// Puffs along the fog's front: where across the window, and how big against its soft edge. Fixed,
// so the front billows the same way every time.
private val Puffs =
    listOf(
        0.00f to 0.80f,
        0.13f to 0.60f,
        0.26f to 0.90f,
        0.39f to 0.55f,
        0.52f to 0.85f,
        0.65f to 0.65f,
        0.78f to 0.90f,
        0.90f to 0.60f,
        1.00f to 0.80f,
    )

/**
 * Draws, in alpha, where a breath at [level] has fogged the glass: solid below its front, a soft
 * band of [edge] of the height above it, and puffs along it so the front billows like a cloud.
 */
internal fun DrawScope.drawFogMask(level: Float, edge: Float = FogDimens.FogEdge) {
    if (level <= 0f) return
    val front = (1f - level) * size.height
    val band = edge * size.height
    drawRect(Color.Black, topLeft = Offset(0f, front), size = Size(size.width, size.height - front))
    drawRect(
        Brush.verticalGradient(
            0f to Color.Transparent,
            1f to Color.Black,
            startY = front - band,
            endY = front,
        ),
        topLeft = Offset(0f, front - band),
        size = Size(size.width, band),
    )
    for ((x, scale) in Puffs) {
        val centre = Offset(x * size.width, front - band / 2)
        val radius = band * scale
        drawCircle(
            Brush.radialGradient(
                0f to Color.Black,
                1f to Color.Transparent,
                center = centre,
                radius = radius,
            ),
            radius,
            centre,
        )
    }
}

/**
 * How much of the glass at height [y] (a fraction, 0 at the top) a breath at [level] has fogged,
 * from 0 to 1: all of it below the front, fading through the soft band of [edge] above it, as
 * [drawFogMask] draws it (leaving out the puffs).
 */
internal fun fogCoverAt(y: Float, level: Float, edge: Float = FogDimens.FogEdge): Float {
    if (level <= 0f) return 0f
    val front = 1f - level
    return ((y - (front - edge)) / edge).coerceIn(0f, 1f)
}
