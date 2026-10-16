package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The inside of the globe: a little Santorini under the weather. A line-for-line port of the HTML
 * prototype's painters (paintIsland, paintBackdrop, paintPlinth, paintScenes, cloudSprite, glow and
 * bokeh), with its colours, numbers and random seeds, so the native globe holds the same island.
 * Each weather's scene is baked once into a T by T bitmap in the prototype's units: [-1, 1] across.
 */

/** The size of the baked interior, as the prototype's texture. */
internal const val T = 512

/** The sea's surface in globe units: the lowest any glitter rests. */
internal const val FLOOR = 0.285f

private const val PIf = PI.toFloat()

/** A weather's light, as the prototype's LIT table has it. */
internal class Lit(
    val sky: List<Pair<Float, String>>,
    val sea: List<String>,
    val plinth: List<String>,
    val isle: String,
    val white: List<String>,
    val side: String,
    val roof: String,
    val key: String,
    val warm: String,
    val tint: String?,
    val wet: Boolean,
    val dome: List<String>,
    val sails: String,
    val glow: IntArray,
)

internal val LIT: Map<GlobeScene, Lit> =
    mapOf(
        GlobeScene.Sunny to
            Lit(
                sky =
                    listOf(0f to "#3f7fc6", .42f to "#86b7df", .74f to "#e2d6bd", 1f to "#f6c58e"),
                sea = listOf("#6aadd8", "#2a6aa3"),
                plinth = listOf("#41b5c3", "#11698a"),
                isle = "#8798b3",
                white = listOf("#fff4e2", "#f0d9bb"),
                side = "#93a6c6",
                roof = "#fff9ef",
                key = "255,238,210",
                warm = "255,178,104",
                tint = null,
                wet = false,
                dome = listOf("#7fb0ff", "#1f58c8", "#0d2e78"),
                sails = "#f6efe2",
                glow = intArrayOf(255, 196, 120),
            ),
        GlobeScene.Windy to
            Lit(
                sky = listOf(0f to "#4a80bf", .5f to "#8db5dd", .8f to "#d2e3f1", 1f to "#e4eef6"),
                sea = listOf("#5c93c6", "#1f5590"),
                plinth = listOf("#38a2c4", "#105a86"),
                isle = "#6b83a2",
                white = listOf("#fbfaf6", "#e2e3e4"),
                side = "#9fb2cc",
                roof = "#ffffff",
                key = "235,243,255",
                warm = "255,214,170",
                tint = "rgba(60,90,140,.07)",
                wet = false,
                dome = listOf("#79abf6", "#1e56c2", "#0d2d74"),
                sails = "#f3f2ee",
                glow = intArrayOf(160, 196, 236),
            ),
        GlobeScene.Shower to
            Lit(
                sky = listOf(0f to "#56657c", .45f to "#8191a7", .8f to "#bcc6d3", 1f to "#d9dee4"),
                sea = listOf("#7e95aa", "#3a5672"),
                plinth = listOf("#5b94a6", "#275670"),
                isle = "#7f8a9c",
                white = listOf("#eef0f3", "#cfd5dd"),
                side = "#8f9db2",
                roof = "#f4f6f8",
                key = "214,222,236",
                warm = "232,214,196",
                tint = "rgba(40,52,74,.16)",
                wet = true,
                dome = listOf("#6f9ae0", "#2a5cb4", "#13336f"),
                sails = "#e6e8ea",
                glow = intArrayOf(176, 190, 214),
            ),
    )

/** `rgba(rgb, a)` with the alpha rounded as the prototype's `toFixed(2)` rounds it. */
internal fun rgba(rgb: String, a: Double): Color {
    val p = rgb.split(",")
    return Color(
        red = p[0].trim().toInt(),
        green = p[1].trim().toInt(),
        blue = p[2].trim().toInt(),
        alpha = ((a * 100).roundToInt() / 100.0 * 255).roundToInt().coerceIn(0, 255),
    )
}

// The caldera wall: a steep cliff of layered tuff and lava with Oia cascading down its top third.
internal val TOPP: List<Pair<Float, Float>> =
    listOf(
        -.76f to .3f,
        -.73f to .2f,
        -.748f to .12f,
        -.716f to .05f,
        -.722f to -.01f,
        -.7f to -.07f,
        -.67f to -.1f,
        -.64f to -.128f,
        -.56f to -.136f,
        -.5f to -.15f,
        -.4f to -.14f,
        -.32f to -.155f,
        -.22f to -.152f,
        -.14f to -.166f,
        0f to -.158f,
        .1f to -.15f,
        .2f to -.142f,
        .28f to -.13f,
        .36f to -.124f,
        .42f to -.11f,
        .5f to -.1f,
        .56f to -.082f,
        .6f to -.05f,
        .635f to -.01f,
        .628f to .05f,
        .67f to .1f,
        .665f to .17f,
        .7f to .22f,
        .72f to .3f,
    )

internal fun topAt(x: Float): Float {
    if (x <= TOPP[0].first) return TOPP[0].second
    for (i in 1 until TOPP.size) {
        if (x <= TOPP[i].first) {
            val (x0, y0) = TOPP[i - 1]
            val (x1, y1) = TOPP[i]
            return y0 + (y1 - y0) * ((x - x0) / (x1 - x0))
        }
    }
    return TOPP.last().second
}

