package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal fun barWidth(fullWidth: Int, collapsedWidth: Int, collapse: Float): Int {
    val target = min(fullWidth, collapsedWidth)
    return (fullWidth + (target - fullWidth) * collapse).roundToInt()
}

internal fun clampNotchCenter(centerX: Float, barWidth: Float): Float =
    centerX.coerceIn(0f, barWidth)

internal class FilletedNotch(
    val leftFilletCenter: Offset,
    val rightFilletCenter: Offset,
    val leftNotchTangent: Offset,
    val rightNotchTangent: Offset,
    val notchCenter: Offset,
    val notchRadius: Float,
    val filletRadius: Float,
)

internal fun filletedNotch(
    centerX: Float,
    centerY: Float,
    notchRadius: Float,
    filletRadius: Float,
): FilletedNotch {
    val notchCenter = Offset(centerX, centerY)
    val sumRadii = notchRadius + filletRadius
    val dy = filletRadius - centerY
    val dx = sqrt(sumRadii * sumRadii - dy * dy)
    val leftFilletCenter = Offset(centerX - dx, filletRadius)
    val rightFilletCenter = Offset(centerX + dx, filletRadius)
    return FilletedNotch(
        leftFilletCenter = leftFilletCenter,
        rightFilletCenter = rightFilletCenter,
        leftNotchTangent = tangentPoint(leftFilletCenter, notchCenter, filletRadius),
        rightNotchTangent = tangentPoint(rightFilletCenter, notchCenter, filletRadius),
        notchCenter = notchCenter,
        notchRadius = notchRadius,
        filletRadius = filletRadius,
    )
}

internal fun notchCutterPath(notch: FilletedNotch): Path {
    val leftFilletRect = Rect(notch.leftFilletCenter, notch.filletRadius)
    val rightFilletRect = Rect(notch.rightFilletCenter, notch.filletRadius)
    val notchRect = Rect(notch.notchCenter, notch.notchRadius)
    val top = -(notch.notchRadius + notch.filletRadius)

    val leftFilletToNotch = angleDegrees(notch.notchCenter - notch.leftFilletCenter)
    val rightFilletToNotch = angleDegrees(notch.notchCenter - notch.rightFilletCenter)
    val notchToLeftFillet = angleDegrees(notch.leftFilletCenter - notch.notchCenter)
    val notchToRightFillet = angleDegrees(notch.rightFilletCenter - notch.notchCenter)

    return Path().apply {
        moveTo(notch.leftFilletCenter.x, 0f)
        arcTo(
            rect = leftFilletRect,
            startAngleDegrees = -90f,
            sweepAngleDegrees = leftFilletToNotch - -90f,
            forceMoveTo = false,
        )
        arcTo(
            rect = notchRect,
            startAngleDegrees = notchToLeftFillet,
            sweepAngleDegrees = notchToRightFillet - notchToLeftFillet,
            forceMoveTo = false,
        )
        arcTo(
            rect = rightFilletRect,
            startAngleDegrees = rightFilletToNotch,
            sweepAngleDegrees = -90f - rightFilletToNotch,
            forceMoveTo = false,
        )
        lineTo(notch.rightFilletCenter.x, top)
        lineTo(notch.leftFilletCenter.x, top)
        close()
    }
}

internal fun barPath(size: Size, cornerRadius: Float, notch: FilletedNotch?): Path {
    val bar =
        Path().apply {
            addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(cornerRadius)))
        }
    if (notch == null) return bar
    return Path.combine(PathOperation.Difference, bar, notchCutterPath(notch))
}

private fun tangentPoint(from: Offset, towards: Offset, radius: Float): Offset {
    val direction = towards - from
    return from + direction * (radius / direction.getDistance())
}

private fun angleDegrees(offset: Offset): Float = atan2(offset.y, offset.x) * (180f / PI.toFloat())
