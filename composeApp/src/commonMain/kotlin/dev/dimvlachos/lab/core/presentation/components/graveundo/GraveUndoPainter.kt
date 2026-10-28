package dev.dimvlachos.lab.core.presentation.components.graveundo

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.canvas2d.mulberry
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveUndoState.Companion.CardS
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveUndoState.Companion.HU
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveUndoState.Companion.smooth
import dev.dimvlachos.lab.core.presentation.ui.GraveUndoScreenColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** The family keys the painter's font strings use. */
internal const val GraveSans = "sys"
internal const val GraveSerif = "serif"

/** The grave's words: the four lines under the buried task's name, and the stone's heading. */
internal class GraveWords(
    val laidToRest: String,
    val tapToUndo: String,
    val backFromGrave: String,
    val restInPeace: String,
    val hereLies: String,
    val bury: String,
)

/**
 * Draws the list and the grave as the prototype's canvas does, call by call, in dp: [drawList] is
 * the rows (a row's face, the soil and "Bury" under it as it is dragged, the hairlines), and
 * [drawGrave] the graveyard where the snackbar goes, the headstone, the hand and the cards flying
 * in and out. [density] is the screen's, for the baked layers and the shadows, which the browser
 * sizes in its own pixels.
 */
internal class GraveUndoPainter(private val words: GraveWords, private val density: Float) {
    private val pose = CardPose()

    // ---------- The list ----------

    fun drawList(c: Canvas2D, s: GraveUndoState, colors: GraveUndoScreenColors) {
        val w = s.width
        c.save()
        c.beginPath()
        c.rect(0f, s.listTop, w, s.height - s.listTop)
        c.clip()
        c.fillStyle = colors.line
        c.fillRect(0f, s.listTop - s.scroll, w, 1f)
        var top = s.listTop + 1f - s.scroll
        for (it in s.items) {
            if (it.gone) continue
            val h = s.rowH * (1 - it.collapse)
            if (h > .01f && top < s.height && top + h > s.listTop) drawRow(c, s, it, top, h, colors)
            top += h
        }
        c.restore()
    }

    private fun drawRow(
        c: Canvas2D,
        s: GraveUndoState,
        it: GraveItem,
        top: Float,
        h: Float,
        colors: GraveUndoScreenColors,
    ) {
        val w = s.width
        c.save()
        c.beginPath()
        c.rect(0f, top, w, h)
        c.clip()
        val under = if (it.opening) 0f else 1 - smooth(0f, .5f, it.collapse)
        if (
            under > .001f && (it.dx != 0f || it.collapse > 0f || it.labelL > 0f || it.labelR > 0f)
        ) {
            c.globalAlpha = under
            c.fillStyle = colors.soil
            c.fillRect(0f, top, w, h)
            drawLabel(c, s, true, it.labelL, it.armK, top, colors)
            drawLabel(c, s, false, it.labelR, it.armK, top, colors)
            c.globalAlpha = 1f
        }
        if (!it.faceHidden) {
            c.fillStyle = colors.row
            c.fillRect(it.dx, top, w, s.rowH)
            drawFace(c, s, it.task, it.done, it.dx, top, w, s.rowH, colors)
        }
        // The hairline under the row, from the text's edge: over the face, as the row's ::after.
        if (h >= 1f) {
            c.fillStyle = colors.line
            c.fillRect(56f, top + h - 1f, w - 56f, 1f)
        }
        c.restore()
    }

    // "Bury" with a little headstone, at the side the row is dragged from.
    private fun drawLabel(
        c: Canvas2D,
        s: GraveUndoState,
        left: Boolean,
        a: Float,
        arm: Float,
        top: Float,
        colors: GraveUndoScreenColors,
    ) {
        if (a <= .002f) return
        c.save()
        c.globalAlpha = c.globalAlpha * a
        c.font = "600 ${14f * s.textScale}px $GraveSans"
        c.textBaseline = "middle"
        c.textAlign = "left"
        val tw = c.measureText(words.bury)
        val lw = 18f + 8f + tw
        val cy = top + s.rowH / 2
        val x0 = if (left) 22f else s.width - 22f - lw
        val k = 1 + .1f * arm
        c.translate(x0 + lw / 2, cy)
        c.scale(k, k)
        c.translate(-lw / 2, 0f)
        c.fillStyle = colors.soilInk
        if (left) {
            drawStoneIcon(c, 0f, -10f, colors)
            c.fillStyle = colors.soilInk
            c.fillText(words.bury, 26f, .5f)
        } else {
            c.fillText(words.bury, 0f, .5f)
            drawStoneIcon(c, tw + 8f, -10f, colors)
        }
        c.restore()
    }

