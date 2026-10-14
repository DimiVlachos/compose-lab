package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.presentation.components.magnet.SoftShadow
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Draws the typewriter: the sheet with what is typed on it, the enamel body with its basket of type
 * bars and its badge, the ribbon, the platen and its knobs, the paper guide, a type bar in flight
 * and the return lever. Everything is in machine units, [unit] px each, the machine's top left at
 * [origin]; [bottom] is how far down the body runs, in px.
 */
internal class TypewriterPainter {
    private val shadow = SoftShadow()
    private val bodyPath = Path()
    private val basketPath = Path()
    private val knobPath = Path()

    // The paper's grain: specks scattered over a sheet, in units from its top left, made once.
    private val grain: List<Offset> =
        List(GrainSpecks) { i ->
            Offset(
                noise(i, 11) * TypewriterDimens.SheetWidth,
                noise(i, 12) * TypewriterDimens.SheetHeight,
            )
        }
    private val grainPoints = ArrayList<Offset>(GrainSpecks)

    fun DrawScope.drawMachine(
        state: TypewriterState,
        atlas: GlyphAtlas,
        colors: AppColors,
        unit: Float,
        origin: Offset,
        bottom: Float,
        badge: TextLayoutResult,
        model: TextLayoutResult,
    ) {
        state.frame
        val u = unit
        fun x(units: Float) = origin.x + units * u
        fun y(units: Float) = origin.y + units * u

        drawSheet(state, atlas, colors, u, origin)
        drawBody(colors, u, origin, bottom, badge, model)
        drawRibbon(state, colors, u, origin)
        drawPlaten(state, colors, u, origin)
        // The paper guide: two chrome tabs either side of where the letters strike.
        for (dx in GuideTabs) {
            val left = x(TypewriterDimens.StrikeX + dx)
            val top = y(TypewriterDimens.StrikeY + 1f)
            with(shadow) { drawSoftShadow(left, top + u, left + 5f * u, top + 13f * u, u, 0.3f) }
            drawRoundRect(
                Brush.horizontalGradient(
                    listOf(colors.clipSheen, colors.clipMetal),
                    startX = left,
                    endX = left + 5f * u,
                ),
                Offset(left, top),
                Size(5f * u, 12f * u),
                CornerRadius(1.5f * u),
            )
        }
        drawTypebar(state, colors, u, origin)
        drawLever(state, colors, u, origin)
        if (state.touchAlpha > 0f) {
            drawTouch(colors.touch, Offset(x(state.touch.x), y(state.touch.y)), state.touchAlpha)
        }
    }

