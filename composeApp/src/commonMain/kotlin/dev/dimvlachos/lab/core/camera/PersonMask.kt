package dev.dimvlachos.lab.core.camera

import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * How opaque a pixel of the person's mask is, from the model's [confidence] it is the person: sure
 * of the room, clear; sure of the person, opaque; between, a short soft ramp, so hair blends.
 */
internal fun personAlpha(confidence: Float): Float {
    val t = ((confidence - EdgeFrom) / (EdgeTo - EdgeFrom)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

/** The frame shrunk to the model's own size, its shape kept: a smaller picture is quicker. */
internal fun segmentSize(width: Int, height: Int): IntSize {
    val scale = SegmentLongSide.toFloat() / max(width, height)
    return IntSize((width * scale).roundToInt(), (height * scale).roundToInt())
}

/**
 * When to stop looking for the person: the first frame may take a while as the model wakes up;
 * after that a frame is waited for briefly, and only many slow or failed frames in a row give up,
 * not one hiccup.
 */
internal class SegmentationHealth {
    private var warmedUp = false
    private var failures = 0

    /** How long to wait for the next frame's mask. */
    val waitMillis: Long
        get() = if (warmedUp) FrameWaitMillis else WarmUpWaitMillis

    /** Given up: frames show whole from now on. */
    val givenUp: Boolean
        get() = failures >= FailuresInARow

    fun succeeded() {
        warmedUp = true
        failures = 0
    }

    fun failed() {
        failures++
    }
}

private const val EdgeFrom = 0.35f
private const val EdgeTo = 0.75f
private const val SegmentLongSide = 256
private const val WarmUpWaitMillis = 3_000L
private const val FrameWaitMillis = 500L
private const val FailuresInARow = 5
