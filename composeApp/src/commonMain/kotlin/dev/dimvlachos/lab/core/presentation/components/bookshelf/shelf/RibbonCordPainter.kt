package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Vec3
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.smoothStep
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.BlurMaskFilter
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.LinearGradient
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Paint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Path
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.RectF
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Shader
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withClip
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withScale
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.BookLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.TurnFrame
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val CORD_WIDTH = 0.026f
private const val CORD_DEPTH = 0.04f
private const val CORD_NOTCH = 0.8f
private const val FOOT_SHADES = 5
private const val SHADOW_ALPHA = 0.28f
private const val SHADOW_X = 1.2f
private const val SHADOW_Y = 1.4f
private const val SHADOW_LIFT_REACH = 0.25f
private const val SHADOW_SPREAD = 0.15f
private const val SHADOW_WIDEN = 0.03f
private const val SHADOW_LEVELS = 16
private const val SHADOW_SOFT = 2f
private const val SHADOW_WIDTH = 0.028f
private const val SHADOW_BLUR = 0.08f
private const val SHADOW_INK = 0x28140A
private const val FABRIC_BEFORE = 4f
private const val OPEN_BOUND = 100_000f
private const val FABRIC_AFTER = 10f

internal fun ribbonFrame(
    layout: BookLayout,
    density: Float,
    scale: Float,
    tail: Float,
    pageUnits: Float,
) =
    RibbonFrame(
        width = layout.geometry.page / density,
        height = layout.geometry.height / density,
        tail = tail * scale,
        depth = CORD_DEPTH * layout.geometry.page / density,
        unit = layout.geometry.page / density / pageUnits.coerceAtLeast(1f),
    )

internal fun DrawScope.drawRibbonCord(
    layout: BookLayout,
    left: Boolean,
    cord: RibbonCord,
    frame: RibbonFrame,
    lighting: ShelfLighting,
    density: Float,
    shadowAlpha: Float,
    surface: TurnFrame,
) {
    val geometry = layout.geometry
    val side = if (left) -1f else 1f
    fun seen(p: Vec3): Offset {
        val (bx, by, bz) = surface.bookPoint(p.x * density + surface.textureShift, p.y * density)
        val w = 1f + (bz + p.z * density) / geometry.perspectivePx
        return Offset(
            (layout.left + geometry.spineX + bx / w) / density,
            (layout.top + geometry.originY + (by - geometry.originY) / w) / density,
        )
    }
    val runs = cordRuns(cord)
    if (runs.isEmpty()) return
    val width = CORD_WIDTH * frame.width
    val spine = (layout.left + geometry.spineX) / density
    val top = layout.top / density
    val page =
        RectF(
            if (left) spine - frame.width else spine,
            top,
            if (left) spine else spine + frame.width,
            top + frame.height,
        )
    val origin = RIBBON_TOP_X * frame.width
    val fabric =
        horizontalGradient(
            spine + side * (origin - FABRIC_BEFORE * frame.unit),
            spine + side * (origin + FABRIC_AFTER * frame.unit),
            0f to shade(RIBBON_RED, 0.7f),
            0.45f to shade(RIBBON_RED, 1.25f),
            1f to shade(RIBBON_RED, 0.8f),
        )
    val footShades = pageRibbonFootShades(frame.width / frame.unit, FOOT_SHADES)
    val hanging =
        LinearGradient(
            spine + side * (PAGE_RIBBON_FOOT_X - PAGE_RIBBON_WIDTH / 2f) * frame.width,
            0f,
            spine + side * (PAGE_RIBBON_FOOT_X + PAGE_RIBBON_WIDTH / 2f) * frame.width,
            0f,
            IntArray(FOOT_SHADES) { shade(RIBBON_RED, footShades[it]) },
            null,
            Shader.TileMode.CLAMP,
        )
    val foot = seen(Vec3(PAGE_RIBBON_FOOT_X * frame.width, frame.height, 0f)).y
    val hangingWidth = PAGE_RIBBON_WIDTH * frame.width
    val onPage = RectF(-OPEN_BOUND, -OPEN_BOUND, OPEN_BOUND, foot)
    val belowPage = RectF(-OPEN_BOUND, foot, OPEN_BOUND, OPEN_BOUND)
    val pageLight = lighting.faceLight(Vec3(0f, 0f, 1f), 1f).toArgb()
    drawIntoCanvas { canvas ->
        Canvas(canvas).withScale(density, density) {
            val appear =
                if (cord.appear < 1f) saveLayerAlpha(null, (cord.appear * 255f).roundToInt())
                else null
            if (shadowAlpha > 0f) {
                val layer = saveLayerAlpha(null, (shadowAlpha * 255f).roundToInt())
                withClip(page) {
                    for (run in runs) drawCordShadow(run, cord, frame, width) { seen(it) }
                }
                restoreToCount(layer)
            }
            for (run in runs) {
                val points = run.map { seen(cord[it]) }
                val notched = run.last() == cord.size - 1
                val path = cordPath(points, width, notched)
                withClip(onPage) {
                    drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = fabric })
                    drawPath(path, multiplyPaint(pageLight))
                }
                withClip(belowPage) {
                    val tail = cordPath(points, hangingWidth, notched)
                    drawPath(tail, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = hanging })
                    drawPath(tail, multiplyPaint(pageLight))
                }
            }
            appear?.let(::restoreToCount)
        }
    }
}

