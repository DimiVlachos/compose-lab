package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp
import kotlin.math.atan
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

    // How far along it each of a run of short straight steps ends: what its length is, and how
    // its parameter is found for a distance along it.
    private val reached =
        FloatArray(LengthSteps + 1).also { reached ->
            var last = p0
            for (i in 1..LengthSteps) {
                val next = at(i / LengthSteps.toFloat())
                reached[i] = reached[i - 1] + (next - last).getDistance()
                last = next
            }
        }

    val length: Float = reached[LengthSteps]

    /**
     * The parameter [share] of the way along it by distance: a cubic's own parameter runs faster
     * where its control points are further apart, so it is not that.
     */
    fun parameterAt(share: Float): Float {
        if (length <= 0f) return share.coerceIn(0f, 1f)
        val goal = share.coerceIn(0f, 1f) * length
        var lo = 0
        var hi = LengthSteps
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (reached[mid] < goal) lo = mid else hi = mid
        }
        val span = reached[hi] - reached[lo]
        val within = if (span > 0f) (goal - reached[lo]) / span else 0f
        return (lo + within) / LengthSteps
    }
}

private const val LengthSteps = 128

/**
 * The throw: cubic pieces end to end, each smooth into the next, gone along by [at] its share of
 * the whole length by distance, so a pace along it is a pace on the screen.
 */
internal class FlightPath(val pieces: List<Cubic>) {
    val length: Float = pieces.sumOf { it.length.toDouble() }.toFloat()
    private val shares: List<Float> = pieces.map { if (length > 0f) it.length / length else 0f }

    // The piece [t] falls in, left in [found] with the parameter along it; a piece with no
    // length is passed over, there being no time on it.
    private var found: Cubic = pieces.first()

    private fun locate(t: Float): Float {
        var start = 0f
        for ((i, piece) in pieces.withIndex()) {
            val share = shares[i]
            if ((share > 0f && t <= start + share) || i == pieces.lastIndex) {
                found = piece
                val along = if (share > 0f) (t - start) / share else 1f
                return piece.parameterAt(along)
            }
            start += share
        }
        found = pieces.last()
        return 1f
    }

    fun at(t: Float): Offset {
        val u = locate(t)
        return found.at(u)
    }

    /** Which way the nose points, in degrees clockwise from the right: along the throw. */
    fun heading(t: Float): Float {
        val u = locate(t)
        val d = found.tangent(u)
        return atan2(d.y, d.x).toDegrees()
    }

    /**
     * How sharply the throw turns at [t]: radians of heading for each unit along it, the same way
     * round as the heading swings.
     */
    fun curvature(t: Float): Float {
        val step = 0.004f
        val from = (t - step).coerceAtLeast(0f)
        val to = (t + step).coerceAtMost(1f)
        var swing = heading(to) - heading(from)
        if (swing > 180f) swing -= 360f
        if (swing < -180f) swing += 360f
        return swing.toRadians() / ((to - from) * length)
    }

    /**
     * How far the plane leans into its turn at [t], going [speed] along it, in degrees, the same
     * way round as the heading swings: as a glider does, just far enough that its lift holds it in
     * the turn against [gravity] (tan bank = speed² × curvature / gravity), so it lies level on a
     * straight and leans the more the faster and tighter it turns, up to its steepest.
     */
    fun bank(t: Float, speed: Float, gravity: Float): Float =
        atan(speed * speed * curvature(t) / gravity)
            .toDegrees()
            .coerceIn(-PaperPlaneDimens.MaxBank, PaperPlaneDimens.MaxBank)
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
            // Level into the sweep, from no further left than keeps the plane on the stage: for a
            // message that starts near the edge it comes in the steeper, never from behind it.
            sweepFrom -
                Offset(
                    (sweepFrom.x - stage.left - beyond / 2f).coerceIn(
                        stage.width * PaperPlaneDimens.ShortestApproach,
                        stage.width * PaperPlaneDimens.ApproachReach,
                    ),
                    0f,
                ),
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
    approachMs: Float,
    val sweepSpeed: Float,
    launchSpeed: Float,
    exitSpeed: Float,
) {
    /**
     * How long it takes to come round to the sweep: [approachMs] at most, less for a loop too short
     * to arrive at the sweep's speed in that time without turning back.
     */
    val approachMs: Float = min(approachMs, 3f * approach / sweepSpeed)
    val sweepMs: Float = sweep / sweepSpeed
    private val exitFrom = sweepSpeed
    private val exitMs: Float = if (exit > 0f) 2f * exit / (exitFrom + exitSpeed) else 0f
    private val exitGain = if (exitMs > 0f) (exitSpeed - exitFrom) / exitMs else 0f
    val totalMs: Float = this.approachMs + sweepMs + exitMs

    // Thrown faster than it could go and still slow to its sweep in time, it would overshoot and
    // come back: held to three times its mean, the slowing never turns it round.
    private val launch = min(launchSpeed * this.approachMs, 3f * approach)
    private val arrive = sweepSpeed * this.approachMs

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
