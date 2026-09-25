package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import kotlin.math.min
import kotlin.math.roundToInt

internal fun barWidth(fullWidth: Int, collapsedWidth: Int, collapse: Float): Int {
    val target = min(fullWidth, collapsedWidth)
    return (fullWidth + (target - fullWidth) * collapse).roundToInt()
}

internal fun barCornerRadius(collapse: Float, barHeight: Float, expandedRadius: Float): Float =
    expandedRadius + (barHeight / 2f - expandedRadius) * collapse

internal fun clampNotchCenter(centerX: Float, barWidth: Float): Float =
    centerX.coerceIn(0f, barWidth)

internal class Notch(val centerX: Float, val centerY: Float, val radius: Float)

internal fun barPath(size: Size, cornerRadius: Float, notch: Notch?): Path {
    val bar =
        Path().apply {
            addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(cornerRadius)))
        }
    if (notch == null) return bar
    val hole = Path().apply { addOval(Rect(Offset(notch.centerX, notch.centerY), notch.radius)) }
    return Path.combine(PathOperation.Difference, bar, hole)
}