    // The label's 18 by 20 headstone icon.
    private fun drawStoneIcon(c: Canvas2D, x: Float, y: Float, colors: GraveUndoScreenColors) {
        c.save()
        c.translate(x, y)
        val a = c.globalAlpha
        c.globalAlpha = a * .9f
        c.fillStyle = colors.soilInk
        c.beginPath()
        c.moveTo(2f, 19f)
        c.lineTo(2f, 8f)
        c.arc(9f, 8f, 7f, PI.toFloat(), TAU)
        c.lineTo(16f, 19f)
        c.closePath()
        c.fill()
        c.globalAlpha = a
        c.lineCap = "round"
        c.lineWidth = 1.4f
        c.strokeStyle = "#4b3527"
        c.beginPath()
        c.moveTo(6f, 9f)
        c.lineTo(12f, 9f)
        c.moveTo(6f, 12f)
        c.lineTo(12f, 12f)
        c.stroke()
        c.strokeStyle = colors.soilInk
        c.beginPath()
        c.moveTo(0f, 19.3f)
        c.lineTo(18f, 19.3f)
        c.stroke()
        c.restore()
    }

    /**
     * A row's face from its top-left ([x], [y]): the check, the title and the line under it, and
     * the time at the right, laid out as the prototype's flex row (18 in, 24 check, 14 gap).
     */
    private fun drawFace(
        c: Canvas2D,
        s: GraveUndoState,
        task: GraveTask,
        done: Boolean,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        colors: GraveUndoScreenColors,
    ) {
        val k = s.textScale
        val cx = x + 18f + 12f
        val cy = y + h / 2
        c.beginPath()
        c.arc(cx, cy, 11f, 0f, TAU)
        if (done) {
            c.fillStyle = colors.accent
            c.fill()
            c.strokeStyle = "#ffffff"
            c.lineWidth = 2f
            c.lineCap = "round"
            c.lineJoin = "round"
            c.beginPath()
            c.moveTo(cx - 4.4f, cy + .4f)
            c.lineTo(cx - 1.4f, cy + 3.2f)
            c.lineTo(cx + 4.6f, cy - 3f)
            c.stroke()
        } else {
            c.strokeStyle = colors.line2
            c.lineWidth = 2f
            c.stroke()
        }
        c.textBaseline = "middle"
        c.font = "500 ${12.5f * k}px $GraveSans"
        c.textAlign = "right"
        c.fillStyle = colors.muted
        val timeW = c.measureText(task.time)
        c.fillText(task.time, x + w - 18f, cy + .5f)
        c.textAlign = "left"
        val tx = x + 56f
        val maxW = w - 56f - 14f - timeW - 18f
        val titleH = 15.5f * 1.25f * k
        val subH = 13f * 1.3f * k
        val t0 = cy - (titleH + subH) / 2
        c.font = "600 ${15.5f * k}px $GraveSans"
        c.fillStyle = if (done) colors.muted else colors.ink
        val title = fit(c, task.title, maxW)
        c.fillText(title, tx, t0 + titleH / 2 + .5f)
        if (done) {
            // The ticked task's line-through, as the row's CSS draws it.
            val tw = c.measureText(title)
            c.fillRect(tx, t0 + titleH / 2 + 1f * k, tw, 1.2f * k)
        }
        c.font = "400 ${13f * k}px $GraveSans"
        c.fillStyle = colors.muted
        c.fillText(fit(c, task.sub, maxW), tx, t0 + titleH + subH / 2 + .5f)
    }

    // A text cut to [maxW] with an ellipsis, as the row's CSS and the prototype's card cut it.
    private val fits = HashMap<String, String>()

    private fun fit(c: Canvas2D, text: String, maxW: Float): String {
        val key = "${c.font}|${maxW.roundToInt()}|$text"
        fits[key]?.let {
            return it
        }
        var t = text
        if (c.measureText(t) > maxW + .5f) {
            while (t.length > 1 && c.measureText("$t…") > maxW) t = t.dropLast(1)
            t += "…"
        }
        if (fits.size > 256) fits.clear()
        fits[key] = t
        return t
    }

