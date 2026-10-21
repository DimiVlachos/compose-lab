package dev.dimvlachos.lab.core.presentation.components.paperbag

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Pattern2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// The prototype's drawing, ported call by call: the same calls in the same order, with the same
// numbers and the same colour strings. Where a number changed for a fix, the comment says so.

/** A thing standing in the bag: its [art], the [slot] across the mouth, how far it has [rise]n. */
internal class BagItem(val art: BagArt, val slot: Int, val rise: Float = 0f)

/**
 * How the bag stands this frame: how much it [bulge]s (0 to 1) and is [tip]ped toward you (0 to 1,
 * eased), its landing squash [sq] and its [wob]ble, what is in it, and the grease-pencil [count]
 * being written over [prevCount], [numP] of the way.
 */
internal class BagPose(
    val bulge: Float = 0f,
    val tip: Float = 0f,
    val sq: Float = 0f,
    val wob: Float = 0f,
    val items: List<BagItem> = emptyList(),
    val count: Int = items.size,
    val prevCount: Int = 0,
    val numP: Float = 1f,
)

/**
 * The type the bag is written on with: [hand] for the grease-pencil number, measured by [measurer].
 * The numbers are baked once each, at a size, and kept while this ink lives.
 */
internal class BagInk(val measurer: TextMeasurer, val hand: FontFamily) {
    internal val numbers = HashMap<String, Canvas2D>()
}

// ---------- Paper texture ----------

// The prototype's Park–Miller generator, seeded as it seeds it.
private fun parkMiller(seed: Int): () -> Double {
    var s = seed.toLong()
    return {
        s = (s * 16807L) % 2147483647L
        s / 2147483647.0
    }
}

/** The kraft paper's fibres and flecks, 96 px square, tiled across the bag's front. */
internal val bagFibres: ImageBitmap by lazy {
    val g = Canvas2D.create(96, 96)
    val rnd = parkMiller(7)
    for (i in 0 until 260) {
        val x = rnd() * 96
        val y = rnd() * 96
        val l = 2 + rnd() * 9
        val a = (rnd() - .5) * .9
        g.strokeStyle =
            if (rnd() < .5) rgba(255, 240, 215, .08 + rnd() * .12)
            else rgba(70, 40, 15, .06 + rnd() * .1)
        g.lineWidth = (.5 + rnd() * .7).toFloat()
        g.beginPath()
        g.moveTo(x.toFloat(), y.toFloat())
        g.lineTo((x + cos(a) * l).toFloat(), (y + sin(a) * l).toFloat())
        g.stroke()
    }
    for (i in 0 until 90) {
        g.fillStyle = rgba(60, 35, 12, .06 + rnd() * .1)
        g.fillRect((rnd() * 96).toFloat(), (rnd() * 96).toFloat(), 1f, 1f)
    }
    g.bitmap
}

private val fibrePattern: Pattern2D by lazy { Pattern2D(bagFibres) }

// ---------- Grease pencil digits ----------

/**
 * [text] in grease pencil at [px] pixels: written four times over, slightly off, with the wax
 * skipping over the paper's tooth.
 */
internal fun pencilNumber(ink: BagInk, text: String, px: Float): Canvas2D {
    val key = text + "@" + px.roundToInt()
    ink.numbers[key]?.let {
        return it
    }
    val w = ceil(px * (.62f * text.length + .5f)).toInt()
    val h = ceil(px * 1.25f).toInt()
    val g = Canvas2D.create(w, h)
    g.textMeasurer = ink.measurer
    g.fonts = mapOf("HAND" to ink.hand)
    g.font = "700 ${px}px HAND"
    g.textAlign = "center"
    g.textBaseline = "middle"
    g.fillStyle = "#231a14"
    val rnd = parkMiller(text[0].code * 31 + text.length)
    for (k in 0 until 4) {
        g.globalAlpha = .55f
        g.fillText(
            text,
            w / 2f + ((rnd() - .5) * px * .05).toFloat(),
            h / 2f + ((rnd() - .5) * px * .05).toFloat(),
        )
    }
    g.globalAlpha = 1f
    g.globalCompositeOperation = "destination-out"
    // Wax skips over the paper's tooth.
    for (i in 0 until w * h / 55) {
        g.fillStyle = rgba(0, 0, 0, .2 + rnd() * .5)
        g.fillRect(
            (rnd() * w).toFloat(),
            (rnd() * h).toFloat(),
            (.5 + rnd() * px * .018).toFloat(),
            (.5 + rnd() * px * .012).toFloat(),
        )
    }
    g.globalCompositeOperation = "source-over"
    ink.numbers[key] = g
    return g
}

