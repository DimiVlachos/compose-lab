package dev.dimvlachos.lab.core.presentation.components.photocopier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import dev.dimvlachos.lab.core.presentation.ui.PhotocopierScreenColors
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** The LCD's family, a key of the canvas's fonts. */
internal const val CopierMonoFont = "JetBrains Mono"

// The prototype's `system-ui`: no key of the canvas's fonts, so the platform's own.
private const val Ui = "system-ui"

/** The machine's colours, by day and by night, as the prototype's `PL` and `PD`. */
private class Plastic(
    val lip: String,
    val lipEdge: String,
    val body0: String,
    val body1: String,
    val edge: String,
    val hl: String,
    val slot: String,
    val frame: String,
    val tick: String,
    val glass0: String,
    val glass1: String,
    val lid0: String,
    val lid1: String,
    val lidEdge: String,
    val pad: String,
    val key: String,
    val lcd: String,
    val lcdInk: String,
    val brand: String,
)

private val PL =
    Plastic(
        "#d9d5cb",
        "#b9b3a6",
        "#e9e4d8",
        "#cbc4b3",
        "#a9a292",
        "rgba(255,255,255,.7)",
        "#2f2b25",
        "#efece4",
        "#8f897b",
        "#2c3e44",
        "#16232a",
        "#45484e",
        "#33363b",
        "#26282c",
        "#ecebe6",
        "#bfb8a8",
        "#18261b",
        "#9ff2aa",
        "rgba(60,54,44,.55)",
    )
private val PD =
    Plastic(
        "#7d786e",
        "#5f5b53",
        "#8f897c",
        "#6c675d",
        "#55514a",
        "rgba(255,255,255,.22)",
        "#141210",
        "#b5b1a7",
        "#5d5950",
        "#1d2a2f",
        "#0d1519",
        "#36383d",
        "#27292d",
        "#1a1b1e",
        "#b9b8b2",
        "#7f796d",
        "#101a12",
        "#8fe39b",
        "rgba(30,27,22,.6)",
    )

private fun rgba(r: Int, g: Int, b: Int, a: Float) =
    Color(r / 255f, g / 255f, b / 255f, a.coerceIn(0f, 1f))

/**
 * The prototype's `shade()`: a > 0 tints toward white, a < 0 darkens toward black. A [Color], not a
 * CSS string, as it changes every frame the lid moves.
 */
internal fun shade(hex: String, a: Float): Color {
    val n = hex.removePrefix("#").toLong(16)
    fun m(v: Long): Float {
        val c = v.toFloat()
        return (if (a >= 0) c + (255 - c) * a else c * (1 + a)).roundToInt() / 255f
    }
    return Color(m(n shr 16 and 255), m(n shr 8 and 255), m(n and 255))
}

/**
 * Draws the copier and every card in flight, ported call by call from the prototype's
 * `drawCopier()`, `drawLid()`, `drawPanel()`, `drawLight()`, `drawCard()` and `draw()`, on a canvas
 * already scaled to dp. Holds what is made once: the toner flecks, baked per theme at the screen's
 * density, and the ellipsised lines, so a frame measures no text twice.
 */
internal class PhotocopierPainter {
    // Each text's three lines as fitted, by text, for the width they were fitted to.
    private val fitted = HashMap<String, Array<String?>>()
    private var fittedTo = 0f
    private var specks = arrayOfNulls<ImageBitmap>(2)
    private var speckDensity = 0f

