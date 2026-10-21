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
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.presentation.components.magnet.SoftShadow
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Draws the typewriter's moving parts over its [MachineShell]: the sheet, and the carriage that
 * carries it, the rubber platen with its chrome knobs, the paper bail with its scale and rollers,
 * and the return lever; the ribbon running from its two spools through the vibrator at the printing
 * point; a type bar swinging up from the basket; and the bell's hammer. Everything is in machine
 * units, a given number of px each, from the machine's top left. Gradients are made once for a size
 * of unit and a palette, each from its own part's top left, and the part is moved under them.
 */
internal class TypewriterPainter {
    private val shadow = SoftShadow()
    private val knobPath = Path()

    // The paper's grain: specks scattered over a sheet, in units from its top left, made once.
    private val grain: List<Offset> =
        List(GrainSpecks) { i ->
            Offset(
                noise(i, 11) * TypewriterDimens.SheetWidth,
                noise(i, 12) * TypewriterDimens.SheetHeight,
            )
        }
    // The grain in px from a sheet's top left, for the size of unit it was made for.
    private var grainPoints: List<Offset> = emptyList()
    private var grainUnit = 0f

    private var brushes: Brushes? = null

    private fun brushes(u: Float, colors: AppColors): Brushes =
        brushes?.takeIf { it.u == u && it.colors == colors }
            ?: Brushes(u, colors).also { brushes = it }

    /** Draws the moving parts, [unit] px to the unit, the machine's top left at [origin]. */
    fun DrawScope.drawMachine(
        state: TypewriterState,
        atlas: GlyphAtlas,
        colors: AppColors,
        unit: Float,
        origin: Offset,
    ) {
        state.frame
        val u = unit
        val b = brushes(u, colors)
        translate(origin.x, origin.y) {
            drawSpools(state, b, colors, u)
            drawHammer(state, colors, u)
            drawSheet(state, atlas, b, colors, u)
            drawPlaten(state, b, u)
            drawRibbon(state, colors, u)
            drawTypebar(state, colors, u)
            drawBail(state, b, colors, u)
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
        // It goes round the platen: drawn no further down than the platen's middle.
        val shown =
            minOf(
                height,
                (TypewriterDimens.PlatenTop + TypewriterDimens.PlatenHeight / 2f) * u - top,
            )
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
            // The paper curves over the roller: a shade just over the platen.
            val roll = TypewriterDimens.PlatenTop * u
            translate(left, roll - RollerShadeHeight * u) {
                drawRect(b.roller, size = Size(width, RollerShadeHeight * u))
            }
        }
    }

    private fun DrawScope.drawGrain(colors: AppColors, left: Float, top: Float, u: Float) {
        if (grainUnit != u) {
            grainPoints = grain.map { it * u }
            grainUnit = u
        }
        translate(left, top) {
            drawPoints(
                grainPoints,
                PointMode.Points,
                colors.typeInk.copy(alpha = GrainAlpha),
                strokeWidth = GrainSize * u,
                cap = StrokeCap.Round,
            )
        }
    }

    // The platen: a rubber roller, lit along its top and its belly, with a chrome knob at each
    // end, its knurled grip turning with the paper and its end face catching the light.
    private fun DrawScope.drawPlaten(state: TypewriterState, b: Brushes, u: Float) {
        val left = (state.carriage - TypewriterDimens.PlatenOverhang) * u
        val top = TypewriterDimens.PlatenTop * u
        val width = (TypewriterDimens.SheetWidth + TypewriterDimens.PlatenOverhang * 2f) * u
        val height = TypewriterDimens.PlatenHeight * u
        // Its shadow on the deck in front of it.
        with(shadow) {
            drawSoftShadow(
                left,
                top + height * 0.6f,
                left + width,
                top + height + 4f * u,
                6f * u,
                PlatenShadow,
            )
        }
        translate(left, top) {
            drawRoundRect(
                b.platen,
                size = Size(width, height),
                cornerRadius = CornerRadius(height / 2f, height / 5f),
            )
            drawRect(b.platenGloss, Offset(0f, height * 0.12f), Size(width, height * 0.1f))
        }
        val roll = (state.feed + state.shown - state.feedIn) * KnobTurn
        val knobTop = top - TypewriterDimens.KnobRise * u
        drawKnob(b, left - TypewriterDimens.KnobWidth * u, knobTop, u, roll, endOnLeft = true)
        drawKnob(b, left + width, knobTop, u, roll, endOnLeft = false)
    }

