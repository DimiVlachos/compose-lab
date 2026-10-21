package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import dev.dimvlachos.lab.core.presentation.canvas2d.mulberry
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// The world, as the prototype builds it: the rod runs along x through the origin, y up, z toward
// the viewer, and the camera looks down at it from E above level, DIST away (in card heights).
private const val DEG = (PI / 180).toFloat()
private const val E = 32 * DEG
private const val DIST = 7f
/** How far the cards' bottom edges sit from the rod. */
internal const val R0 = .2f

// The wheel shows this many fallen cards in front of its position and this many behind it.
private const val PILE_FRONT = 7
private const val PILE_BACK = 15

// A card's lean, in degrees: upright cards tip back by BACK_STEP each further behind the front, a
// card flipped over the top falls to FALLEN, and those under it on the pile lie FALL_STEP further
// over. The pile's last shown cards lean furthest, BACK_MAX and FALL_MAX; deeper ones rest exactly
// beneath them, hidden.
private const val UPRIGHT = -6f
private const val BACK_STEP = 4.6f
private const val BACK_MAX = UPRIGHT - BACK_STEP * PILE_BACK
private const val FALLEN = 72f
private const val FALL_STEP = 3.4f
private const val FALL_MAX = FALLEN + FALL_STEP * (PILE_FRONT - 1)

/** Where the stage leaves room: under the app bar, and over the sheet at the foot. */
private const val TOP = 24f
private const val BOTTOM = 104f

private fun clamp(v: Float, a: Float, b: Float) = min(b, max(a, v))

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun ease(p: Float) = if (p < .5f) 2 * p * p else 1 - (-2 * p + 2).pow(2) / 2

private fun easeOut(p: Float) = 1 - (1 - p).pow(3)

private fun smooth(t0: Float): Float {
    val t = clamp(t0, 0f, 1f)
    return t * t * (3 - 2 * t)
}

/**
 * Card's lean on the rod, in radians, [d] cards behind the wheel's position (negative: in front).
 */
internal fun phiOf(d: Float): Float =
    when {
        d >= 0f -> max(BACK_MAX, UPRIGHT - BACK_STEP * d) * DEG
        d <= -1f -> min(FALL_MAX, FALLEN + FALL_STEP * (-d - 1)) * DEG
        else -> lerp(FALLEN, UPRIGHT, smooth(d + 1)) * DEG
    }

/**
 * Card [i] of the wheel, [d] cards behind its position, leaning [phi]; a [buried] card lies beneath
 * the pile's last shown one, so only its tab shows.
 */
internal class WheelSlot(val i: Int, val d: Float, val phi: Float, val buried: Boolean = false) {
    val depth: Float = sin(phi + E) * (R0 + .6f)
}

/**
 * The cards of a [count]-card wheel at [p] to paint, all but [pulled], in the order they are
 * painted: those less than a card past the pile's last shown ones, and beyond them the nearest
 * divider with its tab in each of the five places [tabOf] gives (null for a card with no tab).
 * Anything further out lies exactly beneath one of those, so leaving it out changes nothing on
 * screen, however the wheel moves.
 */
internal fun wheelSlots(
    p: Float,
    count: Int,
    pulled: Int,
    tabOf: (Int) -> Int? = { null },
): List<WheelSlot> {
    val lo = max(0, floor(p - PILE_FRONT - 1).toInt() + 1)
    val hi = min(count - 1, ceil(p + PILE_BACK + 1).toInt() - 1)
    val list = ArrayList<WheelSlot>()
    for (i in lo..hi) {
        if (i == pulled) continue
        val d = i - p
        list += WheelSlot(i, d, phiOf(d))
    }
    fun bury(deeper: IntProgression) {
        val seen = HashSet<Int>()
        for (i in deeper) {
            if (seen.size == 5) return
            val tab = tabOf(i) ?: continue
            if (i != pulled && seen.add(tab)) list += WheelSlot(i, i - p, phiOf(i - p), true)
        }
    }
    // The wheel can rest past either end of a deck that shrank under it.
    bury(min(lo, count) - 1 downTo 0)
    bury(max(hi + 1, 0) until count)
    // Cards lying together paint the deeper first, so the nearer covers it.
    return list.sortedWith(compareBy<WheelSlot> { it.depth }.thenByDescending { abs(it.d) })
}

/** A point [y] on screen and the perspective scale [s] there. */
internal data class Proj(val y: Float, val s: Float)

/** Where the pulled card comes to rest, facing you: its centre and size. */
internal data class PulledTarget(val cx: Float, val cy: Float, val w: Float, val h: Float)

