package dev.dimvlachos.lab.core.presentation.components.crtpip

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipState.Companion.bez
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipState.Companion.clamp01
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipState.Companion.ease
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipState.Companion.easeIn3
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipState.Companion.easeOut3
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipState.Companion.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val Tau = (2 * PI).toFloat()
private const val Pi = PI.toFloat()

/**
 * The gradients the painters fill every frame, built once for the sizes they were made at: the
 * show's (in its own space, so only its size matters), the set's (drawn from its own corner, so
 * only its unit matters), the line's and the dot's (drawn about their centre, at unit size).
 */
internal class CrtPipPaints {
    private var showW = -1f
    private var showH = -1f
    lateinit var wall: Gradient2D
    lateinit var wallLight: Gradient2D
    lateinit var counter: Gradient2D
    lateinit var bowl: Gradient2D
    lateinit var bread: Gradient2D
    lateinit var vignette: Gradient2D
    var bugFont = ""
        private set

    fun show(w: Float, h: Float) {
        if (w == showW && h == showH) return
        showW = w
        showH = h
        bugFont = "800 ${9.5f * w / 360}px system-ui"
        wall =
            Gradient2D.linear(0f, 0f, w, h * .7f).apply {
                addColorStop(0f, "#f3e8d4")
                addColorStop(1f, "#d9c2a0")
            }
        wallLight =
            Gradient2D.linear(0f, 0f, w * .6f, h * .6f).apply {
                addColorStop(0f, "rgba(255,250,232,.45)")
                addColorStop(1f, "rgba(255,250,232,0)")
            }
        val cy0 = h * .62f
        counter =
            Gradient2D.linear(0f, cy0, 0f, h * 1.1f).apply {
                addColorStop(0f, "#c18a57")
                addColorStop(1f, "#7f5130")
            }
        bowl =
            Gradient2D.linear(0f, h * .7f, 0f, h * .87f).apply {
                addColorStop(0f, "#3f6f9d")
                addColorStop(1f, "#244a6f")
            }
        val cx = w * .53f
        val cy = h * .73f
        val rx = w * .25f
        val ry = h * .16f
        val bx = cx
        val by = cy + ry * .06f
        val brx = rx * .84f
        val bry = ry * .76f
        bread =
            Gradient2D.radial(bx - brx * .2f, by - bry * .3f, 0f, bx, by, brx).apply {
                addColorStop(0f, "#f8cf6c")
                addColorStop(.6f, "#e1a443")
                addColorStop(1f, "#a86a25")
            }
        vignette =
            Gradient2D.radial(w / 2, h / 2, h * .3f, w / 2, h / 2, w * .62f).apply {
                addColorStop(0f, "rgba(0,0,0,0)")
                addColorStop(1f, "rgba(0,0,0,.3)")
            }
    }

    private var unit = -1f
    lateinit var floor: Gradient2D
    lateinit var body: Gradient2D
    lateinit var tube: Gradient2D
    lateinit var glare: Gradient2D
    lateinit var knobs: List<Gradient2D>

    /** The set's, drawn from its top-left corner at the origin. */
    fun cabinet(ck: Float) {
        if (ck == unit) return
        unit = ck
        val w = 150 * ck
        val h = 98 * ck
        floor =
            Gradient2D.radial(w / 2, h + 8 * ck, 0f, w / 2, h + 8 * ck, w * .6f).apply {
                addColorStop(0f, "rgba(30,18,8,.2)")
                addColorStop(1f, "rgba(30,18,8,0)")
            }
        body =
            Gradient2D.linear(0f, 0f, 0f, h).apply {
                addColorStop(0f, "#93603a")
                addColorStop(1f, "#5f3a20")
            }
        val sx = 10 * ck
        val sy = 10 * ck
        val sw = 98 * ck
        val sh = h - 20 * ck
        tube =
            Gradient2D.radial(
                    sx + sw / 2,
                    sy + sh / 2,
                    sh * .3f,
                    sx + sw / 2,
                    sy + sh / 2,
                    sw * .72f,
                )
                .apply {
                    addColorStop(0f, "rgba(0,0,0,0)")
                    addColorStop(1f, "rgba(0,0,0,.55)")
                }
        glare =
            Gradient2D.linear(sx, sy, sx + sw * .6f, sy + sh * .6f).apply {
                addColorStop(0f, "rgba(255,255,255,.22)")
                addColorStop(.5f, "rgba(255,255,255,.04)")
                addColorStop(1f, "rgba(255,255,255,0)")
            }
        val px = sx + sw + 10 * ck
        val pw = w - 10 * ck - px
        knobs =
            List(2) { i ->
                val kx = px + pw / 2
                val ky = sy + 40 * ck + i * 19 * ck
                val kr = 6.6f * ck
                Gradient2D.radial(kx - 2 * ck, ky - 2 * ck, 0f, kx, ky, kr).apply {
                    addColorStop(0f, "#f1e6d2")
                    addColorStop(1f, "#a89a84")
                }
            }
    }

