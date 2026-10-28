package dev.dimvlachos.lab.core.presentation.components.paintroller

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.canvas2d.cssColor
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import dev.dimvlachos.lab.core.presentation.ui.RollerPaints
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The prototype's `shade()`: toward black for a negative [f], toward white for a positive one. */
internal fun shade(c: Color, f: Float): Color {
    fun m(v: Float): Float {
        val b = v * 255f
        return (if (f < 0) b * (1 + f) else b + (255 - b) * f).roundToInt() / 255f
    }
    return Color(m(c.red), m(c.green), m(c.blue))
}

/** [c] at alpha [a]. */
internal fun rgba(c: Color, a: Float) = c.copy(alpha = a)

/** The colours a frame needs, worked out once for each theme rather than every frame. */
internal class RollerPalette(dark: Boolean) {
    val paint = RollerPaints.map { cssColor(it.css(dark)) }
    val plastic = cssColor(if (dark) "#45464d" else "#f1efea")
    val plasticFoot = shade(plastic, -.08f)
    val rim = cssColor(if (dark) "#5d5f66" else "#d3cfc7")
    val floor = cssColor(if (dark) "#2e2f34" else "#dedad2")
    val well = cssColor(if (dark) "#1d1e22" else "#bdb9b0")
    val ribLight = Color(1f, 1f, 1f, if (dark) .14f else .55f)
    val ribDark = Color(0f, 0f, 0f, if (dark) .35f else .12f)
    val film0 = paint.map { rgba(it, 0f) }
    val film1 = paint.map { rgba(it, .35f) }
    val film2 = paint.map { rgba(it, .9f) }
    val deep = paint.map { shade(it, -.28f) }
    val mid = paint.map { shade(it, -.04f) }
    val top = paint.map { shade(it, .06f) }
    val fuzz = paint.map { shade(it, -.1f) }
    // The wet film: fainter on the near-black night ground, and blurred there.
    private val filmA = if (dark) .025f else .04f
    val wetEdge = paint.map { rgba(it, filmA) }
    val wetMid = paint.map { rgba(it, filmA / 2) }

    internal companion object {
        val Light by lazy { RollerPalette(false) }
        val Dark by lazy { RollerPalette(true) }

        fun of(dark: Boolean) = if (dark) Dark else Light
    }
}

private val Shadow18 = Color(0f, 0f, 0f, .18f)
private val Shadow22 = Color(0f, 0f, 0f, .22f)
private val White45 = Color(1f, 1f, 1f, .45f)
private val White0 = Color(1f, 1f, 1f, 0f)
private val White25 = Color(1f, 1f, 1f, .25f)
private val White35 = Color(1f, 1f, 1f, .35f)
private val WireDark = cssColor("#6d7177")
private val WireLight = cssColor("#c9cdd2")
private val Ferrule = cssColor("#a7acb2")
private val Grain = Color(60, 30, 10, (.35f * 255).roundToInt())
private val Knot = cssColor("#6b3f1c")
private val Cap = cssColor("#3a3c40")
private val Beech = listOf("#9a6232", "#d9a467", "#c88c4e", "#7d4d26").map { cssColor(it) }
private val Sheen =
    listOf(
        0f to Color(1f, 1f, 1f, .34f),
        .22f to Color(1f, 1f, 1f, .16f),
        .45f to White0,
        .75f to Color(0f, 0f, 0f, .16f),
        1f to Color(0f, 0f, 0f, .42f),
    )

/**
 * A strip of roller nap: soft fibres, lighter and darker, that scrolls as the roller turns. The
 * prototype's `makeNap()`, from its seed, baked once.
 */
