package dev.dimvlachos.lab.core.presentation.components.pencilcup

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// Ported call by call from the prototype's draw(), pencil(), highlighter(), scissors(), loupe(),
// cupFront() and drawShadowCapsule(), in dp (the prototype's CSS px) under a scale of the density.

private const val Pi = PI.toFloat()

/** The families the tools' printing names: the pencil's stamp and the highlighter's GLOW. */
internal const val MonoFont = "JetBrains Mono"
internal const val SansFont = "Archivo"

/**
 * Speckles in the glaze, placed once with the prototype's fixed seed so they never crawl: where (u,
 * v), how big, and their colour, dark or pale.
 */
private class Speck(val u: Float, val v: Float, val size: Float, val color: Color)

private val Specks: List<Speck> = run {
    var s = 11L
    fun r(): Float {
        s = (s * 16807L) % 2147483647L
        return (s.toDouble() / 2147483647.0).toFloat()
    }
    List(70) {
        val u = r()
        val v = r()
        val size = .4f + r() * .9f
        val a = r()
        val color =
            if (a < .5f) Color(10, 15, 40, alpha255(.35f + a * .4f))
            else Color(220, 235, 255, alpha255(.12f + a * .18f))
        Speck(u, v, size, color)
    }
}

// As the CSS rgba() alpha lands in a canvas: rounded to a byte.
private fun alpha255(a: Float) = (a.coerceIn(0f, 1f) * 255f).toInt()

private fun Canvas2D.lin(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    vararg stops: Pair<Float, String>,
): Gradient2D {
    val g = createLinearGradient(x0, y0, x1, y1)
    for ((o, col) in stops) g.addColorStop(o, col)
    return g
}

private fun Canvas2D.rr(x: Float, y: Float, w: Float, h: Float, radius: Float) {
    val r = min(radius, min(w / 2, h / 2))
    beginPath()
    moveTo(x + r, y)
    arcTo(x + w, y, x + w, y + h, r)
    arcTo(x + w, y + h, x, y + h, r)
    arcTo(x, y + h, x, y, r)
    arcTo(x, y, x + w, y, r)
    closePath()
}

private fun Canvas2D.pencil(len: Float, w: Float, k: Float, dark: Boolean) {
    val kk = k
    rr(-w / 2, -9 * kk, w, 9.5f * kk, 3 * kk)
    fillStyle = lin(-w / 2, 0f, w / 2, 0f, 0f to "#c95f72", .35f to "#f6a7b4", 1f to "#d77588")
    fill()
    fillStyle =
        lin(
            -w / 2,
            0f,
            w / 2,
            0f,
            0f to "#7b7468",
            .3f to "#efe9da",
            .6f to "#b9b09b",
            1f to "#6c6556",
        )
    fillRect(-w / 2 - .4f, -17 * kk, w + .8f, 8.5f * kk)
    strokeStyle = "rgba(60,50,35,.45)"
    lineWidth = .7f
    for (j in 0 until 3) {
        val y = -10.5f * kk - j * 2.3f * kk
        beginPath()
        moveTo(-w / 2, y)
        lineTo(w / 2, y)
        stroke()
    }
    val yB = -17 * kk
    val yT = -(len - 21 * kk)
    val f = w / 6
    // The paint's top edge is the same scallop the wood starts on, so no background shows between.
    fun scallop() {
        quadraticCurveTo(-w / 3, yT - 3 * kk, -f, yT - 1 * kk)
        quadraticCurveTo(0f, yT - 3.5f * kk, f, yT - 1 * kk)
        quadraticCurveTo(w / 3, yT - 3 * kk, w / 2, yT)
    }
    save()
    beginPath()
    moveTo(-w / 2, yB)
    lineTo(-w / 2, yT)
    scallop()
    lineTo(w / 2, yB)
    closePath()
    clip()
    val yP = yT - 4 * kk
    fillStyle = "#d99a12"
    fillRect(-w / 2, yP, w / 2 - f, yB - yP)
    fillStyle = "#ffd447"
    fillRect(-f, yP, 2 * f, yB - yP)
    fillStyle = "#efb21f"
    fillRect(f, yP, w / 2 - f, yB - yP)
    fillStyle = "rgba(255,250,220,.55)"
    fillRect(-f * .6f, yP, f * .5f, yB - yP)
    restore()
    save()
    rotate(-Pi / 2)
    font = "700 ${5.6f * kk}px $MonoFont"
    textAlign = "left"
    fillStyle = "rgba(40,60,30,.65)"
    fillText("No. 2  HB", -yB + 14 * kk, 2 * kk)
    restore()
    // Sharpened wood with scalloped edges, then the graphite point.
    val tip = -(len - 3.5f * kk)
    beginPath()
    moveTo(-w / 2, yT)
    scallop()
    lineTo(1.6f * kk, tip)
    lineTo(-1.6f * kk, tip)
    closePath()
    fillStyle = lin(-w / 2, 0f, w / 2, 0f, 0f to "#c99b66", .4f to "#f3d6ab", 1f to "#d1a674")
    fill()
    beginPath()
    moveTo(-1.6f * kk, tip)
    lineTo(0f, -len)
    lineTo(1.6f * kk, tip)
    closePath()
    fillStyle = if (dark) "#5c5c66" else "#34343a"
    fill()
}

