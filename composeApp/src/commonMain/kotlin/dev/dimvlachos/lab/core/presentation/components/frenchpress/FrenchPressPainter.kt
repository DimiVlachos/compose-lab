package dev.dimvlachos.lab.core.presentation.components.frenchpress

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.components.frenchpress.FrenchPressState.Companion.easeOut
import dev.dimvlachos.lab.core.presentation.components.frenchpress.FrenchPressState.Companion.lerp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** An rgb colour as the prototype's arrays hold them, 0 to 255. */
internal class Rgb(val r: Float, val g: Float, val b: Float)

/**
 * Draws the French press and everything in flight, as the prototype's `drawPress()` and
 * `drawParts()`: the press in its own frame, tipped about its left foot to pour; the liquid clipped
 * to the glass with its surface level on the screen; the grounds, each one a real word, tiny, on
 * its swirl; and the words and letters flying between the page and the spout, drawn from their own
 * layouts on the baseline the page uses. Gradients that don't move are made once a layout, and the
 * words are drawn on the canvas directly, so a frame of hundreds makes no garbage.
 */
internal class FrenchPressPainter {
    private var madeFor = -1
    private var madeDark = false
    private lateinit var chromeMesh: Gradient2D
    private lateinit var chromeBand: Gradient2D
    private lateinit var chromeLid: Gradient2D
    private lateinit var chromeRod: Gradient2D
    private lateinit var highlight: Gradient2D
    private val footR = FloatArray(4)
    private val glassR = FloatArray(4)
    private val out = FloatArray(3)

    private fun chrome(c: Canvas2D, x0: Float, x1: Float, dark: Boolean): Gradient2D {
        val g = c.createLinearGradient(x0, 0f, x1, 0f)
        if (dark) {
            g.addColorStop(0f, "#5d6066")
            g.addColorStop(.3f, "#c9ccd1")
            g.addColorStop(.55f, "#83878e")
            g.addColorStop(1f, "#45484d")
        } else {
            g.addColorStop(0f, "#8a8e95")
            g.addColorStop(.28f, "#f4f5f7")
            g.addColorStop(.55f, "#a9adb3")
            g.addColorStop(.8f, "#d8dade")
            g.addColorStop(1f, "#6f737a")
        }
        return g
    }

    private fun prepare(c: Canvas2D, s: FrenchPressState, dark: Boolean) {
        if (madeFor == s.layoutVersion && madeDark == dark) return
        madeFor = s.layoutVersion
        madeDark = dark
        val cw = s.cw
        val kp = s.kp
        chromeMesh = chrome(c, -cw / 2, cw / 2, dark)
        chromeBand = chrome(c, -cw / 2 - 2 * kp, cw / 2 + 2 * kp, dark)
        chromeLid = chrome(c, -cw / 2 - 3 * kp, cw / 2 + 3 * kp, dark)
        chromeRod = chrome(c, -1.5f * kp, 1.5f * kp, dark)
        highlight =
            c.createLinearGradient(-cw / 2, 0f, cw / 2, 0f).apply {
                addColorStop(0f, "rgba(255,255,255,0)")
                addColorStop(.12f, "rgba(255,255,255,${if (dark) .16 else .55})")
                addColorStop(.22f, "rgba(255,255,255,0)")
                addColorStop(.82f, "rgba(255,255,255,0)")
                addColorStop(.88f, "rgba(255,255,255,${if (dark) .1 else .35})")
                addColorStop(.93f, "rgba(255,255,255,0)")
            }
        footR[0] = 0f
        footR[1] = 0f
        footR[2] = 2 * kp
        footR[3] = 2 * kp
        glassR[0] = 2 * kp
        glassR[1] = 2 * kp
        glassR[2] = 7 * kp
        glassR[3] = 7 * kp
    }

    private fun glassPath(c: Canvas2D, s: FrenchPressState) {
        c.beginPath()
        c.roundRect(-s.cw / 2, s.rimY, s.cw, s.ch, glassR)
    }

