package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.perspective.Homography
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point or a direction in book space. */
internal data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)

    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)

    operator fun times(k: Float) = Vec3(x * k, y * k, z * k)

    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z

    operator fun unaryMinus() = Vec3(-x, -y, -z)
}

/**
 * The eye over a book [width] by [height] px. Book space has x along the gutter (rightwards), y
 * across it towards the far stack and z up off the table, in book units; the eye is pitched down
 * onto the table and draws the book's middle at ([cx], [cy]).
 */
internal class BookCamera(val width: Float, val height: Float) {
    // The book fills the width, unless the frame is shorter than the book's own shape; then it
    // fits the height instead, centred, so it never spills out of its frame.
    val pxPerUnit: Float = minOf(width, height / PopUpDimens.Aspect) / PopUpDimens.UnitsAcross
    val cx: Float = width / 2f
    val cy: Float = height * PopUpDimens.CentreY
    val perspective: Float = PopUpDimens.CameraDistance * pxPerUnit

    private val cosPitch = cos(PopUpDimens.CameraPitch)
    private val sinPitch = sin(PopUpDimens.CameraPitch)

    /** Writes into [out] the homography of the plane [o] + u · [u] + v · [v], in book units. */
    fun plane(o: Vec3, u: Vec3, v: Vec3, out: FloatArray): FloatArray =
        plane(o.x, o.y, o.z, u.x, u.y, u.z, v.x, v.y, v.z, out)

    /** [plane], component by component. */
    fun plane(
        ox: Float,
        oy: Float,
        oz: Float,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        out: FloatArray,
    ): FloatArray {
        val g = pxPerUnit
        return Homography.of(
            ox = cx + g * ox,
            oy = cy - g * up(oy, oz),
            oz = -g * toward(oy, oz),
            ux = g * ux,
            uy = -g * up(uy, uz),
            uz = -g * toward(uy, uz),
            vx = g * vx,
            vy = -g * up(vy, vz),
            vz = -g * toward(vy, vz),
            cx = cx,
            cy = cy,
            perspective = perspective,
            out = out,
        )
    }

    /** Where the eye sees [p]. */
    fun project(p: Vec3): Offset {
        val s = PopUpDimens.CameraDistance / (PopUpDimens.CameraDistance - toward(p.y, p.z))
        return Offset(cx + pxPerUnit * p.x * s, cy - pxPerUnit * up(p.y, p.z) * s)
    }

    // Up the screen, and towards the eye, once the book is pitched.
    private fun up(y: Float, z: Float) = y * cosPitch + z * sinPitch

    private fun toward(y: Float, z: Float) = -y * sinPitch + z * cosPitch
}

internal object PopUpMath {
    /** The angle at which a leaf points straight at the eye. */
    val CameraAngle: Float = (PI / 2).toFloat() - PopUpDimens.CameraPitch

    /** Draw codes from this up are spreads: [Spread] + j is spread j's pieces. */
    const val Spread = 1000

    private val pi = PI.toFloat()
    private val light: Vec3 = run {
        val x = -0.3f
        val y = -0.45f
        val z = 1f
        val n = sqrt(x * x + y * y + z * z)
        Vec3(x / n, y / n, z / n)
    }

    /** The direction from the gutter across a leaf lying at [angle]: 0 on the near stack. */
    fun across(angle: Float): Vec3 = Vec3(0f, -cos(angle), sin(angle))

    /** The side of a leaf at [angle] that faces up when it lies on the near stack. */
    fun leafNormal(angle: Float): Vec3 = Vec3(0f, sin(angle), cos(angle))

    fun leafFrontSeen(angle: Float): Boolean = angle < CameraAngle

    /**
     * Which way a piece stands between a spread's [near] and [far] pages: along their bisector, so
     * it is upright with the spread open flat and folds flat as it shuts.
     */
    fun up(near: Float, far: Float): Vec3 = across((near + far) / 2f)

    /** The side of a piece its art is on: towards the reader when it stands. */
    fun pieceNormal(near: Float, far: Float): Vec3 {
        val a = (near + far) / 2f
        return Vec3(0f, -sin(a), -cos(a))
    }

    fun pieceFrontSeen(near: Float, far: Float): Boolean = (near + far) / 2f > CameraAngle

