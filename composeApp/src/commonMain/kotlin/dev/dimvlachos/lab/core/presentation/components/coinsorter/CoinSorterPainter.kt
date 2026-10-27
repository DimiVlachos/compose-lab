package dev.dimvlachos.lab.core.presentation.components.coinsorter

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.cssColor
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.components.perspective.Homography
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the coin sorter as the prototype does: the tray's two canvases ([drawBack] behind the coins
 * and [drawFront], the acrylic, in front of them), ported call by call from its `drawTray()`; the
 * coins, the prototype's `.coin` CSS under its `perspective(260px) rotateX`; and the outlines of
 * the empty lines mid-sort. Everything is laid out in dp and drawn at full density.
 */
internal class CoinSorterPainter(private val density: Float) {
    // ---------- The tray ----------

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

    private fun lin(
        c: Canvas2D,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        vararg stops: Pair<Float, String>,
    ) =
        c.createLinearGradient(x0, y0, x1, y1).apply {
            stops.forEach { addColorStop(it.first, it.second) }
        }

    private fun funnelPath(c: Canvas2D, s: CoinSorterState) {
        val cx = s.hopCx
        c.beginPath()
        c.moveTo(cx - s.hopTw / 2, s.hopTop)
        c.lineTo(cx + s.hopTw / 2, s.hopTop)
        c.lineTo(cx + s.hopOw / 2, s.hopOut)
        c.lineTo(cx - s.hopOw / 2, s.hopOut)
        c.closePath()
    }