    private fun DrawScope.drawKnob(
        b: Brushes,
        left: Float,
        top: Float,
        u: Float,
        roll: Float,
        endOnLeft: Boolean,
    ) {
        val width = TypewriterDimens.KnobWidth * u
        val height = TypewriterDimens.KnobHeight * u
        with(shadow) {
            drawSoftShadow(
                left,
                top + 3f * u,
                left + width,
                top + height + 3f * u,
                4f * u,
                KnobShadow,
            )
        }
        translate(left, top) {
            knobPath.reset()
            knobPath.addRoundRect(
                RoundRect(Rect(0f, 0f, width, height), CornerRadius(width * 0.3f)),
            )
            clipPath(knobPath) {
                drawRect(b.knob, size = Size(width, height))
                // The knurling, fine ridges round the grip, rolling with the paper.
                val shift = roll - floor(roll / KnobPitch) * KnobPitch
                var y = -KnobPitch + shift
                while (y < TypewriterDimens.KnobHeight) {
                    drawRect(b.knurl, Offset(0f, y * u), Size(width, KnobRidge * u))
                    y += KnobPitch
                }
                // Its outer end, a domed chrome face seen a little from the side.
                val face = width * KnobFace
                val x = if (endOnLeft) 0f else width - face
                drawOval(b.knobEnd, Offset(x, 0f), Size(face, height))
            }
        }
    }

    // The paper bail across the sheet over the line: a chrome rod with its scale, holding the
    // paper down with two small rubber rollers.
    private fun DrawScope.drawBail(
        state: TypewriterState,
        b: Brushes,
        colors: AppColors,
        u: Float,
    ) {
        val left = (state.carriage - BailReach) * u
        val width = (TypewriterDimens.SheetWidth + BailReach * 2f) * u
        val top = TypewriterDimens.BailY * u
        val height = TypewriterDimens.BailHeight * u
        with(shadow) {
            drawSoftShadow(
                left,
                top + u,
                left + width,
                top + height + 1.5f * u,
                1.5f * u,
                BailShadow,
            )
        }
        translate(left, top) {
            drawRoundRect(
                b.bail,
                size = Size(width, height),
                cornerRadius = CornerRadius(height / 2f),
            )
        }
        // The scale: a tick at each letter, longer at each fifth.
        val first = (state.carriage + TypewriterDimens.Margin) * u
        for (i in 0..TypewriterDimens.Columns) {
            val x = first + i * TypewriterDimens.CharWidth * u
            val long = i % 5 == 0
            drawLine(
                colors.typePlaten.copy(alpha = TickAlpha),
                Offset(x, top + height * (if (long) 0.15f else 0.45f)),
                Offset(x, top + height * 0.9f),
                strokeWidth = 0.5f * u,
            )
        }
        for (share in RollerAt) {
            val x = (state.carriage + TypewriterDimens.SheetWidth * share) * u
            val w = RollerWidth * u
            val h = RollerHeight * u
            with(shadow) { drawSoftShadow(x - w / 2f, top + u, x + w / 2f, top + h + u, u, 0.4f) }
            translate(x - w / 2f, top - (h - height) / 2f) {
                drawRoundRect(b.roller2, size = Size(w, h), cornerRadius = CornerRadius(h / 2f))
            }
        }
    }

