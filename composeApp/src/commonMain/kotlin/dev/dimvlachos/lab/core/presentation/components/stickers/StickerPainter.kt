package dev.dimvlachos.lab.core.presentation.components.stickers

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The inks the prototype's canvas reads from its theme, by day and by night: the dashed slots, the
 * sheet's paper and its print, and the die-cuts where the stars sit.
 */
internal class StickerInks(
    val slotLine: String,
    val slotFill: String,
    val paper: String,
    val paper2: String,
    val paperInk: String,
    val diecut: String,
    val dark: Boolean,
) {
    internal companion object {
        val Day =
            StickerInks(
                slotLine = "rgba(120, 100, 70, .42)",
                slotFill = "rgba(120, 100, 70, .05)",
                paper = "#fdfbf6",
                paper2 = "#efe9de",
                paperInk = "rgba(90, 75, 55, .55)",
                diecut = "rgba(70, 55, 35, .16)",
                dark = false,
            )
        val Dusk =
            StickerInks(
                slotLine = "rgba(235, 215, 180, .34)",
                slotFill = "rgba(235, 215, 180, .04)",
                paper = "#e4ddcf",
                paper2 = "#c9c0ae",
                paperInk = "rgba(60, 48, 30, .6)",
                diecut = "rgba(50, 38, 20, .2)",
                dark = true,
            )
    }
}

/** The family name the sheet's print asks [Canvas2D.fonts] for. */
internal const val MonoFont = "MONO"

// ---------- Geometry ----------

private const val RI = .5f

// A tip's x, y and angle, and an inner corner's, on a unit star pointing up.
private val TIPS =
    Array(5) { k ->
        val a = (-PI / 2 + k * 2 * PI / 5).toFloat()
        floatArrayOf(cos(a), sin(a), a)
    }
private val INNER =
    Array(5) { k ->
        val b = (-PI / 2 + k * 2 * PI / 5 + PI / 5).toFloat()
        floatArrayOf(cos(b) * RI, sin(b) * RI, b)
    }

/** The star's outline, its points and inner corners rounded, of radius [r] about the origin. */
internal fun Canvas2D.starPath(r: Float) {
    beginPath()
    val pts = ArrayList<FloatArray>(10)
    for (k in 0 until 5) {
        pts += floatArrayOf(TIPS[k][0] * r, TIPS[k][1] * r, .16f)
        pts += floatArrayOf(INNER[k][0] * r, INNER[k][1] * r, .1f)
    }
    val n = pts.size
    val m0x = (pts[n - 1][0] + pts[0][0]) / 2
    val m0y = (pts[n - 1][1] + pts[0][1]) / 2
    moveTo(m0x, m0y)
    for (k in 0 until n) {
        val p = pts[k]
        val q = pts[(k + 1) % n]
        arcTo(p[0], p[1], (p[0] + q[0]) / 2, (p[1] + q[1]) / 2, p[2] * r)
    }
    closePath()
}

private val RAMP_AT = floatArrayOf(0f, .3f, .55f, .78f, 1f)
private val RAMP =
    arrayOf(
        intArrayOf(70, 44, 8),
        intArrayOf(140, 96, 26),
        intArrayOf(206, 158, 54),
        intArrayOf(243, 212, 120),
        intArrayOf(255, 248, 215),
    )

/** The foil's gold at brightness [t], 0 to 1, as the prototype's ramp. */
internal fun gold(t: Float): Color {
    val u0 = t.coerceIn(0f, 1f)
    for (k in 1 until RAMP_AT.size) {
        if (u0 <= RAMP_AT[k]) {
            val a = RAMP_AT[k - 1]
            val u = (u0 - a) / (RAMP_AT[k] - a)
            val ca = RAMP[k - 1]
            val cb = RAMP[k]
            fun ch(i: Int) = (ca[i] + (cb[i] - ca[i]) * u).roundToInt()
            return Color(red = ch(0), green = ch(1), blue = ch(2))
        }
    }
    return Color(red = 255, green = 248, blue = 215)
}

// The facets: a tip, an inner corner beside it, and the facet's normal, tilted out from the middle.
private class Facet(val tip: FloatArray, val inner: FloatArray, val n: FloatArray)

