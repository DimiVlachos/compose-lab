package dev.dimvlachos.lab.core.presentation.components.padlock

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/** Something the lock does that is heard or felt. */
internal enum class PadlockEvent {
    /** The shackle lands home; its strength is how hard it hit, 0 to 1. */
    Clunk,
    /** A locked shackle springs back open. */
    Pop,
    /** A loose shackle knocks about in its holes. */
    Rattle,
    /** The keyhole cover swings on its rivet. */
    Swing,
    /** The password moved up or down a level without locking or unlocking. */
    Notch,
}

/**
 * The padlock's springs, ported from the prototype's `st` and `step()`: how far the shackle sits up
 * ([lift], in the lock's 300 × 250 units), how far it is swung about its long leg ([swing], in
 * radians), how much it has turned to steel ([steel]), its rattle, the body's bounce on landing,
 * and the keyhole cover ([cover], 0 shut to 1 open). Each level aims the springs; reaching
 * [StrengthLevel.Strong] swings the shackle into line and drops it under gravity until it lands.
 *
 * [time] is the lock's own clock in ms, which [step] advances; the glint is timed on it.
 */
@Stable
internal class PadlockState(private val random: Random = Random.Default) {
    var reducedMotion = false

    /** Hears what the lock does, with the strength of a [PadlockEvent.Clunk]. */
    var onEvent: ((PadlockEvent, Float) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something sets the springs moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    var level = StrengthLevel.Empty
        private set

    var peek by mutableStateOf(false)
        private set

    var time = 0f
        private set

    var lift = Lift[0]
    var swing = Swing[0]
    var steel = 0f
    var rattle = 0f
    var bounce = 0f
    var cover = 0f
    var falling = false
        private set

    var locked = false
        private set

    /** When the glint started on [time], or negative before the first lock. */
    var glintAt = -1f
        private set

    /** How many times the shackle has landed home. */
    var clunks = 0
        private set

    private var liftV = 0f
    private var swingV = 0f
    private var steelV = 0f
    private var rattleV = 0f
    private var bounceV = 0f
    private var coverV = 0f
    private var started = false

    /**
     * The password changed and now scores [next]: the springs take aim at it. While it is weak,
     * every change kicks the loose shackle, as each keystroke does in the prototype.
     */
    fun onText(next: StrengthLevel) {
        wake++
        val prev = level
        level = next
        val strong = next == StrengthLevel.Strong
        if (strong && !locked) falling = true
        var sprang = false
        if (!strong && (locked || falling)) {
            val was = locked
            locked = false
            falling = false
            if (was) {
                liftV = if (reducedMotion) 0f else PopSpeed
                sprang = true
                emit(PadlockEvent.Pop)
            }
        }
        if (started && prev != next && !sprang && !strong && next != StrengthLevel.Empty) {
            emit(PadlockEvent.Notch)
        }
        started = true
        if (next == StrengthLevel.Weak && !reducedMotion) {
            val kick = 1.6f + random.nextFloat() * 1.2f
            rattleV += if (random.nextBoolean()) -kick else kick
            emit(PadlockEvent.Rattle)
        }
        if (reducedMotion) settle()
    }

    /** Shows the password ([on]) or hides it: the keyhole cover swings aside or back. */
    fun showPassword(on: Boolean) {
        wake++
        peek = on
        emit(PadlockEvent.Swing)
        if (reducedMotion) cover = if (on) 1f else 0f
    }

    /** Back to an empty field and a hidden password, the shackle up and swung open. */
    fun reset() {
        level = StrengthLevel.Empty
        peek = false
        lift = Lift[0]
        swing = Swing[0]
        steel = 0f
        rattle = 0f
        bounce = 0f
        cover = 0f
        liftV = 0f
        swingV = 0f
        steelV = 0f
        rattleV = 0f
        bounceV = 0f
        coverV = 0f
        falling = false
        locked = false
        glintAt = -1f
        started = false
        wake++
        frame++
    }

    /** Puts every spring where its level aims it at once, for reduced motion. */
    fun settle() {
        val lv = level.ordinal
        if (level == StrengthLevel.Strong) {
            lift = 0f
            swing = 0f
            locked = true
            falling = false
        } else {
            lift = Lift[lv]
            swing = Swing[lv]
        }
        steel = Steel[lv]
        cover = if (peek) 1f else 0f
        rattle = 0f
        frame++
    }

    /**
     * Whether every spring has come to rest where its level aims it and the glint is over: nothing
     * would move on the next frame, so the frame loop can stop until [wake] changes.
     */
    val settled: Boolean
        get() {
            val lv = level.ordinal
            val speed =
                maxOf(abs(liftV), abs(swingV), abs(steelV), abs(rattleV), abs(bounceV), abs(coverV))
            val there =
                abs(steel - Steel[lv]) < RestGap &&
                    abs(cover - (if (peek) 1f else 0f)) < RestGap &&
                    abs(rattle) < RestGap &&
                    abs(bounce) < RestGap &&
                    (locked || (abs(lift - Lift[lv]) < RestGap && abs(swing - Swing[lv]) < RestGap))
            val glinting = glintAt >= 0f && time - glintAt < GlintMs
            return !falling && speed < RestSpeed && there && !glinting
        }

    /**
     * Advances [seconds] in small fixed substeps, which keep the stiff springs and the drop stable.
     */
    fun step(seconds: Float) {
        time += seconds * 1000f
        if (reducedMotion) {
            settle()
            return
        }
        var left = seconds
        while (left > 0f) {
            val h = min(left, Substep)
            substep(h)
            left -= h
        }
        frame++
    }

    private fun substep(dt: Float) {
        val lv = level.ordinal
        spring(steel, steelV, Steel[lv], 60f, .9f, dt).let { (x, v) ->
            steel = x
            steelV = v
        }
        spring(cover, coverV, if (peek) 1f else 0f, 170f, .55f, dt).let { (x, v) ->
            cover = x
            coverV = v
        }
        spring(rattle, rattleV, 0f, 260f, .12f, dt).let { (x, v) ->
            rattle = x
            rattleV = v
        }
        spring(bounce, bounceV, 0f, 520f, .22f, dt).let { (x, v) ->
            bounce = x
            bounceV = v
        }
        if (falling) {
            // Swing it into line first, then let it drop under its own weight.
            spring(swing, swingV, 0f, 420f, .9f, dt).let { (x, v) ->
                swing = x
                swingV = v
            }
            if (abs(swing) < .04f) {
                liftV -= Gravity * dt
                lift += liftV * dt
                if (lift <= 0f) {
                    val hit = min(1f, -liftV / 600f)
                    lift = 0f
                    liftV = 0f
                    falling = false
                    locked = true
                    bounceV -= 26f * (.5f + hit)
                    rattleV *= .2f
                    glintAt = time
                    clunks++
                    emit(PadlockEvent.Clunk, hit)
                }
            } else {
                spring(lift, liftV, max(lift, 4f), 200f, .8f, dt).let { (x, v) ->
                    lift = x
                    liftV = v
                }
            }
        } else if (locked) {
            lift = 0f
            liftV = 0f
            swing = 0f
            // The prototype left the swing's speed as it was; held at rest, it has none.
            swingV = 0f
        } else {
            spring(lift, liftV, Lift[lv], 150f, .62f, dt).let { (x, v) ->
                lift = x
                liftV = v
            }
            spring(swing, swingV, Swing[lv], 120f, .55f, dt).let { (x, v) ->
                swing = x
                swingV = v
            }
        }
    }

    private fun emit(event: PadlockEvent, strength: Float = 1f) {
        onEvent?.invoke(event, strength)
    }

    internal companion object {
        /** How far the shackle sits up at each level, in the lock's units. */
        val Lift = floatArrayOf(40f, 34f, 22f, 10f, 0f)

        /**
         * How far it is swung about its long leg at each level. Empty and weak swing it half a
         * turn, so the short leg stands clear past the body's side: the prototype's .62 and .46
         * only narrowed the U, which read as raised rather than open.
         */
        val Swing = floatArrayOf(PI.toFloat(), PI.toFloat(), .2f, 0f, 0f)

        /** How much the shackle has turned from brass to steel at each level. */
        val Steel = floatArrayOf(0f, .12f, .42f, .72f, 1f)

        private const val Substep = 1f / 240f
        private const val Gravity = 2900f
        private const val PopSpeed = 340f
        private const val RestSpeed = .02f
        private const val RestGap = .005f

        /** How long the glint and the star after it last, in ms. */
        private const val GlintMs = 760f
    }
}

/** One semi-implicit Euler step of a damped spring: stiffness [k], damping ratio [z]. */
private fun spring(
    x: Float,
    v: Float,
    target: Float,
    k: Float,
    z: Float,
    dt: Float,
): Pair<Float, Float> {
    val c = 2f * sqrt(k) * z
    val nv = v + (k * (target - x) - c * v) * dt
    return (x + nv * dt) to nv
}
