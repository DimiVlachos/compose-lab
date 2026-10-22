package dev.dimvlachos.lab.core.presentation.components.zipper

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Pattern2D
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// The zipper, ported line by line from the prototype's draw(): the same calls, in the same order,
// with the same numbers and CSS colours, over the list area in dp.

private const val SL_H = ZipperState.SliderHeight
private const val TAB = ZipperState.Tab
private const val PITCH = ZipperState.Pitch
private const val HW = 16f
private const val TAU = (2 * PI).toFloat()

/** How far in from the list area's right edge the zipper's middle runs. */
internal const val ZipperInset = 26f

/** The tape's weave: a 4 × 4 tile, made once, as the prototype's pattern canvas. */
internal fun zipperWeave(): Pattern2D {
    val c = Canvas2D.create(4, 4)
    c.fillStyle = "rgba(255,255,255,.05)"
    c.fillRect(0f, 0f, 4f, 1f)
    c.fillRect(0f, 2f, 4f, 1f)
    c.fillStyle = "rgba(0,0,0,.12)"
    c.fillRect(1f, 1f, 1f, 1f)
    c.fillRect(3f, 3f, 1f, 1f)
    c.bitmap.freeze()
    return c.createPattern(c.bitmap)
}

private fun Canvas2D.brass(x0: Float, y0: Float, x1: Float, y1: Float): Gradient2D {
    val g = createLinearGradient(x0, y0, x1, y1)
    g.addColorStop(0f, "#fff1bf")
    g.addColorStop(.28f, "#e2b354")
    g.addColorStop(.62f, "#a9741f")
    g.addColorStop(1f, "#6a440c")
    return g
}

private fun Canvas2D.rr(x: Float, y: Float, w: Float, h: Float, r: Float) {
    beginPath()
    roundRect(x, y, w, h, r)
}

/**
 * Paints the zipper in [s] down the right of a list area [width] dp wide (its height is the
 * state's), [dark] or light, with the tape's [weave]. [k] turns the prototype's shadow sizes, which
 * the browser gives in device pixels at its pixel ratio of 2, into this canvas's pixels.
 */