internal fun wob(x: Float, f: Float, ph: Float): Float =
    sin(x * f + ph) * .6f + sin(x * f * 2.3f + ph * 1.7f) * .3f + sin(x * f * 5.1f + ph * .3f) * .1f

private const val CHx = -.09f
private const val BTx = -.235f
internal const val WMx = .5f

/** A house of the village: whitewashed cube [x]..[x]+[w], [h] tall, standing on [base]. */
internal class House(
    val x: Float,
    val w: Float,
    val h: Float,
    val base: Float,
    val row: Int,
    val tone: Float,
    val win: Float,
    val vault: Boolean,
)

internal val houses: List<House> = run {
    val r = mulberry(7)
    fun R() = r().toFloat()
    val out = mutableListOf<House>()
    for (row in 0 until 4) {
        var x = -.6f + row * .06f + R() * .04f
        val xEnd = .44f - row * .1f
        while (x < xEnd) {
            val w = .056f + R() * .05f - row * .004f
            val h = .046f + R() * .03f
            val mid = x + w / 2
            val skip =
                (row == 0 && mid > -.29f && mid < .0f) ||
                    (row == 1 && mid > -.2f && mid < -.02f) ||
                    (mid > .44f && mid < .58f)
            val base = topAt(mid) + .014f + row * .056f + R() * .016f
            if (!skip && R() > .12f + row * .1f)
                out += House(x, w, h, base, row, R(), R(), R() < .14f)
            x += w + .004f + R() * (.012f + row * .03f)
        }
    }
    out.sortedWith(compareBy<House> { it.row }.thenBy { it.x })
}

private val windBase = topAt(WMx) + .012f

/** The windmill's hub, where the sails turn, in globe units. */
internal val WindmillHub = WMx to windBase - .135f

private fun Canvas2D.unitSpace() = setTransform(T / 2f, 0f, 0f, T / 2f, T / 2f, T / 2f)

private fun Canvas2D.rockPath() {
    beginPath()
    moveTo(-.76f, .5f)
    lineTo(TOPP[0].first, TOPP[0].second)
    for (i in 1 until TOPP.size - 1) {
        val (x0, y0) = TOPP[i]
        val (x1, y1) = TOPP[i + 1]
        quadraticCurveTo(x0, y0, (x0 + x1) / 2, (y0 + y1) / 2)
    }
    lineTo(TOPP.last().first, TOPP.last().second)
    lineTo(.71f, .5f)
    closePath()
}

