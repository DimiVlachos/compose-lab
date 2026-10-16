package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

internal const val TAU = (2 * PI).toFloat()

/**
 * The prototype's seeded random numbers (`mulberry32`), so the same seed lays out the same village
 * and the same flakes as the HTML sketch did.
 */
internal fun mulberry(seed: Int): () -> Double {
    var s = seed
    return {
        s += 0x6D2B79F5
        var t = (s xor (s ushr 15)) * (1 or s)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        ((t xor (t ushr 14)).toUInt().toDouble()) / 4294967296.0
    }
}

private val cssCache = HashMap<String, Color>()

/** A CSS colour as the prototype writes them: `#rgb`, `#rrggbb`, `rgb(...)` or `rgba(...)`. */
internal fun cssColor(css: String): Color =
    cssCache.getOrPut(css) {
        val s = css.trim()
        if (s.startsWith("#")) {
            var h = s.substring(1)
            if (h.length == 3) h = h.map { "$it$it" }.joinToString("")
            val n = h.toLong(16)
            Color(
                red = (n shr 16 and 255).toInt(),
                green = (n shr 8 and 255).toInt(),
                blue = (n and 255).toInt(),
            )
        } else {
            val parts = s.substringAfter("(").substringBefore(")").split(",").map { it.trim() }
            val a = if (parts.size > 3) parts[3].toFloat() else 1f
            Color(
                red = parts[0].toFloat().toInt(),
                green = parts[1].toFloat().toInt(),
                blue = parts[2].toFloat().toInt(),
                alpha = (a.coerceIn(0f, 1f) * 255f).toInt(),
            )
        }
    }

/** A gradient made by [Canvas2D.createLinearGradient] or [Canvas2D.createRadialGradient]. */
internal class Gradient2D
private constructor(
    private val linear: Boolean,
    private val x0: Float,
    private val y0: Float,
    private val r0: Float,
    private val x1: Float,
    private val y1: Float,
    private val r1: Float,
) {
    private val stops = mutableListOf<Pair<Float, Color>>()

    fun addColorStop(offset: Float, color: String) {
        stops += offset to cssColor(color)
    }

    fun addColorStop(offset: Float, color: Color) {
        stops += offset to color
    }

    internal fun shader(): Shader {
        val sorted = stops.sortedBy { it.first }.ifEmpty { listOf(0f to Color.Transparent) }
        val colors = sorted.map { it.second }.let { if (it.size == 1) it + it else it }
        val offsets = sorted.map { it.first }.let { if (it.size == 1) listOf(0f, 1f) else it }
        return if (linear) {
            LinearGradientShader(Offset(x0, y0), Offset(x1, y1), colors, offsets)
        } else {
            // The prototype's radial gradients share a centre (or nearly): the inner circle's
            // radius moves the stops out along the outer one.
            val outer = max(r1, 1e-4f)
            val mapped = offsets.map { (r0 + it * (r1 - r0)) / outer }
            RadialGradientShader(Offset(x1, y1), outer, colors, mapped)
        }
    }

    internal companion object {
        fun linear(x0: Float, y0: Float, x1: Float, y1: Float) =
            Gradient2D(true, x0, y0, 0f, x1, y1, 0f)

        fun radial(x0: Float, y0: Float, r0: Float, x1: Float, y1: Float, r1: Float) =
            Gradient2D(false, x0, y0, r0, x1, y1, r1)
    }
}

/**
 * Enough of the browser's CanvasRenderingContext2D for the snow globe's painters to be ported line
 * by line from the HTML prototype: the same calls, in the same order, with the same numbers. It
 * draws into a Compose [Canvas]; [create] gives one over a fresh [ImageBitmap] to bake a layer
 * once.
 *
 * Paths are kept in user space and drawn under the transform current when they are filled or
 * stroked, which is how the prototype uses them.
 */
internal class Canvas2D(private val canvas: Canvas, val width: Int, val height: Int) {
    private var image: ImageBitmap? = null

    /** The bitmap this draws into, when it was made by [create]. */
    val bitmap: ImageBitmap
        get() = checkNotNull(image) { "This Canvas2D draws into a canvas it doesn't own" }

