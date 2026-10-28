package dev.dimvlachos.lab.core.presentation.components.spider

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Gradient2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderRefreshState.Companion.lerp
import dev.dimvlachos.lab.core.presentation.ui.SpiderRefreshScreenColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** What the spider's drawing keeps between frames for one theme: its body's gradient. */
internal class SpiderPaints(val colors: SpiderRefreshScreenColors) {
    val body: Gradient2D =
        Gradient2D.radial(-4f, -5f, 1f, 0f, 0f, 14f).apply {
            addColorStop(0f, colors.body0)
            addColorStop(1f, colors.body1)
        }
}

private val Pupil = Color(0xFF17121C)
private val Cheek = Color(255, 128, 150).copy(alpha = .38f)
private val Mouth = Color.White.copy(alpha = .75f)
private val Shine = Color.White.copy(alpha = .12f)
private val BallLines = Color(140, 132, 160).copy(alpha = .55f)
private val Sides = intArrayOf(-1, 1)
private val LegBase = floatArrayOf(-.95f, -.4f, .15f, .7f)
private val TuftAngles = floatArrayOf(-.75f, -.25f, .25f, .75f)

/**
 * The feed's overlay as the prototype's `draw()` paints its canvas, in dp in the feed's box: the
 * tuft of web where the spider lives, its thread and the glints running up it, the strands down to
 * each hanging post, and the spider. [accent] is the app's, for its bow.
 */
internal fun Canvas2D.drawSpiderFeed(s: SpiderRefreshState, paints: SpiderPaints, accent: Color) {
    val c = paints.colors
    val k = s.k
    val ax = s.width * .5f
    // A little tuft of web where the spider lives.
    strokeStyle = c.silk.copy(alpha = .42f)
    lineWidth = .8f
    beginPath()
    for (a in TuftAngles) {
        moveTo(ax, 0f)
        lineTo(ax + sin(a) * 11f, cos(a) * 8f)
    }
    moveTo(ax - 6.5f, 3.6f)
    quadraticCurveTo(ax, 6.5f, ax + 6.5f, 3.6f)
    stroke()
    val len = s.spiderY
    if (len <= s.hiddenY + .5f && !s.reeling) return
    val sx = ax + sin(s.swing) * max(0f, len)
    val sy = len
    val rot = -s.swing * .8f
    // The thread.
    strokeStyle = c.silk.copy(alpha = .62f)
    lineWidth = 1f
    beginPath()
    moveTo(ax, 0f)
    lineTo(sx, sy - 12f * k)
    stroke()
    for (i in 0 until s.glints) {
        val gy = s.glintY[i]
        if (gy > sy - 12f * k) continue
        val u = (gy / max(1f, sy)).coerceIn(0f, 1f)
        fillStyle = c.silk.copy(alpha = s.glintA[i] * .9f)
        beginPath()
        arc(lerp(ax, sx, u), gy, 1.3f, 0f, TAU)
        fill()
    }
    // Strands from the spider down to each hanging post.
    if (s.reeling && s.unfold > .05f && s.strand > .01f) {
        val silk = c.silk.copy(alpha = s.unfold * s.strand * .62f)
        strokeStyle = silk
        fillStyle = silk
        lineWidth = .9f
        val hl = 12f
        val wC = s.width - 24f
        val sc = lerp(.32f, 1f, s.unfold)
        val half = wC * sc / 2f
        val mid = hl + wC / 2f
        var topY = s.reelT
        val fromX = sx
        val fromY = sy + 9f * k
        for (i in 0 until s.hanging.size.coerceAtMost(SpiderRefreshState.MaxBatch)) {
            val y = s.reelT + s.cardOffsets[i] * s.unfold
            val xl = mid - half * .4f
            val xr = mid + half * .4f
            beginPath()
            if (i == 0) {
                moveTo(fromX, fromY)
                lineTo(xl, y + 1f)
                moveTo(fromX, fromY)
                lineTo(xr, y + 1f)
            } else {
                moveTo(xl, topY)
                lineTo(xl, y + 1f)
                moveTo(xr, topY)
                lineTo(xr, y + 1f)
            }
            stroke()
            beginPath()
            arc(xl, y + 1f, 1.4f, 0f, TAU)
            arc(xr, y + 1f, 1.4f, 0f, TAU)
            fill()
            topY = y + s.cardHeights[i] * sc - 1f
        }
    }
    drawSpider(s, paints, accent, sx, sy, rot)
}

