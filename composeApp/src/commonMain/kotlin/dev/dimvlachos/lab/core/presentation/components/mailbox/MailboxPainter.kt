package dev.dimvlachos.lab.core.presentation.components.mailbox

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// The prototype's mailbox, drawn in a 100-unit box seen from the front left: an arched front at
// the left, its top and side swept back along the depth vector D, on a wooden post. Ported call by
// call; its colours stay the prototype's CSS. The flag and the letters are changed where the
// prototype had them wrong at icon size (see each).

private const val CX = 30f
private const val CY = 50f
private const val R0 = 16f
private const val BOT = 68f
private const val L = 14f
private const val R = 46f
private const val DX = 40f
private const val DY = -9f
private val PHI = atan2(-40f / 41f, -9f / 41f) // the arch's upper tangent, for the depth vector
private const val OX = -0.667f // the front's outward normal, projected
private const val OY = 0.15f
private const val U0X = -0.9756f // forward along the side
private const val U0Y = 0.2195f

// The flag's pivot. The prototype pivoted it at 72% of the depth halfway up the side with a 34-unit
// arm, so lowered it reached past the front edge and lay across the door. Here it pivots low on the
// side, 70% back by the bottom rib, and lowered it lies flat along the side inside its outline.
private const val PX = R + 0.7f * DX
private const val PY = 63f + 0.7f * DY

// The flag's arm, where its plate starts along it, and the plate's width. The plate and its number
// are bigger than the prototype's, so the count reads at 68 dp: about 13 dp type, 9 dp figures.
private const val FlagLength = 28f
private const val PlateFrom = 6f
private const val PlateWidth = 23f
private const val NumberSize = 21f

// The letters lean out this much further than the prototype's, so they read at icon size.
private const val PeekReach = 1.35f

/** The units the icon shows across, and where they start, so the raised flag is never cut off. */
internal const val MailboxUnits = 112f
internal const val MailboxLeft = -12f
internal const val MailboxTop = 8f

// The post runs down to here.
private const val PostEnd = 98f

private const val FlightMs = 560.0

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun ease(p: Float) = if (p < 0.5f) 2f * p * p else 1f - (-2f * p + 2f).pow(2) / 2f

/**
 * Draws the mailbox of [state] into [g], [size] pixels square: the post, the galvanized body, the
 * door on its hinge, the [letters] inside once it opens, envelopes in flight, and the flag with the
 * count painted on it. [dark] picks the prototype's dusk shades.
 */
internal class MailboxPainter(private val g: Canvas2D, private val dark: Boolean) {
    // The transform every part is drawn under, to compose the door's and the number's with.
    private val base = FloatArray(6)

    fun draw(state: MailboxState, letters: Int, size: Float) {
        val k = size / MailboxUnits
        base[0] = k
        base[1] = 0f
        base[2] = 0f
        base[3] = k
        base[4] = -MailboxLeft * k
        base[5] = -MailboxTop * k
        g.setTransform(base[0], base[1], base[2], base[3], base[4], base[5])
        drawMailbox(state, letters)
    }

    private fun drawMailbox(state: MailboxState, letters: Int) {
        drawPost(PostEnd)
        drawBody()
        // Once the door hangs open the envelopes slide out over it, so they draw after it.
        val overDoor = state.doorAngle > 1.2f
        if (state.doorAngle > 0.02f) {
            drawInside()
            if (overDoor) drawDoor(state.doorAngle)
            drawEnvelopes(letters, state.peek)
            drawBody() // hides the parts of the envelopes that run back into the box
        }
        for (t0 in state.flights) {
            val q = ((state.now - t0) / FlightMs).toFloat().coerceIn(0f, 1f)
            if (q >= 1f) continue
            val sx = CX + OX * 40f
            val sy = CY + 6f + OY * 40f - 24f
            val ex = CX + DX * 0.35f
            val ey = BOT - 6f + DY * 0.35f
            val cx = CX - 8f
            val cy = CY - 22f
            val e = ease(q)
            val x = (1 - e) * (1 - e) * sx + 2 * (1 - e) * e * cx + e * e * ex
            val y = (1 - e) * (1 - e) * sy + 2 * (1 - e) * e * cy + e * e * ey
            g.save()
            if (q > 0.55f) {
                g.beginPath()
                archPath()
                g.clip()
            }
            envelope(
                x,
                y,
                lerp(22f, 15f, e),
                lerp(14f, 9f, e),
                lerp(-0.5f, 0.1f, e),
                (q * 6f).coerceIn(0f, 1f),
            )
            g.restore()
        }
        if (!overDoor) drawDoor(state.doorAngle)
        drawFlag(state)
    }

