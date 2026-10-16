package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.canvas2d.cssColor
import dev.dimvlachos.lab.core.presentation.canvas2d.mulberry
import dev.dimvlachos.lab.core.presentation.ui.SnowGlobeArt
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The turned walnut base: the prototype's bodyPath and paintBase, ported line by line. It paints
 * two faces, as the prototype paints two canvases: the top face and the groove behind the glass,
 * and the turned body, its grain, grooves, felt foot and brass plaque in front of it. Both cover
 * the globe's box (2.12 r by 2.42 r) with the sphere's centre 1.06 r in from its top left.
 */

private const val K = .17f
private const val PI_F = PI.toFloat()

/** The base's two faces, baked for one radius and theme. */
internal class BaseArt(val back: ImageBitmap, val front: ImageBitmap)

private fun Canvas2D.bodyPath(r: Float, y0: Float) {
    beginPath()
    moveTo(-.70f * r, y0)
    bezierCurveTo(-.79f * r, y0 + .05f * r, -.9f * r, .88f * r, -.885f * r, 1.0f * r)
    bezierCurveTo(-.87f * r, 1.12f * r, -.85f * r, 1.22f * r, -.84f * r, 1.28f * r)
    ellipse(0f, 1.28f * r, .84f * r, .84f * r * K, 0f, PI_F, 0f, true)
    bezierCurveTo(.85f * r, 1.22f * r, .87f * r, 1.12f * r, .885f * r, 1.0f * r)
    bezierCurveTo(.9f * r, .88f * r, .79f * r, y0 + .05f * r, .70f * r, y0)
    ellipse(0f, y0, .70f * r, .70f * r * K, 0f, 0f, PI_F, false)
    closePath()
}

/**
 * Paints the base for a sphere of radius [r] px on a screen of [s] px per design px, light or
 * [dark], with [plaque] measuring the engraving.
 */
