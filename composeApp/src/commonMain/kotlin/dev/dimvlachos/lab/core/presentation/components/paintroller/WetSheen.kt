package dev.dimvlachos.lab.core.presentation.components.paintroller

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import dev.dimvlachos.lab.core.presentation.canvas2d.imageBitmapOf
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The prototype's wet paint (`.w::after`) over an accent fill clipped to [shape]: a glossy sheen,
 * the roller's stripes and a stipple, screen-blended over the fill at [wet] (0 dry, 1 just
 * painted), read in the draw phase so drying never recomposes. [under] draws it under the element's
 * content, as a switch's knob sits over its sheen.
 */
internal fun Modifier.wetSheen(shape: Shape, under: Boolean = false, wet: () -> Float): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val clip = Path().apply { addOutline(outline) }
        val w = size.width
        val h = size.height
        // linear-gradient(172deg, …): the gradient line runs at 172° through the centre, as long
        // as the box's projection on it.
        val a = 172f * PI.toFloat() / 180f
        val dx = sin(a)
        val dy = -cos(a)
        val len = abs(w * dx) + abs(h * dy)
        val c = Offset(w / 2, h / 2)
        val gloss =
            Brush.linearGradient(
                0f to Color(1f, 1f, 1f, .42f),
                .34f to Color(1f, 1f, 1f, .1f),
                .52f to Color(1f, 1f, 1f, 0f),
                .78f to Color(1f, 1f, 1f, .18f),
                1f to Color(1f, 1f, 1f, 0f),
                start = c - Offset(dx, dy) * (len / 2),
                end = c + Offset(dx, dy) * (len / 2),
            )
        // repeating-linear-gradient(90deg, …): a dark and a light hairline every 9 px.
        val p = 9.dpPx(density)
        val stripes =
            Brush.linearGradient(
                0f to Color(0f, 0f, 0f, .07f),
                1 / 9f to Color(0f, 0f, 0f, .07f),
                1 / 9f to Color(1f, 1f, 1f, 0f),
                4 / 9f to Color(1f, 1f, 1f, 0f),
                4 / 9f to Color(1f, 1f, 1f, .08f),
                5 / 9f to Color(1f, 1f, 1f, .08f),
                5 / 9f to Color(1f, 1f, 1f, 0f),
                1f to Color(1f, 1f, 1f, 0f),
                start = Offset.Zero,
                end = Offset(p, 0f),
                tileMode = TileMode.Repeated,
            )
        val stipple =
            ShaderBrush(ImageShader(stippleTile(density), TileMode.Repeated, TileMode.Repeated))
        val layer = Paint().apply { blendMode = BlendMode.Screen }
        val bounds = Rect(0f, 0f, w, h)
        onDrawWithContent {
            if (!under) drawContent()
            val amount = wet()
            if (amount > .001f) {
                clipPath(clip) {
                    drawIntoCanvas { canvas ->
                        layer.alpha = amount.coerceAtMost(1f)
                        canvas.saveLayer(bounds, layer)
                        drawRect(gloss)
                        drawRect(stripes)
                        drawRect(stipple)
                        canvas.restore()
                    }
                }
            }
            if (under) drawContent()
        }
    }

private fun Int.dpPx(density: Float) = this * density

private val tiles = HashMap<Int, ImageBitmap>()

/**
 * The stipple: 64 px of SVG `feTurbulence` fractal noise (base frequency 1.1, two octaves, seed 0,
 * stitched) through the prototype's colour matrix, white where `2.2 α − 1.05` is above zero, baked
 * once for each [density] and sampled at CSS px.
 */
internal fun stippleTile(density: Float): ImageBitmap {
    val key = (density * 100).roundToInt()
    tiles[key]?.let {
        return it
    }
    val n = max1((64 * density).roundToInt())
    val px = IntArray(n * n)
    for (j in 0 until n) for (i in 0 until n) {
        val x = (i + .5) / n * 64
        val y = (j + .5) / n * 64
        val alpha = (Turbulence.fractal(3, x, y) + 1) / 2
        val v = (2.2 * alpha - 1.05).coerceIn(0.0, 1.0)
        px[j * n + i] = ((v * 255).roundToInt() shl 24) or 0xFFFFFF
    }
    return imageBitmapOf(px, n, n).also { tiles[key] = it }
}

private fun max1(v: Int) = if (v < 1) 1 else v

/** The SVG specification's reference turbulence, for a 64 × 64 tile at 1.1, two octaves, seed 0. */
internal object Turbulence {
    private const val BSize = 0x100
    private const val BM = 0xff
    private const val PerlinN = 0x1000
    private val lattice = IntArray(BSize + BSize + 2)
    private val gradient = Array(4) { Array(BSize + BSize + 2) { DoubleArray(2) } }

