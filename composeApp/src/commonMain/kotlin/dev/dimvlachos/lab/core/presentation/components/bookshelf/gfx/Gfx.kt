package dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Matrix as ComposeMatrix
import androidx.compose.ui.graphics.Paint as ComposePaint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shader as ComposeShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.components.perspective.Homography

/*
 * The slice of android.graphics the shelf's painters are written in, over Compose's own Canvas, so
 * they are drawn here as they were written: same calls, same order, same numbers. Only what the
 * painters use is here, and each behaves as its Android namesake does.
 */

internal typealias Bitmap = ImageBitmap

internal fun createBitmap(width: Int, height: Int): Bitmap = ImageBitmap(width, height)

/** Bitmaps here are let go by the collector; `recycle` has nothing to do. */
internal fun Bitmap.recycle() = Unit

/** Reads pixels as `Bitmap.getPixels` does: ARGB, [stride] to a row, starting at [offset]. */
internal fun Bitmap.getPixels(
    pixels: IntArray,
    offset: Int,
    stride: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) = readPixels(pixels, x, y, width, height, offset, stride)

internal class RectF(
    var left: Float = 0f,
    var top: Float = 0f,
    var right: Float = 0f,
    var bottom: Float = 0f,
) {
    constructor(
        r: Rect
    ) : this(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())

    fun width() = right - left

    fun height() = bottom - top

    fun centerX() = (left + right) / 2f

    fun centerY() = (top + bottom) / 2f

    fun set(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    internal fun compose() = ComposeRect(left, top, right, bottom)
}

internal class Rect(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0) {
    fun width() = right - left

    fun height() = bottom - top

    val isEmpty: Boolean
        get() = left >= right || top >= bottom
}

internal enum class BlendMode(internal val compose: ComposeBlendMode) {
    MULTIPLY(ComposeBlendMode.Multiply),
    OVERLAY(ComposeBlendMode.Overlay),
    PLUS(ComposeBlendMode.Plus),
    SRC_OVER(ComposeBlendMode.SrcOver),
    SRC_IN(ComposeBlendMode.SrcIn),
    DST_IN(ComposeBlendMode.DstIn),
    DST_OUT(ComposeBlendMode.DstOut),
    SCREEN(ComposeBlendMode.Screen),
    CLEAR(ComposeBlendMode.Clear),
}

/** A typeface is a family at a weight and style; text is measured with [measurer]. */
internal class Typeface(
    val family: FontFamily,
    val measurer: TextMeasurer,
    val weight: FontWeight = FontWeight.Normal,
    val style: FontStyle = FontStyle.Normal,
)

internal abstract class Shader {
    /** A matrix applied to the shader's space, as `Shader.setLocalMatrix`; only scales are used. */
    internal var localMatrix: Matrix? = null

    fun setLocalMatrix(matrix: Matrix?) {
        localMatrix = matrix
    }

    /** The Compose shader, its space moved by ([dx], [dy]) for text drawn translated. */
    internal abstract fun compose(dx: Float = 0f, dy: Float = 0f, scale: Float = 1f): ComposeShader

    enum class TileMode(internal val compose: androidx.compose.ui.graphics.TileMode) {
        CLAMP(androidx.compose.ui.graphics.TileMode.Clamp),
        REPEAT(androidx.compose.ui.graphics.TileMode.Repeated),
        MIRROR(androidx.compose.ui.graphics.TileMode.Mirror),
        DECAL(androidx.compose.ui.graphics.TileMode.Decal),
    }
}

internal class LinearGradient(
    private val x0: Float,
    private val y0: Float,
    private val x1: Float,
    private val y1: Float,
    private val colors: IntArray,
    private val positions: FloatArray?,
    private val tile: TileMode,
) : Shader() {
    constructor(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        color0: Int,
        color1: Int,
        tile: TileMode,
    ) : this(x0, y0, x1, y1, intArrayOf(color0, color1), null, tile)

    private var cached: ComposeShader? = null

    override fun compose(dx: Float, dy: Float, scale: Float): ComposeShader {
        val plain = dx == 0f && dy == 0f && scale == 1f
        if (plain)
            cached?.let {
                return it
            }
        return LinearGradientShader(
                Offset((x0 - dx) * scale, (y0 - dy) * scale),
                Offset((x1 - dx) * scale, (y1 - dy) * scale),
                colors.map { Color(it) },
                positions?.toList(),
                tile.compose,
            )
            .also { if (plain) cached = it }
    }
}

internal class BitmapShader(
    private val bitmap: Bitmap,
    private val tileX: TileMode,
    private val tileY: TileMode,
) : Shader() {
    private val shader by lazy { ImageShader(bitmap, tileX.compose, tileY.compose) }

    override fun compose(dx: Float, dy: Float, scale: Float): ComposeShader = shader
}

/** `BlurMaskFilter`: blurs what is drawn by [radius], as Skia turns a radius into a sigma. */
internal class BlurMaskFilter(val radius: Float, @Suppress("unused") val blur: Blur) {
    enum class Blur {
        NORMAL
    }
}

/** `LightingColorFilter`: each channel times [mul]'s, plus [add]'s. */
internal class LightingColorFilter(private val mul: Int, private val add: Int) {
    internal val compose: ColorFilter by lazy {
        fun m(shift: Int) = ((mul shr shift) and 0xFF) / 255f
        fun a(shift: Int) = ((add shr shift) and 0xFF).toFloat()
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    m(16),
                    0f,
                    0f,
                    0f,
                    a(16),
                    0f,
                    m(8),
                    0f,
                    0f,
                    a(8),
                    0f,
                    0f,
                    m(0),
                    0f,
                    a(0),
                    0f,
                    0f,
                    0f,
                    1f,
                    0f,
                )
            )
        )
    }
}

