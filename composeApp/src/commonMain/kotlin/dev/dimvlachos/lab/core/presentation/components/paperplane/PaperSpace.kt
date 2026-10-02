package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

// The paper in space: x right, y down the stage, z up out of the screen towards the eye. One eye
// for all of it, a camera's distance in front of the plane, so every facet is seen in the same
// perspective; each facet is flat, so the eye sees it through a 3 × 3 projection of the sheet.

internal class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)

    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)

    operator fun times(k: Float) = Vec3(x * k, y * k, z * k)

    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z

    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)

    fun unit(): Vec3 {
        val l = sqrt(this dot this)
        return if (l < 1e-9f) this else this * (1f / l)
    }
}

/**
 * Where the plane is: its balance point [at] on the stage and [lift] up off it towards the eye,
 * nose [heading] degrees clockwise from the right, [roll] degrees about its length, [scale] its
 * size. The eye stands [eye] in front of the stage.
 */
internal class Placement(
    val at: Offset,
    val heading: Float,
    val roll: Float,
    val scale: Float,
    val eye: Float,
    val lift: Float = 0f,
)

/** A facet in space: the sheet's point (x, y) is at [origin] + x [u] + y [v]. */
internal class FacetSpace(val origin: Vec3, val u: Vec3, val v: Vec3) {
    fun at(p: Offset) = origin + u * p.x + v * p.y

    /** Which way the paper faces: towards the eye where z is positive. */
    val normal: Vec3
        get() = (u cross v).unit()

    /**
     * The eye's view of the facet, row by row: the sheet's (x, y, 1) to the stage, up to scale.
     * Seen from [eye] in front of [centre], a point z towards it lies (eye / (eye - z)) further out
     * from the centre.
     */
    fun projection(centre: Offset, eye: Float): FloatArray =
        floatArrayOf(
            eye * u.x - centre.x * u.z,
            eye * v.x - centre.x * v.z,
            eye * origin.x - centre.x * origin.z,
            eye * u.y - centre.y * u.z,
            eye * v.y - centre.y * v.z,
            eye * origin.y - centre.y * origin.z,
            -u.z,
            -v.z,
            eye - origin.z,
        )
}

internal fun FloatArray.project(p: Offset): Offset {
    val w = this[6] * p.x + this[7] * p.y + this[8]
    return Offset(
        (this[0] * p.x + this[1] * p.y + this[2]) / w,
        (this[3] * p.x + this[4] * p.y + this[5]) / w,
    )
}

/**
 * The fold under way at [fold] (0 flat .. the plan's fold count, a dart) and how far it has gone.
 */
internal fun FoldPlan.stage(fold: Float): Pair<Int, Float> {
    val last = folds.size - 1
    val k = floor(fold).toInt().coerceIn(0, last)
    return k to ease(fold - k)
}

/** How far the wings have opened, 0 to 1: what turns the plane over from lying flat to flying. */
internal fun FoldPlan.wingsOpen(fold: Float): Float = ease(fold - (folds.size - 1))

/** Where [facet] is at [fold], placed on the stage by [placement]. */
internal fun FoldPlan.space(facet: Facet, fold: Float, placement: Placement): FacetSpace {
    val (k, progress) = stage(fold)
    val pose = facet.poses[k]
    val z = facet.layers[k] * PaperPlaneDimens.Thickness
    val crease = facet.moves[k]
    // In the flat stack before fold k, then turned about the crease, if fold k moves it.
    fun local(p: Offset): Vec3 {
        val flat = pose.map(p)
        val point = Vec3(flat.x, flat.y, z)
        if (crease == null) return point
        // A fold towards the eye hinges on the top of the stack, one away on its bottom, so the
        // paper it turns lands on that face.
        val hinge =
            if (crease.towardEye) (top[k] + 0.5f) * PaperPlaneDimens.Thickness
            else (bottom[k] - 0.5f) * PaperPlaneDimens.Thickness
        val angle = (if (crease.towardEye) 1f else -1f) * crease.angle * progress
        // About this axis, a positive turn lifts the paper beyond the crease towards the eye.
        val axis = Vec3(crease.side.y, -crease.side.x, 0f)
        return turn(point, Vec3(crease.at.x, crease.at.y, hinge), axis, angle)
    }
    val o = local(Offset.Zero)
    val u = local(Offset(1f, 0f)) - o
    val v = local(Offset(0f, 1f)) - o
    val b = Vec3(balance.x, balance.y, 0f)
    fun world(p: Vec3) = place(p, placement)
    fun worldDir(d: Vec3) = place(d, placement, direction = true)
    return FacetSpace(world(o - b), worldDir(u), worldDir(v))
}

// Turned about its length by the roll, then to its heading, sized, and set at its place.
private fun place(p: Vec3, placement: Placement, direction: Boolean = false): Vec3 {
    val r = placement.roll.toRadians()
    val rolled = Vec3(p.x, p.y * cos(r) - p.z * sin(r), p.y * sin(r) + p.z * cos(r))
    val h = placement.heading.toRadians()
    val headed =
        Vec3(rolled.x * cos(h) - rolled.y * sin(h), rolled.x * sin(h) + rolled.y * cos(h), rolled.z)
    val sized = headed * placement.scale
    return if (direction) sized else sized + Vec3(placement.at.x, placement.at.y, placement.lift)
}

// Rodrigues: [point] turned by [degrees] about the line through [through] along unit [axis].
private fun turn(point: Vec3, through: Vec3, axis: Vec3, degrees: Float): Vec3 {
    if (degrees == 0f) return point
    val t = degrees.toRadians()
    val v = point - through
    val c = cos(t)
    val s = sin(t)
    return through + v * c + (axis cross v) * s + axis * ((axis dot v) * (1f - c))
}

internal fun Float.toRadians() = this * PI.toFloat() / 180f

internal fun Float.toDegrees() = this * 180f / PI.toFloat()