    init {
        var seed = setupSeed(0L)
        fun random(): Long {
            var r = 16807L * (seed % 127773L) - 2836L * (seed / 127773L)
            if (r <= 0) r += 2147483647L
            seed = r
            return r
        }
        for (k in 0 until 4) {
            for (i in 0 until BSize) {
                lattice[i] = i
                for (j in 0 until 2) {
                    gradient[k][i][j] = ((random() % (BSize + BSize)) - BSize).toDouble() / BSize
                }
                val g = gradient[k][i]
                val s = sqrt(g[0] * g[0] + g[1] * g[1])
                g[0] /= s
                g[1] /= s
            }
        }
        var i = BSize - 1
        while (i > 0) {
            val k = lattice[i]
            val j = (random() % BSize).toInt()
            lattice[i] = lattice[j]
            lattice[j] = k
            i--
        }
        for (ii in 0 until BSize + 2) {
            lattice[BSize + ii] = lattice[ii]
            for (k in 0 until 4) for (j in 0 until 2) gradient[k][BSize + ii][j] =
                gradient[k][ii][j]
        }
    }

    private fun setupSeed(s: Long): Long {
        var seed = s
        if (seed <= 0) seed = -(seed % (2147483647L - 1)) + 1
        if (seed > 2147483647L - 1) seed = 2147483647L - 1
        return seed
    }

    private class Stitch(var width: Int, var height: Int, var wrapX: Int, var wrapY: Int)

    private fun sCurve(t: Double) = t * t * (3 - 2 * t)

    private fun lerp(t: Double, a: Double, b: Double) = a + t * (b - a)

    private fun noise2(ch: Int, vx: Double, vy: Double, st: Stitch): Double {
        var t = vx + PerlinN
        // Masked only after the stitch, as Skia does: the specification's listing masks first,
        // which leaves the wrap test never true.
        var bx0 = t.toInt()
        var bx1 = bx0 + 1
        val rx0 = t - t.toInt()
        val rx1 = rx0 - 1
        t = vy + PerlinN
        var by0 = t.toInt()
        var by1 = by0 + 1
        val ry0 = t - t.toInt()
        val ry1 = ry0 - 1
        if (bx0 >= st.wrapX) bx0 -= st.width
        if (bx1 >= st.wrapX) bx1 -= st.width
        if (by0 >= st.wrapY) by0 -= st.height
        if (by1 >= st.wrapY) by1 -= st.height
        bx0 = bx0 and BM
        bx1 = bx1 and BM
        by0 = by0 and BM
        by1 = by1 and BM
        val i = lattice[bx0]
        val j = lattice[bx1]
        val b00 = lattice[i + by0]
        val b10 = lattice[j + by0]
        val b01 = lattice[i + by1]
        val b11 = lattice[j + by1]
        val sx = sCurve(rx0)
        val sy = sCurve(ry0)
        var q = gradient[ch][b00]
        var u = rx0 * q[0] + ry0 * q[1]
        q = gradient[ch][b10]
        var v = rx1 * q[0] + ry0 * q[1]
        val a = lerp(sx, u, v)
        q = gradient[ch][b01]
        u = rx0 * q[0] + ry1 * q[1]
        q = gradient[ch][b11]
        v = rx1 * q[0] + ry1 * q[1]
        val b = lerp(sx, u, v)
        return lerp(sy, a, b)
    }

    /** The fractal sum for channel [ch] (3 is alpha) at ([x], [y]) in the 64 px tile. */
    fun fractal(ch: Int, x: Double, y: Double): Double {
        val tile = 64.0
        fun adjust(f: Double): Double {
            val lo = floor(tile * f) / tile
            val hi = ceil(tile * f) / tile
            return if (f / lo < hi / f) lo else hi
        }
        val fx = adjust(1.1)
        val fy = adjust(1.1)
        val w = (tile * fx + .5).toInt()
        val h = (tile * fy + .5).toInt()
        val st = Stitch(w, h, PerlinN + w, PerlinN + h)
        var vx = x * fx
        var vy = y * fy
        var sum = 0.0
        var ratio = 1.0
        repeat(2) {
            sum += noise2(ch, vx, vy, st) / ratio
            vx *= 2
            vy *= 2
            ratio *= 2
            st.width *= 2
            st.wrapX = 2 * st.wrapX - PerlinN
            st.height *= 2
            st.wrapY = 2 * st.wrapY - PerlinN
        }
        return sum
    }
}
