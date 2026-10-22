package dev.dimvlachos.lab.core.presentation.components.birdcage

import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Pattern2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// The birdcage, ported line by line from the prototype's draw(): the same calls, in the same order,
// with the same numbers and CSS colours. It is drawn in a 300 × 290 box, which [BirdcageBox]
// places.

private const val Pi = PI.toFloat()

/** The linen weave, made once with the prototype's fixed seed so it never crawls. */
internal fun birdcageWeave(): ImageBitmap {
    val x = Canvas2D.create(64, 64)
    var s = 11L
    val r = {
        s = (s * 16807L) % 2147483647L
        s.toFloat() / 2147483647f
    }
    var i = 0
    while (i < 64) {
        x.fillStyle = "rgba(90,70,40,${.05f + r() * .05f})"
        x.fillRect(0f, i.toFloat(), 64f, 1f)
        x.fillStyle = "rgba(255,250,235,${.05f + r() * .05f})"
        x.fillRect(i.toFloat(), 0f, 1f, 64f)
        i += 2
    }
    repeat(26) {
        x.fillStyle = "rgba(110,85,50,${.06f + r() * .08f})"
        val rx = r() * 64f
        val ry = r() * 64f
        val rw = 2f + r() * 6f
        x.fillRect(rx, ry, rw, 1f)
    }
    return x.bitmap
}

private val Dash = floatArrayOf(3f, 3f)
private val NoDash = floatArrayOf()

/**
 * Strokes the path already built three times, as the prototype's `brass()`: a dark edge, the brass
 * and a thin highlight nudged toward the window, its alpha multiplied by how [lit] the bar is, so a
 * dimmed bar stays dimmed.
 */
private fun Canvas2D.brass(dark: Boolean, w: Float, lit: Float) {
    lineCap = "round"
    lineJoin = "round"
    strokeStyle = if (dark) "#4a3209" else "#6b4810"
    lineWidth = w + .9f
    stroke()
    strokeStyle = if (lit > .5f) "#d9ae55" else "#b18431"
    lineWidth = w
    stroke()
    save()
    globalAlpha *= .35f + lit * .55f
    translate(-w * .22f, -w * .1f)
    strokeStyle = "#fff1c4"
    lineWidth = w * .38f
    stroke()
    restore()
}

/** A dome rib from the ring up to the finial, plus its straight bar down to the base. */
private fun Canvas2D.meridian(phi: Float) {
    beginPath()
    for (i in 0..14) {
        val a = (i / 14f) * Pi / 2f
        val x = CX + RX * sin(phi) * cos(a)
        val y = DomeY - RY * sin(a)
        if (i > 0) lineTo(x, y) else moveTo(x, y)
    }
    moveTo(CX + RX * sin(phi), DomeY)
    lineTo(CX + RX * sin(phi), BaseY - 4f)
}

private fun Canvas2D.ring(y: Float, rx: Float, ry: Float, front: Boolean) {
    beginPath()
    ellipse(CX, y, rx, ry, 0f, if (front) 0f else Pi, if (front) Pi else Pi * 2f)
}

private const val NoteFont13 = "600 13px system-ui, sans-serif"
private const val NoteFont15 = "600 15px system-ui, sans-serif"
private const val ZFont = "600 10px system-ui, sans-serif"

