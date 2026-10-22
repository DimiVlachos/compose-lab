package dev.dimvlachos.lab.core.presentation.components.paperfan

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import dev.dimvlachos.lab.core.presentation.components.paperfan.PaperFanState.Companion.Ribs
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val Pi = PI.toFloat()
private const val Folds = Ribs * 2

/**
 * The fan's size for a card [cardWidth] wide (inside its border), as the prototype's `layout()`:
 * its radius, in dp, so it spans the card less a margin, between 110 and 172.
 */
internal fun fanRadius(cardWidth: Float): Float =
    (cardWidth / 2f - 12f).coerceIn(110f, 172f).roundToInt().toFloat()

/** The answer area's full height for a fan of radius [r]: the fan and a margin under its rivet. */
internal fun fanOpenHeight(r: Float): Float = r + 32f

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun ease(t: Float) = if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f

/** A CSS `rgba()` with a computed alpha, without a string to parse and cache. */
private fun rgba(r: Int, g: Int, b: Int, a: Float) =
    Color(r / 255f, g / 255f, b / 255f, a.coerceIn(0f, 1f))

/**
 * The washi's fibres, as the prototype's `fiber` canvas: 420 short curved strokes from a fixed seed
 * (the prototype's Lehmer generator, seeded 11) on a 256-unit tile, baked at [density].
 */
internal fun bakeFiber(density: Float): ImageBitmap {
    val px = ceil(256f * density).toInt()
    val x = Canvas2D.create(px, px)
    x.scale(density, density)
    var s = 11L
    fun r(): Float {
        s = (s * 16807L) % 2147483647L
        return (s.toDouble() / 2147483647.0).toFloat()
    }
    repeat(420) {
        val px0 = r() * 256f
        val py0 = r() * 256f
        val a = r() * Pi
        val l = 4f + r() * 14f
        x.strokeStyle =
            if (r() < .5f) rgba(150, 120, 80, .06f + r() * .1f)
            else rgba(255, 255, 245, .06f + r() * .1f)
        x.lineWidth = .4f + r() * .5f
        x.beginPath()
        x.moveTo(px0, py0)
        x.quadraticCurveTo(
            px0 + cos(a) * l * .5f + (r() - .5f) * 3f,
            py0 + sin(a) * l * .5f,
            px0 + cos(a) * l,
            py0 + sin(a) * l,
        )
        x.stroke()
    }
    return x.bitmap.also { it.freeze() }
}

/** A fan's print in its open pose, and whether its answer fitted on it. */
internal class FanPrint(val image: ImageBitmap, val fit: Boolean)

/** A line of the answer and its baseline, from the rivet. */
internal class PrintLine(val text: String, val y: Float)

/**
 * Breaks [text] into lines that fit the half ring of a fan of radius [r] (inner [rIn]) at [size]
 * px, as the prototype's `fitLines()`: short lines near the top of the fan, longer ones lower down,
 * starting as low as it can. Null if it doesn't fit.
 */
internal fun fitLines(
    measure: (String) -> Float,
    text: String,
    size: Float,
    r: Float,
    rIn: Float,
): List<PrintLine>? {
    val lh = size * 1.3f
    val rt = r - 17f
    val words = text.split(' ')
    val slotY = ArrayList<Float>()
    val slotHalf = ArrayList<Float>()
    var top = -rt + 7f
    while (true) {
        val bot = top + lh - 2f
        if (-bot < rIn + 6f) break
        slotY += top + size
        slotHalf += sqrt(max(0f, rt * rt - top * top)) - 9f
        top += lh
    }
    for (off in slotY.size - 1 downTo 0) {
        val lines = ArrayList<PrintLine>()
        var w = 0
        var ok = true
        var si = off
        while (si < slotY.size && w < words.size) {
            var line = words[w++]
            val room = slotHalf[si] * 2f
            while (w < words.size && measure(line + " " + words[w]) <= room) line +=
                " " + words[w++]
            if (measure(line) > room) ok = false
            lines += PrintLine(line, slotY[si])
            si++
        }
        if (ok && w >= words.size && off <= ceil((slotY.size - lines.size) / 2f)) return lines
    }
    return null
}

/**
 * The fan's print, drawn once in its fully open pose with the rivet at (R + 2, R + 2), as the
 * prototype's `makePrint()`: warm washi with its fibres, an indigo band along the rim, a pine
 * sprig, the shop's red seal and [answer] set in [serif] to fit the half ring.
 */