    /**
     * The prototype's `draw()`: the press, then the parts in flight, on [c] already scaled to dp by
     * [density]. [ink] is the page's text colour.
     */
    fun draw(
        c: Canvas2D,
        s: FrenchPressState,
        text: PressText,
        dark: Boolean,
        ink: Rgb,
        density: Float,
    ) {
        if (s.width <= 0f) return
        prepare(c, s, dark)
        drawPress(c, s, text, dark, density)
        drawParts(c, s, text, dark, ink)
    }

    private fun drawParts(
        c: Canvas2D,
        s: FrenchPressState,
        text: PressText,
        dark: Boolean,
        ink: Rgb,
    ) {
        if (s.parts.isEmpty()) return
        val k = s.k
        val sx = s.spoutX
        val sy = s.spoutY
        val coffee = if (dark) GroundsDark else GroundsLight
        val amber = if (dark) AmberDark else AmberLight
        val now = s.now
        c.drawUnder { canvas ->
            for (i in s.parts.indices) {
                val p = s.parts[i]
                val t = ((now - p.t0) / p.dur).toFloat().coerceIn(0f, 1f)
                val isChar = !p.word
                val x: Float
                val y: Float
                val sc: Float
                val rot: Float
                val to: Rgb
                val e: Float
                val from: Rgb
                if (p.into) {
                    // Sucked in: picked up off the line first, then faster and faster down into
                    // the spout. Each aims a little to the side of the gather point, so the
                    // stream stays a ribbon instead of knotting into one ball above the spout.
                    val lift = .14f
                    val u = (t / lift).coerceIn(0f, 1f)
                    val lx = p.x0
                    val ly = p.y0 - (if (isChar) 1.5f else 6f) * k * easeOut(u)
                    val fan = ((p.index * 37 % 23) / 22f - .5f) * 2
                    e = ((t - lift) / (1 - lift)).coerceIn(0f, 1f).pow(1.5f)
                    val ax = sx + 4 * k + fan * 16 * k
                    val ay = min(sy - 150 * k, ly + 40 * k) + abs(fan) * 22 * k
                    x = cub(lx, lerp(lx, ax, .55f), ax, sx, e)
                    y = cub(ly, ly - (if (isChar) 4f else 14f) * k, ay, sy, e)
                    // Shrinks as soon as it leaves the line, so crowds in mid-air are small.
                    sc = lerp(1f, .3f, sqrt(e))
                    rot = p.spin * e * e
                    from = ink
                    to = if (isChar) amber else coffee
                } else {
                    // Out of the spout: up first, then across and into place.
                    e = easeOut(t)
                    val cx = lerp(p.x0, p.x1, .12f)
                    val cy = lerp(p.y0, p.y1, .92f)
                    x = bez(p.x0, cx, p.x1, e)
                    y = bez(p.y0, cy, p.y1, e)
                    sc = lerp(.34f, 1f, if (isChar) e * e * e else e)
                    rot = p.spin * (1 - e)
                    from = if (isChar) amber else coffee
                    to = ink
                }
                val col = mixColor(from, to, e, 1f)
                canvas.save()
                canvas.translate(x, y)
                if (rot != 0f) canvas.rotate(rot * DEG)
                canvas.scale(sc, sc)
                // Drawn on the same baseline the page's text uses, so nothing hops when a word or
                // letter lands and the real text takes over.
                if (isChar) text.paintLetter(canvas, p.index, col, text.letterBase)
                else text.paintWord(canvas, p.index, col, text.wordBase)
                canvas.restore()
            }
        }
    }

