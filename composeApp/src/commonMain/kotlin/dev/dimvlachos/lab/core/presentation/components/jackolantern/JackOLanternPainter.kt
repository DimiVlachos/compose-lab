package dev.dimvlachos.lab.core.presentation.components.jackolantern

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import dev.dimvlachos.lab.core.presentation.canvas2d.cssColor
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import dev.dimvlachos.lab.core.presentation.components.jackolantern.JackOLanternState.Companion.Tau
import dev.dimvlachos.lab.core.presentation.components.jackolantern.JackOLanternState.Companion.smooth
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** The carved face, in units of the pumpkin's radius: two eyes, a nose and a toothy grin. */
private val Holes =
    arrayOf(
        floatArrayOf(-.62f, -.06f, -.18f, -.08f, -.38f, -.46f),
        floatArrayOf(.18f, -.08f, .62f, -.06f, .38f, -.46f),
        floatArrayOf(-.09f, .1f, .09f, .1f, 0f, -.05f),
        floatArrayOf(
            -.68f,
            .15f,
            -.44f,
            .27f,
            -.31f,
            .17f,
            -.19f,
            .3f,
            .19f,
            .3f,
            .31f,
            .17f,
            .44f,
            .27f,
            .68f,
            .15f,
            .52f,
            .44f,
            .24f,
            .57f,
            .11f,
            .46f,
            -.11f,
            .46f,
            -.24f,
            .57f,
            -.52f,
            .44f,
        ),
    )

/** The ribs, outer ones first: each one's offset and width, in radii. */
private val Ribs = arrayOf(-.56f to .46f, .56f to .46f, -.3f to .5f, .3f to .5f, 0f to .48f)

private fun Canvas2D.holesPath(x: Float, y: Float, r: Float, ox: Float = 0f, oy: Float = 0f) {
    beginPath()
    for (h in Holes) {
        var i = 0
        while (i < h.size) {
            val px = x + h[i] * r + ox
            val py = y + h[i + 1] * r + oy
            if (i == 0) moveTo(px, py) else lineTo(px, py)
            i += 2
        }
        closePath()
    }
}

/** The prototype's `mix()`: two colours mixed [k] of the way in sRGB, each channel rounded. */
internal fun mixRgb(a: Color, b: Color, k: Float): Color {
    fun ch(x: Float, y: Float) = (x * 255 + (y * 255 - x * 255) * k).roundToInt().coerceIn(0, 255)
    return Color(ch(a.red, b.red), ch(a.green, b.green), ch(a.blue, b.blue))
}

private fun rgba(r: Int, g: Int, b: Int, a: Float) =
    Color(r, g, b, (a.coerceIn(0f, 1f) * 255).roundToInt())

private val HiDay = cssColor("#ffb24a")
private val HiNight = cssColor("#b05a1c")
private val MidDay = cssColor("#f07f18")
private val MidNight = cssColor("#7f3a10")
private val EdgeDay = cssColor("#b4500c")
private val EdgeNight = cssColor("#3e1706")
private val StemDay = cssColor("#77843a")
private val StemNight = cssColor("#2d331a")
private val StemDayDark = cssColor("#46521f")
private val StemNightDark = cssColor("#1a1e0e")
private val FleshDay = cssColor("#f6c784")
private val FleshNight = cssColor("#6c3a16")
private val FleshLit = cssColor("#ffd27c")
private val InsideDay = cssColor("#2e1406")
private val InsideNight = cssColor("#120702")
private val LidCut = rgba(60, 22, 4, .65f)

/**
 * The gradients the pumpkin and its light are filled with that only move when the pumpkin does, or
 * change only in strength: made once at full strength and drawn with a `globalAlpha`, which scales
 * a gradient whose stops all scale alike exactly as the prototype's per-frame alphas do. The ribs
 * and stem follow the theme's [t], which only changes while the light passes over the pumpkin.
 */
