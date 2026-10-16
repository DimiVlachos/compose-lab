package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import dev.dimvlachos.lab.core.presentation.ui.SnowGlobeArt

// Drawn in globe units (the sphere's centre at 0,0, its radius 1), so the brushes are built once.
private val GlassRim =
    Brush.radialGradient(
        0.70f to Color.Transparent,
        0.93f to SnowGlobeArt.glassRimShade,
        1f to SnowGlobeArt.glassFresnel,
        center = Offset.Zero,
        radius = 1f,
    )
private val WindowTopLeft = Offset(-0.62f, -0.74f)
private val WindowSize = Size(0.52f, 0.30f)
// An explicit centre and radius: left unspecified they'd resolve against the drawn oval's size
// from the origin, not the oval's own place, and the window would come out transparent.
private val GlassWindow =
    Brush.radialGradient(
        listOf(SnowGlobeArt.glassWindow, Color.Transparent),
        center = Offset(-0.36f, -0.59f),
        radius = 0.28f,
    )
private val RimLine = Stroke(width = 0.012f)
private val Bubble = Offset(0f, -0.86f)
private val BubbleBrush =
    Brush.radialGradient(
        listOf(SnowGlobeArt.bubbleEdge, SnowGlobeArt.bubbleBody),
        center = Bubble,
        radius = 0.08f,
    )

/** Plain glass where the shader can't run: a darkening rim, a fresnel ring and the window. */
internal fun DrawScope.drawGlassFallback(center: Offset, radius: Float) {
    inGlobeUnits(center, radius) {
        drawCircle(GlassRim, 1f, Offset.Zero)
        drawOval(GlassWindow, topLeft = WindowTopLeft, size = WindowSize)
    }
}

/**
 * On both paths: the glass's rim line and the air bubble trapped at the top, which leans with
 * [tilt].
 */
internal fun DrawScope.drawGlassHighlights(center: Offset, radius: Float, tilt: Float) {
    inGlobeUnits(center, radius) {
        drawCircle(SnowGlobeArt.glassRimLine, 1f, Offset.Zero, style = RimLine)
        rotate(degrees = tilt * 57.3f, pivot = Offset.Zero) {
            drawOval(
                BubbleBrush,
                topLeft = Bubble - Offset(0.11f, 0.035f),
                size = Size(0.22f, 0.07f),
            )
        }
    }
}