internal fun paintBase(
    r: Float,
    s: Float,
    dark: Boolean,
    plaque: TextMeasurer,
    density: Density,
): BaseArt {
    val w = ceil(2.12f * r).toInt().coerceAtLeast(1)
    val h = ceil(2.42f * r).toInt().coerceAtLeast(1)
    val ox = 1.06f * r
    val oy = 1.06f * r
    val y0 = .74f * r

    // Back: the top face of the base and the groove the glass sits in.
    val backImage = ImageBitmap(w, h)
    var c = Canvas2D(Canvas(backImage), w, h)
    c.setTransform(1f, 0f, 0f, 1f, ox, oy)
    var g = c.createLinearGradient(-.7f * r, 0f, .7f * r, 0f)
    g.addColorStop(0f, if (dark) "#5a3720" else "#7c4d2b")
    g.addColorStop(.35f, if (dark) "#9a6638" else "#bd8550")
    g.addColorStop(1f, if (dark) "#3a2214" else "#5a361d")
    c.fillStyle = g
    c.beginPath()
    c.ellipse(0f, y0, .70f * r, .70f * r * K, 0f, 0f, TAU)
    c.fill()
    c.fillStyle = "rgba(18,8,4,.9)"
    c.beginPath()
    c.ellipse(0f, y0, .655f * r, .655f * r * K, 0f, 0f, TAU)
    c.fill()

    // Front: the turned body, grain, grooves and a brass plaque.
    val frontImage = ImageBitmap(w, h)
    val frontCanvas = Canvas(frontImage)
    c = Canvas2D(frontCanvas, w, h)
    c.setTransform(1f, 0f, 0f, 1f, ox, oy)
    val tone = if (dark) .78f else 1f
    fun wood(hex: String, f: Float = 1f): String {
        val col = cssColor(hex)
        fun ch(v: Float) = (v * 255 * tone * f).roundToInt()
        return "rgb(${ch(col.red)},${ch(col.green)},${ch(col.blue)})"
    }
    c.bodyPath(r, y0)
    g = c.createLinearGradient(-.9f * r, 0f, .9f * r, 0f)
    for ((o, col) in
        listOf(
            0f to "#3a2112",
            .1f to "#6b4123",
            .24f to "#a8703f",
            .33f to "#c48a55",
            .42f to "#9a6235",
            .6f to "#6f4122",
            .82f to "#432512",
            1f to "#2a160a",
        )) {
        g.addColorStop(o, wood(col))
    }
    c.fillStyle = g
    c.fill()
    c.save()
    c.bodyPath(r, y0)
    c.clip()
    // Grain: fine streaks following the turned surface.
    val rg = mulberry(21)
    fun Rg() = rg().toFloat()
    for (i in 0 until 70) {
        val th = (Rg() * 2 - 1) * 1.45f
        val x0 = sin(th) * .88f * r
        val wv = (Rg() - .5f) * .05f * r
        c.beginPath()
        c.moveTo(x0, y0 - .05f * r)
        c.bezierCurveTo(x0 + wv, .95f * r, x0 - wv, 1.1f * r, x0 + wv * .5f, 1.35f * r)
        c.strokeStyle =
            if (Rg() < .8f) rgba("28,12,4", ((.08 + Rg() * .16) * 100).roundToInt() / 100.0)
            else rgba("255,214,160", ((.05 + Rg() * .07) * 100).roundToInt() / 100.0)
        c.lineWidth = (.4f + Rg() * 1.4f) * s
        c.stroke()
    }
    // Growth rings showing where the bulge was turned.
    for (k in 0 until 6) {
        c.beginPath()
        c.ellipse(
            -.25f * r + k * .02f * r,
            1.0f * r,
            (.2f + k * .07f) * r,
            (.06f + k * .025f) * r,
            0f,
            PI_F * .1f,
            PI_F * .9f,
        )
        c.strokeStyle = "rgba(30,14,6,.12)"
        c.lineWidth = .8f * s
        c.stroke()
    }
    // Soft specular band from the window, and shading top and bottom.
    g = c.createLinearGradient(-.62f * r, 0f, -.3f * r, 0f)
    g.addColorStop(0f, "rgba(255,236,206,0)")
    g.addColorStop(.5f, rgba("255,236,206", if (dark) .12 else .2))
    g.addColorStop(1f, "rgba(255,236,206,0)")
    c.fillStyle = g
    c.fillRect(-r, y0, 2 * r, .7f * r)
    g = c.createLinearGradient(0f, y0, 0f, 1.4f * r)
    g.addColorStop(0f, "rgba(10,4,0,.42)")
    g.addColorStop(.18f, "rgba(10,4,0,0)")
    g.addColorStop(.6f, "rgba(10,4,0,0)")
    g.addColorStop(1f, "rgba(10,4,0,.4)")
    c.fillStyle = g
    c.fillRect(-r, y0 - .2f * r, 2 * r, .9f * r)
    c.restore()
    // Turned grooves.
    for ((yy, hw) in listOf(.9f * r to .868f * r, 1.17f * r to .862f * r)) {
        c.beginPath()
        c.ellipse(0f, yy, hw, hw * K, 0f, PI_F * .02f, PI_F * .98f)
        c.strokeStyle = "rgba(20,8,2,.55)"
        c.lineWidth = 1.2f * s
        c.stroke()
        c.beginPath()
        c.ellipse(0f, yy + 1.4f * s, hw, hw * K, 0f, PI_F * .05f, PI_F * .7f)
        c.strokeStyle = "rgba(255,214,170,.18)"
        c.lineWidth = .9f * s
        c.stroke()
    }
    // The front lip of the top face, with the glass going down into the groove.
    c.save()
    c.beginPath()
    c.ellipse(0f, y0, .70f * r, .70f * r * K, 0f, 0f, PI_F)
    c.closePath()
    g = c.createLinearGradient(-.7f * r, 0f, .7f * r, 0f)
    g.addColorStop(0f, wood("#8a5a33"))
    g.addColorStop(.3f, wood("#d39a63"))
    g.addColorStop(1f, wood("#5a361d"))
    c.fillStyle = g
    c.fill()
    c.globalCompositeOperation = "destination-out"
    c.beginPath()
    c.ellipse(0f, y0, .668f * r, .668f * r * K, 0f, 0f, PI_F)
    c.closePath()
    c.fill()
    c.restore()
    c.beginPath()
    c.ellipse(0f, y0 + .004f * r, .668f * r, .668f * r * K, 0f, PI_F * .04f, PI_F * .96f)
    c.strokeStyle = "rgba(20,8,2,.6)"
    c.lineWidth = 1.1f * s
    c.stroke()
    // Felt foot.
    c.beginPath()
    c.ellipse(0f, 1.28f * r, .84f * r, .84f * r * K, 0f, 0f, PI_F)
    c.strokeStyle = "rgba(18,10,8,.85)"
    c.lineWidth = 2.2f * s
    c.stroke()
    // Brass plaque, curved with the body, with the island's name engraved.
    val pw = .56f * r
    val ph = .15f * r
    val py = 1.04f * r
    c.save()
    c.beginPath()
    c.moveTo(-pw / 2, py - ph / 2)
    c.quadraticCurveTo(0f, py - ph / 2 + .035f * r, pw / 2, py - ph / 2)
    c.lineTo(pw / 2, py + ph / 2)
    c.quadraticCurveTo(0f, py + ph / 2 + .035f * r, -pw / 2, py + ph / 2)
    c.closePath()
    c.shadowColor = "rgba(0,0,0,.45)"
    c.shadowBlur = 3 * s
    c.shadowOffsetY = 1 * s
    g = c.createLinearGradient(-pw / 2, 0f, pw / 2, 0f)
    g.addColorStop(0f, "#7a5718")
    g.addColorStop(.28f, "#e9c871")
    g.addColorStop(.4f, "#fff0b8")
    g.addColorStop(.55f, "#c99a3a")
    g.addColorStop(1f, "#6e4c14")
    c.fillStyle = g
    c.fill()
    c.shadowColor = "rgba(0,0,0,0)"
    c.strokeStyle = "rgba(70,46,8,.7)"
    c.lineWidth = .8f * s
    c.stroke()
    c.restore()

    // The engraving: Canvas2D has no fillText, so Compose's text draws it into the same bitmap,
    // a light pass a hair lower under the dark one, as the prototype's two fillTexts.
    val px = with(density) { (.064f * r).toSp() }
    val style =
        TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = px,
            letterSpacing = with(density) { (.006f * r).toSp() },
        )
    val engraving = plaque.measure(Engraving, style, density = density)
    val at = Offset(ox - engraving.size.width / 2f, oy + py + .02f * r - engraving.size.height / 2f)
    CanvasDrawScope().draw(
        density,
        LayoutDirection.Ltr,
        frontCanvas,
        Size(w.toFloat(), h.toFloat()),
    ) {
        drawText(engraving, SnowGlobeArt.plaqueLight, at + Offset(0f, .8f * s))
        drawText(engraving, SnowGlobeArt.plaqueInk, at)
    }
    return BaseArt(backImage, frontImage)
}

