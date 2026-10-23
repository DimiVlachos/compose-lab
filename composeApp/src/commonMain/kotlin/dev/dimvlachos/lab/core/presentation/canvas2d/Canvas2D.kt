package dev.dimvlachos.lab.core.presentation.canvas2d

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.concurrent.Volatile
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

// Copy on write: the scenes paint on a background thread while the room draws on the main one,
// and a plain HashMap written from both corrupts. A lost write only means parsing again.
@Volatile private var cssCache: Map<String, Color> = emptyMap()

/** A CSS colour as the prototype writes them: `#rgb`, `#rrggbb`, `rgb(...)` or `rgba(...)`. */
internal fun cssColor(css: String): Color =
    cssCache[css] ?: parseCss(css).also { cssCache = cssCache + (css to it) }

private fun parseCss(css: String): Color = run {
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
            // The browser's two-circle gradient: from the inner circle to the outer one, so a
            // highlight offset up and to the left (the dome's, the clouds') stays there.
            twoPointConicalShader(x0, y0, r0, x1, y1, max(r1, 1e-4f), colors, offsets)
        }
    }

    internal companion object {
        fun linear(x0: Float, y0: Float, x1: Float, y1: Float) =
            Gradient2D(true, x0, y0, 0f, x1, y1, 0f)

        fun radial(x0: Float, y0: Float, r0: Float, x1: Float, y1: Float, r1: Float) =
            Gradient2D(false, x0, y0, r0, x1, y1, r1)
    }
}

