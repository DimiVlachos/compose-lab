package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.presentation.components.perspective.Homography
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One strip of a leaf, seen end on along its rulings: its angle about them (0 lies flat on the
 * right page, -pi flat on the left) and where its hinge sits, [hingeX] across the book from the
 * spine and [hingeZ] up off the pages (negative is towards the reader). With the rulings upright
 * this is the strip itself; tilted, it is the leaf's profile across them.
 */
internal data class StripPose(val angle: Float, val hingeX: Float, val hingeZ: Float)

/**
 * Where the book is drawn from: the spine and the eye ([originY] its height, [perspectivePx] its
 * distance), a page's [page] width and [height], and its outer corners.
 */
internal class PageGeometry(
    val spineX: Float,
    val originY: Float,
    val perspectivePx: Float,
    val corner: CornerRadius,
    val page: Float,
    val height: Float,
)

/**
 * Where a hand holds a leaf: [u] across it from the spine (0) to its outer edge (1), and [v] down
 * it from its top (0) to its bottom (1). [weight] is how much the hold still shapes the paper: 1
 * under the finger, easing to 0 once it lets go.
 */
internal data class Grip(val u: Float, val v: Float, val weight: Float = 1f)

/**
 * The leaf at turn progress [t], 0 lying on the right page and 1 turned onto the left.
 *
 * The paper bends along straight rulings, and between two of them it is flat: a wedge, drawn with
 * one projection. Upright, the rulings are the leaf's strips. A leaf held by a corner has them
 * tilted, fanning out from a point on the spine's line beyond the page ([tilt] is how far they lean
 * at the page's outer edge, positive with the top corner leading): the corner then curls up first
 * and tightest, and the rest of the edge follows, as paper does.
 */
internal class TurnFrame(
    val t: Float,
    val width: Float,
    val height: Float,
    val tilt: Float,
    // How far apart the rulings stand, as a share of the leaf across its middle.
    private val spacing: Float,
    val stripWidth: Float,
    val poses: List<StripPose>,
    // sin(pi t): 0 with the leaf down, 1 standing up; it drives every shade that comes and goes
    // with the turn.
    val lift: Float,
    // The angle along the leaf at each ruling, the spine's and the last one's too: wedges + 1.
    private val boundaryAngles: FloatArray,
    // For each wedge, how it lies in the book: a rotation (row by row) and then a move, taking a
    // point of the flat page (x from the spine, y down) to the book (x from the spine, y down, z
    // away from the reader).
    private val placements: FloatArray,
) {
    /** The flat pieces of the leaf, from the spine out. */
    val wedges: Int
        get() = poses.size

    internal fun boundaryAngle(boundary: Int): Float = boundaryAngles[boundary]

    /** Where ruling [k] crosses the paper at height [y], across from the spine. */
    fun rulingX(k: Int, y: Float): Float =
        k * spacing * width * (1f + tilt * (y - height / 2f) / (height / 2f))

    /** Where ruling [k] crosses the leaf's middle, as a share of the leaf from the spine. */
    fun rulingU(k: Int): Float = k * spacing

    /** The wedge that holds the paper at ([x], [y]). */
    fun wedgeAt(x: Float, y: Float): Int {
        val across = x / (width * (1f + tilt * (y - height / 2f) / (height / 2f)))
        return floor(across / spacing).toInt().coerceIn(0, wedges - 1)
    }

    /** Whether wedge [k] shows the reader its front: its face turned towards the reader. */
    fun facesReader(k: Int): Boolean = placements[k * Placement + 8] >= 0f

    /** Where the paper at ([x], [y]) on wedge [k] lies across the book, from the spine. */
    fun bookX(k: Int, x: Float, y: Float): Float {
        val p = k * Placement
        return placements[p] * x + placements[p + 1] * y + placements[p + 9]
    }

    /**
     * Where the paper at ([x], [y]) lies in the book: across from the spine, down, and up off it.
     */
    fun bookPoint(x: Float, y: Float): Triple<Float, Float, Float> {
        val p = wedgeAt(x, y) * Placement
        val r = placements
        return Triple(
            r[p] * x + r[p + 1] * y + r[p + 9],
            r[p + 3] * x + r[p + 4] * y + r[p + 10],
            r[p + 6] * x + r[p + 7] * y + r[p + 11],
        )
    }

    /**
     * Writes into [out] the projection that draws wedge [k]: from the flat page (x from the spine,
     * y down) to where [geometry] sees it.
     */
    fun matrix(k: Int, geometry: PageGeometry, out: Matrix): Matrix =
        Homography.toMatrix(homography(k, geometry), out)

    /** Where [geometry] sees the paper at ([x], [y]). */
    fun project(x: Float, y: Float, geometry: PageGeometry): Offset =
        Homography.project(homography(wedgeAt(x, y), geometry), x, y)

    /**
     * The paper seen at [point] (x from the spine, y down), or null if the leaf is not there. Of
     * paper over paper, the wedge drawn last is the one seen.
     */
    fun unproject(point: Offset, geometry: PageGeometry): Offset? {
        for (k in wedges - 1 downTo 0) {
            val seen = Homography.unproject(homography(k, geometry), point) ?: continue
            val x = seen.x
            val y = seen.y
            if (x < 0f || x > width || y < 0f || y > height) continue
            // On this wedge's plane, but only its own piece of paper counts.
            val across = x / (width * (1f + tilt * (y - height / 2f) / (height / 2f))) / spacing
            if (across >= k && (across < k + 1 || k == wedges - 1)) return Offset(x, y)
        }
        return null
    }

    // Wedge [k]'s projection as a 3 x 3 (row by row): its placement in the book, then seen from
    // the eye, P / (P + z) smaller the further away.
    private fun homography(k: Int, geometry: PageGeometry): FloatArray {
        val p = k * Placement
        val r = placements
        return Homography.of(
            ox = r[p + 9] + geometry.spineX,
            oy = r[p + 10],
            oz = r[p + 11],
            ux = r[p],
            uy = r[p + 3],
            uz = r[p + 6],
            vx = r[p + 1],
            vy = r[p + 4],
            vz = r[p + 7],
            cx = geometry.spineX,
            cy = geometry.originY,
            perspective = geometry.perspectivePx,
            out = scratch,
        )
    }

    private val scratch = FloatArray(9)
}