    /**
     * Toner flecks, made once: a few dark specks and two faint streaks, the way a tired drum leaves
     * them. The prototype's 360 × 72 canvas, here at the screen's density.
     */
    fun speck(dark: Boolean, density: Float): ImageBitmap {
        if (density != speckDensity) {
            specks = arrayOfNulls(2)
            speckDensity = density
        }
        val i = if (dark) 1 else 0
        specks[i]?.let {
            return it
        }
        val w = (360 * density).roundToInt()
        val h = (72 * density).roundToInt()
        val c = Canvas2D.create(w, h)
        c.scale(density, density)
        var s = 9L
        fun r(): Float {
            s = (s * 16807) % 2147483647
            return (s.toDouble() / 2147483647).toFloat()
        }
        val ink = if (dark) intArrayOf(235, 228, 214) else intArrayOf(38, 34, 28)
        for (k in 0 until 170) {
            val x = r() * 360
            val y = r() * 72
            val rad = .3f + r() * r() * 1.15f
            c.fillStyle = rgba(ink[0], ink[1], ink[2], .2f + r() * .5f)
            c.beginPath()
            c.arc(x, y, rad, 0f, 7f)
            c.fill()
        }
        c.fillStyle = rgba(ink[0], ink[1], ink[2], .07f)
        c.fillRect(0f, 51f, 360f, 1.3f)
        c.fillRect(0f, 14f, 360f, .8f)
        val e = c.createLinearGradient(0f, 0f, 0f, 5f)
        e.addColorStop(0f, rgba(ink[0], ink[1], ink[2], .12f))
        e.addColorStop(1f, rgba(ink[0], ink[1], ink[2], 0f))
        c.fillStyle = e
        c.fillRect(0f, 0f, 360f, 5f)
        val bitmap = c.bitmap
        bitmap.freeze()
        specks[i] = bitmap
        return bitmap
    }

    private fun Canvas2D.rr(x: Float, y: Float, w: Float, h: Float, r: Float) {
        beginPath()
        roundRect(x, y, w, h, max(0f, min(r, min(w / 2, h / 2))))
    }

    private fun Canvas2D.quad(vararg a: Float) {
        beginPath()
        moveTo(a[0], a[1])
        var i = 2
        while (i < a.size) {
            lineTo(a[i], a[i + 1])
            i += 2
        }
        closePath()
    }

    private fun Canvas2D.fit(s: String, max: Float, line: Int): String {
        if (max != fittedTo) {
            fitted.clear()
            fittedTo = max
        }
        val lines = fitted.getOrPut(s) { arrayOfNulls(3) }
        lines[line]?.let {
            return it
        }
        val out =
            if (measureText(s) <= max) s
            else {
                var lo = 0
                var hi = s.length
                while (lo < hi) {
                    val m = (lo + hi + 1) shr 1
                    if (measureText(s.substring(0, m) + "…") <= max) lo = m else hi = m - 1
                }
                s.substring(0, lo).trimEnd() + "…"
            }
        lines[line] = out
        return out
    }

    private fun Canvas2D.cardText(
        d: CopierItem,
        w: Float,
        h: Float,
        a: Float,
        col: PhotocopierScreenColors,
    ) {
        val x = -w / 2 + 15
        val max = w - 15 - 44
        textBaseline = "middle"
        textAlign = "left"
        globalAlpha = a
        fillStyle = col.ink
        font = "650 14.5px $Ui"
        fillText(fit(d.title, max, 0), x, -h / 2 + 12 + 9)
        fillStyle = col.muted
        font = "400 12.8px $Ui"
        fillText(fit(d.summary, max, 1), x, -h / 2 + 12 + 18 + 8.5f)
        globalAlpha = a * .85f
        font = "500 11.5px $Ui"
        fillText(fit(d.meta, max, 2), x, -h / 2 + 12 + 35 + 8)
    }

