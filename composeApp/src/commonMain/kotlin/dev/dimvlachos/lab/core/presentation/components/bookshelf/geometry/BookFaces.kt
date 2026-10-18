package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

internal enum class FaceTexture {
    Spine,
    Back,
    Cover,
    Head,
    Fore,
    RightPage,
    LeftPage,
}

internal data class UvRect(
    val u0: Float = 0f,
    val v0: Float = 0f,
    val u1: Float = 1f,
    val v1: Float = 1f,
)

internal data class ProjectedFace(
    val texture: FaceTexture,
    val uv: UvRect,
    val corners: List<Point2>,
    val normal: Vec3,
    val depth: Float,
    val inFront: List<Point2> = corners,
    val insideRow: List<Point2> = emptyList(),
)

internal fun buildBookFaces(pose: BookTransform, readFraction: Float?): List<ProjectedFace> {
    val (t, h, d) = pose.dimensions
    val alpha = coverAngle(pose.open)
    val hingeAt = hingeX(t, readFraction)
    val hinge = Hinge(hingeAt, cos(alpha), sin(alpha))
    val faces = mutableListOf<ProjectedFace>()

    fun add(texture: FaceTexture, uv: UvRect, corners: List<Vec3>, normal: Vec3, hinged: Boolean) {
        val world = corners.map { pose.map(if (hinged) hinge.point(it) else it) }
        val n = pose.mapDirection(if (hinged) hinge.direction(normal) else normal)
        val centre = (world[0] + world[2]) * 0.5f
        val camera = pose.camera
        val eye =
            Vec3(
                camera.vanishingX - centre.x,
                camera.vanishingY - centre.y,
                camera.focal - centre.z,
            )
        if (n dot eye <= 0f) return
        faces +=
            ProjectedFace(
                texture = texture,
                uv = uv,
                corners = world.map(camera::project),
                normal = n.normalized(),
                depth = centre.z,
                inFront = world.inFrontOfRow(pose::rowDistance).map(camera::project),
                insideRow = world.insideRow(pose::rowDistance).map(camera::project),
            )
    }

    add(
        FaceTexture.Spine,
        UvRect(),
        quad(0f, h, 0f, t, h, 0f, t, 0f, 0f, 0f, 0f, 0f),
        Vec3(0f, 0f, 1f),
        false,
    )
    add(
        FaceTexture.Back,
        UvRect(),
        quad(0f, h, -d, 0f, h, 0f, 0f, 0f, 0f, 0f, 0f, -d),
        Vec3(-1f, 0f, 0f),
        false,
    )

    if (pose.open <= 0f) {
        add(
            FaceTexture.Cover,
            UvRect(),
            quad(t, h, 0f, t, h, -d, t, 0f, -d, t, 0f, 0f),
            Vec3(1f, 0f, 0f),
            false,
        )
        add(
            FaceTexture.Head,
            UvRect(),
            quad(0f, h, -d, t, h, -d, t, h, 0f, 0f, h, 0f),
            Vec3(0f, 1f, 0f),
            false,
        )
        add(
            FaceTexture.Head,
            UvRect(),
            quad(0f, 0f, 0f, t, 0f, 0f, t, 0f, -d, 0f, 0f, -d),
            Vec3(0f, -1f, 0f),
            false,
        )
        add(
            FaceTexture.Fore,
            UvRect(),
            quad(t, h, -d, 0f, h, -d, 0f, 0f, -d, t, 0f, -d),
            Vec3(0f, 0f, -1f),
            false,
        )
    } else {
        val f = hingeAt / t
        val x = hingeAt
        add(
            FaceTexture.RightPage,
            UvRect(),
            quad(x, h, 0f, x, h, -d, x, 0f, -d, x, 0f, 0f),
            Vec3(1f, 0f, 0f),
            false,
        )
        add(
            FaceTexture.Head,
            UvRect(u1 = f),
            quad(0f, h, -d, x, h, -d, x, h, 0f, 0f, h, 0f),
            Vec3(0f, 1f, 0f),
            false,
        )
        add(
            FaceTexture.Head,
            UvRect(u1 = f),
            quad(0f, 0f, 0f, x, 0f, 0f, x, 0f, -d, 0f, 0f, -d),
            Vec3(0f, -1f, 0f),
            false,
        )
        add(
            FaceTexture.Fore,
            UvRect(u0 = 1f - f),
            quad(x, h, -d, 0f, h, -d, 0f, 0f, -d, x, 0f, -d),
            Vec3(0f, 0f, -1f),
            false,
        )
        add(
            FaceTexture.Cover,
            UvRect(),
            quad(t, h, 0f, t, h, -d, t, 0f, -d, t, 0f, 0f),
            Vec3(1f, 0f, 0f),
            true,
        )
        add(
            FaceTexture.LeftPage,
            UvRect(),
            quad(x, h, -d, x, h, 0f, x, 0f, 0f, x, 0f, -d),
            Vec3(-1f, 0f, 0f),
            true,
        )
        add(
            FaceTexture.Head,
            UvRect(u0 = f),
            quad(x, h, -d, t, h, -d, t, h, 0f, x, h, 0f),
            Vec3(0f, 1f, 0f),
            true,
        )
        add(
            FaceTexture.Head,
            UvRect(u0 = f),
            quad(x, 0f, 0f, t, 0f, 0f, t, 0f, -d, x, 0f, -d),
            Vec3(0f, -1f, 0f),
            true,
        )
        add(
            FaceTexture.Fore,
            UvRect(u1 = 1f - f),
            quad(t, h, -d, x, h, -d, x, 0f, -d, t, 0f, -d),
            Vec3(0f, 0f, -1f),
            true,
        )
    }
    return faces.sortedBy { it.depth }
}

