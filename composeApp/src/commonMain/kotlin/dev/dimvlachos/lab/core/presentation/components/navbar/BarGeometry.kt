package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.abs
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

internal class BubbleGeometry(val centerY: Float, val halfHeight: Float)

internal fun bubbleGeometry(
    m: Float,
    stretchFactor: Float,
    bubbleSize: Float,
    bubbleOverhang: Float,
    barHeight: Float,
    pillHeight: Float,
): BubbleGeometry {
    val bubbleHeight = bubbleSize * (2f - stretchFactor).coerceIn(0.75f, 1.1f)
    val height = lerp(bubbleHeight, pillHeight, m)
    val centerYAbsolute = lerp(bubbleSize / 2f, bubbleOverhang + barHeight / 2f, m)
    return BubbleGeometry(centerY = centerYAbsolute - bubbleOverhang, halfHeight = height / 2f)
}

internal class MorphedNotch(val notchRadius: Float, val filletRadius: Float, val centerY: Float)

internal fun morphedNotchParams(
    m: Float,
    handoffM: Float,
    bubbleCenterY: Float,
    bubbleHalfHeight: Float,
    gap: Float,
    filletRadius: Float,
): MorphedNotch? {
    if (m >= handoffM) return null
    val t = FastOutSlowInEasing.transform(m / handoffM)
    if (t >= 1f) return null
    val f = filletRadius * (1f - t)
    if (f < 0.5f) return null
    return MorphedNotch(
        notchRadius = bubbleHalfHeight + gap * (1f - t),
        filletRadius = f,
        centerY = bubbleCenterY,
    )
}

internal class FilletedNotch(
    val leftFilletCenter: Offset,
    val rightFilletCenter: Offset,
    val leftOuterTangent: Offset,
    val rightOuterTangent: Offset,
    val leftNotchTangent: Offset,
    val rightNotchTangent: Offset,
    val notchCenter: Offset,
    val notchRadius: Float,
    val filletRadius: Float,
    val barWidth: Float,
    val leftIsCapTangent: Boolean,
    val rightIsCapTangent: Boolean,
)

internal fun filletedNotch(
    centerX: Float,
    centerY: Float,
    notchRadius: Float,
    filletRadius: Float,
    barWidth: Float,
    cornerRadius: Float,
): FilletedNotch {
    require(filletRadius > 0f) { "filletRadius must be positive" }
    val notchCenter = Offset(centerX, centerY)

    val leftEdge = edgeTangentFillet(notchCenter, notchRadius, filletRadius, xSign = -1f)
    val leftFillet =
        if (leftEdge.outerTangent.x < cornerRadius) {
            capTangentFillet(
                notchCenter = notchCenter,
                notchRadius = notchRadius,
                filletRadius = filletRadius,
                capCenter = Offset(cornerRadius, cornerRadius),
                cornerRadius = cornerRadius,
            ) ?: leftEdge
        } else {
            leftEdge
        }

    val rightEdge = edgeTangentFillet(notchCenter, notchRadius, filletRadius, xSign = 1f)
    val rightFillet =
        if (rightEdge.outerTangent.x > barWidth - cornerRadius) {
            capTangentFillet(
                notchCenter = notchCenter,
                notchRadius = notchRadius,
                filletRadius = filletRadius,
                capCenter = Offset(barWidth - cornerRadius, cornerRadius),
                cornerRadius = cornerRadius,
            ) ?: rightEdge
        } else {
            rightEdge
        }

    return FilletedNotch(
        leftFilletCenter = leftFillet.center,
        rightFilletCenter = rightFillet.center,
        leftOuterTangent = leftFillet.outerTangent,
        rightOuterTangent = rightFillet.outerTangent,
        leftNotchTangent = tangentPoint(leftFillet.center, notchCenter, filletRadius),
        rightNotchTangent = tangentPoint(rightFillet.center, notchCenter, filletRadius),
        notchCenter = notchCenter,
        notchRadius = notchRadius,
        filletRadius = filletRadius,
        barWidth = barWidth,
        leftIsCapTangent = leftFillet.isCapTangent,
        rightIsCapTangent = rightFillet.isCapTangent,
    )
}