internal fun bakePrint(
    answer: String,
    r: Float,
    density: Float,
    fiber: ImageBitmap,
    measurer: TextMeasurer,
    serif: FontFamily,
): FanPrint {
    val rIn = r * .3f
    val x = Canvas2D.create(ceil((2 * r + 4) * density).toInt(), ceil((r + 4) * density).toInt())
    x.textMeasurer = measurer
    x.fonts = mapOf("serif" to serif)
    x.scale(density, density)
    x.translate(r + 2, r + 2)
    x.save()
    x.beginPath()
    x.arc(0f, 0f, r, Pi, 0f)
    x.arc(0f, 0f, rIn, 0f, Pi, true)
    x.closePath()
    x.clip()
    // Warm washi paper, a touch lighter toward the rim.
    val g = x.createRadialGradient(0f, 0f, rIn, 0f, 0f, r)
    g.addColorStop(0f, "#efe4cf")
    g.addColorStop(1f, "#fbf5e8")
    x.fillStyle = g
    x.fillRect(-r, -r, 2 * r, r)
    x.globalAlpha = .9f
    var fx = -r
    while (fx < r) {
        var fy = -r
        while (fy < 0f) {
            x.drawImage(fiber, fx, fy, 256f, 256f)
            fy += 256f
        }
        fx += 256f
    }
    x.globalAlpha = 1f
    // Indigo band printed along the rim, with a hairline inside it.
    x.lineWidth = 6f
    x.strokeStyle = "rgba(45,67,112,.88)"
    x.beginPath()
    x.arc(0f, 0f, r - 5, Pi, 0f)
    x.stroke()
    x.lineWidth = .8f
    x.strokeStyle = "rgba(45,67,112,.6)"
    x.beginPath()
    x.arc(0f, 0f, r - 12, Pi, 0f)
    x.stroke()
    x.lineWidth = .8f
    x.strokeStyle = "rgba(45,67,112,.35)"
    x.beginPath()
    x.arc(0f, 0f, rIn + 4, Pi, 0f)
    x.stroke()
    pineSprig(x, r)
    // The shop's red seal, lower right.
    x.save()
    x.translate(r * .62f, -r * .2f)
    x.rotate(.12f)
    x.fillStyle = "rgba(178,44,36,.82)"
    x.beginPath()
    x.arc(0f, 0f, 9.5f, 0f, Pi * 2)
    x.fill()
    x.fillStyle = "#fbf5e8"
    x.font = "700 8px serif"
    x.textAlign = "center"
    x.textBaseline = "middle"
    x.fillText("CC", 0f, .5f)
    x.restore()
    // The answer, printed in sumi-dark ink; the type steps down a size if it doesn't fit.
    var lines: List<PrintLine>? = null
    var size = 13f
    while (size >= 10.5f && lines == null) {
        x.font = "400 ${size}px serif"
        lines = fitLines({ x.measureText(it) }, answer, size, r, rIn)
        if (lines == null) size -= .5f
    }
    x.fillStyle = "#2b241d"
    x.textAlign = "center"
    x.textBaseline = "alphabetic"
    for (l in lines.orEmpty()) x.fillText(l.text, 0f, l.y)
    x.restore()
    return FanPrint(x.bitmap.also { it.freeze() }, lines != null)
}

/**
 * A pine sprig at lower left, in soft ink: a gently curved twig that thins toward its tip, tufts of
 * needles fanning forward from it, shorter toward the tip, a short side shoot and a small cone
 * hanging below.
 */