// ---------- Items (drawn standing, bottom-centre at 0,0, height 1 = one bag height) ----------

/**
 * Draws [art] standing at the origin, [s] pixels to a bag height, any lettering in [ink]'s
 * measurer.
 */
internal fun drawItem(g: Canvas2D, art: BagArt, s: Float, ink: BagInk) {
    g.textMeasurer = ink.measurer
    g.save()
    g.scale(s, s)
    g.lineCap = "round"
    when (art) {
        BagArt.Baguette -> {
            val l = 1.24f
            val w = .16f
            val gr = g.createLinearGradient(-w, 0f, w, 0f)
            gr.addColorStop(0f, "#9c5a22")
            gr.addColorStop(.45f, "#d99a52")
            gr.addColorStop(1f, "#8a4a18")
            g.fillStyle = gr
            g.beginPath()
            g.moveTo(-w * .8f, 0f)
            g.bezierCurveTo(-w * 1.05f, -l * .5f, -w * .9f, -l + .05f, 0f, -l)
            g.bezierCurveTo(w * .9f, -l + .05f, w * 1.05f, -l * .5f, w * .8f, 0f)
            g.closePath()
            g.fill()
            g.strokeStyle = "rgba(250,215,150,.85)"
            g.lineWidth = .028f
            for (i in 0 until 4) {
                val y = -l * (.42f + i * .14f)
                g.beginPath()
                g.moveTo(-w * .45f, y + .05f)
                g.quadraticCurveTo(0f, y - .02f, w * .5f, y - .04f)
                g.stroke()
            }
            g.fillStyle = "rgba(255,240,210,.18)"
            g.beginPath()
            g.ellipse(-w * .35f, -l * .62f, w * .18f, l * .3f, 0f, 0f, TAU)
            g.fill()
        }
        BagArt.Celery -> {
            for (i in -2..2) {
                val x = i * .055f
                val tx = i * .11f
                val gr = g.createLinearGradient(x - .04f, 0f, x + .04f, 0f)
                gr.addColorStop(0f, "#9ec27a")
                gr.addColorStop(.5f, "#d6ebb4")
                gr.addColorStop(1f, "#8fb76a")
                g.strokeStyle = gr
                g.lineWidth = .075f
                g.beginPath()
                g.moveTo(x, 0f)
                g.quadraticCurveTo(x, -.6f, tx, -.88f)
                g.stroke()
            }
            fun leaf(x: Float, y: Float, r: Float, c: String) {
                g.fillStyle = c
                g.beginPath()
                for (k in 0 until 7) {
                    val a = k / 7f * TAU
                    g.ellipse(
                        x + cos(a) * r * .55f,
                        y + sin(a) * r * .45f,
                        r * .42f,
                        r * .3f,
                        a,
                        0f,
                        TAU,
                    )
                }
                g.fill()
            }
            leaf(-.17f, -.92f, .14f, "#4f8a35")
            leaf(.16f, -.94f, .14f, "#4a8432")
            leaf(0f, -1.02f, .16f, "#5e9a3e")
            leaf(-.07f, -.97f, .1f, "#7cb352")
            leaf(.09f, -1.0f, .1f, "#79b04f")
        }
        BagArt.Sunflowers -> {
            g.strokeStyle = "#4c7a32"
            g.lineWidth = .04f
            val heads =
                listOf(
                    floatArrayOf(-.16f, -1.0f, .12f),
                    floatArrayOf(.15f, -1.06f, .13f),
                    floatArrayOf(0f, -1.15f, .14f),
                    floatArrayOf(-.05f, -.9f, .1f),
                    floatArrayOf(.2f, -.86f, .1f),
                )
            for ((x, y) in heads) {
                g.beginPath()
                g.moveTo(x * .3f, 0f)
                g.quadraticCurveTo(x * .4f, y * .5f, x, y)
                g.stroke()
            }
            for ((x, y, r) in heads) {
                g.fillStyle = "#f2b821"
                for (k in 0 until 12) {
                    val a = k / 12f * TAU
                    g.beginPath()
                    g.ellipse(
                        x + cos(a) * r * .7f,
                        y + sin(a) * r * .7f,
                        r * .38f,
                        r * .14f,
                        a,
                        0f,
                        TAU,
                    )
                    g.fill()
                }
                g.fillStyle = "#e09412"
                for (k in 0 until 12) {
                    val a = (k + .5f) / 12f * TAU
                    g.beginPath()
                    g.ellipse(
                        x + cos(a) * r * .55f,
                        y + sin(a) * r * .55f,
                        r * .28f,
                        r * .1f,
                        a,
                        0f,
                        TAU,
                    )
                    g.fill()
                }
                val cg = g.createRadialGradient(x - r * .1f, y - r * .1f, 0f, x, y, r * .45f)
                cg.addColorStop(0f, "#7a4a1c")
                cg.addColorStop(1f, "#3a220c")
                g.fillStyle = cg
                g.beginPath()
                g.arc(x, y, r * .42f, 0f, TAU)
                g.fill()
            }
        }
        BagArt.Milk -> {
            val w = .16f
            val h = .7f
            g.fillStyle = "#f4f1ea"
            g.fillRect(-w, -h + .12f, w * 2, h - .12f)
            g.fillStyle = "#e2ddd2"
            g.beginPath()
            g.moveTo(-w, -h + .12f)
            g.lineTo(0f, -h)
            g.lineTo(w, -h + .12f)
            g.closePath()
            g.fill()
            g.fillStyle = "#d6d0c3"
            g.fillRect(-.03f, -h - .04f, .06f, .05f)
            g.fillStyle = "#2f62a8"
            g.fillRect(-w, -h * .62f, w * 2, .16f)
            g.fillStyle = "rgba(255,255,255,.9)"
            // The prototype sets its label in a .08 px font under the item's scale; a font that
            // small doesn't lay out here, so the label is set a hundred times larger and scaled
            // back down.
            g.save()
            g.scale(.01f, .01f)
            g.font = "700 8px sans-serif"
            g.textAlign = "center"
            g.fillText("MILK", 0f, (-h * .62f + .11f) * 100f)
            g.restore()
        }
        BagArt.Apples -> {
            fun ap(x: Float, y: Float, r: Float) {
                val gr = g.createRadialGradient(x - r * .35f, y - r * .4f, r * .1f, x, y, r)
                gr.addColorStop(0f, "#f2775a")
                gr.addColorStop(.6f, "#c8321f")
                gr.addColorStop(1f, "#8a1a12")
                g.fillStyle = gr
                g.beginPath()
                g.arc(x, y, r, 0f, TAU)
                g.fill()
                g.strokeStyle = "#5a3a1c"
                g.lineWidth = .02f
                g.beginPath()
                g.moveTo(x, y - r * .8f)
                g.lineTo(x + .02f, y - r * 1.15f)
                g.stroke()
                g.fillStyle = "#5f9a3a"
                g.beginPath()
                g.ellipse(x + .06f, y - r * 1.05f, .05f, .022f, -.5f, 0f, TAU)
                g.fill()
            }
            ap(-.12f, -.14f, .14f)
            ap(.13f, -.15f, .14f)
            ap(0f, -.3f, .14f)
        }
        BagArt.Eggs -> {
            g.fillStyle = "#c9b89a"
            g.beginPath()
            g.roundRect(-.3f, -.2f, .6f, .2f, .04f)
            g.fill()
            for (i in 0 until 4) {
                val x = -.22f + i * .147f
                val gr = g.createRadialGradient(x - .03f, -.27f, .01f, x, -.24f, .08f)
                gr.addColorStop(0f, "#f0d2a8")
                gr.addColorStop(1f, "#b77c45")
                g.fillStyle = gr
                g.beginPath()
                g.ellipse(x, -.24f, .062f, .08f, 0f, 0f, TAU)
                g.fill()
            }
            g.fillStyle = "#d8c8aa"
            g.beginPath()
            g.roundRect(-.3f, -.2f, .6f, .07f, .02f)
            g.fill()
        }
    }
    g.restore()
}