    /**
     * A list card in flight, drawn at its real size and scaled; flip < 0 shows its back with the
     * text showing through.
     */
    private fun Canvas2D.drawCard(
        st: PhotocopierState,
        f: Flier,
        col: PhotocopierScreenColors,
        dark: Boolean,
        density: Float,
    ) {
        val w = st.cardW
        val h = st.cardH
        val fl = f.flip
        val back = fl < 0
        val s = f.s
        save()
        translate(f.x + w * s / 2, f.y + h * s / 2)
        rotate(f.rot)
        scale(s, s * max(.02f, abs(fl)))
        textResolution = density * s
        if (f.lift > 0) {
            save()
            // Shadows written for the prototype's DPR 2, in the canvas's pixels.
            setShadowColor(rgba(25, 18, 10, (if (dark) .5f else .26f) * f.lift))
            shadowBlur = 22 * f.lift * density / 2
            shadowOffsetY = 12 * f.lift * density / 2
            rr(-w / 2, -h / 2, w, h, 14f)
            fillStyle = col.card
            fill()
            restore()
        }
        rr(-w / 2, -h / 2, w, h, 14f)
        fillStyle = if (back) col.back else col.card
        fill()
        save()
        rr(-w / 2, -h / 2, w, h, 14f)
        clip()
        if (back) {
            save()
            scale(1f, -1f)
            cardText(f.item, w, h, .07f, col)
            restore()
        } else {
            cardText(f.item, w, h, 1f, col)
            globalAlpha = 1f
            fillStyle = col.muted
            for (i in -1..1) {
                beginPath()
                arc(w / 2 - 25 + i * 5, -h / 2 + 25, 1.5f, 0f, 7f)
                fill()
            }
            if (f.warm > 0) {
                fillStyle = rgba(255, 160, 80, .14f * f.warm)
                fillRect(-w / 2, -h / 2, w, h)
            }
            if (f.speck > 0) {
                globalAlpha = .62f * f.speck
                drawImage(speck(dark, density), -w / 2, -h / 2, w, h)
            }
        }
        // Turning away from the light: the face darkens as it goes edge-on.
        val turn = 1 - abs(fl)
        if (turn > .001f) {
            globalAlpha = 1f
            fillStyle = rgba(0, 0, 0, (if (dark) .42f else .3f) * turn)
            fillRect(-w / 2, -h / 2, w, h)
        }
        restore()
        rr(-w / 2 + .5f, -h / 2 + .5f, w - 1, h - 1, 14f)
        lineWidth = 1 / max(.3f, s)
        strokeStyle = if (f.warm > 0) rgba(230, 140, 64, .6f * f.warm) else col.line
        stroke()
        // A 2px paper edge under the card while it is tilted, so edge-on it reads as card stock,
        // not a hairline.
        val ea = (turn * 2.5f).coerceIn(0f, 1f)
        if (ea > .01f) {
            val sy = s * max(.02f, abs(fl))
            val t = 2 / sy
            globalAlpha = ea
            fillStyle = col.line
            fillRect(-w / 2 + 6, h / 2 - .5f / sy, w - 12, t)
            fillStyle = rgba(0, 0, 0, if (dark) .35f else .14f)
            fillRect(-w / 2 + 6, h / 2 - .5f / sy + t * .5f, w - 12, t * .5f)
            globalAlpha = 1f
        }
        restore()
    }

