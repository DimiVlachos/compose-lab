package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.time.Duration

/** How one sweep of an [ovalScrub] differs from the rest: a hand never repeats itself exactly. */
internal class SweepFeel(val bow: Float, val duration: Duration, val reach: Float = 1f)

/**
 * A hand clearing an oval: it lands at the top of the oval of [radii] about [centre] and scrubs
 * down to the bottom, turning on alternate sides of the oval, so the sweeps are short at the top
 * and bottom and longest through the middle. One sweep per [feel], its turn pushed out or pulled in
 * by the feel's reach. The rows are close enough that the brush joins them into one clear oval.
 */
internal fun ovalScrub(centre: Offset, radii: Offset, feel: List<SweepFeel>): Scrub {
    fun turn(i: Int, reach: Float): Offset {
        // From -1 at the top of the oval to 1 at the bottom, the rows closer together where the
        // oval narrows to its top and bottom, as a hand works into a curve.
        val t = i.toFloat() / feel.size
        val height = -(CapCrowding * cos(PI * t).toFloat() + (1f - CapCrowding) * (1f - 2f * t))
        val side = if (i % 2 == 1) 1f else -1f
        val halfWidth = radii.x * sqrt(1f - height * height) * reach
        return Offset(centre.x + side * halfWidth, centre.y + height * radii.y)
    }
    return Scrub(
        start = turn(0, reach = 1f),
        sweeps = feel.mapIndexed { i, it -> Sweep(turn(i + 1, it.reach), it.bow, it.duration) },
    )
}

// How much of the rows' spacing follows the oval's curve rather than an even step.
private const val CapCrowding = 0.6f