internal class LanternPaints {
    private var x = Float.NaN
    private var y = Float.NaN
    private var r = Float.NaN
    private var t = Float.NaN
    private var w = Float.NaN
    private var h = Float.NaN

    lateinit var shadow: Gradient2D
    lateinit var halo: Gradient2D
    lateinit var ember: Gradient2D
    lateinit var wash: Gradient2D
    val ribs = arrayOfNulls<Gradient2D>(Ribs.size)
    lateinit var stem: Gradient2D

    // One unit puff, drawn under a transform for each, in the smoke's colour of the moment.
    private var puffColor = -1
    lateinit var puff: Gradient2D

    fun update(px: Float, py: Float, pr: Float, theme: Float, width: Float, height: Float) {
        if (px != x || py != y || pr != r || width != w || height != h) {
            x = px
            y = py
            r = pr
            w = width
            h = height
            t = Float.NaN
            innerGradient = null
            shadow =
                Gradient2D.radial(px, py + pr * .86f, 0f, px, py + pr * .86f, pr * .95f).apply {
                    addColorStop(0f, rgba(60, 30, 8, 1f))
                    addColorStop(1f, rgba(60, 30, 8, 0f))
                }
            halo =
                Gradient2D.radial(px, py + pr * .15f, pr * .3f, px, py + pr * .15f, pr * 2.3f)
                    .apply {
                        addColorStop(0f, rgba(255, 140, 40, 1f))
                        addColorStop(1f, rgba(255, 120, 30, 0f))
                    }
            ember =
                Gradient2D.radial(px, py + pr * .53f, 0f, px, py + pr * .53f, pr * .09f).apply {
                    addColorStop(0f, rgba(255, 150, 70, 1f))
                    addColorStop(1f, rgba(255, 90, 30, 0f))
                }
            // The candle's warm wash over the night screen: .34, .12 and 0 of the way out, by the
            // flame's strength.
            wash =
                Gradient2D.radial(px, py, 0f, px, py, height * 1.05f).apply {
                    addColorStop(0f, rgba(255, 150, 60, 1f))
                    addColorStop(.22f, rgba(255, 128, 48, .12f / .34f))
                    addColorStop(1f, rgba(255, 120, 40, 0f))
                }
        }
        if (theme != t) {
            t = theme
            val hi = mixRgb(HiDay, HiNight, theme)
            val md = mixRgb(MidDay, MidNight, theme)
            val ed = mixRgb(EdgeDay, EdgeNight, theme)
            for (i in Ribs.indices) {
                val cx = px + Ribs[i].first * pr
                val cy = py + pr * .05f
                ribs[i] =
                    Gradient2D.radial(cx - pr * .12f, py - pr * .3f, pr * .05f, cx, cy, pr * .95f)
                        .apply {
                            addColorStop(0f, hi)
                            addColorStop(.55f, md)
                            addColorStop(1f, ed)
                        }
            }
            stem =
                Gradient2D.linear(px - pr * .1f, 0f, px + pr * .2f, 0f).apply {
                    addColorStop(0f, mixRgb(StemDay, StemNight, theme))
                    addColorStop(1f, mixRgb(StemDayDark, StemNightDark, theme))
                }
        }
    }

    fun puff(theme: Float): Gradient2D {
        val r = lerpInt(120, 190, theme)
        val g = lerpInt(112, 182, theme)
        val b = lerpInt(108, 178, theme)
        val key = (r shl 16) or (g shl 8) or b
        if (key != puffColor) {
            puffColor = key
            puff =
                Gradient2D.radial(0f, 0f, 0f, 0f, 0f, 1f).apply {
                    addColorStop(0f, Color(r, g, b, 255))
                    addColorStop(1f, Color(r, g, b, 0))
                }
        }
        return puff
    }

    private fun lerpInt(a: Int, b: Int, k: Float) = (a + (b - a) * k).roundToInt()