private val FACETS = buildList {
    for (k in 0 until 5) {
        val t = TIPS[k]
        val a = t[2]
        for ((q, off) in listOf(INNER[(k + 4) % 5] to -54f, INNER[k] to 54f)) {
            val na = a + off * (PI / 180).toFloat()
            val sl = .8f
            val nx = cos(na) * sl
            val ny = sin(na) * sl
            val nl = sqrt3(nx, ny, 1f)
            add(Facet(t, q, floatArrayOf(nx / nl, ny / nl, 1 / nl)))
        }
    }
}

private fun sqrt3(x: Float, y: Float, z: Float) = kotlin.math.sqrt(x * x + y * y + z * z)

// ---------- Foil ----------

/**
 * The embossed foil star of radius [r] lit from [l], with the press glint [glint] of the way over.
 */
internal fun Canvas2D.drawFoil(r: Float, l: FloatArray, glint: Float) {
    save()
    starPath(r)
    clip()
    for (f in FACETS) {
        val b = f.n[0] * l[0] + f.n[1] * l[1] + f.n[2] * l[2]
        val spec = b.coerceIn(0f, 1f).pow(18) * .5f
        val col = gold((b - .3f) / .68f + spec)
        fillStyle = col
        strokeStyle = col
        lineWidth = .6f
        beginPath()
        moveTo(0f, 0f)
        lineTo(f.tip[0] * r * 1.08f, f.tip[1] * r * 1.08f)
        lineTo(f.inner[0] * r * 1.12f, f.inner[1] * r * 1.12f)
        closePath()
        fill()
        stroke()
    }
    // Crisp ridge and valley lines from the embossing.
    lineWidth = .7f
    for (k in 0 until 5) {
        strokeStyle = "rgba(255,246,210,.35)"
        beginPath()
        moveTo(0f, 0f)
        lineTo(TIPS[k][0] * r, TIPS[k][1] * r)
        stroke()
        strokeStyle = "rgba(80,50,8,.25)"
        beginPath()
        moveTo(0f, 0f)
        lineTo(INNER[k][0] * r, INNER[k][1] * r)
        stroke()
    }
    // A brushed sheen that follows the light.
    val g =
        createRadialGradient(
            -l[0] * r * .55f,
            -l[1] * r * .55f,
            0f,
            -l[0] * r * .55f,
            -l[1] * r * .55f,
            r * 1.1f,
        )
    g.addColorStop(0f, "rgba(255,244,205,.38)")
    g.addColorStop(1f, "rgba(255,244,205,0)")
    globalCompositeOperation = "lighter"
    fillStyle = g
    fillRect(-r * 1.3f, -r * 1.3f, r * 2.6f, r * 2.6f)
    if (glint > 0f && glint < 1f) {
        val gx = -r * 1.6f + (r * 3.2f) * glint
        val gg =
            createLinearGradient(
                gx - r * .35f,
                gx - r * .35f - r * .6f,
                gx + r * .35f,
                gx + r * .35f - r * .6f,
            )
        gg.addColorStop(0f, "rgba(255,255,240,0)")
        gg.addColorStop(.5f, "rgba(255,255,240,.75)")
        gg.addColorStop(1f, "rgba(255,255,240,0)")
        fillStyle = gg
        fillRect(-r * 1.3f, -r * 1.3f, r * 2.6f, r * 2.6f)
    }
    globalCompositeOperation = "source-over"
    lineWidth = 2.2f
    strokeStyle = "rgba(255,236,170,.4)"
    starPath(r)
    stroke()
    restore()
    lineWidth = 1f
    strokeStyle = "rgba(105,68,10,.8)"
    starPath(r)
    stroke()
}

// The side of the fold where dot(p, e) <= along: what stays stuck down.
private fun Canvas2D.halfPlane(ex: Float, ey: Float, along: Float, r: Float) {
    val px = -ey
    val py = ex
    val b = r * 4
    val x0 = ex * along
    val y0 = ey * along
    beginPath()
    moveTo(x0 + px * b, y0 + py * b)
    lineTo(x0 - px * b, y0 - py * b)
    lineTo(x0 - px * b - ex * b, y0 - py * b - ey * b)
    lineTo(x0 + px * b - ex * b, y0 + py * b - ey * b)
    closePath()
}

// The fold's reflection, which lays the peeled part back over the star, its paper side up.
private fun Canvas2D.reflectOver(ex: Float, ey: Float, along: Float) =
    transform(
        1 - 2 * ex * ex,
        -2 * ex * ey,
        -2 * ex * ey,
        1 - 2 * ey * ey,
        2 * along * ex,
        2 * along * ey,
    )