// Per wedge: 9 for its rotation, 3 for its move.
private const val Placement = 12

/**
 * How high a page resting in the open book stands at [u] across it (0 at the spine, 1 at its outer
 * edge), as a fraction of [PageTurnDimens.RestLift]: it rises steeply out of the gutter, crests a
 * little before half way and falls gently onto the stack under its edge.
 */
internal fun restHeight(u: Float): Float {
    val fromEdge = 1f - u
    return 1f - fromEdge * fromEdge * fromEdge * fromEdge - PageTurnDimens.RestFall * u * u
}

// The rest curve's slope at [u], per page width, as a fraction of RestLift.
private fun restSlope(u: Float): Float {
    val fromEdge = 1f - u
    return 4f * fromEdge * fromEdge * fromEdge - 2f * PageTurnDimens.RestFall * u
}

// Past the outer edge, where a tilted leaf's far corner reaches, the paper lies as at the edge.
private fun restHeightBeyond(u: Float): Float = restHeight(min(u, 1f))

/**
 * The leaf at progress [t]. The leaf as a whole turns by pi t, and the bow is spread along it. With
 * [bendDirection] 1 the strip at the spine is a bend ahead of the turn and the free edge a bend
 * behind it, so the paper bulges the way the turn goes, as air and the hand hold the edge back; -1
 * mirrors it for a turn running the other way, and the paper's flex can carry it past either.
 *
 * At rest a leaf lies in the open book's curve ([restHeight], [restLift] high) on the right page
 * and in its mirror on the left; the turn carries it from one to the other, flattening it on the
 * way, so a page starts and ends a turn in exactly the shape of the pages around it.
 *
 * A [grip] shapes the bow by where the hand holds the leaf: near the spine there is little lever
 * and the leaf turns stiff and flat; out at the edge the paper bends most around the fingers. A
 * [tilt] leans the rulings (see [TurnFrame]); a [height] is needed for it, the page's.
 */
internal fun turnFrame(
    t: Float,
    leafWidth: Float,
    bendDirection: Float = 1f,
    strips: Int = PageTurnDimens.Strips,
    restLift: Float = PageTurnDimens.RestLift,
    height: Float = leafWidth,
    tilt: Float = 0f,
    grip: Grip? = null,
): TurnFrame {
    val chain = LeafChain(t, leafWidth, bendDirection, strips, restLift, height, tilt, grip)
    val wedges = chain.wedges
    val stripWidth = leafWidth / strips
    val boundaries = FloatArray(wedges + 1) { chain.boundaryAngle(it) }
    val poses = ArrayList<StripPose>(wedges)
    val placements = FloatArray(wedges * Placement)
    var hingeX = 0f
    var hingeZ = 0f
    // The rotation and move of the wedge before, starting from the flat page.
    val r = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    val move = FloatArray(3)
    var previous = 0f
    for (k in 0 until wedges) {
        val angle = chain.angle(k)
        poses += StripPose(angle, hingeX, hingeZ)
        hingeX += chain.spacing * leafWidth * cos(angle)
        hingeZ += chain.spacing * leafWidth * sin(angle)
        chain.fold(k, angle - previous, r, move)
        previous = angle
        r.copyInto(placements, k * Placement)
        move.copyInto(placements, k * Placement + 9)
    }
    return TurnFrame(
        t,
        leafWidth,
        height,
        chain.lean,
        chain.spacing,
        stripWidth,
        poses,
        chain.lift,
        boundaries,
        placements,
    )
}

