package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/*
 * What moves inside the globe each frame, over the baked scene: the prototype's drawInterior,
 * overlays and drawSails, ported line by line. Drawn in the prototype's texture space (T by T,
 * [-1, 1] across), scaled onto the sphere.
 */

private class Drop(val x: Float, val ph: Float, val sp: Float)

private class Glint(val x: Float, val y: Float, val l: Float, val f: Float, val p: Float)

private val RAIN =
    List(56) { i ->
        val r = mulberry(100 + i)
        val x = (-.16 + r() * .5).toFloat()
        val ph = r().toFloat()
        Drop(x, ph, (1.3 + r() * .5).toFloat())
    }

private val GLINT =
    List(46) { i ->
        val r = mulberry(200 + i)
        val y = (.16 + r().pow(1.4) * .2).toFloat()
        val x = (-.56 + (r() - .5) * (.08 + (y - .14) * 1.4)).toFloat()
        val l = (.014 + r() * .03).toFloat()
        val f = (1.5 + r() * 3).toFloat()
        Glint(x, y, l, f, (r() * TAU).toFloat())
    }

// The meltemi's whitecaps: each one's place, speed and phase, from its own seed.
private class Cap(val x: Float, val y: Float, val speed: Float, val phase: Float)

private val CAPS =
    List(26) { i ->
        val r = mulberry(300 + i)
        val x = (-1 + r() * 2).toFloat()
        val y = (.17 + r() * .2).toFloat()
        val speed = (2 + r() * 3).toFloat()
        Cap(x, y, speed, (r() * 6).toFloat())
    }

private fun ease(p: Float) = if (p < .5f) 2 * p * p else 1 - (-2 * p + 2).pow(2) / 2

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun Canvas2D.drawSails(L: Lit, sails: Float) {
    val n = 8
    val len = .13f
    val (hx, hy) = WindmillHub
    save()
    translate(hx, hy)
    for (k in 0 until n) {
        val a = sails + k.toFloat() / n * TAU
        val ca = cos(a)
        val sa = sin(a)
        strokeStyle = "#4c3424"
        lineWidth = .0045f
        beginPath()
        moveTo(0f, 0f)
        lineTo(ca * len, sa * len)
        stroke()
        val b = a + .32f
        val cb = cos(b)
        val sb = sin(b)
        fillStyle = L.sails
        globalAlpha = .9f
        beginPath()
        moveTo(ca * .025f, sa * .025f)
        lineTo(ca * len, sa * len)
        lineTo(cb * len * .86f, sb * len * .86f)
        lineTo(cb * .03f, sb * .03f)
        closePath()
        fill()
        globalAlpha = 1f
    }
    fillStyle = "#3a281c"
    beginPath()
    arc(0f, 0f, .011f, 0f, TAU)
    fill()
    restore()
}

private fun Canvas2D.overlays(w: GlobeScene, a: Float, t: Float, scenes: SantoriniScenes) {
    if (a <= .01f) return
    globalAlpha = a
    when (w) {
        GlobeScene.Sunny -> {
            for (g in GLINT) {
                val k = max(0f, sin(t * g.f + g.p))
                if (k < .2f) continue
                fillStyle = rgba("255,250,226", (k * .8f).toDouble())
                fillRect(g.x, g.y, g.l, .0045f)
            }
        }
        GlobeScene.Windy -> {
            for (i in 0 until 3) {
                val sp = floatArrayOf(.11f, .08f, .14f)[i]
                val off = floatArrayOf(.2f, 1.3f, 2.1f)[i]
                val y = floatArrayOf(-.66f, -.46f, -.8f)[i]
                val s = floatArrayOf(.42f, .34f, .3f)[i]
                val x = ((t * sp + off) % 2.9f) - 1.45f
                globalAlpha = a * .85f
                drawImage(cloudSprite, x - s / 2, y - s / 4, s, s / 2)
            }
            globalAlpha = a
            for (cap in CAPS) {
                val k = max(0f, sin(t * cap.speed + cap.phase))
                if (k > .55f) {
                    fillStyle = rgba("255,255,255", ((k - .55f) * 1.6f).toDouble())
                    fillRect(cap.x, cap.y, .02f, .004f)
                }
            }
        }
        GlobeScene.Shower -> {
            strokeStyle = "rgba(232,240,252,.6)"
            lineWidth = .0042f
            lineCap = "round"
            beginPath()
            for (d in RAIN) {
                val y0 = -.5f
                val y1 = scenes.floorAt(d.x)
                val q = (t * d.sp * .55f + d.ph) % 1f
                val y = lerp(y0, y1, q)
                moveTo(d.x + .01f, y - .045f)
                lineTo(d.x, y)
            }
            stroke()
            for (d in RAIN) {
                val q = (t * d.sp * .55f + d.ph) % 1f
                if (q > .12f) continue
                val y1 = scenes.floorAt(d.x)
                if (y1 < FLOOR) continue
                val k = q / .12f
                strokeStyle = rgba("240,248,255", (.55f * (1 - k)).toDouble())
                lineWidth = .003f
                beginPath()
                ellipse(d.x, y1 + .01f, .008f + .03f * k, .003f + .008f * k, 0f, 0f, TAU)
                stroke()
            }
        }
    }
    globalAlpha = 1f
}

/**
 * The scene inside the sphere of [radius] around [center]: the baked weather (crossfading to a new
 * one), its moving overlays, the windmill's sails and the glitter.
 */
internal fun DrawScope.drawInterior(
    state: SnowGlobeState,
    scenes: SantoriniScenes?,
    center: Offset,
    radius: Float,
) {
    if (scenes == null) return
    val sphere = Path().apply { addOval(Rect(center, radius)) }
    clipPath(sphere) {
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(center.x - radius, center.y - radius)
            canvas.scale(2 * radius / T, 2 * radius / T)
            val c = Canvas2D(canvas, T, T)
            val t = state.time
            val prevW = state.sceneFrom
            val curW = state.sceneTo
            val mix = state.fade
            val me = ease(mix)
            c.setTransform(1f, 0f, 0f, 1f, 0f, 0f)
            if (mix >= 1f || prevW == curW) {
                c.drawImage(scenes.scenes.getValue(curW), 0f, 0f)
            } else {
                c.drawImage(scenes.scenes.getValue(prevW), 0f, 0f)
                c.globalAlpha = me
                c.drawImage(scenes.scenes.getValue(curW), 0f, 0f)
                c.globalAlpha = 1f
            }
            c.setTransform(T / 2f, 0f, 0f, T / 2f, T / 2f, T / 2f)
            if (mix < 1f && prevW != curW) c.overlays(prevW, 1 - me, t, scenes)
            c.overlays(curW, if (mix >= 1f || prevW == curW) 1f else me, t, scenes)
            c.drawSails(LIT.getValue(curW), state.sails)
            c.drawGlitter(state)
            val haze = ((state.energy - .18f) * .32f).coerceIn(0f, .26f)
            if (haze > .005f) {
                c.fillStyle = rgba("255,228,170", haze.toDouble())
                c.fillRect(-1.2f, -1.2f, 2.4f, 2.4f)
            }
            canvas.restore()
        }
    }
}

// The glitter, until its own port lands: gold specks where the field has them.
private fun Canvas2D.drawGlitter(state: SnowGlobeState) {
    val g = state.glitter
    for (i in 0 until g.count) {
        val shine = g.glint(i)
        fillStyle = if (shine > .6f) "#fff4cf" else "#e9c46a"
        beginPath()
        arc(g.x[i], g.y[i], g.size[i] * (.7f + .6f * shine), 0f, TAU)
        fill()
    }
}