    private var lineK = -1f
    lateinit var lineGlow: Gradient2D

    /** The line's bloom, about its centre line. */
    fun line(k: Float) {
        if (k == lineK) return
        lineK = k
        lineGlow =
            Gradient2D.linear(0f, -12 * k, 0f, 12 * k).apply {
                addColorStop(0f, "rgba(150,200,255,0)")
                addColorStop(.5f, "rgba(170,215,255,.55)")
                addColorStop(1f, "rgba(150,200,255,0)")
            }
    }

    // The dot's, about its centre at unit radius, for each theme.
    val dusk =
        Gradient2D.radial(0f, 0f, 0f, 0f, 0f, 3.4f).apply {
            addColorStop(0f, "rgba(14,22,48,.55)")
            addColorStop(.5f, "rgba(14,22,48,.22)")
            addColorStop(1f, "rgba(14,22,48,0)")
        }
    val bloomDark =
        Gradient2D.radial(0f, 0f, 0f, 0f, 0f, 5.5f).apply {
            addColorStop(0f, "rgba(120,190,255,.75)")
            addColorStop(.35f, "rgba(70,150,255,.3)")
            addColorStop(1f, "rgba(60,140,255,0)")
        }
    val bloomLight =
        Gradient2D.radial(0f, 0f, 0f, 0f, 0f, 5.5f).apply {
            addColorStop(0f, "rgba(40,130,255,.85)")
            addColorStop(.35f, "rgba(40,120,255,.42)")
            addColorStop(1f, "rgba(40,120,255,0)")
        }
    val core =
        Gradient2D.radial(0f, 0f, 0f, 0f, 0f, 1.6f).apply {
            addColorStop(0f, "#ffffff")
            addColorStop(.55f, "rgba(235,246,255,.95)")
            addColorStop(1f, "rgba(160,210,255,0)")
        }
}

private fun Canvas2D.rr(x: Float, y: Float, w: Float, h: Float, r: Float) {
    beginPath()
    roundRect(x, y, w, h, r)
}

private fun Canvas2D.fullEllipse(x: Float, y: Float, rx: Float, ry: Float, rot: Float = 0f) {
    beginPath()
    ellipse(x, y, rx, ry, rot, 0f, Tau)
}

private val Jars = listOf("#f4efe6" to "#c9473a", "#8a5a36" to "#3c6e8f", "#eac25a" to "#d9a441")
private val CrumbDark = Color(150, 90, 30, (.35f * 255).toInt())
private val CrumbLight = Color(255, 235, 170, (.45f * 255).toInt())

/**
 * A drawn cooking show, Marlow Kitchen: honey cornbread in a cast-iron skillet, steam, a slow
 * camera drift, at [t] seconds into the show; [w] by [h] in its own units, as the prototype's
 * `drawShow`.
 */
