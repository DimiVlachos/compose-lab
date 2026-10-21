package dev.dimvlachos.lab.core.presentation.components.stickers

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Where the rating's parts sit, in the prototype's CSS pixels, from the top left of the rating: the
 * row of slots ([SlotsHeight] tall), the label under it, then the sheet ([SheetHeight] tall), inset
 * [SheetInset] at the sides. [width] is the rating's width; the prototype's is 324 on a 360 wide
 * screen, so [scale] is how much larger this one is.
 */
internal class StickerLayout(val width: Float) {
    val scale = width / 324f
    private val rowWidth = min(width, 320f * scale)
    private val left = (width - rowWidth) / 2f
    val spacing = rowWidth / StickerCount
    val radius = min(spacing * .38f, SlotsHeight * .42f)
    val slotY = SlotsHeight / 2f

    val sheetTop = SlotsHeight + LabelGap + LabelHeight + SheetGap
    val sheetWidth = width - 2 * SheetInset
    val sheetHeight = SheetHeight
    val sheetCx = width / 2f
    val sheetCy = sheetTop + SheetHeight / 2f
    val sheetRot = -1.6f * D2R

    /** The rating's whole height. */
    val height = sheetTop + SheetHeight

    /** The label's top, under the row. */
    val labelTop = SlotsHeight + LabelGap

    fun slotX(i: Int): Float = left + spacing * (i + .5f)

    // A star's place on the sheet: under its slot, a little above the sheet's middle, turned with
    // the sheet.
    fun sheetX(i: Int): Float {
        val lx = slotX(i) - sheetCx
        val ly = -SheetHeight * .1f
        return sheetCx + lx * cos(sheetRot) - ly * sin(sheetRot)
    }

    fun sheetY(i: Int): Float {
        val lx = slotX(i) - sheetCx
        val ly = -SheetHeight * .1f
        return sheetCy + lx * sin(sheetRot) + ly * cos(sheetRot)
    }

    /** How many star centres a drag at [x] has passed, counting a third of a slot early. */
    fun countAt(x: Float): Int {
        var n = 0
        for (i in 0 until StickerCount) if (x >= slotX(i) - spacing * .32f) n = i + 1
        return n
    }

    /** The star nearest [x], counted from one. */
    fun nearestAt(x: Float): Int {
        var best = 0
        var d = Float.MAX_VALUE
        for (i in 0 until StickerCount) {
            val dd = abs(x - slotX(i))
            if (dd < d) {
                d = dd
                best = i
            }
        }
        return best + 1
    }

    internal companion object {
        const val SlotsHeight = 64f
        const val LabelGap = 2f
        const val LabelHeight = 22f
        const val SheetGap = 16f
        const val SheetHeight = 92f
        const val SheetInset = 6f
    }
}

/**
 * Where a star is and how it lies: centre, turn, size, how high it is lifted (0 stuck down, 1 at
 * the top of its flight), how far it is peeled ([a], 0 to 1 of its width folded over) along which
 * [edge], and its [alpha].
 */
internal class StarPose(
    val x: Float,
    val y: Float,
    val rot: Float,
    val scale: Float,
    val lift: Float,
    val a: Float,
    val edge: Float,
    val alpha: Float,
)

private val E1 = atan2(-.58f, -.81f)
private val E2 = atan2(.5f, .86f)
private const val Peel = .46f

// How far a star rises off the sheet as it peels, so the curl comes up clear of a finger dragging
// across the sheet rather than under it. The prototype peeled it in place.
private const val PeelRise = 7f

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun ease(p: Float) = if (p < .5f) 2 * p * p else 1 - (-2 * p + 2).pow(2) / 2

private fun easeOut(p: Float) = 1 - (1 - p).pow(2)

private fun smooth(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

private fun lerpAng(a: Float, b: Float, t: Float): Float {
    var d = b - a
    val tau = (2 * PI).toFloat()
    while (d > PI) d -= tau
    while (d < -PI) d += tau
    return a + d * t
}

/** [star]'s pose at [p] along its way, as the prototype's `poseOf`, at [now] milliseconds. */
internal fun poseOf(
    layout: StickerLayout,
    state: StarStickerState,
    star: Int,
    p: Float,
    now: Float,
    reduced: Boolean,
): StarPose {
    val s = layout.scale
    val s0x = layout.sheetX(star)
    val s0y = layout.sheetY(star)
    val s0rot = layout.sheetRot + state.sheetJit[star]
    val s1x = layout.slotX(star)
    val s1y = layout.slotY
    val s1rot = state.slotRot[star]
    if (reduced) {
        val on = p > .5f
        return StarPose(
            x = if (on) s1x else s0x,
            y = if (on) s1y else s0y,
            rot = if (on) s1rot else s0rot,
            scale = 1f,
            lift = 0f,
            a = 0f,
            edge = E1,
            alpha = if (on) smooth(.5f, 1f, p) else smooth(.5f, 0f, p),
        )
    }
    val rise = PeelRise * s
    val pose: StarPose
    if (p <= .25f) {
        val u = p / .25f
        pose =
            StarPose(s0x, s0y - rise * easeOut(u), s0rot, 1f, .25f * u, Peel * easeOut(u), E1, 1f)
    } else if (p < .75f) {
        val t = (p - .25f) / .5f
        val e = ease(t)
        val sn = sin(PI.toFloat() * t)
        val arc = 22 * s + abs(s1y - s0y) * .12f
        pose =
            StarPose(
                x = lerp(s0x, s1x, e),
                y = lerp(s0y - rise, s1y, e) - arc * sn,
                rot = lerp(s0rot, s1rot, e) + .22f * sn * (if (star % 2 == 1) 1 else -1),
                scale = 1 + .2f * sn,
                lift = .25f + .75f * sn,
                a = Peel - .18f * sn,
                edge = lerpAng(E1, E2, e),
                alpha = 1f,
            )
    } else {
        val u = (1 - p) / .25f
        pose = StarPose(s1x, s1y, s1rot, 1f, .25f * u, Peel * easeOut(u), E2, 1f)
    }
    if (p >= 1f) {
        val k = (now - state.pressAt[star]) / 1000f
        if (k < 1f) {
            val bounce = 1 + .06f * exp(-k * 9) * cos(k * 26)
            return StarPose(
                pose.x,
                pose.y,
                pose.rot,
                pose.scale * bounce,
                pose.lift,
                pose.a,
                pose.edge,
                1f,
            )
        }
    }
    return pose
}
