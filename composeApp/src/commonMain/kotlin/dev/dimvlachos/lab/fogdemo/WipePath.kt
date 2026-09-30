package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp

/**
 * Where a finger is [fraction] of the way through [path], a list of samples at even time steps: the
 * script, not the playback, decides how fast the hand moves.
 */
internal fun pointAt(path: List<Offset>, fraction: Float): Offset {
    if (path.size == 1) return path.first()
    val position = fraction.coerceIn(0f, 1f) * (path.size - 1)
    val i = position.toInt().coerceAtMost(path.size - 2)
    return lerp(path[i], path[i + 1], position - i)
}