internal fun paintIsland(c: Canvas2D, L: Lit) {
    c.unitSpace()
    val rnd = mulberry(17)
    fun R() = rnd()
    // Strata, top to bottom: pumice over the rim, ochre, rust, charcoal lava, rust-brown, dark
    // lava, dusky red.
    val bands: List<Pair<String, (Float) -> Float>> =
        listOf(
            "#efe1c8" to { x -> topAt(x) - .02f },
            "#e2c49a" to { x -> topAt(x) + .05f + .008f * wob(x, 6f, 1f) },
            "#c99a6a" to { x -> topAt(x) + .072f + .01f * wob(x, 5f, 9f) },
            "#a8522f" to { x -> -.03f + .016f * wob(x, 2.6f, 3f) + .045f * x },
            "#7e3524" to { x -> .02f + .014f * wob(x, 3.3f, 4f) + .04f * x },
            "#2b2428" to { x -> .052f + .01f * wob(x, 4.4f, 5f) + .035f * x },
            "#5a3a34" to { x -> .072f + .014f * wob(x, 3.1f, 6f) + .03f * x },
            "#8c4630" to { x -> .13f + .016f * wob(x, 2.8f, 7f) + .04f * x },
            "#2a2226" to { x -> .17f + .012f * wob(x, 3.9f, 8f) + .02f * x },
            "#473033" to { x -> .2f + .016f * wob(x, 3.4f, 10f) + .02f * x },
            "#221b1e" to { x -> .25f + .01f * wob(x, 6f, 11f) },
        )
    c.save()
    c.rockPath()
    c.clip()
    for ((col, yb) in bands) {
        c.beginPath()
        c.moveTo(-.8f, .6f)
        var x = -.8f
        while (x <= .76f) {
            c.lineTo(x, maxOf(yb(x) + .003f * sin(x * 61), topAt(x) - .02f))
            x += .008f
        }
        c.lineTo(.76f, .6f)
        c.closePath()
        c.fillStyle = col
        c.fill()
    }
    // Thin bright seams where the layers meet.
    for ((_, yb) in bands.drop(3)) {
        c.beginPath()
        var x = -.8f
        while (x <= .76f) {
            val y = yb(x) + .003f * sin(x * 61)
            if (x <= -.8f) c.moveTo(x, y) else c.lineTo(x, y)
            x += .01f
        }
        c.strokeStyle = "rgba(255,222,186,.12)"
        c.lineWidth = .003f
        c.stroke()
    }
    // Erosion: short dark gullies cutting the softer layers, pale dust washing down from the
    // pumice.
    for (i in 0 until 46) {
        val x0 = (-.7 + R() * 1.36).toFloat()
        val y0 = (topAt(x0) + .08 + R() * .16).toFloat()
        val len = (.03 + R() * .08).toFloat()
        val dark = R() < .7
        c.beginPath()
        c.moveTo(x0, y0)
        val a = ((R() - .5) * .015).toFloat()
        val b = ((R() - .5) * .02).toFloat()
        val d = ((R() - .5) * .015).toFloat()
        c.bezierCurveTo(x0 + a, y0 + len * .4f, x0 + b, y0 + len * .7f, x0 + d, y0 + len)
        c.strokeStyle =
            if (dark) rgba("16,10,12", .18 + R() * .2) else rgba("240,220,190", .1 + R() * .12)
        c.lineWidth = (.003 + R() * .007).toFloat()
        c.stroke()
    }
    for (i in 0 until 26) {
        val x0 = (-.68 + R() * 1.3).toFloat()
        val y0 = topAt(x0) + .07f
        val len = (.03 + R() * .06).toFloat()
        val gg = c.createLinearGradient(0f, y0, 0f, y0 + len)
        gg.addColorStop(0f, "rgba(236,218,188,.35)")
        gg.addColorStop(1f, "rgba(236,218,188,0)")
        c.fillStyle = gg
        c.fillRect(x0, y0, (.006 + R() * .01).toFloat(), len)
    }
    // Buttresses: ribs of harder rock, lit on the sun side and shadowed on the other.
    for (i in 0 until 7) {
        val x0 = (-.62 + i * .2 + R() * .06).toFloat()
        val y0 = topAt(x0) + .09f
        val w = (.03 + R() * .03).toFloat()
        val g2 = c.createLinearGradient(x0 - w, 0f, x0 + w, 0f)
        g2.addColorStop(0f, "rgba(255,190,130,0)")
        g2.addColorStop(.35f, "rgba(255,190,130,.16)")
        g2.addColorStop(.55f, "rgba(20,24,50,.2)")
        g2.addColorStop(1f, "rgba(20,24,50,0)")
        c.fillStyle = g2
        c.beginPath()
        c.moveTo(x0 - w, y0)
        c.lineTo(x0 + w, y0)
        c.lineTo(x0 + w * 1.5f, .3f)
        c.lineTo(x0 - w * 1.4f, .3f)
        c.closePath()
        c.fill()
    }
    for (i in 0 until 500) {
        val x = (-.74 + R() * 1.44).toFloat()
        val y = (topAt(x) + .07 + R() * .3).toFloat()
        val l = R() < .4
        c.fillStyle =
            if (l) rgba("255,232,204", .06 + R() * .12) else rgba("14,8,10", .08 + R() * .16)
        c.fillRect(x, y, (.003 + R() * .006).toFloat(), (.003 + R() * .004).toFloat())
    }
    // Scree fanning out at the waterline.
    for (i in 0 until 160) {
        val x = (-.72 + R() * 1.42).toFloat()
        val y = (.25 + R() * .04).toFloat()
        c.fillStyle = rgba(if (R() < .5) "120,96,88" else "40,30,32", .4 + R() * .4)
        c.beginPath()
        c.arc(x, y, (.003 + R() * .005).toFloat(), 0f, TAU)
        c.fill()
    }
    // Late afternoon: warm sun raking in from the left, a cool shadow side to the right, darker
    // towards the sea.
    var g = c.createLinearGradient(-.78f, 0f, .72f, 0f)
    g.addColorStop(0f, rgba(L.warm, .42))
    g.addColorStop(.42f, rgba(L.warm, .1))
    g.addColorStop(.6f, "rgba(30,44,96,0)")
    g.addColorStop(1f, "rgba(30,44,96,.5)")
    c.fillStyle = g
    c.fillRect(-1f, -1f, 2f, 2f)
    g = c.createLinearGradient(0f, -.05f, 0f, .3f)
    g.addColorStop(0f, "rgba(10,8,20,0)")
    g.addColorStop(1f, "rgba(10,8,20,.4)")
    c.fillStyle = g
    c.fillRect(-1f, -1f, 2f, 2f)
    c.restore()
    // Sunlit lip along the rim.
    c.beginPath()
    c.moveTo(TOPP[1].first, TOPP[1].second)
    for (i in 2 until TOPP.size - 2) {
        val (x0, y0) = TOPP[i]
        val (x1, y1) = TOPP[i + 1]
        c.quadraticCurveTo(x0, y0, (x0 + x1) / 2, (y0 + y1) / 2)
    }
    c.strokeStyle = rgba(L.warm, .6)
    c.lineWidth = .006f
    c.stroke()

    // The village: whitewashed cubes cascading down the top of the face, sunlit fronts and cool
    // blue sides.
    fun front(y: Float, h: Float, tone: Float): Gradient2D {
        val gg = c.createLinearGradient(0f, y, 0f, y + h)
        var t0 = L.white[0]
        var t1 = L.white[1]
        if (tone > .9f) {
            t0 = "#f4cf98"
            t1 = "#dca86c"
        } else if (tone > .84f) {
            t0 = "#f3c6b6"
            t1 = "#d99d8e"
        } else if (tone > .8f) {
            t0 = "#cfe1ea"
            t1 = "#a9c3d2"
        }
        gg.addColorStop(0f, t0)
        gg.addColorStop(1f, t1)
        return gg
    }
    val dx = .016f
    val dy = -.01f
    fun cube(h: House) {
        val x = h.x
        val w = h.w
        val base = h.base
        val top = base - h.h
        // Shadow cast down-right onto the cliff (and the roofs below).
        c.fillStyle = "rgba(20,24,58,.34)"
        c.beginPath()
        c.moveTo(x + w, top)
        c.lineTo(x + w + .03f, top + .022f)
        c.lineTo(x + w + .03f, base + .03f)
        c.lineTo(x + .01f, base + .03f)
        c.lineTo(x, base)
        c.closePath()
        c.fill()
        c.fillStyle = L.side
        c.beginPath()
        c.moveTo(x + w, top)
        c.lineTo(x + w + dx, top + dy)
        c.lineTo(x + w + dx, base + dy)
        c.lineTo(x + w, base)
        c.fill()
        if (h.vault) {
            c.fillStyle = L.roof
            c.beginPath()
            c.moveTo(x, top + .01f)
            c.quadraticCurveTo(x + w / 2, top - .02f, x + w, top + .01f)
            c.lineTo(x + w + dx, top + .01f + dy)
            c.quadraticCurveTo(x + w / 2 + dx, top - .02f + dy, x + dx, top + .01f + dy)
            c.closePath()
            c.fill()
            c.fillStyle = front(top, h.h, h.tone)
            c.beginPath()
            c.moveTo(x, base)
            c.lineTo(x, top + .01f)
            c.quadraticCurveTo(x + w / 2, top - .02f, x + w, top + .01f)
            c.lineTo(x + w, base)
            c.closePath()
            c.fill()
        } else {
            c.fillStyle = L.roof
            c.beginPath()
            c.moveTo(x, top)
            c.lineTo(x + dx, top + dy)
            c.lineTo(x + w + dx, top + dy)
            c.lineTo(x + w, top)
            c.closePath()
            c.fill()
            c.fillStyle = front(top, h.h, h.tone)
            c.fillRect(x, top, w, h.h)
            c.fillStyle = "rgba(255,255,255,.8)"
            c.fillRect(x, top, w, .003f)
        }
        // Terrace wall in front, a blue door, a dark arched window.
        c.fillStyle = "rgba(255,246,232,.9)"
        c.fillRect(x - .004f, base - .006f, w + .008f, .006f)
        if (w > .062f) {
            c.fillStyle = if (h.win < .55f) "#2459b8" else "#2a8aa6"
            c.fillRect(x + w * .2f, base - .03f, .013f, .025f)
            c.fillStyle = "rgba(46,44,66,.8)"
            val wx = x + w * .62f
            val wy = top + h.h * .3f
            c.beginPath()
            c.moveTo(wx, wy + .016f)
            c.lineTo(wx, wy + .005f)
            c.arc(wx + .006f, wy + .005f, .006f, PIf, TAU)
            c.lineTo(wx + .012f, wy + .016f)
            c.fill()
        } else {
            c.fillStyle = "#2459b8"
            c.fillRect(x + w * .35f, base - .026f, .012f, .022f)
        }
        if (L.wet) {
            c.strokeStyle = "rgba(255,255,255,.65)"
            c.lineWidth = .003f
            c.beginPath()
            c.moveTo(x + .003f, top + .002f)
            c.lineTo(x + w - .003f, top + .002f)
            c.stroke()
        }
    }

    // Bell tower: an open white wall with two arches and bronze bells.
    val bb = topAt(BTx) + .014f
    val bw = .066f
    val bh = .15f
    val bt = bb - bh
    c.fillStyle = "rgba(20,24,58,.3)"
    c.beginPath()
    c.moveTo(BTx + bw / 2, bt + .02f)
    c.lineTo(BTx + bw / 2 + .04f, bt + .05f)
    c.lineTo(BTx + bw / 2 + .04f, bb + .02f)
    c.lineTo(BTx + bw / 2, bb)
    c.fill()
    c.fillStyle = front(bt, bh, 0f)
    c.beginPath()
    c.moveTo(BTx - bw / 2, bb)
    c.lineTo(BTx - bw / 2, bt + .028f)
    c.quadraticCurveTo(BTx - bw / 2, bt, BTx, bt - .012f)
    c.quadraticCurveTo(BTx + bw / 2, bt, BTx + bw / 2, bt + .028f)
    c.lineTo(BTx + bw / 2, bb)
    c.fill()
    c.fillStyle = L.side
    c.fillRect(BTx + bw / 2 - .006f, bt + .028f, .006f, bh - .028f)
    for ((ax, ay, r) in
        listOf(Triple(-.015f, .062f, .01f), Triple(.015f, .062f, .01f), Triple(0f, .028f, .008f))) {
        c.fillStyle = "#5a7aa8"
        c.beginPath()
        c.moveTo(BTx + ax - r, bt + ay + .018f)
        c.lineTo(BTx + ax - r, bt + ay)
        c.arc(BTx + ax, bt + ay, r, PIf, TAU)
        c.lineTo(BTx + ax + r, bt + ay + .018f)
        c.fill()
        c.fillStyle = "#b8862e"
        c.beginPath()
        c.arc(BTx + ax, bt + ay + .006f, r * .55f, 0f, TAU)
        c.fill()
    }
    c.strokeStyle = L.white[0]
    c.lineWidth = .005f
    c.lineCap = "round"
    c.beginPath()
    c.moveTo(BTx, bt - .012f)
    c.lineTo(BTx, bt - .048f)
    c.moveTo(BTx - .011f, bt - .036f)
    c.lineTo(BTx + .011f, bt - .036f)
    c.stroke()

    // The church: white body and drum, a crisp cobalt dome, a lantern and a cross.
    val cb = topAt(CHx) + .016f
    val cw = .13f
    val chh = .085f
    val ct = cb - chh
    c.fillStyle = "rgba(20,24,58,.32)"
    c.beginPath()
    c.moveTo(CHx + cw / 2, ct - .04f)
    c.lineTo(CHx + cw / 2 + .06f, ct)
    c.lineTo(CHx + cw / 2 + .06f, cb + .03f)
    c.lineTo(CHx + cw / 2, cb)
    c.fill()
    c.fillStyle = L.side
    c.beginPath()
    c.moveTo(CHx + cw / 2, ct)
    c.lineTo(CHx + cw / 2 + dx, ct + dy)
    c.lineTo(CHx + cw / 2 + dx, cb + dy)
    c.lineTo(CHx + cw / 2, cb)
    c.fill()
    c.fillStyle = front(ct, chh, 0f)
    c.fillRect(CHx - cw / 2, ct, cw, chh)
    c.fillStyle = "#2459b8"
    c.beginPath()
    c.moveTo(CHx - .013f, cb)
    c.lineTo(CHx - .013f, cb - .032f)
    c.arc(CHx, cb - .032f, .013f, PIf, TAU)
    c.lineTo(CHx + .013f, cb)
    c.fill()
    val dr = .058f
    val dcx = CHx + .004f
    val drumH = .022f
    val dcy = ct - drumH
    g = c.createLinearGradient(dcx - .05f, 0f, dcx + .05f, 0f)
    g.addColorStop(0f, L.white[0])
    g.addColorStop(.7f, L.white[1])
    g.addColorStop(1f, L.side)
    c.fillStyle = g
    c.fillRect(dcx - .05f, dcy, .1f, drumH)
    c.fillStyle = "rgba(46,44,66,.7)"
    for (ox in listOf(-.028f, 0f, .028f)) c.fillRect(dcx + ox - .004f, dcy + .005f, .008f, .012f)
    c.fillStyle = L.white[0]
    c.fillRect(dcx - .062f, dcy - .006f, .124f, .008f)
    g =
        c.createRadialGradient(
            dcx - .026f,
            dcy - .046f,
            .003f,
            dcx + .006f,
            dcy - .012f,
            dr * 1.12f,
        )
    g.addColorStop(0f, L.dome[0])
    g.addColorStop(.38f, L.dome[1])
    g.addColorStop(1f, L.dome[2])
    c.fillStyle = g
    c.beginPath()
    c.arc(dcx, dcy - .006f, dr, PIf, TAU)
    c.closePath()
    c.fill()
    c.strokeStyle = "rgba(8,20,60,.5)"
    c.lineWidth = .003f
    c.beginPath()
    c.arc(dcx, dcy - .006f, dr, PIf, TAU)
    c.stroke()
    c.strokeStyle = rgba(L.warm, .85)
    c.lineWidth = .005f
    c.beginPath()
    c.arc(dcx, dcy - .006f, dr - .007f, PIf * 1.06f, PIf * 1.36f)
    c.stroke()
    if (L.wet) {
        c.strokeStyle = "rgba(255,255,255,.8)"
        c.lineWidth = .004f
        c.beginPath()
        c.arc(dcx, dcy - .006f, dr - .014f, PIf * 1.1f, PIf * 1.3f)
        c.stroke()
    }
    c.fillStyle = L.white[0]
    c.fillRect(dcx - .008f, dcy - .006f - dr - .016f, .016f, .018f)
    c.strokeStyle = L.white[0]
    c.lineWidth = .005f
    c.beginPath()
    c.moveTo(dcx, dcy - dr - .022f)
    c.lineTo(dcx, dcy - dr - .06f)
    c.moveTo(dcx - .012f, dcy - dr - .048f)
    c.lineTo(dcx + .012f, dcy - dr - .048f)
    c.stroke()

    for (h in houses) cube(h)

    // The windmill tower (its sails turn every frame).
    val wt = windBase - .13f
    val ww = .066f
    c.fillStyle = "rgba(20,24,58,.3)"
    c.beginPath()
    c.moveTo(WMx + ww / 2, wt)
    c.lineTo(WMx + ww / 2 + .04f, wt + .03f)
    c.lineTo(WMx + ww / 2 + .04f, windBase + .02f)
    c.lineTo(WMx + ww / 2, windBase)
    c.fill()
    g = c.createLinearGradient(WMx - ww / 2, 0f, WMx + ww / 2, 0f)
    g.addColorStop(0f, L.white[0])
    g.addColorStop(.55f, L.white[1])
    g.addColorStop(1f, L.side)
    c.fillStyle = g
    c.beginPath()
    c.moveTo(WMx - ww / 2 - .004f, windBase)
    c.lineTo(WMx - ww / 2, wt)
    c.lineTo(WMx + ww / 2, wt)
    c.lineTo(WMx + ww / 2 + .004f, windBase)
    c.fill()
    c.fillStyle = "#2459b8"
    c.beginPath()
    c.moveTo(WMx - .009f, windBase)
    c.lineTo(WMx - .009f, windBase - .028f)
    c.arc(WMx, windBase - .028f, .009f, PIf, TAU)
    c.lineTo(WMx + .009f, windBase)
    c.fill()
    g = c.createLinearGradient(WMx - .042f, 0f, WMx + .042f, 0f)
    g.addColorStop(0f, "#a87650")
    g.addColorStop(.5f, "#6e4a32")
    g.addColorStop(1f, "#3a261a")
    c.fillStyle = g
    c.beginPath()
    c.moveTo(WMx - .042f, wt + .004f)
    c.quadraticCurveTo(WMx - .018f, wt - .046f, WMx, wt - .05f)
    c.quadraticCurveTo(WMx + .018f, wt - .046f, WMx + .042f, wt + .004f)
    c.closePath()
    c.fill()

    if (L.tint != null) {
        c.globalCompositeOperation = "source-atop"
        c.fillStyle = L.tint
        c.fillRect(-1f, -1f, 2f, 2f)
        c.globalCompositeOperation = "source-over"
    }
}

