package dev.dimvlachos.lab.core.presentation.components.rubberband

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val Pi = PI.toFloat()

/**
 * The band's tan latex, as the prototype's `BAND()`: its fill, its taut shade, its edge and its
 * sheen.
 */
private class BandColors(val fill: String, val taut: String, val edge: String, val hi: String)

private val LightBand =
    BandColors("#cf9f62", "#b27a3c", "rgba(110,70,25,.55)", "rgba(255,246,225,.7)")
private val DarkBand =
    BandColors("#b88a52", "#9d6a35", "rgba(40,24,8,.55)", "rgba(255,236,200,.35)")

/**
 * Draws the rubber band unsend over the screen as the prototype's `draw()`: the tethered bubble,
 * the brass eye, the band with its knot and its ring end, the caption above it, the snapped halves
 * curling home, the band whipping into the eye, the burst bubble's shell and its letters landing in
 * the field. Units are dp; [state]'s text layouts are in px. Its scratch arrays are kept, so a
 * frame allocates nothing for the band.
 */
internal class RubberBandPainter {
    private val pts = FloatArray(MaxPts * 2)
    private val ws = FloatArray(MaxPts)
    private val left = FloatArray(MaxPts * 2)
    private val right = FloatArray(MaxPts * 2)
    private val band = Band()
    private val line = Line()
    private val bubble = Path()

    private var haloStroke = Stroke(0f)
    private var glowShadow = Shadow(LetterGlow, blurRadius = 0f)

    // Made again only when the scale changes, not every frame.
    private fun halo(width: Float): Stroke {
        if (haloStroke.width != width) haloStroke = Stroke(width, join = StrokeJoin.Round)
        return haloStroke
    }

    private fun glow(blur: Float): Shadow {
        if (glowShadow.blurRadius != blur) glowShadow = Shadow(LetterGlow, blurRadius = blur)
        return glowShadow
    }

    private class Line : Curve {
        var a = Offset.Zero
        var b = Offset.Zero

        override fun at(t: Float) = Offset(lerp(a.x, b.x, t), lerp(a.y, b.y, t))
    }

    /**
     * The tethered bubble: drawn here so it shows over the composer as it rises out of and back
     * into it.
     */
    fun DrawScope.drawBubble(
        state: RubberBandState,
        text: TextLayoutResult?,
        fill: Color,
        ink: Color,
    ) {
        val t = state.tether ?: return
        if (t.mode == TetherMode.Done || t.popped) return
        val base = state.bubbleBase ?: return
        val d = density
        val cy = base.top + base.height / 2
        clipRect(top = state.threadTop * d) {
            translate((base.left + t.off.x) * d, (cy + t.off.y) * d) {
                scale(t.sc, t.sc, Offset.Zero) {
                    translate(0f, -base.height / 2 * d) {
                        val r = 19f * d
                        bubble.rewind()
                        bubble.addRoundRect(
                            RoundRect(
                                0f,
                                0f,
                                base.width * d,
                                base.height * d,
                                CornerRadius(r),
                                CornerRadius(r),
                                CornerRadius(6f * d),
                                CornerRadius(r),
                            ),
                        )
                        drawPath(bubble, fill)
                        if (text != null) drawText(text, ink, Offset(13f * d, 9f * d))
                    }
                }
            }
        }
    }

    /** The eye, and the band tied from the bubble's knot to it. */
    fun Canvas2D.drawBandLayer(state: RubberBandState, dark: Boolean) {
        val k = state.k
        // The prototype's canvas keeps the round cap ribbon() sets, so every arc after the first
        // frame has it.
        lineCap = "round"
        drawEye(state, dark)
        val t = state.tether ?: return
        if (t.mode == TetherMode.Done || t.popped || state.bubbleBase == null) return
        val b = state.bandOf(t, band)
        val w = state.bandWidth(t, b)
        val l = state.life(t)
        // The neck forms over the last three seconds of the window, and trembles just before it
        // goes.
        val neck = neckOf(l)
        val wob =
            if (l > .86f && !t.held) sin(state.now / 24.0).toFloat() * (l - .86f) * 6 * k else 0f
        val c = if (dark) DarkBand else LightBand
        drawBand(b, w, neck, t.mode == TetherMode.Fly || state.isTaut, wob, c, k)
        drawKnot(b.a, w, c, k)
        // The ring end, wrapped round the eye.
        beginPath()
        arc(state.eye.x, state.eye.y, state.eyeR, Pi * .65f, Pi * 1.35f)
        lineWidth = w * .9f
        strokeStyle = c.fill
        stroke()
    }