private class Pose(
    val yT: Float,
    val yB: Float,
    val hwT: Float,
    val hwB: Float,
    val rot: Float,
    val qb: Float,
    val cx: Float,
)

private fun inPoly(x: Float, y: Float, p: List<Offset>): Boolean {
    var c = false
    var j = p.size - 1
    for (i in p.indices) {
        val (xi, yi) = p[i]
        val (xj, yj) = p[j]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c
        j = i
    }
    return c
}

private fun chromeGrad(g: Canvas2D, x0: Float, y0: Float, x1: Float, y1: Float): Gradient2D {
    val gr = g.createLinearGradient(x0, y0, x1, y1)
    gr.addColorStop(0f, "#2a2622")
    gr.addColorStop(.18f, "#f5e7d0")
    gr.addColorStop(.32f, "#9b9186")
    gr.addColorStop(.55f, "#4a443e")
    gr.addColorStop(.8f, "#b49a7c")
    gr.addColorStop(1f, "#2a2420")
    return gr
}

/**
 * The prototype's card file painted in a Canvas: the room baked once per size, then each frame the
 * wheel's cards in perspective strips, the knobs, and the card pulled out. Measures are the
 * prototype's CSS px, which the caller maps to dp. It remembers what it drew, for hit testing.
 */
internal class CardFileScene(private val cards: List<PaperCard>) {
    var w = 0f
        private set

    var h = 0f
        private set

    private var dpr = 1f
    var k = 100f
        private set

    var cx = 0f
        private set

    var cy = 0f
        private set

    /** How far a finger drags the cards to turn one. */
    var pitch = 24f
        private set

    private var texS = 100f
    private var hiS = 200f
    var target = PulledTarget(0f, 0f, 0f, 0f)
        private set

    /** The paper, type and ink; changing it bakes every card again. */
    var ink: CardInk? = null
        set(value) {
            field = value
            forget()
        }

    private val faces = HashMap<Int, ImageBitmap>()
    private val backs = HashMap<String, ImageBitmap>()
    private var hiFace: ImageBitmap? = null
    private var hiIdx = -1
    private var bg: ImageBitmap? = null

    private class Drawn(val i: Int, val polys: List<List<Offset>>)

    private var drawn = emptyList<Drawn>()
    private var pulledPoly: List<Offset>? = null

    internal val cachedFaces: Int
        get() = faces.size

    private fun forget() {
        faces.clear()
        backs.clear()
        hiFace = null
        hiIdx = -1
    }

    /** Fits the file to a stage of [width] × [height] at [density] pixels to the unit. */
    fun layout(width: Float, height: Float, density: Float) {
        if (width == w && height == h && min(2f, density) == dpr && bg != null) return
        w = width
        h = height
        dpr = min(2f, density)
        val avail = h - TOP - BOTTOM - 16
        k = minOf(w * .41f, avail / 3.0f, 190f)
        cx = w / 2
        cy = TOP + 1.45f * k + max(0f, (avail - 3.0f * k) * .52f)
        pitch = clamp(.17f * k, 18f, 34f)
        texS = k * 1.12f
        val tw = min(w * .9f, 420f)
        hiS = tw / CW
        val th = (CH + TH) * hiS
        target =
            PulledTarget(
                cx,
                clamp(cy - .05f * k, TOP + th / 2 + 8, h - 150 - th / 2 + TH * hiS * .5f),
                tw,
                th,
            )
        forget()
        bg = null
    }

    fun proj(r: Float, phi: Float): Proj {
        val a = phi + E
        val s = DIST / (DIST - r * sin(a))
        return Proj(cy - k * s * r * cos(a), s)
    }

    /** World to screen, for the stand. */
    fun p3(x: Float, y: Float, z: Float): Offset {
        val up = y * cos(E) - z * sin(E)
        val dz = y * sin(E) + z * cos(E)
        val s = DIST / (DIST - dz)
        return Offset(cx + x * k * s, cy - up * k * s)
    }

    // ---------- Textures ----------

    private fun requireInk() = checkNotNull(ink) { "The scene paints with ink" }

    private fun faceOf(i: Int): ImageBitmap =
        faces.getOrPut(i) { bakeFace(cards[i], texS, min(dpr, 1.6f), requireInk()).bitmap }

    private fun backOf(i: Int): ImageBitmap {
        val c = cards[i]
        val key = if (c is PaperCard.Divider) "d${c.ordinal % 5}" else "c"
        return backs.getOrPut(key) {
            val paper =
                if (c is PaperCard.Divider) PaperCard.Divider(c.letter, c.ordinal % 5)
                else PaperCard.Contact("", "", emptyList(), 5)
            bakeBack(paper, texS, min(dpr, 1.6f), requireInk()).bitmap
        }
    }

