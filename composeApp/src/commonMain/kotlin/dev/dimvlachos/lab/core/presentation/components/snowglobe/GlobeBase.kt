package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import dev.dimvlachos.lab.core.presentation.ui.SnowGlobeArt

// Drawn in globe units (the sphere's centre at 0,0, its radius 1), so the brushes are built once.
private const val Floor = 1.32f
private val TableShadow =
    Brush.radialGradient(
        listOf(SnowGlobeArt.tableShadow, Color.Transparent),
        Offset(0f, Floor),
        1.1f,
    )
private val Caustic =
    Brush.radialGradient(
        listOf(SnowGlobeArt.caustic, Color.Transparent),
        Offset(0.35f, Floor + 0.02f),
        0.4f,
    )

private const val BaseTop = 0.82f
private const val BaseWidth = 1.5f
private val Walnut =
    Brush.horizontalGradient(
        listOf(SnowGlobeArt.walnutDark, SnowGlobeArt.walnutLight, SnowGlobeArt.walnutDark),
        startX = -BaseWidth / 2,
        endX = BaseWidth / 2,
    )

/** The table under the globe: its soft shadow and the bright caustic the water throws. */
internal fun DrawScope.drawTable(center: Offset, radius: Float) {
    inGlobeUnits(center, radius) {
        drawOval(TableShadow, topLeft = Offset(-1.1f, Floor - 0.14f), size = Size(2.2f, 0.28f))
        drawOval(Caustic, topLeft = Offset(0.02f, Floor - 0.04f), size = Size(0.66f, 0.12f))
    }
}

/** The turned walnut base the sphere sits in. */
internal fun DrawScope.drawBase(center: Offset, radius: Float) {
    inGlobeUnits(center, radius) {
        drawRoundRect(
            Walnut,
            topLeft = Offset(-BaseWidth / 2, BaseTop),
            size = Size(BaseWidth, 0.48f),
            cornerRadius = CornerRadius(0.08f),
        )
        drawRoundRect(
            SnowGlobeArt.brassBand,
            topLeft = Offset(-BaseWidth * 0.42f, BaseTop + 0.04f),
            size = Size(BaseWidth * 0.84f, 0.03f),
            cornerRadius = CornerRadius(0.015f),
        )
    }
}

/** Draws [block] with the sphere's centre at 0,0 and its radius 1. */
internal inline fun DrawScope.inGlobeUnits(
    center: Offset,
    radius: Float,
    block: DrawScope.() -> Unit,
) {
    translate(center.x, center.y) { scale(radius, radius, pivot = Offset.Zero) { block() } }
}