    // The candlelit inside of the holes, its bright spot leaning with the flame: made again only
    // when the lean moves by a visible step, so a still flame makes none.
    private var innerKey = Int.MIN_VALUE
    private var innerGradient: Gradient2D? = null

    fun inner(lean: Float): Gradient2D {
        val key = (lean * 200f).roundToInt()
        innerGradient?.let { if (key == innerKey) return it }
        innerKey = key
        val l = key / 200f
        return Gradient2D.radial(x + l * r * .08f, y + r * .4f, r * .02f, x, y + r * .2f, r * .95f)
            .apply {
                addColorStop(0f, Inner0)
                addColorStop(.32f, Inner1)
                addColorStop(.72f, Inner2)
                addColorStop(1f, Inner3)
            }
            .also { innerGradient = it }
    }
}

/** The flame seen through the grin, on a unit circle, laid on the flame under a transform. */
private val FlameGradient =
    Gradient2D.radial(0f, 0f, 0f, 0f, 0f, 1f).apply {
        addColorStop(0f, rgba(255, 255, 240, 1f))
        addColorStop(.35f, rgba(255, 226, 130, .95f))
        addColorStop(1f, rgba(255, 140, 40, 0f))
    }

private val Inner0 = cssColor("#fff6c8")
private val Inner1 = cssColor("#ffc650")
private val Inner2 = cssColor("#f07a20")
private val Inner3 = cssColor("#a8400e")
private val EdgeClear = rgba(255, 150, 60, 0f)
private val EdgeWarm = rgba(255, 160, 70, 1f)

/** How far the night copy reaches from the pumpkin at [s]: its soft edge ends here. */
internal fun nightRadius(s: Float, px: Float, py: Float, w: Float, h: Float): Float {
    val far =
        max(max(hypot(px, py), hypot(w - px, py)), max(hypot(px, h - py), hypot(w - px, h - py)))
    return s * (far + NightFeather + 20f)
}

/** The soft edge of the spreading night, in dp. */
internal const val NightFeather = 70f

/**
 * The prototype's `drawPumpkin` and `drawSmoke`, in dp: the carved pumpkin at the state's place,
 * its colours [t] of the way to night, and the smoke curling off its lid.
 */
internal fun Canvas2D.drawLantern(state: JackOLanternState, paints: LanternPaints) {
    val s = state.spread()
    val t = smooth(0f, .1f, s)
    val x = state.px
    val y = state.py
    val r = state.r
    paints.update(x, y, r, t, state.width, state.height)
    drawPumpkin(state, paints, x, y, r, t)
    drawSmoke(state, paints, t)
}