internal val RollerNap: ImageBitmap by lazy {
    val g = Canvas2D.create(256, 128)
    var s = 7L
    fun rnd(): Double {
        s = (s * 16807L) % 2147483647L
        return s / 2147483647.0
    }
    repeat(1400) {
        val x = (rnd() * 256).toFloat()
        val y = (rnd() * 128).toFloat()
        val l = (1.5 + rnd() * 4).toFloat()
        val a = (.05 + rnd() * .14).toFloat()
        g.strokeStyle = if (rnd() < .5) Color(1f, 1f, 1f, a) else Color(0f, 0f, 0f, a)
        g.lineWidth = (.8 + rnd()).toFloat()
        g.beginPath()
        g.moveTo(x, y)
        g.lineTo(x + ((rnd() - .5) * 1.5).toFloat(), y + l)
        g.stroke()
    }
    g.bitmap.also { it.freeze() }
}

/**
 * The roller's canvas, the prototype's `draw()`: the thin wet film over the freshest strokes, the
 * five trays and the roller. [c] is scaled to dp; [density] sizes the blurs, which the prototype
 * gives in pixels at a density of 2.
 */
internal fun Canvas2D.drawPaintRoller(state: PaintRollerState, dark: Boolean, density: Float) {
    if (state.trays.isEmpty()) return
    val pal = RollerPalette.of(dark)
    val to = state.coat
    if (to >= 0 && state.rects.isNotEmpty()) {
        // One union path, filled once, so overlaps don't stack into bands.
        var any = false
        beginPath()
        for (r in state.rects) {
            if (state.now - r.t < 1200) {
                val y0 = max(0f, r.y0)
                rect(r.x0, y0, r.x1 - r.x0, min(state.paintBottom, r.y1) - y0)
                any = true
            }
        }
        if (any) {
            val g = createLinearGradient(0f, 0f, state.width, 0f)
            g.addColorStop(0f, pal.wetEdge[to])
            g.addColorStop(.5f, pal.wetMid[to])
            g.addColorStop(1f, pal.wetEdge[to])
            save()
            if (dark) filter = "blur(${6f * density / 2f}px)"
            fillStyle = g
            fill("nonzero")
            restore()
            filter = "none"
        }
    }
    state.trays.forEachIndexed { i, t -> drawTray(t, i, state.now, pal, dark) }
    drawRoller(state, pal.paint[state.rollerPaint], pal.fuzz[state.rollerPaint], density)
}

private fun Canvas2D.rrect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    beginPath()
    roundRect(x, y, w, h, r)
}

/**
 * A small plastic roller tray seen from above and a little in front: a rim, a ribbed ramp sloping
 * down to a deep well of paint.
 */
