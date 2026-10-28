package dev.dimvlachos.lab.core.presentation.components.jackolantern

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.core.presentation.canvas2d.mulberry
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

/** Something the jack-o'-lantern does that is heard or felt. */
internal enum class LanternEvent {
    /** The match is struck on lighting it. */
    Strike,
    /** The candle catches, a beat after the strike. */
    Catch,
    /** The flame is blown out. */
    Blow,
    /** A small crackle of the wick, now and then while it burns. */
    Crackle,
}

/** The prototype's `soft`: `cubic-bezier(.4, 0, .2, 1)`, which every tween here rides. */
internal val SoftEase = CubicBezierEasing(.4f, 0f, .2f, 1f)

/**
 * The prototype's `tw`: from [from] to [to], starting [t0] ms on the lantern's clock and lasting
 * [ms]. Each change restarts it from wherever its value is, so a change of mind turns round
 * mid-flight.
 */
internal class LanternTween(value: Float = 0f) {
    var from = value
        private set

    var to = value
        private set

    var t0 = 0.0
        private set

    var ms = 1.0
        private set

    fun set(from: Float, to: Float, start: Double, ms: Double) {
        this.from = from
        this.to = to
        t0 = start
        this.ms = max(1.0, ms)
    }

    fun at(now: Double): Float {
        val p = ((now - t0) / ms).coerceIn(0.0, 1.0).toFloat()
        return from + (to - from) * SoftEase.transform(p)
    }

    fun done(now: Double) = now >= t0 + ms
}

/** A puff of smoke off the lid, in dp; [live] while it is in the air. */
internal class Puff {
    var live = false
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var age = 0f
    var life = 1f
    var ph = 0f
    var sz = 2f
}

/**
 * The jack-o'-lantern's state, ported from the prototype's `light()`, `blowOut()`, its gesture
 * (`gDown`, `gMove`, `gUp`), `step()` and the frame's clock, crackles and hint. Units are dp, which
 * play the prototype's CSS px; [layout] puts the pumpkin where the screen's top bar has room for
 * it.
 *
 * [now] is the lantern's own clock in ms, which [advance] moves in 1/120 s substeps. The durations
 * are the prototype's times 1.3, softer, as the user wants them on a phone.
 */
@Stable
internal class JackOLanternState(lit: Boolean = false, seed: Int = 1031) {
    var reducedMotion = false

    /** While a script plays, the hint stays down: the fingertip shows what to do. */
    var scripted = false

    /** Hears what the lantern does. */
    var onEvent: ((LanternEvent) -> Unit)? = null

    /** The lantern's clock, in ms. */
    var now = 0.0
        private set

    /** Lit: the screen is (or is turning) night. */
    var lit by mutableStateOf(lit)
        private set

    /** Counts what to announce: each change of theme by hand or by the script. */
    var said by mutableIntStateOf(0)
        private set

    /** Ticks once a frame: drawing reads it, so the canvas redraws as the clock moves. */
    var frame by mutableLongStateOf(0L)
        private set

    /** The hint chip, and whether it was shown for a lit pumpkin. */
    var hintOn by mutableStateOf(false)
        private set

    var hintLit by mutableStateOf(false)
        private set

    // The pumpkin: its centre, radius and the radius a finger finds it in.
    var px = 0f
        private set

    var py = 0f
        private set

    val r = 25f
    val hit = 46f

    var width = 0f
        private set

    var height = 0f
        private set

    val laidOut
        get() = width > 0f

    private val flame = LanternTween(if (lit) 1f else 0f)
    private val ember = LanternTween(if (lit) 1f else 0f)
    private val spreadT = LanternTween(if (lit) 1f else 0f)

    fun flame() = flame.at(now).coerceIn(0f, 1f)

    fun ember() = ember.at(now).coerceIn(0f, 1f)

    /** How far the night has spread: 0 is all day, 1 all night. */
    fun spread() = spreadT.at(now).coerceIn(0f, 1f)

    /** The spread's smoothed speed, per second, which warms the light's moving edge. */
    var spreadVelocity = 0f
        private set

    var lean = 0f
        private set

    private var leanV = 0f
    private var wind = 0f

    val puffs = Array(PuffCount) { Puff() }
    private var emitUntil = -1.0
    private var emitDir = 1f
    private var lastS = spread()
    private var nextCrackle = 0.0
    private var catchAt = -1.0
    private var hintHideAt = -1.0
    private var lastInteract = 0.0
    private var acc = 0f
    private val random = mulberry(seed)

