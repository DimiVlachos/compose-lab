package dev.dimvlachos.lab.core.presentation.components.sandmandala

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.dimvlachos.lab.core.presentation.canvas2d.PixelSurface
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaState.Companion.Front
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaState.Companion.Lip
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaState.Companion.Pa
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaState.Companion.ease
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaState.Companion.ease3
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaState.Companion.smooth
import dev.dimvlachos.lab.core.presentation.ui.SandMandalaScreenColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The rows' laid-out words: each visit's title and its site and time line, and each section's
 * heading, measured once for the list's width. Index by visit, or by entry for [sections].
 */
internal class SandRowText(
    val titles: List<TextLayoutResult>,
    val subs: List<TextLayoutResult>,
    val letters: List<TextLayoutResult>,
    val sections: Map<Int, TextLayoutResult>,
)

/**
 * Draws the History list as the prototype does: the rows and their headings, which crumble behind a
 * soft front while the sand gathers, the chalk outline under the mandala, and every grain, written
 * a pixel at a time into a [PixelSurface] at the screen's full density, as its render() writes an
 * ImageData: a shadow pass, then each grain a tiny block lit from the top left.
 */
internal class SandMandalaPainter {
    private var surface: PixelSurface? = null
    private var lit = FloatArray(0)
    private var gs = 2
    private var drawnFrame = -1
    private var drawnSurface: PixelSurface? = null

    // Each grain's place, lift and colour this frame, kept between frames.
    private var px = FloatArray(0)
    private var py = FloatArray(0)
    private var pl = FloatArray(0)
    private var pc = IntArray(0)
    private var pv = BooleanArray(0)

    /**
     * Title and site line: where each sits in its row (the prototype's 14.5 px at 1.3, 12 at 1.35).
     */
    fun titleTop(state: SandMandalaState, visit: Int) =
        state.rowTop(visit) + (state.rowH - (TitleLine + SubLine)) / 2f

    fun subTop(state: SandMandalaState, visit: Int) = titleTop(state, visit) + TitleLine