    private fun hiOf(i: Int): ImageBitmap {
        val have = hiFace
        if (hiIdx == i && have != null) return have
        return bakeFace(cards[i], hiS, dpr, requireInk()).bitmap.also {
            hiFace = it
            hiIdx = i
        }
    }

    private fun trimCache(p: Float) {
        if (faces.size < 40) return
        faces.keys.filter { abs(it - p) > 18 }.forEach { faces.remove(it) }
    }

    // ---------- The room (baked): wall, desk, lamp light, the stand ----------

    private fun renderBg(): ImageBitmap {
        val g = Canvas2D.create(max(1, ceil(w * dpr).toInt()), max(1, ceil(h * dpr).toInt()))
        g.setTransform(dpr, 0f, 0f, dpr, 0f, 0f)
        val r = mulberry(4)
        val baseY = cy + 1.1f * k
        val deskY = cy - .55f * k // back edge of the desk
        // Wall: deep olive in lamp shadow.
        val wall = g.createLinearGradient(0f, 0f, 0f, deskY)
        wall.addColorStop(0f, "#15140f")
        wall.addColorStop(1f, "#2a2a20")
        g.fillStyle = wall
        g.fillRect(0f, 0f, w, deskY + 2)
        // Walnut desk, its grain receding to the wall.
        val desk = g.createLinearGradient(0f, deskY, 0f, h)
        desk.addColorStop(0f, "#2b170c")
        desk.addColorStop(.35f, "#4a2814")
        desk.addColorStop(1f, "#2a160b")
        g.fillStyle = desk
        g.fillRect(0f, deskY, w, h - deskY)
        g.save()
        g.beginPath()
        g.rect(0f, deskY, w, h - deskY)
        g.clip()
        val vx = w * .5f
        for (i in 0 until 70) {
            val bx = ((r() * 2.6 - .8) * w).toFloat()
            val a = (r() * .14 + .03).toFloat()
            g.strokeStyle =
                if (r() < .55) Color(red = 18, green = 8, blue = 2, alpha = (a * 255).toInt())
                else Color(red = 140, green = 82, blue = 42, alpha = (a * .7f * 255).toInt())
            g.lineWidth = (.6 + r() * 2.2).toFloat()
            g.beginPath()
            g.moveTo(vx + (bx - vx) * .12f, deskY)
            g.bezierCurveTo(
                vx + (bx - vx) * .4f + ((r() - .5) * 30).toFloat(),
                deskY + (h - deskY) * .4f,
                bx + ((r() - .5) * 40).toFloat(),
                h * .9f,
                bx,
                h + 10,
            )
            g.stroke()
        }
        g.restore()
        // Desk edge highlight.
        g.fillStyle = "rgba(255,190,120,.10)"
        g.fillRect(0f, deskY, w, 1.5f)
        // The lamp, off to the upper left: a pool on the desk and a wash on the wall.
        g.save()
        g.globalCompositeOperation = "lighter"
        val wash =
            g.createRadialGradient(
                cx - .55f * w,
                cy - 1.6f * k,
                0f,
                cx - .55f * w,
                cy - 1.6f * k,
                w * 1.1f,
            )
        wash.addColorStop(0f, "rgba(255,170,80,.30)")
        wash.addColorStop(.45f, "rgba(255,140,60,.10)")
        wash.addColorStop(1f, "rgba(255,140,60,0)")
        g.fillStyle = wash
        g.fillRect(0f, 0f, w, deskY)
        g.save()
        g.translate(cx - .12f * w, baseY + .1f * k)
        g.scale(1f, .42f)
        val pool = g.createRadialGradient(0f, 0f, 0f, 0f, 0f, w * .95f)
        pool.addColorStop(0f, "rgba(255,175,90,.42)")
        pool.addColorStop(.5f, "rgba(255,140,60,.13)")
        pool.addColorStop(1f, "rgba(255,140,60,0)")
        g.fillStyle = pool
        g.fillRect(-w * 1.2f, -w * 1.2f, w * 2.4f, w * 2.4f)
        g.restore()
        g.restore()
        // Shadow of the file, thrown back and to the right.
        g.save()
        g.translate(cx + .55f * k, baseY - .25f * k)
        g.rotate(-.18f)
        g.scale(1f, .34f)
        val sh = g.createRadialGradient(0f, 0f, 0f, 0f, 0f, 1.55f * k)
        sh.addColorStop(0f, "rgba(10,4,0,.6)")
        sh.addColorStop(.6f, "rgba(10,4,0,.25)")
        sh.addColorStop(1f, "rgba(10,4,0,0)")
        g.fillStyle = sh
        g.beginPath()
        g.arc(0f, 0f, 1.55f * k, 0f, TAU)
        g.fill()
        g.restore()
        drawStand(g)
        // Vignette.
        val v = g.createRadialGradient(w / 2, cy, h * .2f, w / 2, cy, h * .85f)
        v.addColorStop(0f, "rgba(0,0,0,0)")
        v.addColorStop(1f, "rgba(0,0,0,.5)")
        g.fillStyle = v
        g.fillRect(0f, 0f, w, h)
        g.bitmap.freeze()
        return g.bitmap
    }