/** A soft cumulus, painted once. */
internal val cloudSprite: Canvas2D by lazy {
    val x = Canvas2D.create(256, 128)
    val puffs =
        listOf(
            Triple(70f, 82f, 34f),
            Triple(110f, 66f, 44f),
            Triple(156f, 72f, 38f),
            Triple(192f, 86f, 28f),
            Triple(128f, 90f, 36f),
            Triple(88f, 92f, 26f),
            Triple(170f, 94f, 26f),
        )
    for ((px, py, r) in puffs) {
        val g = x.createRadialGradient(px - r * .2f, py - r * .35f, r * .1f, px, py, r)
        g.addColorStop(0f, "rgba(255,255,255,1)")
        g.addColorStop(.7f, "rgba(250,251,253,.95)")
        g.addColorStop(1f, "rgba(240,244,250,0)")
        x.fillStyle = g
        x.beginPath()
        x.arc(px, py, r, 0f, TAU)
        x.fill()
    }
    x.globalCompositeOperation = "source-atop"
    val g = x.createLinearGradient(0f, 40f, 0f, 120f)
    g.addColorStop(0f, "rgba(120,135,160,0)")
    g.addColorStop(1f, "rgba(110,125,150,.45)")
    x.fillStyle = g
    x.fillRect(0f, 0f, 256f, 128f)
    x
}

