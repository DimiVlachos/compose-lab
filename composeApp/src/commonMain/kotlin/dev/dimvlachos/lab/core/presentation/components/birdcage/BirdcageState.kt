package dev.dimvlachos.lab.core.presentation.components.birdcage

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/** Something the cage does that is heard or felt. */
internal enum class BirdcageEvent {
    /** The cover was let go to drop over the cage: the chat is muted. */
    Settle,
    /** The cover was let go to roll back up: the chat is on again. */
    Lift,
    /** A message came in and the canary sang. */
    Chirp,
    /** A message came in while muted and only stirred the cloth. */
    Rustle,
    /** A finger took the hem and began to drag it. */
    Swish,
}

/**
 * The birdcage's springs and its cover's drag, ported from the prototype's `st`, `step()` and its
 * `down`/`move`/`up`: how far the linen cover is down ([cover], 0 rolled up on top of the cage, 1
 * over all of it), the hem's [sway], the ripple's [phase], how far the canary's head is tucked in
 * ([sleep]) and how shut its eye is ([lid]). Pulling the hem past halfway, or with a flick, drops
 * the cover and mutes the chat; a tap toggles it.
 *
 * [time] is the cage's own clock in ms, which [step] advances; the chirp, the stir and the wake-up
 * sequence are timed on it.
 */
@Stable
internal class BirdcageState {
    var reducedMotion = false

    /** Hears what the cage does. */
    var onEvent: ((BirdcageEvent) -> Unit)? = null

    /** Bumped on every [step] and drag, so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Whether the chat is muted: the cover was let go to drop, or toggled down. */
    var muted by mutableStateOf(false)
        private set

    /** How many messages came in silently while muted; back to 0 when it is unmuted. */
    var silenced by mutableIntStateOf(0)
        private set

    /** How many times the canary has sung. */
    var chirps = 0
        private set

    var time = 0f
        private set

    var cover = 0f
        private set

    var coverV = 0f
        private set

    var target = 0f
        private set

    /** Whether a finger has the hem, past the slop. */
    var dragging = false
        private set

    var sway = 0f
    var phase = 0f
        private set

    /**
     * How far the head is tucked into the shoulder: 0 up, 1 tucked. It dips a little below 0 as the
     * head overshoots on waking.
     */
    var sleep = 0f
        private set

    /** How shut the canary's eye is: 0 open, 1 closed. */
    var lid = 0f
        private set

    /** When the canary last sang, on [time]; negative before it first does. */
    var chirpAt = -1f
        private set

    /** When a muted message last stirred the cloth, on [time]. */
    var stirAt = -1f
        private set

    /**
     * When the canary last began to wake, on [time]: its eye opens, then its head rises, then its
     * feathers fluff ([fluff]) and its tail flicks ([tailFlick]). Negative while asleep or before.
     */
    var wakeAt = -1f
        private set

    /** The highest the cover has been since it was last let go, for the no-bounce check. */
    internal var peak = 0f
        private set

    private var swayV = 0f
    private var sleepV = 0f
    private var lidV = 0f
    private var wasSleepy = false
    private var drag: Drag? = null
    private var drags = 0

    private class Drag(var y0: Float, var c0: Float) {
        var moved = false
        val times = FloatArray(MaxSamples)
        val values = FloatArray(MaxSamples)
        var count = 0

        fun add(at: Float, c: Float) {
            if (count == MaxSamples) {
                times.copyInto(times, 0, 1)
                values.copyInto(values, 0, 1)
                count--
            }
            times[count] = at
            values[count] = c
            count++
        }
    }

    /** Where the hem hangs, in the cage's units. */
    val hemY: Float
        get() = lerp(HemUp, HemDown, cover.coerceIn(-.05f, 1.06f))

    fun setMuted(m: Boolean, quiet: Boolean = false) {
        if (m == muted) return
        muted = m
        if (!m) silenced = 0
        if (!quiet) emit(if (m) BirdcageEvent.Settle else BirdcageEvent.Lift)
    }