    private fun drawStand(g: Canvas2D) {
        val hx = CW / 2
        val by = -1.32f
        val bd = .4f
        val bw = hx + .34f
        // Base: black enamel slab with a chrome lip.
        val tl = p3(-bw, by, -bd)
        val tr = p3(bw, by, -bd)
        val fr = p3(bw, by, bd)
        val fl = p3(-bw, by, bd)
        val fr2 = p3(bw, by - .16f, bd)
        val fl2 = p3(-bw, by - .16f, bd)
        g.save()
        g.fillStyle = "rgba(0,0,0,.55)"
        g.beginPath()
        g.ellipse((fl2.x + fr2.x) / 2, fl2.y + 3, (fr2.x - fl2.x) * .55f, 9f, 0f, 0f, TAU)
        g.fill()
        val top = g.createLinearGradient(0f, tl.y, 0f, fl.y)
        top.addColorStop(0f, "#14110f")
        top.addColorStop(1f, "#2c2620")
        g.fillStyle = top
        g.beginPath()
        g.moveTo(tl.x, tl.y)
        g.lineTo(tr.x, tr.y)
        g.lineTo(fr.x, fr.y)
        g.lineTo(fl.x, fl.y)
        g.closePath()
        g.fill()
        val tsh = g.createLinearGradient(tl.x, 0f, tr.x, 0f)
        tsh.addColorStop(0f, "rgba(255,190,120,.16)")
        tsh.addColorStop(.5f, "rgba(255,190,120,.04)")
        tsh.addColorStop(1f, "rgba(0,0,0,0)")
        g.fillStyle = tsh
        g.fill()
        val front = g.createLinearGradient(0f, fl.y, 0f, fl2.y)
        front.addColorStop(0f, "#0d0b0a")
        front.addColorStop(1f, "#050404")
        g.fillStyle = front
        g.beginPath()
        g.moveTo(fl.x, fl.y)
        g.lineTo(fr.x, fr.y)
        g.lineTo(fr2.x, fr2.y)
        g.lineTo(fl2.x, fl2.y)
        g.closePath()
        g.fill()
        g.strokeStyle = chromeGrad(g, fl.x, 0f, fr.x, 0f)
        g.lineWidth = max(1.2f, .018f * k)
        g.beginPath()
        g.moveTo(fl.x, fl.y + 1)
        g.lineTo(fr.x, fr.y + 1)
        g.stroke()
        // Two chrome struts up to the rod ends.
        for (sgn in intArrayOf(-1, 1)) {
            val a = p3(sgn * (hx + .2f), by + .02f, 0f)
            val b = p3(sgn * (hx + .1f), 0f, 0f)
            val wdt = .055f * k
            val gr = g.createLinearGradient(a.x - wdt, 0f, a.x + wdt, 0f)
            gr.addColorStop(0f, "#3a332c")
            gr.addColorStop(.35f, "#f2e2c8")
            gr.addColorStop(.6f, "#7b7066")
            gr.addColorStop(1f, "#2a2420")
            g.strokeStyle = gr
            g.lineWidth = wdt
            g.lineCap = "round"
            g.beginPath()
            g.moveTo(a.x, a.y)
            g.lineTo(b.x, b.y)
            g.stroke()
            // Foot plate.
            g.fillStyle = "#1b1714"
            g.beginPath()
            g.ellipse(a.x, a.y + 1, .1f * k, .035f * k, 0f, 0f, TAU)
            g.fill()
            g.strokeStyle = "rgba(240,210,170,.35)"
            g.lineWidth = 1f
            g.stroke()
        }
        // The rod.
        val l = p3(-hx - .1f, 0f, 0f)
        val rr = p3(hx + .1f, 0f, 0f)
        val rw = .06f * k
        g.fillStyle = chromeGrad(g, 0f, l.y - rw, 0f, l.y + rw)
        g.fillRect(l.x, l.y - rw, rr.x - l.x, rw * 2)
        g.restore()
    }