private fun Canvas.drawCordShadow(
    run: List<Int>,
    cord: RibbonCord,
    frame: RibbonFrame,
    width: Float,
    seen: (Vec3) -> Offset,
) {
    val lifts = run.map(cord::shadowLift)
    val points = run.mapIndexed { k, i ->
        val p = cord[i]
        val reach = 1f + lifts[k] * SHADOW_SPREAD
        seen(Vec3(p.x, p.y, 0f)) + Offset(SHADOW_X, SHADOW_Y) * (frame.unit * reach)
    }
    val halves = lifts.map { SHADOW_WIDTH * frame.width / 2f * (1f + it * SHADOW_WIDEN) }
    val alphas = lifts.map {
        SHADOW_ALPHA * (1f - smoothStep(0f, SHADOW_LIFT_REACH * frame.width, it))
    }
    val normals = points.indices.map { normal(points, it) }
    val level = { k: Int ->
        ((alphas[k] + alphas[k + 1]) / 2f / SHADOW_ALPHA * SHADOW_LEVELS).roundToInt()
    }
    var from = 0
    while (from < points.lastIndex) {
        val band = level(from)
        var to = from + 1
        while (to < points.lastIndex && level(to) == band) to++
        if (band > 0) {
            val lift = lifts.subList(from, to + 1).average().toFloat()
            val path =
                Path().apply {
                    (points[from] + normals[from] * halves[from]).let { moveTo(it.x, it.y) }
                    for (i in from + 1..to) (points[i] + normals[i] * halves[i]).let {
                        lineTo(it.x, it.y)
                    }
                    for (i in to downTo from) (points[i] - normals[i] * halves[i]).let {
                        lineTo(it.x, it.y)
                    }
                    close()
                }
            val alpha = SHADOW_ALPHA * band / SHADOW_LEVELS
            drawPath(
                path,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = SHADOW_INK or ((alpha * 255f).roundToInt() shl 24)
                    maskFilter =
                        BlurMaskFilter(
                            SHADOW_SOFT * frame.unit + lift * SHADOW_BLUR,
                            BlurMaskFilter.Blur.NORMAL,
                        )
                },
            )
        }
        from = to
    }
}

private fun cordRuns(cord: RibbonCord): List<List<Int>> {
    val runs = mutableListOf<List<Int>>()
    var current = mutableListOf<Int>()
    for (i in 0 until cord.size) {
        if (!cord.isBehind(i)) {
            current += i
        } else if (current.isNotEmpty()) {
            runs += current
            current = mutableListOf()
        }
    }
    runs += current
    return runs.filter { run -> run.size > 1 && run.any { cord[it].y > 0f } }
}

private fun normal(points: List<Offset>, i: Int): Offset {
    val last = points.lastIndex
    val d = points[(i + 1).coerceAtMost(last)] - points[(i - 1).coerceAtLeast(0)]
    val length = sqrt(d.x * d.x + d.y * d.y)
    return if (length < MIN_LENGTH) Offset(1f, 0f) else Offset(-d.y, d.x) / length
}

private fun cordPath(points: List<Offset>, width: Float, notched: Boolean): Path {
    val last = points.lastIndex
    fun tangent(i: Int): Offset {
        val d = points[(i + 1).coerceAtMost(last)] - points[(i - 1).coerceAtLeast(0)]
        val length = sqrt(d.x * d.x + d.y * d.y)
        return if (length < MIN_LENGTH) Offset(0f, 1f) else d / length
    }
    fun side(i: Int): Offset = tangent(i).let { Offset(-it.y, it.x) * (width / 2f) }
    return Path().apply {
        (points[0] + side(0)).let { moveTo(it.x, it.y) }
        for (i in 1..last) (points[i] + side(i)).let { lineTo(it.x, it.y) }
        if (notched) {
            val notch = points[last] - tangent(last) * (width * CORD_NOTCH)
            lineTo(notch.x, notch.y)
        }
        for (i in last downTo 0) (points[i] - side(i)).let { lineTo(it.x, it.y) }
        close()
    }
}

private const val MIN_LENGTH = 1e-3f
