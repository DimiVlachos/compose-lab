package dev.dimvlachos.lab.core.camera

/** How far a dim frame is brightened at most: past this it is more noise than face. */
const val MaxCameraGain = 6f

// The average brightness, 0 to 1, a frame is lifted towards.
private const val TargetBrightness = 0.4f

// How much of the way to the new gain one frame goes, so the picture never flickers.
private const val Easing = 0.1f

/**
 * The gain for the next camera frame, from the last one and this frame's [meanBrightness] (0 to 1):
 * a dim room's frames brightened towards a normal level, a bright room's left alone.
 */
fun autoGain(previous: Float, meanBrightness: Float): Float {
    val wanted =
        (TargetBrightness / meanBrightness.coerceAtLeast(1e-4f)).coerceIn(1f, MaxCameraGain)
    return previous + (wanted - previous) * Easing
}