    private fun Canvas2D.drawCopier(
        st: PhotocopierState,
        col: PhotocopierScreenColors,
        dark: Boolean,
        density: Float,
    ) {
        val by = st.by
        val W = st.width
        val H = st.height
        if (by > H + 4) return
        val P = if (dark) PD else PL
        val bx = st.bx
        val bw = st.bw
        val lx = st.lx
        val ly = st.ly
        val lw = st.lidW
        val lh = st.lidH
        val gx = st.gx
        val gy = st.gy
        val gw = st.gw
        val gh = st.gh
        val slotY = st.slotY
        // Soft shadow the machine casts up onto the screen.
        var gr = createLinearGradient(0f, by - 44, 0f, by)
        gr.addColorStop(0f, rgba(20, 14, 8, 0f))
        gr.addColorStop(1f, rgba(20, 14, 8, if (dark) .4f else .17f))
        fillStyle = gr
        fillRect(0f, by - 44, W, 44f)
        // The output lip along the back edge.
        rr(bx + 38, by - 8, bw - 76, 20f, 7f)
        fillStyle = P.lip
        fill()
        lineWidth = 1f
        strokeStyle = P.lipEdge
        stroke()
        // Body.
        rr(bx, by, bw, H - by + 40, 22f)
        gr = createLinearGradient(0f, by, 0f, by + st.bh)
        gr.addColorStop(0f, P.body0)
        gr.addColorStop(1f, P.body1)
        fillStyle = gr
        fill()
        strokeStyle = P.edge
        stroke()
        strokeStyle = P.hl
        lineWidth = 1.5f
        beginPath()
        moveTo(bx + 22, by + 1.5f)
        lineTo(bx + bw - 22, by + 1.5f)
        stroke()
        // The slot the copies come out of; it glows when a copy is held over it.
        rr(bx + 44, slotY - 2.5f, bw - 88, 5f, 2.5f)
        fillStyle = P.slot
        fill()
        if (st.hot > 0) {
            save()
            globalCompositeOperation = "lighter"
            val hg = createRadialGradient(W / 2, slotY, 0f, W / 2, slotY, bw * .5f)
            hg.addColorStop(0f, "rgba(120,255,170,.35)")
            hg.addColorStop(1f, "rgba(120,255,170,0)")
            fillStyle = hg
            fillRect(bx, slotY - 40, bw, 80f)
            restore()
        }
        // Hinges at the back of the lid.
        fillStyle = P.lidEdge
        rr(lx + 26, ly - 6, 34f, 9f, 3f)
        fill()
        rr(lx + lw - 60, ly - 6, 34f, 9f, 3f)
        fill()
        // Platen frame, ruler ticks and the glass.
        rr(lx, ly, lw, lh, 8f)
        fillStyle = P.frame
        fill()
        fillStyle = P.tick
        var i = 0
        while (i <= gw) {
            val big = i % 40 == 0
            fillRect(gx + i, gy - if (big) 7 else 4, 1f, if (big) 7f else 4f)
            i += 8
        }
        i = 0
        while (i <= gh) {
            val big = i % 40 == 0
            fillRect(gx - if (big) 7 else 4, gy + i, if (big) 7f else 4f, 1f)
            i += 8
        }
        rr(gx, gy, gw, gh, 3f)
        gr = createLinearGradient(gx, gy, gx + gw * .4f, gy + gh)
        gr.addColorStop(0f, P.glass0)
        gr.addColorStop(1f, P.glass1)
        fillStyle = gr
        fill()
        save()
        rr(gx, gy, gw, gh, 3f)
        clip()
        fillStyle = "rgba(255,255,255,.06)"
        quad(
            gx + gw * .55f,
            gy,
            gx + gw * .72f,
            gy,
            gx + gw * .42f,
            gy + gh,
            gx + gw * .25f,
            gy + gh,
        )
        fill()
        for (f in st.fliers) if (f.onGlass) drawCard(st, f, col, dark, density)
        restore()
        // Registration arrow in the back-left corner.
        fillStyle = "#e2683c"
        quad(gx - 1, gy - 1, gx + 13, gy - 1, gx - 1, gy + 13)
        fill()
        drawLid(st, P, dark)
        drawPanel(st, P, dark, density)
        drawLight(st, dark)
    }