internal fun Canvas2D.drawShow(w: Float, h: Float, t: Float, paints: CrtPipPaints) {
    paints.show(w, h)
    val u = w / 360
    save()
    fillStyle = "#d9c6a6"
    fillRect(0f, 0f, w, h)
    val z = 1.06f + .035f * sin(t * .21f)
    val px = sin(t * .13f) * w * .025f
    translate(w / 2 + px, h / 2)
    scale(z, z)
    translate(-w / 2, -h / 2)
    // Tiled wall, warm light from the left.
    fillStyle = paints.wall
    fillRect(-w * .1f, -h * .1f, w * 1.2f, h * .78f)
    strokeStyle = "rgba(120,92,58,.2)"
    lineWidth = 1 * u
    val th = h * .085f
    var r = 0
    var y = -h * .1f
    while (y < h * .64f) {
        beginPath()
        moveTo(-w * .1f, y)
        lineTo(w * 1.1f, y)
        stroke()
        var x = -w * .1f + (r % 2) * th
        while (x < w * 1.1f) {
            beginPath()
            moveTo(x, y)
            lineTo(x, y + th)
            stroke()
            x += th * 2
        }
        r++
        y += th
    }
    fillStyle = paints.wallLight
    beginPath()
    moveTo(-w * .1f, -h * .1f)
    lineTo(w * .45f, -h * .1f)
    lineTo(w * .1f, h * .64f)
    lineTo(-w * .1f, h * .64f)
    fill()
    // Shelf of jars.
    fillStyle = "#8c5c35"
    fillRect(w * .04f, h * .26f, w * .4f, h * .03f)
    for (i in Jars.indices) {
        val (jar, lid) = Jars[i]
        val jx = w * (.07f + i * .12f)
        val jw = w * .085f
        val jy = h * .1f
        fillStyle = "rgba(255,255,255,.35)"
        rr(jx, jy, jw, h * .16f, 4 * u)
        fill()
        fillStyle = jar
        rr(jx + 2 * u, jy + h * .06f, jw - 4 * u, h * .095f, 3 * u)
        fill()
        fillStyle = lid
        rr(jx - 1 * u, jy - h * .02f, jw + 2 * u, h * .035f, 2 * u)
        fill()
    }
    // Butcher-block counter.
    val cy0 = h * .62f
    fillStyle = paints.counter
    fillRect(-w * .1f, cy0, w * 1.2f, h * .5f)
    strokeStyle = "rgba(70,40,18,.22)"
    lineWidth = 1.2f * u
    var gx = -w * .1f
    while (gx < w * 1.1f) {
        beginPath()
        moveTo(gx, cy0)
        lineTo(gx - w * .03f, h * 1.1f)
        stroke()
        gx += w * .11f
    }
    fillStyle = "rgba(255,235,200,.25)"
    fillRect(-w * .1f, cy0, w * 1.2f, 2 * u)
    // Gingham towel.
    save()
    beginPath()
    moveTo(w * .76f, h * .7f)
    lineTo(w * 1.05f, h * .68f)
    lineTo(w * 1.05f, h * .95f)
    lineTo(w * .8f, h * .97f)
    closePath()
    clip()
    fillStyle = "#fbf6ef"
    fillRect(w * .7f, h * .6f, w * .4f, h * .5f)
    fillStyle = "rgba(200,40,40,.55)"
    var tx = w * .7f
    while (tx < w * 1.1f) {
        fillRect(tx, h * .6f, 6 * u, h * .5f)
        tx += 12 * u
    }
    var ty = h * .6f
    while (ty < h * 1.1f) {
        fillRect(w * .7f, ty, w * .4f, 6 * u)
        ty += 12 * u
    }
    restore()
    // Blue enamel bowl with a whisk.
    fillStyle = "rgba(40,20,8,.25)"
    fullEllipse(w * .15f, h * .86f, w * .11f, h * .04f)
    fill()
    strokeStyle = "#b7b2ab"
    lineWidth = 2.4f * u
    beginPath()
    moveTo(w * .17f, h * .7f)
    lineTo(w * .27f, h * .5f)
    stroke()
    fillStyle = paints.bowl
    beginPath()
    ellipse(w * .15f, h * .71f, w * .11f, h * .045f, 0f, 0f, Pi)
    lineTo(w * .07f, h * .78f)
    quadraticCurveTo(w * .15f, h * .9f, w * .23f, h * .78f)
    closePath()
    fill()
    fillStyle = "#f2efe8"
    fullEllipse(w * .15f, h * .71f, w * .11f, h * .04f)
    fill()
    fillStyle = "#e9d39a"
    fullEllipse(w * .15f, h * .715f, w * .092f, h * .03f)
    fill()
    // The skillet.
    val cx = w * .53f
    val cy = h * .73f
    val rx = w * .25f
    val ry = h * .16f
    fillStyle = "rgba(40,20,8,.35)"
    fullEllipse(cx + 6 * u, cy + ry * .55f, rx * 1.02f, ry * .7f)
    fill()
    strokeStyle = "#23201d"
    lineCap = "round"
    lineWidth = 15 * u
    beginPath()
    moveTo(cx + rx * .9f, cy - ry * .05f)
    lineTo(cx + rx * 1.55f, cy + ry * .28f)
    stroke()
    strokeStyle = "#4a4540"
    lineWidth = 2 * u
    beginPath()
    moveTo(cx + rx * 1.0f, cy - ry * .12f)
    lineTo(cx + rx * 1.5f, cy + ry * .14f)
    stroke()
    fillStyle = "#2a2724"
    fullEllipse(cx, cy + 4 * u, rx, ry * 1.08f)
    fill()
    fillStyle = "#3a3632"
    fullEllipse(cx, cy, rx, ry)
    fill()
    fillStyle = "#191715"
    fullEllipse(cx, cy + 1 * u, rx * .9f, ry * .86f)
    fill()
    val bx = cx
    val by = cy + ry * .06f
    val brx = rx * .84f
    val bry = ry * .76f
    fillStyle = paints.bread
    fullEllipse(bx, by, brx, bry)
    fill()
    // Crumb: a fixed scatter of darker and lighter flecks.
    for (i in 0 until 60) {
        val a = i * 2.399f
        val rr = sqrt((i * 37 % 60) / 60f)
        fillStyle = if (i % 3 != 0) CrumbDark else CrumbLight
        fullEllipse(
            bx + cos(a) * brx * rr * .92f,
            by + sin(a) * bry * rr * .92f,
            1.6f * u,
            1.1f * u,
        )
        fill()
    }
    strokeStyle = "rgba(120,70,22,.38)"
    lineWidth = 1.4f * u
    for (i in 0 until 8) {
        val a = i * Pi / 4 + .2f
        beginPath()
        moveTo(bx, by)
        lineTo(bx + cos(a) * brx * .97f, by + sin(a) * bry * .97f)
        stroke()
    }
    // Butter melting through a 10 s loop, and the honey drizzle in the middle of it.
    val c = t % 10
    val m = c / 10
    fillStyle = Color(255, 232, 140, ((.25f + .35f * m) * 255).toInt())
    fullEllipse(bx, by, brx * (.12f + .3f * m), bry * (.12f + .3f * m))
    fill()
    val bs = lerp(22f, 9f, m) * u
    save()
    translate(bx, by - 2 * u)
    rotate(.25f)
    fillStyle = "#fff3b4"
    rr(-bs / 2, -bs / 2 * .7f, bs, bs * .7f, 3 * u)
    fill()
    fillStyle = "rgba(255,255,255,.7)"
    rr(-bs / 2 + 2 * u, -bs / 2 * .7f + 1.5f * u, bs * .4f, 2.2f * u, 1 * u)
    fill()
    restore()
    val hp = ((c - 3) / 3.6f).coerceIn(0f, 1f)
    if (hp > 0) {
        val pool = ((c - 3.4f) / 4).coerceIn(0f, 1f)
        fillStyle = "rgba(214,140,30,.55)"
        beginPath()
        ellipse(
            bx - brx * .18f,
            by - bry * .12f,
            brx * .32f * pool,
            bry * .26f * pool,
            .3f,
            0f,
            Tau,
        )
        fill()
        fillStyle = "rgba(255,240,200,.6)"
        beginPath()
        ellipse(
            bx - brx * .26f,
            by - bry * .22f,
            brx * .07f * pool,
            bry * .04f * pool,
            .3f,
            0f,
            Tau,
        )
        fill()
    }
    if (hp > 0 && hp < 1) {
        val inn = easeOut3(clamp01(hp * 5)) * (1 - easeIn3(clamp01((hp - .8f) * 5)))
        val dx = bx - brx * .18f + sin(t * 2.1f) * 10 * u
        val dy = lerp(-h * .2f, h * .3f, inn)
        if (hp > .1f && hp < .86f) {
            strokeStyle = "rgba(222,150,36,.92)"
            lineWidth = 3 * u
            beginPath()
            moveTo(dx, dy + 12 * u)
            for (s in 1..10) {
                val yy = lerp(dy + 12 * u, by - bry * .12f, s / 10f)
                lineTo(lerp(dx, bx - brx * .18f, s / 10f) + sin(s * 1.3f + t * 6) * 1.2f * u, yy)
            }
            stroke()
        }
        strokeStyle = "#b47a43"
        lineWidth = 4 * u
        beginPath()
        moveTo(dx + 6 * u, -h * .2f)
        lineTo(dx, dy)
        stroke()
        fillStyle = "#c99252"
        fullEllipse(dx, dy + 5 * u, 9 * u, 11 * u)
        fill()
        strokeStyle = "rgba(90,50,20,.55)"
        lineWidth = 1.4f * u
        for (i in -2..2) {
            beginPath()
            moveTo(dx - 8.5f * u, dy + 5 * u + i * 4 * u)
            lineTo(dx + 8.5f * u, dy + 5 * u + i * 4 * u)
            stroke()
        }
        fillStyle = "rgba(222,150,36,.9)"
        fullEllipse(dx, dy + 15 * u, 4 * u, 3 * u)
        fill()
    }
    // Steam.
    lineCap = "round"
    for (i in 0 until 5) {
        val x0 = bx + (i - 2) * brx * .32f
        val y0 = by - bry * .3f
        val wobble = .6f + .4f * sin(t * .9f + i)
        for (s in 0 until 14) {
            val a = s / 14f
            val b = (s + 1) / 14f
            val ph = t * 1.4f + i * 1.7f
            val xa = x0 + sin(a * 5 + ph) * 12 * u * a
            val xb = x0 + sin(b * 5 + ph) * 12 * u * b
            strokeStyle = Color(1f, 1f, 1f, (.2f * (1 - a) * wobble).coerceIn(0f, 1f))
            lineWidth = (6 + 6 * a) * u
            beginPath()
            moveTo(xa, y0 - a * h * .45f)
            lineTo(xb, y0 - b * h * .45f)
            stroke()
        }
    }
    restore()
    // Channel bug, burned into the picture like a real broadcast, kept inside the 4:3-safe middle
    // so the set's crop doesn't cut it in half.
    font = paints.bugFont
    textAlign = "right"
    val bugR = min(w, w / 2 + h * .6f) - 12 * u
    fillStyle = "rgba(0,0,0,.35)"
    fillText("MARLOW KITCHEN", bugR + 1 * u, 21 * u)
    fillStyle = "rgba(255,255,255,.85)"
    fillText("MARLOW KITCHEN", bugR, 20 * u)
    textAlign = "left"
    fillStyle = paints.vignette
    fillRect(0f, 0f, w, h)
}

