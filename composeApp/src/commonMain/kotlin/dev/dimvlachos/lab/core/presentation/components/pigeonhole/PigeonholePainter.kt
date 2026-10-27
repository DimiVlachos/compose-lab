package dev.dimvlachos.lab.core.presentation.components.pigeonhole

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.LH
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.LW
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.Rail
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.ease
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.hash
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.lerp
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.penName
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.smooth
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** The row's card colours, as the prototype's `--a-card`, `--a-ink` and `--a-muted`. */
internal class LetterInks(val card: Color, val ink: Color, val mut: Color) {
    internal companion object {
        val Day = LetterInks(Color(255, 253, 249), Color(37, 34, 31), Color(123, 114, 104))
        val Dusk = LetterInks(Color(35, 32, 29), Color(238, 232, 224), Color(168, 158, 147))
    }
}

/** The fonts the canvas names: the system's sans, the brass plates' mono and the pen. */
internal const val SysFont = "sys"
internal const val MonoFont = "mono"
internal const val PenFont = "pen"

// The paper's colours, as the prototype's PAPER, PAPER_BACK, PINK and PMUT.
private val Paper = Color(251, 247, 236)
private val PaperBack = Color(240, 231, 211)
private val PInk = Color(42, 40, 48)
private val PMut = Color(128, 119, 106)
private val BackFold = Color(200, 186, 160)
private val TopFold = Color(214, 202, 180)

// Each sender has a stamp of their own colour.
private val Stamps =
    listOf("#b8432f", "#2f5d8a", "#3f7a4a", "#c08a2a", "#6b4a8a", "#2b7a78", "#a0525e")

// The prototype's mix(): rounded channels between [a] and [b].
private fun mix(a: Color, b: Color, t: Float): Color {
    fun ch(x: Float, y: Float) = floor(lerp(x * 255, y * 255, t) + .5f).toInt()
    return Color(ch(a.red, b.red), ch(a.green, b.green), ch(a.blue, b.blue))
}

private fun rgba(r: Int, g: Int, b: Int, a: Float) =
    Color(r, g, b, (a.coerceIn(0f, 1f) * 255).roundToInt())

/**
 * Draws the rack and the letters as the prototype does, on its two canvases: [drawRack] under the
 * rack's buttons (the oak, the cubby holes, each cubby's stack and the letters going in, the rails
 * and brass plates), and [drawFlying] above everything (the letters in the air, the one in hand
 * last). Units are dp; the canvas comes scaled by [density]. The oak, the rails and each folded
 * letter are baked once into images, as the prototype's offscreen canvases; gradients that only
 * move with the rack are made once per layout and drawn under the rack's offset.
 */