/**
 * A star of radius [r] under the current transform, peeled [a] of its width along [edge]: the part
 * still stuck down is foil, the part peeled over shows the sticker's paper back.
 *
 * The prototype painted that back as a flat cream star over the foil. Here it is shaded as paper
 * bending: dark where it turns in the crease, bright where the curl faces the light, settling to
 * the back's own grey towards its tip, with the tack's faint gloss across it.
 */
internal fun Canvas2D.drawSticker(
    r: Float,
    a: Float,
    edge: Float,
    l: FloatArray,
    glint: Float,
    d: Float,
) {
    if (a < .005f) {
        drawFoil(r, l, glint)
        return
    }
    val ex = cos(edge)
    val ey = sin(edge)
    val lf = r * (1 - 2 * a)
    // The flat part, still stuck down.
    save()
    halfPlane(ex, ey, lf, r)
    clip()
    drawFoil(r, l, glint)
    restore()
    // The peeled flap, its paper back reflected over the fold.
    save()
    halfPlane(ex, ey, lf, r)
    clip()
    reflectOver(ex, ey, lf)
    val g = createLinearGradient(ex * lf, ey * lf, ex * r, ey * r)
    g.addColorStop(0f, "#a99d86")
    g.addColorStop(.07f, "#d9d0bf")
    g.addColorStop(.2f, "#fbf8f0")
    g.addColorStop(.42f, "#efe9dc")
    g.addColorStop(1f, "#c9c1b2")
    shadowColor = "rgba(50,32,6,.35)"
    shadowBlur = 5f * d
    shadowOffsetX = -ex * 1.5f * d
    shadowOffsetY = 2f * d
    starPath(r)
    fillStyle = g
    fill()
    shadowColor = "rgba(0,0,0,0)"
    shadowBlur = 0f
    shadowOffsetX = 0f
    shadowOffsetY = 0f
    // The tack's gloss, a soft band across the back.
    val gloss =
        createLinearGradient(
            ex * lf - ey * r,
            ey * lf + ex * r,
            ex * r + ey * r,
            ey * r - ex * r,
        )
    gloss.addColorStop(0f, "rgba(255,255,255,0)")
    gloss.addColorStop(.48f, "rgba(255,255,255,0)")
    gloss.addColorStop(.56f, "rgba(255,255,255,.35)")
    gloss.addColorStop(.66f, "rgba(255,255,255,0)")
    gloss.addColorStop(1f, "rgba(255,255,255,0)")
    fillStyle = gloss
    fill()
    lineWidth = .8f
    strokeStyle = "rgba(90,70,40,.3)"
    stroke()
    restore()
    // The crease.
    save()
    starPath(r)
    clip()
    val px = -ey
    val py = ex
    strokeStyle = "rgba(255,250,230,.55)"
    lineWidth = 1f
    beginPath()
    moveTo(ex * lf + px * r, ey * lf + py * r)
    lineTo(ex * lf - px * r, ey * lf - py * r)
    stroke()
    restore()
}

/**
 * The star's outline as it lies peeled, what is stuck down and the flap laid over, filled for its
 * shadow. The prototype shadowed the finished star's pixels; a clip here would cut the blur off
 * along the fold, so the outline is built as plain polygons, cut at the fold by hand.
 */
internal fun Canvas2D.fillSilhouette(r: Float, a: Float, edge: Float) {
    val star =
        List(10) { k -> if (k % 2 == 0) TIPS[k / 2] else INNER[k / 2] }
            .map { floatArrayOf(it[0] * r, it[1] * r) }
    if (a < .005f) {
        fillPolygon(star)
        return
    }
    val ex = cos(edge)
    val ey = sin(edge)
    val lf = r * (1 - 2 * a)
    val flat = clipToFold(star, ex, ey, lf, keepBelow = true)
    val flap =
        clipToFold(star, ex, ey, lf, keepBelow = false).map { p ->
            val d = 2 * (p[0] * ex + p[1] * ey - lf)
            floatArrayOf(p[0] - d * ex, p[1] - d * ey)
        }
    fillPolygon(flat)
    fillPolygon(flap)
}

private fun Canvas2D.fillPolygon(points: List<FloatArray>) {
    if (points.size < 3) return
    beginPath()
    moveTo(points[0][0], points[0][1])
    for (k in 1 until points.size) lineTo(points[k][0], points[k][1])
    closePath()
    fill()
}

