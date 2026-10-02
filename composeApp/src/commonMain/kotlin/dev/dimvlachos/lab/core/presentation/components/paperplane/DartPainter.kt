package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The same placement, its stage moved by [by]: for drawing in a stage that is not the root. */
internal fun Placement.shiftedBy(by: Offset) = Placement(at + by, heading, roll, scale, eye, lift)

/**
 * Draws a folded dart from the back of the scene to the front, each facet in one stroke: its
 * outline on the sheet through the eye's view of its own flat piece of paper, lit by how it turns
 * to the light.
 */
internal class DartPainter {
    private val matrix = Matrix()
    private val cut = Path()
    private val shadowPaint = Paint().apply { alpha = PaperPlaneDimens.ShadowAlpha }
    private val light =
        Vec3(PaperPlaneDimens.LightX, PaperPlaneDimens.LightY, PaperPlaneDimens.LightZ).unit()

    // A plan's facets never change shape while it flies.
    private var cutsFor: FoldPlan? = null
    private var cuts: List<Path> = emptyList()
    private var middles: List<Offset> = emptyList()
    private var edges: List<Path> = emptyList()

    // The paper's grain, lit in steps too fine to see, so a frame reuses the filters of the last.
    private val grain = ShaderBrush(ImageShader(paperGrain, TileMode.Repeated, TileMode.Repeated))
    private val filters = HashMap<Int, ColorFilter>()
    private val crease = Stroke(PaperPlaneDimens.CreaseWidth)

    /**
     * The dart in [paper], [inside] where the folds show the inside of the sheet: each facet cut a
     * little past its creases so no seam shows, its grain on it so it turns and foreshortens with
     * it, and pressed in along every crease and edge it has.
     */
    fun DrawScope.drawPaper(
        plan: FoldPlan,
        fold: Float,
        placement: Placement,
        paper: Color,
        inside: Color,
    ) {
        val cuts = cutsOf(plan)
        for (seen in seenFrom(plan, fold, placement)) {
            matrix.setProjection(seen.view)
            val color = if (seen.front) inside else paper
            withTransform({ transform(matrix) }) {
                drawPath(cuts[seen.index], grain, colorFilter = filter(seen, color))
                drawPath(
                    edges[seen.index],
                    lerp(lit(color, seen.turn), Color.Black, PaperPlaneDimens.CreaseShade),
                    alpha = PaperPlaneDimens.CreaseAlpha,
                    style = crease,
                )
            }
        }
    }