    private fun archPath() {
        g.moveTo(L, BOT)
        g.lineTo(L, CY)
        g.arc(CX, CY, R0, PI.toFloat(), (PI * 2).toFloat(), false)
        g.lineTo(R, BOT)
        g.closePath()
    }

    private fun bodyPath() {
        g.moveTo(R, BOT)
        g.lineTo(R + DX, BOT + DY)
        g.lineTo(R + DX, CY + DY)
        g.arc(CX + DX, CY + DY, R0, 0f, PHI, true)
        g.lineTo(CX + R0 * cos(PHI), CY + R0 * sin(PHI))
        g.arc(CX, CY, R0, PHI, 0f, false)
        g.closePath()
    }

    private fun drawBody() {
        val gx = 0.2195f
        val gy = 0.9756f
        val grad = g.createLinearGradient(CX - gx * 16, CY - gy * 16, CX + gx * 21, CY + gy * 21)
        grad.addColorStop(0f, if (dark) "#8e9195" else "#a9adb0")
        grad.addColorStop(0.12f, if (dark) "#d9d6cf" else "#f7f2e8")
        grad.addColorStop(0.3f, if (dark) "#a8aaac" else "#c8cacb")
        grad.addColorStop(0.52f, if (dark) "#7f8387" else "#999ea2")
        grad.addColorStop(0.56f, if (dark) "#73777b" else "#8d9297")
        grad.addColorStop(1f, if (dark) "#5d6165" else "#777c81")
        g.beginPath()
        bodyPath()
        g.fillStyle = grad
        g.fill()
        g.save()
        g.beginPath()
        bodyPath()
        g.clip()
        // Sun glint along the curve of the top.
        val sx = CX + 0.3f * DX - 3f
        val sy = CY - 13f + 0.3f * DY
        val rg = g.createRadialGradient(sx, sy, 0f, sx, sy, 18f)
        rg.addColorStop(0f, "rgba(255, 248, 232, .75)")
        rg.addColorStop(1f, "rgba(255, 248, 232, 0)")
        g.fillStyle = rg
        g.beginPath()
        g.ellipse(sx, sy, 18f, 4.5f, atan2(DY, DX), 0f, (PI * 2).toFloat())
        g.fill()
        // Two pressed ribs near each end.
        for (t in floatArrayOf(0.1f, 0.86f)) {
            val ox = DX * t
            val oy = DY * t
            for ((off, col, w) in Ribs) {
                g.beginPath()
                g.moveTo(R + ox + off * 0.97f, BOT + oy - off * 0.22f)
                g.lineTo(R + ox + off * 0.97f, CY + oy - off * 0.22f)
                g.arc(CX + ox + off * 0.97f, CY + oy - off * 0.22f, R0, 0f, PHI, true)
                g.strokeStyle = col
                g.lineWidth = w
                g.stroke()
            }
        }
        // Underside edge.
        g.beginPath()
        g.moveTo(R, BOT - 0.4f)
        g.lineTo(R + DX, BOT + DY - 0.4f)
        g.strokeStyle = "rgba(0,0,0,.28)"
        g.lineWidth = 1.4f
        g.stroke()
        g.restore()
    }

    private fun drawPost(end: Float) {
        val x0 = 46f
        val x1 = 57f
        val pg = g.createLinearGradient(x0, 0f, x1, 0f)
        pg.addColorStop(0f, if (dark) "#5a3a24" else "#8a5a35")
        pg.addColorStop(0.35f, if (dark) "#7d5434" else "#b67e4c")
        pg.addColorStop(1f, if (dark) "#3e2717" else "#6a4226")
        g.fillStyle = pg
        g.fillRect(x0, 60f, x1 - x0, end - 60f)
        g.fillStyle = "rgba(0,0,0,.35)"
        g.fillRect(x0, 60f, x1 - x0, 7f)
        g.fillStyle = "rgba(0,0,0,.12)"
        for (yy in floatArrayOf(74f, 83f, 97f, 109f)) if (yy < end - 2f)
            g.fillRect(x0 + 2f, yy, 6f, 0.7f)
    }

