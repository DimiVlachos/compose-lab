package dev.dimvlachos.lab.core.presentation.components.decoderring

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign

/** Something the ring does that is heard or felt. */
internal enum class DecoderEvent {
    /** A letter of the outer band passed the pointer. */
    Tick,
    /** Letters are clicking down into place. */
    Settle,
    /** Every letter landed in English. */
    Done,
    /** Every letter landed back in Spanish. */
    Back,
}

/**
 * The decoder ring and the wave of letters it drives, ported from the prototype's `st`, `step()`,
 * `go`/`toggle`/`land` and its `down`/`move`/`up`. [g] is the one progress: 0 shows the Spanish, 1
 * the English, and slot i runs over its own window of it ([slotQ]). [ringA] is the dial's angle.
 * Twist angle maps straight to progress; let go past [Commit] and it finishes by itself, sooner and
 * it springs back. A press with no twist is a tap, which toggles.
 *
 * Positions are in dp relative to the ring's centre. [time] is the ring's own clock, in seconds.
 */
@Stable
internal class DecoderRingState {
    var reducedMotion = false

    /** Hears what the ring does. */
    var onEvent: ((DecoderEvent) -> Unit)? = null

    /**
     * Bumped on every step and twist that changes something, so a drawing that reads it redraws.
     */
    var frame by mutableIntStateOf(0)
        private set

    /** Whether the English has landed (and is what the bubble says). */
    var english by mutableStateOf(false)
        private set

    /** Whether a language has landed since [reset], so it can be announced. */
    var announced by mutableStateOf(false)
        private set

    /** Whether something still moves: the wave, a held twist, or the bubble's height. */
    var awake by mutableStateOf(false)
        private set

    /** The bubble's height in dp: the lowest line a letter is on now, shrinking at [Shrink]. */
    var height by mutableFloatStateOf(0f)
        private set

    var g = 0f
        private set

    var ringA = 0f
        private set

    var time = 0f
        private set

    /** Where the wave is heading on its own: 0, 1, or none while held or settled. */
    var target: Int? = null
        private set

    var slots: List<DecoderSlot> = emptyList()
        private set

    /** The line height, and the bubble's padding above and below its lines, in dp. */
    var lineHeight = LineHeight
        private set

    /** What [layout] was last given, so the same lines aren't laid out again. */
    internal var layoutKey: Any? = null

    private var spinDir = 1
    private var ringFromEnglish = false
    private var heightSet = false
    private var lastSettle = -1f
    private var drag: Drag? = null
    private var drags = 0

    private class Drag(
        val id: Int,
        var last: Float,
        val g0: Float,
        val fromEnglish: Boolean,
        val ring0: Float,
        val target0: Int?,
        var lx: Float,
        var ly: Float,
    ) {
        var acc = 0f
        var travel = 0f
    }

    /** Whether a finger (or the script) holds the ring. */
    val held: Boolean
        get() = drag != null

    /** Whether the twist [id] began is the one held. */
    fun holds(id: Int) = drag?.id == id

    /**
     * Lays the wave out from the Spanish [a] to the English [b], both wrapped at the bubble's
     * width, on lines [lineHeight] tall.
     */
    fun layout(a: DecoderLines, b: DecoderLines, lineHeight: Float = LineHeight) {
        this.lineHeight = lineHeight
        slots = DecoderSlot.build(a, b)
        if (!heightSet) {
            height = contentHeight()
            heightSet = true
        } else {
            // Grown at once to the new lines, as the prototype's bubbleH() does in the same frame;
            // a shrink still glides.
            height = max(height, contentHeight())
            wake()
        }
        frame++
    }

    /** How tall the bubble is with [lines] lines. */
    fun heightOf(lines: Int) = lines * lineHeight + PadTop + PadBottom

    /** Slot [i]'s own progress at [g]: 0 not started, 1 landed. */
    fun slotQ(i: Int, g: Float = this.g): Float {
        val n = slots.size
        if (n < 2) return g.coerceIn(0f, 1f)
        return (g * (1 + Spread) - Spread * i / (n - 1)).coerceIn(0f, 1f)
    }

    /** How tall the bubble must be now: as tall as the lowest line any letter currently is on. */
    fun contentHeight(): Float {
        var m = 0f
        for (i in slots.indices) {
            val s = slots[i]
            if (s.blank) continue
            val q = slotQ(i)
            if (q <= 0f) {
                if (s.ca != ' ') m = max(m, s.pa.line.toFloat())
            } else if (q >= 1f) {
                if (s.cb != ' ') m = max(m, s.pb.line.toFloat())
            } else {
                m = max(m, lerp(s.pa.line.toFloat(), s.pb.line.toFloat(), hop(q)))
            }
        }
        return (m + 1) * lineHeight + PadTop + PadBottom
    }

    // ---- Behaviour ----

    private fun setG(to: Float) {
        val before = g
        g = to.coerceIn(0f, 1f)
        // Letters landing make the patter.
        var i = 0
        while (i < slots.size) {
            val a = slotQ(i, before)
            val b = slotQ(i, g)
            if ((a < 1f && b >= 1f) || (a > 0f && b <= 0f)) {
                // Throttled, so a wave of 140 reads as a patter.
                if (lastSettle < 0f || time - lastSettle >= SettleGap) {
                    lastSettle = time
                    onEvent?.invoke(DecoderEvent.Settle)
                }
                break
            }
            i += 3
        }
    }

    private fun setRing(a: Float) {
        val step = TAU / 26f
        val before = floor(ringA / step)
        val after = floor(a / step)
        ringA = a
        if (before != after) onEvent?.invoke(DecoderEvent.Tick)
    }

