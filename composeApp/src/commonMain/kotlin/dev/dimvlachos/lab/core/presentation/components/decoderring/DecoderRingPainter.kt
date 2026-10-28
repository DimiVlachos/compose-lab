package dev.dimvlachos.lab.core.presentation.components.decoderring

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.canvas2d.cssColor
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingState.Companion.ease
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingState.Companion.easeOut
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingState.Companion.hop
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingState.Companion.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Each character laid out once, in the font the canvas has when it is first asked for, so the wave
 * lays out no text while it runs. Make a new one when the font or the resolution changes.
 */
internal class GlyphCache {
    private val glyphs = HashMap<Char, TextLayoutResult>()

    fun get(c: Canvas2D, ch: Char): TextLayoutResult =
        glyphs.getOrPut(ch) { c.layoutText(ch.toString()) }
}

/** The message's face: the system UI font, as the prototype's `system-ui`, at [size] px. */
internal fun decoderFont(size: Float) = "400 ${size}px system-ui"

/** The bubble's padding left of its lines, in dp. */
internal const val PadX = 12f

/** The ring's canvas: 78 dp square, 8 dp outside its 62 dp button all round. */
internal const val RingCanvas = 78f
internal const val RingButton = 62f

private const val Brass = "#b9822a"
private val BrassLight = cssColor("#8a5a12")
private val BrassDark = cssColor("#f1c76f")

/**
 * The message's letters as the prototype's `drawText()` draws them, in dp, at slot positions laid
 * out at the bubble's width. [scale] is the font scale (1 is the prototype's 15 px on 21 px lines);
 * [ink] the bubble's text colour.
 */