private fun Canvas2D.drawPumpkin(
    state: JackOLanternState,
    paints: LanternPaints,
    x: Float,
    y: Float,
    r: Float,
    t: Float,
) {
    val f = state.flame()
    val e = state.ember()
    val fl = state.flicker()
    val lean = state.lean
    val time = (state.now / 1000.0).toFloat()
    // Soft contact shadow under it.
    save()
    translate(0f, y + r * .86f)
    scale(1f, .2f)
    translate(0f, -(y + r * .86f))
    fillStyle = paints.shadow
    globalAlpha = .22f - .1f * t
    fillRect(x - r, y + r * .86f - r, r * 2, r * 2)
    restore()
    // Halo, at night only.
    if (f > .01f && t > .01f) {
        save()
        globalCompositeOperation = "lighter"
        fillStyle = paints.halo
        globalAlpha = .32f * f * t * fl
        fillRect(x - r * 2.5f, y - r * 2.3f, r * 5, r * 5)
        restore()
    }
    // Ribs, outer ones first.
    val outline =
        rgba(
            (115 + (20 - 115) * t).roundToInt(),
            (42 + (6 - 42) * t).roundToInt(),
            (6 + (0 - 6) * t).roundToInt(),
            .32f,
        )
    for (i in Ribs.indices) {
        val (ox, rx) = Ribs[i]
        val cx = x + ox * r
        val cy = y + r * .05f
        fillStyle = paints.ribs[i]!!
        beginPath()
        ellipse(cx, cy, rx * r, r * .8f, 0f, 0f, Tau)
        fill()
        strokeStyle = outline
        lineWidth = .8f
        stroke()
    }
    // Lid cut: a zigzag ring round the stem, with light leaking out of it when lit.
    beginPath()
    for (i in 0..18) {
        val a = i / 18f * Tau
        val rr = if (i % 2 == 1) 1f else .8f
        val px = x + cos(a) * r * .33f * rr
        val py = y - r * .6f + sin(a) * r * .11f * rr
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    closePath()
    strokeStyle = LidCut
    lineWidth = .9f
    stroke()
    if (f > .01f) {
        save()
        globalCompositeOperation = "lighter"
        strokeStyle = rgba(255, 190, 90, .75f * f * fl * (.4f + .6f * t))
        lineWidth = 1.1f
        stroke()
        restore()
    }
    // Stem.
    beginPath()
    moveTo(x - r * .1f, y - r * .66f)
    quadraticCurveTo(x - r * .08f, y - r * .92f, x + r * .1f, y - r * 1.04f)
    quadraticCurveTo(x + r * .2f, y - r * 1.06f, x + r * .22f, y - r * .98f)
    quadraticCurveTo(x + r * .1f, y - r * .86f, x + r * .1f, y - r * .66f)
    closePath()
    fillStyle = paints.stem
    fill()
    // The carved holes: pale flesh rim, then the inside (dark, or candlelit), then the flame seen
    // through them.
    save()
    holesPath(x, y, r)
    fillStyle = mixRgb(mixRgb(FleshDay, FleshNight, t), FleshLit, f)
    fill()
    clip()
    holesPath(x, y, r, r * .03f, r * .06f)
    fillStyle = mixRgb(InsideDay, InsideNight, t)
    fill()
    if (f > .005f) {
        globalAlpha = (f * fl).coerceIn(0f, 1f)
        fillStyle = paints.inner(lean)
        holesPath(x, y, r, r * .03f, r * .06f)
        fill()
        // The flame itself, seen through the grin.
        val bx = x + lean * r * .05f
        val by = y + r * .54f
        val h = r * .5f * f * (1 + .06f * sin(time * 9.1f))
        val w = r * .085f * (.6f + .4f * f)
        val tx = bx + lean * r * .32f
        val ty = by - h
        if (h > .01f) {
            // The flame's gradient is one unit circle, made once and laid on the flame's centre
            // at its size, so the path is drawn in that circle's units.
            val cx = bx
            val cy = by - h * .3f
            fun lx(v: Float) = (v - cx) / h
            fun ly(v: Float) = (v - cy) / h
            save()
            translate(cx, cy)
            scale(h, h)
            fillStyle = FlameGradient
            beginPath()
            moveTo(lx(bx - w), ly(by))
            quadraticCurveTo(lx(bx - w * 1.2f), ly(by - h * .45f), lx(tx), ly(ty))
            quadraticCurveTo(lx(bx + w * 1.2f), ly(by - h * .45f), lx(bx + w), ly(by))
            quadraticCurveTo(lx(bx), ly(by + w * .9f), lx(bx - w), ly(by))
            fill()
            restore()
        }
        globalAlpha = 1f
    }
    // The wick's ember, as the flame catches or dies.
    val emb = e * (1 - f * .85f)
    if (emb > .01f) {
        fillStyle = paints.ember
        globalAlpha = emb
        fillRect(x - r * .1f, y + r * .43f, r * .2f, r * .2f)
        globalAlpha = 1f
    }
    restore()
}

private fun Canvas2D.drawSmoke(state: JackOLanternState, paints: LanternPaints, t: Float) {
    var g: Gradient2D? = null
    for (p in state.puffs) {
        if (!p.live) continue
        val k = p.age / p.life
        val a = .3f * (1 - k).pow(1.6f) * smooth(0f, .12f, p.age)
        if (a < .005f) continue
        if (g == null) g = paints.puff(t)
        save()
        translate(p.x, p.y)
        scale(p.sz, p.sz)
        fillStyle = g
        globalAlpha = a
        fillRect(-1f, -1f, 2f, 2f)
        restore()
    }
}

/**
 * The prototype's glow canvas, laid over the night copy with `screen`: the candle's warm wash, the
 * carved face thrown faintly across the feed ([projection], baked by [bakeProjection]), and a warm
 * leading edge while the light is moving. In dp.
 */
internal fun Canvas2D.drawCandleGlow(
    state: JackOLanternState,
    paints: LanternPaints,
    projection: ImageBitmap?,
) {
    val s = state.spread()
    if (s <= .0005f) return
    val w = state.width
    val h = state.height
    val x = state.px
    val y = state.py
    val f = state.flame()
    val fl = state.flicker()
    paints.update(x, y, state.r, smooth(0f, .1f, s), w, h)
    fillStyle = paints.wash
    globalAlpha = .34f * f * fl
    fillRect(0f, 0f, w, h)
    globalAlpha = 1f
    if (f > .01f && projection != null) {
        globalAlpha = (f * (fl * 1.15f - .15f)).coerceIn(0f, 1f)
        val j = if (state.reducedMotion) 0f else sin((state.now / 1000.0).toFloat() * 5.3f) * 1.2f
        drawImage(projection, j, j * .5f, w, h)
        globalAlpha = 1f
    }
    // A warm leading edge while the light is moving.
    val big = nightRadius(s, x, y, w, h)
    val edge = (kotlin.math.abs(state.spreadVelocity) * 1.4f).coerceIn(0f, 1f) * f
    if (edge > .01f && big > NightFeather) {
        fillStyle =
            Gradient2D.radial(x, y, max(0f, big - NightFeather * 1.6f), x, y, big).apply {
                addColorStop(0f, EdgeClear)
                addColorStop(.6f, EdgeWarm)
                addColorStop(1f, EdgeClear)
            }
        globalAlpha = .16f * edge
        fillRect(0f, 0f, w, h)
        globalAlpha = 1f
    }
}

/** The glow layer's scale, as the prototype's half-size glow canvas: soft, and cheap to bake. */
internal const val GlowScale = .5f

/**
 * The prototype's `bakeProjection`: the grin the candle throws across the feed, baked once for a
 * [w] by [h] dp screen at [GlowScale], blurred, to be drawn over the whole screen.
 */
internal fun bakeProjection(w: Float, h: Float): ImageBitmap {
    val gs = GlowScale
    val c = Canvas2D.create((w * gs).roundToInt(), (h * gs).roundToInt())
    c.setTransform(gs, 0f, 0f, gs, 0f, 0f)
    val cx = w * .44f
    val cy = h * .55f
    // Sized from the shorter side: the prototype's portrait screen is as wide as that, and on a
    // landscape stage a width-sized grin would cover the header.
    val big = min(w, h) * .42f
    c.save()
    c.translate(cx, cy)
    c.rotate(-.1f)
    c.scale(1f, .86f)
    c.fillStyle =
        Gradient2D.radial(0f, 0f, 0f, 0f, 0f, big * 1.5f).apply {
            addColorStop(0f, rgba(255, 150, 60, .16f))
            addColorStop(1f, rgba(255, 150, 60, 0f))
        }
    c.fillRect(-big * 2, -big * 2, big * 4, big * 4)
    c.filter = "blur(${(5 * gs * 2).roundToInt()}px)"
    c.fillStyle = rgba(255, 170, 80, .5f)
    c.holesPath(0f, 0f, big)
    c.fill()
    c.restore()
    c.filter = "none"
    // Drawn every frame while lit: an unchanging bitmap draws from its own pixels.
    return c.bitmap.also { it.freeze() }
}