/**
 * The show, squeezed to [sy] of its height around ([cx], [cy]) and [sx] of its width, with white
 * added as it [bright]ens; [crop] takes the middle of the frame at the [w]:[h] of the set's tube.
 * The prototype draws a frame of the show and scales it; this draws the show itself under the same
 * scale, at full density.
 */
internal fun Canvas2D.picture(
    s: CrtPipState,
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    sy: Float,
    sx: Float,
    bright: Float,
    crop: Boolean,
    paints: CrtPipPaints,
) {
    val vw = s.width
    val vh = s.fullH
    val dw = w * sx
    val dh = max(.5f, h * sy)
    var srcX = 0f
    var srcW = vw
    if (crop) {
        srcW = vh * (w / h)
        srcX = (vw - srcW) / 2
    }
    val x0 = cx - dw / 2
    val y0 = cy - dh / 2
    save()
    beginPath()
    rect(x0, y0, dw, dh)
    clip()
    translate(x0, y0)
    scale(dw / srcW, dh / vh)
    translate(-srcX, 0f)
    drawShow(vw, vh, s.vt, paints)
    restore()
    if (bright > 0) {
        fillStyle = Color(1f, 1f, 1f, bright.coerceIn(0f, 1f))
        fillRect(x0, y0, dw, dh)
    }
}