private fun pineSprig(x: Canvas2D, r: Float) {
    x.save()
    x.translate(-r * .66f, -r * .17f)
    x.rotate(-.5f)
    x.strokeStyle = "rgba(52,84,66,.55)"
    x.fillStyle = "rgba(52,84,66,.55)"
    x.lineCap = "round"
    // The twig: a quadratic curve, filled as a tapering band (1.7 wide at the base, .6 at the tip).
    fun twX(t: Float): Float {
        val u = 1 - t
        return u * u * -20f + 2 * u * t * -2f + t * t * 20f
    }
    fun twY(t: Float): Float {
        val u = 1 - t
        return u * u * 8f + 2 * u * t * 2f + t * t * -5f
    }
    fun tang(t: Float): Float {
        val u = 1 - t
        return atan2(
            2 * u * (2f - 8f) + 2 * t * (-5f - 2f),
            2 * u * (-2f + 20f) + 2 * t * (20f + 2f),
        )
    }
    val sideX = FloatArray(17)
    val sideY = FloatArray(17)
    val otherX = FloatArray(17)
    val otherY = FloatArray(17)
    for (n in 0..16) {
        val t = n / 16f
        val bx = twX(t)
        val by = twY(t)
        val a = tang(t)
        val hw = lerp(.85f, .3f, t)
        sideX[n] = bx - sin(a) * hw
        sideY[n] = by + cos(a) * hw
        otherX[16 - n] = bx + sin(a) * hw
        otherY[16 - n] = by - cos(a) * hw
    }
    x.beginPath()
    x.moveTo(sideX[0], sideY[0])
    for (n in 1..16) x.lineTo(sideX[n], sideY[n])
    for (n in 0..16) x.lineTo(otherX[n], otherY[n])
    x.closePath()
    x.fill()
    // Needles, all in one path so crossings don't darken: each tuft fans from a point on the twig,
    // swept toward the tip.
    fun tuft(bx: Float, by: Float, a: Float, len: Float, tip: Boolean) {
        val fan = ArrayList<Float>()
        if (tip) {
            fan += -.1f
            fan += .1f
        }
        for (k in 0 until 3) {
            fan += -(.3f + k * .24f)
            fan += .3f + k * .24f
        }
        for (d in fan) {
            val l = len * (1 - abs(d) * .3f)
            val ang = a + d
            val bend = a + d * .7f
            x.moveTo(bx, by)
            x.quadraticCurveTo(
                bx + cos(bend) * l * .55f,
                by + sin(bend) * l * .55f,
                bx + cos(ang) * l,
                by + sin(ang) * l,
            )
        }
    }
    x.lineWidth = .6f
    x.beginPath()
    for (t in floatArrayOf(.1f, .3f, .5f, .7f, .86f, 1f)) {
        tuft(twX(t), twY(t), tang(t), lerp(12f, 6.5f, t), t == 1f)
    }
    // The side shoot leaves the twig upward and ends in its own small tuft.
    val sx = twX(.4f)
    val sy = twY(.4f)
    val sa = tang(.4f) - .7f
    val ex = sx + cos(sa) * 9f
    val ey = sy + sin(sa) * 9f
    tuft(ex, ey, sa, 7f, true)
    x.stroke()
    x.lineWidth = .8f
    x.beginPath()
    x.moveTo(sx, sy)
    x.lineTo(ex, ey)
    x.stroke()
    // A small cone hanging below the twig, its scales knocked out in paper colour.
    val cx0 = twX(.26f)
    val cy0 = twY(.26f)
    val ca = Pi / 2 + .5f
    x.save()
    x.translate(cx0 + cos(ca) * 6.4f, cy0 + sin(ca) * 6.4f)
    x.rotate(ca)
    x.lineWidth = .7f
    x.beginPath()
    x.moveTo(-6.4f, 0f)
    x.lineTo(-3.6f, 0f)
    x.stroke()
    x.beginPath()
    x.ellipse(0f, 0f, 4f, 2.7f, 0f, 0f, Pi * 2)
    x.fill()
    x.strokeStyle = "rgba(251,245,232,.75)"
    x.lineWidth = .55f
    for (u in floatArrayOf(-1.6f, .2f, 2f)) {
        x.beginPath()
        x.moveTo(u - 1.2f, -1.9f)
        x.lineTo(u + .3f, 0f)
        x.lineTo(u - 1.2f, 1.9f)
        x.stroke()
    }
    x.restore()
    x.restore()
}

/**
 * Where each question's row and answer area sit, in the fans' canvas (dp), which the drawing reads;
 * and where each fan's rivet was last drawn, which a finger's drag is measured from.
 */
internal class FanSlots(count: Int) {
    val rowX = FloatArray(count)
    val rowY = FloatArray(count)
    val rowW = FloatArray(count)
    val rowH = FloatArray(count)
    val panelX = FloatArray(count)
    val panelY = FloatArray(count)
    val panelW = FloatArray(count)
    val panelH = FloatArray(count)
    val pivotX = FloatArray(count)
    val pivotY = FloatArray(count)

    /** The open fan's radius, in dp. */
    var radius = 0f
}

/**
 * Draws the fans as the prototype's `draw()` and `drawFan()` do, call by call. It keeps the fold
 * angles between frames, so a frame allocates no arrays of its own.
 */
internal class PaperFanPainter(count: Int) {
    private val rib = FloatArray(Ribs + 1)
    private val fold = FloatArray(Folds + 1)
    private val order = IntArray(count) { it }