private fun Canvas2D.highlighter(len: Float, w: Float, k: Float) {
    val kk = k
    val capY = -len + 34 * kk
    rr(-w / 2, capY - 2 * kk, w, -capY + 2 * kk, 6 * kk)
    fillStyle =
        lin(
            -w / 2,
            0f,
            w / 2,
            0f,
            0f to "#c23c78",
            .32f to "#ff8cc1",
            .5f to "#ff5fa6",
            1f to "#c4407d",
        )
    fill()
    fillStyle = "rgba(255,255,255,.82)"
    fillRect(-w / 2, -24 * kk, w, 15 * kk)
    font = "800 ${5.2f * kk}px $SansFont"
    fillStyle = "#d13f83"
    textAlign = "center"
    save()
    rotate(-Pi / 2)
    fillText("GLOW", 16.5f * kk, 1.8f * kk)
    restore()
    textAlign = "left"
    rr(-w / 2 - 1 * kk, -len, w + 2 * kk, 36 * kk, 6 * kk)
    fillStyle =
        lin(
            -w / 2,
            0f,
            w / 2,
            0f,
            0f to "#9e2a5e",
            .3f to "#ff7db8",
            .55f to "#ee4b93",
            1f to "#a32f64",
        )
    fill()
    fillStyle = "rgba(255,255,255,.35)"
    fillRect(-w * .22f, -len + 4 * kk, w * .12f, 28 * kk)
    // Pocket clip.
    rr(w / 2 - 3.5f * kk, -len + 4 * kk, 4.5f * kk, 28 * kk, 2 * kk)
    fillStyle = lin(w / 2 - 4 * kk, 0f, w / 2 + 2 * kk, 0f, 0f to "#b23a70", 1f to "#ff9fcb")
    fill()
}