    private fun Canvas2D.drawLid(st: PhotocopierState, P: Plastic, dark: Boolean) {
        val lx = st.lx
        val ly = st.ly
        val lw = st.lidW
        val lh = st.lidH
        val th = st.lid * 1.83f
        val ct = cos(th)
        val sn = sin(th)
        val fy = ly + lh * ct
        val wid = lw * (1 + .07f * sn)
        val fx = lx - (wid - lw) / 2
        if (ct > .001f) {
            if (sn > .01f) {
                val sg = createLinearGradient(0f, fy, 0f, fy + 30 * sn)
                sg.addColorStop(0f, rgba(0, 0, 0, .36f * sn))
                sg.addColorStop(1f, rgba(0, 0, 0, 0f))
                fillStyle = sg
                fillRect(lx, fy, lw, 30 * sn)
            }
            quad(lx, ly, lx + lw, ly, fx + wid, fy, fx, fy)
            val gr = createLinearGradient(0f, ly, 0f, fy + 1)
            gr.addColorStop(0f, P.lid1)
            if (sn > 0) gr.addColorStop(1f, shade(P.lid0, .14f * sn))
            else gr.addColorStop(1f, P.lid0)
            fillStyle = gr
            fill()
            // Fine molded ribs across the lid, and a recessed grip at the front.
            save()
            quad(lx, ly, lx + lw, ly, fx + wid, fy, fx, fy)
            clip()
            fillStyle = "rgba(255,255,255,.035)"
            var y = 0f
            while (y < lh) {
                fillRect(lx - 20, ly + y * ct, wid + 40, 1f)
                y += 5
            }
            restore()
            val gh = 7 * max(ct, .25f)
            rr(fx + wid / 2 - 30, fy - gh - 5 * ct, 60f, gh, 3f)
            fillStyle = P.lidEdge
            fill()
            quad(fx, fy, fx + wid, fy, fx + wid, fy + 4 + 5 * sn, fx, fy + 4 + 5 * sn)
            fillStyle = P.lidEdge
            fill()
            if (ct > .6f) {
                fillStyle = "rgba(255,255,255,.18)"
                font = "700 9px $Ui"
                textBaseline = "middle"
                textAlign = "left"
                fillText("QUILLMARK", lx + 14, ly + 14 * ct)
            }
        } else {
            // Past upright: the white underside shows above the hinge.
            quad(lx, ly, lx + lw, ly, fx + wid, fy, fx, fy)
            fillStyle = P.lidEdge
            fill()
            if (ly - fy > 16) {
                // The foam pad: in the lid's shadow at the hinge, catching light toward its far
                // edge, with a 1px inner shadow round its rim.
                val pg = createLinearGradient(0f, ly - 4, 0f, fy + 6)
                pg.addColorStop(0f, shade(P.pad, -.2f))
                pg.addColorStop(.55f, shade(P.pad, -.05f))
                pg.addColorStop(1f, P.pad)
                pad(lx, ly, lw, fx, fy, wid)
                fillStyle = pg
                fill()
                save()
                pad(lx, ly, lw, fx, fy, wid)
                clip()
                pad(lx, ly, lw, fx, fy, wid)
                lineWidth = 2f
                strokeStyle = if (dark) "rgba(0,0,0,.3)" else "rgba(0,0,0,.16)"
                stroke()
                restore()
            }
            quad(fx, fy, fx + wid, fy, fx + wid, fy - 4 - 5 * sn, fx, fy - 4 - 5 * sn)
            fillStyle = P.lid0
            fill()
        }
    }

    private fun Canvas2D.pad(lx: Float, ly: Float, lw: Float, fx: Float, fy: Float, wid: Float) =
        quad(lx + 8, ly - 4, lx + lw - 8, ly - 4, fx + wid - 9, fy + 6, fx + 9, fy + 6)

    private fun Canvas2D.drawPanel(
        st: PhotocopierState,
        P: Plastic,
        dark: Boolean,
        density: Float,
    ) {
        val H = st.height
        val py = st.ly + st.lidH + 16
        val ph = min(36f, H - py - 12)
        if (ph < 14) return
        val bx = st.bx
        val bw = st.bw
        rr(bx + 26, py, 84f, ph, 6f)
        fillStyle = P.lcd
        fill()
        save()
        fillStyle = P.lcdInk
        shadowColor = P.lcdInk
        shadowBlur = 6 * density / 2
        font = "600 ${(ph * .4f).roundToInt()}px $CopierMonoFont"
        textBaseline = "middle"
        textAlign = "right"
        fillTextWithShadow(
            if (st.scan > 0) "COPYING" else if (st.busy) "READY" else "1",
            bx + 26 + 76,
            py + ph / 2 + 1,
        )
        restore()
        for (i in 0 until 3) {
            rr(bx + bw / 2 - 46 + i * 32, py + ph / 2 - 9, 24f, 18f, 5f)
            fillStyle = P.key
            fill()
        }
        val sx = bx + bw - 50
        val sy = py + ph / 2
        val r = ph * .46f
        if (st.start > 0) {
            save()
            globalCompositeOperation = if (dark) "lighter" else "source-over"
            val sg = createRadialGradient(sx, sy, 0f, sx, sy, r * 3)
            sg.addColorStop(0f, if (dark) "rgba(90,255,140,.5)" else "rgba(60,215,110,.4)")
            sg.addColorStop(1f, "rgba(90,255,140,0)")
            fillStyle = sg
            fillRect(sx - r * 3, sy - r * 3, r * 6, r * 6)
            restore()
        }
        beginPath()
        arc(sx, sy, r, 0f, 7f)
        fillStyle = if (st.start > 0) "#45d27a" else "#2f9a57"
        fill()
        strokeStyle = "rgba(0,0,0,.25)"
        lineWidth = 1f
        stroke()
        fillStyle = "rgba(255,255,255,.9)"
        quad(sx, sy - r * .42f, sx + r * .42f, sy, sx, sy + r * .42f, sx - r * .42f, sy)
        fill()
        if (py + ph + 18 < H - 4) {
            fillStyle = P.brand
            font = "700 9px $Ui"
            textAlign = "left"
            textBaseline = "middle"
            fillText("QUILLMARK  C·210", bx + 26, py + ph + 14)
        }
    }