    // An envelope, with the airmail border the prototype kept for its large view: at icon size the
    // stripes and a firmer edge are what make it read as mail.
    private fun envelope(x: Float, y: Float, w: Float, h: Float, rot: Float, alpha: Float = 1f) {
        g.save()
        g.globalAlpha = alpha
        g.translate(x, y)
        g.rotate(rot)
        g.fillStyle = "#f6eddc"
        g.strokeStyle = "rgba(90,70,40,.7)"
        g.lineWidth = 0.9f
        g.beginPath()
        g.rect(-w / 2, -h / 2, w, h)
        g.fill()
        g.stroke()
        g.save()
        g.beginPath()
        g.rect(-w / 2, -h / 2, w, h)
        g.clip()
        g.lineWidth = 1.4f
        var i = -w
        while (i < w) {
            g.strokeStyle = if ((i / 3.2f).roundToInt() % 2 != 0) "#c33a2c" else "#2f5aa0"
            g.beginPath()
            g.moveTo(i, -h / 2)
            g.lineTo(i + 1.6f, -h / 2 + 1.6f)
            g.moveTo(i, h / 2)
            g.lineTo(i + 1.6f, h / 2 - 1.6f)
            g.stroke()
            i += 3.2f
        }
        g.restore()
        g.beginPath()
        g.moveTo(-w / 2, -h / 2)
        g.lineTo(0f, h * 0.08f)
        g.lineTo(w / 2, -h / 2)
        g.strokeStyle = "rgba(90,70,40,.55)"
        g.lineWidth = 0.7f
        g.stroke()
        g.restore()
    }

    private fun drawInside() {
        g.save()
        g.beginPath()
        archPath()
        g.clip()
        val ig = g.createLinearGradient(0f, CY - R0, 0f, BOT)
        ig.addColorStop(0f, "#16130f")
        ig.addColorStop(0.7f, "#2a2520")
        ig.addColorStop(1f, "#4a4239")
        g.fillStyle = ig
        g.fillRect(L - 1f, CY - R0 - 1f, R - L + 2f, BOT - CY + R0 + 2f)
        g.restore()
    }

    private fun drawEnvelopes(letters: Int, peek: Float) {
        // Letters lean out of the opening as cards; the clip keeps what's past the arch hidden
        // inside the box.
        val count = min(3, letters)
        if (count == 0) return
        g.save()
        g.beginPath()
        archPath()
        if (peek > 0.05f) {
            g.rect(-60f, -60f, L + 60f, 200f)
            g.rect(-60f, BOT, R + 60f, 100f)
        }
        g.clip()
        for (i in 0 until count) {
            val out = peek * PeekReach * (7f + i * 5f)
            val x = CX - 1f + i * 2.6f + OX * out * 1.25f
            val y = BOT - 5.5f - i * 3.3f + OY * out
            envelope(x, y, 25f, 15.5f, Lean[i] - peek * 0.08f * (i + 1))
        }
        g.restore()
    }

    private fun drawDoor(a: Float) {
        val s = sin(a)
        val c = cos(a)
        val dd = c - s * OY
        g.save()
        transform(1f, 0f, -OX * s, dd, OX * s * BOT, BOT * (1 - dd))
        val outer = dd > 0f
        val fg = g.createLinearGradient(L, CY - R0, R, BOT)
        if (outer) {
            val lit = lerp(1f, 0.78f, s.coerceIn(0f, 1f))
            fun sh(v: Int) = (v * lit).roundToInt()
            fg.addColorStop(
                0f,
                if (dark) Color(sh(196), sh(194), sh(188)) else Color(sh(226), sh(224), sh(218)),
            )
            fg.addColorStop(
                0.5f,
                if (dark) Color(sh(152), sh(155), sh(158)) else Color(sh(178), sh(182), sh(185)),
            )
            fg.addColorStop(
                1f,
                if (dark) Color(sh(112), sh(116), sh(120)) else Color(sh(138), sh(142), sh(147)),
            )
        } else {
            fg.addColorStop(0f, "#9b9fa3")
            fg.addColorStop(1f, "#6f7377")
        }
        g.beginPath()
        archPath()
        g.fillStyle = fg
        g.fill()
        if (outer) {
            g.save()
            g.beginPath()
            g.moveTo(L + 4f, BOT - 3f)
            g.lineTo(L + 4f, CY)
            g.arc(CX, CY, R0 - 4f, PI.toFloat(), (PI * 2).toFloat(), false)
            g.lineTo(R - 4f, BOT - 3f)
            g.strokeStyle = "rgba(255,255,255,.4)"
            g.lineWidth = 1f
            g.stroke()
            g.translate(0.6f, 0.8f)
            g.strokeStyle = "rgba(0,0,0,.14)"
            g.stroke()
            g.restore()
        }
        g.beginPath()
        archPath()
        g.strokeStyle = if (outer) "rgba(255,255,255,.55)" else "rgba(0,0,0,.25)"
        g.lineWidth = 1.6f
        g.stroke()
        g.beginPath()
        archPath()
        g.strokeStyle = "rgba(40,40,40,.35)"
        g.lineWidth = 0.6f
        g.stroke()
        // Latch tab at the top of the door.
        g.fillStyle = if (outer) "#c9ccce" else "#8a8e92"
        g.strokeStyle = "rgba(0,0,0,.3)"
        g.lineWidth = 0.5f
        g.beginPath()
        g.roundRect(CX - 3f, CY - R0 - 3.2f, 6f, 4.4f, 1.2f)
        g.fill()
        g.stroke()
        g.restore()
    }

