package dev.dimvlachos.lab.core.presentation.components.locket

import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// The four Recent photos, ported line by line from the prototype's drawBeach, drawDog, drawRidge
// and drawSunflowers: each drawn once, in a 240-unit square, from a fixed seed.

/** How many pixels each of a photo's 240 units gets: twice, so it stays sharp at full density. */
internal const val PhotoScale = 2

/** A photo's side in its own units. */
internal const val PhotoUnits = 240f

/** The four photos, Folly Beach, Biscuit, Blue Ridge and Sunflowers, baked into bitmaps. */
internal fun locketPhotos(): List<ImageBitmap> =
    listOf<Canvas2D.() -> Unit>(
            { drawBeach() },
            { drawDog() },
            { drawRidge() },
            { drawSunflowers() },
        )
        .map { draw ->
            val side = (PhotoUnits * PhotoScale).toInt()
            val c = Canvas2D.create(side, side)
            c.scale(PhotoScale.toFloat(), PhotoScale.toFloat())
            c.draw()
            c.bitmap.also { it.freeze() }
        }

private const val Tau = (2 * PI).toFloat()

/** The prototype's `seeded()`: Park and Miller's minimal standard generator. */
private fun seeded(seed: Int): () -> Float {
    var s = seed.toLong()
    return {
        s = (s * 16807L) % 2147483647L
        (s.toDouble() / 2147483647.0).toFloat()
    }
}

private fun Canvas2D.drawBeach() {
    var g = createLinearGradient(0f, 0f, 0f, 160f)
    g.addColorStop(0f, "#4c4683")
    g.addColorStop(.5f, "#d9718a")
    g.addColorStop(1f, "#ffc977")
    fillStyle = g
    fillRect(0f, 0f, 240f, 160f)
    g = createRadialGradient(150f, 146f, 0f, 150f, 146f, 90f)
    g.addColorStop(0f, "rgba(255,240,190,.9)")
    g.addColorStop(1f, "rgba(255,200,130,0)")
    fillStyle = g
    fillRect(0f, 40f, 240f, 120f)
    fillStyle = "#fff3c4"
    beginPath()
    arc(150f, 150f, 24f, PI.toFloat(), 0f)
    fill()
    g = createLinearGradient(0f, 158f, 0f, 214f)
    g.addColorStop(0f, "#3e5f86")
    g.addColorStop(1f, "#1b2b48")
    fillStyle = g
    fillRect(0f, 158f, 240f, 60f)
    val r = seeded(11)
    repeat(26) {
        val y = 162f + r() * 46f
        val w = 6f + r() * 26f * (1f - (y - 160f) / 70f)
        val x = 150f - w / 2f + (r() - .5f) * (y - 150f) * .5f
        fillStyle = "rgba(255,214,150,${.25f + r() * .45f})"
        fillRect(x, y, w, 1.6f)
    }
    g = createLinearGradient(0f, 206f, 0f, 240f)
    g.addColorStop(0f, "#d6b07c")
    g.addColorStop(1f, "#a8834f")
    fillStyle = g
    beginPath()
    moveTo(0f, 214f)
    quadraticCurveTo(120f, 200f, 240f, 218f)
    lineTo(240f, 240f)
    lineTo(0f, 240f)
    fill()
}

private fun Canvas2D.drawDog() {
    var g = createLinearGradient(0f, 0f, 0f, 240f)
    g.addColorStop(0f, "#9cc77a")
    g.addColorStop(1f, "#4f8a3c")
    fillStyle = g
    fillRect(0f, 0f, 240f, 240f)
    val r = seeded(5)
    repeat(14) {
        fillStyle = "rgba(255,255,220,${.08f + r() * .14f})"
        beginPath()
        val x = r() * 240f
        val y = r() * 110f
        arc(x, y, 8f + r() * 18f, 0f, Tau)
        fill()
    }
    // Ears, head, muzzle.
    fillStyle = "#a86f33"
    for (s in intArrayOf(-1, 1)) {
        save()
        translate(120f + s * 56f, 118f)
        rotate(s * .28f)
        beginPath()
        ellipse(0f, 10f, 22f, 46f, 0f, 0f, Tau)
        fill()
        restore()
    }
    g = createRadialGradient(104f, 104f, 6f, 120f, 128f, 74f)
    g.addColorStop(0f, "#f0c27e")
    g.addColorStop(1f, "#c98b45")
    fillStyle = g
    beginPath()
    ellipse(120f, 128f, 60f, 58f, 0f, 0f, Tau)
    fill()
    fillStyle = "#f6dcae"
    beginPath()
    ellipse(120f, 160f, 34f, 26f, 0f, 0f, Tau)
    fill()
    fillStyle = "#e8869a"
    beginPath()
    ellipse(120f, 184f, 10f, 13f, 0f, 0f, Tau)
    fill()
    strokeStyle = "#5a3a18"
    lineWidth = 2.2f
    lineCap = "round"
    beginPath()
    moveTo(120f, 152f)
    lineTo(120f, 166f)
    moveTo(104f, 170f)
    quadraticCurveTo(120f, 178f, 136f, 170f)
    stroke()
    fillStyle = "#2b1b0c"
    beginPath()
    ellipse(120f, 150f, 12f, 9f, 0f, 0f, Tau)
    fill()
    for (x in floatArrayOf(98f, 142f)) {
        beginPath()
        arc(x, 118f, 6.5f, 0f, Tau)
        fill()
    }
    fillStyle = "#fff"
    for (x in floatArrayOf(96f, 140f)) {
        beginPath()
        arc(x, 116f, 2f, 0f, Tau)
        fill()
    }
    beginPath()
    ellipse(116f, 147f, 4f, 2.4f, -.3f, 0f, Tau)
    fill()
}