    private fun rnd() = random().toFloat()

    /**
     * The screen's size and the centre of the top bar's pumpkin slot, in dp; the pumpkin sits 3 dp
     * below it, as the prototype's does.
     */
    fun layout(width: Float, height: Float, slotX: Float, slotY: Float) {
        this.width = width
        this.height = height
        px = slotX
        py = slotY + 3f
    }

    private fun announce(on: Boolean) {
        lit = on
        said++
    }

    /** Lights the candle: a strike, the catch, and the night spreading out of the face. */
    fun light() {
        if (lit) return
        announce(true)
        val s = spread()
        val rm = reducedMotion
        ember.set(ember(), 1f, now, if (rm) 1.0 else 260.0)
        flame.set(flame(), 1f, now + if (rm) 0.0 else 208.0, if (rm) 1.0 else 1170.0)
        // From rest the light waits for the flame to catch; mid-flight it turns round at once.
        spreadT.set(
            s,
            1f,
            now + if (rm || s > .002f) 0.0 else 390.0,
            if (rm) 1.0 else 2600.0 * (.35 + .65 * (1 - s)),
        )
        onEvent?.invoke(LanternEvent.Strike)
        catchAt = now + 221.0
        nextCrackle = now + 1800.0
    }

    /**
     * Blows the flame out toward [dir] (1 is right): smoke off the lid, the night drawn back in.
     */
    fun blowOut(dir: Float) {
        if (!lit) return
        announce(false)
        val s = spread()
        val f = flame()
        val e = ember()
        val rm = reducedMotion
        flame.set(f, 0f, now, if (rm) 1.0 else 600.0)
        ember.set(max(e, f), 0f, now + if (rm) 0.0 else 494.0, if (rm) 1.0 else 1430.0)
        spreadT.set(
            s,
            0f,
            now + if (rm || s < .998f) 0.0 else 208.0,
            if (rm) 1.0 else 2470.0 * (.35 + .65 * s),
        )
        wind = dir * 1.7f
        if (!rm) {
            emitUntil = now + 1690.0
            emitDir = dir
        }
        catchAt = -1.0
        onEvent?.invoke(LanternEvent.Blow)
    }

    /** The switch, by a screen reader's click or a key: a blow when lit, a light when out. */
    fun toggle() {
        hideHint()
        lastInteract = now
        if (lit) blowOut(1f) else light()
    }

    /** Lit or out at once, as a reset or a replay's start puts it, with nothing heard or said. */
    fun setInstant(on: Boolean) {
        lit = on
        val v = if (on) 1f else 0f
        flame.set(v, v, now, 1.0)
        ember.set(v, v, now, 1.0)
        spreadT.set(v, v, now, 1.0)
        for (p in puffs) p.live = false
        emitUntil = -1.0
        lean = 0f
        leanV = 0f
        wind = 0f
        lastS = v
        spreadVelocity = 0f
        catchAt = -1.0
        hideHint()
        lastInteract = now
    }

    // ---------- Gesture: a tap lights it; any fast segment through the flame blows it out
    // ----------

    private var down = false
    private var gx0 = 0f
    private var gy0 = 0f
    private var gx = 0f
    private var gy = 0f
    private var gt0 = 0.0
    private var gt = 0.0
    private var moved = 0f
    private var onPumpkin = false
    private var blew = false

    private fun flameX() = px

    private fun flameY() = py + r * .2f

    fun down(x: Float, y: Float, t: Double) {
        hideHint()
        lastInteract = now
        down = true
        gx0 = x
        gy0 = y
        gx = x
        gy = y
        gt0 = t
        gt = t
        moved = 0f
        onPumpkin = hypot(x - px, y - py) < hit
        blew = false
    }

    fun move(x: Float, y: Float, t: Double) {
        if (!down) return
        val dx = x - gx
        val dy = y - gy
        val len = hypot(dx, dy)
        val dt = max(4.0, t - gt).toFloat()
        val sp = len / dt
        moved = max(moved, hypot(x - gx0, y - gy0))
        if (len > .5f && lit && !blew) {
            val d = segDist(flameX(), flameY(), gx, gy, x, y)
            val near = smooth(hit * 1.8f, r * .6f, d)
            val side = if (dx == 0f) 1f else sign(dx)
            wind =
                (wind + side * min(sp, 1.2f) * near * .9f * min(1f, len / 20f)).coerceIn(
                    -1.3f,
                    1.3f,
                )
            if (d < hit * .95f && sp > .35f && len > 6f) {
                blew = true
                blowOut(if (dx == 0f) 1f else sign(dx))
            }
        }
        gx = x
        gy = y
        gt = t
        lastInteract = now
    }