    /** The plate, the channel, the tube insides, the rail, the end stop and the funnel's inside. */
    fun drawBack(c: Canvas2D, s: CoinSorterState, dark: Boolean) {
        c.scale(density, density)
        val k = s.k
        val x0 = s.trayX0
        val x1 = s.trayX1
        val top = s.trayTop
        val h = s.trayH
        val w = x1 - x0
        // Shadow, then the chrome plate with its bands of reflected room. The prototype's shadow
        // is in its canvas's pixels at a density of 2.
        c.save()
        c.shadowColor = if (dark) "rgba(0,0,0,.6)" else "rgba(40,50,60,.3)"
        c.shadowBlur = 14 * k * density / 2
        c.shadowOffsetY = 5 * k * density / 2
        rr(c, x0, top, w, h, 14 * k)
        c.fillStyle = "#888"
        c.fill()
        c.restore()
        rr(c, x0, top, w, h, 14 * k)
        c.fillStyle =
            if (dark)
                lin(
                    c,
                    0f,
                    top,
                    0f,
                    top + h,
                    0f to "#9aa1a8",
                    .16f to "#6c737a",
                    .5f to "#454b51",
                    .53f to "#5a6168",
                    .86f to "#6f767d",
                    1f to "#3a3f44",
                )
            else
                lin(
                    c,
                    0f,
                    top,
                    0f,
                    top + h,
                    0f to "#f6f8f9",
                    .16f to "#dde1e5",
                    .5f to "#b7bec5",
                    .53f to "#cdd3d8",
                    .86f to "#eaedf0",
                    1f to "#a7aeb5",
                )
        c.fill()
        c.save()
        rr(c, x0, top, w, h, 14 * k)
        c.clip()
        c.lineWidth = .6f
        for (b in Hairlines) {
            val (u, v, l, a) = b
            c.strokeStyle = brushInk(u > .5f, a * (if (dark) .25f else .35f))
            c.beginPath()
            c.moveTo(x0 + u * w * .8f, top + v * h)
            c.lineTo(x0 + u * w * .8f + (.1f + l * .3f) * w, top + v * h)
            c.stroke()
        }
        c.restore()
        c.strokeStyle = "rgba(255,255,255,.7)"
        c.lineWidth = 1f
        rr(c, x0 + .5f, top + .5f, w - 1, h - 1, 14 * k)
        c.stroke()
        // The channel the coins roll in: a shallow groove above the rail.
        val ca = s.hopCx - 22 * k
        val cb = x1 - 9 * k
        c.beginPath()
        c.moveTo(ca, s.railY(ca) - 38 * k)
        c.lineTo(cb, s.railY(cb) - 38 * k)
        c.lineTo(cb, s.railY(cb))
        c.lineTo(ca, s.railY(ca))
        c.closePath()
        c.fillStyle = if (dark) "rgba(0,0,0,.22)" else "rgba(40,52,64,.1)"
        c.fill()
        c.strokeStyle = if (dark) "rgba(0,0,0,.4)" else "rgba(40,52,64,.22)"
        c.beginPath()
        c.moveTo(ca, s.railY(ca) - 38 * k)
        c.lineTo(cb, s.railY(cb) - 38 * k)
        c.stroke()
        c.strokeStyle = "rgba(255,255,255,.55)"
        c.beginPath()
        c.moveTo(ca, s.railY(ca) - 37 * k)
        c.lineTo(cb, s.railY(cb) - 37 * k)
        c.stroke()
        // Tube insides.
        for (i in 0 until s.n) {
            val tw = s.slotW[i] + 4 * k
            val cx = s.slotX[i]
            val yT = s.railY(cx)
            rr(c, cx - tw / 2, yT, tw, s.tubeBottom - yT + 3 * k, 3 * k)
            c.fillStyle =
                if (dark)
                    lin(
                        c,
                        cx - tw / 2,
                        0f,
                        cx + tw / 2,
                        0f,
                        0f to "#16191c",
                        .5f to "#2b3035",
                        1f to "#121417",
                    )
                else
                    lin(
                        c,
                        cx - tw / 2,
                        0f,
                        cx + tw / 2,
                        0f,
                        0f to "#3c434a",
                        .5f to "#626b74",
                        1f to "#30363c",
                    )
            c.fill()
        }
        // The rail, broken by the slots; a chamfered dark edge where each slot is cut.
        fun seg(a: Float, b: Float) {
            c.lineWidth = 4.6f * k
            c.lineCap = "butt"
            c.strokeStyle =
                lin(
                    c,
                    0f,
                    s.railY(a),
                    0f,
                    s.railY(a) + 4.6f * k,
                    0f to "#ffffff",
                    .35f to "#c9cfd5",
                    1f to "#6a727a",
                )
            c.beginPath()
            c.moveTo(a, s.railY(a) + 2.3f * k)
            c.lineTo(b, s.railY(b) + 2.3f * k)
            c.stroke()
            c.fillStyle = "rgba(20,24,28,.55)"
            c.fillRect(b - .8f, s.railY(b), 1.6f, 4.6f * k)
            c.fillRect(a - .8f, s.railY(a), 1.6f, 4.6f * k)
        }
        var a = ca
        for (i in 0 until s.n) {
            seg(a, s.slotX[i] - s.slotW[i] / 2)
            a = s.slotX[i] + s.slotW[i] / 2
        }
        seg(a, cb)
        // End stop.
        rr(c, cb - 2 * k, s.railY(cb) - 14 * k, 5 * k, 18 * k, 2 * k)
        c.fillStyle =
            lin(
                c,
                cb - 2 * k,
                0f,
                cb + 3 * k,
                0f,
                0f to "#7b838b",
                .4f to "#f5f7f8",
                1f to "#757d85",
            )
        c.fill()
        // Funnel inside and its throat down to the rail.
        funnelPath(c, s)
        c.fillStyle =
            if (dark) lin(c, 0f, s.hopTop, 0f, s.hopOut, 0f to "#1d2125", 1f to "#2e3439")
            else lin(c, 0f, s.hopTop, 0f, s.hopOut, 0f to "#4a525a", 1f to "#6b747d")
        c.fill()
        c.fillStyle = if (dark) "rgba(0,0,0,.35)" else "rgba(40,52,64,.25)"
        c.fillRect(s.hopCx - s.hopOw / 2, s.hopOut, s.hopOw, s.railY(s.hopCx) - s.hopOut)
    }

