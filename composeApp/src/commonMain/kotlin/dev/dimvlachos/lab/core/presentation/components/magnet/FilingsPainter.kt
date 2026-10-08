package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * in arrays made once; they are drawn as one path for each of a few brightnesses, never a call
 * each, and traced again only when a magnet moves.
 */
internal class FilingsPainter(count: Int = MagnetDimens.FilingCount, seed: Int = FilingsSeed) {
    private val x = FloatArray(count)
    private val y = FloatArray(count)
    private val angle = FloatArray(count)
    private val strength = FloatArray(count)
    private val mx = FloatArray(MagnetDimens.MostMagnets)
    private val my = FloatArray(MagnetDimens.MostMagnets)
    private val field = FloatArray(2)
    private val paths = Array(BucketAlpha.size) { Path() }
    private var stroke: Stroke? = null
    private var strokeDensity = 0f
    private var tracedVersion = -1
    private var tracedWidth = -1f
    private var tracedHeight = -1f

    init {
        val random = Random(seed)
        for (i in 0 until count) {
            x[i] = random.nextFloat()
            y[i] = random.nextFloat()
            angle[i] = random.nextFloat() * 2f * PI.toFloat()
        }
    }

    /**
     * Turns each filing to the field of [magnets] out of the strip, on a [width] × [height] dp
     * table.
     */
    fun align(magnets: List<Magnet>, width: Float, height: Float) {
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
            if (b > MagnetDimens.LieFlat) angle[i] = atan2(field[1], field[0])
            strength[i] = b.coerceAtMost(1f)
        }
    }

    fun angleOf(i: Int): Float = angle[i]

    fun strengthOf(i: Int): Float = strength[i]

    fun spotOf(i: Int): Offset = Offset(x[i], y[i])

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
            align(state.magnets, width, height)
            trace(width, height, density)
            tracedVersion = version
            tracedWidth = width
            tracedHeight = height
        }
        val line =
            stroke?.takeIf { strokeDensity == density }
                ?: Stroke(MagnetDimens.FilingWidth.toPx(), cap = StrokeCap.Round).also {
                    stroke = it
                    strokeDensity = density
                }
        for (b in paths.indices) drawPath(paths[b], color, alpha = BucketAlpha[b], style = line)
    }

    private fun trace(width: Float, height: Float, density: Float) {
        for (path in paths) path.reset()
        val flat = MagnetDimens.FilingLength.value
        val risen = MagnetDimens.RisenLength.value
        for (i in x.indices) {
            val s = strength[i]
            val bucket = (s * paths.size).toInt().coerceAtMost(paths.size - 1)
            val half = (flat + (risen - flat) * s) * density / 2f
            val cx = x[i] * width * density
            val cy = y[i] * height * density
            val dx = cos(angle[i]) * half
            val dy = sin(angle[i]) * half
            paths[bucket].moveTo(cx - dx, cy - dy)
            paths[bucket].lineTo(cx + dx, cy + dy)
        }
    }
}

// Lying flat the filings are barely there; risen in a strong field, they catch the light.
private val BucketAlpha = floatArrayOf(0.12f, 0.22f, 0.36f, 0.58f)

private const val FilingsSeed = 41
