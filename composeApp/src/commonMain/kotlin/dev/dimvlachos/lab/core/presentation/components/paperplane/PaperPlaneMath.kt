package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// A fold eases in and out of its crease, the way a hand presses one.
internal fun ease(p: Float): Float {
    val t = p.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** One cubic piece of a throw, from [p0] to [p3]. */
internal class Cubic(val p0: Offset, val p1: Offset, val p2: Offset, val p3: Offset) {
    fun at(t: Float): Offset {
        val u = 1f - t
        return p0 * (u * u * u) + p1 * (3f * u * u * t) + p2 * (3f * u * t * t) + p3 * (t * t * t)
    }

    fun tangent(t: Float): Offset {
        val u = 1f - t
        return (p1 - p0) * (3f * u * u) + (p2 - p1) * (6f * u * t) + (p3 - p2) * (3f * t * t)
    }

    // How long, measured along it in short straight steps.
    val length: Float by lazy {
        var sum = 0f
        var last = p0
        for (i in 1..LengthSteps) {
            val next = at(i / LengthSteps.toFloat())
            sum += (next - last).getDistance()
            last = next
        }
        sum
    }
}

private const val LengthSteps = 32

/**
 * The throw: cubic pieces end to end, each smooth into the next, gone along by [at] its share of
 * the whole length, shared out among the pieces by theirs.
 */
internal class FlightPath(val pieces: List<Cubic>) {
    val length: Float = pieces.sumOf { it.length.toDouble() }.toFloat()
    private val shares: List<Float> = pieces.map { it.length / length }

    private fun locate(t: Float): Pair<Cubic, Float> {
        var start = 0f
        for ((i, piece) in pieces.withIndex()) {
            val share = shares[i]
            if (t <= start + share || i == pieces.lastIndex) {
                return piece to ((t - start) / share).coerceIn(0f, 1f)
            }
            start += share
        }
        return pieces.last() to 1f
    }

    val end: Offset
        get() = pieces.last().p3

    fun at(t: Float): Offset = locate(t).let { (piece, u) -> piece.at(u) }

    /** Which way the nose points, in degrees clockwise from the right: along the throw. */
    fun heading(t: Float): Float {
        val (piece, u) = locate(t)
        val d = piece.tangent(u)
        return atan2(d.y, d.x).toDegrees()
    }

    /**
     * How far the plane leans into its turn, in degrees: with how fast the heading swings, held to
     * a glider's bank, and level at both ends where it leaves the button and lands.
     */
    fun bank(t: Float): Float {
        val step = 0.01f
        val a = heading((t - step).coerceAtLeast(0f))
        val b = heading((t + step).coerceAtMost(1f))
        var swing = b - a
        if (swing > 180f) swing -= 360f
        if (swing < -180f) swing += 360f
        val rate = swing / (2f * step)
        val lean =
            (rate * PaperPlaneDimens.BankPerTurn).coerceIn(
                -PaperPlaneDimens.MaxBank,
                PaperPlaneDimens.MaxBank,
            )
        return lean * sin(PI.toFloat() * t)
    }
}

/**
 * From [from], leaving nose first along [heading], up the [stage] to the top of a loop over the
 * conversation, round and down to come in level from the left at [sweepFrom], straight over the
 * message to [sweepTo], and on out of the right of the stage, [beyond] past its edge. The loop's
 * height is measured in the room above the start, so with the keyboard up the plane still turns
 * below the top.
 */
internal fun flightPath(
    from: Offset,
    heading: Float,
    sweepFrom: Offset,
    sweepTo: Offset,
    beyond: Float,
    stage: Rect,
): FlightPath {
    val room = from.y - stage.top
    val top =
        Offset(
            stage.left + stage.width * PaperPlaneDimens.LoopAcross,
            from.y - room * PaperPlaneDimens.Climb,
        )
    val h = heading.toRadians()
    val nose = Offset(cos(h), sin(h))
    // Over the top of the loop it flies level, from right to left.
    val sweep = stage.width * PaperPlaneDimens.LoopSweep
    val up =
        Cubic(
            from,
            from + nose * (stage.width * PaperPlaneDimens.LaunchReach),
            top + Offset(sweep, 0f),
            top,
        )
    val down =
        Cubic(
            top,
            top - Offset(sweep, 0f),
            sweepFrom - Offset(stage.width * PaperPlaneDimens.ApproachReach, 0f),
            sweepFrom,
        )
    // Straight and level, its control points a third of the way apart, so it goes along it at an
    // even pace.
    val across = sweepTo - sweepFrom
    val over = Cubic(sweepFrom, sweepFrom + across / 3f, sweepTo - across / 3f, sweepTo)
    val out = Offset(stage.right + beyond, sweepTo.y - stage.width * PaperPlaneDimens.ExitClimb)
    val reach = out.x - sweepTo.x
    val away =
        Cubic(
            sweepTo,
            sweepTo + Offset(reach * 0.4f, 0f),
            Offset(sweepTo.x + reach * 0.7f, lerp(sweepTo.y, out.y, 0.5f)),
            out,
        )
    return FlightPath(listOf(up, down, over, away))
}

/**
 * How far along its throw a plane has gone, by the millisecond: thrown hard at [launchSpeed],
 * slowing over [approachMs] to come in at an even [sweepSpeed] over the [sweep], and speeding up
 * evenly to [exitSpeed] as it leaves. Distances are along the path, speeds in them a millisecond.
 */
internal class Pace(
    val approach: Float,
    val sweep: Float,
    val exit: Float,
    val approachMs: Float,
    val sweepSpeed: Float,
    launchSpeed: Float,
    exitSpeed: Float,
) {
    val sweepMs: Float = sweep / sweepSpeed
    private val exitFrom = sweepSpeed
    private val exitMs: Float = 2f * exit / (exitFrom + exitSpeed)
    private val exitGain = (exitSpeed - exitFrom) / exitMs
    val totalMs: Float = approachMs + sweepMs + exitMs

    // Thrown faster than it could go and still slow to its sweep in time, it would overshoot and
    // come back: held to three times its mean, the slowing never turns it round.
    private val launch = min(launchSpeed * approachMs, 3f * approach)
    private val arrive = min(sweepSpeed * approachMs, 3f * approach)

    fun distance(ms: Float): Float {
        if (ms <= 0f) return 0f
        if (ms < approachMs) {
            // A cubic from rest at the button to the sweep, at the given speeds at each end.
            val x = ms / approachMs
            val x2 = x * x
            val x3 = x2 * x
            return (x3 - 2f * x2 + x) * launch +
                (-2f * x3 + 3f * x2) * approach +
                (x3 - x2) * arrive
        }
        val over = ms - approachMs
        if (over < sweepMs) return approach + sweepSpeed * over
        val out = (over - sweepMs).coerceAtMost(exitMs)
        return approach + sweep + exitFrom * out + 0.5f * exitGain * out * out
    }

    /** When the plane is [across] (a share) of the way over the sweep, in ms from the throw. */
    fun overAt(across: Float): Float = approachMs + across * sweepMs
}