    /**
     * A message comes in: the canary hops and sings, or, muted, it only stirs the cloth and is
     * counted. Returns whether it sang.
     */
    fun deliver(): Boolean {
        frame++
        return if (muted) {
            silenced++
            stirAt = time
            if (!reducedMotion) swayV += 2.2f
            emit(BirdcageEvent.Rustle)
            false
        } else {
            chirpAt = time
            chirps++
            emit(BirdcageEvent.Chirp)
            true
        }
    }

    /** Drops the cover or lifts it, as a tap or the switch does. */
    fun toggle() {
        target = if (muted) 0f else 1f
        if (!reducedMotion) coverV = if (muted) -1.5f else 1.5f
        setMuted(!muted)
        frame++
    }

    /**
     * A finger comes down at [y] (any unit, as long as [move] gets the same) at [at] ms. Returns
     * the gesture's number, which [holds] says is still the one with the hem: a later [down] takes
     * it over.
     */
    fun down(y: Float, at: Float): Int {
        drag = Drag(y, cover).also { it.add(at, cover) }
        return ++drags
    }

    /** Whether gesture [id], from [down], still has the hem: not let go, nor taken over. */
    fun holds(id: Int): Boolean = drag != null && drags == id

    /**
     * The finger is at [y] at [at] ms. Past [slop] it takes the hem, which then follows it 1:1:
     * [pxPerUnit] is how many of [y]'s units one of the cage's units is.
     */
    fun move(y: Float, at: Float, pxPerUnit: Float, slop: Float) {
        val d = drag ?: return
        val dy = y - d.y0
        // Past the slop, keep the distance beyond it rather than restarting from here.
        if (!d.moved && abs(dy) > slop) {
            d.moved = true
            d.y0 += sign(dy) * slop
            d.c0 = cover
            dragging = true
            coverV = 0f
            emit(BirdcageEvent.Swish)
        }
        if (!d.moved) return
        val range = (HemDown - HemUp) * pxPerUnit
        var c = d.c0 + (y - d.y0) / range
        // A little give past either end.
        if (c > 1f) c = 1f + (c - 1f) * .25f
        if (c < 0f) c *= .25f
        val prev = cover
        cover = c
        swayV += (cover - prev) * 4f
        d.add(at, cover)
        frame++
    }

    /**
     * The finger lifts at [at] ms. A tap toggles; a drag is flung on by its speed over the last 100
     * ms and lands down (muted) or up.
     */
    fun up(at: Float) = release(at, fling = true)

    /**
     * The gesture was taken away: it lets go without a fling. One taken away before it moved is
     * dropped, not taken for a tap (the prototype toggled the switch on a cancelled touch).
     */
    fun cancel(at: Float) = release(at, fling = false)

    private fun release(at: Float, fling: Boolean) {
        val d = drag ?: return
        drag = null
        if (!d.moved) {
            if (fling) toggle()
            return
        }
        dragging = false
        var v = 0f
        if (fling) {
            var first = -1
            for (i in 0 until d.count) {
                if (at - d.times[i] <= 100f) {
                    first = i
                    break
                }
            }
            val last = d.count - 1
            if (first >= 0 && last > first && at - d.times[last] < 70f) {
                v =
                    (d.values[last] - d.values[first]) /
                        max(.016f, (d.times[last] - d.times[first]) / 1000f)
            }
        }
        coverV = v.coerceIn(-8f, 8f)
        peak = cover
        val goDown = cover + coverV * .12f > .5f
        target = if (goDown) 1f else 0f
        setMuted(goDown)
        frame++
    }

    /** Back to the cover up, the chat on and the canary awake, without a sound. */
    fun reset() {
        cover = 0f
        coverV = 0f
        target = 0f
        dragging = false
        sleep = 0f
        sleepV = 0f
        lid = 0f
        lidV = 0f
        wasSleepy = false
        wakeAt = -1f
        peak = 0f
        sway = 0f
        swayV = 0f
        chirpAt = -1f
        stirAt = -1f
        silenced = 0
        drag = null
        muted = false
        frame++
    }

    /** Advances [seconds] in 1/240 s substeps, which keep the springs stable. */
    fun step(seconds: Float) {
        time += seconds * 1000f
        var left = seconds
        while (left > 0f) {
            val h = min(left, Substep)
            substep(h)
            left -= h
        }
        frame++
    }

