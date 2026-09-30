package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The part of an [image] that covers a [window] of another shape: the largest centred slice with
 * the window's proportions, so a texture is cropped to fit, never stretched, and its droplets stay
 * round on a tall phone and in the clip's 4:5 frame alike.
 */
internal fun coverCrop(image: IntSize, window: Size): Pair<IntOffset, IntSize> {
    val scale = max(window.width / image.width, window.height / image.height)
    val width = (window.width / scale).roundToInt().coerceAtMost(image.width)
    val height = (window.height / scale).roundToInt().coerceAtMost(image.height)
    return IntOffset((image.width - width) / 2, (image.height - height) / 2) to
        IntSize(width, height)
}