/**
 * The leaf's shape at progress [t], as [turnFrame] lays it out: how many wedges, the angle of each
 * and of each ruling, and how each folds on the one before.
 */
private class LeafChain(
    t: Float,
    private val leafWidth: Float,
    bendDirection: Float,
    private val strips: Int,
    private val restLift: Float,
    height: Float,
    tilt: Float,
    private val grip: Grip?,
) {
    private val theta = (PI * t).toFloat()
    val lift = sin(PI * t).toFloat()
    private val hold = grip?.weight?.coerceIn(0f, 1f) ?: 0f
    private val gripShare = if (hold == 0f) null else GripShare(grip!!.u)
    private val bend =
        PageTurnDimens.BendMax * lift * if (grip == null) 1f else lerp(1f, gripLever(grip.u), hold)
    private val bow = bendDirection.coerceIn(-PageTurnDimens.BowMax, PageTurnDimens.BowMax)
    val lean = tilt.coerceIn(-PageTurnDimens.TiltLimit, PageTurnDimens.TiltLimit)

    // A tilted leaf's far corner lies past the last upright ruling: more wedges reach it, as many
    // as
    // [PageTurnDimens.MaxWedges]; past that they stand further apart.
    private val reach = 1f / (1f - abs(lean))
    val spacing = max(1f / strips, reach / PageTurnDimens.MaxWedges)
    val wedges = max(strips, ceil(reach / spacing - 0.001f).toInt())
    private val half = height / 2f

    // The rest curve's angles on the right page, mirrored on the left: 1 - 2t takes one to the
    // other through flat half way. A hand peeling a corner bends the paper its own way, and the
    // curve the binding gives it goes as the rulings lean.
    private val restSide =
        (1f - 2f * t) *
            (1f - smoothstep((abs(lean) / PageTurnDimens.TiltRestFade).coerceIn(0f, 1f)))

    // How the bow is shared along the leaf, 0 at the spine to 1 at the edge.
    private fun share(u: Float): Float = gripShare?.let { lerp(u, it.at(u), hold) } ?: u

    /** Wedge [k]'s angle about the rulings. */
    fun angle(k: Int): Float {
        val u0 = k * spacing
        val u1 = (k + 1) * spacing
        val rise = restLift * (restHeightBeyond(u1) - restHeightBeyond(u0)) / spacing
        return -theta + bow * bend * (2f * share(u0) - 1f) - atan(rise) * restSide
    }

    /** The angle along the leaf at ruling [b], as smooth as the rest curve is. */
    fun boundaryAngle(b: Int): Float {
        val u = b * spacing
        val rest = -atan(restLift * restSlope(min(u, 1f)))
        return -theta + bow * bend * (2f * share(u) - 1f) + rest * restSide
    }

    /**
     * Folds wedge [k] about its ruling by [turn], how much its angle differs from the one before,
     * on top of the placement so far ([r], [move]): about the line through (u W, half) leaning by
     * the tilt, pointing up the page.
     */
    fun fold(k: Int, turn: Float, r: FloatArray, move: FloatArray) {
        val u0 = k * spacing
        val leanX = -u0 * leafWidth * lean / half
        val length = sqrt(leanX * leanX + 1f)
        foldAbout(r, move, u0 * leafWidth, half, leanX / length, -1f / length, turn)
    }

    /** The wedge holding the paper at ([x], [y]). */
    fun wedgeAt(x: Float, y: Float): Int {
        val across = x / (leafWidth * (1f + lean * (y - half) / half))
        return floor(across / spacing).toInt().coerceIn(0, wedges - 1)
    }
}

/**
 * Where the paper at [grab] (x from the spine, y down) lies in the book, as [turnFrame] would lay
 * it out with the same arguments, into [out] (across from the spine, down, up off the page). Only
 * the wedges up to it are folded, and nothing is kept: a hand's pull is solved with it many times a
 * frame.
 */