private fun Canvas2D.scissors(len: Float, k: Float) {
    val kk = k
    val pv = -.5f * len
    // Closed blades, points down in the cup.
    beginPath()
    moveTo(-5 * kk, pv)
    quadraticCurveTo(-3.5f * kk, pv * .45f, -.6f * kk, 0f)
    lineTo(.4f * kk, 0f)
    quadraticCurveTo(4 * kk, pv * .5f, 5 * kk, pv)
    closePath()
    fillStyle =
        lin(
            -5 * kk,
            0f,
            5 * kk,
            0f,
            0f to "#7c848c",
            .35f to "#f2f5f7",
            .55f to "#aab3bb",
            1f to "#5d656d",
        )
    fill()
    strokeStyle = "rgba(40,46,52,.5)"
    lineWidth = .7f
    beginPath()
    moveTo(0f, pv)
    quadraticCurveTo(.4f * kk, pv * .5f, 0f, 0f)
    stroke()
    // Shanks and handle loops.
    fun loop(cx: Float, cy: Float, rx: Float, ry: Float) {
        beginPath()
        ellipse(cx, cy, rx, ry, 0f, 0f, Pi * 2)
        ellipse(cx, cy, rx * .56f, ry * .62f, 0f, 0f, Pi * 2, true)
        fillStyle =
            lin(
                cx - rx,
                cy - ry,
                cx + rx,
                cy + ry,
                0f to "#ff9c55",
                .5f to "#f0701f",
                1f to "#b44a0c",
            )
        fill("evenodd")
    }
    fillStyle = "#d8601a"
    beginPath()
    moveTo(-4.5f * kk, pv - 2 * kk)
    lineTo(-7 * kk, -len + 18 * kk)
    lineTo(-3 * kk, -len + 18 * kk)
    lineTo(1 * kk, pv - 2 * kk)
    closePath()
    fill()
    beginPath()
    moveTo(4.5f * kk, pv - 2 * kk)
    lineTo(7 * kk, -len + 22 * kk)
    lineTo(3 * kk, -len + 22 * kk)
    lineTo(-1 * kk, pv - 2 * kk)
    closePath()
    fill()
    loop(-6.5f * kk, -len + 10.5f * kk, 6.8f * kk, 10.5f * kk)
    loop(6.8f * kk, -len + 13 * kk, 6.4f * kk, 9.8f * kk)
    val g = createRadialGradient(-.8f * kk, pv - .8f * kk, 0f, 0f, pv, 3 * kk)
    g.addColorStop(0f, "#ffffff")
    g.addColorStop(1f, "#6d757c")
    fillStyle = g
    beginPath()
    arc(0f, pv, 2.6f * kk, 0f, Pi * 2)
    fill()
}

private fun Canvas2D.loupe(len: Float, k: Float) {
    val kk = k
    val hl = .6f * len
    val cy = -(len - 20 * kk)
    val r = 19.5f * kk
    rr(-5 * kk, -hl, 10 * kk, hl, 5 * kk)
    fillStyle =
        lin(
            -5 * kk,
            0f,
            5 * kk,
            0f,
            0f to "#3e2715",
            .35f to "#9a6a43",
            .6f to "#6f4628",
            1f to "#331f10",
        )
    fill()
    fillStyle = lin(-6 * kk, 0f, 6 * kk, 0f, 0f to "#7c5615", .35f to "#f7dc8f", 1f to "#8a6119")
    fillRect(-6 * kk, -hl - 6 * kk, 12 * kk, 7 * kk)
    fillRect(-2.6f * kk, cy + r, 5.2f * kk, -hl - 6 * kk - cy - r + 1)
    // Glass: a cool lens with the lamp in it.
    val g = createRadialGradient(-r * .35f, cy - r * .4f, 1f, 0f, cy, r)
    g.addColorStop(0f, "rgba(255,255,255,.9)")
    g.addColorStop(.35f, "rgba(214,232,240,.55)")
    g.addColorStop(1f, "rgba(120,160,180,.45)")
    fillStyle = g
    beginPath()
    arc(0f, cy, r, 0f, Pi * 2)
    fill()
    strokeStyle = lin(-r, cy - r, r, cy + r, 0f to "#fbe7ae", .4f to "#c89a40", 1f to "#6b4a10")
    lineWidth = 4.2f * kk
    beginPath()
    arc(0f, cy, r, 0f, Pi * 2)
    stroke()
    strokeStyle = "rgba(255,255,255,.85)"
    lineWidth = 1.6f * kk
    beginPath()
    arc(0f, cy, r * .7f, Pi * 1.08f, Pi * 1.42f)
    stroke()
}

private fun Canvas2D.tool(i: Int, len: Float, w: Float, k: Float, dark: Boolean) =
    when (i) {
        CupTools.Note -> pencil(len, w, k, dark)
        CupTools.List -> highlighter(len, w, k)
        CupTools.Clip -> scissors(len, k)
        else -> loupe(len, k)
    }

/**
 * The soft shadow a raised tool casts on the page, down and to the right of the lamp, whatever its
 * angle: the capsule's shadow alone, offset in device pixels as the prototype's are (its values
 * times its pixel ratio, here the [density]).
 */
private fun Canvas2D.shadowCapsule(
    len: Float,
    w: Float,
    lift: Float,
    k: Float,
    dark: Boolean,
    density: Float,
) {
    if (lift < .02f) return
    save()
    setShadowColor(Color(40, 25, 10, alpha255((if (dark) .5f else .26f) * lift)))
    shadowBlur = 10 * k * density
    shadowOffsetX = 6 * k * lift * density
    shadowOffsetY = 9 * k * lift * density
    lineCap = "round"
    lineWidth = w
    beginPath()
    moveTo(0f, -2f)
    lineTo(0f, -len + 4)
    strokeShadow()
    restore()
}