/** An image repeated across what is filled, made by [Canvas2D.createPattern]. */
internal class Pattern2D(internal val image: ImageBitmap)

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
        var lineJoin: String,
        var filterBlur: Float,
        var shadowColor: Color,
        var shadowBlur: Float,
        var shadowOffsetX: Float,
        var shadowOffsetY: Float,
        var imageShade: Color,
        var lineDash: FloatArray? = null,
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
                lineJoin,
                filterBlur,
                shadowColor,
                shadowBlur,
                shadowOffsetX,
                shadowOffsetY,
                imageShade,
                lineDash,
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
            "miter",
            0f,
            Color.Transparent,
            0f,
            0f,
            0f,
            Color.Transparent,
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

    /** `miter`, `round` or `bevel`. */
    var lineJoin: String
        get() = state.lineJoin
        set(value) {
            state.lineJoin = value
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

    /**
     * Not the canvas's own: sets the shadow colour from a [Color], so a colour that changes every
     * frame needn't be written out and parsed as CSS.
     */
    fun setShadowColor(color: Color) {
        state.shadowColor = color
    }

    var shadowBlur: Float
        get() = state.shadowBlur
        set(value) {
            state.shadowBlur = value
        }

    var shadowOffsetX: Float
        get() = state.shadowOffsetX
        set(value) {
            state.shadowOffsetX = value
        }

    var shadowOffsetY: Float
        get() = state.shadowOffsetY
        set(value) {
            state.shadowOffsetY = value
        }

    /**
     * Measures and lays out [fillText] and [measureText]. Text needs one; it is measured at a
     * density of 1, so a font's px are user units, and drawn under the transform.
     */
    var textMeasurer: TextMeasurer? = null

    /** The families a [font] string names, by the name it uses for them. */
    var fonts: Map<String, FontFamily> = emptyMap()

    /** `<weight> <size>px <family>`, as the prototype writes it; the family is a key of [fonts]. */
    var font: String = "400 10px sans-serif"

    /** `left`, `right` or `center`. */
    var textAlign: String = "left"

    /** `alphabetic`, `middle` or `top`. */
    var textBaseline: String = "alphabetic"

    /**
     * Not the canvas's own: how many times its size text is laid out at, then drawn scaled back
     * down, so [fillText] and [measureText] keep their user units. Laid out at its size, a small
     * font under a large transform takes the advances a few pixels' text would get, rounded, and
     * its spaces close up; set this to about the transform's scale to lay it out as it will be
     * seen. 1, the default, lays it out at its size.
     */
    var textResolution: Float = 1f
        set(value) {
            field = if (value.isFinite() && value > 0f) value else 1f
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

    /** Multiplies the transform by the matrix [a]…[f], as the browser's `transform` does. */
    fun transform(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) {
        val m =
            Matrix().apply {
                values[Matrix.ScaleX] = a
                values[Matrix.SkewY] = b
                values[Matrix.SkewX] = c
                values[Matrix.ScaleY] = d
                values[Matrix.TranslateX] = e
                values[Matrix.TranslateY] = f
            }
        m.timesAssign(state.matrix)
        state.matrix = m
    }

    /**
     * Dashes for [stroke], as the browser's `setLineDash`: on and off lengths in user units. An odd
     * list is doubled, as the browser does, and one with a negative or non-finite length is
     * ignored.
     */
    fun setLineDash(segments: FloatArray) {
        if (segments.any { it < 0f || !it.isFinite() }) return
        state.lineDash =
            when {
                segments.isEmpty() || segments.all { it == 0f } -> null
                segments.size % 2 == 1 -> segments + segments
                else -> segments.copyOf()
            }
    }

    // ---- Paths ----

    fun beginPath() {
        // Reused: every path is drawn as soon as it is built, and a frame builds hundreds.
        path.reset()
        current = null
    }

    // Where the path is, and where its shape began, which arcTo needs and Path doesn't tell.
    private var current: Offset? = null
    private var shapeStart = Offset.Zero

    fun moveTo(x: Float, y: Float) {
        path.moveTo(x, y)
        current = Offset(x, y)
        shapeStart = Offset(x, y)
    }

    fun lineTo(x: Float, y: Float) {
        path.lineTo(x, y)
        current = Offset(x, y)
    }

    fun quadraticCurveTo(cx: Float, cy: Float, x: Float, y: Float) {
        path.quadraticTo(cx, cy, x, y)
        current = Offset(x, y)
    }

    fun bezierCurveTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) {
        path.cubicTo(c1x, c1y, c2x, c2y, x, y)
        current = Offset(x, y)
    }

    fun closePath() {
        path.close()
        current = shapeStart
    }

    /**
     * The browser's `arcTo`: a line from where the path is towards ([x1], [y1]), rounded into the
     * line on to ([x2], [y2]) by an arc of radius [r] that touches both.
     */
    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, r: Float) {
        val p0 = current ?: return moveTo(x1, y1)
        val u = Offset(p0.x - x1, p0.y - y1)
        val v = Offset(x2 - x1, y2 - y1)
        val lu = u.getDistance()
        val lv = v.getDistance()
        val cross = u.x * v.y - u.y * v.x
        if (r <= 0f || lu < 1e-6f || lv < 1e-6f || abs(cross) < 1e-6f * lu * lv) {
            return lineTo(x1, y1)
        }
        val a = u / lu
        val b = v / lv
        val theta = kotlin.math.acos((a.x * b.x + a.y * b.y).coerceIn(-1f, 1f))
        val d = r / kotlin.math.tan(theta / 2f)
        val t1 = Offset(x1 + a.x * d, y1 + a.y * d)
        val t2 = Offset(x1 + b.x * d, y1 + b.y * d)
        val bis = (a + b).let { it / it.getDistance() }
        val k = r / kotlin.math.sin(theta / 2f)
        val c = Offset(x1 + bis.x * k, y1 + bis.y * k)
        val start = kotlin.math.atan2(t1.y - c.y, t1.x - c.x)
        var sweep = kotlin.math.atan2(t2.y - c.y, t2.x - c.x) - start
        while (sweep > PI) sweep -= TAU
        while (sweep < -PI) sweep += TAU
        path.lineTo(t1.x, t1.y)
        val deg = 180f / PI.toFloat()
        path.arcTo(Rect(c.x - r, c.y - r, c.x + r, c.y + r), start * deg, sweep * deg, false)
        current = t2
    }

    fun rect(x: Float, y: Float, w: Float, h: Float) {
        path.moveTo(x, y)
        path.lineTo(x + w, y)
        path.lineTo(x + w, y + h)
        path.lineTo(x, y + h)
        path.close()
        // The browser's rect leaves the path at its corner, a new shape begun there.
        current = Offset(x, y)
        shapeStart = Offset(x, y)
    }

    fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        roundRect(x, y, w, h, floatArrayOf(r))

    /**
     * A rounded rectangle as a new shape in the path. [radii] go as the browser takes them: one for
     * every corner, or top-left and top-right, then bottom-right and bottom-left.
     */
    fun roundRect(x: Float, y: Float, w: Float, h: Float, radii: FloatArray) {
        val (tl, tr, br, bl) =
            when (radii.size) {
                1 -> listOf(radii[0], radii[0], radii[0], radii[0])
                2 -> listOf(radii[0], radii[1], radii[0], radii[1])
                3 -> listOf(radii[0], radii[1], radii[2], radii[1])
                else -> listOf(radii[0], radii[1], radii[2], radii[3])
            }
        path.addRoundRect(
            RoundRect(
                Rect(x, y, x + w, y + h),
                CornerRadius(tl),
                CornerRadius(tr),
                CornerRadius(br),
                CornerRadius(bl),
            ),
        )
        current = Offset(x, y)
        shapeStart = Offset(x, y)
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
        // Where the arc starts and ends, for an arcTo after it.
        val cosR = kotlin.math.cos(rotation)
        val sinR = kotlin.math.sin(rotation)
        fun pointAt(a: Float): Offset {
            val px = rx * kotlin.math.cos(a)
            val py = ry * kotlin.math.sin(a)
            return Offset(x + px * cosR - py * sinR, y + px * sinR + py * cosR)
        }
        if (current == null) shapeStart = pointAt(a0)
        current = pointAt(a0 + sweep)
        if (rotation == 0f) {
            // The arc continues the current shape, joined by a line from where the path was, as
            // the browser's does; a separate contour would close on its own and leave gaps.
            val box = Rect(x - rx, y - ry, x + rx, y + ry)
            val start = a0 * 180f / PI.toFloat()
            val move = path.isEmpty
            if (full) {
                val half = if (sweep > 0) 180f else -180f
                path.arcTo(box, start, half, forceMoveTo = move)
                path.arcTo(box, start + half, half, forceMoveTo = false)
            } else {
                path.arcTo(box, start, sweep * 180f / PI.toFloat(), forceMoveTo = move)
            }
            return
        }
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
        paint.colorFilter = null
        paint.style = if (fill) PaintingStyle.Fill else PaintingStyle.Stroke
        paint.strokeWidth = state.lineWidth
        paint.strokeCap =
            when (state.lineCap) {
                "round" -> StrokeCap.Round
                "square" -> StrokeCap.Square
                else -> StrokeCap.Butt
            }
        paint.strokeJoin =
            when (state.lineJoin) {
                "round" -> StrokeJoin.Round
                "bevel" -> StrokeJoin.Bevel
                else -> StrokeJoin.Miter
            }
        paint.pathEffect = state.lineDash?.takeIf { !fill }?.let { PathEffect.dashPathEffect(it) }
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
            is Pattern2D -> {
                paint.color = Color.Black
                paint.shader = ImageShader(style.image, TileMode.Repeated, TileMode.Repeated)
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
                v[Matrix.ScaleX] * v[Matrix.ScaleX] + v[Matrix.SkewY] * v[Matrix.SkewY],
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
        canvas.translate(state.shadowOffsetX, state.shadowOffsetY)
        canvas.concat(state.matrix)
        canvas.drawPath(p, shadow)
        canvas.restore()
    }

    fun fill(rule: String = "nonzero") {
        path.fillType = if (rule == "evenodd") PathFillType.EvenOdd else PathFillType.NonZero
        shadowed(path, true)
        val p = styled(state.fillStyle, true)
        drawStyled(path, p, state.fillStyle)
    }

    /**
     * Not the canvas's own: strokes only the shadow the current path casts, as the browser's trick
     * of stroking a shape far off the canvas with a shadow offset to bring it back does.
     */
    fun strokeShadow() = shadowed(path, false)

    fun stroke() {
        shadowed(path, false)
        val p = styled(state.strokeStyle, false)
        drawStyled(path, p, state.strokeStyle)
    }

    // A blurred solid shape is drawn in the canvas's own pixels, with its sigma in pixels, as the
    // browser's filter is: under a large transform the sigma in user units would be a fraction of
    // a unit, which Android's BlurMaskFilter can't express and drops. A gradient keeps its user
    // space coordinates, so it draws under the transform.
    private fun drawStyled(shape: Path, p: Paint, style: Any) {
        if (state.filterBlur > 0f && style !is Gradient2D && style !is Pattern2D) {
            val device = Path().apply { addPath(shape) }
            device.transform(state.matrix)
            p.strokeWidth = state.lineWidth / scaleOf()
            p.setBlur(state.filterBlur)
            canvas.drawPath(device, p)
        } else {
            transformed { canvas.drawPath(shape, p) }
        }
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

    /**
     * Not the canvas's own: a colour each [drawImage] is shaded toward, by its alpha, over the
     * image's own pixels only, so what the image leaves clear stays clear. Saved and restored.
     */
    var imageShade: Color
        get() = state.imageShade
        set(value) {
            state.imageShade = value
        }

    /**
     * Not the canvas's own: [save], painting from here into a layer of its own that [endLayer] lays
     * down, so a `source-atop` in it lands only on what was painted in it.
     */
    fun beginLayer() {
        stack.addLast(state.copy())
        canvas.saveLayer(Rect(0f, 0f, width.toFloat(), height.toFloat()), Paint())
    }

    /** Lays down the layer [beginLayer] began, restoring what was saved with it. */
    fun endLayer() = restore()

    fun clip(rule: String = "nonzero") {
        val device =
            Path().apply {
                addPath(path)
                fillType = if (rule == "evenodd") PathFillType.EvenOdd else PathFillType.NonZero
                transform(state.matrix)
            }
        canvas.clipPath(device)
    }

    fun createLinearGradient(x0: Float, y0: Float, x1: Float, y1: Float) =
        Gradient2D.linear(x0, y0, x1, y1)

    fun createRadialGradient(x0: Float, y0: Float, r0: Float, x1: Float, y1: Float, r1: Float) =
        Gradient2D.radial(x0, y0, r0, x1, y1, r1)

    fun createPattern(image: ImageBitmap) = Pattern2D(image)

    // ---- Text ----

    private fun layout(text: String): TextLayoutResult {
        val measurer = checkNotNull(textMeasurer) { "Text needs a textMeasurer" }
        val parts = font.trim().split(Regex("\\s+"), limit = 3)
        val weight = parts[0].toIntOrNull() ?: 400
        val size = parts.getOrNull(1)?.removeSuffix("px")?.toFloatOrNull() ?: 10f
        val family = fonts[parts.getOrNull(2)?.trim('"', '\'')] ?: FontFamily.Default
        val style =
            TextStyle(
                fontFamily = family,
                fontWeight = FontWeight(weight.coerceIn(1, 1000)),
                fontSize = with(UnitDensity) { (size * textResolution).toSp() },
            )
        return measurer.measure(text, style, softWrap = false, density = UnitDensity)
    }

    /** How wide [text] is in the current [font], in user units. */
    fun measureText(text: String): Float =
        layout(text).multiParagraph.getLineWidth(0) / textResolution

    /** Draws [text] at ([x], [y]) in the fill colour, placed by [textAlign] and [textBaseline]. */
    fun fillText(text: String, x: Float, y: Float) {
        val l = layout(text)
        val k = textResolution
        val width = l.multiParagraph.getLineWidth(0) / k
        val dx =
            when (textAlign) {
                "right",
                "end" -> -width
                "center" -> -width / 2f
                else -> 0f
            }
        val dy =
            when (textBaseline) {
                "middle" -> -l.size.height / 2f / k
                "top" -> 0f
                else -> -l.firstBaseline / k
            }
        val style = state.fillStyle
        val c = if (style is Color) style else cssColor(style.toString())
        val ink = c.copy(alpha = c.alpha * state.globalAlpha)
        transformed {
            canvas.translate(x + dx, y + dy)
            if (k != 1f) canvas.scale(1f / k, 1f / k)
            l.multiParagraph.paint(canvas, ink)
        }
    }

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
        val shade = state.imageShade
        if (shade.alpha > 0f) {
            val a = shade.alpha
            val k = 1 - a
            p.colorFilter =
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            k,
                            0f,
                            0f,
                            0f,
                            shade.red * a * 255,
                            0f,
                            k,
                            0f,
                            0f,
                            shade.green * a * 255,
                            0f,
                            0f,
                            k,
                            0f,
                            shade.blue * a * 255,
                            0f,
                            0f,
                            0f,
                            1f,
                            0f,
                        ),
                    ),
                )
        }
        if (sx % 1f != 0f || sy % 1f != 0f || sw % 1f != 0f || sh % 1f != 0f) {
            // A source between pixels, as a card's strips are: the whole image is scaled so the
            // source rectangle lands on the destination, and clipped to it.
            transformed {
                canvas.save()
                canvas.clipRect(Rect(dx, dy, dx + dw, dy + dh))
                canvas.translate(dx, dy)
                canvas.scale(dw / sw, dh / sh)
                canvas.translate(-sx, -sy)
                canvas.drawImage(image, Offset.Zero, p)
                canvas.restore()
            }
            return
        }
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

private val UnitDensity = Density(1f, 1f)

/**
 * Blurs what [this] paints by a Gaussian of [sigma] pixels, as CSS `blur()` does; 0 turns it off.
 */
internal expect fun Paint.setBlur(sigma: Float)

/**
 * The canvas's two-circle radial gradient: [colors] at [offsets] from the circle at ([x0], [y0]) of
 * [r0] out to the one at ([x1], [y1]) of [r1].
 */
internal expect fun twoPointConicalShader(
    x0: Float,
    y0: Float,
    r0: Float,
    x1: Float,
    y1: Float,
    r1: Float,
    colors: List<Color>,
    offsets: List<Float>,
): Shader

/**
 * Marks a bitmap that is done being painted as unchanging, so it is drawn from its own pixels: Skia
 * copies a changeable bitmap into a new image every time it is drawn, which a texture drawn in
 * strips every frame can't afford. Nothing may paint into it afterwards.
 */
internal expect fun ImageBitmap.freeze()