    // The ribbon spools either side of the basket: chrome cups, the ribbon wound in them, and a
    // hub that turns on a little at each letter; the ribbon runs from each to the vibrator.
    private fun DrawScope.drawSpools(
        state: TypewriterState,
        b: Brushes,
        colors: AppColors,
        u: Float,
    ) {
        val r = TypewriterDimens.SpoolRadius * u
        val y = TypewriterDimens.SpoolY * u
        val turn = state.struck * SpoolTurn
        for (side in listOf(-1f, 1f)) {
            val c = Offset((TypewriterDimens.StrikeX + side * TypewriterDimens.SpoolX) * u, y)
            with(shadow) {
                drawSoftShadow(
                    c.x - r,
                    c.y - r * 0.6f,
                    c.x + r,
                    c.y + r * 1.15f,
                    3f * u,
                    SpoolShadow,
                )
            }
            translate(c.x - r, c.y - r) {
                drawCircle(b.spoolCup, r, Offset(r, r))
            }
            // The ribbon wound on it, more on the one it winds onto, its turns showing.
            val wound = r * (0.72f + 0.04f * side)
            drawCircle(colors.typeRibbonBlack, wound, c)
            for (ring in 1..3) {
                drawCircle(
                    Color.White.copy(alpha = 0.06f),
                    wound * (0.5f + ring * 0.15f),
                    c,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(0.5f * u),
                )
            }
            rotate(turn * side, c) {
                for (spoke in 0 until 3) {
                    val a = spoke * 2.0944f
                    drawLine(
                        colors.typeChrome,
                        c,
                        c + Offset(cos(a) * r * 0.42f, sin(a) * r * 0.42f),
                        strokeWidth = 1.6f * u,
                        cap = StrokeCap.Round,
                    )
                }
            }
            drawCircle(b.hub, r * 0.2f, c)
        }
    }

    // The ribbon: from each spool to the vibrator at the printing point, which lifts it to the
    // line as a bar strikes, its red half, or the tape's white, where the type will hit.
    private fun DrawScope.drawRibbon(state: TypewriterState, colors: AppColors, u: Float) {
        val p = (state.now - state.ribbonAt) / TypewriterDimens.RibbonSeconds
        val h = if (p < 0f || p >= 1f) 0f else rise(p, RibbonRise)
        val restY = TypewriterDimens.VibratorY
        val liftY = TypewriterDimens.StrikeY - TypewriterDimens.RibbonLift
        val top = lerp(restY, liftY, h)
        val w = TypewriterDimens.RibbonWidth * u
        val half = TypewriterDimens.RibbonHeight / 2f * u
        val x = TypewriterDimens.StrikeX * u
        val y = top * u
        // The ribbon's two runs, low across the deck to the spools.
        for (side in listOf(-1f, 1f)) {
            val spool =
                Offset(
                    (TypewriterDimens.StrikeX +
                        side * (TypewriterDimens.SpoolX - TypewriterDimens.SpoolRadius * 0.55f)) *
                        u,
                    (TypewriterDimens.SpoolY - TypewriterDimens.SpoolRadius * 0.55f) * u,
                )
            // Held low by the guides either side of the vibrator, which alone lifts it.
            val end = Offset(x + side * (w / 2f + RibbonGuide * u), restY * u + half)
            drawLine(colors.typeRibbonBlack, spool, end, strokeWidth = half * 0.9f)
            drawLine(
                colors.typeRibbonRed,
                spool + Offset(0f, half * 0.45f),
                end + Offset(0f, half * 0.45f),
                strokeWidth = half * 0.45f,
            )
            drawLine(
                colors.typeRibbonBlack,
                end,
                Offset(x + side * w / 2f, y + half),
                strokeWidth = half * 0.9f,
            )
        }
        // Lifted, it shows over the platen; at rest it lies under the line, hidden by the platen
        // but for its runs.
        if (h > 0f) {
            drawRect(colors.typeRibbonBlack, Offset(x - w / 2f, y), Size(w, half))
            drawRect(
                if (state.taping) colors.typePaper else colors.typeRibbonRed,
                Offset(x - w / 2f, y + half),
                Size(w, half),
            )
        }
        // The vibrator's chrome fork holding it.
        drawLine(
            colors.typeChrome,
            Offset(x - w / 2f - u, y - u),
            Offset(x - w / 2f - u, y + half * 2f + u),
            strokeWidth = 1.2f * u,
        )
        drawLine(
            colors.typeChrome,
            Offset(x + w / 2f + u, y - u),
            Offset(x + w / 2f + u, y + half * 2f + u),
            strokeWidth = 1.2f * u,
        )
    }