internal class FontMetrics(val ascent: Float, val descent: Float, val top: Float, val bottom: Float)

internal class Paint(flags: Int = 0) {
    constructor(other: Paint) : this() {
        set(other)
    }

    enum class Style {
        FILL,
        STROKE,
        FILL_AND_STROKE,
    }

    enum class Cap {
        BUTT,
        ROUND,
        SQUARE,
    }

    enum class Align {
        LEFT,
        CENTER,
        RIGHT,
    }

    var color: Int = 0xFF000000.toInt()
    var alpha: Int
        get() = color ushr 24
        set(value) {
            color = (color and 0x00FFFFFF) or (value.coerceIn(0, 255) shl 24)
        }

    var style = Style.FILL
    var strokeWidth = 0f
    var strokeCap = Cap.BUTT
    var shader: Shader? = null
    var maskFilter: BlurMaskFilter? = null
    var colorFilter: LightingColorFilter? = null
    var blendMode: BlendMode? = null
    var isAntiAlias = flags and ANTI_ALIAS_FLAG != 0
    var isFilterBitmap = flags and FILTER_BITMAP_FLAG != 0
    var isFakeBoldText = false
    var textSize = 12f
    var typeface: Typeface? = null
    var letterSpacing = 0f
    var textAlign = Align.LEFT

    fun set(other: Paint) {
        color = other.color
        style = other.style
        strokeWidth = other.strokeWidth
        strokeCap = other.strokeCap
        shader = other.shader
        maskFilter = other.maskFilter
        colorFilter = other.colorFilter
        blendMode = other.blendMode
        isAntiAlias = other.isAntiAlias
        isFilterBitmap = other.isFilterBitmap
        isFakeBoldText = other.isFakeBoldText
        textSize = other.textSize
        typeface = other.typeface
        letterSpacing = other.letterSpacing
        textAlign = other.textAlign
    }

    private val native = ComposePaint()
    private var nativeBlur = 0f

    /** This paint as Compose draws with it, its shader moved by ([dx], [dy]). */
    internal fun compose(dx: Float = 0f, dy: Float = 0f): ComposePaint {
        native.color = Color(color)
        native.style = if (style == Style.STROKE) PaintingStyle.Stroke else PaintingStyle.Fill
        native.strokeWidth = strokeWidth
        native.strokeCap =
            when (strokeCap) {
                Cap.BUTT -> StrokeCap.Butt
                Cap.ROUND -> StrokeCap.Round
                Cap.SQUARE -> StrokeCap.Square
            }
        native.shader = shader?.compose(dx, dy)
        native.colorFilter = colorFilter?.compose
        native.blendMode = blendMode?.compose ?: ComposeBlendMode.SrcOver
        native.isAntiAlias = isAntiAlias
        native.filterQuality = if (isFilterBitmap) FilterQuality.Medium else FilterQuality.None
        val sigma = maskFilter?.let { 0.57735f * it.radius + 0.5f } ?: 0f
        if (sigma != nativeBlur) {
            native.setBlur(sigma)
            nativeBlur = sigma
        }
        return native
    }