    private fun DrawScope.drawSheet(
        state: TypewriterState,
        atlas: GlyphAtlas,
        colors: AppColors,
        u: Float,
        origin: Offset,
    ) {
        val left = origin.x + state.carriage * u
        val top = origin.y + state.sheetTop * u
        val width = TypewriterDimens.SheetWidth * u
        val height = TypewriterDimens.SheetHeight * u
        // Hidden under the body below the platen: drawn no further down than the body's top.
        val shown = minOf(height, origin.y + TypewriterDimens.BodyTop * u + u - top)
        if (shown <= 0f) return
        with(shadow) {
            drawSoftShadow(left, top + u, left + width, top + shown, 3f * u, 0.2f)
            drawSoftShadow(left, top + 6f * u, left + width, top + shown, 10f * u, 0.12f)
        }
        clipRect(left, top, left + width, top + shown) {
            drawRect(
                Brush.horizontalGradient(
                    0f to colors.typePaperEdge,
                    0.06f to colors.typePaper,
                    0.94f to colors.typePaper,
                    1f to colors.typePaperEdge,
                    startX = left,
                    endX = left + width,
                ),
                Offset(left, top),
                Size(width, height),
            )
            drawGrain(colors, left, top, u)
            drawTyped(atlas, state.sheet.lines, left, top, u) { line, column ->
                if (state.landed(line, column)) 1f else 0f
            }
            // Letters the tape is lifting off: fading as they rise from the paper.
            for (lifted in state.lifting) {
                val t = ((state.now - lifted.at) / TypewriterDimens.TapeSeconds).coerceIn(0f, 1f)
                drawGlyphAt(
                    atlas,
                    lifted.line,
                    lifted.column,
                    lifted.char,
                    left,
                    top - TypewriterDimens.TapeLift * easeOut(t) * u,
                    u,
                    1f - easeOut(t),
                )
            }
            // The paper curves into the roller: a shade just over the platen.
            val roll = origin.y + TypewriterDimens.PlatenTop * u
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, colors.platen.copy(alpha = RollerShade)),
                    startY = roll - RollerShadeHeight * u,
                    endY = roll,
                ),
                Offset(left, roll - RollerShadeHeight * u),
                Size(width, RollerShadeHeight * u),
            )
        }
    }

    private fun DrawScope.drawGrain(colors: AppColors, left: Float, top: Float, u: Float) {
        grainPoints.clear()
        for (speck in grain) grainPoints += Offset(left + speck.x * u, top + speck.y * u)
        drawPoints(
            grainPoints,
            PointMode.Points,
            colors.typeInk.copy(alpha = GrainAlpha),
            strokeWidth = 0.8f * u,
            cap = StrokeCap.Round,
        )
    }

    private fun DrawScope.drawBody(
        colors: AppColors,
        u: Float,
        origin: Offset,
        bottom: Float,
        badge: TextLayoutResult,
        model: TextLayoutResult,
    ) {
        val top = origin.y + TypewriterDimens.BodyTop * u
        val corner = TypewriterDimens.BodyCorner * u
        with(shadow) { drawSoftShadow(0f, top - 4f * u, size.width, bottom, 10f * u, 0.18f) }
        bodyPath.reset()
        bodyPath.addRoundRect(
            RoundRect(
                Rect(0f, top, size.width, bottom + corner),
                topLeft = CornerRadius(corner),
                topRight = CornerRadius(corner),
            )
        )
        val deep = top + TypewriterDimens.BodyHeight * u
        drawPath(
            bodyPath,
            Brush.verticalGradient(
                0f to colors.enamelLight,
                0.22f to colors.enamel,
                1f to colors.enamelDeep,
                startY = top,
                endY = deep,
            ),
        )
        // Light catching the enamel's top edge.
        clipPath(bodyPath) {
            drawPath(
                bodyPath,
                Color.White.copy(alpha = EdgeLight),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * u),
            )
        }
        drawBasket(colors, u, origin)
        // The badge on the left of the body: the model's name, and under it, smaller, what it is.
        val badgeAt =
            Offset(
                origin.x + TypewriterDimens.BadgeLeft * u,
                origin.y + (TypewriterDimens.BodyTop + TypewriterDimens.BadgeTop) * u,
            )
        drawText(badge, colors.clipSheen.copy(alpha = BadgeAlpha), badgeAt)
        drawText(
            model,
            colors.clipSheen.copy(alpha = BadgeAlpha * ModelAlpha),
            badgeAt + Offset(0f, badge.size.height + u),
        )
    }

    private fun DrawScope.drawBasket(colors: AppColors, u: Float, origin: Offset) {
        val left = origin.x + TypewriterDimens.BasketLeft * u
        val top = origin.y + (TypewriterDimens.BodyTop + TypewriterDimens.BasketTop) * u
        val width = TypewriterDimens.BasketWidth * u
        val height = TypewriterDimens.BasketHeight * u
        basketPath.reset()
        basketPath.addRoundRect(
            RoundRect(
                Rect(left, top, left + width, top + height),
                topLeft = CornerRadius(width / 2f, height),
                topRight = CornerRadius(width / 2f, height),
                bottomLeft = CornerRadius(4f * u),
                bottomRight = CornerRadius(4f * u),
            )
        )
        clipPath(basketPath) {
            drawRect(colors.ribbonBlack, Offset(left, top), Size(width, height))
            // The type bars, a fan of steel from the middle of the basket's foot.
            val hub = Offset(left + width / 2f, top + height)
            for (i in 0..BasketBars) {
                val a = PI + PI * i / BasketBars
                val reach = width
                drawLine(
                    colors.clipMetal,
                    hub,
                    hub + Offset((cos(a) * reach).toFloat(), (sin(a) * reach).toFloat()),
                    strokeWidth = 1.6f * u,
                )
            }
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        colors.platen.copy(alpha = BasketShadeTop),
                        colors.platen.copy(alpha = BasketShadeFoot),
                    ),
                    startY = top,
                    endY = top + height,
                ),
                Offset(left, top),
                Size(width, height),
            )
        }
        drawLine(
            Color.White.copy(alpha = EdgeLight),
            Offset(left + 4f * u, top + height + 0.5f * u),
            Offset(left + width - 4f * u, top + height + 0.5f * u),
            strokeWidth = u,
        )
    }

    private fun DrawScope.drawRibbon(
        state: TypewriterState,
        colors: AppColors,
        u: Float,
        origin: Offset,
    ) {
        val p = (state.now - state.ribbonAt) / TypewriterDimens.RibbonSeconds
        if (p < 0f || p >= 1f) return
        val h = if (p < 0.25f) easeOut(p / 0.25f) else 1f - ease((p - 0.25f) / 0.75f)
        val top =
            lerp(
                TypewriterDimens.PlatenTop + 4f,
                TypewriterDimens.StrikeY - TypewriterDimens.RibbonLift,
                h,
            )
        val left = origin.x + (TypewriterDimens.StrikeX - TypewriterDimens.RibbonWidth / 2f) * u
        val y = origin.y + top * u
        val w = TypewriterDimens.RibbonWidth * u
        val half = TypewriterDimens.RibbonHeight / 2f * u
        // Black over red; the correction tape's white in place of the red as it lifts a letter.
        drawRect(colors.ribbonBlack, Offset(left, y), Size(w, half))
        drawRect(
            if (state.taping) colors.typePaper else colors.ribbonRed,
            Offset(left, y + half),
            Size(w, half),
        )
    }

    private fun DrawScope.drawPlaten(
        state: TypewriterState,
        colors: AppColors,
        u: Float,
        origin: Offset,
    ) {
        val left = origin.x + (state.carriage - TypewriterDimens.PlatenOverhang) * u
        val top = origin.y + TypewriterDimens.PlatenTop * u
        val width = (TypewriterDimens.SheetWidth + TypewriterDimens.PlatenOverhang * 2f) * u
        val height = TypewriterDimens.PlatenHeight * u
        with(shadow) {
            drawSoftShadow(left, top + 3f * u, left + width, top + height + 3f * u, 5f * u, 0.35f)
        }
        // The roll's knobs at either end, ridged, turning with the paper.
        val roll = (state.feed + state.shown - state.feedIn) * KnobTurn
        for (side in listOf(left - TypewriterDimens.KnobWidth * u, left + width)) {
            drawKnob(colors, side, top - TypewriterDimens.KnobRise * u, u, roll)
        }
        drawRoundRect(
            Brush.verticalGradient(
                0f to colors.platenSheen,
                0.22f to lerp(colors.platenSheen, colors.platen, 0.7f),
                0.62f to colors.platen,
                1f to lerp(colors.platen, colors.platenSheen, 0.45f),
                startY = top,
                endY = top + height,
            ),
            Offset(left, top),
            Size(width, height),
            CornerRadius(TypewriterDimens.PlatenCorner * u),
        )
    }

    private fun DrawScope.drawKnob(
        colors: AppColors,
        left: Float,
        top: Float,
        u: Float,
        roll: Float,
    ) {
        val width = TypewriterDimens.KnobWidth * u
        val height = TypewriterDimens.KnobHeight * u
        knobPath.reset()
        knobPath.addRoundRect(
            RoundRect(Rect(left, top, left + width, top + height), CornerRadius(8f * u))
        )
        with(shadow) {
            drawSoftShadow(left, top + 2f * u, left + width, top + height + 2f * u, 4f * u, 0.4f)
        }
        clipPath(knobPath) {
            drawRect(colors.platen, Offset(left, top), Size(width, height))
            // Ridges every four units, rolling round as the paper moves.
            val ridge = lerp(colors.platen, colors.platenSheen, 0.45f)
            val shift = roll - floor(roll / KnobPitch) * KnobPitch
            var y = -KnobPitch + shift
            while (y < TypewriterDimens.KnobHeight) {
                drawRect(ridge, Offset(left, top + y * u), Size(width, 2f * u))
                y += KnobPitch
            }
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.White.copy(alpha = 0.18f),
                    0.4f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.35f),
                    startX = left,
                    endX = left + width,
                ),
                Offset(left, top),
                Size(width, height),
            )
        }
    }

    private fun DrawScope.drawTypebar(
        state: TypewriterState,
        colors: AppColors,
        u: Float,
        origin: Offset,
    ) {
        val p = (state.now - state.barAt) / TypewriterDimens.TypebarSeconds
        if (p < 0f || p >= 1f) return
        val from = state.barFrom
        val px = TypewriterDimens.StrikeX - BarSpread + BarSpread * 2f * from
        val py = BarRest - BarArch * sin(PI.toFloat() * from)
        val h = if (p < 0.35f) easeOut(p / 0.35f) else 1f - ease((p - 0.35f) / 0.65f)
        val tx = lerp(px + (TypewriterDimens.StrikeX - px) * 0.2f, TypewriterDimens.StrikeX, h)
        val ty = lerp(py - 10f, TypewriterDimens.StrikeY - 6f, h)
        val start = Offset(origin.x + px * u, origin.y + py * u)
        val end = Offset(origin.x + tx * u, origin.y + ty * u)
        drawLine(colors.clipMetal, start, end, strokeWidth = 3f * u, cap = StrokeCap.Round)
        drawLine(
            colors.clipSheen,
            start - Offset(0f, 0.6f * u),
            end - Offset(0f, 0.6f * u),
            strokeWidth = 1.2f * u,
            cap = StrokeCap.Round,
        )
    }

    private fun DrawScope.drawLever(
        state: TypewriterState,
        colors: AppColors,
        u: Float,
        origin: Offset,
    ) {
        val pivot =
            Offset(
                origin.x + (state.carriage - TypewriterDimens.LeverOffset) * u,
                origin.y + TypewriterDimens.LeverPivotY * u,
            )
        val angle = TypewriterDimens.LeverAngle + TypewriterDimens.LeverSwing * state.leverPush
        rotate(angle, pivot) {
            translate(pivot.x, pivot.y) {
                with(shadow) { drawSoftShadow(0f, -1f * u, 46f * u, 4f * u, 3f * u, 0.3f) }
                drawRoundRect(
                    Brush.verticalGradient(
                        0f to colors.clipSheen,
                        0.6f to colors.clipMetal,
                        1f to lerp(colors.clipMetal, colors.platen, 0.3f),
                        startY = -2.5f * u,
                        endY = 2.5f * u,
                    ),
                    Offset(0f, -2.5f * u),
                    Size(TypewriterDimens.LeverLength * u, 5f * u),
                    CornerRadius(2.5f * u),
                )
                drawRoundRect(
                    Brush.radialGradient(
                        listOf(lerp(colors.platenSheen, colors.platen, 0.3f), colors.platen),
                        center = Offset(44f * u, -3f * u),
                        radius = 12f * u,
                    ),
                    Offset(36f * u, -7f * u),
                    Size(20f * u, 14f * u),
                    CornerRadius(7f * u),
                )
            }
        }
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, colors.clipMetal, lerp(colors.clipMetal, colors.platen, 0.4f)),
                center = pivot - Offset(2f * u, 2f * u),
                radius = 7f * u,
            ),
            radius = 6f * u,
            center = pivot,
        )
    }

    internal companion object {
        const val GrainSpecks = 160
        const val GrainAlpha = 0.07f
        const val RollerShade = 0.16f
        const val RollerShadeHeight = 9f
        const val EdgeLight = 0.35f
        const val BadgeAlpha = 0.8f
        const val ModelAlpha = 0.7f
        const val BasketBars = 30
        const val BasketShadeTop = 0.78f
        const val BasketShadeFoot = 0.2f
        const val KnobTurn = 0.9f
        const val KnobPitch = 4f

        // A type bar rises from the basket's rim: this far either side of the strike for the
        // keys at its ends, from this far down the machine, the rim arching this high.
        const val BarSpread = 72f
        const val BarRest = 86f
        const val BarArch = 10f

        val GuideTabs = listOf(-15f, 10f)
    }
}