internal fun paintBackdrop(c: Canvas2D, w: GlobeScene) {
    val L = LIT.getValue(w)
    c.unitSpace()
    val hz = .14f
    var g = c.createLinearGradient(0f, -1f, 0f, hz)
    for ((o, col) in L.sky) g.addColorStop(o, col)
    c.fillStyle = g
    c.fillRect(-1.2f, -1.2f, 2.4f, hz + 1.21f)
    when (w) {
        GlobeScene.Sunny -> {
            g = c.createRadialGradient(-.56f, -.36f, 0f, -.56f, -.36f, .9f)
            g.addColorStop(0f, "rgba(255,236,190,.8)")
            g.addColorStop(.18f, "rgba(255,206,140,.38)")
            g.addColorStop(1f, "rgba(255,226,170,0)")
            c.fillStyle = g
            c.fillRect(-1.2f, -1.2f, 2.4f, hz + 1.2f)
            g = c.createRadialGradient(-.565f, -.37f, 0f, -.56f, -.36f, .085f)
            g.addColorStop(0f, "#fffef6")
            g.addColorStop(.7f, "#fff3c8")
            g.addColorStop(1f, "rgba(255,236,170,0)")
            c.fillStyle = g
            c.beginPath()
            c.arc(-.56f, -.36f, .085f, 0f, TAU)
            c.fill()
            c.globalAlpha = .55f
            c.drawImage(cloudSprite, .28f, -.42f, .44f, .22f)
            c.globalAlpha = 1f
        }
        GlobeScene.Windy -> {
            c.filter = "blur(2px)"
            val rnd = mulberry(4)
            fun R() = rnd()
            for (i in 0 until 7) {
                c.fillStyle = rgba("255,255,255", .22 + R() * .25)
                c.beginPath()
                val ex = (-1 + R() * 2).toFloat()
                val ey = (-.85 + R() * .7).toFloat()
                val erx = (.25 + R() * .3).toFloat()
                val ery = (.012 + R() * .014).toFloat()
                c.ellipse(ex, ey, erx, ery, -.06f, 0f, TAU)
                c.fill()
            }
            c.filter = "none"
        }
        GlobeScene.Shower -> {
            // A sun break low on the right, and a rainbow over the village.
            g = c.createRadialGradient(.62f, .02f, 0f, .62f, .02f, .7f)
            g.addColorStop(0f, "rgba(255,240,214,.6)")
            g.addColorStop(1f, "rgba(255,240,214,0)")
            c.fillStyle = g
            c.fillRect(-1.2f, -1.2f, 2.4f, hz + 1.2f)
            c.save()
            c.beginPath()
            c.rect(-1.2f, -1.2f, 2.4f, hz + 1.2f)
            c.clip()
            c.filter = "blur(1.6px)"
            val bands =
                listOf(
                    "255,86,72",
                    "255,160,70",
                    "255,226,92",
                    "110,206,110",
                    "80,150,235",
                    "120,96,220",
                    "170,96,210",
                )
            val rr = .86f
            bands.forEachIndexed { i, b ->
                c.strokeStyle = rgba(b, .34)
                c.lineWidth = .026f
                c.beginPath()
                c.arc(0f, .5f, rr - i * .022f, PIf * 1.04f, PIf * 1.96f)
                c.stroke()
            }
            c.filter = "none"
            c.restore()
            g = c.createLinearGradient(0f, -1f, 0f, -.3f)
            g.addColorStop(0f, "rgba(60,70,90,.55)")
            g.addColorStop(1f, "rgba(60,70,90,0)")
            c.fillStyle = g
            c.fillRect(-1.2f, -1.2f, 2.4f, .9f)
        }
    }
    // Far islands on the horizon: Thirasia and the volcano.
    c.fillStyle = L.isle
    c.beginPath()
    c.moveTo(.26f, hz + .002f)
    c.quadraticCurveTo(.4f, hz - .06f, .62f, hz - .07f)
    c.quadraticCurveTo(.86f, hz - .08f, 1.1f, hz - .05f)
    c.lineTo(1.1f, hz + .002f)
    c.fill()
    c.beginPath()
    c.moveTo(-.5f, hz + .002f)
    c.quadraticCurveTo(-.36f, hz - .03f, -.2f, hz + .002f)
    c.fill()
    // The caldera sea.
    g = c.createLinearGradient(0f, hz, 0f, .3f)
    g.addColorStop(0f, L.sea[0])
    g.addColorStop(1f, L.sea[1])
    c.fillStyle = g
    c.fillRect(-1.2f, hz, 2.4f, 1.2f)
    c.fillStyle = "rgba(255,255,255,.35)"
    c.fillRect(-1.2f, hz - .002f, 2.4f, .005f)
    if (w == GlobeScene.Sunny) {
        val rnd = mulberry(8)
        fun R() = rnd()
        for (i in 0 until 80) {
            val y = (hz + .01 + R().pow(1.5) * .22).toFloat()
            val spread = .04f + (y - hz) * .7f
            c.fillStyle = rgba("255,246,214", .25 + R() * .45)
            val x = (-.56 + (R() - .5) * spread * 2).toFloat()
            c.fillRect(x, y, (.012 + R() * .03).toFloat(), .004f)
        }
    } else if (w == GlobeScene.Windy) {
        val rnd = mulberry(12)
        fun R() = rnd()
        for (i in 0 until 40) {
            c.fillStyle = rgba("255,255,255", .3 + R() * .4)
            val x = (-1 + R() * 2).toFloat()
            val y = (hz + .02 + R() * .2).toFloat()
            c.fillRect(x, y, (.016 + R() * .02).toFloat(), .004f)
        }
    }
}