private fun Canvas2D.drawRidge() {
    val g = createLinearGradient(0f, 0f, 0f, 170f)
    g.addColorStop(0f, "#2b3b6a")
    g.addColorStop(.55f, "#b9799a")
    g.addColorStop(1f, "#f3b67c")
    fillStyle = g
    fillRect(0f, 0f, 240f, 240f)
    fillStyle = "rgba(255,248,226,.9)"
    beginPath()
    arc(176f, 46f, 9f, 0f, Tau)
    fill()
    val cols = listOf("#8a7ba6", "#6a6894", "#4d5180", "#343a66", "#20264a")
    cols.forEachIndexed { k, col ->
        val base = 120f + k * 24f
        val r = seeded(31 + k)
        val ph = floatArrayOf(r() * 6f, r() * 6f, r() * 6f)
        fillStyle = col
        beginPath()
        moveTo(0f, 240f)
        var x = 0
        while (x <= 240) {
            val fx = x.toFloat()
            lineTo(
                fx,
                base - 14f * sin(fx / 38f + ph[0]) - 8f * sin(fx / 17f + ph[1]) -
                    4f * sin(fx / 7f + ph[2]),
            )
            x += 6
        }
        lineTo(240f, 240f)
        fill()
    }
}

private fun Canvas2D.drawSunflowers() {
    val g = createLinearGradient(0f, 0f, 0f, 240f)
    g.addColorStop(0f, "#5aa6e0")
    g.addColorStop(1f, "#d4ecfb")
    fillStyle = g
    fillRect(0f, 0f, 240f, 240f)
    fillStyle = "#4f8a3c"
    fillRect(0f, 196f, 240f, 44f)
    fun flower(x: Float, y: Float, rr: Float) {
        strokeStyle = "#3f7a2e"
        lineWidth = rr * .16f
        beginPath()
        moveTo(x, y)
        quadraticCurveTo(x + rr * .3f, y + rr * 2f, x - rr * .1f, 260f)
        stroke()
        fillStyle = "#4f9a3a"
        beginPath()
        ellipse(x + rr * .5f, y + rr * 1.6f, rr * .55f, rr * .22f, -.5f, 0f, Tau)
        fill()
        for (i in 0 until 18) {
            val a = i / 18f * Tau
            save()
            translate(x, y)
            rotate(a)
            fillStyle = if (i % 2 == 1) "#f2b51f" else "#ffcf3a"
            beginPath()
            ellipse(0f, -rr * .78f, rr * .2f, rr * .48f, 0f, 0f, Tau)
            fill()
            restore()
        }
        val gg = createRadialGradient(x - rr * .1f, y - rr * .1f, 0f, x, y, rr * .48f)
        gg.addColorStop(0f, "#7a5024")
        gg.addColorStop(1f, "#3d240c")
        fillStyle = gg
        beginPath()
        arc(x, y, rr * .46f, 0f, Tau)
        fill()
        val r = seeded((x * 7f + y).roundToInt())
        fillStyle = "rgba(255,220,150,.3)"
        repeat(26) {
            val a = r() * 6.3f
            val d = r() * rr * .4f
            fillRect(x + cos(a) * d, y + sin(a) * d, 1.6f, 1.6f)
        }
    }
    flower(62f, 110f, 34f)
    flower(186f, 132f, 30f)
    flower(118f, 150f, 50f)
}

/** Fills a box with photo [img], cropping the overflow, as CSS `object-fit: cover`. */
internal fun Canvas2D.drawCover(img: ImageBitmap, x: Float, y: Float, w: Float, h: Float) {
    val s = maxOf(w / PhotoUnits, h / PhotoUnits)
    val sw = w / s
    val sh = h / s
    val k = PhotoScale.toFloat()
    drawImage(img, (PhotoUnits - sw) / 2f * k, (PhotoUnits - sh) / 2f * k, sw * k, sh * k, x, y, w, h)
}