    /** A row's face as a card in flight, centred at ([x], [y]), scaled [s], tilted [rot]. */
    private fun drawCard(
        c: Canvas2D,
        st: GraveUndoState,
        g: Grave,
        x: Float,
        y: Float,
        s: Float,
        rot: Float,
        alpha: Float,
        lift: Float,
        colors: GraveUndoScreenColors,
    ) {
        if (alpha <= .002f) return
        c.save()
        c.globalAlpha = alpha
        c.translate(x, y)
        c.rotate(rot)
        c.scale(s, s)
        val w = st.width
        val h = st.rowH
        val r = 12 * lift
        if (lift > .01f) {
            c.setShadowColor(Color.Black.copy(alpha = .3f * lift))
            c.shadowBlur = 22 * lift * max(s, .35f) * density
            c.shadowOffsetY = 7 * lift * max(s, .35f) * density
        }
        c.fillStyle = colors.row
        c.beginPath()
        c.roundRect(-w / 2, -h / 2, w, h, r)
        c.fill()
        c.setShadowColor(Color.Transparent)
        c.shadowBlur = 0f
        c.shadowOffsetY = 0f
        if (lift > .01f) {
            c.strokeStyle = Color(150 / 255f, 140 / 255f, 160 / 255f, .38f * lift)
            c.lineWidth = 1 / max(s, .3f)
            c.stroke()
        }
        drawFace(c, st, g.item.task, g.done, -w / 2, -h / 2, w, h, colors)
        c.restore()
    }

    // ---------- The grave's baked layers: dusk sky behind, soil and grass in front ----------

    private var back: Canvas2D? = null
    private var front: Canvas2D? = null
    private var bakedFor = ""

    private fun bake(s: GraveUndoState) {
        val key = "${s.slotW}|${s.u}"
        if (key == bakedFor && back != null) return
        bakedFor = key
        val u = s.u
        val w = s.slotW
        val h = s.slotH
        val sy = (h * .72f).roundToInt().toFloat()
        val bw = ((w + Pad * 2) * density).roundToInt()
        val bh = ((h + Pad * 2) * density).roundToInt()
        val r = mulberry(13)
        fun rnd() = r().toFloat()
        val g = Canvas2D.create(bw, bh)
        g.setTransform(density, 0f, 0f, density, Pad * density, Pad * density)
        fun shape() {
            g.beginPath()
            g.roundRect(0f, 0f, w, h, 20f)
        }
        g.save()
        g.shadowColor = "rgba(0,0,0,.4)"
        g.shadowBlur = 18 * density
        g.shadowOffsetY = 6 * density
        g.fillStyle = "#231c2f"
        shape()
        g.fill()
        g.restore()
        g.save()
        shape()
        g.clip()
        var gr = g.createLinearGradient(0f, 0f, 0f, sy)
        gr.addColorStop(0f, "#1d182d")
        gr.addColorStop(.6f, "#30243e")
        gr.addColorStop(1f, "#4a3149")
        g.fillStyle = gr
        g.fillRect(0f, 0f, w, sy + 2)
        // Moon low on the right, with a wide soft glow.
        val mx = w - 28 * u
        val my = 21 * u
        val mr = 7.5f * u
        gr = g.createRadialGradient(mx, my, 0f, mx, my, 54 * u)
        gr.addColorStop(0f, "rgba(255,236,196,.3)")
        gr.addColorStop(.3f, "rgba(255,220,180,.1)")
        gr.addColorStop(1f, "rgba(255,220,180,0)")
        g.fillStyle = gr
        g.fillRect(0f, 0f, w, sy)
        gr = g.createRadialGradient(mx - mr * .3f, my - mr * .3f, 0f, mx, my, mr)
        gr.addColorStop(0f, "#fff7e2")
        gr.addColorStop(1f, "#eedcb2")
        g.fillStyle = gr
        g.beginPath()
        g.arc(mx, my, mr, 0f, TAU)
        g.fill()
        g.fillStyle = "rgba(190,165,120,.35)"
        for ((dx, dy, rr) in MoonSpots) {
            g.beginPath()
            g.arc(mx + dx * mr, my + dy * mr, rr * mr, 0f, TAU)
            g.fill()
        }
        // A few stars.
        for (i in 0 until 18) {
            val x = rnd() * w
            val y = rnd() * sy * .55f
            g.fillStyle = Color(1f, 248 / 255f, 230 / 255f, .2f + rnd() * .4f)
            g.fillRect(x, y, .9f * u, .9f * u)
        }
        // Distant headstones and a bit of iron fence along the horizon, very low contrast.
        g.fillStyle = "#271e33"
        for (i in 0 until 7) {
            val x = w * (.42f + i * .085f) + (rnd() - .5f) * 8 * u
            val sw = (6 + rnd() * 5) * u
            val shh = (7 + rnd() * 6) * u
            g.beginPath()
            g.moveTo(x - sw / 2, sy + 1)
            g.lineTo(x - sw / 2, sy - shh + sw / 2)
            g.arc(x, sy - shh + sw / 2, sw / 2, PI.toFloat(), 0f)
            g.lineTo(x + sw / 2, sy + 1)
            g.fill()
        }
        g.strokeStyle = "#2a2036"
        g.lineWidth = 1 * u
        g.beginPath()
        g.moveTo(w * .78f, sy - 9 * u)
        g.lineTo(w, sy - 9 * u)
        g.stroke()
        var fx = w * .78f
        while (fx < w) {
            g.beginPath()
            g.moveTo(fx, sy)
            g.lineTo(fx, sy - 13 * u)
            g.stroke()
            g.fillStyle = "#2a2036"
            g.beginPath()
            g.moveTo(fx - 1.4f * u, sy - 12 * u)
            g.lineTo(fx, sy - 15 * u)
            g.lineTo(fx + 1.4f * u, sy - 12 * u)
            g.fill()
            fx += 5 * u
        }
        g.restore()
        g.strokeStyle = "rgba(255,255,255,.07)"
        g.lineWidth = 1f
        g.beginPath()
        g.roundRect(.5f, .5f, w - 1, h - 1, 19.5f)
        g.stroke()

        val f = Canvas2D.create(bw, bh)
        f.setTransform(density, 0f, 0f, density, Pad * density, Pad * density)
        f.save()
        f.beginPath()
        f.roundRect(0f, 0f, w, h, 20f)
        f.clip()
        gr = f.createLinearGradient(0f, sy, 0f, h)
        gr.addColorStop(0f, "#3b291e")
        gr.addColorStop(1f, "#1f150f")
        f.fillStyle = gr
        f.fillRect(0f, sy, w, h - sy)
        for (i in 0 until 70) {
            f.fillStyle = if (rnd() < .5f) "rgba(0,0,0,.18)" else "rgba(150,110,80,.14)"
            f.beginPath()
            f.arc(rnd() * w, sy + 4 * u + rnd() * (h - sy - 4 * u), (.6f + rnd()) * u, 0f, TAU)
            f.fill()
        }
        // Grass fringe along the soil line.
        f.lineCap = "round"
        var gx = 1f
        while (gx < w) {
            val hh = (2.5f + rnd() * 6) * u
            val lean = (rnd() - .5f) * 3 * u
            f.strokeStyle = Grass[(rnd() * 4).toInt().coerceIn(0, 3)]
            f.lineWidth = (1 + rnd() * .6f) * u
            f.beginPath()
            f.moveTo(gx, sy + 2 * u)
            f.quadraticCurveTo(gx + lean * .3f, sy - hh * .5f, gx + lean, sy - hh)
            f.stroke()
            gx += 2.2f * u
        }
        f.restore()
        back = g
        front = f
    }

