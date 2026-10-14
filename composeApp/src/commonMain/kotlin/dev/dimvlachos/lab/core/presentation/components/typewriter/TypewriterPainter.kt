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
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * and the return lever. Everything is in machine units, a given number of px each, from the
 * machine's top left; the body runs on down to a foot. Gradients are made once for a size of unit
 * and a palette, each from its own part's top left, and the part is moved under them.
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

    private var brushes: Brushes? = null

    private fun brushes(u: Float, colors: AppColors): Brushes =
        brushes?.takeIf { it.u == u && it.colors == colors }
            ?: Brushes(u, colors).also { brushes = it }

    /**
     * Draws the machine, [unit] px to the unit, its top left at [origin], its body running down to
     * [bottom]; [badge] and [model] lettered on the body, if given.
     */
    fun DrawScope.drawMachine(
        state: TypewriterState,
        atlas: GlyphAtlas,
        colors: AppColors,
        unit: Float,
        origin: Offset,
        bottom: Float,
        badge: TextLayoutResult?,
        model: TextLayoutResult?,
    ) {
        state.frame
        val u = unit
        val b = brushes(u, colors)
        drawBody(b, colors, u, origin, bottom, badge, model)
        translate(origin.x, origin.y) {
            drawSheet(state, atlas, b, colors, u)
            drawRibbon(state, colors, u)
            drawPlaten(state, b, u)
            // The paper guide: two chrome tabs either side of where the letters strike.
            for (dx in GuideTabs) {
                val left = (TypewriterDimens.StrikeX + dx) * u
                val top = (TypewriterDimens.StrikeY + 1f) * u
                with(shadow) {
                    drawSoftShadow(
                        left,
                        top + u,
                        left + GuideWidth * u,
                        top + (GuideHeight + 1f) * u,
                        u,
                        GuideShadow,
                    )
                }
                translate(left, top) {
                    drawRoundRect(
                        b.guide,
                        size = Size(GuideWidth * u, GuideHeight * u),
                        cornerRadius = CornerRadius(GuideCorner * u),
                    )
                }
            }
            drawTypebar(state, colors, u)
            drawLever(state, b, u)
            if (state.touchAlpha > 0f) {
                drawTouch(colors.touch, state.touch * u, state.touchAlpha)
            }
        }
    }

    private fun DrawScope.drawSheet(
        state: TypewriterState,
        atlas: GlyphAtlas,
        b: Brushes,
        colors: AppColors,
        u: Float,
    ) {
        val left = state.carriage * u
        val top = state.sheetTop * u
        val width = TypewriterDimens.SheetWidth * u
        val height = TypewriterDimens.SheetHeight * u
        // Hidden under the body below the platen: drawn no further down than the body's top.
        val shown = minOf(height, TypewriterDimens.BodyTop * u + u - top)
        if (shown <= 0f) return
        with(shadow) {
            drawSoftShadow(left, top + u, left + width, top + shown, 3f * u, SheetShadow)
            drawSoftShadow(left, top + 6f * u, left + width, top + shown, 10f * u, SheetFarShadow)
        }
        clipRect(left, top, left + width, top + shown) {
            translate(left, top) { drawRect(b.paper, size = Size(width, height)) }
            drawGrain(colors, left, top, u)
            drawGlyphs(atlas, state.sheet.glyphs, left, top, u) { state.landed(it) }
            // Letters the tape is lifting off: fading as they rise from the paper.
            for (lifted in state.lifting) {
                val t = ((state.now - lifted.at) / TypewriterDimens.TapeSeconds).coerceIn(0f, 1f)
                drawGlyph(
                    atlas,
                    lifted.glyph,
                    left,
                    top - TypewriterDimens.TapeLift * easeOut(t) * u,
                    u,
                    1f - easeOut(t),
                )
            }
            // The paper curves into the roller: a shade just over the platen.
            val roll = TypewriterDimens.PlatenTop * u
            translate(left, roll - RollerShadeHeight * u) {
                drawRect(b.roller, size = Size(width, RollerShadeHeight * u))
            }
        }
    }

    private fun DrawScope.drawGrain(colors: AppColors, left: Float, top: Float, u: Float) {
        grainPoints.clear()
        for (speck in grain) grainPoints += Offset(left + speck.x * u, top + speck.y * u)
        drawPoints(
            grainPoints,
            PointMode.Points,
            colors.typeInk.copy(alpha = GrainAlpha),
            strokeWidth = GrainSize * u,
            cap = StrokeCap.Round,
        )
    }

    private fun DrawScope.drawBody(
        b: Brushes,
        colors: AppColors,
        u: Float,
        origin: Offset,
        bottom: Float,
        badge: TextLayoutResult?,
        model: TextLayoutResult?,
    ) {
        val top = origin.y + TypewriterDimens.BodyTop * u
        val corner = TypewriterDimens.BodyCorner * u
        with(shadow) { drawSoftShadow(0f, top - 4f * u, size.width, bottom, 10f * u, BodyShadow) }
        bodyPath.reset()
        bodyPath.addRoundRect(
            RoundRect(
                Rect(0f, 0f, size.width, bottom - top + corner),
                topLeft = CornerRadius(corner),
                topRight = CornerRadius(corner),
            )
        )
        translate(0f, top) {
            drawPath(bodyPath, b.enamel)
            // Light catching the enamel's top edge.
            clipPath(bodyPath) {
                drawPath(bodyPath, Color.White.copy(alpha = EdgeLight), style = Stroke(3f * u))
            }
        }
        translate(origin.x, origin.y) {
            drawBasket(b, colors, u)
            // The badge on the left of the body: the model's name, and under it, smaller, what it
            // is, lettered in a colour that reads on the enamel.
            val at =
                Offset(
                    TypewriterDimens.BadgeLeft * u,
                    (TypewriterDimens.BodyTop + TypewriterDimens.BadgeTop) * u,
                )
            if (badge != null) drawText(badge, colors.typeBadge, at)
            if (model != null) {
                val below = if (badge != null) badge.size.height + u else 0f
                drawText(model, colors.typeBadge.copy(alpha = ModelAlpha), at + Offset(0f, below))
            }
        }
    }

    private fun DrawScope.drawBasket(b: Brushes, colors: AppColors, u: Float) {
        val left = TypewriterDimens.BasketLeft * u
        val top = (TypewriterDimens.BodyTop + TypewriterDimens.BasketTop) * u
        val width = TypewriterDimens.BasketWidth * u
        val height = TypewriterDimens.BasketHeight * u
        translate(left, top) {
            basketPath.reset()
            basketPath.addRoundRect(
                RoundRect(
                    Rect(0f, 0f, width, height),
                    topLeft = CornerRadius(width / 2f, height),
                    topRight = CornerRadius(width / 2f, height),
                    bottomLeft = CornerRadius(BasketFoot * u),
                    bottomRight = CornerRadius(BasketFoot * u),
                )
            )
            clipPath(basketPath) {
                drawRect(colors.typeRibbonBlack, size = Size(width, height))
                // The type bars, a fan of steel from the middle of the basket's foot.
                val hub = Offset(width / 2f, height)
                for (i in 0..BasketBars) {
                    val a = PI + PI * i / BasketBars
                    drawLine(
                        colors.typeChrome,
                        hub,
                        hub + Offset((cos(a) * width).toFloat(), (sin(a) * width).toFloat()),
                        strokeWidth = BasketBarWidth * u,
                    )
                }
                drawRect(b.basketShade, size = Size(width, height))
            }
            drawLine(
                Color.White.copy(alpha = EdgeLight),
                Offset(BasketFoot * u, height + 0.5f * u),
                Offset(width - BasketFoot * u, height + 0.5f * u),
                strokeWidth = u,
            )
        }
    }

    private fun DrawScope.drawRibbon(state: TypewriterState, colors: AppColors, u: Float) {
        val p = (state.now - state.ribbonAt) / TypewriterDimens.RibbonSeconds
        if (p < 0f || p >= 1f) return
        val h = rise(p, RibbonRise)
        val top =
            lerp(
                TypewriterDimens.PlatenTop + RibbonRest,
                TypewriterDimens.StrikeY - TypewriterDimens.RibbonLift,
                h,
            )
        val left = (TypewriterDimens.StrikeX - TypewriterDimens.RibbonWidth / 2f) * u
        val y = top * u
        val w = TypewriterDimens.RibbonWidth * u
        val half = TypewriterDimens.RibbonHeight / 2f * u
        // Black over red; the correction tape's white in place of the red as it lifts a letter.
        drawRect(colors.typeRibbonBlack, Offset(left, y), Size(w, half))
        drawRect(
            if (state.taping) colors.typePaper else colors.typeRibbonRed,
            Offset(left, y + half),
            Size(w, half),
        )
    }

    private fun DrawScope.drawPlaten(state: TypewriterState, b: Brushes, u: Float) {
        val left = (state.carriage - TypewriterDimens.PlatenOverhang) * u
        val top = TypewriterDimens.PlatenTop * u
        val width = (TypewriterDimens.SheetWidth + TypewriterDimens.PlatenOverhang * 2f) * u
        val height = TypewriterDimens.PlatenHeight * u
        with(shadow) {
            drawSoftShadow(
                left,
                top + 3f * u,
                left + width,
                top + height + 3f * u,
                5f * u,
                PlatenShadow,
            )
        }
        // The roll's knobs at either end, ridged, turning with the paper.
        val roll = (state.feed + state.shown - state.feedIn) * KnobTurn
        val knobTop = top - TypewriterDimens.KnobRise * u
        drawKnob(b, left - TypewriterDimens.KnobWidth * u, knobTop, u, roll)
        drawKnob(b, left + width, knobTop, u, roll)
        translate(left, top) {
            drawRoundRect(
                b.platen,
                size = Size(width, height),
                cornerRadius = CornerRadius(TypewriterDimens.PlatenCorner * u),
            )
        }
    }

    private fun DrawScope.drawKnob(b: Brushes, left: Float, top: Float, u: Float, roll: Float) {
        val width = TypewriterDimens.KnobWidth * u
        val height = TypewriterDimens.KnobHeight * u
        with(shadow) {
            drawSoftShadow(left, top + 2f * u, left + width, top + height + 2f * u, 4f * u, 0.4f)
        }
        translate(left, top) {
            knobPath.reset()
            knobPath.addRoundRect(
                RoundRect(Rect(0f, 0f, width, height), CornerRadius(KnobCorner * u))
            )
            clipPath(knobPath) {
                drawRect(b.colors.typePlaten, size = Size(width, height))
                // Ridges every few units, rolling round as the paper moves.
                val shift = roll - floor(roll / KnobPitch) * KnobPitch
                var y = -KnobPitch + shift
                while (y < TypewriterDimens.KnobHeight) {
                    drawRect(b.ridge, Offset(0f, y * u), Size(width, KnobRidge * u))
                    y += KnobPitch
                }
                drawRect(b.knobShade, size = Size(width, height))
            }
        }
    }

    private fun DrawScope.drawTypebar(state: TypewriterState, colors: AppColors, u: Float) {
        val p = (state.now - state.barAt) / TypewriterDimens.TypebarSeconds
        if (p < 0f || p >= 1f) return
        val from = state.barFrom
        val px = TypewriterDimens.StrikeX - BarSpread + BarSpread * 2f * from
        val py = BarRest - BarArch * sin(PI.toFloat() * from)
        val h = rise(p, BarRise)
        val tx = lerp(px + (TypewriterDimens.StrikeX - px) * BarLean, TypewriterDimens.StrikeX, h)
        val ty = lerp(py - BarArch, TypewriterDimens.StrikeY - BarFace, h)
        val start = Offset(px * u, py * u)
        val end = Offset(tx * u, ty * u)
        drawLine(colors.typeChrome, start, end, strokeWidth = BarWidth * u, cap = StrokeCap.Round)
        val glint = Offset(0f, BarGlint * u)
        drawLine(
            colors.typeChromeSheen,
            start - glint,
            end - glint,
            strokeWidth = BarGlintWidth * u,
            cap = StrokeCap.Round,
        )
    }

    private fun DrawScope.drawLever(state: TypewriterState, b: Brushes, u: Float) {
        val pivot =
            Offset(
                (state.carriage - TypewriterDimens.LeverOffset) * u,
                TypewriterDimens.LeverPivotY * u,
            )
        val length = TypewriterDimens.LeverLength * u
        val angle = TypewriterDimens.LeverAngle + TypewriterDimens.LeverSwing * state.leverPush
        translate(pivot.x, pivot.y) {
            rotate(angle, Offset.Zero) {
                with(shadow) { drawSoftShadow(0f, -u, length, 4f * u, 3f * u, LeverShadow) }
                drawRoundRect(
                    b.arm,
                    Offset(0f, -LeverWidth / 2f * u),
                    Size(length, LeverWidth * u),
                    CornerRadius(LeverWidth / 2f * u),
                )
                // The paddle on its end, a black knob for the thumb.
                translate(
                    (TypewriterDimens.LeverLength - PaddleWidth / 2f) * u,
                    -PaddleHeight / 2f * u,
                ) {
                    drawRoundRect(
                        b.paddle,
                        size = Size(PaddleWidth * u, PaddleHeight * u),
                        cornerRadius = CornerRadius(PaddleHeight / 2f * u),
                    )
                }
            }
            drawCircle(b.pivot, radius = PivotRadius * u, center = Offset.Zero)
        }
    }

    // The gradients, each from its own part's top left, for one size of unit and one palette.
    private class Brushes(val u: Float, val colors: AppColors) {
        val paper =
            Brush.horizontalGradient(
                0f to colors.typePaperEdge,
                PaperEdgeShare to colors.typePaper,
                1f - PaperEdgeShare to colors.typePaper,
                1f to colors.typePaperEdge,
                startX = 0f,
                endX = TypewriterDimens.SheetWidth * u,
            )
        val roller =
            Brush.verticalGradient(
                listOf(Color.Transparent, colors.typePlaten.copy(alpha = RollerShade)),
                startY = 0f,
                endY = RollerShadeHeight * u,
            )
        val enamel =
            Brush.verticalGradient(
                0f to colors.typeEnamelLight,
                EnamelLitShare to colors.typeEnamel,
                1f to colors.typeEnamelDeep,
                startY = 0f,
                endY = TypewriterDimens.BodyHeight * u,
            )
        val basketShade =
            Brush.verticalGradient(
                listOf(
                    colors.typePlaten.copy(alpha = BasketShadeTop),
                    colors.typePlaten.copy(alpha = BasketShadeFoot),
                ),
                startY = 0f,
                endY = TypewriterDimens.BasketHeight * u,
            )
        val platen =
            Brush.verticalGradient(
                0f to colors.typePlatenSheen,
                0.22f to lerp(colors.typePlatenSheen, colors.typePlaten, 0.7f),
                0.62f to colors.typePlaten,
                1f to lerp(colors.typePlaten, colors.typePlatenSheen, 0.45f),
                startY = 0f,
                endY = TypewriterDimens.PlatenHeight * u,
            )
        val ridge = lerp(colors.typePlaten, colors.typePlatenSheen, 0.45f)
        val knobShade =
            Brush.horizontalGradient(
                0f to Color.White.copy(alpha = 0.18f),
                0.4f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.35f),
                startX = 0f,
                endX = TypewriterDimens.KnobWidth * u,
            )
        val guide =
            Brush.horizontalGradient(
                listOf(colors.typeChromeSheen, colors.typeChrome),
                startX = 0f,
                endX = GuideWidth * u,
            )
        val arm =
            Brush.verticalGradient(
                0f to colors.typeChromeSheen,
                0.6f to colors.typeChrome,
                1f to lerp(colors.typeChrome, colors.typePlaten, 0.3f),
                startY = -LeverWidth / 2f * u,
                endY = LeverWidth / 2f * u,
            )
        val paddle =
            Brush.radialGradient(
                listOf(lerp(colors.typePlatenSheen, colors.typePlaten, 0.3f), colors.typePlaten),
                center = Offset(PaddleWidth * 0.4f * u, PaddleHeight * 0.3f * u),
                radius = PaddleWidth * 0.6f * u,
            )
        val pivot =
            Brush.radialGradient(
                listOf(
                    Color.White,
                    colors.typeChrome,
                    lerp(colors.typeChrome, colors.typePlaten, 0.4f),
                ),
                center = Offset(-2f * u, -2f * u),
                radius = (PivotRadius + 1f) * u,
            )
    }

    private companion object {
        const val GrainSpecks = 160
        const val GrainAlpha = 0.07f
        const val GrainSize = 0.8f
        const val RollerShade = 0.16f
        const val RollerShadeHeight = 9f
        const val PaperEdgeShare = 0.06f
        const val EnamelLitShare = 0.22f
        const val EdgeLight = 0.35f
        const val ModelAlpha = 0.8f

        // How dark the shadows are under the sheet, near and far, the body, the platen, a guide
        // tab and the lever.
        const val SheetShadow = 0.2f
        const val SheetFarShadow = 0.12f
        const val BodyShadow = 0.18f
        const val PlatenShadow = 0.35f
        const val GuideShadow = 0.3f
        const val LeverShadow = 0.3f

        const val BasketBars = 30
        const val BasketBarWidth = 1.6f
        const val BasketFoot = 4f
        const val BasketShadeTop = 0.78f
        const val BasketShadeFoot = 0.2f
        const val KnobTurn = 0.9f
        const val KnobPitch = 4f
        const val KnobRidge = 2f
        const val KnobCorner = 8f

        // The guide's tabs, the lever's arm, its paddle and its pivot, in units.
        const val GuideWidth = 5f
        const val GuideHeight = 12f
        const val GuideCorner = 1.5f
        const val LeverWidth = 5f
        const val PaddleWidth = 20f
        const val PaddleHeight = 14f
        const val PivotRadius = 6f

        // A type bar rises from the basket's rim: this far either side of the strike for the
        // keys at its ends, from this far down the machine, the rim arching this high. It leans
        // this share of the way in as it starts, and stops this far over the line, its face on
        // the paper. It is up at the paper this share of the way through a strike.
        const val BarSpread = 72f
        const val BarRest = 86f
        const val BarArch = 10f
        const val BarLean = 0.2f
        const val BarFace = 6f
        const val BarWidth = 3f
        const val BarGlint = 0.6f
        const val BarGlintWidth = 1.2f
        const val BarRise = 0.35f

        // The ribbon rests this far under the platen's top, and is up this share of the way.
        const val RibbonRest = 4f
        const val RibbonRise = 0.25f

        val GuideTabs = listOf(-15f, 10f)

        // Up quickly over the first [share] of [p], then back down more slowly.
        fun rise(p: Float, share: Float): Float =
            if (p < share) easeOut(p / share) else 1f - ease((p - share) / (1f - share))
    }
}