    private fun drawKnobs(g: Canvas2D, p: Float) {
        val hx = CW / 2
        val rot = p * .42f
        for (sgn in intArrayOf(-1, 1)) {
            val a = p3(sgn * (hx + .07f), 0f, 0f)
            val b = p3(sgn * (hx + .33f), 0f, 0f)
            val x0 = min(a.x, b.x)
            val x1 = max(a.x, b.x)
            val rad = .28f * k
            val y = a.y
            g.save()
            g.beginPath()
            g.roundRect(x0, y - rad, x1 - x0, rad * 2, min(rad * .3f, (x1 - x0) * .3f))
            g.shadowColor = "rgba(0,0,0,.55)"
            g.shadowBlur = 14f
            g.shadowOffsetY = 6f
            g.fillStyle = chromeGrad(g, 0f, y - rad, 0f, y + rad)
            g.fill()
            g.shadowColor = "rgba(0,0,0,0)"
            g.shadowBlur = 0f
            g.clip()
            // Knurling: ridges around the circumference roll as the wheel turns.
            for (j in 0 until 28) {
                val ang = j * TAU / 28 + rot
                val c = cos(ang)
                if (c <= 0) continue
                val yy = y - rad * sin(ang)
                g.fillStyle =
                    Color(
                        red = 20,
                        green = 14,
                        blue = 8,
                        alpha = ((.18f + .25f * (1 - c)) * 255).toInt(),
                    )
                g.fillRect(x0, yy - .7f, x1 - x0, 1.4f)
                g.fillStyle =
                    Color(red = 255, green = 240, blue = 215, alpha = (.22f * c * 255).toInt())
                g.fillRect(x0, yy + .8f, x1 - x0, .9f)
            }
            // Warm reflection of the lamp, fixed while the knob turns under it.
            val hl = g.createLinearGradient(0f, y - rad, 0f, y + rad)
            hl.addColorStop(0f, "rgba(255,220,170,0)")
            hl.addColorStop(.22f, "rgba(255,226,180,.55)")
            hl.addColorStop(.34f, "rgba(255,220,170,0)")
            hl.addColorStop(1f, "rgba(0,0,0,.35)")
            g.fillStyle = hl
            g.fillRect(x0, y - rad, x1 - x0, rad * 2)
            val side = g.createLinearGradient(x0, 0f, x1, 0f)
            side.addColorStop(0f, if (sgn < 0) "rgba(0,0,0,.35)" else "rgba(0,0,0,0)")
            side.addColorStop(1f, if (sgn < 0) "rgba(0,0,0,0)" else "rgba(0,0,0,.4)")
            g.fillStyle = side
            g.fillRect(x0, y - rad, x1 - x0, rad * 2)
            g.restore()
        }
    }

    // ---------- The wheel ----------

    private class Strips(val topY: Float, val hwT: Float)

    private fun cardStrips(
        g: Canvas2D,
        tex: ImageBitmap,
        phi: Float,
        alphaDark: Float,
        card: PaperCard,
        polysOut: MutableList<List<Offset>>,
        rOff: Float = 0f,
    ): Strips {
        val rTop = R0 + CH + TH + rOff
        val n = 14
        val th = tex.height.toFloat() / n
        val pts = List(n + 1) { j -> proj(rTop - (j.toFloat() / n) * (CH + TH), phi) }
        // The lamp's shading goes on the paper itself, so the notches stay open.
        g.imageShade = Color(red = 22, green = 12, blue = 4, alpha = (alphaDark * 255).toInt())
        for (j in 0 until n) {
            val a = pts[j]
            val b = pts[j + 1]
            val y0 = min(a.y, b.y)
            val hgt = abs(b.y - a.y) + .7f
            val hw = k * (a.s + b.s) / 2 * CW / 2
            g.drawImage(tex, 0f, j * th, tex.width.toFloat(), th, cx - hw, y0 - .2f, hw * 2, hgt)
        }
        g.imageShade = Color.Transparent
        val t = pts[0]
        val tb = proj(rTop - TH, phi)
        val bb = pts[n]
        val hwT = k * tb.s * CW / 2
        val hwB = k * bb.s * CW / 2
        val body =
            listOf(
                Offset(cx - hwT, tb.y),
                Offset(cx + hwT, tb.y),
                Offset(cx + hwB, bb.y),
                Offset(cx - hwB, bb.y),
            )
        polysOut += body
        if (card is PaperCard.Divider) {
            val u0 = tabX(card.ordinal) - CW / 2
            val u1 = u0 + .3f
            polysOut +=
                listOf(
                    Offset(cx + u0 * k * t.s, t.y),
                    Offset(cx + u1 * k * t.s, t.y),
                    Offset(cx + u1 * k * tb.s, tb.y),
                    Offset(cx + u0 * k * tb.s, tb.y),
                )
        }
        return Strips(tb.y, hwT)
    }