    private fun textStyle(): TextStyle {
        val face = requireNotNull(typeface) { "text needs a typeface" }
        return TextStyle(
            fontFamily = face.family,
            fontWeight = face.weight,
            fontStyle = face.style,
            fontSize = (textSize * TEXT_OVERSAMPLE).sp,
            letterSpacing = letterSpacing.em,
        )
    }

    internal fun layout(text: String): TextLayoutResult =
        requireNotNull(typeface)
            .measurer
            .measure(text, textStyle(), softWrap = false, maxLines = 1, density = PxDensity)

    fun measureText(text: String): Float =
        if (text.isEmpty()) 0f else layout(text).multiParagraph.getLineWidth(0) / TEXT_OVERSAMPLE

    fun ascent(): Float = fontMetrics.ascent

    fun descent(): Float = fontMetrics.descent

    val fontMetrics: FontMetrics
        get() {
            val line = layout("Hg")
            val ascent = -line.firstBaseline / TEXT_OVERSAMPLE
            val descent = (line.size.height - line.firstBaseline) / TEXT_OVERSAMPLE
            return FontMetrics(ascent, descent, ascent, descent)
        }

    companion object {
        const val ANTI_ALIAS_FLAG = 1
        const val FILTER_BITMAP_FLAG = 2
        const val LINEAR_TEXT_FLAG = 64
        const val SUBPIXEL_TEXT_FLAG = 128
    }
}

internal typealias TextPaint = Paint

// Text is measured at one px to the sp, so a Paint's textSize is in the canvas's own units.
private val PxDensity = Density(1f, 1f)

// Text is laid out this many times larger and drawn scaled back down: the painters set text a
// few units tall on a canvas scaled up after, and at that size glyph positions round to whole
// units, swallowing spaces.
private const val TEXT_OVERSAMPLE = 8f

internal class Path {
    internal val compose = ComposePath()

    enum class Direction {
        CW,
        CCW,
    }

    enum class Op(internal val compose: PathOperation) {
        DIFFERENCE(PathOperation.Difference),
        INTERSECT(PathOperation.Intersect),
        UNION(PathOperation.Union),
        XOR(PathOperation.Xor),
    }

    fun moveTo(x: Float, y: Float) = compose.moveTo(x, y)

    fun lineTo(x: Float, y: Float) = compose.lineTo(x, y)

    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) = compose.quadraticTo(x1, y1, x2, y2)

    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        compose.cubicTo(x1, y1, x2, y2, x3, y3)

    fun close() = compose.close()

    fun reset() = compose.reset()

    fun rewind() = compose.rewind()

    fun addRect(left: Float, top: Float, right: Float, bottom: Float, dir: Direction) =
        compose.addRect(ComposeRect(left, top, right, bottom))

    fun addRect(rect: RectF, dir: Direction) = compose.addRect(rect.compose())

    fun addOval(left: Float, top: Float, right: Float, bottom: Float, dir: Direction) =
        compose.addOval(ComposeRect(left, top, right, bottom))

    fun addCircle(x: Float, y: Float, radius: Float, dir: Direction) =
        compose.addOval(ComposeRect(x - radius, y - radius, x + radius, y + radius))

    fun arcTo(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean,
    ) = compose.arcTo(ComposeRect(left, top, right, bottom), startAngle, sweepAngle, forceMoveTo)

    fun op(path: Path, op: Op): Boolean {
        val result = ComposePath.combine(op.compose, compose, path.compose)
        compose.reset()
        compose.addPath(result)
        // Its holes are only holes under the fill rule the operation chose.
        compose.fillType = result.fillType
        return true
    }

    fun offset(dx: Float, dy: Float) = compose.translate(Offset(dx, dy))

    fun addPath(src: Path) = compose.addPath(src.compose)

    val isEmpty: Boolean
        get() = compose.isEmpty

    fun set(src: Path) {
        compose.reset()
        compose.addPath(src.compose)
        compose.fillType = src.compose.fillType
    }
}

internal class Matrix {
    internal val compose = ComposeMatrix()
    private val h = FloatArray(9)

    fun reset() = compose.reset()