internal fun heldPoint(
    grab: Offset,
    t: Float,
    leafWidth: Float,
    bendDirection: Float,
    height: Float,
    tilt: Float,
    grip: Grip?,
    out: FloatArray,
    restLift: Float = PageTurnDimens.RestLift,
    strips: Int = PageTurnDimens.Strips,
) {
    val chain = LeafChain(t, leafWidth, bendDirection, strips, restLift, height, tilt, grip)
    val r = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    val move = FloatArray(3)
    var previous = 0f
    val last = chain.wedgeAt(grab.x, grab.y)
    for (k in 0..last) {
        val angle = chain.angle(k)
        chain.fold(k, angle - previous, r, move)
        previous = angle
    }
    out[0] = r[0] * grab.x + r[1] * grab.y + move[0]
    out[1] = r[3] * grab.x + r[4] * grab.y + move[1]
    out[2] = r[6] * grab.x + r[7] * grab.y + move[2]
}

// Folds what lies past a ruling through ([ax], [ay]) along ([ex], [ey]) by [angle], on top of the
// placement so far ([r], [move]): the paper turns about the ruling as it lay on the flat page, and
// then goes where the wedge before it went. With the ruling pointing up the page, a negative
// angle lifts the paper past it towards the reader.
private fun foldAbout(
    r: FloatArray,
    move: FloatArray,
    ax: Float,
    ay: Float,
    ex: Float,
    ey: Float,
    angle: Float,
) {
    val c = cos(angle)
    val s = sin(angle)
    val d = 1f - c
    // Rodrigues' rotation about (ex, ey, 0).
    val q0 = c + d * ex * ex
    val q1 = d * ex * ey
    val q2 = s * ey
    val q3 = d * ex * ey
    val q4 = c + d * ey * ey
    val q5 = -s * ex
    val q6 = -s * ey
    val q7 = s * ex
    val q8 = c
    // The fold moves the ruling's point by a - Q a; the placement so far carries that along.
    val dx = ax - (q0 * ax + q1 * ay)
    val dy = ay - (q3 * ax + q4 * ay)
    val dz = -(q6 * ax + q7 * ay)
    move[0] += r[0] * dx + r[1] * dy + r[2] * dz
    move[1] += r[3] * dx + r[4] * dy + r[5] * dz
    move[2] += r[6] * dx + r[7] * dy + r[8] * dz
    for (row in 0 until 3) {
        val a = r[row * 3]
        val b = r[row * 3 + 1]
        val e = r[row * 3 + 2]
        r[row * 3] = a * q0 + b * q3 + e * q6
        r[row * 3 + 1] = a * q1 + b * q4 + e * q7
        r[row * 3 + 2] = a * q2 + b * q5 + e * q8
    }
}

/** How much a hold [u] across the leaf bends it: little lever near the spine, most at the edge. */
internal fun gripLever(u: Float): Float =
    lerp(PageTurnDimens.SpineLever, PageTurnDimens.EdgeLever, smoothstep(u.coerceIn(0f, 1f)))

/**
 * How much of the bow lies between the spine and [u], 0 to 1 at the outer edge, for a leaf held at
 * [held]: the paper bends most around the fingers, so the share gathers there.
 */
internal fun gripShare(u: Float, held: Float): Float = GripShare(held).at(u)

// The share for one hold, its constant parts worked out once.
private class GripShare(private val held: Float) {
    private val spread = PageTurnDimens.GripSpread
    private val atSpine = erf(-held / spread)
    private val total = gathered(1f)

    private fun gathered(x: Float): Float =
        x + PageTurnDimens.GripPeak * spread * (SqrtPi / 2f) * (erf((x - held) / spread) - atSpine)

    fun at(u: Float): Float = gathered(u) / total
}

private const val SqrtPi = 1.7724539f

// Abramowitz and Stegun 7.1.26: within 1.5e-7, far finer than a pixel of paper.
private fun erf(x: Float): Float {
    val sign = if (x < 0f) -1f else 1f
    val a = abs(x)
    val k = 1f / (1f + 0.3275911f * a)
    val poly =
        k *
            (0.2548296f +
                k * (-0.28449672f + k * (1.4214138f + k * (-1.4531521f + k * 1.0614054f))))
    return sign * (1f - poly * exp(-a * a))
}

/**
 * Whether a leaf, as [frame] lays it, reaches across the spine to the other side: a leaf hinged on
 * the right of the fold ([rightOfFold]) that curls back over to the left, or the mirror. Seen from
 * above, such a leaf lies over the fold and anything in it.
 */