/** A glowing horizontal line [w] wide at ([cx], [cy]), the last of the sweep. */
internal fun Canvas2D.crtLine(cx: Float, cy: Float, w: Float, k: Float, paints: CrtPipPaints) {
    paints.line(k)
    val hh = 3 * k
    save()
    translate(0f, cy)
    fillStyle = paints.lineGlow
    fillRect(cx - w / 2 - 6 * k, -12 * k, w + 12 * k, 24 * k)
    fillStyle = "#ffffff"
    rr(cx - w / 2, -hh / 2, w, hh, hh / 2)
    fill()
    restore()
}

/**
 * The phosphor dot: white core, blue-white bloom. On the light page a white core alone vanishes, so
 * the light theme adds a soft dusk of tube-black under the bloom and a deeper blue halo.
 */
internal fun Canvas2D.crtDot(
    x: Float,
    y: Float,
    r: Float,
    a: Float,
    dark: Boolean,
    paints: CrtPipPaints,
) {
    if (a <= 0f || r <= 0f) return
    save()
    globalAlpha = a
    translate(x, y)
    scale(r, r)
    if (!dark) {
        fillStyle = paints.dusk
        beginPath()
        arc(0f, 0f, 3.4f, 0f, Tau)
        fill()
    }
    fillStyle = if (dark) paints.bloomDark else paints.bloomLight
    beginPath()
    arc(0f, 0f, 5.5f, 0f, Tau)
    fill()
    fillStyle = paints.core
    beginPath()
    arc(0f, 0f, 1.6f, 0f, Tau)
    fill()
    restore()
}

