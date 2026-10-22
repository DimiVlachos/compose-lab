package dev.dimvlachos.lab.core.presentation.components.locket

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.AX
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.AY
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.BC
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.CL
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.PRX
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.PRY
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.RX
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.RY
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.THUMB
import dev.dimvlachos.lab.core.presentation.components.locket.Locket.TY
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

// The locket, ported line by line from the prototype's draw(): the same calls, in the same order,
// with the same numbers and CSS colours, in its 300 × 380 box, which [LocketBox] places.

/** What the painter writes and colours that come from the screen's theme. */
internal class LocketInk(
    val muted: Color,
    val accent: Color,
    val recent: String,
    val hint: String,
)

private const val Tau = (2 * PI).toFloat()

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun ease(t: Float) = if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f

private fun easeOut(t: Float) = 1f - (1f - t).pow(3)

private fun Canvas2D.gold(x0: Float, y0: Float, x1: Float, y1: Float): Gradient2D {
    val g = createLinearGradient(x0, y0, x1, y1)
    g.addColorStop(0f, "#fff0c2")
    g.addColorStop(.3f, "#e6bd62")
    g.addColorStop(.68f, "#a8772a")
    g.addColorStop(1f, "#5a3c0c")
    return g
}

/** A superellipse: [n] = 2 is the oval, larger squares it off. */
private fun Canvas2D.blob(cx: Float, cy: Float, rx: Float, ry: Float, n: Float, rot: Float) {
    val cr = cos(rot)
    val sr = sin(rot)
    beginPath()
    for (i in 0..64) {
        val t = i / 64f * Tau
        val ct = cos(t)
        val s = sin(t)
        val x = rx * sign(ct) * abs(ct).pow(2f / n)
        val y = ry * sign(s) * abs(s).pow(2f / n)
        val px = cx + x * cr - y * sr
        val py = cy + x * sr + y * cr
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    closePath()
}

/**
 * Paints the locket in [state] into its hero of [box]'s size (in dp, under the canvas's own scale),
 * [dark] or light. [density] turns the carried photo's shadow, which the browser gives in device
 * pixels, into this canvas's pixels.
 */
internal fun Canvas2D.drawLocket(
    state: LocketState,
    box: LocketBox,
    dark: Boolean,
    photos: List<ImageBitmap>,
    ink: LocketInk,
    density: Float,
) {
    val w = box.width
    val h = box.height
    val s = box.s
    // A warm lamp above the locket. It goes in a layer of its own, as the browser's canvas is, so
    // the fade cuts only the glow, not the screen under it.
    beginLayer()
    var g = createRadialGradient(box.ox + 150f * s, box.oy + 120f * s, 0f, box.ox + 150f * s, box.oy + 120f * s, 230f * s)
    g.addColorStop(0f, if (dark) "rgba(255,196,120,.16)" else "rgba(255,236,200,.85)")
    g.addColorStop(1f, "rgba(255,236,200,0)")
    fillStyle = g
    fillRect(0f, 0f, w, h)
    // Fade the glow out to nothing over the hero's top 40 dp and side 40 dp, so no edge shows.
    globalCompositeOperation = "destination-out"
    fade(0f, 0f, 0f, 40f, 0f, 0f, w, 40f)
    fade(0f, 0f, 40f, 0f, 0f, 0f, 40f, h)
    fade(w, 0f, w - 40f, 0f, w - 40f, 0f, 40f, h)
    globalCompositeOperation = "source-over"
    endLayer()

    save()
    translate(box.ox, box.oy)
    scale(s, s)
    drawShadow(state, dark)
    save()
    translate(AX, AY)
    rotate(-state.th)
    drawChain()
    drawBack(state, photos)
    drawLid(state)
    drawHover(state)
    drawGlint(state)
    restore()
    drawStrip(state, box, photos, ink)
    drawMoving(state, box, photos, dark, density * s)
    restore()
}

private fun Canvas2D.fade(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    rx: Float,
    ry: Float,
    rw: Float,
    rh: Float,
) {
    val m = createLinearGradient(x0, y0, x1, y1)
    m.addColorStop(0f, "#000")
    m.addColorStop(1f, "rgba(0,0,0,0)")
    fillStyle = m
    fillRect(rx, ry, rw, rh)
}

private fun Canvas2D.drawShadow(st: LocketState, dark: Boolean) {
    val bx = st.toWorldX(0f, BC)
    val by = st.toWorldY(0f, BC)
    val strong = if (dark) Shadow50 else Shadow22
    val soft = if (dark) Shadow20 else Shadow09
    fun sh(x: Float, y: Float, rx: Float, ry: Float) {
        val r = max(rx, ry)
        val g = createRadialGradient(x, y, 0f, x, y, r)
        g.addColorStop(0f, strong)
        g.addColorStop(.7f, soft)
        g.addColorStop(1f, "rgba(40,22,4,0)")
        save()
        translate(x, y)
        scale(rx / r, ry / r)
        translate(-x, -y)
        fillStyle = g
        beginPath()
        arc(x, y, r, 0f, Tau)
        fill()
        restore()
    }
    sh(bx + 10f, by + 14f, RX + 10f, RY + 10f)
    val o = max(0f, -cos(st.a))
    if (o > .05f) {
        val lx = st.toWorldX(st.lidCenter(), BC)
        val ly = st.toWorldY(st.lidCenter(), BC)
        sh(lx + 8f, ly + 14f, RX * o + 10f, RY + 8f)
    }
}

private fun Canvas2D.drawChain() {
    lineWidth = .9f
    var y = 2f
    var i = 0
    while (y < CL - 4f) {
        strokeStyle = if (i % 2 == 1) "#9a6c22" else "#e9c46c"
        beginPath()
        if (i % 2 == 1) ellipse(0f, y, .9f, 2.3f, 0f, 0f, Tau)
        else ellipse(0f, y, 1.9f, 1.6f, 0f, 0f, Tau)
        stroke()
        y += 3.6f
        i++
    }
    // The bail: a ring and a little collar on top of the locket.
    lineWidth = 2.2f
    strokeStyle = gold(-6f, CL - 6f, 6f, CL + 6f)
    beginPath()
    ellipse(0f, CL, 4.6f, 5.4f, 0f, 0f, Tau)
    stroke()
    fillStyle = gold(-5f, CL + 4f, 5f, BC - RY + 2f)
    beginPath()
    moveTo(-4f, CL + 4f)
    lineTo(4f, CL + 4f)
    lineTo(6f, BC - RY + 3f)
    lineTo(-6f, BC - RY + 3f)
    closePath()
    fill()
}

private fun Canvas2D.drawBack(st: LocketState, photos: List<ImageBitmap>) {
    fillStyle = gold(-RX, BC - RY, RX, BC + RY)
    beginPath()
    ellipse(0f, BC, RX, RY, 0f, 0f, Tau)
    fill()
    strokeStyle = "rgba(60,38,6,.7)"
    lineWidth = .8f
    stroke()
    // The cup and its bezel.
    var g = createRadialGradient(8f, BC + 10f, 4f, 0f, BC, RY)
    g.addColorStop(0f, "#d9a94e")
    g.addColorStop(1f, "#6e4a12")
    fillStyle = g
    beginPath()
    ellipse(0f, BC, RX - 4f, RY - 4f, 0f, 0f, Tau)
    fill()
    // The photo behind the glass.
    save()
    beginPath()
    ellipse(0f, BC, PRX, PRY, 0f, 0f, Tau)
    clip()
    drawCover(photos[st.photo], -PRX, BC - PRY, PRX * 2f, PRY * 2f)
    g = createRadialGradient(0f, BC, PRX * .6f, 0f, BC, PRY * 1.05f)
    g.addColorStop(0f, "rgba(0,0,0,0)")
    g.addColorStop(1f, "rgba(30,15,0,.4)")
    fillStyle = g
    fillRect(-PRX, BC - PRY, PRX * 2f, PRY * 2f)
    // Glass: a soft crescent of the lamp, and a sweep of light when a new photo lands.
    g = createLinearGradient(-PRX, BC - PRY, PRX * .4f, BC)
    g.addColorStop(0f, "rgba(255,255,255,.36)")
    g.addColorStop(.45f, "rgba(255,255,255,.06)")
    g.addColorStop(1f, "rgba(255,255,255,0)")
    fillStyle = g
    fillRect(-PRX, BC - PRY, PRX * 2f, PRY * 2f)
    if (st.glass >= 0f) {
        val p = (st.now - st.glass) / 700f
        if (p >= 0f && p < 1f) {
            val x = lerp(-PRX * 2f, PRX * 2f, ease(p))
            g = createLinearGradient(x - 16f, BC - 20f, x + 16f, BC + 20f)
            g.addColorStop(0f, "rgba(255,255,255,0)")
            g.addColorStop(.5f, Sweep.copy(alpha = .55f * sin(p * PI.toFloat())))
            g.addColorStop(1f, "rgba(255,255,255,0)")
            fillStyle = g
            fillRect(-PRX, BC - PRY, PRX * 2f, PRY * 2f)
        }
    }
    restore()
    lineWidth = 2.6f
    strokeStyle = gold(-PRX, BC - PRY, PRX, BC + PRY)
    beginPath()
    ellipse(0f, BC, PRX + 1.6f, PRY + 1.6f, 0f, 0f, Tau)
    stroke()
    lineWidth = .7f
    strokeStyle = "rgba(70,44,6,.6)"
    beginPath()
    ellipse(0f, BC, PRX + .2f, PRY + .2f, 0f, 0f, Tau)
    stroke()
    // Hinge knuckles, each centred on the rim at its height, where the lid meets the back.
    for (y in floatArrayOf(BC - 14f, BC + 6f)) {
        val d = (y + 4f - BC) / RY
        val x = -RX * sqrt(1f - d * d)
        fillStyle = gold(x - 3f, y, x + 3f, y + 8f)
        beginPath()
        roundRect(x - 2.6f, y, 5.2f, 8f, 2.4f)
        fill()
    }
}

private inline fun Canvas2D.engrave(fn: () -> Unit) {
    save()
    translate(.6f, .8f)
    strokeStyle = "rgba(255,246,214,.65)"
    fillStyle = "rgba(255,246,214,.65)"
    fn()
    restore()
    strokeStyle = "rgba(78,50,8,.78)"
    fillStyle = "rgba(78,50,8,.78)"
    fn()
}

private fun Canvas2D.drawLid(st: LocketState) {
    val ca = cos(st.a)
    val cx = st.lidCenter()
    val w = abs(ca)
    // The lid's thickness shows as it turns.
    val edge = sin(st.a) * 2.6f
    fillStyle = "#7a5418"
    beginPath()
    ellipse(
        cx - edge * (if (ca == 0f) 1f else sign(ca)) * .5f,
        BC,
        max(RX * w, 1.6f),
        RY,
        0f,
        0f,
        Tau,
    )
    fill()
    save()
    translate(cx, BC)
    if (ca >= 0f) {
        scale(max(ca, .001f), 1f)
        val g = createRadialGradient(-14f, -20f, 0f, -4f, -4f, 72f)
        g.addColorStop(0f, "#fff4cf")
        g.addColorStop(.3f, "#e9c26a")
        g.addColorStop(.72f, "#b4832d")
        g.addColorStop(1f, "#644310")
        fillStyle = g
        beginPath()
        ellipse(0f, 0f, RX, RY, 0f, 0f, Tau)
        fill()
        strokeStyle = "rgba(60,38,6,.7)"
        lineWidth = .8f
        stroke()
        // Engraved border, a ring of dots and the initial.
        engrave {
            lineWidth = .9f
            beginPath()
            ellipse(0f, 0f, RX - 6f, RY - 6f, 0f, 0f, Tau)
            stroke()
        }
        engrave {
            for (i in 0 until 30) {
                val t = i / 30f * Tau
                beginPath()
                arc(cos(t) * (RX - 11f), sin(t) * (RY - 11f), .85f, 0f, Tau)
                fill()
            }
        }
        engrave {
            lineWidth = .9f
            for (s in floatArrayOf(-1f, 1f)) {
                beginPath()
                moveTo(0f, s * 37f)
                quadraticCurveTo(12f, s * 44f, 17f, s * 37f)
                moveTo(0f, s * 37f)
                quadraticCurveTo(-12f, s * 44f, -17f, s * 37f)
                stroke()
            }
            // Great Vibes at 44 units: this position centres the N's ink on the lid, well inside
            // the ring of dots.
            font = "400 44px $InitialFont"
            textAlign = "center"
            textBaseline = "middle"
            fillText("N", -3.24f, 6.33f)
        }
        // As it turns away from the lamp it darkens.
        fillStyle = Turn.copy(alpha = (1f - ca) * .45f)
        beginPath()
        ellipse(0f, 0f, RX, RY, 0f, 0f, Tau)
        fill()
        // The catch: a small nub on the free edge, glinting now and then while it waits.
        val inv =
            if (!st.isOpen && st.a < .02f) max(0f, sin(st.now / 520f)).pow(8) else 0f
        fillStyle = gold(RX - 2f, -6f, RX + 5f, 6f)
        beginPath()
        roundRect(RX - 1.5f, -6.5f, 7f, 13f, 3f)
        fill()
        if (inv > .02f) {
            fillStyle = CatchGlint.copy(alpha = inv * .9f)
            beginPath()
            arc(RX + 2f, -2.5f, 2.2f, 0f, Tau)
            fill()
        }
    } else {
        scale(max(-ca, .001f), 1f)
        val g = createRadialGradient(14f, -18f, 0f, 4f, -4f, 70f)
        g.addColorStop(0f, "#fbe3a6")
        g.addColorStop(.45f, "#d2a24b")
        g.addColorStop(1f, "#7b5416")
        fillStyle = g
        beginPath()
        ellipse(0f, 0f, RX, RY, 0f, 0f, Tau)
        fill()
        strokeStyle = "rgba(60,38,6,.7)"
        lineWidth = .8f
        stroke()
        lineWidth = 1.6f
        strokeStyle = gold(-PRX, -PRY, PRX, PRY)
        beginPath()
        ellipse(0f, 0f, PRX + 1.6f, PRY + 1.6f, 0f, 0f, Tau)
        stroke()
        font = "500 11px $EngravingFont"
        textAlign = "center"
        textBaseline = "middle"
        fillStyle = "rgba(255,246,214,.6)"
        fillText("N.W.", .5f, -6.4f)
        fillText("Asheville · 2019", .5f, 9.6f)
        fillStyle = "rgba(78,50,8,.78)"
        fillText("N.W.", 0f, -7f)
        fillText("Asheville · 2019", 0f, 9f)
        fillStyle = Turn.copy(alpha = ((1f + ca) * .35f).coerceIn(0f, 1f))
        beginPath()
        ellipse(0f, 0f, RX, RY, 0f, 0f, Tau)
        fill()
        fillStyle = gold(-RX - 5f, -6f, -RX + 2f, 6f)
        beginPath()
        roundRect(-RX - 5.5f, -6.5f, 7f, 13f, 3f)
        fill()
    }
    restore()
}

private fun Canvas2D.drawHover(st: LocketState) {
    val d = st.drag ?: return
    if (d.kind != DragKind.Photo || !d.moved || !st.overLocket(d.x, d.y, 24f)) return
    val p = .55f + .45f * sin(st.now / 160f)
    strokeStyle = Hover.copy(alpha = .5f + .4f * p)
    lineWidth = 2.4f
    beginPath()
    ellipse(0f, BC, RX + 6f, RY + 6f, 0f, 0f, Tau)
    stroke()
}

private fun Canvas2D.drawGlint(st: LocketState) {
    if (st.glint < 0f) return
    val p = (st.now - st.glint - 60f) / 560f
    if (p <= 0f || p >= 1f) return
    val a = sin(p * PI.toFloat())
    val l = 15f * a
    save()
    globalCompositeOperation = "lighter"
    translate(-24f, BC - 46f)
    rotate(p * .6f)
    fillStyle = Star.copy(alpha = a)
    repeat(4) {
        rotate(PI.toFloat() / 2f)
        beginPath()
        moveTo(0f, -l)
        lineTo(1.2f * a, 0f)
        lineTo(-1.2f * a, 0f)
        closePath()
        fill()
    }
    restore()
}

private fun Canvas2D.drawThumb(
    photo: ImageBitmap,
    x: Float,
    y: Float,
    size: Float,
    n: Float,
    rot: Float,
    alpha: Float = 1f,
) {
    save()
    globalAlpha = alpha
    blob(x, y, size / 2f, size / 2f, n, rot)
    save()
    clip()
    translate(x, y)
    rotate(rot)
    drawCover(photo, -size / 2f, -size / 2f, size, size)
    restore()
    restore()
}

private fun Canvas2D.drawStrip(
    st: LocketState,
    box: LocketBox,
    photos: List<ImageBitmap>,
    ink: LocketInk,
) {
    save()
    translate(box.sdx, box.sdy)
    scale(box.sk, box.sk)
    font = "600 9.5px sans-serif"
    textBaseline = "alphabetic"
    fillStyle = ink.muted
    textAlign = "left"
    fillText(ink.recent, 16f, TY - 8f)
    textAlign = "right"
    font = "500 10px sans-serif"
    fillText(ink.hint, 284f, TY - 8f)
    for (j in 0 until Locket.PHOTOS) {
        val x = Locket.tcx0(j)
        if (st.isAway(j)) {
            setLineDash(Dash)
            strokeStyle = ink.muted
            globalAlpha = .5f
            lineWidth = 1.2f
            beginPath()
            roundRect(x - THUMB / 2f + 1f, TY + 1f, THUMB - 2f, THUMB - 2f, 11f)
            stroke()
            setLineDash(NoDash)
            globalAlpha = 1f
            continue
        }
        save()
        beginPath()
        roundRect(x - THUMB / 2f, TY, THUMB, THUMB, 11f)
        clip()
        drawCover(photos[j], x - THUMB / 2f, TY, THUMB, THUMB)
        restore()
        if (j == st.photo) {
            strokeStyle = ink.accent
            lineWidth = 2f
            beginPath()
            roundRect(x - THUMB / 2f - 3f, TY - 3f, THUMB + 6f, THUMB + 6f, 13f)
            stroke()
            fillStyle = ink.accent
            beginPath()
            arc(x + THUMB / 2f - 7f, TY + 7f, 7f, 0f, Tau)
            fill()
            strokeStyle = "#fff"
            lineWidth = 1.6f
            beginPath()
            moveTo(x + THUMB / 2f - 10f, TY + 7f)
            lineTo(x + THUMB / 2f - 7.6f, TY + 9.4f)
            lineTo(x + THUMB / 2f - 3.8f, TY + 4.8f)
            stroke()
        }
    }
    restore()
}

private fun Canvas2D.drawMoving(
    st: LocketState,
    box: LocketBox,
    photos: List<ImageBitmap>,
    dark: Boolean,
    k: Float,
) {
    // Photos sliding home.
    for (i in st.back.indices.reversed()) {
        val b = st.back[i]
        val p = if (b.ms > 0f) ((st.now - b.t0) / b.ms).coerceIn(0f, 1f) else 1f
        if (p >= 1f) continue
        val e = easeOut(p)
        val size = lerp(box.ts * 1.15f, box.ts, e)
        drawThumb(photos[b.j], lerp(b.x, box.tcx(b.j), e), lerp(b.y, box.tcy, e), size, 7f, 0f)
    }
    // The photo being carried.
    val d = st.drag
    if (d != null && d.kind == DragKind.Photo && d.moved) {
        val size = box.ts * (1f + .15f * st.lift)
        val over = st.overLocket(d.x, d.y, 24f)
        val s2 = if (over) size * .9f else size
        // A soft drop shadow: blur 12, offset (3, 6) box units. Shadows ignore the transform, so
        // the blob is drawn far off to the left and only its shadow is thrown back under the
        // photo; its strength follows the lift.
        save()
        globalAlpha = st.lift
        shadowColor = if (dark) "rgba(30,18,4,.45)" else "rgba(30,18,4,.2)"
        shadowBlur = 12f * k
        shadowOffsetX = (3f + 2000f) * k
        shadowOffsetY = 6f * k
        fillStyle = "#000"
        blob(d.x - 2000f, d.y, s2 / 2f, s2 / 2f, 7f, 0f)
        fill()
        restore()
        drawThumb(photos[d.j], d.x, d.y, s2, 7f, 0f)
    }
    // The photo trimming itself to the oval.
    val pl = st.place ?: return
    val p = if (pl.ms > 0f) ((st.now - pl.t0) / pl.ms).coerceIn(0f, 1f) else 1f
    if (p >= 1f) return
    val e = ease(p)
    val tx = st.toWorldX(0f, BC)
    val ty = st.toWorldY(0f, BC)
    val rot = -st.th * e
    val x = lerp(pl.x, tx, e)
    val y = lerp(pl.y, ty, e)
    val rx = lerp(pl.size / 2f, PRX, e)
    val ry = lerp(pl.size / 2f, PRY, e)
    val n = lerp(7f, 2f, e)
    // What falls outside the oval fades as it is trimmed away.
    if (p < .6f) drawThumb(photos[pl.j], x, y, lerp(pl.size, PRY * 2f, e), 7f, rot, (1f - p / .6f) * .5f)
    save()
    blob(x, y, rx, ry, n, rot)
    clip()
    translate(x, y)
    rotate(rot)
    val side = max(rx, ry) * 2f
    drawCover(photos[pl.j], -side / 2f, -side / 2f, side, side)
    restore()
}

/** The names the painter's fonts go by in [Canvas2D.fonts]. */
internal const val InitialFont = "vibes"
internal const val EngravingFont = "cormorant"

private val Dash = floatArrayOf(3f, 3f)
private val NoDash = floatArrayOf()
private val Sweep = Color(255, 255, 240)
private val Turn = Color(40, 24, 4)
private val CatchGlint = Color(255, 252, 236)
private val Hover = Color(255, 222, 150)
private val Star = Color(255, 255, 240)
private const val Shadow22 = "rgba(40,22,4,.22)"
private const val Shadow09 = "rgba(40,22,4,.088)"
private const val Shadow50 = "rgba(40,22,4,.5)"
private const val Shadow20 = "rgba(40,22,4,.2)"