    /** The snapped bands, the band whipping into the eye, and the burst bubble's shell. */
    fun Canvas2D.drawAfterLayer(state: RubberBandState, dark: Boolean, me: String) {
        val k = state.k
        val c = if (dark) DarkBand else LightBand
        val now = state.now
        // Snapped bands: each half curls back into its end.
        for (i in state.breaks.indices.reversed()) {
            val s = state.breaks[i]
            val p = ((now - s.t0) / RubberBandState.SnapMs).toFloat().coerceIn(0f, 1f)
            if (p >= 1f) continue
            val e = 1 - (1 - p).pow(3)
            for (side in 0..1) {
                val lo = if (side == 0) 0f else .5f + .5f * e
                val hi = if (side == 0) .5f * (1 - e) else 1f
                val sgn = if (side == 0) 1f else -1f
                if (hi - lo < .01f) continue
                val n = 16
                for (j in 0..n) {
                    val tt = lerp(lo, hi, j / n.toFloat())
                    val q = s.band.at(tt)
                    val f = if (sgn > 0) j / n.toFloat() else 1 - j / n.toFloat()
                    pts[j * 2] = q.x
                    pts[j * 2 + 1] =
                        q.y + sin(f * Pi * 2.5f + p * 9) * f * 7 * k * (1 - p * .5f) * sgn
                    ws[j] = s.w * (1.2f - .5f * f) * (1 + p * .6f)
                }
                globalAlpha = 1 - p * .4f
                ribbon(n + 1, c.fill, c.edge, c.hi, k)
                globalAlpha = 1f
            }
        }
        // After the pop: the band whips into the eye.
        if (state.retractT0 >= 0) {
            val p = ((now - state.retractT0) / 200.0).toFloat().coerceIn(0f, 1f)
            val e = p * p
            if (p < 1f) {
                val a = state.retractA
                val b = state.retractB
                line.a = Offset(lerp(a.x, b.x, e), lerp(a.y, b.y, e))
                line.b = b
                drawBand(line, state.retractW * (1 + p), 0f, false, 0f, c, k)
            }
        }
        // The bubble's shell shrinks away under its letters.
        val sh = state.shell
        if (sh != null) {
            val p = ((now - state.shellT0) / 220.0).toFloat().coerceIn(0f, 1f)
            if (p < 1f) {
                val e = 1 - (1 - p).pow(2)
                val w = sh.width * (1 - e * .7f)
                val h = sh.height * (1 - e * .55f)
                val x = sh.left + (sh.width - w) * .15f
                val y = sh.top + (sh.height - h) / 2
                globalAlpha = 1 - e
                beginPath()
                roundRect(x, y, w, h, min(h / 2, 19f))
                fillStyle = me
                fill()
                globalAlpha = 1f
            }
        }
    }

    /**
     * The caption above the band, ringed in the thread's colour so a band running behind it doesn't
     * strike it out: [halo] stroked, then [text] filled, where the caption button's own text is
     * laid out.
     */
    fun DrawScope.drawCaption(
        state: RubberBandState,
        text: TextLayoutResult,
        halo: TextLayoutResult,
    ) {
        if (state.tether == null) return
        val at = state.captionAt ?: return
        val a = state.captionAlpha()
        if (a < .01f) return
        val o = at * density
        drawText(
            halo,
            topLeft = o,
            alpha = a,
            drawStyle = halo(3.5f * state.k * density),
        )
        drawText(text, topLeft = o, alpha = a)
    }