internal fun Canvas2D.drawDecoderText(
    state: DecoderRingState,
    ink: Color,
    dark: Boolean,
    scale: Float,
    glyphs: GlyphCache,
) {
    val lh = state.lineHeight
    val base = 15.5f * scale
    val pt = DecoderRingState.PadTop
    font = decoderFont(15f * scale)
    textBaseline = "alphabetic"
    val slots = state.slots
    val warm = if (dark) BrassDark else BrassLight
    for (i in slots.indices) {
        val s = slots[i]
        if (s.blank) continue
        val q = state.slotQ(i)
        if (q <= 0f || q >= 1f) {
            val ch = if (q <= 0f) s.ca else s.cb
            val p = if (q <= 0f) s.pa else s.pb
            if (ch == ' ') continue
            fillStyle = ink
            globalAlpha = 1f
            fillText(glyphs.get(this, ch), PadX + p.x, pt + p.line * lh + base)
            continue
        }
        // In flight. A letter that stays on its line slides from its Spanish slot to its English
        // one while it spins. A letter that changes line never crosses the bubble: over the first
        // half its Spanish character rolls up and out where it stands, over the second half its
        // reel rolls in at the English slot and lands.
        val lift = sin(PI.toFloat() * q)
        val dy = -4f * lift
        val jump = s.jump
        val first = q < .5f
        val x: Float
        val lineY: Float
        val w: Float
        val cc: Float
        // 0: the reel from the Spanish; 1: the Spanish rolling out; 2: the reel from nothing.
        val reel: Int
        if (!jump) {
            val m = hop(q)
            x = PadX + lerp(s.pa.x, s.pb.x, m)
            lineY = pt + s.pa.line * lh
            w = lerp(s.pa.w, s.pb.w, m)
            // Step 0 is the source character itself, so ¿, ó or a capital never pops to another
            // letter as the reel starts; step d is the target.
            cc = s.d * easeOut(((q - .08f) / .92f).coerceIn(0f, 1f))
            reel = 0
        } else {
            val p = if (first) s.pa else s.pb
            x = PadX + p.x
            lineY = pt + p.line * lh
            w = p.w
            // First half: one step, the Spanish character rolling up into nothing. Second half:
            // from nothing, the reel up to the target.
            if (first) {
                cc = ease(q / .5f)
                reel = 1
            } else {
                cc = s.d * easeOut(((q - .5f) / .5f).coerceIn(0f, 1f))
                reel = 2
            }
        }
        // A warm tile behind the spinning letter, like the dial's window. For a line-changing
        // letter it leaves with the rolled-out character and arrives with the first incoming one.
        val tileA = if (!jump) 1f else if (first) 1f - ease(q / .5f) else cc.coerceIn(0f, 1f)
        globalAlpha = .16f * lift * tileA
        fillStyle = Brass
        beginPath()
        roundRect(x - 1.5f, lineY + 1f + dy, max(w, 6f) + 3f, lh - 2f, 3f)
        fill()
        save()
        beginPath()
        rect(x - 4f, lineY + 1f + dy, max(w, 6f) + 8f, lh - 2f)
        clip()
        val k = min(s.d, floor(cc).toInt())
        val f0 = cc - k
        // Each letter rests most of its step and rolls over quickly at the end of it, like a reel
        // clicking round. (The single roll-out step of a line-changing letter runs over its half.)
        val frac = if (jump && first) cc else ease(((f0 - .45f) / .55f).coerceIn(0f, 1f))
        val cur = reelChar(s, reel, k)
        val nxt = reelChar(s, reel, k + 1)
        // Fade letters in from a space, or out into one.
        val fade =
            when {
                s.ca == ' ' -> (q * 3f).coerceIn(0f, 1f)
                s.cb == ' ' -> ((1f - q) * 3f).coerceIn(0f, 1f)
                else -> 1f
            }
        // Ink warms to brass as the letter lifts, blended rather than switched.
        fillStyle = mixSrgb(ink, warm, ((lift - .2f) / .3f).coerceIn(0f, 1f))
        val by = lineY + base + dy
        if (cur != ' ') {
            globalAlpha = fade * (1f - frac * .6f)
            fillText(glyphs.get(this, cur), x, by - frac * lh)
        }
        if (nxt != ' ' && frac > 0f) {
            globalAlpha = fade * (.4f + frac * .6f)
            fillText(glyphs.get(this, nxt), x, by + (1f - frac) * lh)
        }
        restore()
    }
    globalAlpha = 1f
}

private fun reelChar(s: DecoderSlot, reel: Int, j: Int): Char =
    when (reel) {
        0 -> s.reel(j, s.ca)
        1 -> if (j <= 0) s.ca else ' '
        else -> s.reel(j, ' ')
    }

/** The prototype's `mix()`: channel by channel in sRGB, as its `rgb()` string. */
private fun mixSrgb(a: Color, b: Color, t: Float): Color {
    fun ch(x: Float, y: Float) = kotlin.math.round((x + (y - x) * t) * 255f) / 255f
    return Color(ch(a.red, b.red), ch(a.green, b.green), ch(a.blue, b.blue), a.alpha)
}

/** The ring's two faces of letters, each laid out once. */
internal class RingGlyphs {
    val outer = GlyphCache()
    val inner = GlyphCache()
}

/**
 * The prototype's `drawRing()` in its 78 dp box: a brass disc, an outer band of 26 letters turned
 * to [angle], an inner wheel turned the other way at half speed, and a red enamel pointer that
 * stays put. [shadowScale] turns the prototype's shadow, given in device pixels at a ratio of 2,
 * into this canvas's pixels.
 */