/**
 * Draws [art] as its product tile shows it, filling a [size] square from its top left: stood up in
 * the middle, the long ones leaning.
 */
internal fun drawThumb(g: Canvas2D, art: BagArt, size: Float, ink: BagInk) {
    val k = size / 56f
    val sc = 44f / max(.5f, art.height + .1f) * k
    g.save()
    g.translate(28f * k, 28f * k + art.height * sc / 2f)
    when (art) {
        BagArt.Baguette -> g.rotate(.5f)
        BagArt.Sunflowers,
        BagArt.Celery -> g.rotate(.25f)
        else -> Unit
    }
    drawItem(g, art, sc, ink)
    g.restore()
}

// Where each item stands in the bag: spread across the mouth, tall ones leaning out.
private val Slots =
    listOf(
        -.2f to -.1f,
        .18f to .12f,
        -.02f to .02f,
        .28f to .2f,
        -.3f to -.18f,
        .07f to .06f,
        -.12f to -.05f,
        .22f to .1f,
        -.25f to -.12f,
    )

// How much of a short item shows over the front rim, in bag heights. The prototype stood the
// apples, the milk and the eggs wholly below the rim, where the front hid them.
private const val ShortPeek = .16f

// ---------- The bag (bottom-centre at x,y; s = bag height in px) ----------

