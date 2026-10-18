package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.atan
import kotlin.math.tan

internal object TiltGesture {
    const val MAX_TILT = 0.62f
    const val MIN_GRAB_TILT = 0.035f
    private const val TIP_ZONE = 0.55f
    private const val TAKE_OUT_TILT = 0.34f
    private const val FLICK_TILT = 0.12f
    private const val FLICK_VELOCITY = 650f
    private const val PUT_BACK_PUSH = 70f
    private const val PUT_BACK_VELOCITY = 700f
    private const val PUSH_GAIN = 0.85f
    private const val PUSH_SOFT_START = 140f
    private const val PUSH_SOFTENING = 0.5f
    private const val COVER_CLOSES_BELOW = 0.5f
    private const val COVER_FLICK = 1f

    /** How far a push, in dp, first flattens the open book's arched pages, before it moves. */
    const val PUSH_FLATTEN = 14f

    /** How much of a cover's close first flattens the open book's arched pages. */
    const val COVER_FLATTEN = 0.1f

    fun canTip(grabHeight: Float, spineHeight: Float) = grabHeight >= spineHeight * TIP_ZONE

    fun lever(grabHeight: Float, spineHeight: Float) = maxOf(grabHeight, spineHeight * TIP_ZONE)

    fun tiltFor(dragY: Float, grabHeight: Float, startTilt: Float): Float =
        atan(tan(startTilt) + dragY / grabHeight).coerceIn(0f, MAX_TILT)

    fun shouldTakeOut(tilt: Float, velocityY: Float) =
        tilt > TAKE_OUT_TILT || (velocityY > FLICK_VELOCITY && tilt > FLICK_TILT)

    /** How far down the open book goes for a finger [dragY] down: in step, then softer. */
    fun pushFor(dragY: Float): Float {
        val dy = dragY.coerceAtLeast(0f)
        return dy * PUSH_GAIN - (dy - PUSH_SOFT_START).coerceAtLeast(0f) * PUSH_SOFTENING
    }

    fun shouldPutBack(push: Float, velocityY: Float) =
        push > PUT_BACK_PUSH || velocityY > PUT_BACK_VELOCITY

    /** Whether a cover let go [open] (1 is open) at [velocity] closes a second goes on closing. */
    fun shouldCloseCover(open: Float, velocity: Float) =
        open < COVER_CLOSES_BELOW || velocity > COVER_FLICK
}