    /** While the lid is shut, the scanning bar's light leaks out of the seams and across. */
    private fun Canvas2D.drawLight(st: PhotocopierState, dark: Boolean) {
        if (!(st.scan > 0) || st.lid > .04f) return
        val I = st.scan
        val y = st.gy + st.bar * st.gh
        val lx = st.lx
        val lw = st.lidW
        val W = st.width
        // Added light washes out on a pale screen, so in the light theme it is painted in a
        // deeper green instead.
        val L = if (dark) intArrayOf(110, 255, 165) else intArrayOf(40, 205, 105)
        val E = if (dark) intArrayOf(170, 255, 200) else intArrayOf(70, 225, 130)
        save()
        globalCompositeOperation = if (dark) "lighter" else "source-over"
        val gr = createLinearGradient(0f, y - 70, 0f, y + 70)
        gr.addColorStop(0f, rgba(L[0], L[1], L[2], 0f))
        gr.addColorStop(.5f, rgba(L[0], L[1], L[2], (if (dark) .16f else .13f) * I))
        gr.addColorStop(1f, rgba(L[0], L[1], L[2], 0f))
        fillStyle = gr
        fillRect(0f, y - 70, W, 140f)
        for (ex in floatArrayOf(lx, lx + lw)) {
            val rg = createRadialGradient(ex, y, 0f, ex, y, 90f)
            rg.addColorStop(0f, rgba(E[0], E[1], E[2], (if (dark) .6f else .62f) * I))
            rg.addColorStop(1f, rgba(E[0], E[1], E[2], 0f))
            fillStyle = rg
            fillRect(ex - 90, y - 90, 180f, 180f)
        }
        fillStyle = rgba(225, 255, 235, .95f * I)
        fillRect(lx - 1.5f, y - 16, 3f, 32f)
        fillRect(lx + lw - 1.5f, y - 16, 3f, 32f)
        if (st.bar > .7f) {
            val a = (st.bar - .7f) / .3f * I
            fillStyle = rgba(200, 255, 220, .8f * a)
            fillRect(lx + 12, st.ly + st.lidH - 1, lw - 24, 2f)
            val fg = createLinearGradient(0f, st.ly + st.lidH, 0f, st.ly + st.lidH + 30)
            fg.addColorStop(0f, rgba(L[0], L[1], L[2], .3f * a))
            fg.addColorStop(1f, rgba(L[0], L[1], L[2], 0f))
            fillStyle = fg
            fillRect(lx, st.ly + st.lidH, lw, 30f)
        }
        restore()
    }

    /** The overlay: the copier, then every card in flight over it, then the slot's front lip. */
    fun draw(
        c: Canvas2D,
        st: PhotocopierState,
        col: PhotocopierScreenColors,
        dark: Boolean,
        density: Float,
    ) {
        with(c) {
            textResolution = density
            drawCopier(st, col, dark, density)
            val slotY = st.slotY
            var inSlot = false
            for (f in st.fliers) {
                if (f.onGlass) continue
                save()
                if (f.inSlot) {
                    inSlot = true
                    beginPath()
                    rect(0f, 0f, st.width, slotY)
                    clip()
                }
                drawCard(st, f, col, dark, density)
                restore()
            }
            // The slot's front lip sits over a copy going in or coming out.
            if (inSlot) {
                rr(st.bx + 44, slotY - .5f, st.bw - 88, 3f, 1.5f)
                fillStyle = (if (dark) PD else PL).slot
                fill()
            }
        }
    }
}