internal fun Canvas2D.drawZipper(
    s: ZipperState,
    width: Float,
    dark: Boolean,
    weave: Pattern2D,
    k: Float,
) {
    if (s.height <= 0f) return
    val cx = width - ZipperInset
    val trackTop = s.trackTop
    val trackBot = s.trackBot
    val sy = s.sliderTopFor(s.pos)
    val sb = sy + SL_H
    val tape = if (dark) "#222a3c" else "#2a3550"
    val tapeEdge = if (dark) "#151a26" else "#1c2438"
    // Half the gap between the opened halves: tight at the pull, prised wider further up.
    fun hgAt(y: Float) = min(9f, 1f + (sy + 4f - y) * .3f)
    fun tapeLit(x0: Float, x1: Float) =
        createLinearGradient(x0, 0f, x1, 0f).apply {
            addColorStop(0f, "rgba(255,255,255,.10)")
            addColorStop(1f, "rgba(0,0,0,.14)")
        }

    // The whole tape is one shape: closed below the pull (and behind it), two prised-open halves
    // above it.
    val open = sy > trackTop + 2f
    val t0 = trackTop
    val b0 = sy + 6f
    val tr = 5f
    val br = 7f
    fun hg0(y: Float) = if (open) max(0f, hgAt(y)) else 0f
    fun outerX(side: Int, y: Float) = cx + side * (HW + hg0(y) * .45f)
    fun innerX(side: Int, y: Float) = cx + side * (hgAt(y) + 3f)
    val ys0 = t0 + tr + 3f
    var nys = 0
    while (ys0 + 3f * nys < b0 + 6f) nys++
    // One half's outer edge, top to bottom (down) or bottom to top, with a rounded top end.
    fun halfOuter(side: Int, down: Boolean) {
        if (down) {
            arcTo(outerX(side, t0), t0, outerX(side, ys0), ys0, tr)
            for (j in 0 until nys) {
                val y = ys0 + 3f * j
                lineTo(outerX(side, y), y)
            }
        } else {
            for (j in nys - 1 downTo 0) {
                val y = ys0 + 3f * j
                lineTo(outerX(side, y), y)
            }
            arcTo(outerX(side, t0), t0, innerX(side, t0), t0, tr)
        }
    }
    // inner=false leaves out the inner (lining-side) edges: the outline that gets the stroked edge.
    fun tapePath(inner: Boolean) {
        beginPath()
        if (!open) {
            roundRect(cx - HW, t0, HW * 2f, trackBot - t0, br)
            return
        }
        moveTo(innerX(-1, t0), t0)
        halfOuter(-1, true)
        lineTo(cx - HW, trackBot - br)
        arcTo(cx - HW, trackBot, cx - HW + br, trackBot, br)
        lineTo(cx + HW - br, trackBot)
        arcTo(cx + HW, trackBot, cx + HW, trackBot - br, br)
        halfOuter(1, false)
        lineTo(innerX(1, t0), t0)
        if (!inner) return
        var y = t0
        while (y <= b0) {
            lineTo(innerX(1, y), y)
            y += 3f
        }
        lineTo(innerX(1, b0), b0)
        lineTo(innerX(-1, b0), b0)
        y = b0
        while (y >= t0) {
            lineTo(innerX(-1, y), y)
            y -= 3f
        }
        closePath()
    }

    // Soft shadow of the closed tape on the page.
    save()
    shadowColor = "rgba(0,0,0,.22)"
    shadowBlur = 8f * k
    shadowOffsetX = 1.5f * k
    shadowOffsetY = 2f * k
    fillStyle = tape
    val shTop = if (open) sy + 4f else t0
    beginPath()
    if (open) roundRect(cx - HW, shTop, HW * 2f, trackBot - shTop, OpenShadowRadii)
    else roundRect(cx - HW, shTop, HW * 2f, trackBot - shTop, 7f)
    fill()
    restore()

    // Opened part above the pull: a red flannel lining shows between the two halves.
    if (open) {
        val lin = createLinearGradient(cx - 12f, 0f, cx + 12f, 0f)
        lin.addColorStop(0f, "#4f141b")
        lin.addColorStop(.5f, "#93303a")
        lin.addColorStop(1f, "#4a1219")
        fillStyle = lin
        fillRect(cx - 12f, trackTop, 24f, sy - trackTop + 8f)
        fillStyle = "rgba(255,220,200,.10)"
        var y = trackTop + 2f
        while (y < sy + 6f) {
            fillRect(cx - 12f, y, 24f, 1.2f)
            y += 6f
        }
        fillStyle = "rgba(0,0,0,.16)"
        fillRect(cx - 4f, trackTop, 1.2f, sy - trackTop + 8f)
        fillRect(cx + 3f, trackTop, 1.2f, sy - trackTop + 8f)
        // Each half casts its own shadow onto the lining; the tape itself is filled over them
        // below.
        for (side in Sides) {
            save()
            shadowColor = "rgba(0,0,0,.4)"
            shadowBlur = 5f * k
            shadowOffsetX = side * -1.5f * k
            shadowOffsetY = 1.5f * k
            beginPath()
            moveTo(innerX(side, t0), t0)
            halfOuter(side, true)
            lineTo(outerX(side, b0 + 6f), b0 + 6f)
            lineTo(innerX(side, b0), b0)
            var yy = b0
            while (yy >= t0) {
                lineTo(innerX(side, yy), yy)
                yy -= 3f
            }
            closePath()
            fillStyle = tape
            fill()
            restore()
        }
    }
    // The tape in one piece: lit, woven, stitched along its outer edges, with a dark selvage.
    fillStyle = tape
    tapePath(true)
    fill()
    fillStyle = tapeLit(cx - HW, cx + HW)
    fill()
    save()
    tapePath(true)
    clip()
    fillStyle = weave
    globalAlpha = .9f
    fillRect(cx - HW - 6f, t0, HW * 2f + 12f, trackBot - t0)
    globalAlpha = 1f
    setLineDash(Stitch)
    lineWidth = 1f
    strokeStyle = "rgba(255,255,255,.18)"
    beginPath()
    for (side in Sides) {
        moveTo(outerX(side, t0 + 4f) - side * 2.5f, t0 + 4f)
        var y = t0 + 4f
        while (y < b0 + 6f) {
            lineTo(outerX(side, y) - side * 2.5f, y)
            y += 3f
        }
        lineTo(cx + side * (HW - 2.5f), trackBot - 4f)
    }
    stroke()
    setLineDash(NoDash)
    // Edge: a 2px stroke clipped to the tape leaves 1px inside, like the old inset stroke.
    strokeStyle = tapeEdge
    lineWidth = 2f
    tapePath(false)
    stroke()
    restore()
    if (open) {
        // A little top stop.
        fillStyle = brass(cx - 6f, trackTop, cx + 6f, trackTop + 4f)
        rr(cx - 7f, trackTop - 1f, 14f, 4f, 1.5f)
        fill()
    }

    // Teeth.
    val tb = "#d9b25e"
    val th = "rgba(255,246,214,.85)"
    val ts = "rgba(70,45,8,.75)"
    var i = 0
    var y = trackTop + 5f
    while (y < trackBot - 4f) {
        if (!(y > sy - 3f && y < sb + 1f)) {
            val left = i % 2 == 0
            val h = PITCH * .62f
            val x0: Float
            val x1: Float
            if (y >= sb) {
                x0 = if (left) cx - 6f else cx - 1.5f
                x1 = if (left) cx + 1.5f else cx + 6f
            } else {
                val g = hgAt(y)
                x0 = if (left) cx - g - 6f else cx + g
                x1 = if (left) cx - g else cx + g + 6f
            }
            fillStyle = ts
            fillRect(x0 + .5f, y + 1f, x1 - x0, h)
            fillStyle = tb
            fillRect(x0, y, x1 - x0, h)
            fillStyle = th
            fillRect(x0 + .5f, y, x1 - x0 - 1f, 1f)
        }
        i++
        y += PITCH
    }

    // Slider body: no light of its own on the list, only shadow. A tight dark contact shadow where
    // it sits on the tape, under the soft one it casts further off.
    fun bodyPath() {
        beginPath()
        moveTo(cx - 12f, sy + 3f)
        quadraticCurveTo(cx - 12f, sy, cx - 9f, sy)
        lineTo(cx + 9f, sy)
        quadraticCurveTo(cx + 12f, sy, cx + 12f, sy + 3f)
        lineTo(cx + 8.5f, sb - 3f)
        quadraticCurveTo(cx + 8f, sb, cx + 5f, sb)
        lineTo(cx - 5f, sb)
        quadraticCurveTo(cx - 8f, sb, cx - 8.5f, sb - 3f)
        closePath()
    }
    save()
    shadowColor = "rgba(0,0,0,.4)"
    shadowBlur = 6f * k
    shadowOffsetX = 1.5f * k
    shadowOffsetY = 2.5f * k
    bodyPath()
    fillStyle = brass(cx - 12f, sy, cx + 10f, sb)
    fill()
    shadowColor = "rgba(0,0,0,.35)"
    shadowBlur = 2f * k
    shadowOffsetX = .5f * k
    shadowOffsetY = 1f * k
    fill()
    restore()
    // A small specular on the brass's top-left shoulder, where the light from the top left catches
    // it.
    save()
    bodyPath()
    clip()
    val spec = createRadialGradient(cx - 6f, sy + 5f, 0f, cx - 6f, sy + 5f, 8f)
    spec.addColorStop(0f, "rgba(255,252,236,.5)")
    spec.addColorStop(1f, "rgba(255,252,236,0)")
    fillStyle = spec
    fillRect(cx - 14f, sy - 3f, 16f, 16f)
    restore()
    strokeStyle = "rgba(255,248,220,.55)"
    lineWidth = 1f
    beginPath()
    moveTo(cx - 9f, sy + 1.2f)
    lineTo(cx + 8f, sy + 1.2f)
    stroke()
    // Centre ridge and the bridge the tab hangs from.
    fillStyle = "rgba(80,50,8,.35)"
    rr(cx - 1.6f, sy + 4f, 3.2f, SL_H - 11f, 1.6f)
    fill()
    fillStyle = "rgba(255,240,200,.35)"
    fillRect(cx - 1.2f, sy + 4.5f, 1f, SL_H - 12f)
    fillStyle = brass(cx - 4f, sb - 9f, cx + 4f, sb - 2f)
    rr(cx - 4.5f, sb - 10f, 9f, 7f, 2.5f)
    fill()

    // Pull tab: lies flat pointing down; held, it stands up off the tape (foreshortened, longer
    // shadow).
    val f = s.flip.coerceIn(0f, 1.4f)
    val sf = sin(f)
    val hy = sb - 6.5f
    val len = TAB * cos(f) + 6f * sf
    val wN = 11f
    val wF = 14f + 3f * sf
    val shX = 1.5f + 9f * sf
    val shY = 2f + 7f * sf
    // The further off the tape the tab stands, the softer and fainter the shadow it casts.
    save()
    fillStyle = Color(0f, 0f, 0f, .32f - .14f * sf)
    filter = blurOf((1.5f + 6f * sf) * k)
    rr(cx - wF / 2f + shX, hy + shY, wF, TAB, 6f)
    fill()
    filter = "none"
    restore()
    beginPath()
    moveTo(cx - wN / 2f, hy)
    lineTo(cx + wN / 2f, hy)
    lineTo(cx + wF / 2f, hy + len - 6f)
    quadraticCurveTo(cx + wF / 2f, hy + len, cx + wF / 2f - 6f, hy + len)
    lineTo(cx - wF / 2f + 6f, hy + len)
    quadraticCurveTo(cx - wF / 2f, hy + len, cx - wF / 2f, hy + len - 6f)
    closePath()
    fillStyle = brass(cx - wF / 2f, hy - 4f * f, cx + wF / 2f, hy + len + 6f * (1f - f))
    fill()
    strokeStyle = "rgba(90,58,10,.6)"
    lineWidth = .8f
    stroke()
    // Its face catches more light as it stands up.
    fillStyle = Color(255f / 255f, 244f / 255f, 205f / 255f, .18f + .3f * sf)
    rr(cx - wN / 2f + 1.5f, hy + 2f, wN - 3f, max(2f, len * .35f), 3f)
    fill()
    // The hole the tag's thread goes through.
    val holeY = hy + len - 7f
    fillStyle = if (dark) "#151820" else "#1d2333"
    beginPath()
    ellipse(cx, holeY, 3.2f, 2.2f * max(.45f, cos(f)) + .6f, 0f, 0f, TAU)
    fill()

    // Cotton tag with the letter, always hanging on its thread from the hole, to the left of the
    // tape. Lying on the page it casts a tight shadow; held, it lifts a little and its shadow
    // grows.
    run {
        val t = s.tag.coerceIn(0f, 1f)
        val tagS = ZipperTag.scale(t)
        val hx = cx - 1f
        // The eyelet at (13, 0) in the tag's rotated, scaled frame is at (ex, ey) in list
        // coordinates: the thread ends there.
        val ex = hx + ZipperTag.eyeletDx(s.swing)
        val ey = holeY + ZipperTag.eyeletDy(s.swing)
        save()
        translate(hx + ZipperTag.originDx(s.swing, tagS), holeY + ZipperTag.originDy(s.swing, tagS))
        rotate(ZipperTag.angle(s.swing))
        scale(tagS, tagS)
        shadowColor = TagShadows[(t * 40f).toInt().coerceIn(0, 40)]
        shadowBlur = (4f + 6f * t) * k
        shadowOffsetX = (1f + t) * k
        shadowOffsetY = (2f + 2f * t) * k
        beginPath()
        moveTo(-26f, -22f)
        lineTo(8f, -22f)
        lineTo(20f, -9f)
        lineTo(20f, 9f)
        lineTo(8f, 22f)
        lineTo(-26f, 22f)
        quadraticCurveTo(-30f, 22f, -30f, 18f)
        lineTo(-30f, -18f)
        quadraticCurveTo(-30f, -22f, -26f, -22f)
        closePath()
        val paper = createLinearGradient(-30f, -22f, 20f, 22f)
        paper.addColorStop(0f, "#fbf4e2")
        paper.addColorStop(1f, "#e6d8b8")
        fillStyle = paper
        fill()
        shadowColor = "rgba(0,0,0,0)"
        strokeStyle = "rgba(150,120,70,.35)"
        lineWidth = 1f
        stroke()
        // Eyelet.
        fillStyle = brass(10f, -4f, 16f, 4f)
        beginPath()
        arc(13f, 0f, 3.6f, 0f, TAU)
        fill()
        fillStyle = if (dark) "#16171b" else "#f6f5f2"
        beginPath()
        arc(13f, 0f, 1.8f, 0f, TAU)
        fill()
        fillStyle = "#2b2620"
        font = "700 27px serif"
        textAlign = "center"
        textBaseline = "middle"
        fillText(s.letter, -8f, 2f)
        restore()
        // Thread drawn over the tag so it visibly runs into the eyelet hole, with a little slack.
        strokeStyle = if (dark) "rgba(236,228,210,.75)" else "rgba(120,108,90,.85)"
        lineWidth = 1.2f
        beginPath()
        moveTo(hx, holeY)
        quadraticCurveTo((hx + ex) / 2f, (holeY + ey) / 2f + 3f, ex, ey)
        stroke()
    }
}

private val Sides = intArrayOf(-1, 1)

// The tag's shadow as it lifts, in a few steps, so a lifting tag doesn't build a string a frame.
private val TagShadows = Array(41) { "rgba(0,0,0,${.24f + .04f * it / 40f})" }
private val Stitch = floatArrayOf(2.5f, 2.5f)
private val NoDash = floatArrayOf()
private val OpenShadowRadii = floatArrayOf(0f, 0f, 7f, 7f)

// The tab's shadow blur in a few hundred steps, so a moving tab doesn't build a string a frame.
private val BlurSteps = Array(400) { "blur(${it / 20f}px)" }

private fun blurOf(px: Float): String =
    BlurSteps[(px * 20f).toInt().coerceIn(0, BlurSteps.size - 1)]