internal fun notchCutterPath(notch: FilletedNotch): Path {
    val leftFilletRect = Rect(notch.leftFilletCenter, notch.filletRadius)
    val rightFilletRect = Rect(notch.rightFilletCenter, notch.filletRadius)
    val notchRect = Rect(notch.notchCenter, notch.notchRadius)
    val top = -(notch.notchRadius + notch.filletRadius)

    val leftFilletStart = angleDegrees(notch.leftOuterTangent - notch.leftFilletCenter)
    val leftFilletEnd = angleDegrees(notch.notchCenter - notch.leftFilletCenter)
    val notchStart = angleDegrees(notch.leftFilletCenter - notch.notchCenter)
    val notchEnd = angleDegrees(notch.rightFilletCenter - notch.notchCenter)
    val rightFilletStart = angleDegrees(notch.notchCenter - notch.rightFilletCenter)
    val rightFilletEnd = angleDegrees(notch.rightOuterTangent - notch.rightFilletCenter)

    return Path().apply {
        moveTo(notch.leftOuterTangent.x, notch.leftOuterTangent.y)
        arcTo(
            leftFilletRect,
            leftFilletStart,
            clockwiseSweep(leftFilletStart, leftFilletEnd),
            forceMoveTo = false,
        )
        arcTo(notchRect, notchStart, -clockwiseSweep(notchEnd, notchStart), forceMoveTo = false)
        arcTo(
            rightFilletRect,
            rightFilletStart,
            clockwiseSweep(rightFilletStart, rightFilletEnd),
            forceMoveTo = false,
        )

        // A cap-tangent outer point sits partway down the rounded end, not on the top edge, so a
        // closure straight up from it can cut back through the cap. Step outside the bar's width
        // first (any point past x = 0 or x = barWidth is outside for every y), then go up.
        if (notch.rightIsCapTangent) {
            lineTo(notch.barWidth + 1f, notch.rightOuterTangent.y)
            lineTo(notch.barWidth + 1f, top)
        } else {
            lineTo(notch.rightOuterTangent.x, top)
        }
        if (notch.leftIsCapTangent) {
            lineTo(-1f, top)
            lineTo(-1f, notch.leftOuterTangent.y)
        } else {
            lineTo(notch.leftOuterTangent.x, top)
        }
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

private class OuterFillet(val center: Offset, val outerTangent: Offset, val isCapTangent: Boolean)

private fun edgeTangentFillet(
    notchCenter: Offset,
    notchRadius: Float,
    filletRadius: Float,
    xSign: Float,
): OuterFillet {
    val sumRadii = notchRadius + filletRadius
    val dy = filletRadius - notchCenter.y
    require(dy * dy <= sumRadii * sumRadii) {
        "No tangent point for filletRadius=$filletRadius, centerY=${notchCenter.y}, " +
            "notchRadius=$notchRadius: |filletRadius - centerY| must be <= notchRadius + filletRadius"
    }
    val dx = sqrt(sumRadii * sumRadii - dy * dy)
    val center = Offset(notchCenter.x + xSign * dx, filletRadius)
    return OuterFillet(center, Offset(center.x, 0f), isCapTangent = false)
}

private fun capTangentFillet(
    notchCenter: Offset,
    notchRadius: Float,
    filletRadius: Float,
    capCenter: Offset,
    cornerRadius: Float,
): OuterFillet? {
    val outerRadius = notchRadius + filletRadius
    val innerRadius = cornerRadius - filletRadius
    if (innerRadius <= 0f) return null
    val delta = capCenter - notchCenter
    val d = delta.getDistance()
    if (d <= 0f) return null
    val minDistance = abs(outerRadius - innerRadius)
    val maxDistance = outerRadius + innerRadius
    if (d < minDistance || d > maxDistance) return null

    val a = (outerRadius * outerRadius - innerRadius * innerRadius + d * d) / (2f * d)
    val h = sqrt((outerRadius * outerRadius - a * a).coerceAtLeast(0f))
    val mid = notchCenter + delta * (a / d)
    val perpendicular = Offset(-delta.y, delta.x) * (1f / d)
    val candidateA = mid + perpendicular * h
    val candidateB = mid - perpendicular * h
    val center = if (candidateA.y <= candidateB.y) candidateA else candidateB
    val outerTangent = capCenter + (center - capCenter) * (cornerRadius / innerRadius)
    return OuterFillet(center, outerTangent, isCapTangent = true)
}

private fun tangentPoint(from: Offset, towards: Offset, radius: Float): Offset {
    val direction = towards - from
    return from + direction * (radius / direction.getDistance())
}

private fun angleDegrees(offset: Offset): Float = atan2(offset.y, offset.x) * (180f / PI.toFloat())

private fun clockwiseSweep(start: Float, end: Float): Float = ((end - start) % 360f + 360f) % 360f