private fun Canvas2D.canary(s: BirdcageState, dark: Boolean) {
    val now = s.time
    val sl = s.sleep.coerceIn(0f, 1f)
    var hop = 0f
    var beak = 0f
    var tilt = 0f
    if (s.chirpAt >= 0f) {
        val p = (now - s.chirpAt) / 1100f
        if (p >= 0f && p < 1f) {
            hop = max(0f, sin(p * Pi * 2f)) * 7f * (1f - p * .5f)
            beak = max(0f, sin(p * Pi * 6f)) * (if (p < .8f) 1f else 0f)
            tilt = sin(p * Pi) * -.35f
        }
    }
    val breathe = sin(now / 520f) * .5f * (1f + sl)
    val px = CX + 2f
    val py = 196f - hop
    save()
    translate(px, py)
    // Tail.
    fillStyle = "#c69614"
    beginPath()
    moveTo(-12f, -6f)
    lineTo(-34f, 14f)
    lineTo(-27f, 17f)
    lineTo(-8f, 2f)
    closePath()
    fill()
    // Body, puffed a little when asleep.
    val bx = 0f
    val by = -16f + sl * 2f
    val brx = 19f + sl * 2f + breathe * .4f
    val bry = 15f + sl * 2.5f + breathe
    var g = createRadialGradient(bx - 8f, by - 9f, 2f, bx, by, brx * 1.3f)
    g.addColorStop(0f, "#fff3a8")
    g.addColorStop(.45f, "#f7cf2c")
    g.addColorStop(1f, "#c48d0c")
    fillStyle = g
    beginPath()
    ellipse(bx, by, brx, bry, -.28f, 0f, Pi * 2f)
    fill()
    // Wing.
    fillStyle = "rgba(176,122,10,.55)"
    beginPath()
    ellipse(-4f, by + 1f, 12f, 7.5f, -.45f, 0f, Pi * 2f)
    fill()
    strokeStyle = "rgba(140,92,6,.45)"
    lineWidth = .8f
    for (i in 0 until 3) {
        beginPath()
        moveTo(-12f + i * 4f, by + 2f + i)
        lineTo(-1f + i * 3f, by + 6f + i * .6f)
        stroke()
    }
    // Head: up and alert, or tucked down into the shoulder.
    val hx = lerp(15f, 7f, sl)
    val hy = lerp(-34f, -24f, sl)
    val ha = lerp(tilt, .7f, sl)
    save()
    translate(hx, hy)
    rotate(ha)
    g = createRadialGradient(-3f, -4f, 1f, 0f, 0f, 12f)
    g.addColorStop(0f, "#fff6b8")
    g.addColorStop(.5f, "#f8d434")
    g.addColorStop(1f, "#cf980f")
    fillStyle = g
    beginPath()
    arc(0f, 0f, 10.5f, 0f, Pi * 2f)
    fill()
    // Beak, opening as it sings.
    val open = beak * .32f
    fillStyle = "#e88a2a"
    beginPath()
    moveTo(8f, -3f)
    lineTo(16f, -1f - open * 10f)
    lineTo(9f, 1f)
    closePath()
    fill()
    fillStyle = "#c86f1c"
    beginPath()
    moveTo(9f, 1f)
    lineTo(15f, 2f + open * 8f)
    lineTo(8f, 3f)
    closePath()
    fill()
    // Eye: a bright bead under a lid that closes down over it into a sleepy crescent.
    val lid = s.lid.coerceIn(0f, 1f)
    if (lid < .98f) {
        fillStyle = "#1b1307"
        beginPath()
        ellipse(4f, -3f + lid * .6f, 2f, 2f * (1f - lid), 0f, 0f, Pi * 2f)
        fill()
        if (lid < .6f) {
            globalAlpha = 1f - lid / .6f
            fillStyle = "#fff"
            beginPath()
            arc(4.6f, -3.7f + lid * .6f, .7f, 0f, Pi * 2f)
            fill()
            globalAlpha = 1f
        }
    }
    if (lid > .55f) {
        globalAlpha = ((lid - .55f) / .4f).coerceIn(0f, 1f)
        strokeStyle = "#5a3f0a"
        lineWidth = 1.2f
        beginPath()
        arc(4f, -3.5f, 2.2f, .2f, Pi - .2f)
        stroke()
        globalAlpha = 1f
    }
    restore()
    restore()
    // Feet on the perch.
    strokeStyle = "#b07a46"
    lineWidth = 1.4f
    lineCap = "round"
    for (fx in Feet) {
        beginPath()
        moveTo(px + fx, py - 4f)
        lineTo(px + fx, 200f)
        stroke()
    }
    // Notes rise from the beak while it sings.
    if (s.chirpAt >= 0f) {
        val p = (now - s.chirpAt) / 1500f
        if (p > 0f && p < 1f) {
            save()
            textAlign = "center"
            for (i in 0 until 2) {
                val q = (p * 1.3f - i * .3f).coerceIn(0f, 1f)
                if (q <= 0f || q >= 1f) continue
                globalAlpha = sin(q * Pi) * (if (dark) .85f else .7f)
                fillStyle = if (dark) "#f3d27a" else "#9a6a14"
                font = if (i == 0) NoteFont13 else NoteFont15
                fillText(
                    if (i == 1) "♫" else "♪",
                    px + 30f + q * 16f + i * 10f,
                    py - 34f - q * 34f - i * 6f,
                )
            }
            restore()
            textAlign = "left"
        }
    }
}

