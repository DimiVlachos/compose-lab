package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.min

/** The recorded clip's frame, width over height. */
private const val ClipAspect = 4f / 5f

/**
 * Takes [point], a fraction of the clip's 4:5 frame, to a fraction of [window], the frame sitting
 * centred in it as large as it fits. The choreography is drawn for the clip, so on a taller phone
 * the oval stays an oval rather than stretching with the screen.
 */
internal fun clipFrameToWindow(point: Offset, window: Size): Offset {
    val frameWidth = min(window.width, window.height * ClipAspect)
    val frameHeight = frameWidth / ClipAspect
    val left = (window.width - frameWidth) / 2
    val top = (window.height - frameHeight) / 2
    return Offset(
        (left + point.x * frameWidth) / window.width,
        (top + point.y * frameHeight) / window.height,
    )
}