internal fun paintPlinth(c: Canvas2D, w: GlobeScene, il: Canvas2D?) {
    val L = LIT.getValue(w)
    c.unitSpace()
    val g = c.createLinearGradient(0f, .28f, 0f, .8f)
    g.addColorStop(0f, L.plinth[0])
    g.addColorStop(1f, L.plinth[1])
    c.fillStyle = g
    c.fillRect(-1.2f, .28f, 2.4f, 1f)
    if (il != null) {
        val yf = (FLOOR + 1) / 2 * T
        c.save()
        c.setTransform(1f, 0f, 0f, 1f, 0f, 0f)
        c.beginPath()
        c.rect(0f, yf, T.toFloat(), T * .3f)
        c.clip()
        c.setTransform(1f, 0f, 0f, -.62f, 0f, yf * 1.62f)
        c.filter = "blur(1.4px)"
        c.globalAlpha = .34f
        c.drawImage(il, 0f, 0f)
        c.globalAlpha = 1f
        c.filter = "none"
        c.restore()
        c.unitSpace()
        val fade = c.createLinearGradient(0f, .29f, 0f, .52f)
        fade.addColorStop(0f, "rgba(0,0,0,0)")
        fade.addColorStop(1f, L.plinth[1])
        c.fillStyle = fade
        c.fillRect(-1.2f, .29f, 2.4f, .3f)
        val rnd = mulberry(41)
        fun R() = rnd()
        for (i in 0 until 70) {
            val y = (.295 + R().pow(1.3) * .26).toFloat()
            c.fillStyle = rgba("255,255,255", .06 + R() * .12)
            val x = (-.9 + R() * 1.8).toFloat()
            c.fillRect(x, y, (.04 + R() * .14).toFloat(), (.003 + R() * .003).toFloat())
        }
    }
    c.strokeStyle = "rgba(255,255,255,.55)"
    c.lineWidth = .006f
    c.beginPath()
    var x = -1.1f
    while (x <= 1.1f) {
        val y = .282f + .003f * sin(x * 30)
        if (x <= -1.1f) c.moveTo(x, y) else c.lineTo(x, y)
        x += .02f
    }
    c.stroke()
    c.strokeStyle = "rgba(255,255,255,.7)"
    c.lineWidth = .008f
    c.beginPath()
    c.moveTo(-.74f, .292f)
    c.quadraticCurveTo(0f, .31f, .69f, .29f)
    c.stroke()
    val sg = c.createLinearGradient(-.6f, 0f, .4f, 0f)
    sg.addColorStop(0f, "rgba(255,255,255,0)")
    sg.addColorStop(.4f, "rgba(255,255,255,.14)")
    sg.addColorStop(1f, "rgba(255,255,255,0)")
    c.fillStyle = sg
    c.fillRect(-1f, .32f, 2f, .12f)
}