    // ---------- Headstone ----------

    private val lines = HashMap<String, List<String>>()

    // The title cut into at most two lines that fit the stone, as the prototype's wrapTitle().
    private fun wrapTitle(c: Canvas2D, text: String, maxW: Float): List<String> {
        val key = "${c.font}|$maxW|$text"
        lines[key]?.let {
            return it
        }
        val out = mutableListOf("")
        for (wd in text.split(' ')) {
            val cur = out.last()
            val next = if (cur.isNotEmpty()) "$cur $wd" else wd
            if (c.measureText(next) <= maxW || cur.isEmpty()) out[out.size - 1] = next
            else if (out.size < 2) out += wd else out[1] = out[1] + " " + wd
        }
        if (out.size > 1 && c.measureText(out[1]) > maxW) {
            var t = out[1]
            while (t.length > 1 && c.measureText("$t…") > maxW) t = t.dropLast(1)
            out[1] = "$t…"
        }
        if (lines.size > 64) lines.clear()
        lines[key] = out
        return out
    }

    private fun drawStone(c: Canvas2D, s: GraveUndoState, a: Float) {
        val u = s.u
        val sw = 60 * u
        val sh = 60 * u
        val x = s.stoneX
        val base = s.soil + 3 * u
        val arch = 17 * u
        c.save()
        c.globalAlpha = a
        fun path() {
            c.beginPath()
            c.moveTo(x - sw / 2, base)
            c.lineTo(x - sw / 2, base - sh + arch)
            c.ellipse(x, base - sh + arch, sw / 2, arch, 0f, PI.toFloat(), TAU)
            c.lineTo(x + sw / 2, base)
            c.closePath()
        }
        c.fillStyle = "rgba(0,0,0,.35)"
        c.save()
        c.translate(-2 * u, 1 * u)
        path()
        c.fill()
        c.restore()
        val gr = c.createLinearGradient(x - sw / 2, 0f, x + sw / 2, 0f)
        gr.addColorStop(0f, "#53555e")
        gr.addColorStop(.45f, "#7d7f88")
        gr.addColorStop(.8f, "#a2a4ab")
        gr.addColorStop(1f, "#6a6c74")
        c.fillStyle = gr
        path()
        c.fill()
        c.save()
        path()
        c.clip()
        val v = c.createLinearGradient(0f, base - sh, 0f, base)
        v.addColorStop(0f, "rgba(255,240,220,.12)")
        v.addColorStop(1f, "rgba(20,10,30,.35)")
        c.fillStyle = v
        c.fillRect(x - sw, base - sh - 2, sw * 2, sh + 4)
        for (i in 0 until StoneDots.size / 3) {
            val fx = StoneDots[i * 3]
            val fy = StoneDots[i * 3 + 1]
            val fs = StoneDots[i * 3 + 2]
            c.fillStyle = if (fs < .5f) DotDark else DotLight
            c.fillRect(x + (fx - .5f) * sw, base - fy * sh, (.6f + fs) * u, (.6f + fs) * u)
        }
        c.fillStyle = "rgba(88,118,62,.55)"
        for ((dx, dy, rr) in Moss) {
            c.beginPath()
            c.ellipse(x + dx * sw, base - dy * sh, rr * sw, rr * sw * .55f, 0f, 0f, TAU)
            c.fill()
        }
        // Engravings: every grave on this stone, each at its own strength.
        val last = s.lastGone
        if (last != null) carve(c, s, last, a, x, base, sh, sw)
        for (g in s.graves) carve(c, s, g, a, x, base, sh, sw)
        c.restore()
        c.globalAlpha = a
        c.strokeStyle = "rgba(255,255,255,.16)"
        c.lineWidth = .8f * u
        c.beginPath()
        c.ellipse(
            x,
            base - sh + arch,
            sw / 2 - .6f * u,
            arch - .6f * u,
            0f,
            -PI.toFloat() * .45f,
            -PI.toFloat() * .02f,
        )
        c.stroke()
        c.restore()
    }