    /**
     * Draws every fan of [state] into [c] (whose units are dp), over a canvas [width] by [height]:
     * folded ones first, so an open one lies over its neighbours while it travels. [prints] gives
     * fan `i`'s print, or null while it isn't ready. Shadow sizes are in dp, so [density] turns
     * them into the canvas's pixels.
     */
    fun draw(
        c: Canvas2D,
        state: PaperFanState,
        slots: FanSlots,
        prints: (Int) -> ImageBitmap?,
        r: Float,
        width: Float,
        height: Float,
        dark: Boolean,
        density: Float,
    ) {
        slots.radius = r
        c.save()
        c.beginPath()
        c.rect(0f, 0f, width, height)
        c.clip()
        // Folded fans first, so an open one lies over its neighbours while it travels.
        for (i in order.indices) order[i] = i
        for (a in 1 until order.size) {
            val v = order[a]
            var b = a - 1
            while (b >= 0 && state.travel(order[b]) > state.travel(v)) {
                order[b + 1] = order[b]
                b--
            }
            order[b + 1] = v
        }
        for (i in order) {
            val tr = state.travel(i)
            val e = ease(tr.coerceIn(0f, 1f))
            val cx = slots.rowX[i] + slots.rowW[i] - 16f
            val cy = slots.rowY[i] + slots.rowH[i] / 2f + 9f
            val pbX = slots.panelX[i]
            val pbY = slots.panelY[i]
            val pbW = slots.panelW[i]
            val pbH = slots.panelH[i]
            val ox = pbX + pbW / 2f
            val oy = pbY + pbH - 16f
            // The fan drops out of the row first and slides over, turns and grows a beat later,
            // so its guard stays below the question text.
            val eDrop = ease((tr / .65f).coerceIn(0f, 1f))
            val eTurn = ease(((tr - .2f) / .8f).coerceIn(0f, 1f))
            val px = lerp(cx, ox, eTurn)
            val py = lerp(cy, oy, eDrop)
            val sc = lerp(.27f, 1f, eTurn)
            val rot = lerp(.42f, 0f, eTurn)
            slots.pivotX[i] = px
            slots.pivotY[i] = py
            if (tr > .02f && pbH > 1f) {
                // The lamp's pool of light on the card, gathering as the fan arrives: an oval
                // squeezed to the panel's height, so it fades to nothing before its top and
                // bottom edges.
                val ph = pbH / 2f
                c.save()
                c.beginPath()
                c.rect(pbX, pbY, pbW, pbH)
                c.clip()
                c.translate(ox - r * .2f, pbY + ph)
                c.scale(1f, ph / (r * 1.2f))
                val pool = c.createRadialGradient(0f, 0f, 0f, 0f, 0f, r * 1.2f)
                pool.addColorStop(
                    0f,
                    if (dark) rgba(255, 200, 130, .07f * e) else rgba(255, 226, 170, .35f * e),
                )
                pool.addColorStop(1f, "rgba(255,226,170,0)")
                c.fillStyle = pool
                c.fillRect(-r * 1.2f, -r * 1.2f, r * 2.4f, r * 2.4f)
                c.restore()
            }
            drawFan(c, state, i, prints(i), r, px, py, sc, rot, dark, density)
        }
        c.restore()
    }

    private fun foldAngles(state: PaperFanState, i: Int) {
        for (r in 0..Ribs) rib[r] =
            -Pi + Pi * (r.toFloat() / Ribs) * state.spread(i, r).coerceIn(0f, 1.05f)
        for (r in 1..Ribs) if (rib[r] < rib[r - 1] + .003f) rib[r] = rib[r - 1] + .003f
        for (j in 0..Folds) fold[j] =
            if (j % 2 == 1) (rib[(j - 1) / 2] + rib[(j + 1) / 2]) / 2f else rib[j / 2]
    }

    private fun wedge(c: Canvas2D, a0: Float, a1: Float, eps: Float, r: Float, rIn: Float) {
        val b0 = a0 - eps
        val b1 = a1 + eps
        c.beginPath()
        c.moveTo(cos(b0) * rIn, sin(b0) * rIn)
        c.lineTo(cos(b0) * (r + .5f), sin(b0) * (r + .5f))
        c.lineTo(cos(b1) * (r + .5f), sin(b1) * (r + .5f))
        c.lineTo(cos(b1) * rIn, sin(b1) * rIn)
        c.closePath()
    }

