package dev.dimvlachos.lab.core.presentation.components.frostcard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import dev.dimvlachos.lab.core.presentation.canvas2d.imageBitmapOf
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** The families the card's art is lettered in, by the names its font strings use. */
internal const val FrostSerif = "frost-serif"
internal const val FrostMono = "frost-mono"

/**
 * Draws the prototype's card as its canvas does, call for call: the plum-and-copper debit card
 * (baked once per size), the frost that grows over it, the haze that drains its colour, the warm
 * hole a thumb opens, the wet lip round it, the glints, and the beads of melt and their streaks.
 * Lengths are in dp; [density] is how many pixels a dp is, the resolution the layers are baked at.
 */
internal class FrostCardPainter(
    private val density: Float,
    private val measurer: TextMeasurer,
    private val fonts: Map<String, FontFamily>,
) {
    private var cw = 0f
    private var ch = 0f
    private var cr = 14f
    private var card: ImageBitmap? = null
    private var shadow: ImageBitmap? = null

    // The frost layer once every segment is in, copied into a frozen bitmap: a changeable one is
    // copied by Skia each time it is drawn.
    private var frostDone: ImageBitmap? = null
    private var cold: ImageBitmap? = null
    private var frost: Canvas2D? = null
    private var growth: FrostGrowth? = null
    private var drawnIdx = 0
    private var drawnF = -1f
    private var hazeF = -1f
    private var haze: ImageBitmap? = null
    private var hazePixels = IntArray(0)
    private var glint: ImageBitmap? = null
    private var bead: ImageBitmap? = null
    private var icyBead: ImageBitmap? = null

    // Paints and a path for the water and the hole, reused every frame.
    private val fillPaint = Paint()
    private val strokePaint =
        Paint().apply {
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
        }
    private val imagePaint = Paint()
    private val hole = Path()

    /** Bakes the card and its cold copy and makes the frost layer, for a card of [w] by [h] dp. */
    private fun build(w: Float, h: Float, g: FrostGrowth) {
        cw = w
        ch = h
        cr = max(10f, w * .045f)
        growth = g
        val pw = (w * density).roundToInt().coerceAtLeast(1)
        val ph = (h * density).roundToInt().coerceAtLeast(1)
        val c = Canvas2D.create(pw, ph)
        c.textMeasurer = measurer
        c.fonts = fonts
        c.textResolution = density
        c.scale(density, density)
        drawCard(c, w, h)
        card = c.bitmap.also { it.freeze() }
        // The card's box-shadow, 0 14px 26px -12px and then 0 2px 5px, baked once with a margin.
        val m = ShadowMargin
        val sh =
            Canvas2D.create(
                ((w + 2 * m) * density).roundToInt(),
                ((h + 2 * m) * density).roundToInt(),
            )
        sh.scale(density, density)
        sh.translate(m, m)
        // The adapter casts a fill's shadow even when the fill itself is clear.
        sh.fillStyle = Color.Transparent
        sh.shadowColor = "rgba(40,20,20,.55)"
        sh.shadowBlur = 26f
        sh.shadowOffsetY = 14f
        sh.beginPath()
        sh.roundRect(12f, 12f, w - 24f, h - 24f, max(0f, cr - 12f))
        sh.fill()
        sh.shadowColor = "rgba(0,0,0,.12)"
        sh.shadowBlur = 5f
        sh.shadowOffsetY = 2f
        cardPath(sh, w, h, cr)
        sh.fill()
        shadow = sh.bitmap.also { it.freeze() }
        // The cold card: colour drained, cooled, a little lighter. Frost reveals it where it forms.
        val coldImage = ImageBitmap(pw, ph)
        Canvas(coldImage)
            .drawImage(c.bitmap, Offset.Zero, Paint().apply { colorFilter = ColdFilter })
        val k = Canvas2D(Canvas(coldImage), pw, ph)
        k.globalCompositeOperation = "source-atop"
        k.fillStyle = "rgba(150,188,222,.42)"
        k.fillRect(0f, 0f, pw.toFloat(), ph.toFloat())
        val gr = k.createLinearGradient(0f, 0f, pw.toFloat(), ph.toFloat())
        gr.addColorStop(0f, "rgba(235,245,255,.25)")
        gr.addColorStop(1f, "rgba(120,160,200,.15)")
        k.fillStyle = gr
        k.fillRect(0f, 0f, pw.toFloat(), ph.toFloat())
        coldImage.freeze()
        cold = coldImage
        frost = Canvas2D.create(pw, ph)
        frostDone = null
        drawnIdx = 0
        drawnF = -1f
        hazeF = -1f
        hazePixels = IntArray(g.gw * g.gh)
        if (glint == null) bakeSprites()
    }

    // A glint's glow, and a bead's lens (warm and on ice), each drawn once at 64 px and then
    // scaled, so the water makes no gradient per bead per frame. Their gradients' stops all
    // carry the bead's alpha, which drawing at that alpha does instead.
    private fun bakeSprites() {
        Canvas2D.create(64, 64).let { c ->
            val g = c.createRadialGradient(32f, 32f, 0f, 32f, 32f, 32f)
            g.addColorStop(0f, "rgba(255,255,255,1)")
            g.addColorStop(1f, "rgba(220,240,255,0)")
            c.fillStyle = g
            c.beginPath()
            c.arc(32f, 32f, 32f, 0f, 7f)
            c.fill()
            glint = c.bitmap.also { it.freeze() }
        }
        // The bead: radius 1 drawn at 32 px, so the gradient's r is 32.
        fun lens(icy: Boolean): ImageBitmap {
            val c = Canvas2D.create(64, 64)
            c.translate(32f, 32f)
            val r = 32f
            val g = c.createRadialGradient(-r * .2f, -r * .3f, r * .1f, 0f, 0f, r)
            if (icy) {
                g.addColorStop(0f, "rgba(245,250,255,.55)")
                g.addColorStop(1f, "rgba(200,222,240,.7)")
            } else {
                g.addColorStop(0f, "rgba(255,240,230,.16)")
                g.addColorStop(.55f, "rgba(255,235,225,.05)")
                g.addColorStop(.82f, "rgba(40,10,25,.26)")
                g.addColorStop(1f, "rgba(20,5,15,.5)")
            }
            c.fillStyle = g
            c.beginPath()
            c.arc(0f, 0f, r, 0f, 7f)
            c.fill()
            c.fillStyle = "rgba(255,255,255,.38)"
            c.beginPath()
            c.ellipse(r * .18f, r * .48f, r * .42f, r * .2f, 0f, 0f, 7f)
            c.fill()
            c.fillStyle = "rgba(255,255,255,.9)"
            c.beginPath()
            c.arc(-r * .36f, -r * .38f, max(.45f, r * .24f), 0f, 7f)
            c.fill()
            return c.bitmap.also { it.freeze() }
        }
        bead = lens(false)
        icyBead = lens(true)
    }

    // ---------- The card ----------

    private fun cardPath(c: Canvas2D, w: Float, h: Float, r: Float) {
        c.beginPath()
        c.roundRect(0f, 0f, w, h, r)
    }

    private fun drawCard(c: Canvas2D, w: Float, h: Float) {
        val seeded = ParkMiller(19)
        val r = { seeded.next().toFloat() }
        c.save()
        cardPath(c, w, h, cr)
        c.clip()
        // Warm anodised metal: plum into copper, lit from the top left.
        var g = c.createLinearGradient(0f, 0f, w, h)
        g.addColorStop(0f, "#3a1934")
        g.addColorStop(.38f, "#7b2b3a")
        g.addColorStop(.72f, "#bd5a35")
        g.addColorStop(1f, "#e09a52")
        c.fillStyle = g
        c.fillRect(0f, 0f, w, h)
        // Brushed grain.
        repeat(520) {
            val y = r() * h
            c.fillStyle =
                if (r() < .5f) Color(255, 230, 210).copy(alpha = .02f + r() * .035f)
                else Color(30, 0, 10).copy(alpha = .03f + r() * .04f)
            c.fillRect(0f, y, w, .5f + r() * .7f)
        }
        // Guilloché waves, faint.
        c.lineWidth = .6f
        for (k in 0 until 26) {
            c.strokeStyle = Color(255, 214, 180).copy(alpha = .05f + if (k % 3 == 0) .03f else 0f)
            c.beginPath()
            var x = -4f
            while (x <= w + 4) {
                val y =
                    h * .82f - k * h * .028f +
                        sin(x / w * 7 + k * .45f) * h * .07f +
                        sin(x / w * 2.3f - k * .2f) * h * .05f
                if (x < 0) c.moveTo(x, y) else c.lineTo(x, y)
                x += 4f
            }
            c.stroke()
        }
        // A broad sheen across the top left, and a darker fall-off bottom right.
        g = c.createLinearGradient(0f, 0f, w * .9f, h)
        g.addColorStop(0f, "rgba(255,240,225,.22)")
        g.addColorStop(.35f, "rgba(255,240,225,.04)")
        g.addColorStop(.36f, "rgba(255,240,225,.09)")
        g.addColorStop(.55f, "rgba(255,240,225,0)")
        g.addColorStop(1f, "rgba(20,0,10,.18)")
        c.fillStyle = g
        c.fillRect(0f, 0f, w, h)
        // Bank wordmark and card type.
        val u = w / 100f
        c.fillStyle = "rgba(255,246,236,.94)"
        c.font = "700 ${6.4f * u}px $FrostSerif"
        c.textBaseline = "alphabetic"
        c.fillText("Juniper", 6.5f * u, 12.5f * u)
        c.fillStyle = "rgba(255,236,220,.6)"
        c.font = "600 ${2.9f * u}px sans"
        c.textAlign = "right"
        c.fillText("DEBIT", w - 6.5f * u, 11.5f * u)
        c.textAlign = "left"
        // A sprig of juniper next to the name.
        c.strokeStyle = "rgba(255,240,228,.7)"
        c.lineWidth = .7f * u / 2.5f
        c.lineCap = "round"
        c.beginPath()
        c.moveTo(31 * u, 12 * u)
        c.lineTo(36 * u, 7 * u)
        c.stroke()
        for (i in 0 until 4) {
            val t = .2f + i * .22f
            val x = lerp(31f, 36f, t) * u
            val y = lerp(12f, 7f, t) * u
            c.beginPath()
            c.moveTo(x, y)
            c.lineTo(x + 1.6f * u, y + .5f * u)
            c.moveTo(x, y)
            c.lineTo(x - .4f * u, y - 1.6f * u)
            c.stroke()
        }
        c.fillStyle = "#9fb7d6"
        c.beginPath()
        c.arc(33.6f * u, 10.4f * u, .8f * u, 0f, 7f)
        c.fill()
        // EMV chip.
        val chx = 8 * u
        val chy = 22 * u
        val chw = 12.5f * u
        val chh = 9.6f * u
        g = c.createLinearGradient(chx, chy, chx + chw, chy + chh)
        g.addColorStop(0f, "#f6dc96")
        g.addColorStop(.45f, "#c9a44e")
        g.addColorStop(.55f, "#e8cd84")
        g.addColorStop(1f, "#9e7a2f")
        c.fillStyle = g
        c.beginPath()
        c.roundRect(chx, chy, chw, chh, 1.6f * u)
        c.fill()
        c.strokeStyle = "rgba(90,64,20,.55)"
        c.lineWidth = .35f * u
        c.beginPath()
        c.moveTo(chx, chy + chh * .36f)
        c.lineTo(chx + chw * .34f, chy + chh * .36f)
        c.moveTo(chx, chy + chh * .66f)
        c.lineTo(chx + chw * .34f, chy + chh * .66f)
        c.moveTo(chx + chw, chy + chh * .36f)
        c.lineTo(chx + chw * .66f, chy + chh * .36f)
        c.moveTo(chx + chw, chy + chh * .66f)
        c.lineTo(chx + chw * .66f, chy + chh * .66f)
        c.moveTo(chx + chw * .5f, chy)
        c.lineTo(chx + chw * .5f, chy + chh * .26f)
        c.moveTo(chx + chw * .5f, chy + chh)
        c.lineTo(chx + chw * .5f, chy + chh * .76f)
        c.stroke()
        c.beginPath()
        c.roundRect(chx + chw * .34f, chy + chh * .26f, chw * .32f, chh * .5f, .8f * u)
        c.stroke()
        // Contactless waves.
        c.strokeStyle = "rgba(255,240,228,.75)"
        c.lineWidth = .55f * u
        for (i in 0 until 3) {
            c.beginPath()
            c.arc(24 * u, chy + chh / 2, (2 + i * 1.6f) * u, -.75f, .75f)
            c.stroke()
        }
        // Embossed numbers: a dark under-edge, a light top-edge, a silver face.
        emboss(c, "4921 7730 1846 3087", 7.5f * u, 45 * u, 6.1f * u, .25f * u, u)
        c.fillStyle = "rgba(255,236,220,.6)"
        c.font = "600 ${2.1f * u}px sans"
        c.fillText("VALID", 40 * u, 51.8f * u)
        c.fillText("THRU", 40 * u, 54.4f * u)
        emboss(c, "09/29", 47 * u, 54.4f * u, 3.9f * u, 0f, u)
        emboss(c, "MAYA R. ALVAREZ", 7.5f * u, 58.2f * u, 3.9f * u, .15f * u, u)
        // Network mark, invented: two overlapping rings.
        c.lineWidth = .9f * u
        c.strokeStyle = "rgba(255,244,232,.85)"
        c.beginPath()
        c.arc(w - 15 * u, 55 * u, 4 * u, 0f, 7f)
        c.stroke()
        c.beginPath()
        c.arc(w - 10 * u, 55 * u, 4 * u, 0f, 7f)
        c.stroke()
        c.restore()
        // A thin bright bevel.
        c.save()
        cardPath(c, w, h, cr)
        c.lineWidth = 1f
        c.strokeStyle = "rgba(255,225,200,.35)"
        c.stroke()
        c.restore()
    }

    // The prototype's emb(): each character set by hand with its tracking, three times. The face's
    // gradient fills text, which the canvas port can't do directly: the letters go in a layer in
    // white and the gradient is laid over them source-atop.
    private fun emboss(
        c: Canvas2D,
        txt: String,
        x: Float,
        y: Float,
        size: Float,
        track: Float,
        u: Float,
    ) {
        c.font = "600 ${size}px $FrostMono"
        fun put(dx: Float, dy: Float, fill: Any) {
            c.fillStyle = fill
            var xx = x
            for (ch in txt) {
                val s = ch.toString()
                c.fillText(s, xx + dx, y + dy)
                xx += c.measureText(s) + track
            }
        }
        put(.5f * u / 2.2f, .7f * u / 2.2f, "rgba(25,5,10,.55)")
        put(-.35f * u / 2.2f, -.4f * u / 2.2f, "rgba(255,240,230,.45)")
        c.beginLayer()
        put(0f, 0f, "#ffffff")
        val gg = c.createLinearGradient(0f, y - size, 0f, y)
        gg.addColorStop(0f, "#fbf3ec")
        gg.addColorStop(.6f, "#dccbc0")
        gg.addColorStop(1f, "#b9a39a")
        c.globalCompositeOperation = "source-atop"
        c.fillStyle = gg
        c.fillRect(0f, y - size * 1.4f, cw, size * 1.8f)
        c.endLayer()
    }

    // ---------- The frost, drawn as it arrives ----------

    private fun updateFrostLayer(f: Float) {
        val c = frost ?: return
        val g = growth ?: return
        if (f < drawnF || drawnF < 0f) {
            c.setTransform(1f, 0f, 0f, 1f, 0f, 0f)
            c.clearRect(0f, 0f, c.width.toFloat(), c.height.toFloat())
            drawnIdx = 0
            frostDone = null
        }
        drawnF = f
        val i = drawnIdx
        var j = i
        while (j < g.count && g.born[j] <= f) j++
        if (j == i) return
        drawnIdx = j
        c.setTransform(density, 0f, 0f, density, 0f, 0f)
        c.lineCap = "round"
        for (pass in 0 until 2) for (gen in 0 until 4) {
            c.beginPath()
            var any = false
            val ox = if (pass == 1) 0f else .45f
            val oy = if (pass == 1) 0f else .65f
            for (k in i until j) {
                if (g.gen[k] != gen) continue
                any = true
                c.moveTo(g.x1[k] + ox, g.y1[k] + oy)
                c.lineTo(g.x2[k] + ox, g.y2[k] + oy)
            }
            if (!any) continue
            c.lineWidth = GenWidth[gen] + if (pass == 1) 0f else .5f
            c.strokeStyle = if (pass == 1) GenLight[gen] else GenShade[gen]
            c.stroke()
        }
        // The spines catch the light: a brighter core on the main ferns.
        c.beginPath()
        var any = false
        for (k in i until j) {
            if (g.gen[k] != 0) continue
            any = true
            c.moveTo(g.x1[k] - .2f, g.y1[k] - .2f)
            c.lineTo(g.x2[k] - .2f, g.y2[k] - .2f)
        }
        if (any) {
            c.lineWidth = .55f
            c.strokeStyle = SpineCore
            c.stroke()
        }
        if (drawnIdx == g.count) {
            val done = ImageBitmap(c.width, c.height)
            Canvas(done).drawImage(c.bitmap, Offset.Zero, imagePaint.apply { alpha = 1f })
            done.freeze()
            frostDone = done
        }
    }

    // The haze: where the front has been the card goes pale, a pixel per 4 dp cell. Remade only
    // while the frost is growing.
    private fun updateHaze(f: Float) {
        val g = growth ?: return
        if (hazeF == f && haze != null) return
        hazeF = f
        val px = hazePixels
        for (i in px.indices) {
            val a = smooth(g.arr[i], g.arr[i] + .11f, f) * g.dens[i]
            px[i] = ((a * 255).roundToInt().coerceIn(0, 255) shl 24) or HazeRgb
        }
        haze = imageBitmapOf(px, g.gw, g.gh)
    }

    // ---------- Drawing ----------

    /**
     * Draws the card at ([x], [y]) on [c] (in dp) with the frost, hole and water [s] holds, and the
     * card's shadow under it.
     */
    fun draw(c: Canvas2D, s: FrostCardState, x: Float, y: Float) {
        val g = s.growth ?: return
        if (g !== growth || card == null) build(s.cw, s.ch, g)
        val card = card ?: return
        c.save()
        c.translate(x, y)
        shadow?.let {
            val m = ShadowMargin
            c.drawImage(it, -m, -m, cw + 2 * m, ch + 2 * m)
        }
        c.drawImage(card, 0f, 0f, cw, ch)
        if (s.f > 0f) drawIce(c, s, g)
        drawWater(c, s)
        c.restore()
    }

    private fun drawIce(c: Canvas2D, s: FrostCardState, g: FrostGrowth) {
        updateFrostLayer(s.f)
        updateHaze(s.f)
        val cold = cold ?: return
        val haze = haze ?: return
        val frostImage = frostDone ?: frost?.bitmap ?: return
        c.beginLayer(0f, 0f, cw, ch)
        c.drawImage(cold, 0f, 0f, cw, ch)
        c.globalCompositeOperation = "destination-in"
        c.drawImage(haze, 0f, 0f, cw, ch)
        c.globalCompositeOperation = "source-over"
        c.globalAlpha = .5f
        c.drawImage(haze, 0f, 0f, cw, ch)
        c.globalAlpha = 1f
        c.drawImage(frostImage, 0f, 0f, cw, ch)
        // A thin rim of ice along the edge.
        val rim = smooth(0f, .25f, s.f)
        c.save()
        cardPath(c, cw, ch, cr)
        c.clip()
        c.lineWidth = 3.2f
        c.strokeStyle = Color(238, 247, 255).copy(alpha = .55f * rim)
        cardPath(c, cw, ch, cr)
        c.stroke()
        c.lineWidth = 1f
        c.strokeStyle = Color.White.copy(alpha = .8f * rim)
        c.beginPath()
        c.roundRect(1.6f, 1.6f, cw - 3.2f, ch - 3.2f, cr - 1.6f)
        c.stroke()
        c.restore()
        if (s.r > 0f) {
            c.globalCompositeOperation = "destination-out"
            val gr = c.createRadialGradient(s.cx, s.cy, max(0f, s.r - 10), s.cx, s.cy, s.r + 7)
            gr.addColorStop(0f, Color.Black)
            gr.addColorStop(1f, Color.Transparent)
            c.fillStyle = gr
            holePath(c, s, s.r, 7f)
            c.fill()
            c.globalCompositeOperation = "source-over"
        }
        c.globalCompositeOperation = "destination-in"
        c.fillStyle = Color.Black
        cardPath(c, cw, ch, cr)
        c.fill()
        c.globalCompositeOperation = "source-over"
        c.endLayer()

        c.save()
        cardPath(c, cw, ch, cr)
        c.clip()
        // The meniscus where the ice stops: a bright wet lip and a faint shadow outside it.
        if (s.r > 2f) {
            c.lineWidth = 2.2f
            c.strokeStyle = WetLip
            holePath(c, s, s.r, -4f)
            c.stroke()
            c.lineWidth = 1f
            c.strokeStyle = WetShade
            holePath(c, s, s.r, -1.5f)
            c.stroke()
            // Wet metal shines a little.
            val gr =
                c.createRadialGradient(
                    s.cx - s.r * .3f,
                    s.cy - s.r * .35f,
                    0f,
                    s.cx,
                    s.cy,
                    s.r,
                )
            gr.addColorStop(0f, WetShine)
            gr.addColorStop(1f, Color.Transparent)
            c.fillStyle = gr
            holePath(c, s, s.r, -5f)
            c.fill()
        }
        // Glints: a few needle tips catch the light now and then.
        val t = s.time
        val sprite = glint
        c.drawUnder { canvas ->
            for (i in 0 until g.sparks) {
                if (g.sparkBorn[i] > s.f) continue
                val gx = g.sparkX[i]
                val gy = g.sparkY[i]
                if (
                    s.r > 0f &&
                        hypot(gx - s.cx, gy - s.cy) < s.r * s.holeR(atan2(gy - s.cy, gx - s.cx)) + 4
                )
                    continue
                val a =
                    max(0f, sin(t * g.sparkSpeed[i] + g.sparkPhase[i])).pow(14) *
                        smooth(0f, .08f, s.f - g.sparkBorn[i])
                if (a < .03f) continue
                val l = 2.8f * g.sparkSize[i]
                strokePaint.color = Color.White.copy(alpha = a * .85f)
                strokePaint.strokeWidth = .55f
                strokePaint.blendMode = BlendMode.SrcOver
                canvas.drawLine(Offset(gx - l, gy), Offset(gx + l, gy), strokePaint)
                canvas.drawLine(Offset(gx, gy - l), Offset(gx, gy + l), strokePaint)
                if (sprite != null) {
                    val rr = 2.2f * g.sparkSize[i]
                    imagePaint.alpha = a
                    drawSprite(canvas, sprite, gx, gy, rr, rr)
                }
            }
        }
        c.restore()
    }

    // Draws [sprite] (64 px, centred) over a box of half-width [rx] and half-height [ry] at (x, y).
    private fun drawSprite(
        canvas: Canvas,
        sprite: ImageBitmap,
        x: Float,
        y: Float,
        rx: Float,
        ry: Float,
    ) {
        canvas.save()
        canvas.translate(x - rx, y - ry)
        canvas.scale(rx * 2 / 64f, ry * 2 / 64f)
        canvas.drawImage(sprite, Offset.Zero, imagePaint)
        canvas.restore()
    }

    private fun holePath(c: Canvas2D, s: FrostCardState, r: Float, grow: Float) {
        c.beginPath()
        for (i in 0..72) {
            val a = i / 72f * TwoPi
            val rr = r * s.holeR(a) + grow
            val x = s.cx + cos(a) * rr
            val y = s.cy + sin(a) * rr
            if (i == 0) c.moveTo(x, y) else c.lineTo(x, y)
        }
        c.closePath()
    }

    private fun drawWater(c: Canvas2D, s: FrostCardState) {
        val bead = bead ?: return
        val icy = icyBead ?: return
        if (s.drops == 0 && s.streaks == 0) return
        c.drawUnder { canvas ->
            // Wet streaks left behind by running drops: wet metal reads a touch darker and
            // richer, with a thin sheen on one side.
            strokePaint.blendMode = BlendMode.SrcOver
            for (i in 0 until s.streaks) {
                val a = s.slife[i].coerceIn(0f, 1f)
                strokePaint.color = StreakDark.copy(alpha = .08f * a)
                strokePaint.strokeWidth = s.sw[i]
                canvas.drawLine(Offset(s.sx1[i], s.sy1[i]), Offset(s.sx2[i], s.sy2[i]), strokePaint)
                strokePaint.color = StreakSheen.copy(alpha = .07f * a)
                strokePaint.strokeWidth = s.sw[i] * .3f
                val o = s.sw[i] * .3f
                canvas.drawLine(
                    Offset(s.sx1[i] - o, s.sy1[i]),
                    Offset(s.sx2[i] - o, s.sy2[i]),
                    strokePaint,
                )
            }
            for (i in 0 until s.drops) {
                val a = s.da[i]
                val r = s.dr[i]
                val fall = s.dfall[i]
                val fr = !fall && s.iced(s.dx[i], s.dy[i])
                val sy =
                    when {
                        fall -> 1.25f
                        s.dhang[i] -> 1 + min(.5f, s.dht[i] * 1.4f)
                        else -> 1 + (s.dvy[i] / 160f).coerceIn(0f, .45f)
                    }
                val sx = 1 / sqrt(sy)
                // Soft shadow on the card, then a lens: clear middle, dark lower rim, a bright
                // caustic, a specular dot.
                if (!fall) {
                    fillPaint.color = BeadShadow.copy(alpha = .16f * a)
                    canvas.drawOval(
                        s.dx[i] + (.5f - r * 1.02f) * sx,
                        s.dy[i] + (r * .35f - r * 1.02f) * sy,
                        s.dx[i] + (.5f + r * 1.02f) * sx,
                        s.dy[i] + (r * .35f + r * 1.02f) * sy,
                        fillPaint,
                    )
                }
                imagePaint.alpha = a
                drawSprite(canvas, if (fr) icy else bead, s.dx[i], s.dy[i], r * sx, r * sy)
            }
        }
    }

    private companion object {
        const val TwoPi = (2 * PI).toFloat()

        // Room round the card for its baked shadow, in dp.
        const val ShadowMargin = 40f
        const val HazeRgb = (236 shl 16) or (244 shl 8) or 252

        // The four generations' strokes: width, light alpha, shade alpha.
        val GenWidth = floatArrayOf(1.5f, 1.0f, .7f, .55f)
        val GenLight =
            arrayOf(
                Color(250, 253, 255).copy(alpha = .74f),
                Color(250, 253, 255).copy(alpha = .62f),
                Color(250, 253, 255).copy(alpha = .5f),
                Color(250, 253, 255).copy(alpha = .42f),
            )
        val GenShade =
            arrayOf(
                Color(28, 58, 96).copy(alpha = .22f),
                Color(28, 58, 96).copy(alpha = .18f),
                Color(28, 58, 96).copy(alpha = .12f),
                Color(28, 58, 96).copy(alpha = .08f),
            )
        val SpineCore = Color.White.copy(alpha = .9f)
        val WetLip = Color.White.copy(alpha = .3f)
        val WetShade = Color(20, 50, 90).copy(alpha = .2f)
        val WetShine = Color.White.copy(alpha = .1f)
        val StreakDark = Color(40, 0, 12)
        val StreakSheen = Color(255, 235, 220)
        val BeadShadow = Color(20, 5, 10)

        // The CSS filter `saturate(.12) brightness(1.18) contrast(.85)`, as one colour matrix.
        val ColdFilter: ColorFilter = run {
            val s = .12f
            val b = 1.18f
            val k = .85f
            val rows =
                arrayOf(
                    floatArrayOf(.213f + .787f * s, .715f - .715f * s, .072f - .072f * s),
                    floatArrayOf(.213f - .213f * s, .715f + .285f * s, .072f - .072f * s),
                    floatArrayOf(.213f - .213f * s, .715f - .715f * s, .072f + .928f * s),
                )
            val off = (.5f - .5f * k) * 255f
            val m = FloatArray(20)
            for (i in 0 until 3) {
                for (j in 0 until 3) m[i * 5 + j] = rows[i][j] * b * k
                m[i * 5 + 4] = off
            }
            m[18] = 1f
            ColorFilter.colorMatrix(ColorMatrix(m))
        }

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    }
}