// The part of the polygon on one side of the fold, dot(p, e) = along (Sutherland-Hodgman).
private fun clipToFold(
    points: List<FloatArray>,
    ex: Float,
    ey: Float,
    along: Float,
    keepBelow: Boolean,
): List<FloatArray> {
    fun side(p: FloatArray) = (p[0] * ex + p[1] * ey - along).let { if (keepBelow) -it else it }
    val out = ArrayList<FloatArray>()
    for (k in points.indices) {
        val p = points[k]
        val q = points[(k + 1) % points.size]
        val sp = side(p)
        val sq = side(q)
        if (sp >= 0) out += p
        if ((sp >= 0) != (sq >= 0)) {
            val t = sp / (sp - sq)
            out += floatArrayOf(p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t)
        }
    }
    return out
}

// ---------- Sheet and slots ----------

/** The liner sheet the stars start on, turned a little, with its print and its die-cuts. */
internal fun Canvas2D.drawSheet(
    layout: StickerLayout,
    state: StarStickerState,
    inks: StickerInks,
    d: Float,
) {
    val s = layout.scale
    save()
    translate(layout.sheetCx, layout.sheetCy)
    rotate(layout.sheetRot)
    val w = layout.sheetWidth
    val h = layout.sheetHeight
    val rr = 6 * s
    val cut = 16 * s
    // The body, its top right corner lifted.
    beginPath()
    moveTo(-w / 2 + rr, -h / 2)
    lineTo(w / 2 - cut, -h / 2)
    lineTo(w / 2, -h / 2 + cut)
    lineTo(w / 2, h / 2 - rr)
    arcTo(w / 2, h / 2, w / 2 - rr, h / 2, rr)
    lineTo(-w / 2 + rr, h / 2)
    arcTo(-w / 2, h / 2, -w / 2, h / 2 - rr, rr)
    lineTo(-w / 2, -h / 2 + rr)
    arcTo(-w / 2, -h / 2, -w / 2 + rr, -h / 2, rr)
    closePath()
    shadowColor = if (inks.dark) "rgba(0,0,0,.55)" else "rgba(80,55,20,.22)"
    shadowBlur = 16 * s * d
    shadowOffsetY = 6 * s * d
    val g = createLinearGradient(0f, -h / 2, 0f, h / 2)
    g.addColorStop(0f, inks.paper)
    g.addColorStop(1f, inks.paper2)
    fillStyle = g
    fill()
    shadowColor = "rgba(0,0,0,0)"
    shadowBlur = 0f
    shadowOffsetY = 0f
    // The liner's gloss.
    val gl = createLinearGradient(-w / 2, -h / 2, w / 2, h / 2)
    gl.addColorStop(0f, "rgba(255,255,255,0)")
    gl.addColorStop(.42f, "rgba(255,255,255,.28)")
    gl.addColorStop(.5f, "rgba(255,255,255,0)")
    gl.addColorStop(1f, "rgba(255,255,255,0)")
    fillStyle = gl
    fill()
    lineWidth = .8f
    strokeStyle = "rgba(90,70,40,.18)"
    stroke()
    // The lifted corner.
    beginPath()
    moveTo(w / 2 - cut, -h / 2)
    lineTo(w / 2 - cut * .82f, -h / 2 + cut * .82f)
    lineTo(w / 2, -h / 2 + cut)
    closePath()
    fillStyle = inks.paper2
    fill()
    strokeStyle = "rgba(90,70,40,.22)"
    stroke()
    // The printing on the liner.
    if (textMeasurer != null) {
        fillStyle = inks.paperInk
        font = "600 ${7.6f * s}px $MonoFont"
        textBaseline = "alphabetic"
        textAlign = "left"
        fillText("GOLD STARS  ·  GREAT JOB!", -w / 2 + 10 * s, h / 2 - 8 * s)
        textAlign = "right"
        fillText("No. 12", w / 2 - 10 * s, h / 2 - 8 * s)
        textAlign = "left"
    }
    restore()
    // The die-cuts where each star sits.
    val r = layout.radius
    for (i in 0 until StickerCount) {
        save()
        translate(layout.sheetX(i), layout.sheetY(i))
        rotate(layout.sheetRot + state.sheetJit[i])
        starPath(r * 1.04f)
        lineWidth = 1f
        strokeStyle = inks.diecut
        stroke()
        starPath(r * .98f)
        fillStyle = "rgba(255,255,255,.18)"
        fill()
        restore()
    }
}