internal class PigeonholePainter(
    private val density: Float,
    private val measurer: TextMeasurer,
    private val fonts: Map<String, FontFamily>,
) {
    private var laid = -1
    private var rackImg: Canvas2D? = null
    private var railImg: Canvas2D? = null
    private val sprites = HashMap<String, Canvas2D>()
    private var k = 1f
    private var rowW = 300f
    private var rowH = 77f
    private var fs = 1f
    private var width = 0f

    // Per layout, in the rack's own coordinates.
    private val glow = arrayOfNulls<Gradient2D>(6)
    private val contact = arrayOfNulls<Gradient2D>(6)
    private var plate: Gradient2D? = null
    private var plateHot: Gradient2D? = null
    private var monoFont = ""
    private var dash = FloatArray(2)
    private val names = arrayOfNulls<String>(6)
    private val stack = arrayOfNulls<Mail>(3)

    private fun ensure(s: PigeonholeState) {
        if (laid == s.layoutVersion && rackImg != null) return
        laid = s.layoutVersion
        k = s.k
        rowW = s.rowW
        rowH = s.rowH
        fs = s.textScale
        width = s.width
        sprites.clear()
        plates.clear()
        buildRack(s)
        val top = 0f
        for (i in 0 until 6) {
            val rx = s.cubX(i)
            val rw = s.cubW()
            val rt = s.railTop(i, top)
            glow[i] =
                Gradient2D.radial(
                        rx + rw / 2,
                        rt - 10 * k,
                        4 * k,
                        rx + rw / 2,
                        rt - 10 * k,
                        rw * .75f,
                    )
                    .apply {
                        addColorStop(0f, "rgba(255,196,120,.45)")
                        addColorStop(1f, "rgba(255,170,90,0)")
                    }
            contact[i] =
                Gradient2D.linear(0f, rt - 8 * k, 0f, rt).apply {
                    addColorStop(0f, "rgba(0,0,0,0)")
                    addColorStop(1f, "rgba(0,0,0,.35)")
                }
            names[i] = s.cubbies[i].name.uppercase()
        }
        val ph = 19 * k
        plate = plateGradient(ph, false)
        plateHot = plateGradient(ph, true)
        monoFont = "600 ${7.4f * k}px $MonoFont"
        dash = floatArrayOf(1.1f * k, 1.1f * k)
    }

    private fun plateGradient(ph: Float, hot: Boolean) =
        Gradient2D.linear(0f, -ph / 2, 0f, ph / 2).apply {
            addColorStop(0f, if (hot) "#fff0b8" else "#efd38c")
            addColorStop(.5f, if (hot) "#e8c060" else "#c9a24a")
            addColorStop(1f, "#94712a")
        }

    private fun Canvas2D.setup(): Canvas2D {
        textMeasurer = measurer
        fonts = this@PigeonholePainter.fonts
        textResolution = density
        return this
    }

    // ---------- Rack drawing ----------

    // The prototype's Park–Miller rnd().
    private class Rnd(seed: Int) {
        private var s = seed.toLong()

        fun next(): Float {
            s = (s * 16807) % 2147483647
            return ((s - 1) / 2147483646.0).toFloat()
        }
    }

    private fun rr(c: Canvas2D, x: Float, y: Float, w: Float, h: Float, r0: Float) {
        val r = min(r0, min(w / 2, h / 2))
        c.beginPath()
        c.moveTo(x + r, y)
        c.arcTo(x + w, y, x + w, y + h, r)
        c.arcTo(x + w, y + h, x, y + h, r)
        c.arcTo(x, y + h, x, y, r)
        c.arcTo(x, y, x + w, y, r)
        c.closePath()
    }

    private fun grain(
        c: Canvas2D,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        n: Int,
        seed: Int,
        color: Color,
        vertical: Boolean,
    ) {
        val r = Rnd(seed)
        c.save()
        c.beginPath()
        c.rect(x, y, w, h)
        c.clip()
        repeat(n) {
            val o = r.next() * (if (vertical) w else h)
            val amp = (1 + r.next() * 3) * k
            val fr = (.01f + r.next() * .03f) / k
            val ph = r.next() * 6
            c.strokeStyle = color.copy(alpha = .05f + r.next() * .12f)
            c.lineWidth = (.5f + r.next() * 1.1f) * k
            c.beginPath()
            val l = if (vertical) h else w
            var t = 0f
            while (t <= l) {
                val d = o + sin(t * fr + ph) * amp + sin(t * fr * 3.1f + ph * 2) * amp * .3f
                if (vertical) {
                    if (t == 0f) c.moveTo(x + d, y + t) else c.lineTo(x + d, y + t)
                } else {
                    if (t == 0f) c.moveTo(x + t, y + d) else c.lineTo(x + t, y + d)
                }
                t += 6 * k
            }
            c.stroke()
        }
        c.restore()
    }

    private fun buildRack(s: PigeonholeState) {
        val w = s.width
        val rackH = s.rackH
        val dpr = density
        val img = Canvas2D.create((w * dpr).roundToInt(), (rackH * dpr).roundToInt())
        val c = img
        c.scale(dpr, dpr)
        // The oak cabinet front, rounded at the top like a sheet.
        rr(c, 0f, 0f, w, rackH + 30 * k, 20 * k)
        c.save()
        c.clip()
        val g = c.createLinearGradient(0f, 0f, 0f, rackH)
        g.addColorStop(0f, "#c58a55")
        g.addColorStop(.5f, "#ad723f")
        g.addColorStop(1f, "#955b2f")
        c.fillStyle = g
        c.fillRect(0f, 0f, w, rackH)
        grain(c, 0f, 0f, w, rackH, 70, 7, Color(80, 38, 12), false)
        c.fillStyle = "rgba(255,232,196,.55)"
        c.fillRect(0f, 0f, w, 1.2f * k)
        c.fillStyle = "rgba(60,28,8,.32)"
        rr(c, w / 2 - 20 * k, 7 * k, 40 * k, 4.5f * k, 2.25f * k)
        c.fill()
        // The cubby holes: a dark walnut back wall, shadowed where the frame overhangs it.
        for (i in 0 until 6) {
            val rx = s.cubX(i)
            val ry = s.cubY(i, 0f)
            val rw = s.cubW()
            val rh = s.cubH()
            val ig = c.createLinearGradient(0f, ry, 0f, ry + rh)
            ig.addColorStop(0f, "#24150b")
            ig.addColorStop(1f, "#3d2414")
            c.fillStyle = ig
            rr(c, rx, ry, rw, rh, 3 * k)
            c.fill()
            grain(c, rx, ry, rw, rh, 9, 31 + i, Color(120, 70, 30), true)
            c.save()
            rr(c, rx, ry, rw, rh, 3 * k)
            c.clip()
            var sg = c.createLinearGradient(0f, ry, 0f, ry + 16 * k)
            sg.addColorStop(0f, "rgba(0,0,0,.55)")
            sg.addColorStop(1f, "rgba(0,0,0,0)")
            c.fillStyle = sg
            c.fillRect(rx, ry, rw, 16 * k)
            sg = c.createLinearGradient(rx, 0f, rx + 10 * k, 0f)
            sg.addColorStop(0f, "rgba(0,0,0,.4)")
            sg.addColorStop(1f, "rgba(0,0,0,0)")
            c.fillStyle = sg
            c.fillRect(rx, ry, 10 * k, rh)
            sg = c.createLinearGradient(rx + rw, 0f, rx + rw - 8 * k, 0f)
            sg.addColorStop(0f, "rgba(0,0,0,.3)")
            sg.addColorStop(1f, "rgba(0,0,0,0)")
            c.fillStyle = sg
            c.fillRect(rx + rw - 8 * k, ry, 8 * k, rh)
            c.restore()
            // A worn bevel where the frame meets the hole.
            c.strokeStyle = "rgba(255,225,180,.28)"
            c.lineWidth = 1 * k
            rr(c, rx - .5f * k, ry - .5f * k, rw + 1 * k, rh + 1 * k, 3.5f * k)
            c.stroke()
        }
        c.restore()
        img.bitmap.freeze()
        rackImg = img
        // The rails go on top of the letters, so they are a second image.
        val rail = Canvas2D.create((w * dpr).roundToInt(), (rackH * dpr).roundToInt())
        rail.scale(dpr, dpr)
        for (i in 0 until 6) {
            val rx = s.cubX(i)
            val rw = s.cubW()
            val rt = s.railTop(i, 0f)
            val rg = rail.createLinearGradient(0f, rt, 0f, rt + Rail * k)
            rg.addColorStop(0f, "#cf9a63")
            rg.addColorStop(1f, "#a56c3b")
            rail.fillStyle = rg
            rail.fillRect(rx - 1, rt, rw + 2, Rail * k)
            grain(rail, rx - 1, rt, rw + 2, Rail * k, 4, 91 + i, Color(90, 45, 15), false)
            rail.fillStyle = "rgba(255,238,205,.6)"
            rail.fillRect(rx - 1, rt, rw + 2, 1 * k)
            rail.fillStyle = "rgba(60,28,8,.35)"
            rail.fillRect(rx - 1, rt + Rail * k - 1 * k, rw + 2, 1 * k)
        }
        rail.bitmap.freeze()
        railImg = rail
    }

    /** The rack, its cubbies' stacks, the letters going in, the rails and brass plates. */
    fun drawRack(c0: Canvas2D, s: PigeonholeState, inks: LetterInks) {
        if (s.width <= 0f) return
        // Baked on the screen's first frame, not on the frame a letter is first picked up.
        ensure(s)
        val top = s.rackTop()
        if (top >= s.height) return
        val c = c0.setup()
        val rack = rackImg ?: return
        val rail = railImg ?: return
        c.drawImage(rack, 0f, top, s.width, s.rackH)
        val cw0 = s.cubW()
        val ch0 = s.cubH()
        for (i in 0 until 6) {
            val rx = s.cubX(i)
            val ryL = s.cubY(i, 0f)
            val rtL = s.railTop(i, 0f)
            val hot = s.hot(i)
            c.save()
            c.translate(0f, top)
            rr(c, rx, ryL, cw0, ch0, 3 * k)
            c.clip()
            if (hot) {
                c.fillStyle = glow[i]!!
                c.fillRect(rx, ryL, cw0, ch0)
            }
            // The stack, back to front. While a letter comes in or goes out, the rest ease back or
            // forward one place.
            val n = s.cubVisible(i, stack)
            val shift = s.stackShift(i)
            for (j in min(n, 3) - 1 downTo 0) {
                val m = stack[j]!!
                val h = hash(m.id)
                val je = j + shift
                drawLetter(
                    c,
                    m,
                    s.slotX(i) + ((h % 3) - 1) * 6 * k * min(je, 1f),
                    s.slotY(i, je, 0f),
                    f = 1f,
                    s = .86f,
                    a = ((h % 5) - 2) * .03f,
                    lift = 0f,
                    dim = min(je, 2f) * .16f,
                    pm0 = Float.NaN,
                    inks = inks,
                )
            }
            c.translate(0f, -top)
            for (l in s.letters) {
                if (l.inCubby && l.cub == i) drawLetter(c, l, inks)
            }
            c.translate(0f, top)
            // A contact shadow where the letters meet the rail.
            c.fillStyle = contact[i]!!
            c.fillRect(rx, rtL - 8 * k, cw0, 8 * k)
            c.restore()
            // The rail across the front, then its brass label holder.
            val rt = rtL + top
            c.drawImage(
                rail.bitmap,
                (rx - 1) * density,
                rtL * density,
                (cw0 + 2) * density,
                Rail * k * density,
                rx - 1,
                rt,
                cw0 + 2,
                Rail * k,
            )
            val bs = s.plateScale(i)
            val pw = min(78 * k, cw0 - 10 * k)
            val ph = 19 * k
            val count = s.cubCount(i)
            val open = s.cubbies[i].id == s.view
            val img = plateImage(i, pw, ph, hot, open, count)
            val iw = pw + 2 * PlateM * k
            val ih = ph + 2 * PlateM * k
            c.save()
            c.translate(rx + cw0 / 2, rt + Rail * k / 2)
            c.scale(bs, bs)
            c.drawImage(img.bitmap, -iw / 2, -ih / 2, iw, ih)
            c.restore()
        }
    }

    // Each cubby's brass label holder, its name and its count, baked as it is drawn until one of
    // them changes, so a frame draws one image per plate rather than its gradient, rivets and text.
    private val plates = HashMap<Int, Canvas2D>()

    private fun plateImage(
        i: Int,
        pw: Float,
        ph: Float,
        hot: Boolean,
        open: Boolean,
        n: Int,
    ): Canvas2D {
        val key = (n * 6 + i) * 4 + (if (hot) 2 else 0) + (if (open) 1 else 0)
        plates[key]?.let {
            return it
        }
        if (plates.size >= 64) plates.clear()
        val iw = pw + 2 * PlateM * k
        val ih = ph + 2 * PlateM * k
        val c = Canvas2D.create(ceil(iw * density).toInt(), ceil(ih * density).toInt()).setup()
        c.scale(density, density)
        c.translate(iw / 2, ih / 2)
        c.fillStyle = (if (hot) plateHot else plate)!!
        rr(c, -pw / 2, -ph / 2, pw, ph, 3 * k)
        c.fill()
        c.strokeStyle = "rgba(70,45,10,.6)"
        c.lineWidth = .8f * k
        c.stroke()
        for (side in 0..1) {
            val sx = if (side == 0) -pw / 2 + 4.5f * k else pw / 2 - 4.5f * k
            c.fillStyle = "#7b5a1e"
            c.beginPath()
            c.arc(sx, 0f, 1.7f * k, 0f, 7f)
            c.fill()
            c.fillStyle = "rgba(255,240,200,.7)"
            c.beginPath()
            c.arc(sx - .4f * k, -.5f * k, .7f * k, 0f, 7f)
            c.fill()
        }
        val cw = pw - 15 * k
        val chh = ph - 6 * k
        c.fillStyle = if (open) "#fff7e0" else "#f6eedb"
        c.fillRect(-cw / 2, -chh / 2, cw, chh)
        c.fillStyle = "rgba(0,0,0,.12)"
        c.fillRect(-cw / 2, -chh / 2, cw, .8f * k)
        c.font = monoFont
        c.textBaseline = "middle"
        c.fillStyle = "#2c2620"
        c.textAlign = "left"
        c.fillText(names[i]!!, -cw / 2 + 3 * k, .4f * k)
        c.fillStyle = "#a8321f"
        c.textAlign = "right"
        c.fillText(count(n), cw / 2 - 3 * k, .4f * k)
        c.bitmap.freeze()
        plates[key] = c
        return c
    }

    private val counts = Array(100) { it.toString() }

    private fun count(n: Int) = if (n in counts.indices) counts[n] else n.toString()

    /** The letters in the air, the one in hand last, over everything. */
    fun drawFlying(c0: Canvas2D, s: PigeonholeState, inks: LetterInks) {
        if (s.width <= 0f || s.letters.isEmpty()) return
        ensure(s)
        val c = c0.setup()
        val carried = s.carry?.letter
        for (l in s.letters) if (!l.inCubby && l !== carried) drawLetter(c, l, inks)
        if (carried != null) drawLetter(c, carried, inks)
    }

    // ---------- The letter ----------

    private fun drawLetter(c: Canvas2D, l: Letter, inks: LetterInks) =
        drawLetter(c, l.mail, l.x, l.y, l.f, l.s, l.a, l.lift, l.dim, l.pm, inks)

    private fun sprite(m: Mail, inks: LetterInks): Canvas2D =
        sprites.getOrPut(m.id) {
            val sw = LW * k + 28 * k
            val sh = LH * k + 28 * k
            val sp = Canvas2D.create(ceil(sw * density).toInt(), ceil(sh * density).toInt())
            sp.setup()
            sp.scale(density, density)
            drawLetter(sp, m, sw / 2, sh / 2, 1f, 1f, 0f, 0f, 0f, Float.NaN, inks, fresh = true)
            sp.bitmap.freeze()
            sp
        }

    // fold 0 is the row's card; up to 0.38 it narrows into a paper page; then the bottom third
    // folds up and the top third down.
    private fun drawLetter(
        c: Canvas2D,
        mail: Mail,
        x: Float,
        y: Float,
        f: Float,
        s: Float,
        a: Float,
        lift: Float,
        dim: Float,
        pm0: Float,
        inks: LetterInks,
        fresh: Boolean = false,
    ) {
        val m = smooth((f / .38f).coerceIn(0f, 1f))
        if (f >= .999f && !fresh && lift == 0f) {
            val sp = sprite(mail, inks)
            val sw = LW * k + 28 * k
            val sh = LH * k + 28 * k
            c.save()
            c.translate(x, y)
            c.rotate(a)
            c.scale(s, s)
            c.drawImage(sp, -sw / 2, -sh / 2, sw, sh)
            if (dim != 0f) {
                c.fillStyle = rgba(20, 10, 4, dim)
                c.fillRect(-LW * k / 2, -LH * k / 2, LW * k, LH * k)
            }
            c.restore()
            return
        }
        val item = mail.item
        val w = lerp(rowW, LW * k, m)
        val ph = lerp(rowH / 3, LH * k, m)
        val fb = ease(((f - .4f) / .3f).coerceIn(0f, 1f))
        val ft = ease(((f - .64f) / .36f).coerceIn(0f, 1f))
        val cb = cos(fb * PI.toFloat())
        val ct = cos(ft * PI.toFloat())
        // The face turns over on its own short clock, and the ink switches at the half-way point so
        // the text never sinks into the face.
        val pm = if (!pm0.isNaN()) smooth(pm0) else smooth((f / .16f).coerceIn(0f, 1f))
        val face = mix(inks.card, Paper, pm)
        val ink = if (pm < .5f) inks.ink else PInk
        val mut = if (pm < .5f) inks.mut else PMut
        val pad = lerp(14f, 6 * k, m)
        val rad = lerp(14f, 2 * k, m)
        val topY = if (ct >= 0) -ph / 2 - ph * ct else -ph / 2
        val botY = if (cb >= 0) ph / 2 + ph * cb else ph / 2
        c.save()
        c.translate(x, y)
        c.rotate(a)
        c.scale(s, s)
        // The sheet with its shadow; higher off the page while it's carried.
        c.save()
        c.setShadowColor(rgba(30, 16, 6, .18f + .14f * lift))
        c.shadowBlur = (5 + 14 * lift) * k * density
        c.shadowOffsetY = (2 + 7 * lift) * k * density
        c.fillStyle = face
        rr(c, -w / 2, topY, w, botY - topY, rad)
        c.fill()
        c.restore()
        if (m < 1) {
            c.strokeStyle = rgba(120, 100, 80, .25f * (1 - m))
            c.lineWidth = 1f
            rr(c, -w / 2 + .5f, topY + .5f, w - 1, botY - topY - 1, rad)
            c.stroke()
        }
        val s1 = lerp(14.5f * fs, 7.6f * k, m)
        val s2 = lerp(13.5f * fs, 7 * k, m)
        val s3 = lerp(13f * fs, 6.6f * k, m)
        val tx = -w / 2 + pad
        val inner = w - 2 * pad
        c.textBaseline = "middle"
        c.textAlign = "left"
        // Middle third: the subject.
        c.fillStyle = ink
        text(c, fit(item.subj, Face.Subject, s2, inner), Face.Subject, s2, tx, 0f)
        // Bottom third, about the lower crease.
        c.save()
        c.translate(0f, ph / 2)
        if (cb >= 0) {
            c.scale(1f, max(cb, .001f))
            c.fillStyle = mut
            text(
                c,
                fit(item.prev, Face.Preview, s3, inner),
                Face.Preview,
                s3,
                tx,
                ph / 2 - 6 * (1 - m),
            )
            if (m > .9f) {
                c.strokeStyle = rgba(110, 100, 90, .25f * m)
                c.lineWidth = .7f
                for (i in 1..2) {
                    c.beginPath()
                    c.moveTo(tx, ph / 2 + i * 6 * k)
                    c.lineTo(tx + inner * (if (i == 2) .55f else .9f), ph / 2 + i * 6 * k)
                    c.stroke()
                }
            }
            c.fillStyle = rgba(40, 20, 5, (1 - cb) * .3f)
            c.fillRect(-w / 2, 0f, w, ph)
        } else {
            c.scale(1f, -cb)
            c.fillStyle = mix(PaperBack, BackFold, (1 + cb) * .6f)
            c.fillRect(-w / 2, -ph, w, ph)
            c.fillStyle = rgba(255, 255, 255, .25f * -cb)
            c.fillRect(-w / 2, -ph, w, 1f)
        }
        c.restore()
        if (fb > 0 && fb < 1) {
            c.strokeStyle = "rgba(80,60,40,.25)"
            c.lineWidth = 1f
            c.beginPath()
            c.moveTo(-w / 2, ph / 2)
            c.lineTo(w / 2, ph / 2)
            c.stroke()
        }
        // Top third, about the upper crease: sender and time on the inside, the sender's name
        // written on the outside.
        c.save()
        c.translate(0f, -ph / 2)
        if (ct >= 0) {
            c.scale(1f, max(ct, .001f))
            val ty = -ph / 2 + 6 * (1 - m)
            val ts = lerp(12f * fs, 7f, m)
            val tw = if (m < .8f) textWidth(item.time, Face.Time) * ts / RefPx else 0f
            c.fillStyle = ink
            val from = fit(item.from, Face.From, s1, inner - (if (tw > 0) tw + 10 else 0f))
            text(c, from, Face.From, s1, tx, ty)
            if (tw > 0) {
                c.fillStyle = mut
                c.globalAlpha = 1 - m / .8f
                c.textAlign = "right"
                text(c, item.time, Face.Time, ts, w / 2 - pad, ty)
                c.textAlign = "left"
                c.globalAlpha = 1f
            }
            c.fillStyle = rgba(40, 20, 5, (1 - ct) * .3f)
            c.fillRect(-w / 2, -ph, w, ph)
        } else {
            c.scale(1f, -ct)
            c.fillStyle = mix(Paper, TopFold, (1 + ct) * .7f)
            c.fillRect(-w / 2, 0f, w, ph)
            // The outside: the sender's stamp in the top corner, their first name in pen, and a
            // postmark-ish rule, all in the top half so they show above the rail.
            val sw = 10 * k
            val sh = 12 * k
            val sx = w / 2 - 4 * k - sw
            val sy = 3.5f * k
            val sc = Stamps[hash(item.from) % Stamps.size]
            c.globalAlpha = -ct
            c.fillStyle = "#fffaf0"
            c.fillRect(sx, sy, sw, sh)
            c.strokeStyle = "rgba(150,130,100,.55)"
            c.lineWidth = .9f * k
            c.setLineDash(dash)
            c.beginPath()
            c.rect(sx, sy, sw, sh)
            c.stroke()
            c.setLineDash(NoDash)
            c.fillStyle = sc
            c.fillRect(sx + 1.6f * k, sy + 1.6f * k, sw - 3.2f * k, sh - 3.2f * k)
            c.fillStyle = "rgba(255,250,235,.8)"
            c.beginPath()
            c.arc(sx + sw / 2, sy + sh * .45f, 1.8f * k, 0f, 7f)
            c.fill()
            c.strokeStyle = "rgba(36,40,80,.35)"
            c.lineWidth = .7f * k
            c.beginPath()
            c.arc(sx + 1 * k, sy + sh * .8f, 4.5f * k, -.4f, 1.3f)
            c.stroke()
            c.globalAlpha = 1f
            c.fillStyle = rgba(36, 40, 80, .88f * -ct)
            c.textAlign = "left"
            val pen = 16.5f * k
            val name = fit(penName(item.from), Face.Pen, pen, sx - 2 * k - (-w / 2 + 6 * k))
            text(c, name, Face.Pen, pen, -w / 2 + 6 * k, ph * .4f)
            c.strokeStyle = rgba(36, 40, 80, .35f * -ct)
            c.lineWidth = .8f * k
            c.beginPath()
            c.moveTo(-w / 2 + 6 * k, ph * .62f)
            c.lineTo(w * .22f, ph * .62f)
            c.stroke()
            c.fillStyle = rgba(255, 255, 255, .5f * -ct)
            c.fillRect(-w / 2, ph - 1, w, 1f)
            c.textAlign = "left"
        }
        c.restore()
        if (ft > 0 && ft < 1) {
            c.strokeStyle = "rgba(80,60,40,.25)"
            c.lineWidth = 1f
            c.beginPath()
            c.moveTo(-w / 2, -ph / 2)
            c.lineTo(w / 2, -ph / 2)
            c.stroke()
        }
        if (dim != 0f) {
            c.fillStyle = rgba(20, 10, 4, dim)
            rr(c, -w / 2, topY, w, botY - topY, rad)
            c.fill()
        }
        c.restore()
    }

    // ---------- Fitting text ----------

    // Each face's font at RefPx, for [text].
    private val refFonts = Face.entries.map { "${it.weight} ${RefPx}px ${it.family}" }

    // [str] at [size], laid out at RefPx and drawn scaled: a size that animates every frame keeps
    // one layout instead of laying the text out again on every frame.
    private fun text(c: Canvas2D, str: String, face: Face, size: Float, x: Float, y: Float) {
        val q = size / RefPx
        c.save()
        c.translate(x, y)
        c.scale(q, q)
        c.font = refFonts[face.ordinal]
        c.fillText(str, 0f, 0f)
        c.restore()
    }

    /** The faces the letter writes in, as weight and family. */
    private enum class Face(val weight: Int, val family: String) {
        From(650, SysFont),
        Subject(600, SysFont),
        Preview(400, SysFont),
        Time(400, SysFont),
        Pen(600, PenFont),
    }

    // Each face's strings, laid out once at RefPx: the advance up to each character, and the
    // ellipsis's width. Width scales with the font size, so a size that animates every frame is
    // fitted without laying text out again.
    private class Measured(val ends: FloatArray, val fitted: Array<String?>)

    private val measured = Face.entries.associateWith { HashMap<String, Measured>() }
    private val ellipsis = FloatArray(Face.entries.size) { -1f }

    private fun style(face: Face) =
        TextStyle(
            fontFamily = fonts[face.family] ?: FontFamily.Default,
            fontWeight = FontWeight(face.weight),
            fontSize = RefPx.sp,
        )

    private fun measure(text: String, face: Face): Measured =
        measured.getValue(face).getOrPut(text) {
            val l = measurer.measure(text, style(face), softWrap = false, density = Unit1)
            val ends = FloatArray(text.length + 1) { i -> l.getHorizontalPosition(i, true) }
            ends[text.length] = l.multiParagraph.getLineWidth(0)
            Measured(ends, arrayOfNulls(text.length + 1))
        }

    private fun ellipsisWidth(face: Face): Float {
        val i = face.ordinal
        if (ellipsis[i] < 0) {
            val l = measurer.measure("…", style(face), softWrap = false, density = Unit1)
            ellipsis[i] = l.multiParagraph.getLineWidth(0)
        }
        return ellipsis[i]
    }

    private fun textWidth(text: String, face: Face) = measure(text, face).ends.last()

    // The prototype's fit(): the whole string if it fits in [max], else as much as fits with "…".
    private fun fit(text: String, face: Face, size: Float, max: Float): String {
        val m = measure(text, face)
        val scale = size / RefPx
        if (m.ends.last() * scale <= max) return text
        val e = ellipsisWidth(face)
        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi + 1) shr 1
            if ((m.ends[mid] + e) * scale <= max) lo = mid else hi = mid - 1
        }
        if (lo == 0) return ""
        return m.fitted[lo] ?: (text.substring(0, lo).trimEnd() + "…").also { m.fitted[lo] = it }
    }

    private companion object {
        const val RefPx = 40f
        // A baked plate's margin around its stroke, in dp at k = 1.
        const val PlateM = 1.5f
        val Unit1 = Density(1f, 1f)
        val NoDash = FloatArray(0)
    }
}