internal fun reachesAcrossSpine(frame: TurnFrame, rightOfFold: Boolean): Boolean {
    val tolerance = frame.stripWidth * 0.01f
    for (k in 0..frame.wedges) {
        for (y in floatArrayOf(0f, frame.height)) {
            val x = min(frame.rulingX(k, y), frame.width)
            val across = frame.bookX(frame.wedgeAt(x, y), x, y)
            if (if (rightOfFold) across < -tolerance else across > tolerance) return true
        }
    }
    return false
}

/**
 * Where strip [index]'s slice starts across its page, as a fraction of the page. A strip facing the
 * reader shows the leaf's front, the right page, in order from the spine. Facing away it shows the
 * back, which ends up as the left page: its strips run from the spine outwards too, so they take
 * the page from the far end.
 */
internal fun stripSliceStart(
    front: Boolean,
    index: Int,
    strips: Int = PageTurnDimens.Strips,
): Float = if (front) index.toFloat() / strips else (strips - index - 1).toFloat() / strips

/** The strip that carries the leaf's outer corners, rounded on the side away from the spine. */
internal fun stripIsOuterEdge(index: Int, strips: Int = PageTurnDimens.Strips): Boolean =
    index == strips - 1

/** A strip draws its slice this much wider so it overlaps the next one by [seamPx]. */
internal fun stripSliceScale(stripWidth: Float, seamPx: Float): Float = 1f + seamPx / stripWidth

/**
 * The shade across strip [index], from its spine edge to its outer edge: the more an edge faces
 * away from the reader, the darker. Shading the edges, not the strip, keeps the bow smooth.
 */
internal fun stripShadeAlphas(frame: TurnFrame, index: Int): Pair<Float, Float> =
    (1f - edgeLight(frame, index)) * PageTurnDimens.ShadowMax to
        (1f - edgeLight(frame, index + 1)) * PageTurnDimens.ShadowMax

/** The sheen on strip [index]; eased by the lift squared, so it leaves as gently as the shadow. */
internal fun stripGlareAlpha(frame: TurnFrame, index: Int): Float {
    val light = edgeLight(frame, index)
    return frame.lift * frame.lift * light * light * PageTurnDimens.GlareMax
}

/**
 * How much of the binding thread of a sewn pamphlet shows, 0 to 1. It runs through the fold of the
 * innermost sheet, between the two pages of spread [centre]: leaf centre - 1 on the left of the
 * fold and leaf centre on the right, the thread lying on their inner faces. Each side leaves the
 * fold open while it lies on top of its stack ([leftTop], [rightTop]); a leaf of the fold in the
 * air leaves it open only while the strip at its spine, at the angle given in [spineAngles], still
 * shows its inner face, and closes it as that strip passes upright over the thread.
 */
internal fun stitchesShown(
    centre: Int,
    leftTop: Int,
    rightTop: Int,
    spineAngles: Map<Int, Float>,
): Float {
    fun side(leaf: Int, onTop: Boolean, innerFaceIsBack: Boolean): Float {
        val angle = spineAngles[leaf] ?: return if (onTop) 1f else 0f
        // cos > 0: the strip faces the reader with its front; < 0, with its back.
        val facing = if (innerFaceIsBack) -cos(angle) else cos(angle)
        return smoothstep((facing / PageTurnDimens.FoldFadeCos).coerceIn(0f, 1f))
    }
    return side(centre - 1, leftTop == centre - 1, innerFaceIsBack = true) *
        side(centre, rightTop == centre, innerFaceIsBack = false)
}

/**
 * Whether a page let go at [progress], moving at [velocity] turns a second, finishes turning. A
 * flick either way decides by its direction, wherever the page is; a slower release goes by whether
 * the page is past [PageTurnDimens.CommitProgress].
 */
internal fun shouldCommit(progress: Float, velocity: Float): Boolean =
    when {
        velocity > PageTurnDimens.CommitVelocity -> true
        velocity < -PageTurnDimens.CommitVelocity -> false
        else -> progress > PageTurnDimens.CommitProgress
    }

/**
 * The bend direction for a turn that is going [forward], while its progress is increasing or not:
 * the bow always bulges the way the page is moving.
 */
internal fun bendTowards(forward: Boolean, progressIncreasing: Boolean): Float =
    if (forward == progressIncreasing) 1f else -1f