internal fun Canvas2D.drawDecoderRing(angle: Float, shadowScale: Float, glyphs: RingGlyphs) {
    val cx = RingCanvas / 2f
    val cy = RingCanvas / 2f
    val r = 29f
    // Shadow on the bubble background.
    save()
    shadowColor = "rgba(40,24,6,.35)"
    shadowBlur = 7f * shadowScale
    shadowOffsetY = 2.5f * shadowScale
    beginPath()
    arc(cx, cy, r, 0f, TAU)
    fillStyle = "#9b6a22"
    fill()
    restore()
    // Outer band: brass with the alphabet, turning with the finger.
    var gr = createRadialGradient(cx - 9f, cy - 11f, 2f, cx, cy, r)
    gr.addColorStop(0f, "#fbe6a6")
    gr.addColorStop(.55f, "#d9a547")
    gr.addColorStop(1f, "#94631f")
    beginPath()
    arc(cx, cy, r, 0f, TAU)
    fillStyle = gr
    fill()
    lineWidth = 1f
    strokeStyle = "rgba(90,55,10,.6)"
    stroke()
    // Knurled edge.
    save()
    translate(cx, cy)
    rotate(angle)
    strokeStyle = "rgba(90,55,10,.45)"
    lineWidth = .8f
    for (j in 0 until 52) {
        val a = j / 52f * TAU
        beginPath()
        moveTo(sin(a) * (r - .5f), -cos(a) * (r - .5f))
        lineTo(sin(a) * (r - 2.6f), -cos(a) * (r - 2.6f))
        stroke()
    }
    font = "700 6.6px JetBrains Mono"
    textAlign = "center"
    textBaseline = "middle"
    for (j in 0 until 26) {
        val a = j / 26f * TAU
        save()
        rotate(a)
        translate(0f, -(r - 7.4f))
        val l = glyphs.outer.get(this, 'A' + j)
        fillStyle = "rgba(255,240,200,.55)"
        fillText(l, .4f, .5f)
        fillStyle = "#5a3a0c"
        fillText(l, 0f, 0f)
        restore()
    }
    restore()
    // Inner wheel: darker, turning the other way at half speed, with its own ring of letters.
    save()
    translate(cx, cy)
    beginPath()
    arc(0f, 0f, r - 12.5f, 0f, TAU)
    gr = createRadialGradient(-5f, -6f, 1f, 0f, 0f, r - 12.5f)
    gr.addColorStop(0f, "#e9c070")
    gr.addColorStop(1f, "#a8752a")
    fillStyle = gr
    fill()
    strokeStyle = "rgba(80,48,8,.55)"
    lineWidth = 1f
    stroke()
    rotate(-angle * .5f)
    font = "700 4.6px JetBrains Mono"
    textAlign = "center"
    textBaseline = "middle"
    fillStyle = "rgba(70,40,6,.8)"
    for (j in 0 until 26) {
        save()
        rotate(j / 26f * TAU)
        translate(0f, -(r - 16.6f))
        fillText(glyphs.inner.get(this, 'A' + (j + 13) % 26), 0f, 0f)
        restore()
    }
    // Embossed star in the middle.
    beginPath()
    for (j in 0 until 10) {
        val a = j / 10f * TAU
        val rr = if (j % 2 == 1) 2.6f else 6f
        if (j == 0) moveTo(sin(a) * rr, -cos(a) * rr) else lineTo(sin(a) * rr, -cos(a) * rr)
    }
    closePath()
    fillStyle = "#c23a2b"
    fill()
    strokeStyle = "rgba(255,220,160,.6)"
    lineWidth = .6f
    stroke()
    restore()
    // Specular sheen, fixed to the light.
    save()
    beginPath()
    arc(cx, cy, r - 1f, PI.toFloat() * 1.05f, PI.toFloat() * 1.6f)
    strokeStyle = "rgba(255,250,225,.65)"
    lineWidth = 1.6f
    stroke()
    restore()
    // The red enamel pointer at the top stays put.
    save()
    beginPath()
    moveTo(cx - 4.2f, cy - r - 4f)
    lineTo(cx + 4.2f, cy - r - 4f)
    lineTo(cx, cy - r + 4.5f)
    closePath()
    fillStyle = "#c8372a"
    fill()
    strokeStyle = "#7a1a10"
    lineWidth = .8f
    stroke()
    restore()
}