private fun Canvas2D.drawTray(t: Tray, i: Int, now: Double, pal: RollerPalette, dark: Boolean) {
    val x = t.cx - t.tw / 2
    val y = t.top
    val w = t.tw
    val h = t.th
    save()
    fillStyle = Shadow18
    rrect(x + 1.5f, y + 3.5f, w, h, 8f)
    fill()
    val body = createLinearGradient(0f, y, 0f, y + h)
    body.addColorStop(0f, pal.plastic)
    body.addColorStop(1f, pal.plasticFoot)
    fillStyle = body
    rrect(x, y, w, h, 8f)
    fill()
    strokeStyle = pal.rim
    lineWidth = 1f
    stroke()
    // The ramp, its floor shadowed at the top where the rim overhangs.
    val rx = x + 5
    val ry = y + 5
    val rW = w - 10
    val rH = h * .44f
    fillStyle = pal.floor
    rrect(rx, ry, rW, rH, 4f)
    fill()
    save()
    rrect(rx, ry, rW, rH, 4f)
    clip()
    // Paint the roller has spread up the ramp: thick near the well, streaky and thin at the top.
    val film = createLinearGradient(0f, ry, 0f, ry + rH)
    film.addColorStop(0f, pal.film0[i])
    film.addColorStop(.45f, pal.film1[i])
    film.addColorStop(1f, pal.film2[i])
    fillStyle = film
    fillRect(rx, ry, rW, rH)
    // Raised ribs: a highlight with a shadow under it.
    for (j in 0 until 4) {
        val yy = ry + 3 + j * rH / 4.2f
        fillStyle = pal.ribLight
        fillRect(rx + 3, yy, rW - 6, 1f)
        fillStyle = pal.ribDark
        fillRect(rx + 3, yy + 1, rW - 6, 1f)
    }
    fillStyle = Shadow18
    fillRect(rx, ry, rW, 2f)
    restore()
    // The well, full of paint: deep colour, a darker meniscus at the back, a soft highlight.
    val wy = ry + rH + 2
    val wh = y + h - 5 - wy
    fillStyle = pal.well
    rrect(rx, wy, rW, wh, 5f)
    fill()
    val pg = createLinearGradient(0f, wy, 0f, wy + wh)
    pg.addColorStop(0f, pal.deep[i])
    pg.addColorStop(.25f, pal.mid[i])
    pg.addColorStop(1f, pal.top[i])
    fillStyle = pg
    rrect(rx + 1, wy + 1, rW - 2, wh - 2, 4.5f)
    fill()
    save()
    rrect(rx + 1, wy + 1, rW - 2, wh - 2, 4.5f)
    clip()
    translate(rx + rW * .38f, wy + wh * .55f)
    scale(1f, .32f)
    val hl = createRadialGradient(0f, 0f, 0f, 0f, 0f, rW * .42f)
    hl.addColorStop(0f, White45)
    hl.addColorStop(1f, White0)
    fillStyle = hl
    beginPath()
    arc(0f, 0f, rW * .42f, 0f, TAU)
    fill()
    restore()
    val age = now - t.ripple
    if (age < 700) {
        val p = (age / 700).toFloat()
        save()
        rrect(rx + 1, wy + 1, rW - 2, wh - 2, 4.5f)
        clip()
        strokeStyle = Color(1f, 1f, 1f, .6f * (1 - p))
        lineWidth = 1.3f
        for (q in floatArrayOf(p, max(0f, p - .25f))) {
            beginPath()
            ellipse(t.cx, wy + wh * .5f, rW * (.1f + q * .55f), wh * (.1f + q * .38f), 0f, 0f, TAU)
            stroke()
        }
        restore()
    }
    restore()
}