    /**
     * The letters out of the bubble, white while its shell is behind them, its blue in the air, the
     * field's [ink] once down. [layouts] are each letter set in the field's style, at
     * [RubberBandState.FieldFont].
     */
    fun DrawScope.drawLetters(
        state: RubberBandState,
        layouts: List<TextLayoutResult>,
        me: Color,
        ink: Color,
    ) {
        val ls = state.letters
        if (ls.isEmpty() || layouts.size != ls.size) return
        val now = state.now
        val k = state.k
        val d = density
        // The shell is over half faded by 60 ms; after that white glyphs would sit as a ghost on
        // the field.
        val shellOn = state.shell != null && now - state.shellT0 < 60
        for (i in ls.indices) {
            val l = ls[i]
            if (l.ch == " ") continue
            val p = ((now - l.t0) / RubberBandState.LetterMs).toFloat().coerceIn(0f, 1f)
            val e = if (p < 1f) 1 - (1 - p).pow(2.4f) else 1f
            val x = lerp(l.x0, l.x1, e)
            val y = lerp(l.y0, l.y1, e) - sin(min(1f, p * 1.15f) * Pi) * l.up
            val sz = lerp(l.s0, l.s1, e) / RubberBandState.FieldFont
            val land = if (p > .88f) sin((p - .88f) / .12f * Pi) * .12f else 0f
            val lay = layouts[i]
            val white = p < .18f && shellOn
            val color = if (white) Color.White else if (p < .7f) me else ink
            translate(x * d, y * d) {
                rotateRad(l.rot * sin(p * Pi), Offset.Zero) {
                    scale(sz * (1 + land), sz * (1 - land), Offset.Zero) {
                        drawText(
                            lay,
                            color,
                            Offset(-lay.size.width / 2f, -lay.size.height / 2f),
                            alpha = if (l.clip) 1 - e else 1f,
                            shadow = if (white) glow(4 * k * d / 2) else null,
                        )
                    }
                }
            }
        }
    }

    private fun Canvas2D.drawBand(
        at: Curve,
        w: Float,
        neck: Float,
        taut: Boolean,
        wob: Float,
        c: BandColors,
        k: Float,
    ) {
        val n = 32
        val a = at.at(0f)
        val b = at.at(1f)
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dd = hypot(dx, dy).takeIf { it > 0f } ?: 1f
        for (i in 0..n) {
            val t = i / n.toFloat()
            var p = at.at(t)
            if (wob != 0f) {
                val s = sin(t * Pi) * wob
                p = Offset(p.x - dy / dd * s, p.y + dx / dd * s)
            }
            pts[i * 2] = p.x
            pts[i * 2 + 1] = p.y
            ws[i] = ribbonWidth(w, neck, t)
        }
        ribbon(n + 1, if (taut) c.taut else c.fill, c.edge, c.hi, k)
    }

