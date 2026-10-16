package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/** Plain glass where the shader can't run: a darkening rim, a fresnel ring and the window. */
internal fun DrawScope.drawGlassFallback(center: Offset, radius: Float) {
    drawCircle(
        Brush.radialGradient(
            0.70f to Color.Transparent,
            0.93f to Color(0x22101826),
            1f to Color(0x55E8F0FF),
            center = center,
            radius = radius,
        ),
        radius,
        center,
    )
    drawOval(
        Brush.radialGradient(listOf(Color(0x8CFFFFFF), Color.Transparent)),
        topLeft = center + Offset(-radius * 0.62f, -radius * 0.74f),
        size = Size(radius * 0.52f, radius * 0.30f),
    )
}

/**
 * On both paths: the glass's rim line and the air bubble trapped at the top, which leans with
 * [tilt].
 */
internal fun DrawScope.drawGlassHighlights(center: Offset, radius: Float, tilt: Float) {
    drawCircle(Color(0x40FFFFFF), radius, center, style = Stroke(width = radius * 0.012f))
    rotate(degrees = tilt * 57.3f, pivot = center) {
        val bubble = center + Offset(0f, -radius * 0.86f)
        drawOval(
            Brush.radialGradient(
                listOf(Color(0x30FFFFFF), Color(0x70FFFFFF)),
                center = bubble,
                radius = radius * 0.08f,
            ),
            topLeft = bubble - Offset(radius * 0.11f, radius * 0.035f),
            size = Size(radius * 0.22f, radius * 0.07f),
        )
    }
}