    // The struck type bar: up from its place in the basket, over to the printing point, and back,
    // its type face foremost, nearer and so a touch thicker as it rises.
    private fun DrawScope.drawTypebar(state: TypewriterState, colors: AppColors, u: Float) {
        val p = (state.now - state.barAt) / TypewriterDimens.TypebarSeconds
        if (p < 0f || p >= 1f) return
        val h = rise(p, BarRise)
        val a = basketAngle(state.barFrom)
        val cx = TypewriterDimens.StrikeX
        val cy = TypewriterDimens.BasketY
        val pivot =
            Offset(
                (cx + cos(a) * TypewriterDimens.BasketInner) * u,
                (cy + sin(a) * TypewriterDimens.BasketInner) * u,
            )
        val rest =
            Offset(
                (cx + cos(a) * TypewriterDimens.BasketOuter) * u,
                (cy + sin(a) * TypewriterDimens.BasketOuter) * u,
            )
        val strike = Offset(cx * u, (TypewriterDimens.StrikeY - BarFace) * u)
        val head = Offset(lerp(rest.x, strike.x, h), lerp(rest.y, strike.y, h))
        val width = lerp(1.4f, BarWidth, h) * u
        drawLine(colors.typeChrome, pivot, head, strokeWidth = width, cap = StrokeCap.Round)
        drawLine(
            colors.typeChromeSheen,
            pivot,
            head,
            strokeWidth = width * 0.4f,
            cap = StrokeCap.Round,
        )
        drawCircle(colors.typePlaten, width * 0.9f, head)
    }