    /**
     * The dart's shadow on the stage below it: each corner cast along the light down onto the
     * screen. Paper lying on the screen hides its own; held up off it, the shadow falls away.
     */
    fun DrawScope.drawShadow(plan: FoldPlan, fold: Float, placement: Placement) {
        if (placement.lift < 0.5f) return
        val alongX = -light.x / light.z
        val alongY = -light.y / light.z
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), shadowPaint)
            for (facet in plan.facets) {
                val space = plan.space(facet, fold, placement)
                cut.reset()
                facet.polygon.forEachIndexed { i, p ->
                    val q = space.at(p)
                    val x = q.x + q.z * alongX
                    val y = q.y + q.z * alongY
                    if (i == 0) cut.moveTo(x, y) else cut.lineTo(x, y)
                }
                cut.close()
                drawPath(cut, Color.Black)
            }
            canvas.restore()
        }
    }

    // The facets as the eye sees them, back to front, edge-on ones left out.
    private fun seenFrom(plan: FoldPlan, fold: Float, placement: Placement): List<Seen> {
        cutsOf(plan)
        return plan.facets.indices
            .mapNotNull { i ->
                val space = plan.space(plan.facets[i], fold, placement)
                val view = space.projection(placement.at, placement.eye)
                // Seen the same way round as it lies on the sheet: its front, folded inside the
                // dart.
                val turned = determinant(view)
                if (abs(turned) < 1e-6f * abs(view[8] * view[8] * view[8])) return@mapNotNull null
                val n = space.normal.let { if (it.z < 0f) it * -1f else it }
                Seen(i, view, turned > 0f, (n dot light) - light.z, space.at(middles[i]).z)
            }
            .sortedBy { it.depth }
    }

    // Lit as the paper turns to the light from the window, shaded as it turns away: no change for
    // paper lying flat on the screen.
    private fun lit(color: Color, turn: Float): Color {
        val (keep, glow) = lighting(turn)
        return Color(
            color.red * keep + glow,
            color.green * keep + glow,
            color.blue * keep + glow,
            color.alpha,
        )
    }

    private fun lighting(turn: Float): Pair<Float, Float> {
        val shade =
            if (turn < 0f) min(-turn * PaperPlaneDimens.MaxShade, PaperPlaneDimens.MaxShade) else 0f
        val glow = if (turn > 0f) turn * PaperPlaneDimens.MaxLight else 0f
        return (1f - shade - glow) to glow
    }

    private fun filter(seen: Seen, color: Color): ColorFilter {
        val step = (seen.turn * LightSteps).toInt()
        val key = (if (seen.front) 1 else -1) * (step + 4 * LightSteps.toInt())
        return filters.getOrPut(key) {
            ColorFilter.lighting(lit(color, step / LightSteps), Color.Black)
        }
    }

    // Each facet cut a little past its creases, so the ones either side overlap: cut exactly,
    // their smoothed edges would let the background show through in a hairline along every one.
    private fun cutsOf(plan: FoldPlan): List<Path> {
        if (cutsFor !== plan) {
            cutsFor = plan
            cuts =
                plan.facets.map { facet ->
                    Path().apply {
                        facet.polygon.grown(PaperPlaneDimens.Seam).forEachIndexed { i, p ->
                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                        close()
                    }
                }
            edges =
                plan.facets.map { facet ->
                    Path().apply {
                        facet.polygon.forEachIndexed { i, p ->
                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                        close()
                    }
                }
            middles =
                plan.facets.map { facet ->
                    facet.polygon.fold(Offset.Zero) { sum, p -> sum + p } /
                        facet.polygon.size.toFloat()
                }
        }
        return cuts
    }

    private class Seen(
        val index: Int,
        val view: FloatArray,
        val front: Boolean,
        val turn: Float,
        val depth: Float,
    )
}

// How much a projection flips and stretches the sheet: positive keeps it the same way round, as
// seen from in front of the eye.
private fun determinant(h: FloatArray) =
    h[0] * (h[4] * h[8] - h[5] * h[7]) - h[1] * (h[3] * h[8] - h[5] * h[6]) +
        h[2] * (h[3] * h[7] - h[4] * h[6])

private const val LightSteps = 64f

// The grain of writing paper, a tile of it to repeat across the sheet a unit a pixel: light
// mottling, and a scatter of short fibres lying mostly along the sheet, each a shade darker.
private val paperGrain: ImageBitmap by lazy {
    val size = GrainTile
    val image = ImageBitmap(size, size)
    val canvas = Canvas(image)
    val paint = Paint()
    var seed = 0x2F6B1D
    fun next(): Float {
        seed = seed * 1103515245 + 12345
        return ((seed ushr 8) and 0xFFFF) / 65535f
    }
    for (y in 0 until size) {
        for (x in 0 until size) {
            val v = 1f - PaperPlaneDimens.Grain * next()
            paint.color = Color(v, v, v)
            canvas.drawRect(Rect(x.toFloat(), y.toFloat(), x + 1f, y + 1f), paint)
        }
    }
    paint.strokeWidth = 0.6f
    repeat(GrainFibres) {
        val v = 1f - PaperPlaneDimens.Fibre * (0.5f + next() / 2f)
        paint.color = Color(v, v, v)
        val from = Offset(next() * size, next() * size)
        val angle = (next() - 0.5f) * 1.2f
        val length = 2f + next() * 5f
        canvas.drawLine(from, from + Offset(cos(angle), sin(angle)) * length, paint)
    }
    image
}

private const val GrainTile = 96
private const val GrainFibres = 140

// A 3 × 3 projection, row by row, into a 4 × 4 that draws in the plane z = 0.
private fun Matrix.setProjection(h: FloatArray) {
    reset()
    this[0, 0] = h[0]
    this[1, 0] = h[1]
    this[3, 0] = h[2]
    this[0, 1] = h[3]
    this[1, 1] = h[4]
    this[3, 1] = h[5]
    this[0, 3] = h[6]
    this[1, 3] = h[7]
    this[3, 3] = h[8]
}