    fun setScale(sx: Float, sy: Float) {
        compose.reset()
        compose.scale(sx, sy)
    }

    internal val scaleX: Float
        get() = compose[0, 0]

    internal val scaleY: Float
        get() = compose[1, 1]

    /**
     * The projection that takes the [count] = 4 points in [src] to those in [dst], as
     * `Matrix.setPolyToPoly`. False when the source points are degenerate.
     */
    fun setPolyToPoly(
        src: FloatArray,
        srcIndex: Int,
        dst: FloatArray,
        dstIndex: Int,
        count: Int,
    ): Boolean {
        require(count == 4)
        val from = squareToQuad(src, srcIndex)
        val to = squareToQuad(dst, dstIndex)
        val inverse = invert3(from) ?: return false
        // to · from⁻¹ takes the source quad to the unit square, then the square to the target.
        multiply3(to, inverse, h)
        Homography.toMatrix(h, compose)
        return true
    }
}

// The projection that takes the unit square to the quad at [p] (x0 y0 … x3 y3, corners in order).
private fun squareToQuad(p: FloatArray, at: Int): FloatArray {
    val x0 = p[at]
    val y0 = p[at + 1]
    val x1 = p[at + 2]
    val y1 = p[at + 3]
    val x2 = p[at + 4]
    val y2 = p[at + 5]
    val x3 = p[at + 6]
    val y3 = p[at + 7]
    val sx = x0 - x1 + x2 - x3
    val sy = y0 - y1 + y2 - y3
    if (sx == 0f && sy == 0f) {
        return floatArrayOf(x1 - x0, x3 - x0, x0, y1 - y0, y3 - y0, y0, 0f, 0f, 1f)
    }
    val dx1 = x1 - x2
    val dx2 = x3 - x2
    val dy1 = y1 - y2
    val dy2 = y3 - y2
    val den = dx1 * dy2 - dx2 * dy1
    val g = (sx * dy2 - dx2 * sy) / den
    val hh = (dx1 * sy - sx * dy1) / den
    return floatArrayOf(
        x1 - x0 + g * x1,
        x3 - x0 + hh * x3,
        x0,
        y1 - y0 + g * y1,
        y3 - y0 + hh * y3,
        y0,
        g,
        hh,
        1f,
    )
}

private fun invert3(m: FloatArray): FloatArray? {
    val a0 = m[4] * m[8] - m[5] * m[7]
    val a1 = m[2] * m[7] - m[1] * m[8]
    val a2 = m[1] * m[5] - m[2] * m[4]
    val a3 = m[5] * m[6] - m[3] * m[8]
    val a4 = m[0] * m[8] - m[2] * m[6]
    val a5 = m[2] * m[3] - m[0] * m[5]
    val a6 = m[3] * m[7] - m[4] * m[6]
    val a7 = m[1] * m[6] - m[0] * m[7]
    val a8 = m[0] * m[4] - m[1] * m[3]
    val det = m[0] * a0 + m[1] * a3 + m[2] * a6
    if (det == 0f) return null
    return floatArrayOf(a0, a1, a2, a3, a4, a5, a6, a7, a8).also {
        for (i in it.indices) it[i] /= det
    }
}

private fun multiply3(a: FloatArray, b: FloatArray, out: FloatArray) {
    for (r in 0 until 3) for (c in 0 until 3) {
        out[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c]
    }
}

internal class Canvas(internal val compose: ComposeCanvas) {
    constructor(bitmap: Bitmap) : this(ComposeCanvas(bitmap))

    fun save(): Int {
        compose.save()
        return ++depth
    }

    fun restore() {
        compose.restore()
        depth--
    }

    fun restoreToCount(count: Int) {
        while (depth >= count) restore()
    }

    private var depth = 0

    fun saveLayerAlpha(bounds: RectF?, alpha: Int): Int {
        compose.saveLayer(
            bounds?.compose() ?: Everywhere,
            ComposePaint().apply { this.alpha = alpha / 255f },
        )
        return ++depth
    }

    fun saveLayer(bounds: RectF?, paint: Paint?): Int {
        compose.saveLayer(bounds?.compose() ?: Everywhere, paint?.compose() ?: ComposePaint())
        return ++depth
    }

    fun translate(dx: Float, dy: Float) = compose.translate(dx, dy)

