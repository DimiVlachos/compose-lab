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
        FloatArray(9).also { projectInto(it, centre, eye) }

    /** [projection], written into [into] so a frame needs no new array. */
    fun projectInto(into: FloatArray, centre: Offset, eye: Float) {
        into[0] = eye * u.x - centre.x * u.z
        into[1] = eye * v.x - centre.x * v.z
        into[2] = eye * origin.x - centre.x * origin.z
        into[3] = eye * u.y - centre.y * u.z
        into[4] = eye * v.y - centre.y * v.z
        into[5] = eye * origin.y - centre.y * origin.z
        into[6] = -u.z
        into[7] = -v.z
        into[8] = eye - origin.z
    }
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
    return Pose(placement).place(FacetSpace(o - b, u, v))
}

/**
 * Every facet of the finished dart about its balance point, before it is placed: the dart's shape
 * never changes in the air, so it is worked out once and only turned and set in place each frame.
 */
internal fun FoldPlan.folded(): List<FacetSpace> = facets.map {
    space(it, folds.size.toFloat(), AtRest)
}

private val AtRest = Placement(Offset.Zero, 0f, 0f, 1f, 1f)

/**
 * A [Placement] as a move in space: turned about its length by the roll, then to its heading,
 * sized, and set at its place, its turns worked out once for every point it moves.
 */
internal class Pose(placement: Placement) {
    private val cr = cos(placement.roll.toRadians())
    private val sr = sin(placement.roll.toRadians())
    private val ch = cos(placement.heading.toRadians())
    private val sh = sin(placement.heading.toRadians())
    private val scale = placement.scale
    private val shift = Vec3(placement.at.x, placement.at.y, placement.lift)

    fun direction(p: Vec3): Vec3 {
        val y = p.y * cr - p.z * sr
        val z = p.y * sr + p.z * cr
        return Vec3((p.x * ch - y * sh) * scale, (p.x * sh + y * ch) * scale, z * scale)
    }

    fun point(p: Vec3): Vec3 = direction(p) + shift

    fun place(space: FacetSpace) =
        FacetSpace(point(space.origin), direction(space.u), direction(space.v))
}

/**
 * The order to draw the facets in, back to front, seen from [eye]: each rigid part of the dart (the
 * keel and each wing) is a flat stack of paper, so within it the layers lie in order, the nearest
 * the eye on top; the parts themselves go in by how far off they are. By the facets' own middles
 * alone, the stacks' layers, a hair apart, would be ordered by their shapes instead, and a flap
 * folded over a wing drawn under it.
 */
internal fun FoldPlan.drawOrder(spaces: List<FacetSpace>, eye: Vec3): List<Int> {
    val last = folds.size - 1
    val parts = facets.map { facet -> folds[last].creases.indexOf(facet.moves[last]) + 1 }
    val middles = facets.mapIndexed { i, facet -> spaces[i].at(facet.middle) }
    val depth =
        parts.distinct().associateWith { part ->
            parts.indices.filter { parts[it] == part }.map { middles[it].z }.average()
        }
    // Which way a stack's layers rise, in space: its paper's normal, the right way round for the
    // mirror its pose may hold.
    val rising =
        parts.distinct().associateWith { part ->
            val i = parts.indexOf(part)
            val pose = facets[i].poses[last]
            val up = spaces[i].normal * (if (pose.a * pose.d - pose.b * pose.c < 0f) -1f else 1f)
            ((eye - middles[i]) dot up) > 0f
        }
    return facets.indices.sortedWith(
        compareBy<Int> { depth.getValue(parts[it]) }
            .thenBy { i ->
                val layer = facets[i].layers[last]
                if (rising.getValue(parts[i])) layer else -layer
            }
    )
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