internal const val RIBBON_NEAR = 0.072f
internal const val RIBBON_FAR = 0.1f

internal fun ribbonAnchors(
    pose: BookPose,
    readFraction: Float?,
    onLeftPage: Boolean = false,
): Pair<Point2, Point2> {
    val (t, _, d) = pose.dimensions
    val x = hingeX(t, readFraction)
    val alpha = coverAngle(pose.open)
    val hinge = Hinge(x, cos(alpha), sin(alpha))
    fun anchor(depth: Float): Point2 {
        val local = Vec3(x, 0f, -d * depth)
        return pose.camera.project(pose.map(if (onLeftPage) hinge.point(local) else local))
    }
    return anchor(RIBBON_NEAR) to anchor(RIBBON_FAR)
}

private class Hinge(private val at: Float, private val cosA: Float, private val sinA: Float) {
    fun point(p: Vec3): Vec3 {
        val a = p.x - at
        return Vec3(at + a * cosA - p.z * sinA, p.y, a * sinA + p.z * cosA)
    }

    fun direction(n: Vec3) = Vec3(n.x * cosA - n.z * sinA, n.y, n.x * sinA + n.z * cosA)
}

@Suppress("LongParameterList")
private fun quad(
    x0: Float,
    y0: Float,
    z0: Float,
    x1: Float,
    y1: Float,
    z1: Float,
    x2: Float,
    y2: Float,
    z2: Float,
    x3: Float,
    y3: Float,
    z3: Float,
) = listOf(Vec3(x0, y0, z0), Vec3(x1, y1, z1), Vec3(x2, y2, z2), Vec3(x3, y3, z3))

internal fun List<Vec3>.inFrontOfRow(distance: (Vec3) -> Float = { it.z }): List<Vec3> =
    clipAtRow(distance, inFront = true)

internal fun List<Vec3>.insideRow(distance: (Vec3) -> Float = { it.z }): List<Vec3> =
    clipAtRow(distance, inFront = false)

private fun List<Vec3>.clipAtRow(distance: (Vec3) -> Float, inFront: Boolean): List<Vec3> {
    val side = map { if (inFront) distance(it) else -distance(it) }
    if (side.all { it >= 0f }) return this
    val clipped = mutableListOf<Vec3>()
    for (i in indices) {
        val j = (i + 1) % size
        val a = this[i]
        val b = this[j]
        if (side[i] >= 0f) clipped += a
        if (side[i] * side[j] < 0f) clipped += mix(a, b, side[i] / (side[i] - side[j]))
    }
    return if (clipped.size >= 3) clipped else emptyList()
}

internal fun coverAngle(open: Float): Float = acos(1f - 2f * smoothStep(0f, 1f, open))

internal data class ShadowShape(val outline: List<Point2>, val height: Float)

internal fun bookShadow(
    pose: BookPose,
    readFraction: Float?,
    light: Point2,
    reach: Float,
): ShadowShape {
    val (t, h, d) = pose.dimensions
    val hingeAt = hingeX(t, readFraction)
    val alpha = coverAngle(pose.open)
    val hinge = Hinge(hingeAt, cos(alpha), sin(alpha))
    val corners =
        listOf(0f, h).flatMap { y ->
            listOf(0f, -d).flatMap { z ->
                listOf(
                    Vec3(0f, y, z),
                    Vec3(hingeAt, y, z),
                    hinge.point(Vec3(t, y, z)),
                )
            }
        }
    var total = 0f
    val cast = corners.map { local ->
        val p = pose.map(local)
        val lift = reach * (1f - exp(-pose.rowDistance(p).coerceAtLeast(0f) / reach))
        total += lift
        Point2(p.x + light.x * lift, p.y + light.y * lift)
    }
    return ShadowShape(convexHull(cast), total / cast.size)
}

internal fun convexHull(points: List<Point2>): List<Point2> {
    val sorted = points.sortedWith(compareBy({ it.x }, { it.y }))
    if (sorted.size < 3) return sorted
    fun cross(o: Point2, a: Point2, b: Point2) =
        (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    val hull = ArrayList<Point2>(sorted.size * 2)
    for (pass in 0..1) {
        val start = hull.size
        for (p in if (pass == 0) sorted else sorted.asReversed()) {
            while (hull.size >= start + 2 && cross(hull[hull.size - 2], hull.last(), p) <= 0f) {
                hull.removeAt(hull.lastIndex)
            }
            hull += p
        }
        hull.removeAt(hull.lastIndex)
    }
    return hull
}
