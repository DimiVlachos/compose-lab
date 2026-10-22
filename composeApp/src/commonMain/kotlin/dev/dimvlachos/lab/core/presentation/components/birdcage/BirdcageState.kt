package dev.dimvlachos.lab.core.presentation.components.birdcage

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
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
 * over all of it), the hem's [sway], the ripple's [phase] and how asleep the canary is ([sleep]).
 * Pulling the hem past halfway, or with a flick, drops the cover and mutes the chat; a tap toggles
 * it.
 *
 * [time] is the cage's own clock in ms, which [step] advances; the chirp, the stir and the wake-up
 * shake are timed on it.
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

    var sleep = 0f
        private set

    /** When the canary last sang, on [time]; negative before it first does. */
    var chirpAt = -1f
        private set

    /** When a muted message last stirred the cloth, on [time]. */
    var stirAt = -1f
        private set

    /** When the canary last woke and shook out its feathers, on [time]. */
    var shakeAt = -1f
        private set

    private var swayV = 0f
    private var sleepV = 0f
    private var drag: Drag? = null

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
        if (!m) {
            silenced = 0
            shakeAt = time
        }
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

    /** A finger comes down at [y] (any unit, as long as [move] gets the same) at [at] ms. */
    fun down(y: Float, at: Float) {
        drag = Drag(y, cover).also { it.add(at, cover) }
    }

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
        sway = 0f
        swayV = 0f
        chirpAt = -1f
        stirAt = -1f
        silenced = 0
        drag = null
        muted = false
        shakeAt = -1f
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
            sway = 0f
            return
        }
        if (!dragging) {
            val prev = coverV
            coverV = spring(cover, coverV, target, 70f, .42f, dt)
            cover += coverV * dt
            // The hem swings when the cover stops.
            swayV += (prev - coverV) * .55f
        }
        swayV = spring(sway, swayV, 0f, 38f, .16f, dt)
        sway += swayV * dt
        phase += dt * (2.2f + abs(coverV) * 4f)
        val sleepy = if (muted && cover > .78f) 1f else 0f
        sleepV = spring(sleep, sleepV, sleepy, if (sleepy > 0f) 14f else 30f, .9f, dt)
        sleep += sleepV * dt
    }

    private fun emit(event: BirdcageEvent) {
        onEvent?.invoke(event)
    }

    internal companion object {
        private const val Substep = 1f / 240f
        private const val MaxSamples = 12
    }
}

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * One semi-implicit Euler step of a damped spring, stiffness [k] and damping ratio [z]: the new
 * speed, which the caller then moves [x] by, as the prototype's `spring()` does.
 */
private fun spring(x: Float, v: Float, target: Float, k: Float, z: Float, dt: Float): Float {
    val c = 2f * sqrt(k) * z
    return v + (k * (target - x) - c * v) * dt
}