    /** The acrylic funnel and tube fronts, over the coins. */
    fun drawFront(f: Canvas2D, s: CoinSorterState, dark: Boolean) {
        f.scale(density, density)
        val k = s.k
        val glass = if (dark) .07f else .16f
        funnelPath(f, s)
        f.fillStyle = Color.White.copy(alpha = glass)
        f.fill()
        f.strokeStyle = "rgba(255,255,255,.6)"
        f.lineWidth = 1f
        f.stroke()
        f.save()
        funnelPath(f, s)
        f.clip()
        val fl = s.hopCx - s.hopTw / 2
        f.fillStyle =
            lin(
                f,
                fl,
                0f,
                fl + 14 * k,
                0f,
                0f to "rgba(255,255,255,0)",
                .5f to "rgba(255,255,255,.45)",
                1f to "rgba(255,255,255,0)",
            )
        f.fillRect(fl + 4 * k, s.hopTop, 12 * k, s.hopOut - s.hopTop)
        f.restore()
        rr(f, fl - 2 * k, s.hopTop - 3 * k, s.hopTw + 4 * k, 5 * k, 2.5f * k)
        f.fillStyle =
            lin(
                f,
                0f,
                s.hopTop - 3 * k,
                0f,
                s.hopTop + 2 * k,
                0f to "#ffffff",
                .5f to "#bfc6cc",
                1f to "#6f777f",
            )
        f.fill()
        // The throat's left wall: the right side is open, so the coin rolls out along the rail.
        val tl = s.hopCx - s.hopOw / 2
        rr(f, tl - 3 * k, s.hopOut - 1, 3.4f * k, s.railY(s.hopCx) - s.hopOut + 3 * k, 1.5f * k)
        f.fillStyle =
            lin(f, tl - 3 * k, 0f, tl, 0f, 0f to "#727a82", .5f to "#eef1f3", 1f to "#8d959c")
        f.fill()
        for (i in 0 until s.n) {
            val tw = s.slotW[i] + 4 * k
            val cx = s.slotX[i]
            val yT = s.railY(cx) + 4.6f * k
            val x = cx - tw / 2
            val tb = s.tubeBottom
            f.fillStyle = Color.White.copy(alpha = glass * .7f)
            f.fillRect(x, yT, tw, tb - yT)
            f.fillStyle = "rgba(255,255,255,.32)"
            f.fillRect(x + tw * .16f, yT, max(1.2f, tw * .1f), tb - yT)
            f.strokeStyle = "rgba(255,255,255,.45)"
            f.lineWidth = .8f
            f.beginPath()
            f.rect(x + .4f, yT, tw - .8f, tb - yT)
            f.stroke()
            rr(f, x - 1.2f * k, tb, tw + 2.4f * k, 3.6f * k, 1.5f * k)
            f.fillStyle =
                lin(
                    f,
                    0f,
                    tb,
                    0f,
                    tb + 3.6f * k,
                    0f to "#ffffff",
                    .5f to "#b9c0c7",
                    1f to "#69717a",
                )
            f.fill()
        }
    }

    // The hairlines' inks, made once: light ones above the middle of the plate, dark below.
    private val inks = HashMap<Int, Color>()

    private fun brushInk(light: Boolean, a: Float): Color {
        val key = (a * 10000).toInt() * 2 + if (light) 1 else 0
        return inks.getOrPut(key) {
            if (light) Color.White.copy(alpha = a) else Color(40, 50, 60).copy(alpha = a)
        }
    }

    // ---------- The wells ----------

    private val dash = PathEffect.dashPathEffect(floatArrayOf(3f * density, 2f * density))
    private val wellStroke = Stroke(1.5f * density, pathEffect = dash)

    /** A faint dashed outline on each line of the list while no row is on it. */
    fun DrawScope.drawWells(s: CoinSorterState, line: Color) {
        val inset = .75f * density
        for (j in 0 until s.n) {
            val a = s.wellA[j]
            if (a <= 0f) continue
            drawRoundRect(
                line,
                Offset(s.rowX() * density + inset, s.rowY(j) * density + inset),
                Size(s.rowW() * density - 2 * inset, s.rowH * density - 2 * inset),
                CornerRadius(14 * density - inset),
                wellStroke,
                alpha = cssEase(a),
            )
        }
    }

    // ---------- The coins ----------

    private class CoinLook(
        var d: Float = -1f,
        var metal: Metal = Metal.Nickel,
        var face: String = "",
        var cents: String = "",
        var size: Float = 0f,
        var disc: Brush? = null,
        var sheenA: Brush? = null,
        var sheenB: Brush? = null,
        var bands: Brush? = null,
        var reeds: Brush? = null,
        var faceText: TextLayoutResult? = null,
        var centsText: TextLayoutResult? = null,
    )

    private val looks = HashMap<Int, CoinLook>()
    private val shadow = Paint().apply { color = Color.Black.copy(alpha = .35f) }
    private var shadowBlurred = false
    private val h = FloatArray(9)
    private val m = Matrix()
    private val textDensity = Density(density, 1f)

    /**
     * The coins: those in flight over the tray when [fly], every other one when not, as the
     * prototype moves a coin between its `#fly` and `#coins` layers.
     */
    fun DrawScope.drawCoins(s: CoinSorterState, measurer: TextMeasurer, fly: Boolean) {
        for (r in s.rows) {
            if (r.asRow || r.d <= 0f) continue
            if (s.flying(r) != fly) continue
            drawCoin(r, s.items[r.index].metal, measurer)
        }
    }