    private fun carve(
        c: Canvas2D,
        s: GraveUndoState,
        g: Grave,
        a: Float,
        x: Float,
        base: Float,
        sh: Float,
        sw: Float,
    ) {
        if (g.engr <= .003f) return
        val u = s.u
        c.globalAlpha = a * g.engr
        c.textBaseline = "middle"
        // "HERE LIES", letter-spaced 1.1 as the prototype sets it, centred letter by letter.
        c.font = "700 ${5.8f * u}px $GraveSerif"
        c.textAlign = "left"
        val gap = 1.1f * u
        val head = words.hereLies
        var total = 0f
        for (ch in head) total += c.measureText(ch.toString()) + gap
        var hx = x - total / 2 + gap / 2
        for (ch in head) {
            val t = ch.toString()
            carveText(c, t, hx, base - sh + 17 * u, u)
            hx += c.measureText(t) + gap
        }
        c.textAlign = "center"
        c.font = "600 ${8f * u}px $GraveSerif"
        val ls = wrapTitle(c, g.item.task.title, sw - 9 * u)
        for (i in ls.indices) carveText(c, ls[i], x, base - sh + 29 * u + i * 9.4f * u, u)
    }

    private fun carveText(c: Canvas2D, t: String, x: Float, y: Float, u: Float) {
        c.fillStyle = CarveLight
        c.fillText(t, x, y + .6f * u)
        c.fillStyle = CarveDark
        c.fillText(t, x, y)
    }

    // ---------- The hand ----------

    // Each bone: x1, y1, x2, y2, width, and whether it is a fingertip; filled in each frame.
    private val segs = FloatArray(32 * 6)
    private var nSegs = 0

    private fun bone(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, tip: Boolean = false) {
        val i = nSegs * 6
        segs[i] = x1
        segs[i + 1] = y1
        segs[i + 2] = x2
        segs[i + 3] = y2
        segs[i + 4] = w
        segs[i + 5] = if (tip) 1f else 0f
        nSegs++
    }