    /**
     * How dark the lamp leaves a card [d] behind the wheel's position, leaning [phi]: the raw
     * figure, which the paper's shading clamps and its lit edge reads as it is.
     */
    private fun darkOf(d: Float, phi: Float, front: Boolean): Float {
        // The lamp's direction (y, z), normalised.
        val ln = hypot(hypot(.35f, .8f), .55f)
        val nDotL = (-sin(phi) * .8f / ln + cos(phi) * .55f / ln) * (if (front) 1 else -1)
        val lit = .5f + .62f * max(0f, nDotL)
        var dark = clamp(1 - lit, 0f, .55f)
        if (d > 0) dark += min(.32f, .028f * d)
        if (!front) dark += .2f + min(.3f, .05f * max(0f, -d - 1))
        else if (d < -.5f) dark += .12f * smooth((-d - .5f) * 2)
        return dark
    }

    /**
     * The gap above a card standing [front] on, its top edge at [r]: a soft contact shadow on the
     * card behind, then a lit paper edge. A fallen card shows its back, its top edge now its lower
     * one, lit, with the gap's shadow below it. [fade] fades both, as a card rises out of the gap.
     */
    private fun drawGap(g: Canvas2D, r: Strips, front: Boolean, dark: Float, fade: Float = 1f) {
        g.save()
        g.globalAlpha = fade
        if (front) {
            val sh = g.createLinearGradient(0f, r.topY - 7, 0f, r.topY)
            sh.addColorStop(0f, "rgba(15,8,2,0)")
            sh.addColorStop(1f, "rgba(15,8,2,.32)")
            g.fillStyle = sh
            g.fillRect(cx - r.hwT + 3, r.topY - 7, r.hwT * 2 - 6, 7f)
            g.fillStyle =
                Color(
                    red = 255,
                    green = 236,
                    blue = 200,
                    alpha = (.55f * (1 - dark) * 255).toInt().coerceIn(0, 255),
                )
            g.fillRect(cx - r.hwT + 4, r.topY - .2f, r.hwT * 2 - 8, 1f)
        } else {
            val sh = g.createLinearGradient(0f, r.topY, 0f, r.topY + 6)
            sh.addColorStop(0f, "rgba(15,8,2,.4)")
            sh.addColorStop(1f, "rgba(15,8,2,0)")
            g.fillStyle = sh
            g.fillRect(cx - r.hwT + 3, r.topY, r.hwT * 2 - 6, 6f)
            g.fillStyle =
                Color(
                    red = 255,
                    green = 226,
                    blue = 180,
                    alpha = (.35f * (1 - dark) * 255).toInt().coerceIn(0, 255),
                )
            g.fillRect(cx - r.hwT + 4, r.topY - 1, r.hwT * 2 - 8, 1f)
        }
        g.restore()
    }

    private fun drawWheel(g: Canvas2D, p: Float, pulled: Int, q: Float, reduced: Boolean) {
        var list =
            wheelSlots(p, cards.size, pulled) { (cards[it] as? PaperCard.Divider)?.ordinal?.rem(5) }
        // A pulled card still in its rail stands among the others, painted in its turn, so it
        // leaves and lands among them as the wheel draws them.
        val rail = pulled in cards.indices && pulledPose(q, reduced).qb <= 0f
        if (rail) {
            list =
                (list + WheelSlot(pulled, pulled - p, phiOf(pulled - p))).sortedWith(
                    compareBy<WheelSlot> { it.depth }.thenByDescending { abs(it.d) },
                )
        }
        val out = ArrayList<Drawn>(list.size)
        for (c in list) {
            if (rail && c.i == pulled) {
                drawInRail(g, c, q)
                continue
            }
            val card = cards[c.i]
            val front = cos(c.phi + E) > 0
            val tex = if (front) faceOf(c.i) else backOf(c.i)
            val dark = darkOf(c.d, c.phi, front)
            val polys = ArrayList<List<Offset>>(2)
            val r = cardStrips(g, tex, c.phi, clamp(dark, 0f, .75f), card, polys)
            // A buried card lies on the card covering it, with no gap between them to shade or
            // light.
            if (!c.buried) drawGap(g, r, front, dark)
            out += Drawn(c.i, polys)
        }
        drawn = out
    }