    private fun drawPress(
        c: Canvas2D,
        s: FrenchPressState,
        text: PressText,
        dark: Boolean,
        density: Float,
    ) {
        val cw = s.cw
        val ch = s.ch
        val gb = s.gb
        val rimY = s.rimY
        val kp = s.kp
        val pP = s.p
        val my = s.meshY(pP)
        val ky = s.knobY(pP)
        c.save()
        // Shadow on the page, not tipped.
        c.fillStyle = if (dark) "rgba(0,0,0,.45)" else "rgba(60,40,20,.2)"
        c.beginPath()
        c.ellipse(s.ox + s.tilt * 30 * kp, s.oy + 1 * kp, cw * .72f, 4.5f * kp, 0f, 0f, TAU)
        c.fill()
        c.translate(s.ox, s.oy)
        c.translate(s.pvx, 0f)
        c.rotate(s.tilt)
        c.translate(-s.pvx, 0f)

        // Feet and the bottom band of the frame.
        c.fillStyle = chromeMesh
        c.beginPath()
        c.roundRect(-cw / 2 + 1 * kp, -4 * kp, 7 * kp, 4 * kp, footR)
        c.roundRect(cw / 2 - 8 * kp, -4 * kp, 7 * kp, 4 * kp, footR)
        c.fill()
        c.fillStyle = chromeBand
        c.beginPath()
        c.roundRect(-cw / 2 - 2 * kp, gb - 1 * kp, cw + 4 * kp, 9 * kp, 2.5f * kp)
        c.fill()

        // Glass back.
        glassPath(c, s)
        c.fillStyle = if (dark) "rgba(200,215,230,.06)" else "rgba(225,238,246,.38)"
        c.fill()
        c.save()
        glassPath(c, s)
        c.clip()
        // The liquid's surface stays level on screen while the press tips: drawn in screen space
        // inside the tipped clip.
        val outF = if (s.inkCount > 0) s.charsOut.toFloat() / s.inkCount else 0f
        val levelL = lerp(s.level0, my - 1.2f * kp, if (s.clarity > 0f) outF else 0f)
        val murk = if (s.total > 0) (s.grains.size.toFloat() / s.total).coerceIn(0f, 1f) else 0f
        val murkC = if (dark) MurkDark else MurkLight
        val amberC = if (dark) AmberDark else AmberLight
        val waterC = if (dark) WaterDark else WaterLight
        val m14 = (murk * 1.4f).coerceIn(0f, 1f)
        val clarity = s.clarity
        val aboveR: Float
        val aboveG: Float
        val aboveB: Float
        if (clarity > 0f) {
            aboveR = lerp(murkC.r, amberC.r, clarity)
            aboveG = lerp(murkC.g, amberC.g, clarity)
            aboveB = lerp(murkC.b, amberC.b, clarity)
        } else {
            aboveR = lerp(waterC.r, murkC.r, m14)
            aboveG = lerp(waterC.g, murkC.g, m14)
            aboveB = lerp(waterC.b, murkC.b, m14)
        }
        val aboveA =
            if (clarity > 0f) lerp(.2f + .6f * murk, if (dark) .62f else .58f, clarity)
            else lerp(if (dark) .2f else .32f, .2f + .62f * murk, m14)
        run {
            val lsx = s.screenX(0f, levelL)
            val lsy = s.screenY(0f, levelL)
            c.save()
            c.setTransform(density, 0f, 0f, density, 0f, 0f)
            val lg = c.createLinearGradient(0f, lsy, 0f, s.oy)
            lg.addColorStop(0f, rgba(aboveR, aboveG, aboveB, aboveA * .82f))
            lg.addColorStop(1f, rgba(aboveR, aboveG, aboveB, min(1f, aboveA * 1.12f)))
            c.fillStyle = lg
            c.fillRect(0f, lsy, s.width, s.height - lsy)
            // Meniscus line.
            c.strokeStyle =
                if (clarity > .5f) rgba(255f, 214f, 150f, if (dark) .25f else .55f)
                else rgba(255f, 255f, 255f, if (dark) .25f else .55f)
            c.lineWidth = 1.2f * kp
            c.beginPath()
            c.moveTo(lsx - cw, lsy)
            c.lineTo(lsx + cw, lsy)
            c.stroke()
            c.restore()
        }
        // Below the mesh, once it's down, the pressed grounds.
        if (s.grains.isNotEmpty()) {
            val pileA = (s.grains.size.toFloat() / maxOf(1, s.total)).coerceIn(0f, 1f) * pP
            c.fillStyle = rgba(murkC.r, murkC.g, murkC.b, .75f * pileA)
            c.fillRect(-cw / 2, my, cw, gb - my)
        }
        // The summary, as faint lines rising through the clear coffee.
        if (clarity > 0f && outF < 1f) {
            c.strokeStyle = rgba(255f, 243f, 222f, .7f * clarity * (1 - outF))
            c.lineWidth = 1.6f * kp
            c.lineCap = "round"
            for (j in 0 until 3) {
                val y = lerp(my - 4 * kp, s.level0 + 7 * kp + j * 7 * kp, easeOut(clarity))
                val w = cw * SummaryLines[j]
                if (y > levelL + 2 * kp) {
                    c.beginPath()
                    c.moveTo(-w / 2, y)
                    c.lineTo(w / 2, y)
                    c.stroke()
                }
            }
        }
        // The grounds: each one a real word, tiny, on its swirl.
        if (s.grains.isNotEmpty()) {
            val gc = if (dark) GroundsDark else GroundsLight
            c.drawUnder { canvas ->
                for (i in s.grains.indices) {
                    val g = s.grains[i]
                    s.grainPos(g, out)
                    val z = out[2]
                    val sc = .3f * (1 + .14f * z)
                    canvas.save()
                    canvas.translate(out[0], out[1])
                    canvas.rotate((g.rot + g.th * .2f * (1 - pP)) * DEG)
                    canvas.scale(sc, sc)
                    // The prototype's middle baseline: the word centred on its box.
                    text.paintWord(
                        canvas,
                        g.word,
                        mixColor(gc, gc, 0f, .55f + .4f * (z + 1) / 2),
                        text.wordBase,
                    )
                    canvas.restore()
                }
            }
        }
        // Rod inside the glass, then the mesh.
        c.strokeStyle = if (dark) "#9ea2a8" else "#8d9097"
        c.lineWidth = 1.6f * kp
        c.beginPath()
        c.moveTo(0f, rimY)
        c.lineTo(0f, my)
        c.stroke()
        c.fillStyle = chromeMesh
        c.beginPath()
        c.ellipse(0f, my, cw / 2 - 1 * kp, 3 * kp, 0f, 0f, TAU)
        c.fill()
        c.save()
        c.beginPath()
        c.ellipse(0f, my, cw / 2 - 3 * kp, 2.2f * kp, 0f, 0f, TAU)
        c.clip()
        c.strokeStyle = if (dark) "rgba(20,20,24,.55)" else "rgba(60,62,68,.45)"
        c.lineWidth = .6f * kp
        var x = -cw / 2
        while (x < cw / 2) {
            c.beginPath()
            c.moveTo(x, my - 4 * kp)
            c.lineTo(x + 4 * kp, my + 4 * kp)
            c.stroke()
            c.beginPath()
            c.moveTo(x + 4 * kp, my - 4 * kp)
            c.lineTo(x, my + 4 * kp)
            c.stroke()
            x += 2.2f * kp
        }
        c.restore()
        c.restore() // glass clip

        // Glass edges and highlights.
        glassPath(c, s)
        c.strokeStyle = if (dark) "rgba(220,230,240,.28)" else "rgba(120,140,155,.45)"
        c.lineWidth = 1.3f * kp
        c.stroke()
        c.fillStyle = highlight
        c.fillRect(-cw / 2, rimY + 10 * kp, cw, ch - 14 * kp)

        // Handle, on the right, out of matte black.
        c.lineCap = "round"
        c.lineJoin = "round"
        c.strokeStyle = if (dark) "#0a0a0b" else "#1d1d20"
        c.lineWidth = 6.5f * kp
        c.beginPath()
        c.moveTo(cw / 2, rimY + 13 * kp)
        c.lineTo(cw / 2 + 12 * kp, rimY + 17 * kp)
        c.lineTo(cw / 2 + 13 * kp, gb - 18 * kp)
        c.lineTo(cw / 2, gb - 7 * kp)
        c.stroke()
        c.strokeStyle = "rgba(255,255,255,.14)"
        c.lineWidth = 1.4f * kp
        c.beginPath()
        c.moveTo(cw / 2 + 10.5f * kp, rimY + 18 * kp)
        c.lineTo(cw / 2 + 11 * kp, gb - 19 * kp)
        c.stroke()
        // Top band of the frame, round the glass under the rim.
        c.fillStyle = chromeBand
        c.beginPath()
        c.roundRect(-cw / 2 - 1.5f * kp, rimY + 8 * kp, cw + 3 * kp, 6 * kp, 2 * kp)
        c.fill()

        // Lid: a low chrome dome with a lip for the spout on the left.
        c.fillStyle = chromeLid
        c.beginPath()
        c.moveTo(-cw / 2 - 2.5f * kp, rimY + 3 * kp)
        c.lineTo(-cw / 2 - 2.5f * kp, rimY - 1.5f * kp)
        c.quadraticCurveTo(0f, rimY - 12 * kp, cw / 2 + 2.5f * kp, rimY - 1.5f * kp)
        c.lineTo(cw / 2 + 2.5f * kp, rimY + 3 * kp)
        c.closePath()
        c.fill()
        c.fillStyle = if (dark) "#3a3c40" else "#7d8188"
        c.beginPath()
        c.moveTo(-cw / 2 - 2 * kp, rimY - 2 * kp)
        c.lineTo(-cw / 2 - 8 * kp, rimY - 3.2f * kp)
        c.lineTo(-cw / 2 - 2 * kp, rimY + 2.5f * kp)
        c.closePath()
        c.fill()
        c.fillStyle = if (dark) "rgba(255,255,255,.2)" else "rgba(255,255,255,.7)"
        c.beginPath()
        c.ellipse(-cw * .18f, rimY - 5 * kp, cw * .16f, 1.2f * kp, -.12f, 0f, TAU)
        c.fill()
        // Rod above the lid and the knob.
        c.strokeStyle = chromeRod
        c.lineWidth = 2.6f * kp
        c.lineCap = "butt"
        c.beginPath()
        c.moveTo(0f, ky)
        c.lineTo(0f, rimY - 8 * kp)
        c.stroke()
        val kg = c.createRadialGradient(-2.5f * kp, ky - 3 * kp, 1 * kp, 0f, ky, 8 * kp)
        kg.addColorStop(0f, if (dark) "#55555a" else "#5a5a60")
        kg.addColorStop(.5f, "#1b1b1e")
        kg.addColorStop(1f, "#09090a")
        c.fillStyle = kg
        c.beginPath()
        c.arc(0f, ky, 7 * kp, 0f, TAU)
        c.fill()
        c.fillStyle = "rgba(255,255,255,.35)"
        c.beginPath()
        c.ellipse(-2.4f * kp, ky - 3 * kp, 2.2f * kp, 1.3f * kp, -.5f, 0f, TAU)
        c.fill()
        c.restore()
    }