/**
 * Draws [glyphs] as typed on a sheet whose top left is at [left], [top], [u] px to the unit, each
 * letter in its place as it was struck, those [shown] says.
 */
internal inline fun DrawScope.drawGlyphs(
    atlas: GlyphAtlas,
    glyphs: List<Glyph>,
    left: Float,
    top: Float,
    u: Float,
    shown: (Glyph) -> Boolean = { true },
) {
    for (i in glyphs.indices) {
        val glyph = glyphs[i]
        if (shown(glyph)) drawGlyph(atlas, glyph, left, top, u, 1f)
    }
}

/** Draws one letter, [glyph], on a sheet whose top left is at [left], [top], at [alpha]. */
internal fun DrawScope.drawGlyph(
    atlas: GlyphAtlas,
    glyph: Glyph,
    left: Float,
    top: Float,
    u: Float,
    alpha: Float,
) {
    val strike = glyph.strike
    val across = TypewriterDimens.Margin + (glyph.column + 0.5f) * TypewriterDimens.CharWidth
    val down =
        TypewriterDimens.TopMargin +
            glyph.line * TypewriterDimens.LineHeight +
            TypewriterDimens.GlyphBaseline
    with(atlas) {
        drawGlyph(
            glyph.char,
            left + (across + strike.dx) * u,
            top + (down + strike.dy) * u,
            strike.tilt,
            strike.ink * alpha,
        )
    }
}
