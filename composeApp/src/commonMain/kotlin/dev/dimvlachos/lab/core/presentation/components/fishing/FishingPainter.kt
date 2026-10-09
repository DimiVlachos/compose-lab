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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
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
    private val surface = Path()

    private var shadeFor = -1f
    private var shadeColors: AppColors? = null
    private var shade: Brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    private var shaft: Brush = shade

    // The deep sea over the list while a catch comes up, made for the height it was last drawn at.
    private var deepFor = -1f
    private var deepColors: AppColors? = null
    private var deep: Brush = shade

    // The strokes, made for the density they were last drawn at.
    private var strokesFor = -1f
    private var rodStroke = Stroke()
    private var lineStroke = Stroke()
    private var crestStroke = Stroke()
    private var sheenStroke = Stroke()
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
        val shift = -(1f - open) * FishingDimens.WaterLevel.toPx()
        // A catch on the line first, under the water and the line, and not kept to the band: it
        // goes on down into the list.
        drawCatch(state, colors, shift)
        clipRect(bottom = band * open) {
            translate(top = shift) {
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
        sheenStroke = Stroke(SheenWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
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

    // The caught cards hauled up from the deep on the line: small and faint far down, growing to
    // their full size and clearing as they come up, one hung under another, until each reaches its
    // place at the top of the list just as the list makes room for it.
    private fun DrawScope.drawCatch(state: FishingRefreshState, colors: AppColors, shift: Float) {
        val rig = state.rig
        if (!rig.carrying) return
        val box = state.box ?: return
        if (!box.isAttached) return
        // The list under the water darkens into the deep the catch comes up from.
        val level = FishingDimens.WaterLevel.toPx() + shift
        if (size.height != deepFor || colors !== deepColors) {
            deepFor = size.height
            deepColors = colors
            deep =
                Brush.verticalGradient(
                    0f to colors.waterDeep.copy(alpha = DeepTop),
                    1f to colors.waterDeep.copy(alpha = DeepFoot),
                    startY = FishingDimens.WaterLevel.toPx(),
                    endY = size.height,
                )
        }
        drawRect(
            deep,
            topLeft = Offset(0f, level),
            size = Size(size.width, size.height - level),
            alpha = rig.depth,
        )
        val hauled = rig.hauled
        val scale = DeepScale + (1f - DeepScale) * hauled
        val clear = DeepClear + (1f - DeepClear) * hauled
        val sway = CatchSway * sin(rig.clock * SwayPace) * (1f - hauled)
        val gap = ChainGap.toPx()
        // Where the line's leader comes down from: the bobber, or the card above.
        var from = state.px(rig.bobber) + Offset(0f, shift + BobberRadius.toPx())
        // The first card's top comes up along the rig's own path; each next hangs under the last.
        var top = state.px(rig.catchTop) + Offset(0f, shift)
        for (i in 0 until rig.catchCount) {
            if (!state.hooked(i)) break
            val card = state.caught[i] ?: break
            val where = card.coordinates ?: break
            if (!where.isAttached) break
            val width = card.size.width.toFloat()
            val height = card.size.height.toFloat()
            if (width <= 0f) break
            // Where it goes in the list, by the time it is all the way up.
            val placed = box.localPositionOf(where, Offset.Zero) + Offset(width / 2f, 0f)
            val at = top + (placed - top) * hauled
            drawLine(colors.fishingLine, from, at, LineWidth.toPx(), alpha = clear)
            card.layer.alpha = clear
            withTransform({
                translate(at.x, at.y)
                rotate(sway * (i + 1), pivot = Offset.Zero)
                scale(scale, scale, pivot = Offset.Zero)
                translate(-width / 2f, 0f)
            }) {
                drawLayer(card.layer)
            }
            from = at + Offset(0f, height * scale)
            top = from + Offset(0f, gap)
        }
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
        // With a catch on it, the leader runs down to the catch instead, drawn with it.
        if (out && !(rig.carrying && state.caught.isNotEmpty())) {
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
        val rig = state.rig
        val level = FishingDimens.WaterLevel.toPx()
        val columns = rig.water.heights.size
        val gap = size.width / (columns - 1)
        val dp = state.density
        // The surface through every column, ripples and swell together, curving smoothly through
        // the midpoints between them rather than in straight runs from one to the next.
        water.rewind()
        var lastX = 0f
        var lastY = level + rig.surfaceAt(0f) * dp
        water.moveTo(lastX, lastY)
        for (i in 1 until columns) {
            val x = i * gap
            val y = level + rig.surfaceAt(x / dp) * dp
            water.quadraticTo(lastX, lastY, (lastX + x) / 2f, (lastY + y) / 2f)
            lastX = x
            lastY = y
        }
        water.lineTo(lastX, lastY)
        surface.rewind()
        surface.addPath(water)
        water.lineTo(size.width, band)
        water.lineTo(0f, band)
        water.close()
        if (band != shadeFor || colors !== shadeColors) {
            shadeFor = band
            shadeColors = colors
            // Clear and bright at the top, deepening below, then fading out at its foot, so the
            // water melts into the list under it rather than ending in a hard edge across it.
            shade =
                Brush.verticalGradient(
                    0f to colors.waterTop,
                    DeepestAt to colors.waterDeep,
                    1f to colors.waterDeep.copy(alpha = 0f),
                    startY = level,
                    endY = band,
                )
            shaft =
                Brush.verticalGradient(
                    0f to colors.waterLight,
                    1f to colors.waterLight.copy(alpha = 0f),
                    startY = level,
                    endY = band,
                )
        }
        drawPath(water, shade)
        // Sunlight reaching down into the water in slanting shafts that sway with the swell.
        clipPath(water) {
            val wide = ShaftWidth.toPx()
            for (i in ShaftAt.indices) {
                val sway = ShaftSway.toPx() * sin(rig.clock * ShaftPace + i * 1.7f)
                val x = size.width * ShaftAt[i] + sway
                val glow = ShaftGlow * (0.6f + 0.4f * sin(rig.clock * ShaftFlicker + i * 2.3f))
                rotate(ShaftLean, pivot = Offset(x, level)) {
                    // Three widths laid over each other: brightest down the middle, fading out to
                    // the sides, so a shaft has soft edges rather than a pane of glass's.
                    for (share in ShaftLayers) {
                        val layer = wide * share
                        drawRect(
                            shaft,
                            topLeft = Offset(x - layer / 2f, level - wide),
                            size = Size(layer, band - level + wide * 2f),
                            alpha = glow / ShaftLayers.size,
                        )
                    }
                }
            }
        }
        // Light caught just under the surface, then the crest where water meets air.
        translate(top = SheenDepth.toPx()) {
            drawPath(surface, colors.waterLight, alpha = SheenGlow, style = sheenStroke)
        }
        drawPath(surface, colors.waterCrest, style = crestStroke)
        // Sun glints on the crest, each twinkling on and off on its own beat as the swell rolls.
        for (i in 0 until Glints) {
            val beat = sin(rig.clock * GlintPace * (1f + i * 0.13f) + i * 2.1f)
            if (beat <= 0f) continue
            val twinkle = beat * beat * beat * beat * beat * beat
            val x = size.width * (i + 0.5f) / Glints + GlintDrift.toPx() * sin(rig.clock * 0.4f + i)
            val y = level + rig.surfaceAt(x / dp) * dp
            drawCircle(colors.waterCrest, GlintRadius.toPx(), Offset(x, y), alpha = twinkle)
        }
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
        val CrestWidth = 1.2.dp

        // A soft band of light this wide, this far under the surface, this bright.
        val SheenWidth = 5.dp
        val SheenDepth = 4.dp
        const val SheenGlow = 0.22f

        // Shafts of sunlight: where across the band they fall, this wide, leaning this many
        // degrees, swaying this far at this pace, flickering at this pace, at most this bright.
        val ShaftAt = floatArrayOf(0.12f, 0.37f, 0.6f, 0.85f)
        val ShaftWidth = 22.dp
        val ShaftLayers = floatArrayOf(1f, 0.6f, 0.3f)
        const val ShaftLean = -14f
        val ShaftSway = 10.dp
        const val ShaftPace = 0.5f
        const val ShaftFlicker = 0.8f
        const val ShaftGlow = 0.2f

        // Glints on the crest: this many, this small, drifting this far, twinkling at this pace.
        const val Glints = 7
        val GlintRadius = 1.4.dp
        val GlintDrift = 18.dp
        const val GlintPace = 1.6f

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

        // A catch comes up from the deep this small and this clear, swaying this many degrees at
        // this pace as it comes, each card hung this far under the one above.
        // The deep over the list is this see-through at the water's foot and this much at the
        // bottom of the list.
        const val DeepTop = 0.55f
        const val DeepFoot = 0.85f
        const val DeepScale = 0.45f
        const val DeepClear = 0.3f
        const val CatchSway = 5f
        const val SwayPace = 3f
        val ChainGap = 10.dp

        // The hook shows once the bobber is this many dp above the water.
        const val HookShows = 8f
    }
}
