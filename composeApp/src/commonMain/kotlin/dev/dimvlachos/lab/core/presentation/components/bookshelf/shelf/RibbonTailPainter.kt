package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Point2
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Vec3
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.LinearGradient
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Paint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Path
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Shader
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withScale
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.BookLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.TurnFrame
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.stripShadeAlphas
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal const val RIBBON_TAIL_LENGTH = 26f
internal const val RIBBON_TAIL_NOTCH = 0.8f
private const val TAIL_OVERLAP = 1f
private const val EDGE_ON_SHADE = 0.45f
private const val FOOT_SHADES = 5

internal class TailMotion(
    val density: Float,
    val scale: Float,
    val timeMillis: Long,
    val rope: RibbonRope,
    val pageWidth: Float,
)

internal fun DrawScope.drawLeafRibbonTail(
    layout: BookLayout,
    frame: TurnFrame,
    lighting: ShelfLighting,
    motion: TailMotion,
) {
    fun edge(depth: Float): Vec3 {
        val (x, y, z) = frame.bookPoint(depth * frame.pageWidth + frame.textureShift, frame.height)
        return Vec3(x, y, z)
    }
    val wedge =
        frame.wedgeAt(PAGE_RIBBON_FOOT_X * frame.pageWidth + frame.textureShift, frame.height)
    val shade = stripShadeAlphas(frame, wedge).let { (from, to) -> (from + to) / 2f }
    drawProjectedTail(
        layout,
        edge(PAGE_RIBBON_FOOT_X - PAGE_RIBBON_WIDTH / 2f),
        edge(PAGE_RIBBON_FOOT_X + PAGE_RIBBON_WIDTH / 2f),
        lighting,
        motion,
        shade,
        RIBBON_TAIL_LENGTH,
    )
}

@Suppress("LongParameterList")
private fun DrawScope.drawProjectedTail(
    layout: BookLayout,
    near: Vec3,
    far: Vec3,
    lighting: ShelfLighting,
    motion: TailMotion,
    shade: Float,
    length: Float,
) {
    val geometry = layout.geometry
    val density = motion.density
    fun seen(p: Vec3): Point2 {
        val w = 1f + p.z * density / geometry.perspectivePx
        return Point2(
            (layout.left + geometry.spineX + p.x * density / w) / density,
            (layout.top + geometry.originY + (p.y * density - geometry.originY) / w) / density,
        )
    }
    val nearDp = near * (1f / density)
    val farDp = far * (1f / density)
    val rope = motion.rope
    rope.follow(
        (nearDp + farDp) * 0.5f,
        length * motion.scale,
        motion.timeMillis,
        RopeSpace.Book,
    )
    val flatWidth = PAGE_RIBBON_WIDTH * geometry.page / density
    drawIntoCanvas { canvas ->
        Canvas(canvas).withScale(density, density) {
            drawRibbonStrip(
                seen(nearDp) to seen(farDp),
                List(rope.size) { seen(rope[it]) },
                1f,
                lighting,
                flatWidth,
                shade,
                pageRibbonFootShades(motion.pageWidth, FOOT_SHADES),
                onPage = true,
            )
        }
    }
}

@Suppress("LongParameterList")
internal fun Canvas.drawRibbonTail(
    anchors: Pair<Point2, Point2>,
    alpha: Float,
    lighting: ShelfLighting,
    motion: TailMotion,
) {
    val (a, b) = anchors
    val rope = motion.rope
    rope.follow(
        Vec3((a.x + b.x) / 2f, (a.y + b.y) / 2f, 0f),
        RIBBON_TAIL_LENGTH * motion.scale,
        motion.timeMillis,
        RopeSpace.Screen,
    )
    drawRibbonStrip(
        anchors,
        List(rope.size) { Point2(rope[it].x, rope[it].y) },
        alpha,
        lighting,
        shades = pageRibbonFootShades(motion.pageWidth, FOOT_SHADES),
    )
}

@Suppress("LongParameterList")
private fun Canvas.drawRibbonStrip(
    anchors: Pair<Point2, Point2>,
    seen: List<Point2>,
    alpha: Float,
    lighting: ShelfLighting,
    flatWidth: Float = 0f,
    shade: Float = 0f,
    shades: FloatArray,
    onPage: Boolean = false,
) {
    val (a, b) = anchors
    val rope = seen.map { Offset(it.x, it.y) }
    val across = Offset(b.x - a.x, b.y - a.y)
    val width = across.getDistance()
    val reference = if (flatWidth > 0f) flatWidth else width
    val facing = (width / reference.coerceAtLeast(MIN_LENGTH)).coerceIn(0f, 1f)
    val light = (EDGE_ON_SHADE + (1f - EDGE_ON_SHADE) * facing) * (1f - shade)
    val last = rope.lastIndex
    fun tangent(i: Int): Offset =
        (rope[(i + 1).coerceAtMost(last)] - rope[(i - 1).coerceAtLeast(0)]).unit()
    fun side(i: Int): Offset {
        val t = tangent(i)
        val perp = Offset(-t.y, t.x)
        val oriented = if (perp.x * across.x + perp.y * across.y < 0f) -perp else perp
        return oriented * (width / 2f)
    }
    val lift = tangent(0) * TAIL_OVERLAP
    val notch = rope[last] - tangent(last) * (width * RIBBON_TAIL_NOTCH)
    val path =
        Path().apply {
            moveTo(a.x - lift.x, a.y - lift.y)
            lineTo(b.x - lift.x, b.y - lift.y)
            for (i in 1..last) (rope[i] + side(i)).let { lineTo(it.x, it.y) }
            lineTo(notch.x, notch.y)
            for (i in last downTo 1) (rope[i] - side(i)).let { lineTo(it.x, it.y) }
            close()
        }
    val fill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
                LinearGradient(
                    a.x,
                    a.y,
                    b.x,
                    b.y,
                    IntArray(shades.size) { shade(RIBBON_RED, shades[it] * light) },
                    null,
                    Shader.TileMode.CLAMP,
                )
            this.alpha = (alpha * 255f).roundToInt()
        }
    drawPath(path, fill)
    val surface =
        if (onPage) lighting.faceLight(Vec3(0f, 0f, 1f), 1f) else lighting.lambert(Vec3(0f, 0f, 1f))
    drawPath(path, multiplyPaint(surface.toArgb(alpha)))
}

private const val MIN_LENGTH = 1e-3f

private fun Offset.unit(): Offset {
    val length = sqrt(x * x + y * y)
    return if (length < MIN_LENGTH) Offset(0f, 1f) else this / length
}