/**
 * How far the rulings of a leaf held at [grip] would rather lean: a hold near the top or bottom
 * corner peels that corner first, one at the middle of the edge turns the leaf straight, and the
 * nearer the spine the hold, the less a corner leads.
 */
internal fun gripTilt(grip: Grip): Float {
    val fromMiddle = abs(grip.v - 0.5f)
    val corner =
        smoothstep(
            ((fromMiddle - PageTurnDimens.TiltDeadZone) / (0.5f - PageTurnDimens.TiltDeadZone))
                .coerceIn(0f, 1f)
        )
    val side = if (grip.v < 0.5f) 1f else -1f
    return side * PageTurnDimens.TiltMax * corner * smoothstep(grip.u.coerceIn(0f, 1f))
}

/**
 * How far a leaf held at [grab] (x from the spine, y down) has turned to bring that paper across to
 * [targetX], where the finger is: the finger's place across turns the leaf, its height only says
 * where along the edge the hand holds it (see [leanFor]). Solved on from [from], the leaf's last
 * turn, at its lean [tilt].
 *
 * Seen in perspective, paper lifting off the page first moves out, towards the reader, before it
 * comes back over: matched exactly, a finger's first move would snap the leaf up. So the paper is
 * first placed under the finger as if seen from straight above, where it only ever moves one way,
 * then [PageTurnDimens.PinPerspective] of the way to where the eye sees it.
 */
internal fun pinTurn(
    grab: Offset,
    targetX: Float,
    from: Float,
    bow: Float,
    tilt: Float,
    grip: Grip,
    geometry: PageGeometry,
    restLift: Float = PageTurnDimens.RestLift,
    liftedFrom: Float = Float.NaN,
): Float {
    // Near either end of the turn, where the paper first creeps the wrong way, the turn is found
    // by search; anywhere else it follows on smoothly from the last one.
    val nearAnEnd = from < PinSearchEnds || from > 1f - PinSearchEnds
    var t =
        if (nearAnEnd) {
            turnUnder(grab, targetX, from, tilt, bow, grip, geometry, restLift, liftedFrom)
        } else from.coerceIn(0f, 1f)
    val point = FloatArray(3)
    fun miss(t: Float): Float =
        (heldSeenX(grab, t, bow, tilt, grip, geometry, restLift, point, liftedFrom) - targetX) /
            geometry.page
    repeat(PageTurnDimens.PinSteps) {
        val here = miss(t)
        val probe = if (t > 1f - PinProbe) -PinProbe else PinProbe
        val slope = (miss(t + probe) - here) / probe
        if (abs(slope) < 1e-6f) return t
        val step = (-here / slope).coerceIn(-PinMaxStep, PinMaxStep)
        t = (t + step).coerceIn(0f, 1f)
        if (abs(step) < PinSettled) return t
    }
    return t
}

/**
 * Where across a hand's held paper at [grab] is taken to be, at turn [t], for placing it under the
 * finger: as seen from straight above, then [PageTurnDimens.PinPerspective] of the way to the eye's
 * view (see [pinTurn]).
 *
 * Paper lifting off its page first creeps outwards, as its rest curve flattens and it rises towards
 * the eye, before it comes back over: a finger's first move in would find no turn to match, and
 * then a sudden one. Near the page it was taken from ([liftedFrom], 0 or 1, or NaN for a leaf
 * caught in the air), so, the paper is taken to swing about the spine as a flat leaf would, always
 * inwards, handing over to where it really is as it rises.
 */
internal fun heldSeenX(
    grab: Offset,
    t: Float,
    bow: Float,
    tilt: Float,
    grip: Grip?,
    geometry: PageGeometry,
    restLift: Float = PageTurnDimens.RestLift,
    point: FloatArray = FloatArray(3),
    liftedFrom: Float = Float.NaN,
): Float {
    fun seen(t: Float): Float {
        heldPoint(grab, t, geometry.page, bow, geometry.height, tilt, grip, point, restLift)
        val (x, _, z) = point
        val scale =
            PageTurnDimens.PinPerspective *
                (geometry.perspectivePx / (geometry.perspectivePx + z) - 1f)
        return x * (1f + scale)
    }
    val real = seen(t)
    if (liftedFrom.isNaN()) return geometry.spineX + real
    val risen = smoothstep((abs(t - liftedFrom) / PageTurnDimens.LiftSwing).coerceIn(0f, 1f))
    if (risen >= 1f) return geometry.spineX + real
    // The flat leaf's swing, from where the paper lay on its page.
    val resting = if (liftedFrom < 0.5f) seen(0f) else -seen(1f)
    val swing = resting * cos(PI.toFloat() * t)
    return geometry.spineX + lerp(swing, real, risen)
}