/**
 * Draws the bag in [pose], its bottom centre at ([x], [y]) and [s] units tall, in the [dark] theme
 * or the light one. [pixels] is how many pixels a unit is, so the number is baked sharp.
 */
internal fun drawBag(
    g: Canvas2D,
    x: Float,
    y: Float,
    s: Float,
    pose: BagPose,
    ink: BagInk,
    dark: Boolean,
    pixels: Float,
) {
    val bulge = pose.bulge
    val tip = pose.tip
    val sqY = 1 - pose.sq * .14f
    val sqX = 1 + pose.sq * .1f
    g.save()
    g.translate(x, y)
    g.rotate(pose.wob * .09f)
    g.scale(sqX, sqY)
    val w = s * .74f * (1 + .1f * bulge)
    val h = s * (1 - .1f * tip)
    val side = w * .17f
    val bow = s * (.012f + .05f * bulge)
    val depth = s * (.06f + .34f * tip) // how far up the back rim sits: more when tipped toward you
    val l = -w / 2
    val r = w / 2
    val t = -h

    // Shadow on the counter
    g.save()
    g.scale(1f, .22f)
    val sg = g.createRadialGradient(side * .3f, 0f, 0f, side * .3f, 0f, w * .8f)
    sg.addColorStop(0f, rgba(30, 18, 6, if (dark) .5 else .32))
    sg.addColorStop(1f, "rgba(30,18,6,0)")
    g.fillStyle = sg
    g.beginPath()
    g.arc(side * .3f, 0f, w * .8f, 0f, TAU)
    g.fill()
    g.restore()

    // Inside: the back wall and floor of the opening, seen over the front rim.
    val backY = t - depth
    g.fillStyle = if (dark) "#3a2814" else "#5b3d1d"
    g.beginPath()
    g.moveTo(l, t)
    g.lineTo(l + side * .6f, backY)
    g.lineTo(r + side, backY)
    g.lineTo(r + side, t + side * .2f)
    g.lineTo(r, t)
    g.closePath()
    g.fill()
    val ig = g.createLinearGradient(0f, backY, 0f, t)
    ig.addColorStop(0f, "rgba(201,152,98,.9)")
    ig.addColorStop(1f, "rgba(70,44,18,.0)")
    g.fillStyle = ig
    g.beginPath()
    g.moveTo(l + side * .6f, backY)
    g.lineTo(r + side, backY)
    g.lineTo(r + side, backY + depth * .45f)
    g.lineTo(l + side * .6f, backY + depth * .45f)
    g.closePath()
    g.fill()
    // Back rim teeth
    g.fillStyle = if (dark) "#9c7445" else "#c49a66"
    serration(g, l + side * .6f, r + side, backY, s * .028f, -1f)

    // Contents, standing on the bag's floor; the front panel will hide their lower parts.
    val order = pose.items.sortedBy { it.art.height }
    val floor = -s * .04f - tip * s * .28f // tipping lifts the contents into view
    // Below the rim the contents stay inside the bag. The prototype let the stems of a bunch at
    // the right of the mouth show through the air beside the gusset.
    g.save()
    g.beginPath()
    g.moveTo(-s * 4, -s * 4)
    g.lineTo(s * 4, -s * 4)
    g.lineTo(s * 4, t)
    g.lineTo(r + side, t)
    g.lineTo(r + side, 0f)
    g.lineTo(l, 0f)
    g.lineTo(l, t)
    g.lineTo(-s * 4, t)
    g.closePath()
    g.clip()
    for (item in order) {
        val (sx, lean) = Slots[item.slot % Slots.size]
        val hgt = item.art.height
        val rise = item.rise
        val stand = floor - (if (hgt < .5f) s * (.8f + tip * .2f) * (1 - hgt) else 0f)
        // Short things stand high enough for their tops to show over the rim.
        val peek = -h - ShortPeek * s + hgt * s
        g.save()
        g.translate(sx * w + side * .35f, min(stand, peek) - rise * s * .9f)
        g.rotate(lean * (.6f + tip * .3f))
        // It fades once it has cleared the rim, not while it is still in the bag, as it did at
        // .6 to 1 in the prototype.
        if (rise > 0f) g.globalAlpha = 1 - ((rise - 1f) / .3f).coerceIn(0f, 1f)
        drawItem(g, item.art, s, ink)
        g.restore()
    }
    g.restore()

    // Side gusset (right), darker and with its centre pleat.
    val sideG = g.createLinearGradient(r, 0f, r + side, 0f)
    sideG.addColorStop(0f, if (dark) "#7a5530" else "#a8784a")
    sideG.addColorStop(1f, if (dark) "#5f4023" else "#8d6236")
    g.fillStyle = sideG
    g.beginPath()
    g.moveTo(r, t)
    g.lineTo(r + side, t + side * .2f - depth * .25f)
    g.lineTo(r + side, -side * .25f)
    g.lineTo(r, 0f)
    g.closePath()
    g.fill()
    g.strokeStyle = "rgba(50,28,10,.35)"
    g.lineWidth = max(.6f, s * .008f)
    g.beginPath()
    g.moveTo(r + side * .55f, t + side * .1f)
    g.lineTo(r + side * .5f, -s * .3f)
    g.lineTo(r + side * .55f, -side * .1f)
    g.stroke()

    // Front panel, bowed by its contents.
    fun frontPath() {
        g.beginPath()
        g.moveTo(l, t)
        g.lineTo(r, t)
        g.quadraticCurveTo(r + bow, t * .45f, r, 0f)
        g.lineTo(l, 0f)
        g.quadraticCurveTo(l - bow, t * .45f, l, t)
        g.closePath()
    }
    val fg = g.createLinearGradient(l, t, r, 0f)
    if (dark) {
        fg.addColorStop(0f, "#b48650")
        fg.addColorStop(1f, "#8a6136")
    } else {
        fg.addColorStop(0f, "#dcb07a")
        fg.addColorStop(.55f, "#c6955c")
        fg.addColorStop(1f, "#ad7c45")
    }
    frontPath()
    g.fillStyle = fg
    g.fill()
    g.save()
    frontPath()
    g.clip()
    g.globalAlpha = .9f
    g.fillStyle = fibrePattern
    g.fillRect(l - bow, t, w + bow * 2, -t)
    g.globalAlpha = 1f
    // Bulge shading: a soft highlight down the belly, darker edges.
    val bg = g.createLinearGradient(l, 0f, r, 0f)
    bg.addColorStop(0f, rgba(60, 32, 10, .12 + bulge * .12))
    bg.addColorStop(.38f, "rgba(255,235,200,.12)")
    bg.addColorStop(1f, rgba(60, 32, 10, .1 + bulge * .14))
    g.fillStyle = bg
    g.fillRect(l - bow, t, w + bow * 2, -t)
    // Fold crease near the bottom, and the shadow under the rim.
    g.strokeStyle = "rgba(80,46,16,.28)"
    g.lineWidth = max(.6f, s * .01f)
    g.beginPath()
    g.moveTo(l, -h * .14f)
    g.lineTo(r, -h * .15f)
    g.stroke()
    g.strokeStyle = "rgba(255,236,205,.3)"
    g.beginPath()
    g.moveTo(l, -h * .14f + s * .012f)
    g.lineTo(r, -h * .15f + s * .012f)
    g.stroke()
    val rg = g.createLinearGradient(0f, t, 0f, t + s * .12f)
    rg.addColorStop(0f, "rgba(60,32,10,.28)")
    rg.addColorStop(1f, "rgba(60,32,10,0)")
    g.fillStyle = rg
    g.fillRect(l - bow, t, w + bow * 2, s * .12f)
    // The count, in grease pencil.
    val n = pose.count
    if (n > 0 || pose.prevCount > 0) {
        val show = if (n > 0) n.toString() else pose.prevCount.toString()
        val px = s * (if (s < 50) .62f else .44f)
        val bake = max(10f, px * pixels)
        val img = pencilNumber(ink, show, bake)
        val dw = img.width / pixels
        val dh = img.height / pixels
        val cx = -w * .02f
        val cy = t + h * (if (s < 50) .56f else .5f)
        g.save()
        g.translate(cx, cy)
        g.rotate(-.08f)
        val inkAlpha = if (dark) .85f else .92f
        val p = pose.numP
        // The old number rubs off quickly while the new one is written in, left to right, over
        // the glyph.
        if (
            pose.prevCount > 0 && p < 1 && (if (n > 0) pose.prevCount.toString() != show else true)
        ) {
            val old = pencilNumber(ink, pose.prevCount.toString(), bake)
            g.globalAlpha = inkAlpha * (1 - p * 2.5f).coerceIn(0f, 1f)
            g.drawImage(old, -old.width / pixels / 2, -dh / 2, old.width / pixels, dh)
        }
        if (n > 0) {
            g.beginPath()
            g.rect(-dw / 2, -dh / 2, dw * (.2f + .65f * (p * 1.15f - .1f).coerceIn(0f, 1f)), dh)
            g.clip()
            g.globalAlpha = inkAlpha
            g.drawImage(img, -dw / 2, -dh / 2, dw, dh)
        }
        g.restore()
    }
    g.restore()
    // Front rim teeth
    g.fillStyle = if (dark) "#c09160" else "#e2b986"
    serration(g, l, r, t, s * .03f, 1f)
    g.restore()
}

private fun serration(g: Canvas2D, x0: Float, x1: Float, y: Float, tooth: Float, dir: Float) {
    val n = max(4, ((x1 - x0) / (tooth * 1.6f)).roundToInt())
    g.beginPath()
    g.moveTo(x0, y + tooth * .2f * dir)
    for (i in 0..n) {
        val x = x0 + (x1 - x0) * (i.toFloat() / n)
        g.lineTo(x, y + (if (i % 2 == 1) tooth * dir else 0f) - tooth * .35f * dir)
    }
    g.lineTo(x1, y + tooth * .6f * dir)
    g.lineTo(x0, y + tooth * .6f * dir)
    g.closePath()
    g.fill()
}

/** The prototype's `rgba(r,g,b,a)` with a computed alpha, without a string to parse each frame. */
private fun rgba(r: Int, g: Int, b: Int, a: Double): Color =
    Color(r, g, b, (a.coerceIn(0.0, 1.0) * 255).roundToInt())
