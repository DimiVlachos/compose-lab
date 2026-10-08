package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * The iron filings dusted over the table: [count] short strokes, each turning to follow the field
 * of the magnets out where it lies, and rising, longer and brighter, where the field is strong.
 * Where it is too weak to turn them they lie as they fell. Their spots, angles and strengths live
 * in arrays made once, and so do the lines they are drawn as: one batch of raw line points for each
 * of a few brightnesses, never a call or a path contour each, traced again only when a magnet
 * moves. A path of a couple of thousand strokes cost a frame on its own, built and then stroked;
 * raw lines are drawn as they are.
 */
internal class FilingsPainter(count: Int = MagnetDimens.FilingCount, seed: Int = FilingsSeed) {
    private val x = FloatArray(count)
    private val y = FloatArray(count)
    private val angle = FloatArray(count)

    // The angle each filing fell at, and the one the field last turned it to.
    private val fell = FloatArray(count)
    private val turned = FloatArray(count)
    private val strength = FloatArray(count)
    private val mx = FloatArray(MagnetDimens.MostMagnets)
    private val my = FloatArray(MagnetDimens.MostMagnets)
    private val field = FloatArray(2)

    // Each brightness's lines, four floats a filing, room for every filing in each; what a
    // brightness doesn't use lies far off the table, so it draws nothing.
    private val points = Array(BucketAlpha.size) { FloatArray(count * 4) }
    private val traced = IntArray(BucketAlpha.size)
    private val paints = Array(BucketAlpha.size) { Paint().apply { strokeCap = StrokeCap.Butt } }
    private var tracedVersion = -1
    private var tracedWidth = -1f
    private var tracedHeight = -1f

    init {
        val random = Random(seed)
        for (i in 0 until count) {
            x[i] = random.nextFloat()
            y[i] = random.nextFloat()
            angle[i] = random.nextFloat() * 2f * PI.toFloat()
            fell[i] = angle[i]
            turned[i] = angle[i]
        }
    }

    /**
     * Turns each filing to the field of [magnets] out of the strip, on a [width] × [height] dp
     * table. Where the field is too weak to turn it, a filing lies [calm] of the way back from the
     * last angle the field gave it to the angle it fell at, as filings settle once a magnet goes.
     */
    fun align(magnets: List<Magnet>, width: Float, height: Float, calm: Float = 1f) {
        var n = 0
        for (magnet in magnets) {
            if (!magnet.out || n == mx.size) continue
            mx[n] = magnet.at.x
            my[n] = magnet.at.y
            n++
        }
        for (i in x.indices) {
            MagnetField.fieldAt(x[i] * width, y[i] * height, mx, my, n, field)
            val b = hypot(field[0], field[1])
            if (b > MagnetDimens.LieFlat) {
                turned[i] = atan2(field[1], field[0])
                angle[i] = turned[i]
            } else {
                angle[i] = turned[i] + axisTurn(turned[i], fell[i]) * calm
            }
            strength[i] = b.coerceAtMost(1f)
        }
    }

    fun angleOf(i: Int): Float = angle[i]

    fun strengthOf(i: Int): Float = strength[i]

    fun spotOf(i: Int): Offset = Offset(x[i], y[i])

    val buckets: Int
        get() = points.size

    fun pointsOf(bucket: Int): FloatArray = points[bucket]

    fun tracedIn(bucket: Int): Int = traced[bucket]

    /**
     * Draws the filings over [state]'s table in [color]. It reads only the field's version, so it
     * is drawn again when a magnet moves, not every frame.
     */
    fun DrawScope.drawFilings(state: MagnetState, color: Color) {
        val version = state.fieldVersion
        val width = state.width
        val height = state.tableHeight
        if (width <= 0f || height <= 0f) return
        if (version != tracedVersion || width != tracedWidth || height != tracedHeight) {
            align(state.magnets, width, height, state.calm)
            trace(width, height, density)
            tracedVersion = version
            tracedWidth = width
            tracedHeight = height
        }
        val lineWidth = MagnetDimens.FilingWidth.toPx()
        drawIntoCanvas { canvas ->
            for (b in points.indices) {
                if (traced[b] == 0) continue
                val paint = paints[b]
                paint.color = color
                paint.alpha = BucketAlpha[b]
                paint.strokeWidth = lineWidth
                canvas.drawRawPoints(PointMode.Lines, points[b], paint)
            }
        }
    }

    /** Lays each filing's line into its brightness's points, in px at [density]. */
    fun trace(width: Float, height: Float, density: Float) {
        traced.fill(0)
        val flat = MagnetDimens.FilingLength.value
        val risen = MagnetDimens.RisenLength.value
        for (i in x.indices) {
            val s = strength[i]
            val bucket = (s * points.size).toInt().coerceAtMost(points.size - 1)
            val half = (flat + (risen - flat) * s) * density / 2f
            val cx = x[i] * width * density
            val cy = y[i] * height * density
            val dx = cos(angle[i]) * half
            val dy = sin(angle[i]) * half
            val line = points[bucket]
            val k = traced[bucket]++ * 4
            line[k] = cx - dx
            line[k + 1] = cy - dy
            line[k + 2] = cx + dx
            line[k + 3] = cy + dy
        }
        for (b in points.indices) points[b].fill(OffTable, traced[b] * 4)
    }
}

// Lying flat the filings are barely there; risen in a strong field, they catch the light.
private val BucketAlpha = floatArrayOf(0.12f, 0.22f, 0.36f, 0.58f)

private const val FilingsSeed = 41

// The shorter turn from line [from] to line [to]: a filing has no head, so at most a quarter turn
// either way.
private fun axisTurn(from: Float, to: Float): Float {
    val pi = PI.toFloat()
    var d = (to - from) % pi
    if (d < -pi / 2f) d += pi
    if (d > pi / 2f) d -= pi
    return d
}

// Where a brightness's spare lines lie: far off the table, where they draw nothing.
private const val OffTable = -1_000f