/**
 * The dot's flight: it swings out to the right of the page and falls into place, with a continuous
 * phosphor tail that thins and fades behind it, the same curve a moment earlier.
 */
internal fun Canvas2D.crtFlight(s: CrtPipState, dark: Boolean, paints: CrtPipPaints) {
    val p = s.pose
    val k = s.k
    val f = p.flightF
    val cxp = max(p.fromX, p.toX) + 40 * k
    val cyp = min(p.fromY, p.toY) + abs(p.toY - p.fromY) * .2f
    save()
    // Butt caps: round caps overlap at every joint and bead the tail.
    lineCap = "butt"
    for (j in 18 downTo 1) {
        val fa = f - j * .014f
        val fb = f - (j - 1) * .014f
        if (fb <= 0) continue
        val ea = ease(max(0f, fa))
        val eb = ease(fb)
        val ax = bez(p.fromX, cxp, p.toX, ea)
        val ay = bez(p.fromY, cyp, p.toY, ea)
        val bx = bez(p.fromX, cxp, p.toX, eb)
        val by = bez(p.fromY, cyp, p.toY, eb)
        val q = 1 - j / 19f
        strokeStyle =
            (if (dark) TailBlueDark else TailBlueLight).copy(alpha = (if (dark) .32f else .4f) * q)
        lineWidth = (2 + 9 * q) * k
        beginPath()
        moveTo(ax, ay)
        lineTo(bx, by)
        stroke()
        strokeStyle = (if (dark) TailCoreDark else TailCoreLight).copy(alpha = .7f * q)
        lineWidth = (1 + 2.4f * q) * k
        beginPath()
        moveTo(ax, ay)
        lineTo(bx, by)
        stroke()
    }
    restore()
    val e = ease(f)
    crtDot(
        bez(p.fromX, cxp, p.toX, e),
        bez(p.fromY, cyp, p.toY, e),
        (4 + 1.2f * sin(f * Pi)) * k,
        1f,
        dark,
        paints,
    )
}

