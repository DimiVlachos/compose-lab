package dev.dimvlachos.lab.core.presentation.components.birdcage

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// The cage's geometry, as the prototype lays it out in its 300 × 290 box.

internal const val BoxW = 300f
internal const val BoxH = 290f
internal const val CX = 150f
internal const val DomeY = 118f
internal const val RX = 70f
internal const val RY = 64f
internal const val BaseY = 236f
internal const val TopY = DomeY - RY
internal const val HemUp = TopY + 22f
internal const val HemDown = BaseY + 18f

/** The middle ring sits above the canary's head, so its front arc never runs across the neck. */
internal const val Ring2Y = 140f

/** The cloth's crown: where [halfW] reaches 0, so the top of the cover is a rounded dome. */
internal const val ClothY0 = DomeY - RY - 8f

/** Where the cage's 300 × 290 box sits in a hero of [width] by [height], in the same units. */
internal class BirdcageBox(width: Float, height: Float) {
    val scale = min(min(width / BoxW, height / BoxH), 1.5f)
    val left = (width - BoxW * scale) / 2f

    /** On the bottom of the hero, next to the state text; any spare height goes to the rod. */
    val top = height - BoxH * scale

    /** The switch's bounds round the cage: the prototype's button. */
    val hitLeft = left + (CX - RX - 14f) * scale
    val hitTop = top + (TopY - 14f) * scale
    val hitWidth = (RX * 2f + 28f) * scale
    val hitHeight = (BaseY - TopY + 30f) * scale
}

/**
 * Half-width of the cloth at height [y]: it follows the dome, then hangs a little proud of the
 * bars.
 */
internal fun halfW(y: Float): Float {
    val rx = RX + 8f
    val ry = RY + 8f
    if (y < DomeY) {
        val k = (DomeY - y) / ry
        return rx * sqrt(max(0f, 1f - k * k))
    }
    return rx + (y - DomeY) * .06f
}

/**
 * One sway offset for the sides, the hem and the pleats, growing towards the hem, so they all swing
 * together.
 */
internal fun swayX(y: Float, sway: Float): Float {
    val k = (y - ClothY0) / (HemDown - ClothY0)
    return sway * 6f * k * k
}

/** x of the cloth's left ([side] -1) or right (+1) edge at height [y]. */
internal fun clothSide(y: Float, side: Int, sway: Float, phase: Float): Float {
    val k = (y - ClothY0) / (HemDown - ClothY0)
    return CX +
        side * halfW(y) +
        swayX(y, sway) +
        sin(phase + y * .05f + (if (side > 0) 1f else 0f)) * .8f * k
}

/**
 * x along the hem ([u] 0..1) at hem height [hy]: from side to side, bowing out in the middle as it
 * sways. [inset] pulls the ends in, so the rolled hem's round caps sit inside the side lines.
 */
internal fun hemX(hy: Float, u: Float, inset: Float, sway: Float, phase: Float): Float =
    lerp(clothSide(hy, -1, sway, phase) + inset, clothSide(hy, 1, sway, phase) - inset, u) +
        sway * 3f * (1f - abs(2f * u - 1f))

/**
 * How far the hem dips below [hy] at [u]: its scallops, deeper as the linen [gather]s at the crown,
 * a ripple that grows as it sways, and the middle's dip as it lands on the base ([ripple]).
 */
internal fun hemDY(
    u: Float,
    sway: Float,
    phase: Float,
    gather: Float = 0f,
    ripple: Float = 0f,
): Float =
    abs(sin(u * PI.toFloat() * 4.5f)) * 3.2f * (1f + gather * 1.2f) +
        sin(phase * 1.3f + u * 6f) * (1f + abs(sway) * 2f) +
        max(0f, ripple) * 4f * sin(u * PI.toFloat())