    /**
     * The pulled card [c] sliding up out of its rail, [q] of the way out: shaded as the wheel
     * shades it, its gap fading as it rises from it and the shadow it lifts with growing in.
     */
    private fun drawInRail(g: Canvas2D, c: WheelSlot, q: Float) {
        val qa = clamp(q / .32f, 0f, 1f)
        val rOff = .78f * easeOut(qa)
        val top = proj(R0 + CH + rOff, c.phi)
        val bottom = proj(R0 + rOff, c.phi)
        val hwm = k * top.s * CW / 2
        val hwB = k * bottom.s * CW / 2
        if (qa > 0f) liftedShadow(g, top.y, bottom.y, hwm, hwB, qa, 0f)
        val dark = darkOf(c.d, c.phi, true)
        val polys = ArrayList<List<Offset>>(2)
        val r = cardStrips(g, faceOf(c.i), c.phi, clamp(dark, 0f, .75f), cards[c.i], polys, rOff)
        drawGap(g, r, true, dark, 1 - qa)
        pulledPoly = polys.first()
    }

    /**
     * The shadow a lifted card casts, its body from [bodyTop] to [yB], [hwm] and [hwB] across at
     * them, [lift] of the way to its full strength and [qb] of the way to the front. It is cast
     * from a paper-coloured fill clipped from under the card, so that only the shadow shows round
     * it and its notches show what lies behind.
     */
    private fun liftedShadow(
        g: Canvas2D,
        bodyTop: Float,
        yB: Float,
        hwm: Float,
        hwB: Float,
        lift: Float,
        qb: Float,
    ) {
        g.save()
        g.beginPath()
        g.rect(-w, -h, w * 3, h * 3)
        g.moveTo(cx - hwm, bodyTop)
        g.lineTo(cx + hwm, bodyTop)
        g.lineTo(cx + hwB, yB)
        g.lineTo(cx - hwB, yB)
        g.closePath()
        g.clip("evenodd")
        // In hundredths, so the colour cache sees a hundred strings, not one a frame.
        g.shadowColor = "rgba(0,0,0,${((.25f + .35f * qb) * lift * 100).roundToInt() / 100f})"
        g.shadowBlur = (6 + 34 * qb) * dpr
        g.shadowOffsetY = (3 + 22 * qb) * dpr
        g.fillStyle = "#e9dfc9"
        g.beginPath()
        g.moveTo(cx - hwm, bodyTop + 1)
        g.lineTo(cx + hwm, bodyTop + 1)
        g.lineTo(cx + hwB - 1, yB - 2)
        g.lineTo(cx - hwB + 1, yB - 2)
        g.closePath()
        g.fill()
        g.restore()
    }

    // ---------- The pulled card ----------

    private fun pulledPose(q: Float, reduced: Boolean): Pose {
        // Stage A: slide up out of the rail. Stage B: fly to the front, face on.
        val qa = clamp(q / .32f, 0f, 1f)
        val qb = clamp((q - .32f) / .68f, 0f, 1f)
        val phi = UPRIGHT * DEG
        val rOff = .78f * easeOut(qa)
        val t = proj(R0 + CH + TH + rOff, phi)
        val b = proj(R0 + rOff, phi)
        val e = if (reduced) qb else ease(qb)
        return Pose(
            yT = lerp(t.y, target.cy - target.h / 2, e),
            yB = lerp(b.y, target.cy + target.h / 2, e),
            hwT = lerp(k * t.s * CW / 2, target.w / 2, e),
            hwB = lerp(k * b.s * CW / 2, target.w / 2, e),
            rot = sin(e * PI.toFloat()) * -.07f,
            qb = e,
            cx = target.cx,
        )
    }