    fun scale(sx: Float, sy: Float) = compose.scale(sx, sy)

    fun scale(sx: Float, sy: Float, px: Float, py: Float) {
        compose.translate(px, py)
        compose.scale(sx, sy)
        compose.translate(-px, -py)
    }

    fun rotate(degrees: Float) = compose.rotate(degrees)

    fun rotate(degrees: Float, px: Float, py: Float) {
        compose.translate(px, py)
        compose.rotate(degrees)
        compose.translate(-px, -py)
    }

    fun concat(matrix: Matrix) = compose.concat(matrix.compose)

    fun clipRect(left: Float, top: Float, right: Float, bottom: Float) =
        compose.clipRect(left, top, right, bottom)

    fun clipRect(rect: RectF) = compose.clipRect(rect.left, rect.top, rect.right, rect.bottom)

    fun clipPath(path: Path) = compose.clipPath(path.compose)

    fun clipOutRect(left: Float, top: Float, right: Float, bottom: Float) =
        compose.clipRect(left, top, right, bottom, ClipOp.Difference)

    fun drawColor(color: Int) = drawColor(color, BlendMode.SRC_OVER)

    fun drawColor(color: Int, mode: BlendMode) =
        compose.drawRect(
            Everywhere,
            ComposePaint().apply {
                this.color = Color(color)
                blendMode = mode.compose
            },
        )

    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) =
        inShaderSpace(paint) { s ->
            compose.drawRect(left / s.x, top / s.y, right / s.x, bottom / s.y, paint.compose())
        }

    fun drawRect(rect: RectF, paint: Paint) =
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)

    fun drawPath(path: Path, paint: Paint) =
        inShaderSpace(paint) { s ->
            if (s.x == 1f && s.y == 1f) compose.drawPath(path.compose, paint.compose())
            else {
                val scaled = ComposePath().apply { addPath(path.compose) }
                scaled.transform(ComposeMatrix().apply { scale(1f / s.x, 1f / s.y) })
                compose.drawPath(scaled, paint.compose())
            }
        }

    fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) =
        compose.drawCircle(Offset(cx, cy), radius, paint.compose())

    fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) =
        compose.drawOval(left, top, right, bottom, paint.compose())

    fun drawOval(rect: RectF, paint: Paint) =
        drawOval(rect.left, rect.top, rect.right, rect.bottom, paint)

    fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float, paint: Paint) =
        compose.drawLine(Offset(x0, y0), Offset(x1, y1), paint.compose())

    fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) =
        compose.drawImage(bitmap, Offset(left, top), paint?.compose() ?: Plain)

    fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
        val from = src ?: Rect(0, 0, bitmap.width, bitmap.height)
        // Compose draws into whole pixels; the canvas's own transform carries the fraction.
        save()
        translate(dst.left, dst.top)
        scale(dst.width() / from.width(), dst.height() / from.height())
        compose.drawImageRect(
            bitmap,
            IntOffset(from.left, from.top),
            IntSize(from.width(), from.height()),
            IntOffset.Zero,
            IntSize(from.width(), from.height()),
            paint?.compose() ?: Plain,
        )
        restore()
    }

    fun drawBitmap(bitmap: Bitmap, matrix: Matrix, paint: Paint?) {
        save()
        concat(matrix)
        compose.drawImage(bitmap, Offset.Zero, paint?.compose() ?: Plain)
        restore()
    }

    /** Text with its baseline at [y], placed at [x] by the paint's alignment. */
    fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        if (text.isEmpty()) return
        val line = paint.layout(text)
        val width = line.multiParagraph.getLineWidth(0) / TEXT_OVERSAMPLE
        val left =
            when (paint.textAlign) {
                Paint.Align.LEFT -> x
                Paint.Align.CENTER -> x - width / 2f
                Paint.Align.RIGHT -> x - width
            }
        val top = y - line.firstBaseline / TEXT_OVERSAMPLE
        compose.save()
        compose.translate(left, top)
        compose.scale(1f / TEXT_OVERSAMPLE, 1f / TEXT_OVERSAMPLE)
        val shader = paint.shader
        val color = Color(paint.color)
        if (shader != null)
            line.multiParagraph.paint(
                compose,
                ShaderBrush(shader.compose(left, top, TEXT_OVERSAMPLE)),
                alpha = color.alpha,
                blendMode = paint.blendMode?.compose ?: ComposeBlendMode.SrcOver,
            )
        else
            line.multiParagraph.paint(
                compose,
                color,
                blendMode = paint.blendMode?.compose ?: ComposeBlendMode.SrcOver,
            )
        if (paint.isFakeBoldText) {
            // Fake bold strokes each glyph's outline a little, as Skia does.
            val stroke =
                androidx.compose.ui.graphics.drawscope.Stroke(
                    fakeBoldStroke(paint.textSize) * TEXT_OVERSAMPLE
                )
            if (shader != null)
                line.multiParagraph.paint(
                    compose,
                    ShaderBrush(shader.compose(left, top, TEXT_OVERSAMPLE)),
                    alpha = color.alpha,
                    drawStyle = stroke,
                )
            else line.multiParagraph.paint(compose, color, drawStyle = stroke)
        }
        compose.restore()
    }

    // A shader with a local scale draws as though the canvas were scaled by it: the geometry is
    // drawn in the shader's space, so its pixels are Android's.
    private inline fun inShaderSpace(paint: Paint, draw: (Offset) -> Unit) {
        val local = paint.shader?.localMatrix
        if (local == null) return draw(Offset(1f, 1f))
        val s = Offset(local.scaleX, local.scaleY)
        compose.save()
        compose.scale(s.x, s.y)
        if (paint.style == Paint.Style.STROKE) {
            // A stroke keeps its width on the page.
            val width = paint.strokeWidth
            paint.strokeWidth = width / s.x
            draw(s)
            paint.strokeWidth = width
        } else draw(s)
        compose.restore()
    }

    private companion object {
        val Everywhere = ComposeRect(-1e6f, -1e6f, 1e6f, 1e6f)
        val Plain = ComposePaint().apply { filterQuality = FilterQuality.Medium }
    }
}