    // A ribbon whose width varies along it, with a soft sheen along one side, the way latex catches
    // light.
    private fun Canvas2D.ribbon(n: Int, fill: String, edge: String, hi: String, k: Float) {
        var minW = Float.MAX_VALUE
        for (i in 0 until n) {
            val ia = max(0, i - 1)
            val ib = min(n - 1, i + 1)
            val dx = pts[ib * 2] - pts[ia * 2]
            val dy = pts[ib * 2 + 1] - pts[ia * 2 + 1]
            val d = hypot(dx, dy).takeIf { it > 0f } ?: 1f
            val nx = -dy / d
            val ny = dx / d
            val w = ws[i] / 2
            left[i * 2] = pts[i * 2] + nx * w
            left[i * 2 + 1] = pts[i * 2 + 1] + ny * w
            right[i * 2] = pts[i * 2] - nx * w
            right[i * 2 + 1] = pts[i * 2 + 1] - ny * w
            minW = min(minW, ws[i])
        }
        beginPath()
        moveTo(left[0], left[1])
        for (i in 1 until n) lineTo(left[i * 2], left[i * 2 + 1])
        for (i in n - 1 downTo 0) lineTo(right[i * 2], right[i * 2 + 1])
        closePath()
        fillStyle = fill
        fill()
        lineWidth = .8f * k
        strokeStyle = edge
        stroke()
        beginPath()
        for (i in 0 until n) {
            val x = lerp(pts[i * 2], left[i * 2], .45f)
            val y = lerp(pts[i * 2 + 1], left[i * 2 + 1], .45f)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        lineWidth = max(.6f, minW * .22f)
        strokeStyle = hi
        // Left round, as the prototype's is, for the ring end and the eye drawn after it.
        lineCap = "round"
        stroke()
    }

    private fun Canvas2D.drawKnot(a: Offset, w: Float, c: BandColors, k: Float) {
        save()
        translate(a.x, a.y)
        beginPath()
        ellipse(0f, 0f, w * .95f + 1.4f * k, w * .7f + 1 * k, -.5f, 0f, Pi * 2)
        fillStyle = c.taut
        fill()
        lineWidth = .8f * k
        strokeStyle = c.edge
        stroke()
        beginPath()
        arc(-w * .2f, -w * .2f, w * .3f, 0f, Pi * 2)
        fillStyle = c.hi
        fill()
        restore()
    }

    // A brass screw eye set into the composer.
    private fun Canvas2D.drawEye(state: RubberBandState, dark: Boolean) {
        val k = state.k
        val x = state.eye.x
        val y = state.eye.y
        val r = state.eyeR
        save()
        beginPath()
        ellipse(x + 1 * k, y + r + 6 * k, r * 1.15f, r * .45f, 0f, 0f, Pi * 2)
        fillStyle = if (dark) "rgba(0,0,0,.5)" else "rgba(60,40,10,.18)"
        fill()
        // Shank.
        var g = createLinearGradient(x - 2 * k, 0f, x + 2 * k, 0f)
        g.addColorStop(0f, "#7a5a1e")
        g.addColorStop(.5f, "#e8c46a")
        g.addColorStop(1f, "#8c6a26")
        fillStyle = g
        fillRect(x - 1.6f * k, y + r * .8f, 3.2f * k, 6.5f * k)
        // Collar.
        beginPath()
        ellipse(x, y + r + 6.2f * k, 3.6f * k, 1.5f * k, 0f, 0f, Pi * 2)
        fillStyle = "#9a7630"
        fill()
        // Ring.
        g = createLinearGradient(x - r, y - r, x + r, y + r)
        g.addColorStop(0f, "#f6dd8c")
        g.addColorStop(.45f, "#c99a3c")
        g.addColorStop(1f, "#6e5017")
        beginPath()
        arc(x, y, r, 0f, Pi * 2)
        lineWidth = 2.6f * k
        strokeStyle = g
        stroke()
        beginPath()
        arc(x, y, r, Pi * 1.1f, Pi * 1.5f)
        lineWidth = .9f * k
        strokeStyle = "rgba(255,250,225,.85)"
        stroke()
        restore()
    }

    /**
     * The composer's slot round the eye: a shallow dish, a soft radial shade with an inset shadow
     * along its top, as the prototype's `.eyeslot`. Drawn in a box of [size] dp.
     */
    fun Canvas2D.drawEyeSlot(size: Float) {
        val r = size / 2
        val cx = r
        val cy = r
        save()
        beginPath()
        arc(cx, cy, r, 0f, Pi * 2)
        clip()
        // radial-gradient(circle at 50% 45%, ...): out to the farthest corner, its last stop at 70
        // %.
        val gx = size * .5f
        val gy = size * .45f
        val far = hypot(max(gx, size - gx), max(gy, size - gy))
        val g = createRadialGradient(gx, gy, 0f, gx, gy, far)
        g.addColorStop(0f, "rgba(0,0,0,.08)")
        g.addColorStop(.7f, "rgba(0,0,0,.02)")
        g.addColorStop(1f, "rgba(0,0,0,.02)")
        fillStyle = g
        fillRect(0f, 0f, size, size)
        // inset 0 1px 2px rgba(0,0,0,.12): a ring outside the dish casting its shadow in.
        beginPath()
        arc(cx, cy, r + 12f, 0f, Pi * 2)
        arc(cx, cy, r, 0f, Pi * 2, ccw = true)
        shadowColor = "rgba(0,0,0,.12)"
        shadowBlur = 2f * transformScale
        shadowOffsetY = 1f * transformScale
        fillStyle = "rgba(0,0,0,1)"
        fill("evenodd")
        restore()
    }

    private companion object {
        const val MaxPts = 33
        val LetterGlow = Color(30, 60, 140, (.45f * 255).toInt())
    }
}