    private fun drawPulled(g: Canvas2D, pulled: Int, q: Float, reduced: Boolean) {
        if (pulled < 0 || pulled >= cards.size) {
            pulledPoly = null
            return
        }
        val pose = pulledPose(q, reduced)
        // Still in its rail, the wheel paints it.
        if (pose.qb <= 0f) return
        val midY = (pose.yT + pose.yB) / 2
        if (pose.qb > 0) {
            g.fillStyle =
                Color(red = 8, green = 4, blue = 1, alpha = (.42f * pose.qb * 255).toInt())
            g.fillRect(0f, 0f, w, h)
        }
        val tex = if (pose.hwT * 2 > texS * CW * 1.15f) hiOf(pulled) else faceOf(pulled)
        g.save()
        g.translate(pose.cx, midY)
        g.rotate(pose.rot)
        g.translate(-pose.cx, -midY)
        // Down the card, [a] of the way from its top: where it is, and its half width there. Just
        // out of its rail it is in the rail's perspective, as the wheel drew it; it eases to lying
        // flat as it comes to the front.
        val inRail = { a: Float -> proj(R0 + CH + TH + .78f - a * (CH + TH), phiOf(0f)) }
        val yAt = { a: Float -> lerp(inRail(a).y, lerp(pose.yT, pose.yB, a), pose.qb) }
        val hwAt = { a: Float ->
            lerp(k * inRail(a).s * CW / 2, lerp(pose.hwT, pose.hwB, a), pose.qb)
        }
        // Lifted shadow.
        val bodyTop = yAt(TH / (CH + TH))
        val hwm = hwAt(TH / (CH + TH))
        val yB = yAt(1f)
        val hwB = hwAt(1f)
        liftedShadow(g, bodyTop, yB, hwm, hwB, 1f, pose.qb)
        // The lamp's shading it had in its rail, gone as it comes to the front.
        val dark = clamp(darkOf(0f, phiOf(0f), true), 0f, .75f) * (1 - pose.qb)
        g.imageShade = Color(red = 22, green = 12, blue = 4, alpha = (dark * 255).toInt())
        // As many strips as the wheel draws a card in, so it is sampled alike as it leaves the
        // rail.
        val n = 14
        val sh = tex.height.toFloat() / n
        // The card in a layer of its own, so the sheen below lands on its paper and not its
        // notches.
        g.beginLayer()
        for (j in 0 until n) {
            val a = j.toFloat() / n
            val b = (j + 1).toFloat() / n
            val y0 = yAt(a)
            val y1 = yAt(b)
            val hw = (hwAt(a) + hwAt(b)) / 2
            g.drawImage(
                tex,
                0f,
                j * sh,
                tex.width.toFloat(),
                sh,
                pose.cx - hw,
                y0 - .2f,
                hw * 2,
                y1 - y0 + .7f,
            )
        }
        // Lamp sheen across the lifted card.
        if (pose.qb > 0) {
            val gl =
                g.createLinearGradient(pose.cx - pose.hwB, pose.yT, pose.cx + pose.hwB, pose.yB)
            gl.addColorStop(
                0f,
                Color(red = 255, green = 230, blue = 190, alpha = (.12f * pose.qb * 255).toInt()),
            )
            gl.addColorStop(.5f, "rgba(255,230,190,0)")
            gl.addColorStop(
                1f,
                Color(red = 40, green = 20, blue = 5, alpha = (.12f * pose.qb * 255).toInt()),
            )
            g.fillStyle = gl
            g.globalCompositeOperation = "source-atop"
            g.beginPath()
            g.moveTo(pose.cx - hwm, bodyTop)
            g.lineTo(pose.cx + hwm, bodyTop)
            g.lineTo(pose.cx + hwB, yB)
            g.lineTo(pose.cx - hwB, yB)
            g.closePath()
            g.fill()
        }
        g.endLayer()
        g.restore()
        pulledPoly =
            listOf(
                Offset(pose.cx - hwm, bodyTop),
                Offset(pose.cx + hwm, bodyTop),
                Offset(pose.cx + hwB, yB),
                Offset(pose.cx - hwB, yB),
            )
    }

    /**
     * Paints a frame into [g], whose transform maps the prototype's px to the canvas: the room, the
     * wheel at [position], the knobs, and card [pulled] (−1 for none) [q] of the way out.
     */
    fun paint(g: Canvas2D, position: Float, pulled: Int, q: Float, reduced: Boolean) {
        if (w <= 0f || h <= 0f) return
        val room = bg ?: renderBg().also { bg = it }
        g.drawImage(room, 0f, 0f, w, h)
        drawWheel(g, position, pulled, q, reduced)
        drawKnobs(g, position)
        drawPulled(g, pulled, q, reduced)
        trimCache(position)
    }

    // ---------- Hit testing, on the frame last painted ----------

    /** The card drawn under ([x], [y]), the nearest first, or −1. */
    fun cardAt(x: Float, y: Float): Int {
        for (n in drawn.indices.reversed()) {
            for (poly in drawn[n].polys) if (inPoly(x, y, poly)) return drawn[n].i
        }
        return -1
    }

    /** Whether ([x], [y]) is on a knob, with a little to spare at its outer end. */
    fun knobAt(x: Float, y: Float): Boolean {
        val hx = CW / 2
        val a = p3(hx + .02f, 0f, 0f).x
        val b = p3(hx + .42f, 0f, 0f).x
        val ax = abs(x - cx)
        return ax > a - cx && ax < b - cx + 6 && abs(y - cy) < .42f * k
    }

    /** Whether ([x], [y]) lies where the cards are, so a drag there turns them. */
    fun onCards(x: Float, y: Float): Boolean =
        y > cy - 1.6f * k && y < cy + 1.3f * k && abs(x - cx) < CW * k * .62f

    /** Whether ([x], [y]) is on the pulled-out card. */
    fun inPulled(x: Float, y: Float): Boolean = pulledPoly?.let { inPoly(x, y, it) } == true
}