    private fun stick(
        c: Canvas2D,
        a: Float,
        r0: Float,
        r1: Float,
        w0: Float,
        w1: Float,
        fill: String,
        edge: String,
        hi: String,
    ) {
        val ca = cos(a)
        val sa = sin(a)
        val nx = -sa
        val ny = ca
        c.beginPath()
        c.moveTo(ca * r0 + nx * w0 / 2, sa * r0 + ny * w0 / 2)
        c.lineTo(ca * r1 + nx * w1 / 2, sa * r1 + ny * w1 / 2)
        c.lineTo(ca * r1 - nx * w1 / 2, sa * r1 - ny * w1 / 2)
        c.lineTo(ca * r0 - nx * w0 / 2, sa * r0 - ny * w0 / 2)
        c.closePath()
        c.fillStyle = fill
        c.fill()
        c.strokeStyle = edge
        c.lineWidth = .7f
        c.stroke()
        c.strokeStyle = hi
        c.lineWidth = max(.6f, w1 * .28f)
        c.beginPath()
        c.moveTo(ca * (r0 + 3) + nx * w0 * .18f, sa * (r0 + 3) + ny * w0 * .18f)
        c.lineTo(ca * (r1 - 2) + nx * w1 * .18f, sa * (r1 - 2) + ny * w1 * .18f)
        c.stroke()
    }