    private fun drawHand(c: Canvas2D, s: GraveUndoState, g: Grave, alpha: Float) {
        val u = s.u
        val sway = s.sway(g)
        val wrist = s.wrist(g)
        val wig = if (s.reducedMotion) 0f else g.wig
        val t = s.simNow
        nSegs = 0
        // Each bone is a cartoon "dog bone": a slim shaft with a rounded knob at each end, so the
        // fingers read as jointed, rounded bones rather than sticks.
        // Forearm: radius and ulna run down into the dirt.
        bone(-3.7f, 48f, -3.2f, 3f, 5.4f)
        bone(3.9f, 48f, 3.5f, 3f, 4.9f)
        // Fingers (index to little), each from its knuckle: metacarpal, then three phalanges that
        // curl gently inward, as if holding the card up by its lower edge.
        for (i in 0 until 4) {
            val b = i * 4
            bone(Knuckles[b], Knuckles[b + 1], Knuckles[b + 2], Knuckles[b + 3], 2.9f)
            var x = Knuckles[b + 2]
            var y = Knuckles[b + 3]
            var a = Spread[i]
            for (j in 0 until 3) {
                a += wig * .2f * sin(t * .0065f + i * 1.25f + j * .6f) + if (j > 0) Curl[i] else 0f
                val l = Lens[i * 3 + j]
                val nx = x + sin(a) * l
                val ny = y - cos(a) * l
                bone(x, y, nx, ny, 2.75f - j * .3f, j == 2)
                x = nx
                y = ny
            }
        }
        // Thumb wraps round the lower left of the card.
        val ta = wig * .25f * sin(t * .006f + 4)
        bone(-5.6f, -5.8f, -10f, -11.6f, 3f)
        bone(-10f, -11.6f, -12f + ta * 3, -17.6f, 2.7f)
        bone(-12f + ta * 3, -17.6f, -10.8f + ta * 4, -22.4f, 2.4f, true)
        c.save()
        c.globalAlpha = alpha
        c.translate(s.handX, s.soil)
        c.rotate(sway)
        c.translate(0f, wrist)
        c.scale(u * HU, u * HU)
        c.lineCap = "round"
        pass(c, BoneEdge, 1.3f)
        // Carpals: a little cluster at the wrist.
        c.fillStyle = BoneEdge
        for (i in 0 until Carpals.size / 3) {
            c.beginPath()
            c.arc(Carpals[i * 3], Carpals[i * 3 + 1], Carpals[i * 3 + 2] + .65f, 0f, TAU)
            c.fill()
        }
        c.fillStyle = Bone
        for (i in 0 until Carpals.size / 3) {
            c.beginPath()
            c.arc(Carpals[i * 3], Carpals[i * 3 + 1], Carpals[i * 3 + 2], 0f, TAU)
            c.fill()
        }
        pass(c, Bone, 0f)
        // A soft highlight down the left of each shaft.
        c.translate(-.4f, -.2f)
        c.strokeStyle = BoneHi
        for (k in 0 until nSegs) {
            val i = k * 6
            c.lineWidth = segs[i + 4] * .22f
            c.beginPath()
            c.moveTo(segs[i], segs[i + 1])
            c.lineTo(segs[i + 2], segs[i + 3])
            c.stroke()
        }
        c.restore()
    }

    private fun pass(c: Canvas2D, style: Color, extra: Float) {
        c.strokeStyle = style
        c.fillStyle = style
        for (k in 0 until nSegs) {
            val i = k * 6
            val x1 = segs[i]
            val y1 = segs[i + 1]
            val x2 = segs[i + 2]
            val y2 = segs[i + 3]
            val w = segs[i + 4]
            val tip = segs[i + 5] > 0f
            c.lineWidth = w * .74f + extra
            c.beginPath()
            c.moveTo(x1, y1)
            c.lineTo(x2, y2)
            c.stroke()
            // Rounded knobs at both ends; a fingertip ends in a smaller dome.
            c.beginPath()
            c.arc(x1, y1, w * .5f + extra / 2, 0f, TAU)
            c.fill()
            c.beginPath()
            c.arc(x2, y2, (if (tip) w * .4f else w * .5f) + extra / 2, 0f, TAU)
            c.fill()
        }
    }

    // ---------- The grave, the hand and the cards ----------

