package dev.dimvlachos.lab.core.presentation.components.zipper

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The cotton tag's geometry, as the prototype's `tagPose()` and `fitSwing()`: the tag always hangs
 * on its thread from the hole in the tab, a pendulum [Thread] dp long at an angle from straight
 * down (positive is out to the left). The tag's own frame has its eyelet at (13, 0), its body
 * reaching left from there, and is turned so the body hangs on along the thread. All in dp of the
 * list area.
 */
internal object ZipperTag {
    const val Thread = 30f

    /** Where the stiff cotton holds the tag at rest, and a little higher while the tab stands. */
    const val SwingRest = 1.35f
    const val SwingHeld = 1.62f
    const val SwingMin = .6f
    const val SwingMax = 2f

    /** How far inside the list area's top and bottom the tag stays, its shadow with it. */
    private const val PadTop = 3f
    private const val PadBottom = 6f

    // The tag's outline's corners, in its own frame.
    private val CornersX = floatArrayOf(-30f, 8f, 20f, 20f, 8f, -30f)
    private val CornersY = floatArrayOf(-22f, -22f, -9f, 9f, 22f, 22f)

    /** The tag's scale: lying on the page, and a little larger lifted, at [t] 0 to 1. */
    fun scale(t: Float) = .86f + .14f * t.coerceIn(0f, 1f)

    /** Where the hole in the tab is, below the pull's top [sy], with the tab at [flip]. */
    fun holeY(sy: Float, flip: Float): Float {
        val f = flip.coerceIn(0f, 1.4f)
        return sy + ZipperState.SliderHeight - 6.5f + ZipperState.Tab * cos(f) + 6f * sin(f) - 7f
    }

    /** The eyelet's offset from the hole, hung at [swing]. */
    fun eyeletDx(swing: Float) = -Thread * sin(swing)

    fun eyeletDy(swing: Float) = Thread * cos(swing)

    /** The angle the tag's frame is turned by, hung at [swing]. */
    fun angle(swing: Float) = swing - (PI / 2).toFloat()

    /** The tag's frame's origin's offset from the hole, hung at [swing] at scale [k]. */
    fun originDx(swing: Float, k: Float) = eyeletDx(swing) - k * 13f * cos(angle(swing))

    fun originDy(swing: Float, k: Float) = eyeletDy(swing) - k * 13f * sin(angle(swing))

    /** The highest (or, [bottom], lowest) the tag's outline reaches below the hole. */
    fun extentY(swing: Float, k: Float, bottom: Boolean): Float {
        val a = angle(swing)
        val ca = cos(a)
        val sa = sin(a)
        val oy = originDy(swing, k)
        var e = if (bottom) -Float.MAX_VALUE else Float.MAX_VALUE
        for (i in CornersX.indices) {
            val y = oy + k * (CornersX[i] * sa + CornersY[i] * ca)
            e = if (bottom) max(e, y) else min(e, y)
        }
        return e
    }

    /** The furthest left (or, [right], right) the tag's outline reaches from the hole. */
    fun extentX(swing: Float, k: Float, right: Boolean): Float {
        val a = angle(swing)
        val ca = cos(a)
        val sa = sin(a)
        val ox = originDx(swing, k)
        var e = if (right) -Float.MAX_VALUE else Float.MAX_VALUE
        for (i in CornersX.indices) {
            val x = ox + k * (CornersX[i] * ca - CornersY[i] * sa)
            e = if (right) max(e, x) else min(e, x)
        }
        return e
    }

    /**
     * The nearest angle to [swing] at which the tag, hung from a hole at [holeY] at scale [k],
     * stays inside a list area [height] tall: it rests on the bottom edge rather than hang off it,
     * and is pushed down from the top one.
     */
    fun fit(swing: Float, holeY: Float, k: Float, height: Float): Float {
        val th = swing.coerceIn(SwingMin, SwingMax)
        val low = holeY + extentY(th, k, bottom = true) > height - PadBottom
        val high = holeY + extentY(th, k, bottom = false) < PadTop
        if (!low && !high) return th
        var a = th
        var b = if (low) SwingMax else SwingMin
        repeat(12) {
            val m = (a + b) / 2f
            val out =
                if (low) holeY + extentY(m, k, bottom = true) > height - PadBottom
                else holeY + extentY(m, k, bottom = false) < PadTop
            if (out) a = m else b = m
        }
        return b
    }
}