    private class State(
        var matrix: Matrix,
        var fillStyle: Any,
        var strokeStyle: Any,
        var lineWidth: Float,
        var globalAlpha: Float,
        var composite: String,
        var lineCap: String,
        var filterBlur: Float,
        var shadowColor: Color,
        var shadowBlur: Float,
        var shadowOffsetY: Float,
    ) {
        fun copy() =
            State(
                Matrix(matrix.values.copyOf()),
                fillStyle,
                strokeStyle,
                lineWidth,
                globalAlpha,
                composite,
                lineCap,
                filterBlur,
                shadowColor,
                shadowBlur,
                shadowOffsetY,
            )
    }

    private var state =
        State(
            Matrix(),
            "#000000",
            "#000000",
            1f,
            1f,
            "source-over",
            "butt",
            0f,
            Color.Transparent,
            0f,
            0f,
        )
    private val stack = ArrayDeque<State>()
    private var path = Path()
    private val paint = Paint()

    var fillStyle: Any
        get() = state.fillStyle
        set(value) {
            state.fillStyle = value
        }

    var strokeStyle: Any
        get() = state.strokeStyle
        set(value) {
            state.strokeStyle = value
        }

    var lineWidth: Float
        get() = state.lineWidth
        set(value) {
            state.lineWidth = value
        }

    var globalAlpha: Float
        get() = state.globalAlpha
        set(value) {
            state.globalAlpha = value.coerceIn(0f, 1f)
        }

    var globalCompositeOperation: String
        get() = state.composite
        set(value) {
            state.composite = value
        }

    var lineCap: String
        get() = state.lineCap
        set(value) {
            state.lineCap = value
        }

    /** `blur(Npx)` or `none`, as the prototype sets it. */
    var filter: String = "none"
        set(value) {
            field = value
            state.filterBlur =
                if (value.startsWith("blur("))
                    value.removePrefix("blur(").removeSuffix(")").removeSuffix("px").toFloat()
                else 0f
        }

    var shadowColor: String = "rgba(0,0,0,0)"
        set(value) {
            field = value
            state.shadowColor = cssColor(value)
        }

    var shadowBlur: Float
        get() = state.shadowBlur
        set(value) {
            state.shadowBlur = value
        }

    var shadowOffsetY: Float
        get() = state.shadowOffsetY
        set(value) {
            state.shadowOffsetY = value
        }

    // ---- State and transform ----

    fun save() {
        stack.addLast(state.copy())
        canvas.save()
    }

    fun restore() {
        if (stack.isEmpty()) return
        state = stack.removeLast()
        filter = filter
        canvas.restore()
    }

    fun setTransform(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) {
        state.matrix =
            Matrix().apply {
                values[Matrix.ScaleX] = a
                values[Matrix.SkewY] = b
                values[Matrix.SkewX] = c
                values[Matrix.ScaleY] = d
                values[Matrix.TranslateX] = e
                values[Matrix.TranslateY] = f
            }
    }

    fun translate(x: Float, y: Float) = state.matrix.translate(x, y)

    fun scale(x: Float, y: Float) = state.matrix.scale(x, y)

    fun rotate(radians: Float) = state.matrix.rotateZ(radians * 180f / PI.toFloat())

    // ---- Paths ----

    fun beginPath() {
        // Reused: every path is drawn as soon as it is built, and a frame builds hundreds.
        path.reset()
    }

    fun moveTo(x: Float, y: Float) = path.moveTo(x, y)

    fun lineTo(x: Float, y: Float) = path.lineTo(x, y)

    fun quadraticCurveTo(cx: Float, cy: Float, x: Float, y: Float) = path.quadraticTo(cx, cy, x, y)