    fun drawGrave(c: Canvas2D, s: GraveUndoState, colors: GraveUndoScreenColors) {
        if (s.width <= 0f) return
        bake(s)
        val pa = smooth(0f, .3f, s.pillV)
        val show = s.pillV > .002f
        val px = s.slotX
        val py = s.slotY
        val pw = s.slotW
        val ph = s.slotH
        val u = s.u
        if (show) {
            c.save()
            c.globalAlpha = pa
            c.drawImage(back!!, px - Pad, py - Pad, pw + Pad * 2, ph + Pad * 2)
            c.restore()
            drawStone(c, s, pa)
            // Status text: the buried task's name and what's happening, crossfading on change.
            val g = s.named
            val maxW = px + pw - 44 * u - s.textX
            c.save()
            c.globalAlpha = pa
            c.textAlign = "left"
            c.textBaseline = "middle"
            if (g != null) {
                c.font = "600 ${13.5f * u}px $GraveSans"
                c.fillStyle = "#f3ecf6"
                c.fillText(fit(c, g.item.task.title, maxW), s.textX, py + 30 * u)
            }
            c.font = "500 ${12f * u}px $GraveSans"
            c.fillStyle = "#c9bdd2"
            val k = s.statusK
            if (k < 1f && s.statusPrev != GraveStatus.None) {
                c.globalAlpha = pa * (1 - k)
                c.fillText(statusText(s.statusPrev), s.textX, py + 49 * u)
            }
            c.globalAlpha = pa * k
            c.fillText(statusText(s.status), s.textX, py + 49 * u)
            c.restore()
        }
        // Things in the ground: cards sinking or rising, and hands. Clipped at the soil line.
        for (g in s.graves) {
            val inGround = g.phase == GravePhase.Fly || g.phase == GravePhase.Undo || g.rise > .001f
            if (!inGround && g.phase != GravePhase.Land) continue
            c.save()
            if (g.phase != GravePhase.Land) {
                c.beginPath()
                c.rect(0f, 0f, s.width, s.soil + .5f * u)
                c.clip()
            }
            if (g.rise > .001f && g.phase != GravePhase.Undo) {
                s.cardInHand(g, pose)
                drawCard(c, s, g, pose.x, pose.y, CardS, pose.rot, pa, 1f, colors)
                drawHand(c, s, g, pa)
            }
            if (g.phase == GravePhase.Undo && g.rise > .001f) drawHand(c, s, g, pa)
            if (g.phase == GravePhase.Fly) {
                s.flyPos(g, s.flyE(g), pose)
                drawCard(c, s, g, pose.x, pose.y, pose.s, pose.rot, 1f, pose.lift, colors)
            }
            c.restore()
        }
        if (show) {
            c.save()
            c.globalAlpha = pa
            c.drawImage(front!!, px - Pad, py - Pad, pw + Pad * 2, ph + Pad * 2)
            // The fresh mound in front of the stone.
            val first = s.graves.firstOrNull()
            val mk =
                if (s.graves.any { it.phase != GravePhase.Fly }) 1f
                else .8f + .2f * smooth(.85f, 1f, if (first != null) s.flyE(first) else 0f)
            val mx = s.handX
            val my = s.soil + 2 * u
            val mrx = 30 * u
            val mry = 8 * u * mk
            val gr = c.createLinearGradient(0f, my - mry, 0f, my + mry)
            gr.addColorStop(0f, "#6a4c37")
            gr.addColorStop(.5f, "#4b3527")
            gr.addColorStop(1f, "#2c1e15")
            c.fillStyle = gr
            c.beginPath()
            c.ellipse(mx, my, mrx, mry, 0f, 0f, TAU)
            c.fill()
            for (i in 0 until MoundDots.size / 4) {
                val a = MoundDots[i * 4]
                val d = MoundDots[i * 4 + 1]
                c.fillStyle = if (MoundDots[i * 4 + 2] < .5f) MoundLight else MoundDark
                c.beginPath()
                c.arc(
                    mx + cos(a) * d * mrx * .9f,
                    my - sin(a) * d * mry * .8f,
                    (.6f + MoundDots[i * 4 + 3] * 1.1f) * u,
                    0f,
                    TAU,
                )
                c.fill()
            }
            // Mist drifting along the ground.
            c.beginPath()
            c.roundRect(px, py, pw, ph, 20f)
            c.clip()
            for (i in 0 until 3) {
                val span = pw + 160 * u
                val x = px - 80 * u + ((s.simNow * .012f * u * (1 + i * .4f) + i * span / 3) % span)
                val y = s.soil - 4 * u + i * 3 * u
                val q = c.createRadialGradient(x, y, 0f, x, y, 70 * u)
                q.addColorStop(0f, "rgba(220,214,236,.16)")
                q.addColorStop(1f, "rgba(220,214,236,0)")
                c.save()
                c.translate(x, y)
                c.scale(1f, .22f)
                c.translate(-x, -y)
                c.fillStyle = q
                c.beginPath()
                c.arc(x, y, 70 * u, 0f, TAU)
                c.fill()
                c.restore()
            }
            c.restore()
        }
        val cr = s.crumbs
        for (i in 0 until cr.n) {
            c.fillStyle = if (cr.dark[i]) CrumbDark else CrumbLight
            c.beginPath()
            c.arc(cr.x[i], cr.y[i], cr.r[i], 0f, TAU)
            c.fill()
        }
        // Cards flying home are above everything else.
        for (g in s.graves) {
            if (g.phase == GravePhase.Undo) {
                s.undoPos(g, s.undoE(g), pose)
                drawCard(c, s, g, pose.x, pose.y, pose.s, pose.rot, 1f, pose.lift, colors)
            }
            if (g.phase == GravePhase.Land)
                drawCard(c, s, g, g.toX, s.rowTop(g.item) + s.rowH / 2, 1f, 0f, g.alpha, 0f, colors)
        }
    }