/** The island's name on the plaque, in Greek as the souvenir would have it. */
private const val Engraving = "ΣΑΝΤΟΡΙΝΗ"

/**
 * The room the globe stands in, each frame: the prototype's drawRoom, the wall down to the horizon
 * and the table from it, the window's light, at night the glow from the globe onto the table, the
 * shadow the globe casts to the right with the caustic its water lens focuses there, and the
 * contact shadow under the base. [shift] and [tilt] move the shadows with the glass.
 */
internal fun Canvas2D.drawRoom(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    r: Float,
    dark: Boolean,
    from: GlobeScene,
    to: GlobeScene,
    fade: Float,
    shift: Float,
    tilt: Float,
) {
    val rm = room(dark, from, to, fade)
    fun rgbOf(col: androidx.compose.ui.graphics.Color, a: Double = 1.0) =
        rgba(
            "${(col.red * 255).roundToInt()},${(col.green * 255).roundToInt()},${(col.blue * 255).roundToInt()}",
            a,
        )
    val baseB = cy + 1.3f * r
    val hz = cy + .62f * r
    var g = createLinearGradient(0f, 0f, 0f, h)
    g.addColorStop(0f, rgbOf(rm.wall0))
    g.addColorStop((hz / h - .06f).coerceIn(0f, 1f), rgbOf(rm.wall1))
    g.addColorStop((hz / h + .07f).coerceIn(0f, 1f), rgbOf(rm.floor0))
    g.addColorStop(1f, rgbOf(rm.floor1))
    fillStyle = g
    fillRect(0f, 0f, w, h)
    // Window light from the upper left (a lamp at night).
    g = createRadialGradient(w * .05f, h * .02f, 0f, w * .05f, h * .02f, h * .75f)
    g.addColorStop(0f, if (dark) "rgba(255,200,140,.16)" else "rgba(255,252,244,.6)")
    g.addColorStop(1f, "rgba(255,252,244,0)")
    fillStyle = g
    fillRect(0f, 0f, w, h)
    val bx = cx + shift + sin(tilt) * .2f * r
    // A glow from inside the globe onto the table at night.
    if (dark) {
        val a = LIT.getValue(from).glow
        val b = LIT.getValue(to).glow
        val t = ease(fade)
        val gc =
            (0 until 3).joinToString(",") { (a[it] + (b[it] - a[it]) * t).roundToInt().toString() }
        save()
        translate(bx, baseB)
        scale(1f, .28f)
        g = createRadialGradient(0f, 0f, r * .3f, 0f, 0f, r * 2.4f)
        g.addColorStop(0f, rgba(gc, .2))
        g.addColorStop(1f, rgba(gc, 0.0))
        fillStyle = g
        beginPath()
        arc(0f, 0f, r * 2.4f, 0f, TAU)
        fill()
        restore()
    }
    // Cast shadow to the right, with the bright caustic the water lens focuses inside it.
    val sx = bx + .86f * r + tilt * 1.1f * r
    val sy = baseB - .2f * r
    save()
    translate(sx, sy)
    scale(1f, .17f)
    g = createRadialGradient(0f, 0f, 0f, 0f, 0f, r * .9f)
    g.addColorStop(0f, rgba("40,24,16", if (dark) .35 else .2))
    g.addColorStop(1f, "rgba(40,24,16,0)")
    fillStyle = g
    beginPath()
    arc(0f, 0f, r * .9f, 0f, TAU)
    fill()
    restore()
    val k =
        when (to) {
            GlobeScene.Shower -> .45f
            GlobeScene.Windy -> .75f
            GlobeScene.Sunny -> 1f
        }
    save()
    translate(sx + .06f * r, sy + .01f * r)
    scale(1f, .2f)
    globalCompositeOperation = if (dark) "lighter" else "source-over"
    g = createRadialGradient(0f, 0f, 0f, 0f, 0f, r * .34f)
    g.addColorStop(0f, rgba("255,250,232", .75 * k))
    g.addColorStop(.3f, rgba("255,228,170", .38 * k))
    g.addColorStop(1f, "rgba(255,214,140,0)")
    fillStyle = g
    beginPath()
    arc(0f, 0f, r * .34f, 0f, TAU)
    fill()
    g = createRadialGradient(r * .02f, 0f, r * .18f, r * .02f, 0f, r * .4f)
    g.addColorStop(0f, "rgba(120,170,255,0)")
    g.addColorStop(.5f, rgba("255,150,110", .1 * k))
    g.addColorStop(1f, "rgba(255,150,110,0)")
    fillStyle = g
    beginPath()
    arc(r * .02f, 0f, r * .4f, 0f, TAU)
    fill()
    restore()
    // Contact shadow under the base.
    save()
    translate(bx, baseB)
    scale(1f, .11f)
    g = createRadialGradient(0f, 0f, r * .5f, 0f, 0f, r * 1.05f)
    g.addColorStop(0f, rgba("30,16,8", if (dark) .7 else .45))
    g.addColorStop(1f, "rgba(30,16,8,0)")
    fillStyle = g
    beginPath()
    arc(0f, 0f, r * 1.05f, 0f, TAU)
    fill()
    restore()
}