    fun up(x: Float, y: Float, t: Double) {
        if (!down) return
        move(x, y, t)
        if (!blew && onPumpkin && moved < 14f && t - gt0 < 800.0) {
            if (!lit) light()
            else {
                wind = (wind + if (rnd() < .5f) -.5f else .5f).coerceIn(-1.3f, 1.3f)
                showHint(true)
            }
        }
        down = false
        lastInteract = now
    }

    fun cancel() {
        down = false
    }

    // ---------- Hint chip ----------

    private fun showHint(force: Boolean) {
        if (hintOn && !force) return
        hintLit = lit
        hintOn = true
        if (force) hintHideAt = now + 2400.0
    }

    private fun hideHint() {
        hintOn = false
        hintHideAt = -1.0
    }

    // ---------- Clock ----------

    /** Moves the clock on by [dt] seconds: the physics in substeps, then timers, crackles, hint. */
    fun advance(dt: Float) {
        acc += dt
        while (acc >= Step) {
            step(Step)
            now += Step * 1000.0
            acc -= Step
        }
        if (catchAt >= 0 && now >= catchAt) {
            catchAt = -1.0
            onEvent?.invoke(LanternEvent.Catch)
        }
        if (hintHideAt >= 0 && now >= hintHideAt) {
            hintHideAt = -1.0
            if (!scripted) hideHint()
            lastInteract = now
        }
        val s = spread()
        if (dt > 0f) spreadVelocity += ((s - lastS) / dt - spreadVelocity) * .3f
        lastS = s
        if (lit && now > nextCrackle) {
            onEvent?.invoke(LanternEvent.Crackle)
            nextCrackle = now + 1800.0 + rnd() * 3600.0
        }
        if (scripted) hideHint() else if (!hintOn && now - lastInteract > 3800.0) showHint(false)
        frame++
    }

    private fun step(dt: Float) {
        wind *= exp(-dt * 3.2f)
        val target = wind.coerceIn(-1.2f, 1.2f)
        leanV += ((target - lean) * 70f - leanV * 11f) * dt
        lean += leanV * dt
        if (now < emitUntil && rnd() < dt * 42f) {
            val p = puffs.firstOrNull { !it.live }
            if (p != null) {
                p.live = true
                p.x = px + (rnd() - .5f) * 6f
                p.y = py - r * .62f
                p.vx = emitDir * (14f + rnd() * 22f)
                p.vy = -(16f + rnd() * 14f)
                p.age = 0f
                p.life = 1.7f + rnd() * .9f
                p.ph = rnd() * Tau
                p.sz = 2f + rnd() * 2f
            }
        }
        for (p in puffs) {
            if (!p.live) continue
            p.age += dt
            if (p.age > p.life) {
                p.live = false
                continue
            }
            p.vx *= exp(-dt * 1.4f)
            p.vy *= exp(-dt * .5f)
            p.x += (p.vx + sin(p.age * 2.6f + p.ph) * 9f) * dt
            p.y += (p.vy - 6f) * dt
            p.sz += dt * 6f
        }
    }

    /** At rest: out, every tween done, no smoke and the flame's lean spent. */
    val settled
        get() =
            !lit &&
                flame.done(now) &&
                ember.done(now) &&
                spreadT.done(now) &&
                puffs.none { it.live } &&
                abs(lean) < .001f &&
                abs(leanV) < .001f

    /** The prototype's candle flicker: summed sines, about ±10%, never a flash. */
    fun flicker(): Float {
        if (reducedMotion) return 1f
        val t = (now / 1000.0).toFloat()
        return 1f +
            .045f * sin(t * 7.3f) +
            .035f * sin(t * 11.9f + 1.3f) +
            .025f * sin(t * 17.3f + .4f)
    }

    internal companion object {
        const val Step = 1f / 120f
        const val PuffCount = 96
        const val Tau = (PI * 2).toFloat()

        fun smooth(a: Float, b: Float, x: Float): Float {
            val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
            return t * t * (3 - 2 * t)
        }

        fun segDist(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
            val dx = bx - ax
            val dy = by - ay
            val l = dx * dx + dy * dy
            val t = if (l > 0f) (((px - ax) * dx + (py - ay) * dy) / l).coerceIn(0f, 1f) else 0f
            return hypot(px - (ax + dx * t), py - (ay + dy * t))
        }
    }
}