    private fun statusText(st: GraveStatus) =
        when (st) {
            GraveStatus.LaidToRest -> words.laidToRest
            GraveStatus.TapToUndo -> words.tapToUndo
            GraveStatus.BackFromGrave -> words.backFromGrave
            GraveStatus.RestInPeace -> words.restInPeace
            GraveStatus.None -> ""
        }

    private companion object {
        const val Pad = 24f
        val Bone = Color(0xFFEFE5CF)
        val BoneEdge = Color(66 / 255f, 54 / 255f, 42 / 255f, .9f)
        val BoneHi = Color(1f, 252 / 255f, 242 / 255f, .75f)
        val CarveLight = Color(1f, 1f, 1f, .22f)
        val CarveDark = Color(28 / 255f, 26 / 255f, 36 / 255f, .82f)
        val DotDark = Color(0f, 0f, 0f, .14f)
        val DotLight = Color(1f, 1f, 1f, .09f)
        val MoundLight = Color(130 / 255f, 98 / 255f, 72 / 255f, .7f)
        val MoundDark = Color(30 / 255f, 20 / 255f, 14 / 255f, .5f)
        val CrumbDark = Color(0xFF5D4331)
        val CrumbLight = Color(0xFF7B5C43)
        val Grass = arrayOf("#2f3d24", "#3f5230", "#55683a", "#46592f")
        val MoonSpots =
            listOf(Triple(-.3f, .2f, .22f), Triple(.25f, -.2f, .16f), Triple(.1f, .4f, .12f))
        val Moss =
            listOf(Triple(-.42f, .06f, .16f), Triple(-.3f, .02f, .11f), Triple(.36f, .04f, .1f))
        val Knuckles =
            floatArrayOf(
                -5.2f,
                -8.8f,
                -6.6f,
                -17.8f,
                -1.75f,
                -9.6f,
                -2.2f,
                -19.2f,
                1.75f,
                -9.6f,
                2.1f,
                -19.2f,
                5.1f,
                -8.8f,
                6.3f,
                -17.2f,
            )
        val Lens =
            floatArrayOf(5.4f, 3.9f, 3.1f, 5.8f, 4.1f, 3.2f, 5.5f, 3.9f, 3.1f, 4.4f, 3.2f, 2.6f)
        val Spread = floatArrayOf(-.15f, -.05f, .05f, .17f)
        val Curl = floatArrayOf(.2f, .08f, -.08f, -.22f)
        val Carpals =
            floatArrayOf(
                -4.6f,
                -3.2f,
                2.3f,
                -1.5f,
                -4.6f,
                2.2f,
                1.8f,
                -4.6f,
                2.2f,
                4.8f,
                -3.2f,
                2.2f,
                -3f,
                -7.2f,
                2f,
                0f,
                -7.9f,
                2f,
                3f,
                -7.2f,
                2f,
            )

        // The prototype's STONE_DOTS: 34 triples from mulberry(5).
        val StoneDots: FloatArray = mulberry(5).let { r -> FloatArray(34 * 3) { r().toFloat() } }

        // The mound's 16 dots, from mulberry(41) in the prototype's order: angle, distance, colour
        // and size.
        val MoundDots: FloatArray =
            mulberry(41).let { r ->
                val out = FloatArray(16 * 4)
                for (i in 0 until 16) {
                    out[i * 4] = (r() * PI).toFloat()
                    out[i * 4 + 1] = sqrt(r()).toFloat()
                    out[i * 4 + 2] = r().toFloat()
                    out[i * 4 + 3] = r().toFloat()
                }
                out
            }
    }
}