    private fun DrawScope.drawCoin(r: CoinRow, metal: Metal, measurer: TextMeasurer) {
        val look = looks.getOrPut(r.index) { CoinLook() }
        prepare(look, r, metal, measurer)
        val dp = density
        val d = r.d * dp
        val rad = d / 2
        val cx = r.x * dp
        val cy = r.y * dp
        // translate(x - r, y - r) perspective(260px) rotateX(ax), about the coin's centre.
        val a = min(r.ax, 86f) * PI.toFloat() / 180
        val ca = cos(a)
        val sa = sin(a)
        Homography.of(
            cx - rad,
            cy - rad * ca,
            rad * sa,
            1f,
            0f,
            0f,
            0f,
            ca,
            -sa,
            cx,
            cy,
            260 * dp,
            h,
        )
        Homography.toMatrix(h, m)
        withTransform({ transform(m) }) {
            val c = Offset(rad, rad)
            // The drop shadow: 0 1.5 2.5 at 35 %.
            if (!shadowBlurred) {
                shadow.setBlur(1.25f * dp)
                shadowBlurred = true
            }
            drawIntoCanvas { it.drawCircle(Offset(rad, rad + 1.5f * dp), rad, shadow) }
            drawCircle(look.disc!!, rad, c)
            // The inset rings, the last of the box-shadows first.
            drawCircle(RingDark, rad - 1.8f * dp, c, style = Stroke(3.6f * dp))
            drawCircle(RingLight, rad - 1.3f * dp, c, style = Stroke(2.6f * dp))
            drawCircle(RingEdge, rad - .6f * dp, c, style = Stroke(1.2f * dp))
            // The face, upright, with its 0.8 px text shadow.
            val ink = Inks[metal.ordinal]
            val lift = Lifts[metal.ordinal]
            val ft = look.faceText!!
            val ct = look.centsText
            val total = ft.size.height + (ct?.size?.height ?: 0)
            val fy = rad - total / 2f
            val fx = rad - ft.size.width / 2f
            drawText(ft, lift, Offset(fx, fy + .8f * dp))
            drawText(ft, ink, Offset(fx, fy))
            if (ct != null) {
                val x = rad - ct.size.width / 2f
                val y = fy + ft.size.height
                drawText(ct, lift, Offset(x, y + .8f * dp), alpha = .85f)
                drawText(ct, ink, Offset(x, y), alpha = .85f)
            }
            // The milled edge turns with the roll; the sheen stays still.
            rotate(r.ang * 180 / PI.toFloat(), c) {
                val ring = Stroke(2.4f * dp)
                drawCircle(look.bands!!, rad - 1.2f * dp, c, style = ring)
                drawCircle(look.reeds!!, rad - 1.2f * dp, c, style = ring)
            }
            drawCircle(look.sheenB!!, rad, c)
            drawCircle(look.sheenA!!, rad, c)
        }
    }