/**
 * How far the paper trails a finger that has gone [travel] in from where it took the page off its
 * stack, over a lift-off of [span]. Pinned exactly, paper lying flat leaps up at the first touch of
 * a pull: its edge barely moves across while it lifts. So at first it trails, sliding a little
 * under the fingertip, and lifts in step with the finger; past [span] it catches up again over
 * twice as far, smoothly, and from there on lies under the finger.
 */
internal fun liftLag(travel: Float, span: Float): Float =
    when {
        travel <= 0f -> 0f
        travel < span -> travel - travel * travel / (2f * span)
        else -> span / 2f * (1f - smoothstep(((travel - span) / (2f * span)).coerceIn(0f, 1f)))
    }

/**
 * Where a held leaf's rulings lean at turn [t]: towards the corner the hand holds ([gripTilt]), and
 * square to the way the finger has lately pulled ([pullTilt]), growing in as the leaf leaves its
 * page so taking it never jumps.
 */
internal fun leanFor(grip: Grip, pull: Offset, t: Float, geometry: PageGeometry): Float {
    val pulled = pullTilt(pull, geometry)
    // A pull on a slant decides the fold while it lasts; going straight across, the hold does.
    val hold = gripTilt(grip) * (1f - abs(pulled) / PageTurnDimens.TiltMax)
    return ((hold + pulled) * tiltRamp(t)).coerceIn(
        -PageTurnDimens.TiltLimit,
        PageTurnDimens.TiltLimit,
    )
}

/**
 * How far a [pull] leans the rulings: paper folds square to the way it is pulled, so a pull down
 * and in towards the spine leans them to peel the top corner first, a pull up and in the bottom
 * one, and a straight pull leaves them as the hold has them. [pull] is the finger's recent way, x
 * towards the leaf's outer edge (negative is towards the spine), y down. Only a pull that really
 * goes in towards the spine leans them: a finger moving up or down leaves that to the hold.
 */
internal fun pullTilt(pull: Offset, geometry: PageGeometry): Float {
    val settle = PageTurnDimens.PullSettle * geometry.page
    val towardsSpine = -pull.x
    if (towardsSpine <= 0f) return 0f
    // Rulings square to the pull: their slope across, at the outer edge, is its rise over run.
    // A finger going mostly up or down has little run: a floor under it keeps the lean steady.
    val lean = pull.y / (towardsSpine + settle / 4f) * geometry.height / (2f * geometry.page)
    // It counts in only as the finger really goes across.
    val going = smoothstep((towardsSpine / settle).coerceIn(0f, 1f))
    return lean.coerceIn(-PageTurnDimens.TiltMax, PageTurnDimens.TiltMax) * going
}

/**
 * The turn nearest [from] that puts the held paper, seen from straight above, across at [targetX]:
 * searched outwards both ways at once and narrowed down, not followed down a slope. Paper leaving a
 * page first creeps the wrong way a little as its rest curve flattens, and a slope there points the
 * wrong way. If the finger is out of reach, the turn that comes nearest.
 */
private fun turnUnder(
    grab: Offset,
    targetX: Float,
    from: Float,
    tilt: Float,
    bow: Float,
    grip: Grip,
    geometry: PageGeometry,
    restLift: Float,
    liftedFrom: Float,
): Float {
    val point = FloatArray(3)
    fun miss(t: Float) =
        heldSeenX(grab, t, bow, tilt, grip, geometry, restLift, point, liftedFrom) - targetX
    val start = from.coerceIn(0f, 1f)
    val here = miss(start)
    if (here == 0f) return start
    var nearest = start
    var nearestMiss = abs(here)
    // The last turn looked at each way, and how far it missed.
    val edge = floatArrayOf(start, start)
    val edgeMiss = floatArrayOf(here, here)
    var step = 1
    while (edge[0] < 1f || edge[1] > 0f) {
        for (side in 0..1) {
            val a = edge[side]
            val b = (start + (if (side == 0) 1f else -1f) * step * PinScanStep).coerceIn(0f, 1f)
            if (b == a) continue
            val missB = miss(b)
            if (abs(missB) < nearestMiss) {
                nearest = b
                nearestMiss = abs(missB)
            }
            if ((edgeMiss[side] < 0f) != (missB < 0f)) {
                var lo = a
                var hi = b
                var missLo = edgeMiss[side]
                repeat(PinBisections) {
                    val mid = (lo + hi) / 2f
                    val missMid = miss(mid)
                    if ((missMid < 0f) == (missLo < 0f)) {
                        lo = mid
                        missLo = missMid
                    } else hi = mid
                }
                return (lo + hi) / 2f
            }
            edge[side] = b
            edgeMiss[side] = missB
        }
        step++
    }
    return nearest
}

