package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * One swing of the hand to its next turn, [to], bowing [bow] up the glass at the middle as a
 * forearm does, swinging from the elbow.
 */
internal class Sweep(val to: Offset, val bow: Float, val duration: Duration)

/** A finger that lands at [start] and scrubs back and forth through [sweeps] without lifting. */
internal class Scrub(val start: Offset, val sweeps: List<Sweep>) {
    val duration: Duration
        get() = sweeps.fold(Duration.ZERO) { total, sweep -> total + sweep.duration }

    /**
     * Where the finger is every [step], so the spacing of the points carries its speed: bunched at
     * the turns, spread out mid-sweep.
     */
    fun path(step: Duration = 16.milliseconds): List<Offset> {
        val points = mutableListOf(start)
        var from = start
        for ((index, sweep) in sweeps.withIndex()) {
            val steps = (sweep.duration / step).roundToInt()
            val swing = Swing(lands = index == 0, lifts = index == sweeps.lastIndex)
            for (i in 1..steps) points += sweep.pointAt(from, swing.progress(i.toFloat() / steps))
            from = sweep.to
        }
        return points
    }
}

/**
 * How far through a sweep the finger is at [time][progress]. A swinging arm moves like a pendulum,
 * fastest mid-sweep and still at each turn; but a finger lands on the glass and lifts off it
 * moving, so the sweep it [lands] on starts at full speed and the one it [lifts] from ends at it.
 */
private class Swing(val lands: Boolean, val lifts: Boolean) {
    fun progress(time: Float): Float =
        when {
            lands && lifts -> time
            lands -> sin(PI / 2 * time).toFloat()
            lifts -> (1 - cos(PI / 2 * time)).toFloat()
            else -> ((1 - cos(PI * time)) / 2).toFloat()
        }
}

private fun Sweep.pointAt(from: Offset, progress: Float): Offset {
    val chord = to - from
    val length = chord.getDistance()
    if (length == 0f) return to
    // Of the chord's two normals, the one pointing up the glass, whichever way the hand is going.
    val normal = Offset(-chord.y, chord.x) / length
    val up = if (normal.y > 0f) -normal else normal
    return lerp(from, to, progress) + up * (bow * 4 * progress * (1 - progress))
}