    internal companion object {
        // The prototype's C: the water, the murk, the clear amber and the grounds.
        val WaterLight = Rgb(170f, 205f, 224f)
        val WaterDark = Rgb(110f, 150f, 175f)
        val MurkLight = Rgb(84f, 50f, 25f)
        val MurkDark = Rgb(70f, 44f, 24f)
        val AmberLight = Rgb(206f, 136f, 54f)
        val AmberDark = Rgb(214f, 142f, 62f)
        val GroundsLight = Rgb(62f, 37f, 19f)
        val GroundsDark = Rgb(146f, 100f, 62f)
        private val SummaryLines = floatArrayOf(.62f, .5f, .56f)
        private const val DEG = 180f / kotlin.math.PI.toFloat()

        fun bez(a: Float, c: Float, b: Float, t: Float) =
            (1 - t) * (1 - t) * a + 2 * (1 - t) * t * c + t * t * b

        fun cub(a: Float, b: Float, c: Float, d: Float, t: Float): Float {
            val u = 1 - t
            return u * u * u * a + 3 * u * u * t * b + 3 * u * t * t * c + t * t * t * d
        }

        // The prototype's rgba(mix(a, b, t), alpha), its channels cut to whole numbers.
        fun mixColor(a: Rgb, b: Rgb, t: Float, alpha: Float) =
            Color(
                red = lerp(a.r, b.r, t).toInt().coerceIn(0, 255),
                green = lerp(a.g, b.g, t).toInt().coerceIn(0, 255),
                blue = lerp(a.b, b.b, t).toInt().coerceIn(0, 255),
                alpha = (alpha.coerceIn(0f, 1f) * 255 + .5f).toInt(),
            )

        fun rgba(r: Float, g: Float, b: Float, a: Float): Color =
            Color(
                red = r.toInt().coerceIn(0, 255),
                green = g.toInt().coerceIn(0, 255),
                blue = b.toInt().coerceIn(0, 255),
                alpha = (a.coerceIn(0f, 1f) * 255 + .5f).toInt(),
            )
    }
}
