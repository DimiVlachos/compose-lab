package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** One stroke of a drawing: where the fingertip is at even time steps, and how long it takes. */
internal class DrawnStroke(val path: List<Offset>, val duration: Duration)

/**
 * A heart pierced by an arrow, drawn on fogged glass with a fingertip, in fractions of the clip's
 * frame, stroke by stroke as a hand draws it: the heart in one go from the dip at its top, round
 * the right lobe, down to the point and up the left; the shaft from lower left to upper right in
 * two pieces, up to the heart and out beyond it, left out inside so the arrow pierces it rather
 * than lying on top; two strokes of arrowhead at the tip; two feathers at the tail.
 */
internal fun heartWithArrow(step: Duration = 16.milliseconds): List<DrawnStroke> {
    val tail = Offset(0.18f, 0.76f)
    val tip = Offset(0.84f, 0.18f)
    val angle = atan2(tip.y - tail.y, tip.x - tail.x)
    val along = Offset(cos(angle), sin(angle))
    val across = Offset(-along.y, along.x)
    fun line(from: Offset, to: Offset, duration: Duration) =
        timed(duration, step) { t -> from + (to - from) * easeInOut(t) }
    fun head(side: Float) =
        line(tip - rotate(along, side * HeadSpread) * HeadLength, tip, 280.milliseconds)
    // A feather is a V pointing to the tip: in from one side, through the shaft, out the other.
    fun feather(distanceFromTail: Float): DrawnStroke {
        val base = tail + along * distanceFromTail
        val back = along * -FeatherSweep
        val from = base + across * FeatherHalfWidth + back
        val to = base - across * FeatherHalfWidth + back
        return timed(300.milliseconds, step) { t ->
            if (t < 0.5f) from + (base - from) * easeInOut(t * 2)
            else base + (to - base) * easeInOut(t * 2 - 1)
        }
    }
    val heart = heart(step)
    val (entry, exit) = crossings(heart.path, tail, tip)
    return listOf(
        heart,
        line(tail, entry, 450.milliseconds),
        line(exit, tip, 400.milliseconds),
        head(1f),
        head(-1f),
        feather(0.02f),
        feather(0.06f),
    )
}

// The classic heart curve, scaled into the frame, starting at the dip; a slight wobble of the hand.
private fun heart(step: Duration): DrawnStroke =
    timed(2_200.milliseconds, step) { progress ->
        val t = 2 * PI * progress
        val x = 16 * sin(t).pow(3)
        val y = 13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)
        val wobble = (sin(progress * 23.0) * HeartWobble).toFloat()
        Offset(
            HeartCentre.x + (x / 17 * HeartSize).toFloat() + wobble,
            HeartCentre.y - (y / 17 * HeartSize * HeartStretch).toFloat(),
        )
    }

// Where the line from [from] to [to] first meets the outline and where it last leaves it.
private fun crossings(outline: List<Offset>, from: Offset, to: Offset): Pair<Offset, Offset> {
    val direction = to - from
    val along =
        outline.zipWithNext().mapNotNull { (a, b) ->
            val edge = b - a
            val denominator = direction.x * edge.y - direction.y * edge.x
            if (denominator == 0f) return@mapNotNull null
            val start = a - from
            val onLine = (start.x * edge.y - start.y * edge.x) / denominator
            val onEdge = (start.x * direction.y - start.y * direction.x) / denominator
            onLine.takeIf { it in 0f..1f && onEdge in 0f..1f }
        }
    return from + direction * along.min() to from + direction * along.max()
}

private fun timed(duration: Duration, step: Duration, at: (Float) -> Offset): DrawnStroke {
    val steps = (duration / step).roundToInt().coerceAtLeast(1)
    return DrawnStroke(List(steps + 1) { at(it.toFloat() / steps) }, duration)
}

private fun easeInOut(t: Float) = ((1 - cos(PI * t)) / 2).toFloat()

private fun rotate(v: Offset, radians: Float) =
    Offset(v.x * cos(radians) - v.y * sin(radians), v.x * sin(radians) + v.y * cos(radians))

private val HeartCentre = Offset(0.5f, 0.44f)
private const val HeartSize = 0.24
private const val HeartStretch = 0.95
private const val HeartWobble = 0.003
private const val HeadLength = 0.085f
private const val HeadSpread = 0.45f
private const val FeatherHalfWidth = 0.06f
private const val FeatherSweep = 0.05f
