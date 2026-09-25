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
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tanh

internal fun barWidth(fullWidth: Int, collapsedWidth: Int, collapse: Float): Int {
    val target = min(fullWidth, collapsedWidth)
    return (fullWidth + (target - fullWidth) * collapse).roundToInt()
}

internal fun clampNotchCenter(centerX: Float, barWidth: Float): Float =
    centerX.coerceIn(0f, barWidth)

internal fun softCompressToCeiling(value: Float, rest: Float, ceiling: Float?): Float {
    if (ceiling == null || value <= rest) return value
    val room = (ceiling - rest).coerceAtLeast(0.01f)
    val excess = value - rest
    return rest + room * tanh(excess / room)
}

internal class BubbleExtents(val left: Float, val right: Float)

private const val MinBubbleExtentFraction = 0.5f

internal fun bubbleExtents(
    rawLeft: Float,
    rawRight: Float,
    calmCenter: Float,
    stretch: Boolean,
    bubbleHalfWidthRest: Float,
    extentCeiling: Float?,
): BubbleExtents {
    val leftStretch = if (stretch) (calmCenter - rawLeft) * 2f else 1f
    val rightStretch = if (stretch) (rawRight - calmCenter) * 2f else 1f
    val minExtent = bubbleHalfWidthRest * MinBubbleExtentFraction
    val left =
        softCompressToCeiling(bubbleHalfWidthRest * leftStretch, bubbleHalfWidthRest, extentCeiling)
            .coerceAtLeast(minExtent)
    val right =
        softCompressToCeiling(
                bubbleHalfWidthRest * rightStretch,
                bubbleHalfWidthRest,
                extentCeiling,
            )
            .coerceAtLeast(minExtent)
    return BubbleExtents(left, right)
}

internal class BarLayout(val width: Int, val left: Int, val inset: Int)

internal fun barLayout(
    containerWidth: Int,
    collapsedWidth: Int,
    collapse: Float,
    actionInset: Int,
    actionReveal: Float,
): BarLayout {
    val inset = (actionInset * actionReveal).roundToInt()
    val width = barWidth(containerWidth - inset, collapsedWidth, collapse)
    val left = (containerWidth - (width + inset)) / 2
    return BarLayout(width, left, inset)
}

internal fun barHeightPx(collapse: Float, expandedHeight: Float, collapsedHeight: Float): Float =
    lerp(expandedHeight, collapsedHeight, collapse)

internal fun pillHeightPx(barHeight: Float, pillInset: Float): Float = barHeight - 2f * pillInset

internal fun actionButtonSizePx(
    collapse: Float,
    expandedHeight: Float,
    collapsedHeight: Float,
): Float = barHeightPx(collapse, expandedHeight, collapsedHeight)

internal fun actionInsetPx(
    collapse: Float,
    gap: Float,
    expandedHeight: Float,
    collapsedHeight: Float,
): Float = gap + actionButtonSizePx(collapse, expandedHeight, collapsedHeight)

internal fun bubbleHandoff(
    bubbleOverhang: Float,
    barHeight: Float,
    collapsedBarHeight: Float,
    pillInset: Float,
): Float {
    val shrink = barHeight - collapsedBarHeight
    if (shrink <= 0f) return bubbleOverhang / (bubbleOverhang + pillInset)
    val b = bubbleOverhang + pillInset - shrink
    val discriminant = b * b + 4f * shrink * bubbleOverhang
    return (-b + sqrt(discriminant)) / (2f * shrink)
}

internal class BubbleGeometry(val centerY: Float, val halfHeight: Float)

internal fun bubbleGeometry(
    m: Float,
    stretchFactor: Float,
    bubbleSize: Float,
    bubbleOverhang: Float,
    restBarHeight: Float,
    barHeight: Float,
    pillHeight: Float,
): BubbleGeometry {
    val bubbleHeight = bubbleSize * (2f - stretchFactor).coerceIn(0.75f, 1.1f)
    val height = lerp(bubbleHeight, pillHeight, m)
    val barBottom = bubbleOverhang + restBarHeight
    val barCenterAbsolute = barBottom - barHeight / 2f
    val centerYAbsolute = lerp(bubbleSize / 2f, barCenterAbsolute, m)
    val barTop = barBottom - barHeight
    return BubbleGeometry(centerY = centerYAbsolute - barTop, halfHeight = height / 2f)
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
    val leftFilletRadius: Float,
    val rightFilletRadius: Float,
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

    val leftFillet =
        blendedFillet(
            notchCenter = notchCenter,
            notchRadius = notchRadius,
            filletRadius = filletRadius,
            xSign = -1f,
            capCenter = Offset(cornerRadius, cornerRadius),
            cornerRadius = cornerRadius,
            edgeThreshold = cornerRadius,
        )

    val rightFillet =
        blendedFillet(
            notchCenter = notchCenter,
            notchRadius = notchRadius,
            filletRadius = filletRadius,
            xSign = 1f,
            capCenter = Offset(barWidth - cornerRadius, cornerRadius),
            cornerRadius = cornerRadius,
            edgeThreshold = barWidth - cornerRadius,
        )

    return FilletedNotch(
        leftFilletCenter = leftFillet.center,
        rightFilletCenter = rightFillet.center,
        leftOuterTangent = leftFillet.outerTangent,
        rightOuterTangent = rightFillet.outerTangent,
        leftNotchTangent = tangentPoint(leftFillet.center, notchCenter, leftFillet.radius),
        rightNotchTangent = tangentPoint(rightFillet.center, notchCenter, rightFillet.radius),
        notchCenter = notchCenter,
        notchRadius = notchRadius,
        leftFilletRadius = leftFillet.radius,
        rightFilletRadius = rightFillet.radius,
        barWidth = barWidth,
        leftIsCapTangent = leftFillet.isCapTangent,
        rightIsCapTangent = rightFillet.isCapTangent,
    )
}