/** The row of dashed star slots the stickers are pressed into. */
internal fun Canvas2D.drawSlots(layout: StickerLayout, inks: StickerInks) {
    val s = layout.scale
    val r = layout.radius
    for (i in 0 until StickerCount) {
        save()
        translate(layout.slotX(i), layout.slotY)
        starPath(r * .94f)
        fillStyle = inks.slotFill
        fill()
        setLineDash(floatArrayOf(3.2f * s, 3.4f * s))
        lineWidth = 1.3f
        strokeStyle = inks.slotLine
        stroke()
        restore()
    }
}

// ---------- The whole rating ----------

/**
 * What the light does between frames: each star's last x and how fast it was moving, and how far
 * the finger has tipped the light, so the foil glints as stars move.
 */
@Stable
internal class StickerLight {
    val lastX = FloatArray(StickerCount) { Float.NaN }
    val tilt = FloatArray(StickerCount)
    var pointerTilt = 0f
    var pointerTiltGoal by mutableFloatStateOf(0f)

    /** Whether the light has caught up with the finger. */
    val settled: Boolean
        get() = abs(pointerTilt - pointerTiltGoal) < .001f

    /** Follows the finger's tilt, by [dt] seconds. */
    fun step(dt: Float) {
        pointerTilt += (pointerTiltGoal - pointerTilt) * (dt * 6).coerceAtMost(1f)
    }
}

/** Paints the sheet, the slots and the stars, highest last, as the prototype's `render`. */
internal fun Canvas2D.paintStickers(
    layout: StickerLayout,
    state: StarStickerState,
    light: StickerLight,
    inks: StickerInks,
    reduced: Boolean,
    d: Float,
) {
    drawSheet(layout, state, inks, d)
    drawSlots(layout, inks)
    val now = state.now
    val t = now / 1000f
    val s = layout.scale
    val poses =
        (0 until StickerCount).map { it to poseOf(layout, state, it, state.pos(it), now, reduced) }
    for ((i, pose) in poses.sortedBy { it.second.lift }) {
        val last = light.lastX[i]
        val v = if (last.isNaN()) 0f else (pose.x - last) * 60f
        light.lastX[i] = pose.x
        light.tilt[i] += ((v / (700 * s)).coerceIn(-1f, 1f) - light.tilt[i]) * .2f
        val amb = if (reduced) 0f else 1f
        var lx =
            -.42f +
                amb * .16f * sin(t * .7f + i * 1.3f) +
                light.tilt[i] * .9f +
                light.pointerTilt * .5f
        var ly = -.6f + amb * .12f * cos(t * .55f + i * .9f) - pose.lift * .25f
        var lz = .7f
        val ll = sqrt3(lx, ly, lz)
        lx /= ll
        ly /= ll
        lz /= ll
        // The light in the star's own turned frame.
        val cr = cos(-pose.rot)
        val sr = sin(-pose.rot)
        val l = floatArrayOf(lx * cr - ly * sr, lx * sr + ly * cr, lz)
        val glint = (now - state.pressAt[i]) / 650f
        save()
        globalAlpha = pose.alpha
        translate(pose.x, pose.y)
        scale(pose.scale, pose.scale)
        rotate(pose.rot)
        // Its shadow, from the shape it makes as it lies, longer and softer the higher it is.
        val lift = pose.lift
        shadowColor =
            if (inks.dark) "rgba(0,0,0,${fmt(.45f + .2f * lift)})"
            else "rgba(70,45,8,${fmt(.26f + .16f * lift)})"
        shadowBlur = (1.5f + 14 * lift) * s * d
        shadowOffsetX = (.8f + 7 * lift) * s * d
        shadowOffsetY = (1.4f + 13 * lift) * s * d
        fillStyle = "rgb(140,96,26)"
        fillSilhouette(layout.radius, pose.a, pose.edge)
        shadowColor = "rgba(0,0,0,0)"
        shadowBlur = 0f
        shadowOffsetX = 0f
        shadowOffsetY = 0f
        drawSticker(layout.radius, pose.a, pose.edge, l, glint, d)
        restore()
    }
}

// Shadow alphas in steps of a hundredth, so the colour cache holds a few dozen strings, not one a
// frame.
private fun fmt(a: Float): String = ((a * 100).roundToInt() / 100f).toString()