    // A coin's brushes and face, made again only when its size, metal or face changes.
    private fun prepare(look: CoinLook, r: CoinRow, metal: Metal, measurer: TextMeasurer) {
        val d = r.d * density
        if (look.d != d || look.metal != metal) {
            look.d = d
            look.metal = metal
            val stops = DiscStops[metal.ordinal]
            // radial-gradient(circle at 38% 34%): a circle out to the farthest corner.
            look.disc =
                Brush.radialGradient(
                    *stops,
                    center = Offset(.38f * d, .34f * d),
                    radius = .9055f * d,
                )
            look.sheenA =
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = .75f),
                    .42f to Color.White.copy(alpha = 0f),
                    center = Offset(.3f * d, .24f * d),
                    radius = 1.0332f * d,
                )
            look.sheenB =
                Brush.radialGradient(
                    0f to Color.Black.copy(alpha = .18f),
                    .45f to Color.Black.copy(alpha = 0f),
                    center = Offset(.72f * d, .82f * d),
                    radius = 1.0912f * d,
                )
            val c = Offset(d / 2, d / 2)
            look.bands = Brush.sweepGradient(*BandStops, center = c)
            look.reeds = Brush.sweepGradient(*ReedStops, center = c)
        }
        if (look.face != r.face || look.cents != r.cents || look.size != r.faceSize) {
            look.face = r.face
            look.cents = r.cents
            look.size = r.faceSize
            look.faceText =
                measurer.measure(
                    r.face,
                    faceStyle(r.faceSize, FontWeight.ExtraBold),
                    density = textDensity,
                )
            look.centsText =
                if (r.cents.isEmpty()) null
                else
                    measurer.measure(
                        r.cents,
                        faceStyle(r.faceSize * .52f, FontWeight.Bold),
                        density = textDensity,
                    )
        }
    }

    private fun faceStyle(size: Float, weight: FontWeight) =
        TextStyle(
            fontSize = size.sp,
            fontWeight = weight,
            letterSpacing = (-.03).em,
            lineHeight = .95.em,
            lineHeightStyle =
                LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )

    private companion object {
        // Brushed-steel hairlines, placed once with the prototype's fixed seed.
        val Hairlines: List<FloatArray> = run {
            var s = 7L
            fun r(): Float {
                s = (s * 16807) % 2147483647
                return (s.toDouble() / 2147483647).toFloat()
            }
            List(90) { floatArrayOf(r(), r(), r(), .15f + r() * .5f) }
        }

        val RingDark = Color.Black.copy(alpha = .1f)
        val RingLight = Color.White.copy(alpha = .42f)
        val RingEdge = Color.Black.copy(alpha = .22f)

        // Nickel, copper, brass: the disc's radial stops, its ink and the ink's light lift.
        val DiscStops =
            arrayOf(
                arrayOf(
                    0f to cssColor("#fdfdfd"),
                    .46f to cssColor("#ccd1d6"),
                    1f to cssColor("#8a9198"),
                ),
                arrayOf(
                    0f to cssColor("#ffd9b8"),
                    .48f to cssColor("#cf7d47"),
                    1f to cssColor("#8a4520"),
                ),
                arrayOf(
                    0f to cssColor("#fff4c4"),
                    .48f to cssColor("#d9b04f"),
                    1f to cssColor("#8c6919"),
                ),
            )
        val Inks =
            arrayOf(
                cssColor("rgba(50, 58, 66, .62)"),
                cssColor("rgba(90, 38, 10, .6)"),
                cssColor("rgba(95, 66, 8, .6)"),
            )
        val Lifts =
            arrayOf(
                cssColor("rgba(255, 255, 255, .7)"),
                cssColor("rgba(255, 220, 190, .6)"),
                cssColor("rgba(255, 245, 200, .6)"),
            )

        // CSS conic gradients start at the top; a sweep starts at 3 o'clock, so every stop is
        // moved a quarter turn on.
        private fun conic(vararg stops: Pair<Float, Color>): Array<Pair<Float, Color>> {
            val out = ArrayList<Pair<Float, Color>>()
            for ((o, c) in stops) out += ((o + .75f) % 1f) to c
            out.sortBy { it.first }
            // Close the turn at both ends with the colour found there.
            val first = colorAt(stops, .25f)
            return (listOf(0f to first) + out + listOf(1f to first)).toTypedArray()
        }

        private fun colorAt(stops: Array<out Pair<Float, Color>>, at: Float): Color {
            for (i in 0 until stops.size - 1) {
                val (o0, c0) = stops[i]
                val (o1, c1) = stops[i + 1]
                if (at in o0..o1) {
                    val t = if (o1 > o0) (at - o0) / (o1 - o0) else 0f
                    return androidx.compose.ui.graphics.lerp(c0, c1, t)
                }
            }
            return stops.last().second
        }

        private val Bright = Color.White.copy(alpha = .7f)
        private val Dim = Color.Black.copy(alpha = .3f)
        val BandStops = conic(0f to Bright, .25f to Dim, .5f to Bright, .75f to Dim, 1f to Bright)

        // repeating-conic-gradient: 5° dark, 5° light, all the way round, hard edged.
        val ReedStops: Array<Pair<Float, Color>> = run {
            val dark = Color.Black.copy(alpha = .22f)
            val light = Color.White.copy(alpha = .18f)
            val out = ArrayList<Pair<Float, Color>>()
            for (i in 0 until 36) {
                val a = i / 36f
                val b = a + 1 / 72f
                val e = (i + 1) / 36f
                out += a to dark
                out += b to dark
                out += b to light
                out += e to light
            }
            out.toTypedArray()
        }

        // CSS's `ease`, near enough for a 140 ms fade.
        fun cssEase(t: Float) = t * t * (3 - 2 * t)
    }
}
