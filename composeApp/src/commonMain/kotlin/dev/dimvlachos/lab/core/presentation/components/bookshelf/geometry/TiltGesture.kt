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

    fun canTip(grabHeight: Float, spineHeight: Float) = grabHeight >= spineHeight * TIP_ZONE

    fun lever(grabHeight: Float, spineHeight: Float) = maxOf(grabHeight, spineHeight * TIP_ZONE)

    fun tiltFor(dragY: Float, grabHeight: Float, startTilt: Float): Float =
        atan(tan(startTilt) + dragY / grabHeight).coerceIn(0f, MAX_TILT)

    fun shouldTakeOut(tilt: Float, velocityY: Float) =
        tilt > TAKE_OUT_TILT || (velocityY > FLICK_VELOCITY && tilt > FLICK_TILT)
}
