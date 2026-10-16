package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.canvas2d.mulberry
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// A card in the prototype's world units: its width, its body's height and the strip above the body
// a divider's tab stands in.
internal const val CW = 1.75f
internal const val CH = 1f
internal const val TH = .17f

/**
 * What is painted on a card: a divider's letter and tab, or a contact's typed and written lines.
 */
internal sealed interface PaperCard {
    /** The [ordinal]th divider; its tab steps along the card as the prototype's do. */
    data class Divider(val letter: Char, val ordinal: Int) : PaperCard

    /** A contact's card: [name] typed, [kind] in small capitals, [lines] written in ink. */
    data class Contact(
        val name: String,
        val kind: String,
        val lines: List<String>,
        val seed: Int,
    ) : PaperCard
}

/**
 * What the cards are painted with: the [measurer] that sets their text, the [fonts] it sets it in
 * (TYPED, HAND and SANS, as the prototype names them) and the paper's [grain].
 */
internal class CardInk(
    val measurer: TextMeasurer,
    val fonts: Map<String, FontFamily>,
    val grain: ImageBitmap,
)

private fun mk(w: Float, h: Float, dpr: Float): Canvas2D {
    val c = Canvas2D.create(max(1, ceil(w * dpr).toInt()), max(1, ceil(h * dpr).toInt()))
    c.setTransform(dpr, 0f, 0f, dpr, 0f, 0f)
    return c
}

private fun channel(v: Double) = v.roundToInt().coerceIn(0, 255)

/** The paper's grain: warm noise with short fibres through it, tiled across every card. */
internal fun makeGrain(): ImageBitmap {
    val image = ImageBitmap(160, 160)
    val canvas = Canvas(image)
    val r = mulberry(7)
    val dot = Paint()
    for (i in 0 until 160 * 160) {
        val v = 200 + r() * 55
        dot.color = Color(red = channel(v), green = channel(v * .97), blue = channel(v * .9))
        val x = (i % 160).toFloat()
        val y = (i / 160).toFloat()
        canvas.drawRect(x, y, x + 1f, y + 1f, dot)
    }
    val g = Canvas2D(canvas, 160, 160)
    for (i in 0 until 260) {
        g.strokeStyle =
            Color(red = 120, green = 95, blue = 60, alpha = ((.05 + r() * .08) * 255).toInt())
        g.lineWidth = .5f
        val x = (r() * 160).toFloat()
        val y = (r() * 160).toFloat()
        val a = (r() * TAU).toFloat()
        val l = (2 + r() * 7).toFloat()
        g.beginPath()
        g.moveTo(x, y)
        g.lineTo(x + cos(a) * l, y + sin(a) * l)
        g.stroke()
    }
    return image
}

/** Where the [ordinal]th divider's tab starts, as a share of the card's width. */
internal fun tabX(ordinal: Int) = .08f + (ordinal % 5) * .318f

private fun outline(g: Canvas2D, card: PaperCard, s: Float) {
    val top = TH * s
    val bw = CW * s
    val bh = CH * s
    val rad = .04f * s
    g.beginPath()
    g.roundRect(0f, top, bw, bh, rad)
    if (card is PaperCard.Divider) {
        val x0 = tabX(card.ordinal) * s
        val w = .3f * s
        g.roundRect(x0, .006f * s, w, top + rad * 2, floatArrayOf(.045f * s, .045f * s, 0f, 0f))
    }
}

private fun notches(g: Canvas2D, s: Float) {
    val bottom = (TH + CH) * s
    g.save()
    g.globalCompositeOperation = "destination-out"
    for (fx in floatArrayOf(.34f, .66f)) {
        val x = CW * s * fx
        g.beginPath()
        g.arc(x, bottom - .085f * s, .042f * s, 0f, TAU)
        g.fill()
        g.fillRect(x - .013f * s, bottom - .09f * s, .026f * s, .1f * s)
    }
    g.restore()
}

private fun paperBase(g: Canvas2D, card: PaperCard, s: Float, back: Boolean, grain: ImageBitmap) {
    val w = CW * s
    val h = (CH + TH) * s
    val r =
        mulberry(
            when (card) {
                is PaperCard.Contact -> card.seed
                is PaperCard.Divider -> (card.ordinal + 3) * 97
            }
        )
    outline(g, card, s)
    val base: Any =
        when {
            card is PaperCard.Divider -> if (back) "#d2b981" else "#dfc690"
            back -> "#ece3cf"
            else -> {
                val hue = (40 + r() * 6).toFloat()
                val saturation = ((52 + r() * 14) / 100).toFloat()
                val lightness = ((92 + r() * 2.5) / 100).toFloat()
                Color.hsl(hue, saturation, lightness)
            }
        }
    g.fillStyle = base
    g.fill()
    g.save()
    outline(g, card, s)
    g.clip()
    g.globalCompositeOperation = "multiply"
    g.globalAlpha = .55f
    g.fillStyle = g.createPattern(grain)
    g.save()
    val gs = max(.6f, s / 220f)
    g.scale(gs, gs)
    g.fillRect(0f, 0f, w * 4, h * 4)
    g.restore()
    g.globalAlpha = 1f
    g.globalCompositeOperation = "source-over"
    // Lamp light from the top left, falling off toward the rail.
    val lg = g.createLinearGradient(0f, 0f, w * .7f, h)
    lg.addColorStop(0f, "rgba(255,240,205,.18)")
    lg.addColorStop(.6f, "rgba(255,255,255,0)")
    lg.addColorStop(1f, "rgba(90,60,25,.14)")
    g.fillStyle = lg
    g.fillRect(0f, 0f, w, h)
    g.restore()
}

