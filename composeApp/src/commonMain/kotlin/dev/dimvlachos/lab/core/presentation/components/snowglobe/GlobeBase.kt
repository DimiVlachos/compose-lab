package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** The table under the globe: its soft shadow and the bright caustic the water throws. */
internal fun DrawScope.drawTable(center: Offset, radius: Float) {
    val floor = center.y + radius * 1.32f
    drawOval(
        Brush.radialGradient(
            listOf(Color(0x55000000), Color.Transparent),
            Offset(center.x, floor),
            radius * 1.1f,
        ),
        topLeft = Offset(center.x - radius * 1.1f, floor - radius * 0.14f),
        size = Size(radius * 2.2f, radius * 0.28f),
    )
    drawOval(
        Brush.radialGradient(
            listOf(Color(0x66FFE9B0), Color.Transparent),
            Offset(center.x + radius * 0.35f, floor + radius * 0.02f),
            radius * 0.4f,
        ),
        topLeft = Offset(center.x + radius * 0.02f, floor - radius * 0.04f),
        size = Size(radius * 0.66f, radius * 0.12f),
    )
}

/** The turned walnut base the sphere sits in. */
internal fun DrawScope.drawBase(center: Offset, radius: Float) {
    val top = center.y + radius * 0.82f
    val width = radius * 1.5f
    drawRoundRect(
        Brush.horizontalGradient(
            listOf(Color(0xFF3B2416), Color(0xFF7A4B2C), Color(0xFF3B2416)),
            startX = center.x - width / 2,
            endX = center.x + width / 2,
        ),
        topLeft = Offset(center.x - width / 2, top),
        size = Size(width, radius * 0.48f),
        cornerRadius = CornerRadius(radius * 0.08f),
    )
    drawRoundRect(
        Color(0xFFC9A15A),
        topLeft = Offset(center.x - width * 0.42f, top + radius * 0.04f),
        size = Size(width * 0.84f, radius * 0.03f),
        cornerRadius = CornerRadius(radius * 0.015f),
    )
}