private val TailBlueDark = Color(110, 180, 255)
private val TailBlueLight = Color(40, 120, 240)
private val TailCoreDark = Color(240, 248, 255)
private val TailCoreLight = Color(120, 185, 255)
private val Speaker = Color(30, 15, 5, (.55f * 255).toInt())

/**
 * The fx layer over the page: the full player's picture clipped to its slot, the line, the dot in
 * flight and the dot on its own, as [CrtPipState.pose] has them.
 */
internal fun Canvas2D.drawCrtFx(s: CrtPipState, dark: Boolean, paints: CrtPipPaints) {
    val p = s.pose
    if (p.picture) {
        save()
        beginPath()
        rect(0f, s.slotTop, s.width, s.slotH)
        clip()
        picture(
            s,
            s.playerX,
            p.pictureCy,
            s.width,
            s.fullH,
            p.pictureSy,
            p.pictureSx,
            p.pictureBright,
            false,
            paints,
        )
        restore()
    }
    if (p.line) crtLine(p.lineCx, p.lineCy, p.lineW, s.k, paints)
    if (p.flight) crtFlight(s, dark, paints)
    if (p.dot) crtDot(p.dotX, p.dotY, p.dotR, p.dotA, dark, paints)
}

/**
 * The mini set: wood cabinet, bulging glass, a speaker, two knobs, rabbit ears, scaled about its
 * screen by the pose and drawn whole; the layer it is in fades it as one piece.
 */