    private fun drawFlag(state: MailboxState) {
        val th = state.flagAngle
        val cs = cos(th)
        val sn = sin(th)
        // Along the arm, and across it to the plate's far edge.
        val ux = cs * U0X
        val uy = cs * U0Y - sn
        val vx = -sn * U0X
        val vy = -sn * U0Y - cs
        fun px(a: Float, b: Float) = PX + ux * a + vx * b
        fun py(a: Float, b: Float) = PY + uy * a + vy * b
        g.lineCap = "round"
        g.beginPath()
        g.moveTo(PX, PY)
        g.lineTo(px(FlagLength, 0f), py(FlagLength, 0f))
        g.strokeStyle = "#8e1a14"
        g.lineWidth = 3.4f
        g.stroke()
        g.beginPath()
        g.moveTo(px(2f, 0.5f), py(2f, 0.5f))
        g.lineTo(px(FlagLength - 1f, 0.5f), py(FlagLength - 1f, 0.5f))
        g.strokeStyle = "rgba(255,140,120,.55)"
        g.lineWidth = 0.8f
        g.stroke()
        val fg =
            g.createLinearGradient(
                px(PlateFrom, 0f),
                py(PlateFrom, 0f),
                px(FlagLength, PlateWidth),
                py(FlagLength, PlateWidth),
            )
        fg.addColorStop(0f, "#e2483a")
        fg.addColorStop(0.55f, "#c92d22")
        fg.addColorStop(1f, "#9c1c15")
        g.beginPath()
        g.moveTo(px(PlateFrom, 0f), py(PlateFrom, 0f))
        g.lineTo(px(FlagLength, 0f), py(FlagLength, 0f))
        g.lineTo(px(FlagLength, PlateWidth), py(FlagLength, PlateWidth))
        g.lineTo(px(PlateFrom, PlateWidth), py(PlateFrom, PlateWidth))
        g.closePath()
        g.fillStyle = fg
        g.fill()
        g.strokeStyle = "#6e110c"
        g.lineWidth = 0.7f
        g.stroke()
        g.beginPath()
        g.moveTo(px(PlateFrom + 0.8f, PlateWidth - 0.9f), py(PlateFrom + 0.8f, PlateWidth - 0.9f))
        g.lineTo(px(FlagLength - 0.8f, PlateWidth - 0.9f), py(FlagLength - 0.8f, PlateWidth - 0.9f))
        g.strokeStyle = "rgba(255,190,170,.5)"
        g.lineWidth = 0.7f
        g.stroke()
        g.lineCap = "butt"
        // Pivot rivet.
        g.beginPath()
        g.arc(PX, PY, 2.3f, 0f, (PI * 2).toFloat())
        g.fillStyle = "#5f6367"
        g.fill()
        g.beginPath()
        g.arc(PX - 0.6f, PY - 0.6f, 0.9f, 0f, (PI * 2).toFloat())
        g.fillStyle = "rgba(255,255,255,.6)"
        g.fill()
        val alpha = state.numberAlpha
        if (alpha > 0.01f && state.shown > 0 && g.textMeasurer != null) {
            val cx = px((PlateFrom + FlagLength) / 2f, PlateWidth / 2f)
            val cy = py((PlateFrom + FlagLength) / 2f, PlateWidth / 2f)
            g.save()
            g.globalAlpha = alpha
            transform(vx, vy, -ux, -uy, cx, cy)
            g.font = "800 ${NumberSize.toInt()}px system-ui"
            g.textAlign = "center"
            g.textBaseline = "middle"
            val text = if (state.shown > 9) "9+" else state.shown.toString()
            g.fillStyle = "rgba(80,8,4,.45)"
            g.fillText(text, 0.5f, 1.3f)
            g.fillStyle = "#fff7ee"
            g.fillText(text, 0f, 0.7f)
            g.restore()
        }
    }

    // The canvas's transform(): [base] times this matrix. Every caller is at the base level.
    private fun transform(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) {
        val (a0, b0, c0, d0, e0) = base
        val f0 = base[5]
        g.setTransform(
            a0 * a + c0 * b,
            b0 * a + d0 * b,
            a0 * c + c0 * d,
            b0 * c + d0 * d,
            a0 * e + c0 * f + e0,
            b0 * e + d0 * f + f0,
        )
    }

    private companion object {
        val Ribs =
            listOf(
                Triple(0f, "rgba(255,255,255,.5)", 1.1f),
                Triple(1.1f, "rgba(0,0,0,.2)", 1f),
            )
        val Lean = floatArrayOf(-0.2f, 0.1f, -0.06f)
    }
}
