package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import dev.dimvlachos.lab.fogdemo.presentation.components.wallGlassOn
import kotlin.math.min

/**
 * The mirror's glass in the recorded clip's 4:5 frame, width over height: the stage the clip's
 * choreography is drawn on.
 */
internal val ClipGlassAspect: Float = wallGlassOn(Size(4f, 5f)).let { it.width / it.height }

/**
 * Takes [point], a fraction of the clip's glass, to a fraction of [window], the frame sitting
 * centred in it as large as it fits. The choreography is drawn for the clip, so on a taller phone
 * the oval stays an oval rather than stretching with the screen.
 */
internal fun clipFrameToWindow(point: Offset, window: Size): Offset {
    val frameWidth = min(window.width, window.height * ClipGlassAspect)
    val frameHeight = frameWidth / ClipGlassAspect
    val left = (window.width - frameWidth) / 2
    val top = (window.height - frameHeight) / 2
    return Offset(
        (left + point.x * frameWidth) / window.width,
        (top + point.y * frameHeight) / window.height,
    )
}
