package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.graphics.Matrix
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * One strip of a leaf: its angle about the spine's axis (0 lies flat on the right page, -pi flat on
 * the left) and where its hinge sits, [hingeX] across the book from the spine and [hingeZ] up off
 * the pages (negative is towards the reader).
 */
internal data class StripPose(val angle: Float, val hingeX: Float, val hingeZ: Float)

/** The leaf at turn progress [t], 0 lying on the right page and 1 turned onto the left. */
internal class TurnFrame(
    val t: Float,
    val stripWidth: Float,
    val poses: List<StripPose>,
    // sin(pi t): 0 with the leaf down, 1 standing up; it drives every shade that comes and goes
    // with the turn.
    val lift: Float,
    // The angle along the leaf at each edge between two strips, and at both ends: strips + 1.
    private val boundaryAngles: FloatArray,
) {
    internal fun boundaryAngle(boundary: Int): Float = boundaryAngles[boundary]
}

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

/**
 * The leaf's chain of strips at progress [t]. The leaf as a whole turns by pi t, and the bow is
 * spread along it. With [bendDirection] 1 the strip at the spine is a bend ahead of the turn and
 * the free edge a bend behind it, so the paper bulges the way the turn goes, as air and the hand
 * hold the edge back; -1 mirrors it for a turn running the other way.
 *
 * At rest a leaf lies in the open book's curve ([restHeight], [restLift] high) on the right page
 * and in its mirror on the left; the turn carries it from one to the other, flattening it on the
 * way, so a page starts and ends a turn in exactly the shape of the pages around it.
 */
internal fun turnFrame(
    t: Float,
    leafWidth: Float,
    bendDirection: Float = 1f,
    strips: Int = PageTurnDimens.Strips,
    restLift: Float = PageTurnDimens.RestLift,
): TurnFrame {
    val theta = (PI * t).toFloat()
    val lift = sin(PI * t).toFloat()
    val bend = PageTurnDimens.BendMax * lift
    val direction = bendDirection.coerceIn(-1f, 1f)
    val stripWidth = leafWidth / strips
    // The rest curve's angles on the right page, mirrored on the left: 1 - 2t takes one to the
    // other through flat half way.
    val restSide = 1f - 2f * t
    val boundaries =
        FloatArray(strips + 1) { b ->
            val rest = -atan(restLift * restSlope(b.toFloat() / strips))
            -theta + direction * (b * 2f * bend / strips - bend) + rest * restSide
        }
    val poses = ArrayList<StripPose>(strips)
    var hingeX = 0f
    var hingeZ = 0f
    for (i in 0 until strips) {
        val u0 = i.toFloat() / strips
        val u1 = (i + 1).toFloat() / strips
        val rise = restLift * (restHeight(u1) - restHeight(u0)) * strips
        val angle = -theta + direction * (i * 2f * bend / strips - bend) - atan(rise) * restSide
        poses += StripPose(angle, hingeX, hingeZ)
        hingeX += stripWidth * cos(angle)
        hingeZ += stripWidth * sin(angle)
    }
    return TurnFrame(t, stripWidth, poses, lift, boundaries)
}

/**
 * Writes into [out] the projection that draws a strip, laid out from (0, 0) at its own size, where
 * its [pose] puts it in the book: rotated about the vertical axis through its hinge, then seen from
 * [perspectivePx] away with the eye at ([spineX], [originY]).
 */
internal fun stripMatrix(
    pose: StripPose,
    spineX: Float,
    originY: Float,
    perspectivePx: Float,
    out: Matrix,
): Matrix {
    // A point x along the strip sits at X = hingeX + x cos(a), Z = hingeZ + x sin(a), relative to
    // the spine. Seen from the eye, both X and Y (from its centre) scale by P / (P + Z).
    val cosA = cos(pose.angle)
    val sinA = sin(pose.angle)
    val wx = sinA / perspectivePx
    val w0 = 1f + pose.hingeZ / perspectivePx
    out.reset()
    out[0, 0] = cosA + spineX * wx
    out[0, 1] = originY * wx
    out[0, 3] = wx
    out[3, 0] = pose.hingeX + spineX * w0
    out[3, 1] = originY * w0 - originY
    out[3, 3] = w0
    return out
}

internal fun stripFacesReader(pose: StripPose): Boolean = cos(pose.angle) >= 0f

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

/** How much the stitches sit over the leaf rather than under it: 0 mid-turn, 1 at either end. */
internal fun stitchesOverLeaf(t: Float): Float =
    1f - (min(t, 1f - t) / PageTurnDimens.StitchFadeSpan).coerceIn(0f, 1f)

/** A horizontal drag of [dragPx] as turn progress, on a book [bookWidthPx] wide. */
internal fun dragToProgress(dragPx: Float, bookWidthPx: Float): Float =
    dragPx / (PageTurnDimens.DragSpan * bookWidthPx)

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
 * The bend after a drag moved the turn by [delta] progress: it walks towards the direction of the
 * move, crossing from one side to the other over [PageTurnDimens.BendFlipSpan] of progress.
 */
internal fun bendAfterDrag(current: Float, forward: Boolean, delta: Float): Float {
    if (delta == 0f) return current
    val target = bendTowards(forward, progressIncreasing = delta > 0f)
    val step = abs(delta) * 2f / PageTurnDimens.BendFlipSpan
    return if (target > current) min(target, current + step) else max(target, current - step)
}

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

private fun smoothstep(x: Float): Float = x * x * (3f - 2f * x)

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