internal fun notchCutterPath(notch: FilletedNotch): Path {
    val leftFilletRect = Rect(notch.leftFilletCenter, notch.leftFilletRadius)
    val rightFilletRect = Rect(notch.rightFilletCenter, notch.rightFilletRadius)
    val notchRect = Rect(notch.notchCenter, notch.notchRadius)
    val top = -(notch.notchRadius + max(notch.leftFilletRadius, notch.rightFilletRadius))

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

private class OuterFillet(
    val center: Offset,
    val outerTangent: Offset,
    val radius: Float,
    val isCapTangent: Boolean,
)

private const val BranchBlendWindowPx = 16f

private fun blendedFillet(
    notchCenter: Offset,
    notchRadius: Float,
    filletRadius: Float,
    xSign: Float,
    capCenter: Offset,
    cornerRadius: Float,
    edgeThreshold: Float,
): OuterFillet {
    val edge = edgeTangentFillet(notchCenter, notchRadius, filletRadius, xSign)
    val penetration = xSign * (edge.outerTangent.x - edgeThreshold)
    if (penetration <= 0f) return edge
    val cap =
        capTangentFillet(
            notchCenter = notchCenter,
            notchRadius = notchRadius,
            filletRadius = filletRadius,
            capCenter = capCenter,
            cornerRadius = cornerRadius,
            outwardSign = xSign,
        ) ?: return edge
    val window = BranchBlendWindowPx
    val t = (penetration / window).coerceIn(0f, 1f)
    if (t >= 1f) return cap
    return OuterFillet(
        center = lerpOffset(edge.center, cap.center, t),
        outerTangent = lerpOffset(edge.outerTangent, cap.outerTangent, t),
        radius = lerp(edge.radius, cap.radius, t),
        isCapTangent = true,
    )
}

private fun lerpOffset(start: Offset, stop: Offset, fraction: Float): Offset =
    Offset(lerp(start.x, stop.x, fraction), lerp(start.y, stop.y, fraction))

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
    return OuterFillet(center, Offset(center.x, 0f), filletRadius, isCapTangent = false)
}

private const val CapFilletSafetyMarginPx = 0.01f

private fun capTangentFillet(
    notchCenter: Offset,
    notchRadius: Float,
    filletRadius: Float,
    capCenter: Offset,
    cornerRadius: Float,
    outwardSign: Float,
): OuterFillet? {
    val delta = capCenter - notchCenter
    val d = delta.getDistance()
    val fittingRadius = (d - notchRadius + cornerRadius) / 2f - CapFilletSafetyMarginPx
    val radius = min(filletRadius, fittingRadius)
    if (radius <= 0f) return null
    val outerRadius = notchRadius + radius
    val innerRadius = cornerRadius - radius
    if (innerRadius <= 0f) return null
    if (d <= 0f) return null
    if (d > outerRadius + innerRadius) return null

    val a = (outerRadius * outerRadius - innerRadius * innerRadius + d * d) / (2f * d)
    val h = sqrt((outerRadius * outerRadius - a * a).coerceAtLeast(0f))
    val mid = notchCenter + delta * (a / d)
    val perpendicular = Offset(-delta.y, delta.x) * (1f / d)
    val candidateA = mid + perpendicular * h
    val candidateB = mid - perpendicular * h
    val center = if (outwardSign * (candidateA.x - candidateB.x) >= 0f) candidateA else candidateB
    val outerTangent = capCenter + (center - capCenter) * (cornerRadius / innerRadius)
    return OuterFillet(center, outerTangent, radius, isCapTangent = true)
}

private fun tangentPoint(from: Offset, towards: Offset, radius: Float): Offset {
    val direction = towards - from
    return from + direction * (radius / direction.getDistance())
}

private fun angleDegrees(offset: Offset): Float = atan2(offset.y, offset.x) * (180f / PI.toFloat())

private fun clockwiseSweep(start: Float, end: Float): Float = ((end - start) % 360f + 360f) % 360f