/**
 * The three weathers' interiors, and where glitter rests, painted once (the prototype's
 * paintScenes).
 */
internal class SantoriniScenes
private constructor(
    val scenes: Map<GlobeScene, ImageBitmap>,
    /** The topmost solid surface across the globe, sampled at 160 columns: the prototype's TOP. */
    val top: FloatArray,
) {
    /** Where glitter can come to rest at [x]: the prototype's floorAt. */
    fun floorAt(x: Float): Float =
        top[floor((x + 1) / 2 * top.size).toInt().coerceIn(0, top.size - 1)]

    internal companion object {
        /**
         * How many times the scenes have been painted, for tests that check they're painted once.
         */
        var paints = 0
            private set

        /** The scenes, painted the first time they're asked for and kept: they never change. */
        val shared: SantoriniScenes by lazy { paint() }

        fun paint(): SantoriniScenes {
            paints++
            val scenes = HashMap<GlobeScene, ImageBitmap>()
            var sunIsland: Canvas2D? = null
            for (w in listOf(GlobeScene.Sunny, GlobeScene.Windy, GlobeScene.Shower)) {
                val il = Canvas2D.create(T, T)
                paintIsland(il, LIT.getValue(w))
                if (w == GlobeScene.Sunny) sunIsland = il
                val c = Canvas2D.create(T, T)
                paintBackdrop(c, w)
                c.setTransform(1f, 0f, 0f, 1f, 0f, 0f)
                c.drawImage(il, 0f, 0f)
                if (w == GlobeScene.Shower) {
                    c.unitSpace()
                    c.globalAlpha = .96f
                    c.drawImage(cloudSprite, -.2f, -.66f, .56f, .28f)
                    c.globalAlpha = 1f
                }
                paintPlinth(c, w, il)
                scenes[w] = c.bitmap
            }
            // Where glitter can come to rest: the roofs, the dome and the cliff, scanned from the
            // figurine's alpha.
            val px = checkNotNull(sunIsland).bitmap.toPixelMap()
            val top = FloatArray(160) { FLOOR }
            for (i in top.indices) {
                val x = -1 + (i + .5f) / top.size * 2
                val col = floor((x + 1) / 2 * T).toInt()
                var y = FLOOR
                for (py in 0 until (T * .7f).toInt()) {
                    if (px[col, py].alpha * 255 > 150) {
                        y = min(FLOOR, py.toFloat() / T * 2 - 1)
                        break
                    }
                }
                top[i] = y
            }
            return SantoriniScenes(scenes, top)
        }
    }
}

