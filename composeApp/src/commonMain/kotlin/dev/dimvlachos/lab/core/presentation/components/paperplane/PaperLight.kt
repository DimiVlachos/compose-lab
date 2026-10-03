package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// How paper is lit. Most of the light on it is the room's, from every side at once: a sheet turned
// any way is as white, but where the dart's own paper stands over it, as in the V between its folds
// or under a wing, less of the room reaches it and it falls into shade. A little is the window's,
// from one side, so a face turned to it is a touch brighter than one turned from it.

/** How much of the room each side of a facet sees, 0 none to 1 all: [front] along its normal. */
internal class Openness(val front: Float, val back: Float) {
    fun of(frontSeen: Boolean) = if (frontSeen) front else back
}

/**
 * How much of the room each side of each facet of the folded dart sees: rays out from points across
 * it, spread over its side as light falls on it, the share of them no other part of the dart stands
 * in the way of. The dart is rigid in the air, so this is worked out the once.
 */
internal fun FoldPlan.openness(): List<Openness> {
    val spaces = folded()
    val parts = parts()
    // Each facet as its plane and its outline on the sheet, to find where a ray meets it.
    fun blocked(from: Vec3, along: Vec3, part: Int): Boolean =
        facets.indices.any { j ->
            if (parts[j] == part) return@any false
            val space = spaces[j]
            val n = space.u cross space.v
            val facing = along dot n
            if (abs(facing) < 1e-6f) return@any false
            val t = ((space.origin - from) dot n) / facing
            if (t <= 0f) return@any false
            val hit = from + along * t - space.origin
            facets[j].polygon.holds(Offset(hit dot space.u, hit dot space.v))
        }
    return facets.mapIndexed { i, facet ->
        val space = spaces[i]
        val n = space.normal
        val points = facet.samplePoints().map(space::at)
        fun seen(side: Vec3): Float {
            var open = 0
            var total = 0
            for (p in points) {
                val from = p + side * Lift
                for (ray in rays(side)) {
                    total++
                    if (!blocked(from, ray, parts[i])) open++
                }
            }
            return open.toFloat() / total
        }
        Openness(seen(n), seen(n * -1f))
    }
}

/**
 * How much brighter than paper lying flat on the screen in the open a facet facing [normal] is, as
 * a share, seeing [open] of the room and lit by the window along [light]: the room's light by how
 * much of the room it sees, the window's falling off with the cosine of its angle to it. Seen from
 * either side, it is the side facing the eye that counts.
 */
internal fun brightness(normal: Vec3, light: Vec3, open: Float = 1f): Float {
    val n = if (normal.z < 0f) normal * -1f else normal
    val room = PaperPlaneDimens.RoomLight
    val window = PaperPlaneDimens.WindowLight
    val flat = room + window * light.z
    val lit = room * open + window * open * max(0f, n dot light)
    return lit / flat - 1f
}

// Where on a facet the room is looked at from: its middle, and halfway from it to each corner.
private fun Facet.samplePoints(): List<Offset> = listOf(middle) + polygon.map { (it + middle) / 2f }

// Whether the convex [this] holds [p], its corners either way round.
private fun List<Offset>.holds(p: Offset): Boolean {
    var sign = 0
    for (k in indices) {
        val a = this[k]
        val b = this[(k + 1) % size]
        val cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
        if (abs(cross) < 1e-6f) continue
        val s = if (cross > 0f) 1 else -1
        if (sign == 0) sign = s else if (s != sign) return false
    }
    return true
}

// Rays over the side facing [up], spread as the light a surface takes in falls on it, most of them
// steep, few grazing: a spiral of them over the disc, raised onto the dome above it.
private fun rays(up: Vec3): List<Vec3> {
    val helper = if (abs(up.x) < 0.9f) Vec3(1f, 0f, 0f) else Vec3(0f, 1f, 0f)
    val a = (up cross helper).unit()
    val b = up cross a
    return List(RayCount) { k ->
        val r = sqrt((k + 0.5f) / RayCount)
        val angle = k * GoldenAngle
        val x = r * cos(angle)
        val y = r * sin(angle)
        val z = sqrt(max(0f, 1f - x * x - y * y))
        a * x + b * y + up * z
    }
}

// Rays from each point, and how far off its paper they start, in sheet units: past the layers
// stacked on it, which are its own part and never shade it.
private const val RayCount = 96
private const val Lift = 0.05f
private val GoldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat()