private val Feet = floatArrayOf(-4f, 5f)

/** The seeds heaped in the cup: x, y, then their light and dark colours. */
private class Seed(val x: Float, val y: Float, val light: String, val dark: String)

private val Seeds =
    listOf(
        Seed(-58f, 214.6f, "#d9b35c", "#ae8f4a"),
        Seed(-55f, 213.6f, "#b98a3a", "#946e2e"),
        Seed(-52f, 214.8f, "#e3c477", "#b69d5f"),
        Seed(-49f, 213.9f, "#c69a48", "#9e7b3a"),
        Seed(-54f, 215.4f, "#a77a30", "#866226"),
        Seed(-50.5f, 215.2f, "#d9b35c", "#ae8f4a"),
    )

/**
 * Draws the cage, the canary and the cover for [s] in a hero of [w] by [h] (in the units this
 * canvas is drawn in). [weave] is [birdcageWeave]'s; [shadowScale] turns the prototype's shadow
 * pixels, set at its pixel ratio of 2, into this canvas's.
 */
internal fun Canvas2D.drawBirdcage(
    s: BirdcageState,
    w: Float,
    h: Float,
    dark: Boolean,
    weave: Pattern2D,
    shadowScale: Float,
) {
    val box = BirdcageBox(w, h)
    val sc = box.scale
    // One layer, so the rod's fade and the light behind it act on the cage alone.
    beginLayer()
    translate(box.left, box.top)
    scale(sc, sc)
    // Hanger: a brass rod from the top of the hero to the ring. Its top 24 px fade out, so it
    // reads as hanging from above rather than from the subtitle.
    val top = -box.top / sc - 2f
    val fade = 24f / sc
    beginPath()
    moveTo(CX, top)
    lineTo(CX, TopY - 22f)
    brass(dark, 2.4f, .7f)
    save()
    globalCompositeOperation = "destination-out"
    var g = createLinearGradient(0f, top + 2f, 0f, top + 2f + fade)
    g.addColorStop(0f, "rgba(0,0,0,1)")
    g.addColorStop(1f, "rgba(0,0,0,0)")
    fillStyle = g
    fillRect(CX - 10f, top - 2f, 20f, fade + 4f)
    restore()
    // Window light from the upper left, a warm pool on the wall behind the cage (drawn behind the
    // rod).
    save()
    globalCompositeOperation = "destination-over"
    translate(CX - 10f, 150f)
    scale(1.35f, 1f)
    g = createRadialGradient(-20f, -30f, 8f, 0f, 0f, 130f)
    g.addColorStop(0f, if (dark) "rgba(255,190,110,.16)" else "rgba(255,220,160,.6)")
    g.addColorStop(.6f, if (dark) "rgba(255,190,110,.05)" else "rgba(255,220,160,.18)")
    g.addColorStop(1f, "rgba(255,220,160,0)")
    fillStyle = g
    fillRect(-140f, -140f, 280f, 280f)
    restore()

    beginPath()
    arc(CX, TopY - 15f, 7f, 0f, Pi * 2f)
    brass(dark, 2.6f, .7f)

    val c = s.cover.coerceIn(-.05f, 1.06f)
    val hy = s.hemY
    val covered = c > .985f && !s.dragging

    if (!covered) {
        // Back half of the cage: dim bars and rings, then the bird, then the front bars.
        val n = 9
        save()
        globalAlpha = .45f
        for (i in 0 until n) {
            val phi = -Pi / 2f + Pi * (i + .5f) / n
            meridian(phi + Pi / n / 2f)
            brass(dark, 1.3f, .2f)
        }
        ring(DomeY, RX, 9f, false)
        brass(dark, 1.6f, .3f)
        ring(Ring2Y, RX, 9f, false)
        brass(dark, 1.2f, .3f)
        restore()
        // Perch and seed cup.
        strokeStyle = "#7a5130"
        lineWidth = 4.2f
        lineCap = "round"
        beginPath()
        moveTo(CX - 46f, 201f)
        lineTo(CX + 46f, 201f)
        stroke()
        strokeStyle = "#c2925f"
        lineWidth = 1.4f
        beginPath()
        moveTo(CX - 45f, 199.6f)
        lineTo(CX + 45f, 199.6f)
        stroke()
        // Seed cup: a small porcelain bowl, lit from the left, with a brass rim and seeds showing.
        beginPath()
        moveTo(CX - 62f, 214.5f)
        lineTo(CX - 44f, 214.5f)
        lineTo(CX - 46f, 222f)
        quadraticCurveTo(CX - 53f, 229.5f, CX - 60f, 222f)
        closePath()
        g = createLinearGradient(CX - 62f, 0f, CX - 44f, 0f)
        if (dark) {
            g.addColorStop(0f, "#b4ad9c")
            g.addColorStop(.4f, "#9a937f")
            g.addColorStop(1f, "#5f5a4c")
        } else {
            g.addColorStop(0f, "#fbf8f0")
            g.addColorStop(.4f, "#e6dfcf")
            g.addColorStop(1f, "#a99d84")
        }
        fillStyle = g
        fill()
        strokeStyle = if (dark) "rgba(20,15,5,.5)" else "rgba(90,70,40,.35)"
        lineWidth = .8f
        stroke()
        // Seeds heaped in the mouth of the cup.
        fillStyle = if (dark) "#3a2d18" else "#6b5330"
        beginPath()
        ellipse(CX - 53f, 214.5f, 8.6f, 2.2f, 0f, 0f, Pi * 2f)
        fill()
        for (seed in Seeds) {
            fillStyle = if (dark) seed.dark else seed.light
            beginPath()
            ellipse(CX + seed.x, seed.y, 1.3f, .8f, .4f, 0f, Pi * 2f)
            fill()
        }
        // Rounded brass lip.
        strokeStyle = "#c99a46"
        lineWidth = 1.5f
        beginPath()
        ellipse(CX - 53f, 214.5f, 9.2f, 2.4f, 0f, 0f, Pi * 2f)
        stroke()
        strokeStyle = "rgba(255,241,196,.7)"
        lineWidth = .6f
        beginPath()
        ellipse(CX - 53f, 214.1f, 9.2f, 2.4f, 0f, Pi * 1.05f, Pi * 1.6f)
        stroke()
        canary(s, dark)
        for (i in 0 until n) {
            val phi = -Pi / 2f + Pi * (i + .5f) / n
            meridian(phi)
            brass(dark, 1.9f, (.5f - sin(phi) * .6f).coerceIn(0f, 1f))
        }
        ring(DomeY, RX, 9f, true)
        brass(dark, 2.2f, .7f)
        ring(Ring2Y, RX, 9f, true)
        brass(dark, 1.6f, .6f)
        // Finial knob.
        g = createRadialGradient(CX - 2.5f, TopY - 4f, .5f, CX, TopY - 2f, 8f)
        g.addColorStop(0f, "#fff3c8")
        g.addColorStop(.5f, "#d0a24a")
        g.addColorStop(1f, "#6e4a10")
        fillStyle = g
        beginPath()
        arc(CX, TopY - 3f, 6.5f, 0f, Pi * 2f)
        fill()
        // Base tray, lit from the left.
        save()
        shadowColor = if (dark) "rgba(0,0,0,.55)" else "rgba(70,45,15,.3)"
        shadowBlur = 14f * shadowScale
        shadowOffsetX = 8f * shadowScale
        shadowOffsetY = 10f * shadowScale
        beginPath()
        moveTo(CX - RX - 4f, BaseY - 4f)
        lineTo(CX + RX + 4f, BaseY - 4f)
        lineTo(CX + RX - 4f, BaseY + 12f)
        lineTo(CX - RX + 4f, BaseY + 12f)
        closePath()
        g = createLinearGradient(CX - RX, 0f, CX + RX, 0f)
        g.addColorStop(0f, "#8a6018")
        g.addColorStop(.18f, "#f0cf82")
        g.addColorStop(.35f, "#d1a24a")
        g.addColorStop(.75f, "#9d711f")
        g.addColorStop(1f, "#5c3e0b")
        fillStyle = g
        fill()
        restore()
        fillStyle = "rgba(255,245,210,.55)"
        fillRect(CX - RX - 3f, BaseY - 4f, RX * 2f + 6f, 1.4f)
    }

    // ---- The linen cover ----
    val sway = s.sway
    val ph = s.phase
    save()
    shadowColor = if (dark) "rgba(0,0,0,.5)" else "rgba(80,55,25,.28)"
    shadowBlur = 16f * shadowScale
    shadowOffsetX = 8f * shadowScale
    shadowOffsetY = 10f * shadowScale
    clothPath(hy, sway, ph, s.gather, s.ripple)
    fillStyle = if (dark) "#a99c80" else "#e9dfc8"
    fill()
    restore()
    save()
    clothPath(hy, sway, ph, s.gather, s.ripple)
    clip()
    fillStyle = weave
    fillRect(CX - 120f, TopY - 10f, 240f, 240f)
    // Lit from the window on the left, falling into shade on the right.
    g = createLinearGradient(CX - RX - 10f, 0f, CX + RX + 10f, 0f)
    g.addColorStop(0f, "rgba(255,248,228,.55)")
    g.addColorStop(.35f, "rgba(255,248,228,.12)")
    g.addColorStop(.7f, "rgba(90,60,30,.12)")
    g.addColorStop(1f, "rgba(70,45,20,.42)")
    fillStyle = g
    fillRect(CX - 120f, TopY - 10f, 240f, 240f)
    // A soft highlight where the cloth rounds over the dome.
    g = createRadialGradient(CX - 26f, TopY + 22f, 2f, CX - 26f, TopY + 22f, 60f)
    g.addColorStop(0f, "rgba(255,252,240,.5)")
    g.addColorStop(1f, "rgba(255,252,240,0)")
    fillStyle = g
    fillRect(CX - 120f, TopY - 10f, 240f, 160f)
    // Pleats: folds that start as a point under the dome ring and open out to the hem. Drawn on
    // their own layer, faded in over their top 30 px, and the whole layer fades in as the cover
    // comes down.
    val yT = DomeY + 6f
    val pleatA = ((hy - yT - 6f) / 40f).coerceIn(0f, 1f)
    if (pleatA > 0f) {
        beginLayer(pleatA)
        val pleats = 7
        val yB = hy + 6f
        val pw = 9f
        for (i in 0 until pleats) {
            val u = (i + .5f) / pleats
            val xTop = CX + (u - .5f) * RX * 1.6f
            val xHem = hemX(hy, .5f + (u - .5f) * 1.025f, 0f, sway, ph)
            // Sheared so the band's cross-section gradient follows the fold from apex to hem.
            val sh = (xHem - xTop) / (yB - yT)
            save()
            transform(1f, 0f, sh, 1f, xTop - sh * yT, 0f)
            g = createLinearGradient(-pw, 0f, pw, 0f)
            g.addColorStop(0f, "rgba(255,250,235,.0)")
            g.addColorStop(.3f, "rgba(255,250,235,.24)")
            g.addColorStop(.62f, "rgba(70,45,15,.15)")
            g.addColorStop(1f, "rgba(70,45,15,0)")
            fillStyle = g
            beginPath()
            moveTo(0f, yT)
            lineTo(pw, yB)
            lineTo(-pw, yB)
            closePath()
            fill()
            restore()
        }
        globalCompositeOperation = "destination-in"
        g = createLinearGradient(0f, yT, 0f, yT + 30f)
        g.addColorStop(0f, "rgba(0,0,0,0)")
        g.addColorStop(1f, "rgba(0,0,0,1)")
        fillStyle = g
        fillRect(CX - 120f, yT - 2f, 240f, yB - yT + 10f)
        endLayer()
    }
    // A faint stitched band near the hem.
    setLineDash(Dash)
    strokeStyle = "rgba(120,85,40,.35)"
    lineWidth = .9f
    beginPath()
    moveTo(CX - halfW(hy - 12f), hy - 12f)
    lineTo(CX + halfW(hy - 12f), hy - 12f)
    stroke()
    setLineDash(NoDash)
    restore()
    // Rolled hem: a fat linen tube that's easy to grab.
    val hw = halfW(hy)
    val roll = lerp(11f, 7f, c.coerceIn(0f, 1f)) + s.gather * 2.5f
    beginPath()
    for (i in 0..18) {
        val u = i / 18f
        val x = hemX(hy, u, (roll + 1.5f) / 2f, sway, ph)
        val y = hy + hemDY(u, sway, ph, s.gather, s.ripple) - 2f
        if (i > 0) lineTo(x, y) else moveTo(x, y)
    }
    lineCap = "round"
    lineJoin = "round"
    strokeStyle = if (dark) "#695b42" else "#a8946d"
    lineWidth = roll + 1.5f
    stroke()
    strokeStyle = if (dark) "#a19477" else "#ddd0b2"
    lineWidth = roll
    stroke()
    save()
    translate(-.6f, -roll * .22f)
    strokeStyle = "rgba(255,251,238,.75)"
    lineWidth = roll * .35f
    stroke()
    restore()
    // Muted: messages only stir the cloth, with a tiny "z" breathing out from under the hem.
    if (covered || c > .9f) {
        val sleepy = s.sleep.coerceIn(0f, 1f)
        if (sleepy > .3f) {
            val p = (s.time % 2600f) / 2600f
            save()
            globalAlpha = sin(p * Pi) * (if (dark) .55f else .75f) * sleepy
            fillStyle = if (dark) "#e6d7b0" else "#8a7a5e"
            // The prototype's 10 + 4p px, as the 10 px "z" scaled about where it stands, so its
            // layout is measured once.
            font = ZFont
            textAlign = "left"
            translate(CX + hw + 4f + p * 8f, hy - 30f - p * 22f)
            val k = (10f + p * 4f) / 10f
            scale(k, k)
            fillText("z", 0f, 0f)
            restore()
        }
    }
    endLayer()
}

/** The cover's outline, from the crown down the left side, along the hem and back up the right. */
private fun Canvas2D.clothPath(hy: Float, sway: Float, ph: Float, gather: Float, ripple: Float) {
    beginPath()
    // Sampled densely near the crown (t²), where the dome outline turns fastest, so the top is
    // round.
    val steps = 48
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val y = lerp(ClothY0, hy, t * t)
        if (i > 0) lineTo(clothSide(y, -1, sway, ph), y) else moveTo(clothSide(y, -1, sway, ph), y)
    }
    val n = 18
    for (i in 0..n) {
        val u = i / n.toFloat()
        lineTo(hemX(hy, u, 0f, sway, ph), hy + hemDY(u, sway, ph, gather, ripple))
    }
    for (i in steps downTo 0) {
        val t = i / steps.toFloat()
        val y = lerp(ClothY0, hy, t * t)
        lineTo(clothSide(y, 1, sway, ph), y)
    }
    closePath()
}