private fun edgeStroke(g: Canvas2D, card: PaperCard, s: Float) {
    g.save()
    outline(g, card, s)
    g.strokeStyle = if (card is PaperCard.Divider) "rgba(110,80,35,.55)" else "rgba(120,98,70,.45)"
    g.lineWidth = max(.6f, .006f * s)
    g.stroke()
    g.restore()
}

/**
 * A card's face at [s] px to the world unit and a pixel ratio of [dpr]: a divider's letter on its
 * tab, or a contact's name typed twice over, as a typewriter strikes, with their number, line and
 * note written in blue ink on the rules.
 */
internal fun bakeFace(card: PaperCard, s: Float, dpr: Float, ink: CardInk): Canvas2D {
    val g = mk(CW * s, (CH + TH) * s, dpr)
    g.textMeasurer = ink.measurer
    g.fonts = ink.fonts
    val top = TH * s
    val bw = CW * s
    paperBase(g, card, s, false, ink.grain)
    when (card) {
        is PaperCard.Divider -> {
            val letter = card.letter.toString()
            g.fillStyle = "#3d2c18"
            g.font = "800 ${.12f * s}px SANS"
            g.textAlign = "center"
            g.textBaseline = "middle"
            g.fillText(letter, (tabX(card.ordinal) + .15f) * s, top * .56f)
            g.fillStyle = "rgba(80,55,25,.13)"
            g.font = "800 ${.62f * s}px SANS"
            g.fillText(letter, bw * .5f, top + CH * s * .55f)
            g.strokeStyle = "rgba(110,80,35,.28)"
            g.lineWidth = .006f * s
            g.beginPath()
            g.rect(.06f * s, top + .06f * s, bw - .12f * s, CH * s - .2f * s)
            g.stroke()
        }
        is PaperCard.Contact -> {
            val r = mulberry(card.seed)
            val red = top + .25f * s
            g.fillStyle = "rgba(205,72,62,.6)"
            g.fillRect(.03f * s, red, bw - .06f * s, max(.7f, .007f * s))
            for (i in 1..5) {
                val y = red + .126f * s * i
                if (y > top + CH * s - .12f * s) break
                g.fillStyle = "rgba(80,120,190,.3)"
                g.fillRect(.03f * s, y, bw - .06f * s, max(.5f, .005f * s))
            }
            // Typed name, struck twice so it reads as a typewriter.
            g.textBaseline = "alphabetic"
            g.textAlign = "left"
            var fs = .118f * s
            g.font = "700 ${fs}px TYPED"
            while (g.measureText(card.name) > bw * .62f && fs > .07f * s) {
                fs *= .94f
                g.font = "700 ${fs}px TYPED"
            }
            g.fillStyle = "rgba(29,26,23,.35)"
            g.fillText(card.name, .075f * s + .004f * s, red - .055f * s + .003f * s)
            g.fillStyle = "#211d1a"
            g.fillText(card.name, .075f * s, red - .055f * s)
            g.font = "400 ${.058f * s}px TYPED"
            g.textAlign = "right"
            g.fillStyle = "#86694c"
            g.fillText(card.kind.uppercase(), bw - .07f * s, red - .058f * s)
            // Handwriting in blue ink, on the rules.
            g.textAlign = "left"
            card.lines.forEachIndexed { i, t ->
                val y = red + .126f * s * (i + 1) - .018f * s
                g.save()
                g.translate(.09f * s + r().toFloat() * .03f * s, y)
                g.rotate(((r() - .5) * .025).toFloat())
                var hs = (if (i == 0) .118f else .1f) * s
                g.font = "600 ${hs}px HAND"
                while (g.measureText(t) > bw * .84f && hs > .06f * s) {
                    hs *= .94f
                    g.font = "600 ${hs}px HAND"
                }
                g.fillStyle = if (i == 0) "#1f3778" else "rgba(31,55,120,.88)"
                g.fillText(t, 0f, 0f)
                g.restore()
            }
        }
    }
    edgeStroke(g, card, s)
    notches(g, s)
    return g
}

/** A card's plain back, as a card fallen over the top shows it. */
internal fun bakeBack(card: PaperCard, s: Float, dpr: Float, ink: CardInk): Canvas2D {
    val g = mk(CW * s, (CH + TH) * s, dpr)
    paperBase(g, card, s, true, ink.grain)
    if (card !is PaperCard.Divider) {
        g.fillStyle = "rgba(205,72,62,.12)"
        g.fillRect(.03f * s, (TH + .25f) * s, CW * s - .06f * s, .007f * s)
    }
    g.save()
    outline(g, card, s)
    g.clip()
    g.fillStyle = "rgba(60,40,20,.1)"
    g.fillRect(0f, 0f, CW * s, (CH + TH) * s)
    g.restore()
    edgeStroke(g, card, s)
    notches(g, s)
    return g
}