/** The prototype's `drawSpider()`: legs, the silk ball, body, bow, eyes, cheeks and mouth. */
private fun Canvas2D.drawSpider(
    s: SpiderRefreshState,
    paints: SpiderPaints,
    accent: Color,
    cx: Float,
    cy: Float,
    rot: Float,
) {
    val c = paints.colors
    val pop = s.pop
    val mood = s.mood
    save()
    translate(cx, cy)
    rotate(rot)
    scale(s.k * (1 + pop * .6f), s.k * (1 - pop * .3f))
    // Legs, behind the body: four a side, each a hip, a raised knee and a foot.
    val amp =
        when (mood) {
            SpiderMood.Climb,
            SpiderMood.Scurry -> .5f
            SpiderMood.Spin -> .35f
            else -> .14f
        }
    strokeStyle = c.leg
    lineWidth = 1.9f
    lineCap = "round"
    lineJoin = "round"
    for (side in Sides) for (k in 0 until 4) {
        val a = LegBase[k] + sin(s.legPhase + k * 1.7f + (if (side > 0) PI.toFloat() else 0f)) * amp
        val hx = cos(a) * 9f
        val hy = sin(a) * 9f + 1f
        val kx = hx + cos(a - .75f) * 9f
        val ky = hy + sin(a - .75f) * 9f
        val fx = kx + cos(a + .85f) * 8f
        val fy = ky + sin(a + .85f) * 8f
        beginPath()
        moveTo(side * hx, hy)
        lineTo(side * kx, ky)
        lineTo(side * fx, fy)
        stroke()
    }
    // The silk ball it spins while the posts load, held under the body.
    if (s.bundle > .01f) {
        val r = 7f * s.bundle
        val by = 16f
        fillStyle = c.ball
        strokeStyle = c.ballEdge
        lineWidth = .8f
        beginPath()
        arc(0f, by, r, 0f, TAU)
        fill()
        stroke()
        save()
        beginPath()
        arc(0f, by, r, 0f, TAU)
        clip()
        strokeStyle = BallLines
        lineWidth = .7f
        for (i in 0 until 4) {
            beginPath()
            ellipse(0f, by, r, r * .35f, s.ballTurn + i * .8f, 0f, TAU)
            stroke()
        }
        restore()
    }
    // Body.
    fillStyle = paints.body
    beginPath()
    arc(0f, 0f, 12.5f, 0f, TAU)
    fill()
    strokeStyle = c.rim
    lineWidth = 1f
    stroke()
    fillStyle = Shine
    beginPath()
    ellipse(-4.5f, -6f, 4f, 2.2f, -.6f, 0f, TAU)
    fill()
    // A small orange bow where the thread ties on.
    fillStyle = accent
    beginPath()
    moveTo(0f, -12f)
    lineTo(-5f, -15.5f)
    lineTo(-5f, -9.5f)
    closePath()
    fill()
    beginPath()
    moveTo(0f, -12f)
    lineTo(5f, -15.5f)
    lineTo(5f, -9.5f)
    closePath()
    fill()
    beginPath()
    arc(0f, -12f, 1.8f, 0f, TAU)
    fill()
    // Eyes look at the finger, at the silk ball, or up the thread.
    var lx: Float
    var ly: Float
    when (mood) {
        SpiderMood.Spin,
        SpiderMood.Reel -> {
            lx = 0f
            ly = 1f
        }
        SpiderMood.Climb,
        SpiderMood.Scurry -> {
            lx = 0f
            ly = -1f
        }
        SpiderMood.Empty -> {
            lx = 0f
            ly = .7f
        }
        else -> {
            val dx = s.lookX - cx
            val dy = s.lookY - cy
            val d = hypot(dx, dy).takeIf { it > 0f } ?: 1f
            lx = dx / d
            ly = dy / d
        }
    }
    val wideOpen = s.armed && mood == SpiderMood.Peek
    val wide = if (wideOpen) 1.12f else 1f
    val lid = if (mood == SpiderMood.Empty) .5f else 1 - s.blink * .9f
    for (side in Sides) {
        val ex = side * 5f
        val ey = -1.5f
        fillStyle = Color.White
        beginPath()
        ellipse(ex, ey, 4.2f * wide, 4.7f * wide * lid, 0f, 0f, TAU)
        fill()
        if (lid > .2f) {
            save()
            beginPath()
            ellipse(ex, ey, 4.2f * wide, 4.7f * wide * lid, 0f, 0f, TAU)
            clip()
            val px = ex + lx * 1.5f
            val py = ey + ly * 1.7f
            fillStyle = Pupil
            beginPath()
            arc(px, py, 2.4f * wide, 0f, TAU)
            fill()
            fillStyle = Color.White
            beginPath()
            arc(px - .8f, py - 1f, .85f, 0f, TAU)
            fill()
            restore()
        }
    }
    // Cheeks and mouth.
    fillStyle = Cheek
    beginPath()
    ellipse(-8.3f, 4.2f, 2.2f, 1.3f, 0f, 0f, TAU)
    fill()
    beginPath()
    ellipse(8.3f, 4.2f, 2.2f, 1.3f, 0f, 0f, TAU)
    fill()
    strokeStyle = Mouth
    lineWidth = 1.1f
    beginPath()
    when {
        mood == SpiderMood.Empty -> {
            moveTo(-2f, 6.2f)
            lineTo(2f, 6.2f)
        }
        wideOpen -> arc(0f, 4.4f, 2.6f, .1f * PI.toFloat(), .9f * PI.toFloat())
        else -> arc(0f, 4.6f, 2f, .18f * PI.toFloat(), .82f * PI.toFloat())
    }
    stroke()
    restore()
}