/** A soft warm glow with a faint cross, for the glints. */
internal val glowSprite: Canvas2D by lazy {
    val x = Canvas2D.create(48, 48)
    val g = x.createRadialGradient(24f, 24f, 0f, 24f, 24f, 24f)
    g.addColorStop(0f, "rgba(255,250,226,1)")
    g.addColorStop(.18f, "rgba(255,232,170,.55)")
    g.addColorStop(1f, "rgba(255,214,140,0)")
    x.fillStyle = g
    x.fillRect(0f, 0f, 48f, 48f)
    x.strokeStyle = "rgba(255,248,226,.32)"
    x.lineWidth = 1f
    x.beginPath()
    x.moveTo(24f, 4f)
    x.lineTo(24f, 44f)
    x.moveTo(4f, 24f)
    x.lineTo(44f, 24f)
    x.stroke()
    x
}

/** An out-of-focus warm disc, for near flakes. */
internal val bokehSprite: Canvas2D by lazy {
    val x = Canvas2D.create(32, 32)
    val g = x.createRadialGradient(16f, 16f, 0f, 16f, 16f, 16f)
    g.addColorStop(0f, "rgba(255,236,190,.55)")
    g.addColorStop(.6f, "rgba(255,214,140,.3)")
    g.addColorStop(1f, "rgba(255,214,140,0)")
    x.fillStyle = g
    x.fillRect(0f, 0f, 32f, 32f)
    x
}
