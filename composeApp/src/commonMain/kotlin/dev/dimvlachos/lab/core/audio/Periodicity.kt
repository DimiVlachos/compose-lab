package dev.dimvlachos.lab.core.audio

import kotlin.math.max
import kotlin.math.sqrt

/**
 * How strongly [frame] repeats at a pitch between [minHz] and [maxHz]: the best normalised
 * correlation of the frame with itself shifted by one period. Near 1 for a voice, a hum or music,
 * which repeat; low for a blow, whose turbulent air does not. A light pre-emphasis first flattens a
 * blow's low rumble, so its slow swell cannot pass for a long period.
 */
internal fun periodicity(
    frame: FloatArray,
    sampleRate: Int,
    minHz: Float = 70f,
    maxHz: Float = 400f,
): Float {
    val x =
        FloatArray(frame.size) { i -> if (i == 0) frame[0] else frame[i] - 0.95f * frame[i - 1] }
    val minLag = (sampleRate / maxHz).toInt()
    val maxLag = (sampleRate / minHz).toInt().coerceAtMost(x.size / 2)
    var best = 0f
    for (lag in minLag..maxLag) {
        var xy = 0.0
        var xx = 0.0
        var yy = 0.0
        for (i in 0 until x.size - lag) {
            xy += x[i] * x[i + lag]
            xx += x[i] * x[i]
            yy += x[i + lag] * x[i + lag]
        }
        if (xx > 0.0 && yy > 0.0) best = max(best, (xy / sqrt(xx * yy)).toFloat())
    }
    return best
}
