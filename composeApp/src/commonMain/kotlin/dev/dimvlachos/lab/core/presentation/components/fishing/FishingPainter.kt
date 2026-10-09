package dev.dimvlachos.lab.core.presentation.components.fishing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a [FishingRefreshState]'s band: the rod, its line and the bobber in the air, the water over
 * the top of the list, and a splash's drops. Its paths and strokes are kept and drawn again each
 * frame, so a frame allocates nothing; only the water's shading and the strokes are made again, and
 * only when the band's height, the density or the colours change.
 */
internal class FishingPainter {
    private val rod = Path()
    private val line = Path()
    private val water = Path()

    private var shadeFor = -1f
    private var shadeColors: AppColors? = null
    private var shade: Brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))

    // The strokes, made for the density they were last drawn at.
    private var strokesFor = -1f
    private var rodStroke = Stroke()
    private var lineStroke = Stroke()
    private var crestStroke = Stroke()
    private var hookStroke = Stroke()

    /** Draws the band, open as far as the pull has opened it, over whatever is under it. */
    fun DrawScope.draw(state: FishingRefreshState, colors: AppColors) {
        state.frame
        val open = state.open
        if (open <= 0f) return
        if (density != strokesFor) makeStrokes()
        val band = FishingDimens.Band.toPx()
        // The band opens as far as the pull, and its water always meets the top of the list, which
        // moves down by the air above the water as it opens: the rod comes down into the gap from
        // above rather than lying over the list.
        clipRect(bottom = band * open) {
            translate(top = -(1f - open) * FishingDimens.WaterLevel.toPx()) {
                drawRod(state, colors)
                drawLine(state, colors)
                drawBobber(state, colors)
                drawWater(state, colors, band)
                drawSplash(state, colors)
            }
        }
    }

    private fun DrawScope.makeStrokes() {
        strokesFor = density
        rodStroke = Stroke(RodWidth.toPx(), cap = StrokeCap.Round)
        lineStroke = Stroke(LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        crestStroke = Stroke(CrestWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        hookStroke = Stroke(LineWidth.toPx() * HookWeight, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawRod(state: FishingRefreshState, colors: AppColors) {
        val rig = state.rig
        val butt = state.px(rig.butt)
        val tip = state.px(rig.tip)
        // The rod bends along its length: it leaves the butt pointing where it would rest, and
        // curves round to its bent tip.
        val reach = FishingDimens.RodLength.toPx()
        val ahead =
            butt +
                Offset(cos(FishingDimens.RodRise), -sin(FishingDimens.RodRise)) * (reach * RodStiff)
        rod.rewind()
        rod.moveTo(butt.x, butt.y)
        rod.quadraticTo(ahead.x, ahead.y, tip.x, tip.y)
        drawPath(rod, colors.rodBlank, style = rodStroke)
        // The cork handle and the reel, near the butt.
        val along = (ahead - butt) / (reach * RodStiff)
        val handleEnd = butt + along * HandleLength.toPx()
        drawLine(colors.rodCork, butt, handleEnd, HandleWidth.toPx(), StrokeCap.Round)
        val reel = butt + along * ReelAt.toPx() + Offset(0f, ReelDrop.toPx())
        drawCircle(colors.rodReel, ReelRadius.toPx(), reel)
        drawCircle(colors.background, ReelRadius.toPx() * 0.4f, reel)
    }

    private fun DrawScope.drawLine(state: FishingRefreshState, colors: AppColors) {
        val rope = state.rig.line
        line.rewind()
        val first = state.px(rope[0])
        line.moveTo(first.x, first.y)
        // Through the midpoints, curving at each point: a smooth line from a few points.
        for (i in 1 until rope.size - 1) {
            val at = state.px(rope[i])
            val next = state.px(rope[i + 1])
            line.quadraticTo(at.x, at.y, (at.x + next.x) / 2f, (at.y + next.y) / 2f)
        }
        val last = state.px(rope[rope.size - 1])
        line.lineTo(last.x, last.y)
        drawPath(line, colors.fishingLine, style = lineStroke)
    }

    private fun DrawScope.drawBobber(state: FishingRefreshState, colors: AppColors) {
        val rig = state.rig
        val alpha = rig.bobberAlpha
        if (alpha <= 0f) return
        val at = state.px(rig.bobber)
        val radius = BobberRadius.toPx()
        // Out of the water, an empty hook hangs under it.
        val out = rig.bobber.y < FishingDimens.WaterLevel.value - HookShows
        if (out) {
            val hook = at + Offset(0f, radius + HookDrop.toPx())
            drawLine(colors.fishingLine, at, hook, LineWidth.toPx(), alpha = alpha)
            drawArc(
                colors.rodReel,
                startAngle = 0f,
                sweepAngle = 200f,
                useCenter = false,
                topLeft = hook - Offset(HookRadius.toPx() * 2f, HookRadius.toPx()),
                size = Size(HookRadius.toPx() * 2f, HookRadius.toPx() * 2f),
                alpha = alpha,
                style = hookStroke,
            )
        }
        drawCircle(colors.bobberWhite, radius, at, alpha = alpha)
        drawArc(
            colors.bobberRed,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = at - Offset(radius, radius),
            size = Size(radius * 2f, radius * 2f),
            alpha = alpha,
        )
        drawLine(
            colors.bobberRed,
            at - Offset(0f, radius),
            at - Offset(0f, radius + StemLength.toPx()),
            StemWidth.toPx(),
            StrokeCap.Round,
            alpha = alpha,
        )
    }

    private fun DrawScope.drawWater(state: FishingRefreshState, colors: AppColors, band: Float) {
        val surface = state.rig.water
        val level = FishingDimens.WaterLevel.toPx()
        val heights = surface.heights
        val gap = size.width / (heights.size - 1)
        water.rewind()
        water.moveTo(0f, level + heights[0] * state.density)
        for (i in 1 until heights.size) {
            water.lineTo(i * gap, level + heights[i] * state.density)
        }
        // The crest along the top, then the body under it down to the band's foot.
        drawPath(water, colors.waterCrest, style = crestStroke)
        water.lineTo(size.width, band)
        water.lineTo(0f, band)
        water.close()
        if (band != shadeFor || colors !== shadeColors) {
            shadeFor = band
            shadeColors = colors
            // Clear at the top, deeper below, then fading out at its foot, so the water melts into
            // the list under it rather than ending in a hard edge across it.
            shade =
                Brush.verticalGradient(
                    0f to colors.waterTop,
                    DeepestAt to colors.waterDeep,
                    1f to colors.waterDeep.copy(alpha = 0f),
                    startY = level,
                    endY = band,
                )
        }
        drawPath(water, shade)
    }

    // A splash's drops: thrown up and out from where it landed, falling back as they fade.
    private fun DrawScope.drawSplash(state: FishingRefreshState, colors: AppColors) {
        val rig = state.rig
        if (!rig.splashing) return
        val p = rig.splashAge / FishingDimens.SplashSeconds
        val from = state.px(rig.splashAt)
        val high = FishingDimens.SplashHeight.toPx()
        val reach = FishingDimens.SplashReach.toPx()
        for (i in DropLean.indices) {
            // Each drop thrown its own way, some higher than others, as water splashes: not an even
            // fan.
            val rise = high * DropRise[i]
            val x = from.x + DropLean[i] * reach * p
            val y = from.y - 4f * rise * p * (1f - p)
            drawCircle(colors.waterCrest, DropRadius.toPx() * (1f - p * 0.5f), Offset(x, y), 1f - p)
        }
    }

    private companion object {
        // How straight the rod leaves its butt, as a share of its length: a stiff rod bends near
        // its tip.
        const val RodStiff = 0.6f
        val RodWidth = 3.dp
        val HandleLength = 30.dp
        val HandleWidth = 6.dp
        val ReelAt = 22.dp
        val ReelDrop = 7.dp
        val ReelRadius = 5.dp
        val LineWidth = 1.dp
        val BobberRadius = 5.dp
        val StemLength = 4.dp
        val StemWidth = 1.5.dp
        val HookDrop = 6.dp
        val HookRadius = 3.dp
        val CrestWidth = 1.5.dp

        // The hook's wire, this many times the line's width.
        const val HookWeight = 1.4f
        val DropRadius = 2.dp

        // Where a splash's drops lean, from far left (-1) to far right (1), and how high each
        // flies,
        // as a share of the splash's height: uneven, as a real splash is.
        val DropLean = floatArrayOf(-1f, -0.6f, -0.25f, 0.1f, 0.4f, 0.75f, 0.95f)
        val DropRise = floatArrayOf(0.5f, 0.85f, 1f, 0.7f, 0.95f, 0.6f, 0.4f)

        // How far down the water it is at its deepest colour, as a share of its depth.
        const val DeepestAt = 0.55f

        // The hook shows once the bobber is this many dp above the water.
        const val HookShows = 8f
    }
}
