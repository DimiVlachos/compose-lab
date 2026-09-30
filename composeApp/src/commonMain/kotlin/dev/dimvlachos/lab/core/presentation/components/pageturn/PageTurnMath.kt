package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.graphics.Matrix
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * One strip of the turning leaf: its angle about the spine's axis (0 lies flat on the right page,
 * -pi flat on the left) and where its hinge sits, [hingeX] across the book from the spine and
 * [hingeZ] up off the pages (negative is towards the reader).
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
    private val theta: Float,
    private val bend: Float,
    private val bendDirection: Float,
) {
    // The angle along the leaf at the edge between strip [boundary] - 1 and [boundary].
    internal fun boundaryAngle(boundary: Int): Float =
        -theta + bendDirection * (boundary * 2f * bend / poses.size - bend)
}

/**
 * The leaf's chain of strips at progress [t]. The leaf as a whole turns by pi t, and the bow is
 * spread along it: the strip at the spine leans back by the bend, the free edge leads by it, and
 * [bendDirection] (-1..1) says which way. A page pulled forward leads with its edge; pushed back,
 * the edge trails.
 */
internal fun turnFrame(
    t: Float,
    leafWidth: Float,
    bendDirection: Float = 1f,
    strips: Int = PageTurnDimens.Strips,
): TurnFrame {
    val theta = (PI * t).toFloat()
    val lift = sin(PI * t).toFloat()
    val bend = PageTurnDimens.BendMax * lift
    val direction = bendDirection.coerceIn(-1f, 1f)
    val stripWidth = leafWidth / strips
    val poses = ArrayList<StripPose>(strips)
    var hingeX = 0f
    var hingeZ = 0f
    for (i in 0 until strips) {
        val angle = -theta + direction * (i * 2f * bend / strips - bend)
        poses += StripPose(angle, hingeX, hingeZ)
        hingeX += stripWidth * cos(angle)
        hingeZ += stripWidth * sin(angle)
    }
    return TurnFrame(t, stripWidth, poses, lift, theta, bend, direction)
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

/** Whether a page let go at [progress], moving at [velocity] turns a second, finishes turning. */
internal fun shouldCommit(progress: Float, velocity: Float): Boolean =
    progress > PageTurnDimens.CommitProgress || velocity > PageTurnDimens.CommitVelocity

/**
 * The bend direction for a turn that is going [forward], while its progress is increasing or not:
 * the edge always leads the way the page is moving.
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
