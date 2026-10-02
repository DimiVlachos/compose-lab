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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The same placement, its stage moved by [by]: for drawing in a stage that is not the root. */
internal fun Placement.shiftedBy(by: Offset) = Placement(at + by, heading, roll, scale, eye, lift)

/**
 * Draws a folded dart and its shadow, the dart from the back of the scene to the front, each facet
 * its outline on the sheet through the eye's view of its own flat piece of paper, lit by how it
 * turns to the light. One painter draws one plan; the dart's shape is worked out the first time and
 * only turned and set in place after that.
 */
internal class DartPainter {
    private val matrix = Matrix()
    private val view = FloatArray(9)
    private val shadow = Path()
    private val light =
        Vec3(PaperPlaneDimens.LightX, PaperPlaneDimens.LightY, PaperPlaneDimens.LightZ).unit()

    // What never changes while a plan flies: its facets' cuts and edges, and the dart at rest.
    private var shapedFor: FoldPlan? = null
    private var cuts: List<Path> = emptyList()
    private var edges: List<Path> = emptyList()
    private var rest: List<FacetSpace> = emptyList()

    // The paper's grain, lit in steps too fine to see, so a frame reuses the filters of the last;
    // made again if the paper's colours change.
    private val grain = ShaderBrush(ImageShader(paperGrain, TileMode.Repeated, TileMode.Repeated))
    private val filters = arrayOfNulls<ColorFilter>(2 * FilterSteps)
    private var filtersFor: Pair<Color, Color>? = null
    private val crease = Stroke(PaperPlaneDimens.CreaseWidth)

    /**
     * The dart at [placement] in [paper], [inside] where the folds show the inside of the sheet,
     * with its shadow under it: each facet cut a little past its creases so no seam shows, its
     * grain on it so it turns and foreshortens with it, and pressed in along every crease and edge
     * it has.
     */
    fun DrawScope.drawDart(plan: FoldPlan, placement: Placement, paper: Color, inside: Color) {
        shape(plan)
        if (filtersFor != (paper to inside)) {
            filters.fill(null)
            filtersFor = paper to inside
        }
        val pose = Pose(placement)
        val spaces = rest.map(pose::place)
        drawShadow(plan, spaces, placement)
        val eye = Vec3(placement.at.x, placement.at.y, placement.eye)
        for (i in plan.drawOrder(spaces, eye)) {
            val space = spaces[i]
            space.projectInto(view, placement.at, placement.eye)
            // Seen the same way round as it lies on the sheet: its front, folded inside the dart.
            val turned = determinant(view)
            if (abs(turned) < 1e-6f * abs(view[8] * view[8] * view[8])) continue
            val front = turned > 0f
            val n = space.normal.let { if (it.z < 0f) it * -1f else it }
            val turn = (n dot light) - light.z
            val color = if (front) inside else paper
            matrix.setProjection(view)
            withTransform({ transform(matrix) }) {
                drawPath(cuts[i], grain, colorFilter = filter(front, turn, color))
                drawPath(
                    edges[i],
                    lerp(lit(color, turn), Color.Black, PaperPlaneDimens.CreaseShade),
                    alpha = PaperPlaneDimens.CreaseAlpha,
                    style = crease,
                )
            }
        }
    }

    // The dart's shadow on the stage below it: each corner cast along the light down onto the
    // screen, the whole of it one outline, so where facets overlap it is no darker. Paper lying
    // on the screen hides its own; held up off it, the shadow falls away.
    private fun DrawScope.drawShadow(
        plan: FoldPlan,
        spaces: List<FacetSpace>,
        placement: Placement,
    ) {
        if (placement.lift < 0.5f) return
        val alongX = -light.x / light.z
        val alongY = -light.y / light.z
        shadow.rewind()
        plan.facets.forEachIndexed { i, facet ->
            val cast =
                facet.polygon.map { p ->
                    val q = spaces[i].at(p)
                    Offset(q.x + q.z * alongX, q.y + q.z * alongY)
                }
            // All one way round, so the overlaps fill once.
            val corners = if (signedArea(cast) < 0f) cast.asReversed() else cast
            corners.forEachIndexed { k, c ->
                if (k == 0) shadow.moveTo(c.x, c.y) else shadow.lineTo(c.x, c.y)
            }
            shadow.close()
        }
        drawPath(shadow, Color.Black, alpha = PaperPlaneDimens.ShadowAlpha)
    }

    // Lit as the paper turns to the light from the window, shaded as it turns away: no change for
    // paper lying flat on the screen.
    private fun lit(color: Color, turn: Float): Color {
        val shade =
            if (turn < 0f) min(-turn * PaperPlaneDimens.MaxShade, PaperPlaneDimens.MaxShade) else 0f
        val glow = if (turn > 0f) turn * PaperPlaneDimens.MaxLight else 0f
        val keep = 1f - shade - glow
        return Color(
            color.red * keep + glow,
            color.green * keep + glow,
            color.blue * keep + glow,
            color.alpha,
        )
    }

    private fun filter(front: Boolean, turn: Float, color: Color): ColorFilter {
        val step = (turn * LightSteps).toInt().coerceIn(-FilterSteps / 2, FilterSteps / 2 - 1)
        val slot = step + FilterSteps / 2 + if (front) FilterSteps else 0
        return filters[slot]
            ?: ColorFilter.lighting(lit(color, step / LightSteps), Color.Black).also {
                filters[slot] = it
            }
    }

    // Each facet cut a little past its creases, so the ones either side overlap: cut exactly,
    // their smoothed edges would let the background show through in a hairline along every one.
    private fun shape(plan: FoldPlan) {
        if (shapedFor === plan) return
        shapedFor = plan
        cuts = plan.facets.map { it.polygon.grown(PaperPlaneDimens.Seam).toPath() }
        edges = plan.facets.map { it.polygon.toPath() }
        rest = plan.folded()
    }
}

private fun List<Offset>.toPath() =
    Path().apply {
        forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        close()
    }

// How much a projection flips and stretches the sheet: positive keeps it the same way round, as
// seen from in front of the eye.
private fun determinant(h: FloatArray) =
    h[0] * (h[4] * h[8] - h[5] * h[7]) - h[1] * (h[3] * h[8] - h[5] * h[6]) +
        h[2] * (h[3] * h[7] - h[4] * h[6])

private const val LightSteps = 64f

// Light steps either side of none, for each side of the paper: a turn lies within ±1.5.
private const val FilterSteps = 256

// The grain of writing paper, a tile of it to repeat across the sheet a unit a pixel: light
// mottling, and a scatter of short fibres lying mostly along the sheet, each a shade darker.
internal val paperGrain: ImageBitmap by lazy {
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