internal inline fun Canvas.withSave(block: Canvas.() -> Unit) {
    val count = save()
    try {
        block()
    } finally {
        restoreToCount(count)
    }
}

internal inline fun Canvas.withScale(
    x: Float = 1f,
    y: Float = 1f,
    pivotX: Float = 0f,
    pivotY: Float = 0f,
    block: Canvas.() -> Unit,
) = withSave {
    scale(x, y, pivotX, pivotY)
    block()
}

internal inline fun Canvas.withTranslation(x: Float = 0f, y: Float = 0f, block: Canvas.() -> Unit) =
    withSave {
        translate(x, y)
        block()
    }

internal inline fun Canvas.withRotation(
    degrees: Float = 0f,
    pivotX: Float = 0f,
    pivotY: Float = 0f,
    block: Canvas.() -> Unit,
) = withSave {
    rotate(degrees, pivotX, pivotY)
    block()
}

internal inline fun Canvas.withClip(path: Path, block: Canvas.() -> Unit) = withSave {
    clipPath(path)
    block()
}

internal inline fun Canvas.withClip(rect: RectF, block: Canvas.() -> Unit) = withSave {
    clipRect(rect)
    block()
}

internal inline fun Canvas.withClip(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: Canvas.() -> Unit,
) = withSave {
    clipRect(left, top, right, bottom)
    block()
}

/** `Math.toDegrees`, which common Kotlin lacks. */
internal fun toDegrees(radians: Double): Double = radians * 180.0 / kotlin.math.PI

/**
 * `TextUtils.ellipsize(text, paint, avail, TruncateAt.END)`: [text] whole if it fits in [avail],
 * else as much of it as fits with "…" after it.
 */
internal fun ellipsize(text: String, paint: Paint, avail: Float): String {
    if (paint.measureText(text) <= avail) return text
    var low = 0
    var high = text.length
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (paint.measureText(text.substring(0, mid).trimEnd() + "…") <= avail) low = mid
        else high = mid - 1
    }
    return if (low == 0) "" else text.substring(0, low).trimEnd() + "…"
}

/**
 * How wide fake bold strokes a glyph's outline at [textSize], as Skia emboldens: a twenty-fourth of
 * the size up to 9, a thirty-second from 36, and in between in proportion.
 */
internal fun fakeBoldStroke(textSize: Float): Float {
    val t = ((textSize - 9f) / 27f).coerceIn(0f, 1f)
    return textSize * (1f / 24f + (1f / 32f - 1f / 24f) * t)
}