    private fun drawFan(
        c: Canvas2D,
        state: PaperFanState,
        i: Int,
        print: ImageBitmap?,
        r: Float,
        px: Float,
        py: Float,
        sc: Float,
        rot: Float,
        dark: Boolean,
        density: Float,
    ) {
        val rIn = r * .3f
        foldAngles(state, i)
        val spread = fold[Folds] - fold[0]
        // Soft shadow on the card, cast down and right from the lamp. Only the shadow shows: the
        // shape itself is filled clear.
        if (spread > .05f) {
            c.save()
            c.translate(px, py)
            c.scale(sc, sc)
            c.rotate(rot)
            c.shadowColor = if (dark) "rgba(0,0,0,.55)" else "rgba(80,55,25,.26)"
            c.shadowBlur = 16f * sc * density
            c.shadowOffsetX = 5f * sc * density
            c.shadowOffsetY = 8f * sc * density
            c.beginPath()
            c.moveTo(0f, 0f)
            for (j in 0..Folds) c.lineTo(cos(fold[j]) * r, sin(fold[j]) * r)
            c.closePath()
            c.fillStyle = Color.Transparent
            c.fill()
            c.restore()
        }
        c.save()
        c.translate(px, py)
        c.scale(sc, sc)
        c.rotate(rot)
        // Back guard and the bamboo ribs, which show between the rivet and the paper.
        stick(c, rib[0], -9f, r + 1, 6.4f, 4.6f, "#3d2418", "#1c0f08", "rgba(214,150,110,.45)")
        for (k in 1 until Ribs) {
            stick(
                c,
                rib[k],
                -8f,
                rIn + 8,
                4.2f,
                3.2f,
                "#c99a5c",
                "#83592b",
                "rgba(255,236,196,.55)",
            )
        }
        // The pleated leaf: every pleat draws its slice of the open print, rotated and squeezed
        // into place.
        if (spread > .02f && print != null) {
            for (j in 0 until Folds) {
                val a0 = fold[j]
                val a1 = fold[j + 1]
                val span = a1 - a0
                if (span < .0008f) continue
                val kk = span / (Pi / Folds)
                val mid = (a0 + a1) / 2f
                val omid = (-Pi + Pi * j / Folds + -Pi + Pi * (j + 1) / Folds) / 2f
                c.save()
                wedge(c, a0, a1, .0025f, r, rIn)
                c.clip()
                c.save()
                c.rotate(mid)
                c.scale(1f, kk)
                c.rotate(-omid)
                c.drawImage(print, -r - 2, -r - 2, 2 * r + 4, r + 4)
                c.restore()
                // Pleats facing the lamp are lit, the others shaded; steeper pleats show more
                // contrast.
                val steep = 1f - kk.coerceIn(0f, 1f)
                val lightDir = cos(mid - (-Pi * .72f))
                val a = (.085f + .24f * steep) * (.65f + .35f * lightDir)
                c.fillStyle = if (j % 2 == 0) rgba(255, 250, 236, a * .9f) else rgba(70, 45, 20, a)
                c.fill()
                // A gentle darkening toward the inner edge, where the pleats bunch.
                val g = c.createRadialGradient(0f, 0f, rIn, 0f, 0f, r)
                g.addColorStop(0f, rgba(90, 60, 30, .12f + .1f * steep))
                g.addColorStop(.35f, "rgba(90,60,30,0)")
                c.fillStyle = g
                c.fill()
                c.restore()
            }
            // Creases: valleys dark, mountains catch a thin highlight.
            c.lineWidth = .7f
            for (j in 1 until Folds) {
                c.strokeStyle = if (j % 2 == 1) "rgba(255,252,240,.6)" else "rgba(90,62,30,.4)"
                c.beginPath()
                c.moveTo(cos(fold[j]) * rIn, sin(fold[j]) * rIn)
                c.lineTo(cos(fold[j]) * r, sin(fold[j]) * r)
                c.stroke()
            }
            // The rim: straight runs between folds, like real pleated paper.
            c.beginPath()
            c.moveTo(cos(fold[0]) * r, sin(fold[0]) * r)
            for (j in 1..Folds) c.lineTo(cos(fold[j]) * r, sin(fold[j]) * r)
            c.strokeStyle = "rgba(70,50,25,.35)"
            c.lineWidth = .8f
            c.stroke()
            // Lamp glow from the upper left across the open paper; dimmer in a dark room.
            c.save()
            c.beginPath()
            c.moveTo(cos(fold[0]) * rIn, sin(fold[0]) * rIn)
            for (j in 0..Folds) c.lineTo(cos(fold[j]) * r, sin(fold[j]) * r)
            for (j in Folds downTo 0) c.lineTo(cos(fold[j]) * rIn, sin(fold[j]) * rIn)
            c.closePath()
            c.clip()
            c.globalCompositeOperation = "lighter"
            c.fillStyle = glow(c, r)
            c.fillRect(-r - 4, -r - 4, 2 * r + 8, r + 8)
            c.globalCompositeOperation = "source-over"
            if (dark) {
                c.fillStyle = "rgba(16,12,8,.2)"
                c.fillRect(-r - 4, -r - 4, 2 * r + 8, r + 8)
            }
            c.restore()
        }
        // Folded, the paper shows as a cream sliver along the stack.
        val shut = 1f - (spread * 3f).coerceIn(0f, 1f)
        if (shut > 0f) {
            c.save()
            c.globalAlpha = shut
            val a = fold[Folds] - .045f
            c.strokeStyle = "#efe3cc"
            c.lineWidth = 9f
            c.lineCap = "butt"
            c.beginPath()
            c.moveTo(cos(a) * rIn, sin(a) * rIn)
            c.lineTo(cos(a) * (r - 1), sin(a) * (r - 1))
            c.stroke()
            c.strokeStyle = "rgba(45,67,112,.9)"
            c.lineWidth = 9f
            c.beginPath()
            c.moveTo(cos(a) * (r - 8), sin(a) * (r - 8))
            c.lineTo(cos(a) * (r - 1), sin(a) * (r - 1))
            c.stroke()
            c.restore()
        }
        // Front guard, lacquered, over the paper.
        stick(c, rib[Ribs], -9f, r + 1, 6.4f, 4.6f, "#43281b", "#1c0f08", "rgba(232,170,128,.6)")
        // Brass rivet.
        c.fillStyle = rivet
        c.beginPath()
        c.arc(0f, 0f, 3.6f, 0f, Pi * 2)
        c.fill()
        c.restore()
    }

    private val rivet =
        Gradient2D.radial(-1f, -1.2f, 0f, 0f, 0f, 4f).apply {
            addColorStop(0f, "#fff3cf")
            addColorStop(.5f, "#c99a45")
            addColorStop(1f, "#5e400c")
        }

    private var glowFor = -1f
    private var glowGradient: Gradient2D? = null

    // The lamp's glow depends only on the fan's size, so it is made once for it.
    private fun glow(c: Canvas2D, r: Float): Gradient2D {
        glowGradient
            ?.takeIf { glowFor == r }
            ?.let {
                return it
            }
        return c.createRadialGradient(-r * .55f, -r * 1.05f, 0f, -r * .55f, -r * 1.05f, r * 1.7f)
            .apply {
                addColorStop(0f, "rgba(255,236,196,.2)")
                addColorStop(1f, "rgba(255,236,196,0)")
            }
            .also {
                glowGradient = it
                glowFor = r
            }
    }
}