    /** The rows: favicons, words and headings, crumbling behind the gather's front. */
    fun DrawScope.drawRows(
        state: SandMandalaState,
        text: SandRowText,
        colors: SandMandalaScreenColors,
        sitePigments: List<Color>,
    ) {
        if (!state.rowsShown) return
        val d = density
        val gathering = state.ph == SandPhase.Gather
        val t = state.t
        var visit = 0
        for (e in state.entries.indices) {
            when (val entry = state.entries[e]) {
                is SandEntry.Section -> {
                    val layout = text.sections[e] ?: continue
                    // A heading fades with its first row's front.
                    val alpha =
                        if (gathering) {
                            val t0 = state.frontT0.getOrElse(visit) { 0f }
                            1f - smooth(t0, t0 + Front * .6f, t)
                        } else 1f
                    if (alpha <= 0f) continue
                    drawText(
                        layout,
                        topLeft = Offset(20f * d, (state.entryTop[e] + 10f) * d),
                        alpha = alpha,
                    )
                }
                is SandEntry.Visit -> {
                    val i = visit++
                    val top = state.rowTop(i)
                    if (top > state.height) continue
                    val fy = state.favY(i)
                    drawRoundRect(
                        sitePigments[entry.visit.site],
                        topLeft = Offset(20f * d, fy * d),
                        size = Size(30f * d, 30f * d),
                        cornerRadius = CornerRadius(8f * d),
                    )
                    val letter = text.letters[i]
                    drawText(
                        letter,
                        topLeft =
                            Offset(
                                (35f * d - letter.size.width / 2f),
                                (fy + 15f) * d - letter.size.height / 2f,
                            ),
                    )
                    drawText(text.titles[i], topLeft = Offset(TextX * d, titleTop(state, i) * d))
                    drawText(text.subs[i], topLeft = Offset(TextX * d, subTop(state, i) * d))
                    if (gathering) {
                        // The crumbling front: the list's own paper drawn over what has gone, then
                        // a 30 dp fade, as the prototype's mask. The fade is one brush, kept, and
                        // moved along the row.
                        val p = ((t - state.frontT0[i]) / Front).coerceIn(0f, 1f)
                        if (p > 0f) {
                            val a = p * (state.width + 30f) - 30f
                            if (a > 0f)
                                drawRect(
                                    colors.background,
                                    topLeft = Offset(0f, top * d),
                                    size = Size(a * d, state.rowH * d),
                                )
                            val fade = fadeBrush(colors.background, d)
                            clipRect(0f, top * d, size.width, (top + state.rowH) * d) {
                                translate(left = a * d, top = top * d) {
                                    drawRect(fade, size = Size(30f * d, state.rowH * d))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** The chalk outline the monks draw before they pour any sand. */
    fun DrawScope.drawChalk(state: SandMandalaState, colors: SandMandalaScreenColors) {
        val ch = chalkAlpha(state)
        if (ch <= 0f) return
        val d = density
        val color = colors.chalk.copy(alpha = colors.chalk.alpha * ch)
        val stroke = chalkStroke(d)
        val c = Offset(state.cx * d, state.cy * d)
        val r = state.r * d
        for (k in ChalkRings) drawCircle(color, r * k, c, style = stroke)
        for (k in ChalkSquares) drawRect(
            color,
            Offset(c.x - r * k, c.y - r * k),
            Size(r * k * 2, r * k * 2),
            style = stroke,
        )
        val k = .38f * r
        drawLine(color, Offset(c.x - k, c.y - k), Offset(c.x + k, c.y + k), .8f * d)
        drawLine(color, Offset(c.x + k, c.y - k), Offset(c.x - k, c.y + k), .8f * d)
    }

    private var fade: Brush? = null
    private var fadeColor = Color.Unspecified
    private var fadeDensity = 0f

    // The crumbling front's 30 dp fade from the list's paper to nothing, made once per colour.
    private fun fadeBrush(paper: Color, d: Float): Brush {
        val f = fade
        if (f != null && fadeColor == paper && fadeDensity == d) return f
        val made =
            Brush.horizontalGradient(
                0f to paper,
                1f to paper.copy(alpha = 0f),
                startX = 0f,
                endX = 30f * d,
            )
        fade = made
        fadeColor = paper
        fadeDensity = d
        return made
    }

    private var stroke: Stroke? = null

    private fun chalkStroke(d: Float): Stroke {
        val s = stroke
        if (s != null && s.width == .8f * d) return s
        return Stroke(.8f * d).also { stroke = it }
    }

    /** The grains, rendered when the state has moved since the last frame. */
    fun DrawScope.drawGrains(state: SandMandalaState, colors: SandMandalaScreenColors) {
        if (state.n == 0) return
        // At rest in the rows, or swept away, there is no sand to draw.
        if (state.ph == null && (state.at == SandAt.Rows || state.at == SandAt.Cleared)) return
        // The prototype's scale: its canvas caps the device pixel ratio at 2, and the browser shows
        // it scaled up. Every pixel is cleared, written and uploaded each frame, so a 3× screen at
        // full density would move 2.25 times as many; drawn unfiltered, the grains keep their
        // crisp lit blocks.
        val dpr = min(density, MaxScale)
        val bw = max(1, (state.width * dpr).roundToInt())
        val bh = max(1, (state.height * dpr).roundToInt())
        var s = surface
        if (s == null || s.width != bw || s.height != bh) {
            s = PixelSurface(bw, bh)
            surface = s
            gs = max(2, (1.55f * dpr).roundToInt())
            lit = FloatArray(gs * gs)
            for (y in 0 until gs) for (x in 0 until gs) {
                val dd = (x + y).toFloat() / max(1, 2 * (gs - 1))
                lit[y * gs + x] = 1.2f - .42f * dd
            }
            drawnFrame = -1
            committed = null
            committedFrame = -2
        }
        if (state.frame != drawnFrame || drawnSurface !== s) {
            render(state, s, dpr, colors)
            drawnFrame = state.frame
            drawnSurface = s
        }
        val img = s.commitIfNeeded()
        drawImage(
            img,
            dstOffset = IntOffset.Zero,
            dstSize =
                IntSize(
                    (state.width * density).roundToInt(),
                    (state.height * density).roundToInt(),
                ),
            filterQuality = FilterQuality.None,
        )
    }

    private var committedFrame = -2
    private var committed: androidx.compose.ui.graphics.ImageBitmap? = null

    private fun PixelSurface.commitIfNeeded(): androidx.compose.ui.graphics.ImageBitmap {
        val c = committed
        if (c != null && committedFrame == drawnFrame) return c
        val image = commit()
        committed = image
        committedFrame = drawnFrame
        return image
    }

    private fun render(
        state: SandMandalaState,
        surface: PixelSurface,
        dpr: Float,
        colors: SandMandalaScreenColors,
    ) {
        val buf = surface.pixels
        // The grains' arrays, read once.
        val sTx = state.tx
        val sTy = state.ty
        val sDx = state.dx
        val sDy = state.dy
        val sHx = state.hx
        val sHy = state.hy
        val sCcx = state.ccx
        val sCcy = state.ccy
        val sRel = state.rel
        val sGd = state.gd
        val sSd = state.sd
        val sSdur = state.sdur
        val sR0 = state.r0
        val sTh0 = state.th0
        val sHsh = state.hsh
        val sHpx = state.hpx
        val sHpy = state.hpy
        val sPd = state.pd
        val sRnd = state.rnd
        val sCH = state.cH
        val sCT = state.cT
        val sCG = state.cG
        val sHq = state.hq
        buf.fill(0)
        val bw = surface.width
        val n = state.n
        if (px.size < n) {
            px = FloatArray(n)
            py = FloatArray(n)
            pl = FloatArray(n)
            pc = IntArray(n * 3)
            pv = BooleanArray(n)
        }
        val sh1 = colors.shadow1
        val sh2 = colors.shadow2
        val sh3 = colors.shadow3
        val ph = state.ph
        val at = state.at
        val t = state.t
        val g = gs
        val bwm = bw - g
        val bhm = surface.height - g
        val cx = state.cx
        val cy = state.cy
        val lipY = state.lipY
        var mov = 0
        for (i in 0 until n) {
            var x = 0f
            var y = 0f
            var lift = 0f
            var m1 = 0f
            var m2 = 0f
            var shade = 1f
            var vis = true
            if (ph == SandPhase.Gather || (ph == null && at == SandAt.Rows)) {
                val u = if (ph != null) ((t - sRel[i]) / sGd[i]).coerceIn(0f, 1f) else 0f
                if (u <= 0f) vis = false
                else {
                    val e = ease3(u)
                    val a = 1 - e
                    val ex = sTx[i] + sDx[i]
                    val ey = sTy[i] + sDy[i]
                    x = a * a * sHx[i] + 2 * a * e * sCcx[i] + e * e * ex
                    y = a * a * sHy[i] + 2 * a * e * sCcy[i] + e * e * ey
                    // A first little drop as the grain crumbles off the letter.
                    y += sin(min(1f, u * 5) * PI.toFloat()) * 2.2f * (1 - u)
                    lift = sin(PI.toFloat() * u) * 7
                    m1 = smooth(0f, .55f, u)
                    if (u < 1) mov++
                }
            } else if (
                ph == SandPhase.Sweep || at == SandAt.Heap || (ph == null && at == SandAt.Mandala)
            ) {
                val u =
                    if (ph == SandPhase.Sweep) ((t - sSd[i]) / sSdur[i]).coerceIn(0f, 1f)
                    else if (at == SandAt.Heap) 1f else 0f
                m1 = 1f
                if (u <= 0f) {
                    x = sTx[i] + sDx[i]
                    y = sTy[i] + sDy[i]
                } else {
                    val e = ease(u)
                    val rn = min(1.2f, sR0[i] / state.r)
                    val rq = sHq[i]
                    val rr2 = sR0[i] + (rq - sR0[i]) * e
                    val th = sTh0[i] + state.spin * (1.7f + 1.1f * rn) * e
                    x = cx + rr2 * cos(th)
                    y = cy + rr2 * sin(th)
                    lift = sin(PI.toFloat() * u) * 2.5f
                    m2 = smooth(.25f, 1f, u) * .97f
                    shade = 1f + (sHsh[i] - 1f) * smooth(.5f, 1f, u)
                    if (u < 1) mov++
                }
            } else if (ph == SandPhase.Pour || at == SandAt.Cleared) {
                val tau = if (ph == SandPhase.Pour) t - sPd[i] else 99f
                m1 = 1f
                m2 = 1f
                shade = sHsh[i]
                if (tau <= 0f) {
                    x = sHpx[i]
                    y = sHpy[i]
                } else {
                    if (tau < Lip) {
                        val e = ease(tau / Lip)
                        x = sHpx[i] + (cx + (sRnd[i] - .5f) * 9 - sHpx[i]) * e
                        y = sHpy[i] + (lipY - sHpy[i]) * e
                    } else {
                        val f = tau - Lip
                        y = lipY + 40 * f + .5f * Pa * f * f
                        x = cx + (sRnd[i] - .5f) * (9 + f * 26) + sin(f * 9 + sRnd[i] * 6) * 1.2f
                    }
                    lift = min(4f, tau * 16)
                    shade *= 1 + min(.1f, tau * .25f)
                    if (y > state.height + 4) vis = false else mov++
                }
            } else vis = false
            pv[i] = vis
            if (!vis) continue
            px[i] = x
            py[i] = y
            pl[i] = lift
            val i3 = i * 3
            for (c in 0 until 3) {
                var v =
                    if (m1 < 1f) sCH[i3 + c] + (sCT[i3 + c] - sCH[i3 + c]) * m1
                    else sCT[i3 + c].toFloat()
                if (m2 > 0f) v += (sCG[i3 + c] - v) * m2
                // As a Uint8ClampedArray stores it.
                pc[i3 + c] = (v * shade).roundToInt().coerceIn(0, 255)
            }
            // Shadow first, slightly further off while a grain is in the air.
            val o = ((.7f + lift * .45f) * dpr).roundToInt()
            val sxp = (x * dpr - g / 2f).roundToInt() + o
            val syp = (y * dpr - g / 2f).roundToInt() + o
            if (sxp < 0 || syp < 0 || sxp > bwm || syp > bhm) continue
            for (yy in 0 until g) {
                var k = (syp + yy) * bw + sxp
                for (xx in 0 until g) {
                    val b = buf[k]
                    buf[k] = if (b == 0) sh1 else if (b == sh1) sh2 else sh3
                    k++
                }
            }
        }
        // Then each grain as a tiny lit block: bright at the top left, dark at the bottom right.
        for (i in 0 until n) {
            if (!pv[i]) continue
            val xp = (px[i] * dpr - g / 2f).roundToInt()
            val yp = (py[i] * dpr - g / 2f).roundToInt()
            if (xp < 0 || yp < 0 || xp > bwm || yp > bhm) continue
            val i3 = i * 3
            val r = pc[i3]
            val gg = pc[i3 + 1]
            val b = pc[i3 + 2]
            for (yy in 0 until g) {
                var k = (yp + yy) * bw + xp
                for (xx in 0 until g) {
                    val f = lit[yy * g + xx]
                    val rr = min(255, (r * f).toInt())
                    val g2 = min(255, (gg * f).toInt())
                    val bb = min(255, (b * f).toInt())
                    buf[k] = (0xFF shl 24) or (rr shl 16) or (g2 shl 8) or bb
                    k++
                }
            }
        }
        state.moving = mov.toFloat() / max(1, n)
    }

    internal companion object {
        /**
         * Where the rows' words start: past the 20 dp margin, the 30 dp favicon and a 14 dp gap.
         */
        const val TextX = 64f

        /** The sand's pixels per dp at most, as the prototype's DPR cap. */
        const val MaxScale = 2f
        const val TitleLine = 14.5f * 1.3f
        const val SubLine = 12f * 1.35f

        private val ChalkRings = floatArrayOf(1f, .945f, .845f, .765f, .645f, .265f)
        private val ChalkSquares = floatArrayOf(.45f, .38f)

        /**
         * The chalk shows while the sand is laid, faintly under the mandala, and goes as it sweeps.
         */
        fun chalkAlpha(state: SandMandalaState): Float =
            when {
                state.ph == SandPhase.Gather ->
                    smooth(.1f, .8f, state.t) *
                        (1 - smooth(state.gt - .6f, state.gt, state.t) * .55f)
                state.at == SandAt.Mandala -> .45f
                state.ph == SandPhase.Sweep -> .45f * (1 - smooth(0f, .5f, state.t))
                else -> 0f
            }

        /** "No history" fades in as the heap drains. */
        fun emptyAlpha(state: SandMandalaState): Float =
            when {
                state.at == SandAt.Cleared -> 1f
                state.ph == SandPhase.Pour -> smooth(state.pt * .55f, state.pt, state.t)
                else -> 0f
            }
    }
}