    // The bell's hammer, a little chrome arm beside the dome that swings in and taps it.
    private fun DrawScope.drawHammer(state: TypewriterState, colors: AppColors, u: Float) {
        val pivot =
            Offset((TypewriterDimens.BellX + HammerReach) * u, (TypewriterDimens.BellY + 3f) * u)
        val angle = HammerRest - (HammerRest - HammerHit) * state.hammer
        rotate(angle, pivot) {
            drawLine(
                colors.typeChrome,
                pivot,
                pivot - Offset(HammerReach * u, 0f),
                strokeWidth = 1.4f * u,
                cap = StrokeCap.Round,
            )
            drawCircle(colors.typeChromeSheen, 1.6f * u, pivot - Offset(HammerReach * u, 0f))
        }
        drawCircle(colors.typeChrome, 1.2f * u, pivot)
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
        private val chromeDark = lerp(colors.typeChrome, colors.typePlaten, 0.45f)
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
        // Rubber: dark, a soft sheen along its top, its belly in shadow, a little light bounced
        // up from the deck under it.
        val platen =
            Brush.verticalGradient(
                0f to lerp(colors.typePlatenSheen, colors.typePlaten, 0.35f),
                0.2f to colors.typePlatenSheen,
                0.42f to lerp(colors.typePlatenSheen, colors.typePlaten, 0.75f),
                0.78f to colors.typePlaten,
                1f to lerp(colors.typePlaten, colors.typeEnamelDeep, 0.35f),
                startY = 0f,
                endY = TypewriterDimens.PlatenHeight * u,
            )
        val platenGloss =
            Brush.verticalGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                startY = TypewriterDimens.PlatenHeight * 0.12f * u,
                endY = TypewriterDimens.PlatenHeight * 0.22f * u,
            )
        val knob =
            Brush.verticalGradient(
                0f to chromeDark,
                0.18f to colors.typeChromeSheen,
                0.32f to Color.White,
                0.5f to colors.typeChrome,
                0.8f to chromeDark,
                1f to colors.typeChrome,
                startY = 0f,
                endY = TypewriterDimens.KnobHeight * u,
            )
        val knurl = colors.typePlaten.copy(alpha = 0.28f)
        val knobEnd =
            Brush.radialGradient(
                listOf(Color.White, colors.typeChromeSheen, colors.typeChrome, chromeDark),
                center =
                    Offset(
                        TypewriterDimens.KnobWidth * KnobFace * 0.4f * u,
                        TypewriterDimens.KnobHeight * 0.3f * u,
                    ),
                radius = TypewriterDimens.KnobHeight * 0.6f * u,
            )
        val bail =
            Brush.verticalGradient(
                0f to colors.typeChromeSheen,
                0.35f to Color.White,
                0.6f to colors.typeChrome,
                1f to chromeDark,
                startY = 0f,
                endY = TypewriterDimens.BailHeight * u,
            )
        val roller2 =
            Brush.verticalGradient(
                listOf(colors.typePlatenSheen, colors.typePlaten),
                startY = 0f,
                endY = RollerHeight * u,
            )
        val spoolCup =
            Brush.radialGradient(
                listOf(Color.White, colors.typeChromeSheen, colors.typeChrome, chromeDark),
                center =
                    Offset(
                        TypewriterDimens.SpoolRadius * 0.7f * u,
                        TypewriterDimens.SpoolRadius * 0.55f * u,
                    ),
                radius = TypewriterDimens.SpoolRadius * 1.5f * u,
            )
        val hub =
            Brush.radialGradient(
                listOf(Color.White, colors.typeChrome),
                radius = TypewriterDimens.SpoolRadius * 0.3f * u,
            )
        val arm =
            Brush.verticalGradient(
                0f to colors.typeChromeSheen,
                0.6f to colors.typeChrome,
                1f to chromeDark,
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
                listOf(Color.White, colors.typeChrome, chromeDark),
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

        // How dark the shadows are under the sheet, near and far, the platen, a knob, the bail,
        // a spool and the lever.
        const val SheetShadow = 0.2f
        const val SheetFarShadow = 0.12f
        const val PlatenShadow = 0.55f
        const val KnobShadow = 0.45f
        const val BailShadow = 0.35f
        const val SpoolShadow = 0.45f
        const val LeverShadow = 0.3f

        // The knobs' knurling, its pitch and ridge, how fast it turns with the paper, and how much
        // of a knob its end face shows.
        const val KnobTurn = 0.9f
        const val KnobPitch = 3f
        const val KnobRidge = 1f
        const val KnobFace = 0.42f

        // The bail reaches this far past the sheet either side; its rollers sit at these shares
        // of the sheet, this big; its ticks are this dark.
        const val BailReach = 8f
        val RollerAt = listOf(0.22f, 0.78f)
        const val RollerWidth = 10f
        const val RollerHeight = 6f
        const val TickAlpha = 0.55f

        // A spool turns on this many degrees a letter.
        const val SpoolTurn = 9f

        // The bell's hammer: this long, at rest this far round, and on the dome this far.
        const val HammerReach = 9f
        const val HammerRest = -35f
        const val HammerHit = 0f

        // The lever's arm, its paddle and its pivot, in units.
        const val LeverWidth = 5f
        const val PaddleWidth = 20f
        const val PaddleHeight = 14f
        const val PivotRadius = 6f

        // A type bar is up at the paper this share of the way through a strike, stops this far
        // over the line, and is this thick there.
        const val BarFace = 2f
        const val BarWidth = 3.2f
        const val BarRise = 0.35f

        // The ribbon's guides stand this far out from the vibrator.
        const val RibbonGuide = 6f

        // The ribbon is up this share of the way.
        const val RibbonRise = 0.25f

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