internal fun Canvas2D.drawCrtCabinet(s: CrtPipState, paints: CrtPipPaints, random: Random) {
    val p = s.pose
    if (!p.cab) return
    val ck = s.ck
    paints.cabinet(ck)
    val mx = s.setX
    val my = s.setY
    save()
    translate(mx, my)
    scale(p.cabScale, p.cabScale)
    translate(-mx, -my)
    // Drawn from its own corner, so its gradients are the same wherever it is.
    translate(s.cabX, s.cabY)
    val w = s.cabW
    val h = s.cabH
    // Shadow, legs, antennas.
    fillStyle = paints.floor
    fullEllipse(w / 2, h + 8 * ck, w * .6f, 12 * ck)
    fill()
    fillStyle = "#3a2414"
    beginPath()
    moveTo(18 * ck, h - 2 * ck)
    lineTo(26 * ck, h - 2 * ck)
    lineTo(20 * ck, h + 8 * ck)
    lineTo(17 * ck, h + 8 * ck)
    fill()
    beginPath()
    moveTo(w - 26 * ck, h - 2 * ck)
    lineTo(w - 18 * ck, h - 2 * ck)
    lineTo(w - 17 * ck, h + 8 * ck)
    lineTo(w - 20 * ck, h + 8 * ck)
    fill()
    val ax = w * .56f
    val ay = 1 * ck
    strokeStyle = "#9aa0a6"
    lineWidth = 1.6f * ck
    lineCap = "round"
    for (i in 0 until 2) {
        val dx = if (i == 0) -16.5f else 19.5f
        val dy = if (i == 0) -27f else -24f
        beginPath()
        moveTo(ax, ay - 3 * ck)
        lineTo(ax + dx * ck, ay + dy * ck)
        stroke()
        fillStyle = "#c9ced3"
        beginPath()
        arc(ax + dx * ck, ay + dy * ck, 2.2f * ck, 0f, Tau)
        fill()
    }
    fillStyle = "#2c2a28"
    beginPath()
    ellipse(ax, ay, 10 * ck, 6 * ck, 0f, Pi, 0f)
    fill()
    // Walnut body with grain.
    fillStyle = paints.body
    rr(0f, 0f, w, h, 15 * ck)
    fill()
    save()
    rr(0f, 0f, w, h, 15 * ck)
    clip()
    strokeStyle = "rgba(50,26,10,.28)"
    lineWidth = 1 * ck
    for (i in 0 until 9) {
        val yy = 6 * ck + i * h / 9
        beginPath()
        moveTo(0f, yy)
        bezierCurveTo(
            w * .3f,
            yy + sin(i * 1.7f) * 5 * ck,
            w * .6f,
            yy - cos(i * 1.3f) * 5 * ck,
            w,
            yy + sin(i.toFloat()) * 3 * ck,
        )
        stroke()
    }
    fillStyle = "rgba(255,225,190,.22)"
    fillRect(0f, 0f, w, 2.5f * ck)
    restore()
    strokeStyle = "rgba(30,15,5,.5)"
    lineWidth = 1 * ck
    rr(.5f, .5f, w - 1, h - 1, 15 * ck)
    stroke()
    // Bezel and glass.
    val sx = 10 * ck
    val sy = 10 * ck
    val sw = s.screenW
    val sh = s.screenH
    fillStyle = "#17120e"
    rr(sx - 4 * ck, sy - 4 * ck, sw + 8 * ck, sh + 8 * ck, 17 * ck)
    fill()
    save()
    rr(sx, sy, sw, sh, 14 * ck)
    clip()
    fillStyle = "#101512"
    fillRect(sx, sy, sw, sh)
    val cx = sx + sw / 2
    val cy = sy + sh / 2
    when (p.screen) {
        PipScreen.None -> Unit
        PipScreen.Picture ->
            picture(s, cx, cy, sw, sh, p.screenSy, 1f, p.screenBright, true, paints)
        PipScreen.Line -> crtLine(cx, cy, p.screenLine, s.k, paints)
        PipScreen.Warming -> {
            picture(s, cx, cy, sw, sh, p.screenSy, 1f, p.screenBright, true, paints)
            staticNoise(sx, sy, sw, sh, p.screenNoise, s.k, random)
        }
    }
    // Scanlines, the tube's curve and a glare.
    fillStyle = "rgba(0,0,0,.16)"
    var yy = sy
    while (yy < sy + sh) {
        fillRect(sx, yy, sw, 1 * ck)
        yy += 2.4f * ck
    }
    fillStyle = paints.tube
    fillRect(sx, sy, sw, sh)
    fillStyle = paints.glare
    fullEllipse(sx + sw * .3f, sy + sh * .22f, sw * .36f, sh * .2f, -.35f)
    fill()
    restore()
    // Speaker slots and two knobs.
    val px = sx + sw + 10 * ck
    val pw = w - 10 * ck - px
    fillStyle = Speaker
    for (i in 0 until 5) {
        rr(px, sy + 4 * ck + i * 5 * ck, pw, 2.2f * ck, 1 * ck)
        fill()
    }
    for (i in 0 until 2) {
        val kx = px + pw / 2
        val ky = sy + 40 * ck + i * 19 * ck
        val kr = 6.6f * ck
        fillStyle = "#2b1a0e"
        beginPath()
        arc(kx, ky + 1 * ck, kr + 1 * ck, 0f, Tau)
        fill()
        fillStyle = paints.knobs[i]
        beginPath()
        arc(kx, ky, kr, 0f, Tau)
        fill()
        val a = if (i != 0) .9f else -.6f + sin(s.vt * .2f) * .02f
        strokeStyle = "#3a2a1a"
        lineWidth = 1.6f * ck
        beginPath()
        moveTo(kx, ky)
        lineTo(kx + sin(a) * kr * .8f, ky - cos(a) * kr * .8f)
        stroke()
    }
    restore()
}

/** A crackle of static: 160 grey specks over the tube, fresh every frame. */
private fun Canvas2D.staticNoise(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    a: Float,
    k: Float,
    random: Random,
) {
    if (a <= 0f) return
    val size = 2 * k
    for (i in 0 until 160) {
        val v = random.nextFloat()
        fillStyle = Color(v, v, v, a)
        fillRect(x + random.nextFloat() * w, y + random.nextFloat() * h, size, size)
    }
}