    private fun land(toEnglish: Boolean) {
        if (english == toEnglish) return
        english = toEnglish
        announced = true
        onEvent?.invoke(if (toEnglish) DecoderEvent.Done else DecoderEvent.Back)
    }

    private fun go(to: Int) {
        target = to
        if (reducedMotion) {
            setG(to.toFloat())
            land(to == 1)
            target = null
        }
        wake()
    }

    /** Turns the ring the other way round: a tap, Enter or a screen reader's click. */
    fun toggle() {
        val cur = target ?: if (g < .5f) 0 else 1
        ringFromEnglish = cur == 1
        spinDir = if (cur == 1) -1 else 1
        go(1 - cur)
    }

    /** Advances [dt] seconds: the wave in 1/240 s substeps, then the bubble's height. */
    fun step(dt: Float) {
        if (dt <= 0f) return
        time += dt
        val g0 = g
        val ring0 = ringA
        val h0 = height
        var left = dt
        while (left > 0f) {
            val h = minOf(left, 1f / 240f)
            substep(h)
            left -= h
        }
        val t = contentHeight()
        height = if (reducedMotion || !heightSet || t >= height) t else max(t, height - Shrink * dt)
        heightSet = true
        awake = target != null || drag != null || height != t
        // A finger resting on the ring moves nothing: nothing is drawn again.
        if (g != g0 || ringA != ring0 || height != h0) frame++
    }

    private fun substep(dt: Float) {
        val to = target ?: return
        if (drag != null) return
        val dg = dt * 1000f / AutoMs
        val s = sign(to - g)
        val ng = g + s * dg
        val done = if (s >= 0f) ng >= to else ng <= to
        val g1 = if (done) to.toFloat() else ng
        // The dial keeps turning the way the finger turned it: forward as the wave advances, back
        // as it springs back.
        setRing(ringA + (g1 - g) * Turn * spinDir * (if (ringFromEnglish) -1 else 1))
        setG(g1)
        if (done) {
            land(to == 1)
            target = null
        }
    }

    // ---- Gesture ----

    /** A finger comes down at ([dx], [dy]) from the ring's centre; returns its twist's id. */
    fun down(dx: Float, dy: Float): Int {
        // target0 remembers where an animation in progress was heading, so a tap on it reverses
        // it, as Enter does.
        val d = Drag(++drags, atan2(dy, dx), g, g >= .5f, ringA, target, dx, dy)
        drag = d
        target = null
        wake()
        return d.id
    }

    /** The finger moves to ([dx], [dy]); [radius] is the ring's. */
    fun move(dx: Float, dy: Float, radius: Float) {
        val d = drag ?: return
        d.travel += hypot(dx - d.lx, dy - d.ly)
        d.lx = dx
        d.ly = dy
        // Too near the centre the angle is noise; wait for the finger to come back out.
        if (hypot(dx, dy) < radius * .22f) return
        val a = atan2(dy, dx)
        var da = a - d.last
        while (da > PI) da -= TAU
        while (da < -PI) da += TAU
        d.last = a
        d.acc += da
        val p = (abs(d.acc) / Turn).coerceIn(0f, 1f)
        setRing(d.ring0 + d.acc)
        // Relative to where the wave was when the finger came down, so grabbing it mid-animation
        // doesn't snap it to an end.
        setG(if (d.fromEnglish) d.g0 - p else d.g0 + p)
        frame++
    }

    /**
     * The finger lifts at ([dx], [dy]). A cancel (the touch was taken away) finishes from the last
     * point the finger reached.
     */
    fun up(dx: Float, dy: Float, radius: Float, cancel: Boolean = false) {
        if (drag == null) return
        if (!cancel) move(dx, dy, radius)
        val d = drag ?: return
        drag = null
        val twisted = abs(d.acc) > .2f || d.travel > 10f
        spinDir = if (d.acc < 0f) -1 else 1
        ringFromEnglish = d.fromEnglish
        if (!twisted) {
            target = d.target0
            toggle()
            return
        }
        if (!d.fromEnglish) go(if (g > Commit) 1 else 0) else go(if (g < 1 - Commit) 0 else 1)
    }

    /** Drops the twist [id] began without letting it go: the wave stays where it is. */
    fun drop(id: Int) {
        if (drag?.id == id) drag = null
    }

    /** Lets the held twist go where it is, as a cancel: it finishes to the nearer end. */
    fun release() {
        val d = drag ?: return
        up(d.lx, d.ly, 0f, cancel = true)
    }

    /** Back to the Spanish at rest, silently: nothing spinning, nothing announced. */
    fun reset() {
        g = 0f
        target = null
        drag = null
        ringA = 0f
        spinDir = 1
        ringFromEnglish = false
        english = false
        announced = false
        heightSet = false
        height = contentHeight()
        heightSet = slots.isNotEmpty()
        awake = false
        frame++
    }

    private fun wake() {
        awake = true
        frame++
    }

    internal companion object {
        /** The wave: slot i starts [Spread] slot-lengths after slot 0, scaled by its index. */
        const val Spread = 3.4f
        /** A full twist: 330°, the angle that runs the whole wave. */
        const val Turn = (330 * PI / 180).toFloat()
        const val Commit = .28f
        /** How long the wave takes on its own, end to end. */
        const val AutoMs = 1500f
        /** How fast an emptied line's height goes, in dp a second. */
        const val Shrink = 220f
        const val LineHeight = 21f
        const val PadTop = 8f
        const val PadBottom = 9f
        private const val SettleGap = .028f

        /**
         * A slot's sideways slide, and the bubble's line curve, over the first half of its window.
         */
        fun hop(q: Float) = ease((q / .5f).coerceIn(0f, 1f))

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        fun easeOut(t: Float) = 1 - (1 - t).pow(3)

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    }
}

private const val TAU = (2 * PI).toFloat()
