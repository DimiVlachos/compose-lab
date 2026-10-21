package dev.dimvlachos.lab.core.presentation.components.padlock

import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

// The padlock, ported line by line from the prototype's draw(): the same calls, in the same order,
// with the same numbers and CSS colours. It is drawn in a 300 × 250 box, which [PadlockBox] places.

/** Where the lock's 300 × 250 box sits in a hero area of [width] by [height], in the same units. */
internal class PadlockBox(width: Float, height: Float) {
    val scale = minOf(width / 300f, height / 250f, 1.35f)
    val left = (width - 300f * scale) / 2f
    val top = (height - 250f * scale) / 2f + 6f * scale

    /** The lock's tap target, round the body and the shackle: the prototype's button. */
    val hitLeft = left + 80f * scale
    val hitTop = top + 30f * scale
    val hitWidth = 140f * scale
    val hitHeight = 210f * scale
}

/** The brushed grain on the body, made once with the prototype's fixed seed so it never crawls. */
internal fun padlockGrain(): ImageBitmap {
    val c = Canvas2D.create(256, 128)
    var s = 7L
    val r = {
        s = (s * 16807L) % 2147483647L
        s.toFloat() / 2147483647f
    }
    repeat(260) {
        val y = r() * 128f
        val a = r() * .1f
        val l = 30f + r() * 160f
        val x0 = r() * 256f
        c.strokeStyle = if (r() < .5f) "rgba(255,240,200,$a)" else "rgba(60,35,5,$a)"
        c.lineWidth = .5f + r() * .7f
        c.beginPath()
        c.moveTo(x0, y)
        c.lineTo(x0 + l, y + (r() - .5f))
        c.stroke()
    }
    return c.bitmap
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun ease(t: Float) = if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f

private fun hex(h: String) = IntArray(3) { h.substring(1 + it * 2, 3 + it * 2).toInt(16) }

private fun mixc(a: String, b: String, t: Float): String {
    val x = hex(a)
    val y = hex(b)
    val c = IntArray(3) { round(lerp(x[it].toFloat(), y[it].toFloat(), t)).toInt() }
    return "rgb(${c[0]},${c[1]},${c[2]})"
}

private val Brass =
    mapOf(
        "edge" to "#3e2806",
        "dark" to "#6d4a12",
        "mid" to "#bb8c33",
        "light" to "#f3d68d",
        "spec" to "#fff7dc",
    )
private val SteelInk =
    mapOf(
        "edge" to "#20252a",
        "dark" to "#4a535c",
        "mid" to "#97a2ac",
        "light" to "#e3e9ee",
        "spec" to "#ffffff",
    )

/**
 * A round metal bar along [path]: stroked several times, each narrower and nudged toward the light,
 * from the edge to the specular line, in brass turning to steel by [steel].
 */
private fun Canvas2D.tube(path: () -> Unit, t: Float, steel: Float) {
    fun col(k: String) = mixc(Brass.getValue(k), SteelInk.getValue(k), steel)
    lineJoin = "round"
    fun pass(w: Float, color: String, dx: Float, dy: Float, a: Float) {
        save()
        globalAlpha = a
        translate(dx, dy)
        lineWidth = w
        strokeStyle = color
        path()
        stroke()
        restore()
    }
    pass(t + 1.6f, col("edge"), .4f, .8f, .9f)
    pass(t, col("dark"), 0f, 0f, 1f)
    pass(t * .74f, col("mid"), -t * .07f, -t * .05f, 1f)
    pass(t * .4f, col("light"), -t * .15f, -t * .1f, .95f)
    pass(t * .13f, col("spec"), -t * .2f, -t * .14f, .85f)
    // Warm bounce light from the brass body on the far side of the bar.
    pass(
        t * .12f,
        if (steel < .5f) "rgba(255,200,120,.35)" else "rgba(255,225,180,.28)",
        t * .3f,
        t * .12f,
        1f,
    )
}

/**
 * Paints the lock in [state] into a hero area of [width] by [height] (in dp, under the canvas's own
 * scale), [dark] or light. [shadowScale] turns the prototype's shadow sizes, which the browser
 * gives in device pixels at its pixel ratio of 2, into this canvas's pixels.
 */
internal fun Canvas2D.drawPadlock(
    state: PadlockState,
    width: Float,
    height: Float,
    dark: Boolean,
    grain: ImageBitmap,
    shadowScale: Float,
) {
    val box = PadlockBox(width, height)
    save()
    translate(box.left, box.top)
    scale(box.scale, box.scale)
    val s = state
    val steel = s.steel.coerceIn(0f, 1f)
    val cx = 150f
    val gy = 236f
    val bw = lerp(104f, 124f, steel)
    val bh = lerp(88f, 100f, steel) * (1f + s.bounce * .004f)
    val top = gy - bh
    val left = cx - bw / 2f

    // A warm pool of lamp light on the wall behind.
    // Squashed into an ellipse and kept inside the hero, so it never shows a hard edge.
    save()
    translate(cx, 140f)
    scale(1.5f, 1f)
    val pool = createRadialGradient(-6f, -18f, 6f, 0f, 0f, 108f)
    pool.addColorStop(0f, if (dark) "rgba(255,186,96,.17)" else "rgba(255,214,150,.55)")
    pool.addColorStop(.55f, if (dark) "rgba(255,186,96,.06)" else "rgba(255,214,150,.2)")
    pool.addColorStop(1f, "rgba(255,214,150,0)")
    fillStyle = pool
    fillRect(-110f, -110f, 220f, 220f)
    restore()
    // Contact shadow and a softer cast shadow down-right, away from the light.
    var g = createRadialGradient(cx + 10f, gy + 2f, 4f, cx + 10f, gy + 2f, bw * .75f)
    g.addColorStop(0f, if (dark) "rgba(0,0,0,.55)" else "rgba(70,45,15,.32)")
    g.addColorStop(1f, "rgba(70,45,15,0)")
    save()
    translate(0f, gy + 2f)
    scale(1f, .16f)
    translate(0f, -(gy + 2f))
    fillStyle = g
    fillRect(cx - bw, gy - bw, bw * 2.2f, bw * 2f)
    restore()

    // ---- Shackle (drawn first: its legs disappear into the body) ----
    val t = lerp(11f, 17f, steel)
    val half = bw * .28f - (t - 11f) * .2f
    val xr = cx + half
    val r = half
    // Swung up to half a turn about the long leg, so a weak shackle stands open past the body.
    val sw = s.swing.coerceIn(-.2f, PI.toFloat() + .2f)
    val cw = cos(sw)
    val xl = xr - 2f * r * cw
    val armTop = top - lerp(30f, 34f, steel) - s.lift
    val rightBottom = top + 34f
    val leftBottom = top + 12f - s.lift
    save()
    translate(xr, top)
    rotate(s.rattle * .25f)
    translate(-xr, -top)
    val path = {
        beginPath()
        moveTo(xl, leftBottom)
        lineTo(xl, armTop)
        // Past a quarter turn the U is mirrored: the arc runs from the short leg on the right,
        // over the top, back to the long leg.
        if (cw >= 0f) ellipse(xr - r * cw, armTop, r * cw, r, 0f, PI.toFloat(), PI.toFloat() * 2f)
        else ellipse(xr - r * cw, armTop, -r * cw, r, 0f, 0f, -PI.toFloat(), ccw = true)
        lineTo(xr, rightBottom)
    }
    lineCap = "round"
    tube(path, t, steel)
    // A swung-open short leg turns away from the light, so it darkens.
    if (sw > .05f) {
        save()
        globalAlpha = (sw * .5f).coerceIn(0f, .35f)
        strokeStyle = "#1a1206"
        lineWidth = t + 1f
        beginPath()
        moveTo(xl, leftBottom)
        lineTo(xl, armTop + 4f)
        stroke()
        restore()
    }
    // The notch cut in the short leg, visible once it's out of the body.
    if (top - leftBottom < -2f || s.lift > 14f) {
        save()
        globalAlpha = ((s.lift - 12f) / 8f).coerceIn(0f, 1f) * .55f
        strokeStyle = "#1b1306"
        lineWidth = 1.4f
        beginPath()
        moveTo(xl - t * .45f, leftBottom - 7f)
        lineTo(xl + t * .45f, leftBottom - 7f)
        stroke()
        restore()
    }
    restore()

    // ---- Body ----
    val rad = bh * .15f
    save()
    shadowColor = if (dark) "rgba(0,0,0,.6)" else "rgba(60,35,5,.35)"
    shadowBlur = 14f * shadowScale
    shadowOffsetX = 5f * shadowScale
    shadowOffsetY = 7f * shadowScale
    beginPath()
    roundRect(left, top, bw, bh, rad)
    g = createLinearGradient(left, 0f, left + bw, 0f)
    g.addColorStop(0f, "#6a4610")
    g.addColorStop(.1f, "#b98a31")
    g.addColorStop(.27f, "#f6db94")
    g.addColorStop(.36f, "#e3bb62")
    g.addColorStop(.62f, "#b8892f")
    g.addColorStop(.88f, "#8a611b")
    g.addColorStop(1f, "#4e3209")
    fillStyle = g
    fill()
    restore()
    save()
    beginPath()
    roundRect(left, top, bw, bh, rad)
    clip()
    g = createLinearGradient(0f, top, 0f, gy)
    g.addColorStop(0f, "rgba(255,244,210,.32)")
    g.addColorStop(.18f, "rgba(255,244,210,0)")
    g.addColorStop(.7f, "rgba(60,30,0,0)")
    g.addColorStop(1f, "rgba(50,25,0,.38)")
    fillStyle = g
    fillRect(left, top, bw, bh)
    globalAlpha = .9f
    drawImage(grain, left, top, bw, bh)
    globalAlpha = 1f
    // The top face catches the lamp.
    g = createLinearGradient(0f, top, 0f, top + 6f)
    g.addColorStop(0f, "rgba(255,250,228,.75)")
    g.addColorStop(1f, "rgba(255,250,228,0)")
    fillStyle = g
    fillRect(left, top, bw, 6f)
    // Stamped maker's line, debossed. It sits higher than the prototype's (bh * .22), where the
    // keyhole cover swinging up crossed it.
    font = "700 7.4px mono"
    textAlign = "center"
    val stamp = if (steel > .8f) "HARDENED · 50MM" else "SOLID BRASS · 50MM"
    val stampY = top + bh * StampAt
    fillStyle = "rgba(255,240,200,.45)"
    fillText(stamp, cx, stampY + .8f)
    fillStyle = "rgba(60,35,5,.5)"
    fillText(stamp, cx, stampY)
    // Glint: a bright band sweeps across the brass once it locks.
    if (s.glintAt >= 0f) {
        val p = (s.time - s.glintAt) / 720f
        if (p in 0f..1f) {
            val gx = lerp(left - 60f, left + bw + 60f, ease(p))
            g = createLinearGradient(gx - 26f, top, gx + 26f, top + 30f)
            g.addColorStop(0f, "rgba(255,255,240,0)")
            g.addColorStop(.5f, "rgba(255,255,240,.75)")
            g.addColorStop(1f, "rgba(255,255,240,0)")
            globalCompositeOperation = "lighter"
            fillStyle = g
            fillRect(left, top, bw, bh)
            globalCompositeOperation = "source-over"
        }
    }
    restore()
    // Bevel: light along the top-left edge, shade along the bottom-right.
    save()
    beginPath()
    roundRect(left + 1f, top + 1f, bw - 2f, bh - 2f, rad - 1f)
    g = createLinearGradient(left, top, left + bw, gy)
    g.addColorStop(0f, "rgba(255,246,214,.85)")
    g.addColorStop(.45f, "rgba(255,246,214,.12)")
    g.addColorStop(.6f, "rgba(60,35,5,.15)")
    g.addColorStop(1f, "rgba(50,28,3,.7)")
    strokeStyle = g
    lineWidth = 2f
    stroke()
    restore()

    // ---- Keyhole, with light behind it when you peek ----
    val ky = top + bh * .6f
    val kr = bh * .07f
    val cover = s.cover.coerceIn(-.1f, 1.15f)
    // Raised escutcheon ring.
    g = createRadialGradient(cx - kr, ky - kr, 1f, cx, ky, kr * 2.6f)
    g.addColorStop(0f, "#f8e2a4")
    g.addColorStop(.6f, "#c7993f")
    g.addColorStop(1f, "#7c5615")
    fillStyle = g
    beginPath()
    arc(cx, ky + kr * .4f, kr * 2.4f, 0f, PI.toFloat() * 2f)
    fill()
    strokeStyle = "rgba(60,35,5,.45)"
    lineWidth = .8f
    stroke()
    val keyhole = {
        beginPath()
        arc(cx, ky, kr, PI.toFloat() * .72f, PI.toFloat() * 2.28f)
        lineTo(cx + kr * .62f, ky + kr * 2.5f)
        lineTo(cx - kr * .62f, ky + kr * 2.5f)
        closePath()
    }
    keyhole()
    fillStyle = "#140c03"
    fill()
    val glow = cover.coerceIn(0f, 1f)
    if (glow > .01f) {
        save()
        keyhole()
        clip()
        g = createRadialGradient(cx, ky + kr * .8f, 0f, cx, ky + kr * .8f, kr * 2.4f)
        g.addColorStop(0f, "rgba(255,236,190,$glow)")
        g.addColorStop(.5f, "rgba(255,184,92,${glow * .9f})")
        g.addColorStop(1f, "rgba(150,80,20,${glow * .5f})")
        fillStyle = g
        fillRect(cx - kr * 2f, ky - kr * 2f, kr * 4f, kr * 5f)
        restore()
        save()
        globalCompositeOperation = "lighter"
        g = createRadialGradient(cx, ky + kr, 0f, cx, ky + kr, kr * 7f)
        g.addColorStop(0f, "rgba(255,190,100,${glow * .32f})")
        g.addColorStop(1f, "rgba(255,190,100,0)")
        fillStyle = g
        beginPath()
        arc(cx, ky + kr, kr * 7f, 0f, PI.toFloat() * 2f)
        fill()
        restore()
    }
    // The swinging keyhole cover, on a rivet above the hole. It swings at most [CoverSwing] (the
    // prototype's 1.95, overshooting to 2.24, carried the disc up over the stamp).
    val px = cx
    val py = ky - kr * 2.9f
    val ang = -min(cover, 1.1f) * CoverSwing
    val d = kr * 3.15f
    val cr = kr * 1.95f
    val ccx = px + sin(-ang) * d
    val ccy = py + cos(ang) * d
    save()
    shadowColor = "rgba(40,22,2,.5)"
    shadowBlur = 3f * shadowScale
    shadowOffsetX = 1.4f * shadowScale
    shadowOffsetY = 2f * shadowScale
    beginPath()
    arc(ccx, ccy, cr, 0f, PI.toFloat() * 2f)
    moveTo(px, py)
    arc(px, py, kr * .95f, 0f, PI.toFloat() * 2f)
    g = createRadialGradient(ccx - cr * .45f, ccy - cr * .5f, 0f, ccx, ccy, cr * 1.2f)
    g.addColorStop(0f, "#fff0bf")
    g.addColorStop(.45f, "#d8ab52")
    g.addColorStop(1f, "#7d5714")
    fillStyle = g
    fill()
    restore()
    // The arm joining the rivet to the disc.
    save()
    strokeStyle = "#b9893a"
    lineWidth = kr * 1.15f
    lineCap = "round"
    beginPath()
    moveTo(px, py)
    lineTo(ccx, ccy)
    stroke()
    restore()
    beginPath()
    arc(ccx, ccy, cr * .92f, 0f, PI.toFloat() * 2f)
    g = createRadialGradient(ccx - cr * .45f, ccy - cr * .5f, 0f, ccx, ccy, cr * 1.1f)
    g.addColorStop(0f, "#fff3cc")
    g.addColorStop(.5f, "#d2a44b")
    g.addColorStop(1f, "#8a6119")
    fillStyle = g
    fill()
    strokeStyle = "rgba(60,35,5,.4)"
    lineWidth = .7f
    stroke()
    // Rivet head.
    g = createRadialGradient(px - 1f, py - 1.2f, 0f, px, py, kr * .9f)
    g.addColorStop(0f, "#fffbe8")
    g.addColorStop(.5f, "#c99c45")
    g.addColorStop(1f, "#5e400c")
    fillStyle = g
    beginPath()
    arc(px, py, kr * .85f, 0f, PI.toFloat() * 2f)
    fill()

    // A small star of light where the shackle top meets the lamp, just after it locks.
    if (s.glintAt >= 0f) {
        val p = (s.time - s.glintAt - 120f) / 600f
        if (p > 0f && p < 1f) {
            val a = sin(p * PI.toFloat())
            val sx = xr - r - r * .55f
            val sy = armTop - r * .78f
            val l = 16f * a
            save()
            globalCompositeOperation = "lighter"
            translate(sx, sy)
            rotate(p * .6f)
            fillStyle = "rgba(255,255,245,$a)"
            repeat(4) {
                rotate(PI.toFloat() / 2f)
                beginPath()
                moveTo(0f, -l)
                lineTo(1.3f * a, 0f)
                lineTo(-1.3f * a, 0f)
                closePath()
                fill()
            }
            g = createRadialGradient(0f, 0f, 0f, 0f, 0f, 7f * a + .1f)
            g.addColorStop(0f, "rgba(255,255,240,$a)")
            g.addColorStop(1f, "rgba(255,255,240,0)")
            fillStyle = g
            beginPath()
            arc(0f, 0f, 7f * a + .1f, 0f, PI.toFloat() * 2f)
            fill()
            restore()
        }
    }
    restore()
}

/** Where the stamp's baseline sits down the body, as a share of its height. */
internal const val StampAt = .17f

/** How far the keyhole cover swings at most, in radians. */
internal const val CoverSwing = 1.6f

/** The lock's sizes, for checks that the stamp and the cover never meet. */
internal object PadlockGeometry {
    /** The keyhole cover disc's top edge at [cover], as a share of the body's height. */
    fun coverTop(cover: Float): Float {
        val kr = .07f
        val py = .6f - kr * 2.9f
        val ang = -min(cover.coerceIn(-.1f, 1.15f), 1.1f) * CoverSwing
        return py + cos(ang) * kr * 3.15f - kr * 1.95f
    }

    /** Whether the cover's disc stays clear of the stamp across its whole swing. */
    fun coverClearsStamp(): Boolean = (0..115).all { i -> coverTop(i / 100f) > StampAt + .02f }
}