    private fun substep(dt: Float) {
        if (reducedMotion) {
            if (!dragging) cover = target
            sleep = if (muted && cover > .8f) 1f else 0f
            lid = sleep
            wasSleepy = sleep > 0f
            sway = 0f
            return
        }
        if (!dragging) {
            val prev = coverV
            val side = sign(cover - target)
            // Heavy linen: critically damped, and it never passes its end. If it would, it stops
            // there.
            coverV = spring(cover, coverV, target, 60f, 1f, dt)
            cover += coverV * dt
            if (side != 0f && sign(cover - target) == -side) {
                cover = target
                coverV = 0f
            }
            // The hem swings a little when the cover slows.
            swayV += (prev - coverV) * .3f
            peak = max(peak, cover)
        }
        swayV = spring(sway, swayV, 0f, 14f, .38f, dt)
        sway += swayV * dt
        phase += dt * (2.2f + abs(coverV) * 4f)
        val sleepy = muted && cover > .78f
        // Waking is a little sequence: the eye blinks open, the head rises out of the shoulder,
        // then
        // the feathers fluff.
        if (wasSleepy && !sleepy && (sleep > .3f || lid > .3f)) wakeAt = time
        if (sleepy) wakeAt = -1f
        wasSleepy = sleepy
        val w = if (wakeAt >= 0f) time - wakeAt else Float.POSITIVE_INFINITY
        // Head: tucks in slowly when it gets dark; on waking it waits for the eye, then rises with
        // a
        // slight overshoot.
        val head = if (sleepy) 1f else if (w < WakeHeadMs) sleep else 0f
        sleepV = spring(sleep, sleepV, head, if (sleepy) 12f else 46f, if (sleepy) 1f else .62f, dt)
        sleep += sleepV * dt
        // Eyelid: closes once the head is mostly tucked; on waking it opens first, then blinks
        // once.
        if (w < WakeEndMs) {
            lid =
                when {
                    w < WakeOpenMs -> 1f - ease(w / WakeOpenMs)
                    w >= WakeBlinkMs && w < WakeBlinkMs + BlinkMs ->
                        sin((w - WakeBlinkMs) / BlinkMs * PI.toFloat()) * .9f
                    else -> 0f
                }
            lidV = 0f
        } else {
            lidV = spring(lid, lidV, if (sleepy && sleep > .6f) 1f else 0f, 22f, 1f, dt)
            lid += lidV * dt
        }
    }

    /** How puffed the feathers are as it wakes: up and back down over [WakeFluffLenMs]. */
    val fluff: Float
        get() {
            if (wakeAt < 0f) return 0f
            val p = (time - wakeAt - WakeFluffMs) / WakeFluffLenMs
            return if (p > 0f && p < 1f) sin(p * PI.toFloat()) * (1f - p * .35f) else 0f
        }

    /** The tail's flick as it wakes, in radians. */
    val tailFlick: Float
        get() {
            if (wakeAt < 0f) return 0f
            val p = (time - wakeAt - WakeTailMs) / WakeTailLenMs
            return if (p > 0f && p < 1f) sin(p * PI.toFloat() * 2f) * (1f - p) * .22f else 0f
        }

    private fun emit(event: BirdcageEvent) {
        onEvent?.invoke(event)
    }

    internal companion object {
        private const val Substep = 1f / 240f
        private const val MaxSamples = 12

        // The wake timeline, in ms from when the cover lets the light in.
        internal const val WakeOpenMs = 200f
        internal const val WakeHeadMs = 150f
        private const val WakeBlinkMs = 460f
        private const val BlinkMs = 180f
        internal const val WakeFluffMs = 560f
        private const val WakeFluffLenMs = 650f
        private const val WakeTailMs = 700f
        private const val WakeTailLenMs = 460f
        private const val WakeEndMs = 1400f
    }
}

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** The prototype's ease in-out quad. */
private fun ease(t: Float) = if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f).let { it * it } / 2f

/**
 * One semi-implicit Euler step of a damped spring, stiffness [k] and damping ratio [z]: the new
 * speed, which the caller then moves [x] by, as the prototype's `spring()` does.
 */
private fun spring(x: Float, v: Float, target: Float, k: Float, z: Float, dt: Float): Float {
    val c = 2f * sqrt(k) * z
    return v + (k * (target - x) - c * v) * dt
}