private fun Canvas2D.cupFront(s: PencilCupState) {
    val cx = s.cupX
    beginPath()
    moveTo(cx - s.topWidth / 2, s.rimY)
    lineTo(cx - s.footWidth / 2, s.botY)
    ellipse(cx, s.botY, s.footWidth / 2, s.rimDepth * .85f, 0f, Pi, 0f, true)
    lineTo(cx + s.topWidth / 2, s.rimY)
    ellipse(cx, s.rimY, s.topWidth / 2, s.rimDepth, 0f, 0f, Pi, false)
    closePath()
}

/**
 * The cup layer: the lamp's pool and the cup's contact shadow on the page, the cup's inside and
 * back rim, the tools in it or fanned out, and the glazed front wall over whatever is below the
 * rim. In dp, under a scale of the [density].
 */
internal fun Canvas2D.drawPencilCup(s: PencilCupState, dark: Boolean, density: Float) {
    val k = s.k
    val cx = s.cupX
    val rimY = s.rimY
    val botY = s.botY
    val wT = s.topWidth
    val ry = s.rimDepth
    val press = s.press
    val rock = s.rock

    // Warm lamp pool from the top left, soft on the paper around the cup.
    save()
    translate(cx - 18 * k, botY - 56 * k)
    scale(1.35f, 1f)
    var g = createRadialGradient(0f, 0f, 4f, 0f, 0f, 100 * k)
    g.addColorStop(0f, if (dark) "rgba(255,190,110,.13)" else "rgba(255,214,150,.3)")
    g.addColorStop(1f, "rgba(255,214,150,0)")
    fillStyle = g
    fillRect(-130 * k, -130 * k, 260 * k, 260 * k)
    restore()
    // Contact shadow under the cup, cast down and to the right.
    save()
    translate(cx + 9 * k, botY + 2 * k)
    scale(1f, .22f)
    g = createRadialGradient(0f, 0f, 2f, 0f, 0f, wT * .9f)
    g.addColorStop(0f, if (dark) "rgba(0,0,0,.6)" else "rgba(60,40,20,.38)")
    g.addColorStop(1f, "rgba(60,40,20,0)")
    fillStyle = g
    beginPath()
    arc(0f, 0f, wT * .9f, 0f, Pi * 2)
    fill()
    restore()

    // The whole cup squashes a touch when pressed and rocks when tools land.
    save()
    translate(cx, botY)
    rotate(rock * .06f)
    scale(1 + press * .04f, 1 - press * .06f)
    translate(-cx, -botY)

    // Inside of the cup.
    beginPath()
    ellipse(cx, rimY, wT / 2 - 1.5f * k, ry - .8f * k, 0f, 0f, Pi * 2)
    g = createLinearGradient(0f, rimY - ry, 0f, rimY + ry)
    g.addColorStop(0f, "#0b1636")
    g.addColorStop(1f, "#22356a")
    fillStyle = g
    fill()
    // Back half of the rim, behind the tools.
    lineWidth = 2.6f * k
    strokeStyle = "rgba(200,210,235,.6)"
    beginPath()
    ellipse(cx, rimY, wT / 2 - 1 * k, ry, 0f, Pi, Pi * 2)
    stroke()

    // Tools, back to front. The front wall covers whatever is below the rim.
    for (i in 0 until CupTools.count) {
        val t = s.tools[i]
        if (t.mode == ToolMode.Away || t.mode == ToolMode.Fly) continue
        val spec = CupTools.specs[i]
        save()
        translate(t.x, t.y)
        rotate(t.a)
        scale(t.s, t.s)
        val lift = ((s.restY() - t.y) / (60 * k)).coerceIn(0f, 1f)
        shadowCapsule(spec.length * k, spec.width * k * .8f, lift, k, dark, density)
        tool(i, spec.length * k, spec.width * k, k, dark)
        restore()
    }

    // Front wall: clay first, then the glaze stopping short of the foot in drips.
    save()
    cupFront(s)
    clip()
    fillStyle =
        lin(cx - wT / 2, 0f, cx + wT / 2, 0f, 0f to "#9a7b5c", .35f to "#e2c7a2", 1f to "#86684b")
    fillRect(cx - wT, rimY - ry * 2, wT * 2, botY - rimY + ry * 4)
    beginPath()
    moveTo(cx - wT, rimY - ry * 2)
    val footY = botY - 11 * k
    for (j in 0..24) {
        val u = j / 24f
        val x = cx - wT / 2 + u * wT
        val drip = max(0f, sin(u * 19.3f + 1.2f)).pow(6) * 7 * k + sin(u * 7.1f) * 1.4f * k
        lineTo(x, footY + drip)
    }
    lineTo(cx + wT, rimY - ry * 2)
    closePath()
    fillStyle =
        lin(
            cx - wT / 2,
            0f,
            cx + wT / 2,
            0f,
            0f to "#0d1d4a",
            .18f to "#2b4ea6",
            .32f to "#5d85dc",
            .42f to "#2f55b0",
            .75f to "#1c3a86",
            1f to "#0c1840",
        )
    fill()
    save()
    clip()
    g = createLinearGradient(0f, rimY, 0f, botY)
    g.addColorStop(0f, "rgba(160,190,255,.18)")
    g.addColorStop(.5f, "rgba(0,0,0,0)")
    g.addColorStop(1f, "rgba(0,0,20,.3)")
    fillStyle = g
    fillRect(cx - wT, rimY - ry, wT * 2, botY - rimY + ry * 2)
    for (sp in Specks) {
        val x = cx - wT / 2 + sp.u * wT
        val y = rimY + sp.v * (botY - rimY)
        fillStyle = sp.color
        beginPath()
        arc(x, y, sp.size * k * .8f, 0f, Pi * 2)
        fill()
    }
    // Glossy highlight band, where the lamp sits in the glaze; clipped to the glaze so it ends in
    // the drips.
    g = createLinearGradient(cx - wT * .36f, 0f, cx - wT * .14f, 0f)
    g.addColorStop(0f, "rgba(255,255,255,0)")
    g.addColorStop(.5f, "rgba(255,255,255,.5)")
    g.addColorStop(1f, "rgba(255,255,255,0)")
    fillStyle = g
    fillRect(cx - wT * .36f, rimY + ry * .6f, wT * .22f, footY - rimY - ry * .6f + 10 * k)
    restore()
    // Hand-painted white slip plus on the front.
    val px = cx + 3 * k
    val py = rimY + (footY - rimY) * .5f
    strokeStyle = "rgba(250,246,236,.95)"
    lineCap = "round"
    lineWidth = 3.6f * k
    beginPath()
    moveTo(px - 9 * k, py + .6f * k)
    quadraticCurveTo(px, py - 1.2f * k, px + 9.5f * k, py - .2f * k)
    stroke()
    beginPath()
    moveTo(px - .4f * k, py - 9.5f * k)
    quadraticCurveTo(px + 1.2f * k, py, px + .2f * k, py + 9 * k)
    stroke()
    restore()
    // Rim: the glaze thins to pale clay at the lip (front half; the back half is behind the tools).
    lineWidth = 2.6f * k
    strokeStyle = "rgba(232,220,200,.95)"
    beginPath()
    ellipse(cx, rimY, wT / 2 - 1 * k, ry, 0f, 0f, Pi)
    stroke()
    restore()
}

/** The tool flying up into the new sheet, on its own layer above the sheet. */
internal fun Canvas2D.drawPencilCupFlight(s: PencilCupState, dark: Boolean, density: Float) {
    val k = s.k
    for (i in 0 until CupTools.count) {
        val t = s.tools[i]
        if (t.mode != ToolMode.Fly || t.al <= 0f) continue
        val spec = CupTools.specs[i]
        save()
        globalAlpha = t.al
        translate(t.x, t.y)
        rotate(t.a)
        scale(t.s, t.s)
        shadowCapsule(spec.length * k, spec.width * k * .8f, 1f, k, dark, density)
        tool(i, spec.length * k, spec.width * k, k, dark)
        restore()
    }
}