    fun bezierCurveTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) =
        path.cubicTo(c1x, c1y, c2x, c2y, x, y)

    fun closePath() = path.close()

    fun rect(x: Float, y: Float, w: Float, h: Float) {
        path.moveTo(x, y)
        path.lineTo(x + w, y)
        path.lineTo(x + w, y + h)
        path.lineTo(x, y + h)
        path.close()
    }

    fun arc(x: Float, y: Float, r: Float, a0: Float, a1: Float, ccw: Boolean = false) =
        ellipse(x, y, r, r, 0f, a0, a1, ccw)

    fun ellipse(
        x: Float,
        y: Float,
        rx: Float,
        ry: Float,
        rotation: Float,
        a0: Float,
        a1: Float,
        ccw: Boolean = false,
    ) {
        var sweep = a1 - a0
        if (!ccw && sweep < 0f) sweep = (sweep % TAU) + TAU
        if (ccw && sweep > 0f) sweep = (sweep % TAU) - TAU
        if (abs(sweep) > TAU) sweep = if (sweep > 0) TAU else -TAU
        val full = abs(sweep) >= TAU - 1e-4f
        val piece = Path()
        val bounds = Rect(-rx, -ry, rx, ry)
        if (full) {
            // A whole turn: two halves, which every platform draws (a single 360° arcTo may not).
            val start = a0 * 180f / PI.toFloat()
            val half = if (sweep > 0) 180f else -180f
            piece.arcTo(bounds, start, half, forceMoveTo = true)
            piece.arcTo(bounds, start + half, half, forceMoveTo = false)
        } else {
            piece.arcTo(
                bounds,
                a0 * 180f / PI.toFloat(),
                sweep * 180f / PI.toFloat(),
                forceMoveTo = true,
            )
        }
        val m = Matrix()
        m.translate(x, y)
        if (rotation != 0f) m.rotateZ(rotation * 180f / PI.toFloat())
        piece.transform(m)
        if (path.isEmpty) path.addPath(piece) else connectAndAppend(piece)
    }

    // Canvas2D joins an arc to the path with a straight line from where the path was.
    private fun connectAndAppend(piece: Path) {
        val start = piece.getBounds().let { _ -> firstPoint(piece) }
        if (start != null) path.lineTo(start.x, start.y)
        path.addPath(piece)
    }

    private fun firstPoint(p: Path): Offset? {
        val measure = androidx.compose.ui.graphics.PathMeasure()
        measure.setPath(p, false)
        return if (measure.length > 0f) measure.getPosition(0f) else null
    }

    // ---- Drawing ----

    private fun styled(style: Any, fill: Boolean): Paint {
        paint.shader = null
        paint.style = if (fill) PaintingStyle.Fill else PaintingStyle.Stroke
        paint.strokeWidth = state.lineWidth
        paint.strokeCap =
            when (state.lineCap) {
                "round" -> StrokeCap.Round
                "square" -> StrokeCap.Square
                else -> StrokeCap.Butt
            }
        paint.strokeJoin = StrokeJoin.Miter
        paint.blendMode =
            when (state.composite) {
                "lighter" -> BlendMode.Plus
                "destination-out" -> BlendMode.DstOut
                "source-atop" -> BlendMode.SrcAtop
                "multiply" -> BlendMode.Multiply
                "screen" -> BlendMode.Screen
                else -> BlendMode.SrcOver
            }
        when (style) {
            is Gradient2D -> {
                paint.color = Color.Black
                paint.shader = style.shader()
                paint.alpha = state.globalAlpha
            }
            is Color -> paint.color = style.copy(alpha = style.alpha * state.globalAlpha)
            else -> {
                val c = cssColor(style.toString())
                paint.color = c.copy(alpha = c.alpha * state.globalAlpha)
            }
        }
        paint.setBlur(state.filterBlur * scaleOf())
        return paint
    }

    // Blur radii are given in pixels but drawn under the transform, which scales them: one pixel
    // in user units.
    private fun scaleOf(): Float {
        val v = state.matrix.values
        val s =
            kotlin.math.sqrt(
                v[Matrix.ScaleX] * v[Matrix.ScaleX] + v[Matrix.SkewY] * v[Matrix.SkewY]
            )
        return if (s > 0f) 1f / s else 1f
    }

    private inline fun transformed(block: () -> Unit) {
        canvas.save()
        canvas.concat(state.matrix)
        block()
        canvas.restore()
    }

    private fun shadowed(p: Path, fill: Boolean) {
        if (state.shadowBlur <= 0f || state.shadowColor.alpha <= 0f) return
        val shadow = styled(state.shadowColor, fill)
        shadow.setBlur(state.shadowBlur / 2f * scaleOf())
        canvas.save()
        canvas.translate(0f, state.shadowOffsetY)
        canvas.concat(state.matrix)
        canvas.drawPath(p, shadow)
        canvas.restore()
    }

    fun fill(rule: String = "nonzero") {
        path.fillType = if (rule == "evenodd") PathFillType.EvenOdd else PathFillType.NonZero
        shadowed(path, true)
        val p = styled(state.fillStyle, true)
        transformed { canvas.drawPath(path, p) }
    }

    fun stroke() {
        shadowed(path, false)
        val p = styled(state.strokeStyle, false)
        transformed { canvas.drawPath(path, p) }
    }

    fun fillRect(x: Float, y: Float, w: Float, h: Float) {
        val r = Path().apply { addRect(Rect(x, y, x + w, y + h)) }
        shadowed(r, true)
        val p = styled(state.fillStyle, true)
        transformed { canvas.drawRect(Rect(x, y, x + w, y + h), p) }
    }

    fun clearRect(x: Float, y: Float, w: Float, h: Float) {
        val p = Paint().apply { blendMode = BlendMode.Clear }
        transformed { canvas.drawRect(Rect(x, y, x + w, y + h), p) }
    }

    fun clip() {
        val device =
            Path().apply {
                addPath(path)
                transform(state.matrix)
            }
        canvas.clipPath(device)
    }

    fun createLinearGradient(x0: Float, y0: Float, x1: Float, y1: Float) =
        Gradient2D.linear(x0, y0, x1, y1)

    fun createRadialGradient(x0: Float, y0: Float, r0: Float, x1: Float, y1: Float, r1: Float) =
        Gradient2D.radial(x0, y0, r0, x1, y1, r1)

    /** Draws [image] into the rectangle at ([dx], [dy]) of [dw] by [dh]. */
    fun drawImage(
        image: ImageBitmap,
        dx: Float,
        dy: Float,
        dw: Float = image.width.toFloat(),
        dh: Float = image.height.toFloat(),
    ) = drawImage(image, 0f, 0f, image.width.toFloat(), image.height.toFloat(), dx, dy, dw, dh)

    fun drawImage(
        image: ImageBitmap,
        sx: Float,
        sy: Float,
        sw: Float,
        sh: Float,
        dx: Float,
        dy: Float,
        dw: Float,
        dh: Float,
    ) {
        val p = styled(Color.White, true)
        p.color = Color.White.copy(alpha = state.globalAlpha)
        transformed {
            // Drawn into a unit box under a scale, so fractional destinations land where they
            // should.
            canvas.save()
            canvas.translate(dx, dy)
            canvas.scale(dw / sw, dh / sh)
            canvas.drawImageRect(
                image,
                IntOffset(sx.toInt(), sy.toInt()),
                IntSize(sw.toInt().coerceAtLeast(1), sh.toInt().coerceAtLeast(1)),
                IntOffset.Zero,
                IntSize(sw.toInt().coerceAtLeast(1), sh.toInt().coerceAtLeast(1)),
                p,
            )
            canvas.restore()
        }
    }

    fun drawImage(
        source: Canvas2D,
        dx: Float,
        dy: Float,
        dw: Float = source.width.toFloat(),
        dh: Float = source.height.toFloat(),
    ) = drawImage(source.bitmap, dx, dy, dw, dh)

    internal companion object {
        /** A fresh, transparent [width] by [height] bitmap to paint into. */
        fun create(width: Int, height: Int): Canvas2D {
            val image = ImageBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
            return Canvas2D(Canvas(image), width, height).also { it.image = image }
        }
    }
}

/**
 * Blurs what [this] paints by a Gaussian of [sigma] pixels, as CSS `blur()` does; 0 turns it off.
 */
internal expect fun Paint.setBlur(sigma: Float)