private const val PinScanStep = 0.02f
private const val PinSearchEnds = 0.12f
// A step this small moves the paper well under a pixel.
private const val PinSettled = 2e-4f
private const val PinBisections = 10

private const val PinProbe = 1e-3f
private const val PinMaxStep = 0.08f

// The hold's lean grows as the leaf leaves its page, from nothing, so taking a page never jumps.
private fun tiltRamp(t: Float): Float =
    smoothstep((min(t, 1f - t) / PageTurnDimens.TiltRamp).coerceIn(0f, 1f))

/** A damped spring's [value] and [velocity], stepped frame by frame. */
internal data class Spring(val value: Float, val velocity: Float = 0f) {
    /**
     * The spring [dt] seconds on, pulled towards [target]: ringing at [frequency] hertz, damped by
     * [damping] (1 settles without overshoot, less rings a little).
     */
    fun step(target: Float, dt: Float, frequency: Float, damping: Float): Spring {
        val omega = 2f * PI.toFloat() * frequency
        var value = value
        var velocity = velocity
        var left = dt
        while (left > 0f) {
            val h = min(left, SpringStep)
            velocity += (-omega * omega * (value - target) - 2f * damping * omega * velocity) * h
            value += velocity * h
            left -= h
        }
        return Spring(value, velocity)
    }
}

// Stiff springs are stepped finely, whatever the frame rate.
private const val SpringStep = 1f / 240f

/**
 * Where the bow of a leaf turning at [velocity] (its own progress a second) is pulled. A hand
 * holding it ([held]) leads: the paper sags back behind the fingers towards the spine. Let go, the
 * free edge trails instead, held back by the air. Either way the bow keeps to the way the leaf last
 * went ([direction]) when still, and moving, the air pushes it back the more, the faster.
 */
internal fun bowTarget(direction: Float, velocity: Float, held: Boolean): Float =
    direction * (if (held) -PageTurnDimens.HoldBow else PageTurnDimens.BowRest) +
        (velocity * PageTurnDimens.FlexGain).coerceIn(
            -PageTurnDimens.FlexMax,
            PageTurnDimens.FlexMax,
        )

private fun edgeLight(frame: TurnFrame, boundary: Int): Float =
    abs(cos(frame.boundaryAngle(boundary)))

/**
 * How high a sheet [depth] down its stack arches: the top page by RestLift, and every sheet under
 * it a little flatter. Flatter paper reaches further from the spine, so the sheets' edges fan out
 * past the page above, as an open book's do. [depth] is fractional while a stack settles.
 */
internal fun sheetLift(depth: Float): Float =
    PageTurnDimens.RestLift *
        (1f - PageTurnDimens.SheetFlatten * depth).coerceAtLeast(PageTurnDimens.FlattestShare)

/**
 * How far a leaf turning at [t] has let the sheets under it rise: they take its place in the first
 * half of the turn, as it leaves them. 0 to 1.
 */
internal fun stackRise(t: Float): Float =
    smoothstep((t / PageTurnDimens.SettleSpan).coerceIn(0f, 1f))

/**
 * How far a leaf turning at [t] has pressed the stack it lands on: in the last part of the turn, as
 * it comes down onto it. 0 to 1.
 */
internal fun stackLand(t: Float): Float =
    smoothstep(
        ((t - (1f - PageTurnDimens.SettleSpan)) / PageTurnDimens.SettleSpan).coerceIn(0f, 1f)
    )

internal fun smoothstep(x: Float): Float = x * x * (3f - 2f * x)

/**
 * The extra shade on a resting page at [u] across it: the paper falling into the spine is darker,
 * clearing towards the crest, and it dims again a little as it curls down onto the stack. A lifted
 * leaf ([lift]) loses it.
 */
internal fun gutterRestShade(u: Float, lift: Float): Float {
    val near = (1f - u / PageTurnDimens.GutterRestSpan).coerceIn(0f, 1f)
    val falling =
        ((u - (1f - PageTurnDimens.EdgeRestSpan)) / PageTurnDimens.EdgeRestSpan).coerceIn(0f, 1f)
    return (PageTurnDimens.GutterRestAlpha * near * near +
        PageTurnDimens.EdgeRestAlpha * falling * falling) * (1f - lift)
}