/**
 * Draws [lines] as typed on a sheet whose top left is at [left], [top], [u] px to the unit, each
 * letter in its place as [strikeOf] has it struck, at the alpha [shown] gives its place.
 */
internal fun DrawScope.drawTyped(
    atlas: GlyphAtlas,
    lines: List<String>,
    left: Float,
    top: Float,
    u: Float,
    shown: (line: Int, column: Int) -> Float = { _, _ -> 1f },
) {
    for ((line, text) in lines.withIndex()) {
        for ((column, char) in text.withIndex()) {
            if (char == ' ') continue
            val alpha = shown(line, column)
            if (alpha <= 0f) continue
            drawGlyphAt(atlas, line, column, char, left, top, u, alpha)
        }
    }
}

/** Draws one letter, [char], struck at [column] of [line] on a sheet at [left], [top]. */
internal fun DrawScope.drawGlyphAt(
    atlas: GlyphAtlas,
    line: Int,
    column: Int,
    char: Char,
    left: Float,
    top: Float,
    u: Float,
    alpha: Float,
) {
    val strike = strikeOf(line, column, char)
    val x =
        left +
            (TypewriterDimens.Margin + (column + 0.5f) * TypewriterDimens.CharWidth + strike.dx) * u
    val baseline =
        top +
            (TypewriterDimens.TopMargin +
                line * TypewriterDimens.LineHeight +
                TypewriterDimens.GlyphBaseline +
                strike.dy) * u
    with(atlas) { drawGlyph(char, x, baseline, strike.tilt, strike.ink * alpha) }
}