/** The roller: a nap cover on a wire frame with a beech handle, at its pose, in [color]. */
private fun Canvas2D.drawRoller(
    state: PaintRollerState,
    color: Color,
    fuzz: Color,
    density: Float,
) {
    val x = state.rx
    val y = state.ry
    val s = state.rs
    val l = state.rw * s
    val d = state.rd * s
    val u = s * state.k
    save()
    // Shadow on the wall, down and to the right, softer as the roller grows.
    save()
    filter = "blur(${max(1f, 7 * s) * density / 2f}px)"
    fillStyle = Shadow22
    rrect(x - l / 2 + 5 * u, y - d / 2 + 9 * u, l, d, d * .4f)
    fill()
    beginPath()
    moveTo(x + 4 * u, y + 66 * u)
    lineTo(x + 8 * u, y + 132 * u)
    lineWidth = 14 * u
    strokeStyle = Shadow18
    stroke()
    restore()
    filter = "none"
    // The wire frame: out of the right end cap, round and down under the middle to the handle.
    val ex = x + l / 2
    lineCap = "round"
    lineJoin = "round"
    fun wire() {
        beginPath()
        moveTo(ex - 2 * u, y)
        lineTo(ex + 8 * u, y)
        bezierCurveTo(ex + 20 * u, y, ex + 18 * u, y + 24 * u, ex + 4 * u, y + 30 * u)
        lineTo(x + 10 * u, y + 46 * u)
        quadraticCurveTo(x, y + 51 * u, x, y + 60 * u)
    }
    wire()
    strokeStyle = WireDark
    lineWidth = 4.2f * u
    stroke()
    wire()
    strokeStyle = WireLight
    lineWidth = 2.2f * u
    stroke()
    // Handle: a steel ferrule, then a lacquered beech grip.
    fillStyle = Ferrule
    rrect(x - 5.5f * u, y + 56 * u, 11 * u, 12 * u, 2 * u)
    fill()
    fillStyle = White45
    fillRect(x - 3.5f * u, y + 57 * u, 2 * u, 10 * u)
    val g = createLinearGradient(x - 8 * u, 0f, x + 8 * u, 0f)
    g.addColorStop(0f, Beech[0])
    g.addColorStop(.35f, Beech[1])
    g.addColorStop(.55f, Beech[2])
    g.addColorStop(1f, Beech[3])
    fillStyle = g
    rrect(x - 8 * u, y + 67 * u, 16 * u, 62 * u, 8 * u)
    fill()
    strokeStyle = Grain
    lineWidth = .8f * u
    for (j in 0 until 3) {
        beginPath()
        moveTo(x - 3 * u + j * 3 * u, y + 72 * u)
        quadraticCurveTo(x - 1 * u + j * 3 * u, y + 98 * u, x - 3 * u + j * 3 * u, y + 124 * u)
        stroke()
    }
    fillStyle = Knot
    beginPath()
    arc(x, y + 121 * u, 2.2f * u, 0f, TAU)
    fill()
    // The cover: paint-soaked nap, shaded round, its fibres scrolling as it turns.
    save()
    rrect(x - l / 2, y - d / 2, l, d, d * .42f)
    fillStyle = color
    fill()
    clip()
    val off = ((state.phase % 128f) + 128f) % 128f
    val sc = d / 64f
    globalAlpha = .9f
    var yy = -128 * sc + off * sc
    while (yy < d + 128 * sc) {
        var xx = 0f
        while (xx < l) {
            drawImage(RollerNap, x - l / 2 + xx, y - d / 2 + yy, 256 * sc, 128 * sc)
            xx += 256 * sc
        }
        yy += 128 * sc
    }
    globalAlpha = 1f
    val sh = createLinearGradient(0f, y - d / 2, 0f, y + d / 2)
    for ((o, c) in Sheen) sh.addColorStop(o, c)
    fillStyle = sh
    fillRect(x - l / 2, y - d / 2, l, d)
    // Wet gloss: a thin specular line along the top.
    fillStyle = White35
    fillRect(x - l / 2 + d * .4f, y - d * .3f, l - d * .8f, max(1f, d * .05f))
    restore()
    // Fuzzy along both edges.
    strokeStyle = fuzz
    lineWidth = max(.6f, 1.1f * u)
    var h = 11L
    fun rnd(): Float {
        h = (h * 48271L) % 2147483647L
        return (h / 2147483647.0).toFloat()
    }
    beginPath()
    var fx = x - l / 2 + d * .35f
    val step = 2.6f * u
    if (step > 0f) {
        while (fx < x + l / 2 - d * .35f) {
            moveTo(fx, y - d / 2 + 1)
            lineTo(fx + (rnd() - .5f) * 2 * u, y - d / 2 - rnd() * 2.2f * u)
            moveTo(fx, y + d / 2 - 1)
            lineTo(fx + (rnd() - .5f) * 2 * u, y + d / 2 + rnd() * 2.2f * u)
            fx += step
        }
    }
    stroke()
    // End caps.
    for (sx in intArrayOf(-1, 1)) {
        val cx = x + sx * (l / 2 - 1.5f * u)
        fillStyle = Cap
        beginPath()
        ellipse(cx, y, 3.2f * u, d * .47f, 0f, 0f, TAU)
        fill()
        fillStyle = White25
        beginPath()
        ellipse(cx - sx * .8f * u, y - d * .15f, 1 * u, d * .22f, 0f, 0f, TAU)
        fill()
    }
    restore()
}