    /** Where [piece]'s foot is, at [x] along the gutter, with its pages at [near] and [far]. */
    fun base(piece: PopUpPiece, near: Float, far: Float, x: Float): Vec3 {
        val page = if (piece.side == PopUpSide.Near) near else far
        val a = across(page)
        return Vec3(x, a.y * piece.fromGutter, a.z * piece.fromGutter)
    }

    /** How bright paper facing [normal] is in the light: 0.4 to 1. */
    fun brightness(normal: Vec3): Float = (0.7f + 0.6f * (normal dot light)).coerceIn(0.4f, 1f)

    /** How much of a piece's shadow shows, from nothing to all as its spread opens flat. */
    fun shadowFade(near: Float, far: Float): Float {
        val t =
            ((far - near - PopUpDimens.ShadowFrom) / (pi - PopUpDimens.ShadowFrom)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Where the light casts [p] onto the plane of the page lying at [pageAngle]. */
    fun ontoPage(p: Vec3, pageAngle: Float): Vec3 {
        val n = leafNormal(pageAngle)
        val towards = n dot light
        if (abs(towards) < 1e-4f) return p
        return p - light * ((n dot p) / towards)
    }

    /**
     * The shadow on the table of a leaf [width] by [depth] lying at [angle]: its four corners (two
     * along the gutter, two along its outer edge) cast along the light onto the table. Lying flat
     * it shades its own footprint; standing, it throws a shadow away from the light.
     */
    fun castOnTable(angle: Float, width: Float, depth: Float): List<Vec3> {
        val edge = across(angle) * depth
        return listOf(
                Vec3(-width / 2f, 0f, 0f),
                Vec3(width / 2f, 0f, 0f),
                Vec3(width / 2f, edge.y, edge.z),
                Vec3(-width / 2f, edge.y, edge.z),
            )
            .map { ontoPage(it, 0f).copy(z = 0f) }
    }

    /**
     * Writes into [out] what to draw, back to front, for leaves at [angles] (leaf 0 the cover, the
     * fixed back board after the last), and returns how many: leaf j as j, the board as
     * angles.size, and the pieces of spread j, between leaf j and the next, as [Spread] + j.
     *
     * Leaves and spreads all turn about the gutter, so they are ordered by how far they point from
     * the eye. The spread the eye looks into comes last; any other one is drawn between its two
     * leaves, behind the one that is shutting it.
     */
    fun drawOrder(angles: FloatArray, out: IntArray): Int {
        val leaves = angles.size
        // Room for every leaf, the board and every spread.
        if (codes.size < 2 * leaves + 1) {
            codes = IntArray(2 * leaves + 1)
            keys = FloatArray(2 * leaves + 1)
            stack = FloatArray(2 * leaves + 1)
        }
        var count = 0
        for (j in 0..leaves) {
            val angle = if (j < leaves) angles[j] else 0f
            codes[count] = j
            keys[count] = abs(angle - CameraAngle)
            stack[count] = if (angle < pi / 2f) -j.toFloat() else j.toFloat()
            count++
        }
        for (j in 0 until leaves) {
            val above = angles[j]
            val below = if (j + 1 < leaves) angles[j + 1] else 0f
            if (above - below < PopUpDimens.OpenFrom) continue
            codes[count] = Spread + j
            // The spread the eye looks into is nearest of all. Any other one is drawn between
            // its two leaves, so the leaf shutting it covers it as it comes over, rather than
            // it vanishing all at once.
            keys[count] =
                if (below <= CameraAngle && CameraAngle <= above) -1f
                else abs((above + below) / 2f - CameraAngle)
            stack[count] = 0f
            count++
        }
        // Insertion sort, furthest first; leaves in a stack from the bottom up.
        for (i in 1 until count) {
            val code = codes[i]
            val key = keys[i]
            val depth = stack[i]
            var k = i - 1
            while (k >= 0 && (keys[k] < key || (keys[k] == key && stack[k] > depth))) {
                codes[k + 1] = codes[k]
                keys[k + 1] = keys[k]
                stack[k + 1] = stack[k]
                k--
            }
            codes[k + 1] = code
            keys[k + 1] = key
            stack[k + 1] = depth
        }
        codes.copyInto(out, 0, 0, count)
        return count
    }

    // Scratch for ordering, grown to fit the largest book drawn. Drawing happens on the UI thread
    // only, so one set serves every book.
    private var codes = IntArray(16)
    private var keys = FloatArray(16)
    private var stack = FloatArray(16)
}
