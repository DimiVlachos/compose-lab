package dev.dimvlachos.lab.core.presentation.components.keyhook

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** What hangs on a keyring. */
internal sealed class KeyItem {
    /** How long it hangs below the ring, in the item's own units. */
    abstract val length: Float

    /** A cut key in [metal], [size] of a full house key. */
    class Key(val metal: KeyMetal, val size: Float = 1f) : KeyItem() {
        override val length = 27f * size
    }

    /** A car's remote fob. */
    data object Fob : KeyItem() {
        override val length = 23f
    }

    /** A leather tag in [color] (`#rrggbb`) with [text] stamped in it. */
    class Tag(val color: String, val text: String) : KeyItem() {
        override val length = 23f

        // The shades the prototype mixes from its colour: the strap, and the face's light and dark.
        internal val strap = mixColor(color, 0, .35f)
        internal val lit = mixColor(color, 255, .18f)
        internal val deep = mixColor(color, 0, .3f)
    }

    /** A fishing bobber. */
    data object Bobber : KeyItem() {
        override val length = 17f
    }
}

/**
 * [hex] (`#rrggbb`) mixed [t] of the way to the grey [to] (0 black, 255 white), as the prototype's
 * `mix()`.
 */
internal fun mixColor(hex: String, to: Int, t: Float): Color {
    val n = hex.removePrefix("#").toInt(16)
    fun ch(v: Int) = (v + (to - v) * t).roundToInt()
    return Color(ch(n shr 16 and 255), ch(n shr 8 and 255), ch(n and 255))
}

internal enum class KeyMetal {
    Brass,
    Nickel,
}

/**
 * Someone in the household: their [name], the [plate] over their hook, their [initials] and avatar
 * [color] (`#rrggbb`), and the [items] on their keyring (two or three).
 */
internal class KeyHolder(
    val name: String,
    val first: String,
    val plate: String,
    val initials: String,
    val color: String,
    val items: List<KeyItem>,
) {
    init {
        require(items.size in 2..3) { "A keyring holds two or three things" }
    }
}

/** Something the rack does that is heard or felt; its strength is 0 to 1. */
internal enum class KeyRackEvent {
    /** Keys knocking together. */
    Tink,
    /** A set landing on its hook. */
    Clack,
    /** A set picked up. */
    Lift,
    /** The card's clip snapping shut on a set. */
    ClipShut,
    /** The card's clip snapping shut behind a set that has left it. */
    ClipShutEmpty,
}

/**
 * The snap clip under the account card, in U from the card's foot straight under its strap, as the
 * prototype's `CLIP_*`: the bend's centre and radius, the gate's hinge, the nose its free end
 * latches on, and the path a ring takes through it — E outside the mouth, M where the shut gate
 * runs, G the gap the open gate leaves at the nose, B the bottom of the bend, where the ring hangs.
 */
internal object SnapClip {
    const val R = 5.2f
    const val CenterX = 0f
    const val CenterY = 24.8f
    const val HingeX = .8f
    const val HingeY = 14f
    const val NoseX = 5.2f
    const val NoseY = 19.4f

    /** How far the gate swings in, in radians, fully open. */
    const val GateOpen = .9f

    /** A full pass through the clip, from outside to the bend, in ms. */
    const val ThreadMs = 300f

    // E, M, G, B.
    val PathX = floatArrayOf(7.26f, 3f, 2.1f, 0f)
    val PathY = floatArrayOf(13.23f, 16.7f, 20.2f, 30f)
    val PathLen =
        FloatArray(4).also {
            for (i in 1 until 4) {
                it[i] = it[i - 1] + hypot(PathX[i] - PathX[i - 1], PathY[i] - PathY[i - 1])
            }
        }

    /** The bottom of the bend, where a ring hangs, below the card's foot. */
    val Drop = PathY[3]

    /** Where along the path [c] (1 at E outside, 0 at B in the bend) is: x (or y if [y]) in U. */
    fun point(c: Float, y: Boolean): Float {
        val d = (1f - c) * PathLen[3]
        var i = 1
        while (i < 3 && PathLen[i] < d) i++
        val t = ((d - PathLen[i - 1]) / (PathLen[i] - PathLen[i - 1])).coerceIn(0f, 1f)
        val a = if (y) PathY else PathX
        return a[i - 1] + (a[i] - a[i - 1]) * t
    }
}

/**
 * The clip's spring gate, as the prototype's `gate`: [angle] 0 shut on the nose to 1 swung in.
 * Opening, a ring pushes it in against its spring; let go, the spring snaps it back onto the nose,
 * where it bounces a little and rests.
 */
internal class ClipGate {
    var angle = 0f
        private set

    internal var speed = 0f
        private set

    private var armed = false

    /**
     * Moves the gate toward [open] for [h] seconds; with [reduced] it is simply open or shut.
     * Returns the strength of a snap onto the nose (0 for none).
     */
    fun step(h: Float, open: Boolean, reduced: Boolean): Float {
        if (reduced) {
            angle = if (open) 1f else 0f
            speed = 0f
            armed = false
            return 0f
        }
        val k = if (open) 1400f else 2600f
        val z = if (open) 1f else .3f
        val to = if (open) 1f else -.3f
        speed += (k * (to - angle) - 2f * sqrt(k) * z * speed) * h
        angle += speed * h
        if (angle > .5f) armed = true
        if (angle > 1f) {
            angle = 1f
            speed = min(0f, speed)
        }
        var snap = 0f
        if (angle < 0f) {
            angle = 0f
            if (armed) {
                armed = false
                snap = (-speed / 40f).coerceIn(.4f, 1f)
            }
            speed = if (speed < -4f) -speed * .25f else 0f
        }
        return snap
    }

    /** Shut and still on the nose. */
    val shut: Boolean
        get() = angle == 0f && speed == 0f

    fun reset() {
        angle = 0f
        speed = 0f
        armed = false
    }
}

/**
 * The rack's geometry for a hero [width] × [height] (dp), as the prototype's `layout()` works it
 * out: [u] scales the rack, [ks] the keys, which grow into whatever height the screen gives them.
 * [cardBottom] is the account card's foot, where the snap clip hangs.
 */
internal class RackGeometry(val width: Float, val height: Float, val count: Int) {
    val cardBottom = CardBottom
    val u = (min(width / 330f, height / 380f)).coerceIn(.8f, 1.35f)
    val ks =
        if (height > 460f) min(1.75f, 1.45f + (height - 460f) / 150f)
        else ((height - 300f) / 80f).coerceIn(1.1f, 1.45f)
    val boardTop = height - (62f + 18f * ks + 50f * ks + 14f) * u
    val boardL = 12f * u
    val boardR = width - 12f * u
    val carryX = width * .76f
    val carryY = cardBottom + SnapClip.Drop * u

    /** The clip's point [c] along its path, in dp (see [SnapClip.point]). */
    fun clipX(c: Float) = carryX + SnapClip.point(c, false) * u

    fun clipY(c: Float) = cardBottom + SnapClip.point(c, true) * u

    fun hookX(i: Int): Float {
        val inner = boardR - boardL - 40f * u
        return boardL + 20f * u + inner * (i + .5f) / count + 5f * u
    }

    val hookY = boardTop + 62f * u

    /**
     * Where the hint's top goes when it is [hintHeight] tall: under the card, easing down to midway
     * between the card and the board when there's a lot of wall between them.
     */
    fun hintTop(hintHeight: Float): Float {
        val free = boardTop - (cardBottom + 16f) - hintHeight
        return cardBottom + 16f + min(free / 2f, max(0f, (free - 118f) * 1.5f))
    }

    internal companion object {
        /** The account card is 62 dp tall at the hero's top. */
        const val CardBottom = 62f
    }
}

/** One set of keys, as the prototype's `rings[i]`. */
internal class KeyRing(val holder: KeyHolder, val index: Int) {
    internal enum class Mode {
        Rest,
        Drag,
        Fly,
    }

    var mode = Mode.Rest
        internal set

    // The anchor: where it is, how fast it moves and is pushed, and where it aims.
    var x = 0f
        internal set

    var y = 0f
        internal set

    internal var vx = 0f
    internal var vy = 0f
    internal var ax = 0f
    internal var ay = 0f
    internal var tx = 0f
    internal var ty = 0f
    internal var offX = 0f
    internal var offY = 0f

    /** The ring's swing, in radians, and its speed. */
    var th = 0f
        internal set

    internal var om = 0f

    /** 0 hanging still to 1 lifted off: the shadow grows and the set scales up a little. */
    var lift = 0f
        internal set

    internal var cool = 0f

    // The flight: how long it has flown (ms, negative while it waits), for how long, from where,
    // over what.
    internal var flyEl = 0f
    internal var flyMs = 0f
    internal var fromX = 0f
    internal var fromY = 0f
    internal var ctrlX = 0f
    internal var ctrlY = 0f

    /**
     * Whether the ring is threaded on the card's clip, and where along it (1 out at its mouth, 0 in
     * its bend).
     */
    var on = false
        internal set

    var clip = 0f
        internal set

    // The clip pass under way, if any: from where, to where, how far in and for how long (ms).
    internal var passing = false
    internal var passFrom = 0f
    internal var passTo = 0f
    internal var passT = 0f
    internal var passMs = 0f

    /** Each item's angle and speed. */
    val ph = FloatArray(holder.items.size)
    internal val pom = FloatArray(holder.items.size)

    /** Where each item settles with motion, for the current size. */
    internal val rest = FloatArray(holder.items.size)

    val bias: FloatArray
        get() = Bias[holder.items.size - 2]

    val slots: FloatArray
        get() = Slots[holder.items.size - 2]

    internal companion object {
        // Where each item hangs on the ring and how far it leans from the others at rest.
        val Slots = arrayOf(floatArrayOf(-.42f, .42f), floatArrayOf(-.72f, 0f, .72f))
        val Bias = arrayOf(floatArrayOf(-.2f, .26f), floatArrayOf(-.4f, .05f, .44f))
    }
}

/**
 * The key rack's sets and their physics, ported from the prototype's `rings`, `step()`, `sendTo()`
 * and its gestures. Each anchor follows its target on a damped spring; the ring is a pendulum swung
 * by its anchor's acceleration and each item a shorter pendulum loosely tied to the ring.
 *
 * Whoever's set is clipped to the card ([carried]) is signed in once it lands there ([signedIn]);
 * hanging it on the rack signs them out. [time] is the rack's own clock, in ms.
 */
@Stable
internal class KeyRackState(
    val holders: List<KeyHolder>,
    private val startCarried: Int? = 0,
    private val random: Random = Random.Default,
) {
    var reducedMotion = false
        set(value) {
            if (field == value) return
            field = value
            wake++
        }

    /** Hears what the rack does, with its strength. */
    var onEvent: ((KeyRackEvent, Float) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something sets the rack moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    val rings = holders.mapIndexed { i, h -> KeyRing(h, i) }

    /** The card's clip's spring gate. */
    val gate = ClipGate()

    /** Whose set is on the card, or flying there: -1 for nobody. */
    var carriedIndex by mutableIntStateOf(startCarried ?: -1)
        private set

    /** Who is signed in: changes only once a set lands, as the prototype's `shown`. */
    var signedIn by mutableStateOf(startCarried?.let { holders[it] })
        private set

    var geometry: RackGeometry? = null
        private set

    var time = 0.0
        private set

    private class Grab(
        val ring: KeyRing,
        val scripted: Boolean,
        val t0: Double,
        val x0: Float,
        val y0: Float,
    ) {
        var moved = 0f
    }

    private var grab: Grab? = null
    private var fingX = 0f
    private var fingY = 0f

    // Tinks after a landing, at a time on the clock, with a strength.
    private val laterAt = DoubleArray(MaxLater)
    private val laterK = FloatArray(MaxLater)
    private var laterCount = 0

    /** The set being carried by a finger, or null. */
    val grabbed: KeyRing?
        get() = grab?.ring

    /** Whether the set in hand is the script's. */
    val grabIsScripted: Boolean
        get() = grab?.scripted == true

    init {
        reset()
    }

    /** Lays the rack out for a hero [width] × [height] dp; sets at rest move to their places. */
    fun layout(width: Float, height: Float) {
        val g = geometry
        if (g != null && g.width == width && g.height == height) return
        if (width <= 0f || height <= 0f) return
        val ng = RackGeometry(width, height, holders.size)
        geometry = ng
        for (r in rings) {
            for (j in r.ph.indices) r.rest[j] = restPh(ng, r, j)
            if (r.mode == KeyRing.Mode.Rest) {
                destX(r).let {
                    r.x = it
                    r.tx = it
                }
                destY(r).let {
                    r.y = it
                    r.ty = it
                }
                r.vx = 0f
                r.vy = 0f
            }
            if (reducedMotion) for (j in r.ph.indices) r.ph[j] = r.rest[j]
        }
        wake++
        frame++
    }

    /** Back to the start: the first set on the card and the rest on their hooks, all still. */
    fun reset() {
        grab = null
        laterCount = 0
        carriedIndex = startCarried ?: -1
        gate.reset()
        for (r in rings) {
            r.mode = KeyRing.Mode.Rest
            r.on = r.index == startCarried
            r.clip = 0f
            r.passing = false
            if (geometry != null) {
                r.x = destX(r)
                r.y = destY(r)
            }
            r.tx = r.x
            r.ty = r.y
            r.vx = 0f
            r.vy = 0f
            r.th = 0f
            r.om = 0f
            r.lift = 0f
            for (j in r.ph.indices) {
                r.ph[j] = if (reducedMotion && geometry != null) r.rest[j] else r.bias[j]
                r.pom[j] = 0f
            }
        }
        signedIn = startCarried?.let { holders[it] }
        wake++
        frame++
    }

    private fun isCarried(r: KeyRing) = carriedIndex == r.index

    fun destX(r: KeyRing): Float {
        val g = checkNotNull(geometry)
        return if (isCarried(r)) g.carryX else g.hookX(r.index)
    }

    fun destY(r: KeyRing): Float {
        val g = checkNotNull(geometry)
        return if (isCarried(r)) g.carryY else g.hookY
    }

    // Where a flight ends: a set bound for the card first flies to the clip's mouth, then threads
    // in.
    private fun flyToX(r: KeyRing) =
        if (isCarried(r) && !r.on) checkNotNull(geometry).clipX(1f) else destX(r)

    private fun flyToY(r: KeyRing) =
        if (isCarried(r) && !r.on) checkNotNull(geometry).clipY(1f) else destY(r)

    // Which way a ring on the clip is headed: down into the bend (0), or out (1).
    private fun clipGoal(r: KeyRing) =
        if (r.mode == KeyRing.Mode.Rest || (r.mode == KeyRing.Mode.Fly && isCarried(r))) 0f else 1f

    /**
     * Whether some ring wants the clip's gate open: passing its gap, arriving, or carried close.
     */
    private fun gateWanted(): Boolean {
        val geo = geometry ?: return false
        val ex = geo.clipX(1f)
        val ey = geo.clipY(1f)
        for (r in rings) {
            if (r.on) {
                val wants =
                    if (clipGoal(r) == 1f) !(r.mode == KeyRing.Mode.Fly && r.flyEl < 0f)
                    else r.clip > .25f
                if (wants) return true
            } else if (r.mode == KeyRing.Mode.Fly && isCarried(r) && r.flyMs - r.flyEl < 160f) {
                return true
            } else if (r.mode == KeyRing.Mode.Drag && hypot(r.x - ex, r.y - ey) < 36f * geo.u) {
                return true
            }
        }
        return false
    }

    /**
     * Moves [r] along the clip toward [goal] for [h] s, eased; it can't pass the gap until the gate
     * is open. Returns whether it got there.
     */
    private fun moveClip(r: KeyRing, goal: Float, h: Float): Boolean {
        if (!r.passing || r.passTo != goal) {
            r.passing = true
            r.passFrom = r.clip
            r.passTo = goal
            r.passT = 0f
            r.passMs = SnapClip.ThreadMs * max(.3f, abs(goal - r.clip))
        }
        val t = min(r.passMs, r.passT + h * 1000f)
        var c = r.passFrom + (r.passTo - r.passFrom) * ease(t / r.passMs)
        var held = false
        if (gate.angle < .8f) {
            if (goal == 0f && r.clip >= .75f && c < .75f) {
                c = .75f
                held = true
            }
            if (goal == 1f && r.clip <= .25f && c > .25f) {
                c = .25f
                held = true
            }
        }
        if (!held) r.passT = t
        r.clip = c
        if (r.passT >= r.passMs) {
            r.clip = goal
            r.passing = false
            return true
        }
        return false
    }

    private val u
        get() = geometry?.u ?: 1f

    private val ks
        get() = geometry?.ks ?: 1.4f

    /** The ring's radius. */
    val ringRadius
        get() = 9f * u * ks

    fun ringCenterX(r: KeyRing) = r.x + sin(r.th) * ringRadius

    fun ringCenterY(r: KeyRing) = r.y + cos(r.th) * ringRadius

    /** A point on the set, a finger's width below its ring, where the script takes hold of it. */
    fun ringPointX(r: KeyRing) = ringCenterX(r) + sin(r.th) * 24f * u * ks

    fun ringPointY(r: KeyRing) = ringCenterY(r) + cos(r.th) * 24f * u * ks

    /** The set whose ring or keys are under ([x], [y]), or null. */
    fun hitTest(x: Float, y: Float): KeyRing? {
        if (geometry == null) return null
        var best: KeyRing? = null
        var bd = 24f * u * ks
        for (r in rings) {
            val dx = sin(r.th)
            val dy = cos(r.th)
            val d =
                distSeg(
                    x,
                    y,
                    r.x - dx * 4f * u,
                    r.y - dy * 4f * u,
                    ringCenterX(r) + dx * 44f * u * ks,
                    ringCenterY(r) + dy * 44f * u * ks,
                )
            if (d < bd) {
                bd = d
                best = r
            }
        }
        return best
    }

    /** A finger takes hold of [r] at ([x], [y]). */
    fun startGrab(r: KeyRing, x: Float, y: Float, scripted: Boolean = false) {
        // One hand at a time: whatever was held is let go first.
        if (grab != null) letGo()
        grab = Grab(r, scripted, time, x, y)
        fingX = x
        fingY = y
        r.mode = KeyRing.Mode.Drag
        r.offX = r.x - x
        r.offY = r.y - y
        // With no motion a set comes straight off the clip; otherwise it slides out through the
        // gate.
        if (reducedMotion) {
            r.on = false
            r.passing = false
        }
        wake++
        emit(KeyRackEvent.Lift, 1f)
    }

    /** The finger holding a set moves to ([x], [y]); [scripted] says whose finger it is. */
    fun moveGrab(x: Float, y: Float, scripted: Boolean = false) {
        val g = grab ?: return
        if (g.scripted != scripted) return
        g.moved = max(g.moved, hypot(x - g.x0, y - g.y0))
        fingX = x
        fingY = y
    }

    /**
     * The finger lets go at ([x], [y]): a quick touch that hardly moved toggles the set; a drop
     * above the rack clips it to the card, anywhere on the rack hangs it on its own hook.
     */
    fun endGrab(x: Float, y: Float, scripted: Boolean = false) {
        val g = grab ?: return
        if (g.scripted != scripted) return
        release(g, x, y)
    }

    private fun release(g: Grab, x: Float, y: Float) {
        grab = null
        val r = g.ring
        val geo = geometry
        if (geo == null) {
            r.mode = KeyRing.Mode.Rest
            return
        }
        if (g.moved < 8f * u && time - g.t0 < 400f) toggle(r)
        else if (y < geo.boardTop - 8f * u) take(r) else hang(r)
    }

    /** Lets go of the set in hand where the finger last was. */
    fun letGo() {
        grab?.let { release(it, fingX, fingY) }
    }

    /** Signs [r]'s holder in, or out if they are. */
    fun toggle(r: KeyRing) {
        if (isCarried(r)) hang(r) else take(r)
    }

    private fun take(r: KeyRing) {
        val old = carriedIndex
        carriedIndex = r.index
        if (old >= 0 && old != r.index) sendTo(rings[old], 70f)
        sendTo(r)
    }

    private fun hang(r: KeyRing) {
        if (isCarried(r)) carriedIndex = -1
        sendTo(r)
    }

    private fun sendTo(r: KeyRing, delay: Float = 0f) {
        wake++
        if (geometry == null) return
        if (reducedMotion) {
            val toX = destX(r)
            val toY = destY(r)
            r.mode = KeyRing.Mode.Rest
            r.x = toX
            r.y = toY
            r.tx = toX
            r.ty = toY
            r.on = isCarried(r)
            r.clip = 0f
            r.passing = false
            land(r)
            return
        }
        // Still on the clip, it threads in or out first; the flight home starts at the mouth.
        if (r.on) {
            r.mode = KeyRing.Mode.Fly
            r.flyEl = -delay
            r.flyMs = 0f
            return
        }
        startFly(r, delay)
    }

    private fun startFly(r: KeyRing, delay: Float) {
        val toX = flyToX(r)
        val toY = flyToY(r)
        val d = hypot(toX - r.x, toY - r.y)
        r.mode = KeyRing.Mode.Fly
        r.flyEl = -delay
        r.flyMs = (300f + d * .9f).coerceIn(360f, 640f)
        r.fromX = r.x
        r.fromY = r.y
        if (isCarried(r)) {
            // To the card it swings out past the clip and comes back into its mouth from outside.
            val out = min(30f * u, d * .5f)
            r.ctrlX = toX + out
            r.ctrlY = toY - out * .15f
        } else {
            r.ctrlX = (r.x + toX) / 2f
            r.ctrlY = min(r.y, toY) - min(70f * u, d * .35f)
        }
    }

    private fun land(r: KeyRing) {
        val speed = hypot(r.vx, r.vy)
        r.mode = KeyRing.Mode.Rest
        // On a hook the set lands with a clack; on the card the clip's gate has just snapped shut
        // on it (with no motion, now).
        if (isCarried(r)) {
            r.on = true
            r.clip = 0f
            r.passing = false
            if (reducedMotion) emit(KeyRackEvent.ClipShut, 1f)
        } else emit(KeyRackEvent.Clack, (speed / 900f).coerceIn(.25f, 1f))
        for (i in 0 until 3) {
            later(
                time + 30.0 + i * (40.0 + random.nextFloat() * 50.0),
                .5f + random.nextFloat() * .4f,
            )
        }
        applySignedIn()
    }

    private fun later(at: Double, k: Float) {
        if (laterCount == MaxLater) return
        laterAt[laterCount] = at
        laterK[laterCount] = k
        laterCount++
    }

    private fun applySignedIn() {
        val c = carriedIndex
        val who: KeyHolder? =
            if (c < 0) null
            else {
                val r = rings[c]
                if (r.mode == KeyRing.Mode.Rest) r.holder else return
            }
        if (who !== signedIn) signedIn = who
    }

    /**
     * Whether every set hangs still where it belongs, nothing is in hand and no tink is waiting:
     * the frame loop can stop until [wake] changes.
     */
    val settled: Boolean
        get() {
            if (grab != null || laterCount > 0 || geometry == null) return false
            if (!gate.shut) return false
            for (r in rings) {
                if (r.mode != KeyRing.Mode.Rest) return false
                if (reducedMotion) continue
                if (abs(r.x - r.tx) > RestGap || abs(r.y - r.ty) > RestGap) return false
                if (abs(r.vx) > RestSpeed || abs(r.vy) > RestSpeed) return false
                if (abs(r.th) > AngleGap || abs(r.om) > AngleSpeed || r.lift > AngleGap)
                    return false
                for (j in r.ph.indices) {
                    if (abs(r.ph[j] - r.rest[j]) > AngleGap || abs(r.pom[j]) > AngleSpeed) {
                        return false
                    }
                }
            }
            return true
        }

    /** Advances [seconds] in small fixed substeps, which keep the stiff springs stable. */
    fun step(seconds: Float) {
        if (geometry == null) return
        var left = seconds
        while (left > 0f) {
            val h = min(left, Substep)
            substep(h)
            left -= h
        }
        frame++
    }

    private fun substep(h: Float) {
        time += h * 1000.0
        // The tinks after a landing, each on its time.
        var i = 0
        while (i < laterCount) {
            if (laterAt[i] <= time) {
                emit(KeyRackEvent.Tink, laterK[i])
                laterCount--
                laterAt[i] = laterAt[laterCount]
                laterK[i] = laterK[laterCount]
            } else i++
        }
        val geo = checkNotNull(geometry)
        val u = geo.u
        val ks = geo.ks
        val snap = gate.step(h, gateWanted(), reducedMotion)
        if (snap > 0f) {
            val seated = rings.any { it.on }
            emit(if (seated) KeyRackEvent.ClipShut else KeyRackEvent.ClipShutEmpty, snap)
        }
        for (r in rings) {
            val threading = r.on && r.mode != KeyRing.Mode.Rest
            if (threading) {
                // On the clip: in through the gap and down into the bend, or out the way it came.
                val goal = clipGoal(r)
                var done = false
                if (goal == 1f && r.mode == KeyRing.Mode.Fly && r.flyEl < 0f) r.flyEl += h * 1000f
                else done = moveClip(r, goal, h)
                r.tx = geo.clipX(r.clip)
                r.ty = geo.clipY(r.clip)
                if (done && goal == 0f) land(r)
                else if (done) {
                    r.on = false
                    if (r.mode == KeyRing.Mode.Drag) {
                        r.offX = r.tx - fingX
                        r.offY = r.ty - fingY
                    } else startFly(r, 0f)
                }
            } else
                when (r.mode) {
                    KeyRing.Mode.Drag -> {
                        // The anchor slides sideways until it's straight above the fingertip but
                        // keeps
                        // its height, so the keys never drop on pickup.
                        val k = min(1f, h * 12f)
                        r.offX += (0f - r.offX) * k
                        r.offY = min(r.offY, -2f * u)
                        // Dragged low, the set stops where its bottom (and shadow) still fits.
                        r.tx = fingX + r.offX
                        r.ty = min(fingY + r.offY, geo.height - (70f * ks + 28f) * u)
                    }
                    KeyRing.Mode.Fly -> {
                        r.flyEl += h * 1000f
                        val p = (r.flyEl / r.flyMs).coerceIn(0f, 1f)
                        val e = ease(p)
                        val q = 1f - e
                        r.tx = q * q * r.fromX + 2f * q * e * r.ctrlX + e * e * flyToX(r)
                        r.ty = q * q * r.fromY + 2f * q * e * r.ctrlY + e * e * flyToY(r)
                        // Bound for the card it reaches the clip's mouth and threads on; for a
                        // hook, it
                        // lands.
                        if (r.flyEl >= r.flyMs) {
                            if (isCarried(r)) {
                                r.on = true
                                r.clip = 1f
                                r.passing = false
                            } else land(r)
                        }
                    }
                    KeyRing.Mode.Rest -> {
                        r.tx = destX(r)
                        r.ty = destY(r)
                    }
                }
            if (reducedMotion) {
                r.x = r.tx
                r.y = r.ty
                r.vx = 0f
                r.vy = 0f
                r.th = 0f
                r.om = 0f
                for (j in r.ph.indices) {
                    r.ph[j] = r.rest[j]
                    r.pom[j] = 0f
                }
                continue
            }
            val resting = r.mode == KeyRing.Mode.Rest
            if (r.on && !resting) {
                // Threaded on the clip the ring goes exactly where the clip leads it; the pendulum
                // still feels each turn.
                val vx = (r.tx - r.x) / h
                val vy = (r.ty - r.y) / h
                r.ax = (vx - r.vx) / h
                r.ay = (vy - r.vy) / h
                r.vx = vx
                r.vy = vy
                r.x = r.tx
                r.y = r.ty
            } else {
                val k =
                    when (r.mode) {
                        KeyRing.Mode.Rest -> 700f
                        KeyRing.Mode.Fly -> 1500f
                        KeyRing.Mode.Drag -> 1100f
                    }
                val z = if (resting) .7f else .9f
                val c = 2f * sqrt(k) * z
                r.ax = k * (r.tx - r.x) - c * r.vx
                r.ay = k * (r.ty - r.y) - c * r.vy
                r.vx += r.ax * h
                r.vy += r.ay * h
                r.x += r.vx * h
                r.y += r.vy * h
            }
            r.lift += ((if (resting) 0f else 1f) - r.lift) * min(1f, h * 9f)
            val g = 2600f * u
            val ax = r.ax
            val ay = r.ay
            // A pendulum swung by its pivot: the pivot's own acceleration is what moves it.
            r.om += ((-(g - ay) * sin(r.th) - ax * cos(r.th)) / (30f * u * ks) - 1.5f * r.om) * h
            r.th += r.om * h
            val bias = r.bias
            for (j in r.ph.indices) {
                val l = r.holder.items[j].length * u * ks
                val b = bias[j]
                val ph = r.ph[j]
                r.pom[j] +=
                    ((-(g - ay) * sin(ph) - ax * cos(ph)) / l + 34f * (r.th + b - ph) -
                        3.2f * (r.pom[j] - r.om) -
                        .6f * r.pom[j]) * h
                r.ph[j] += r.pom[j] * h
            }
            r.th = r.th.coerceIn(-2.6f, 2.6f)
            // The keys knock together whenever they're moving fast: a tink, with a short cooldown.
            r.cool -= h
            var spread = 0f
            for (j in r.ph.indices) spread += abs(r.pom[j] - r.om)
            val e = abs(r.om) + spread * .35f
            if (e > 4.2f && r.cool <= 0f) {
                r.cool = .09f + random.nextFloat() * .08f
                emit(KeyRackEvent.Tink, min(1f, e / 12f))
            }
        }
    }

    private fun emit(event: KeyRackEvent, strength: Float) {
        onEvent?.invoke(event, strength)
    }

    internal companion object {
        private const val Substep = 1f / 240f
        private const val MaxLater = 24
        private const val RestGap = .05f
        private const val RestSpeed = .5f
        private const val AngleGap = .002f
        private const val AngleSpeed = .003f

        fun ease(t: Float): Float =
            if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f).let { it * it } / 2f

        /**
         * The angle item [j] settles at with motion: gravity against its pull toward its bias, (g /
         * L) sin φ = 34 (b − φ), solved by Newton's method as the prototype's `restPh()`.
         */
        fun restPh(g: RackGeometry, r: KeyRing, j: Int): Float {
            val b = r.bias[j]
            val q = 2600f * g.u / (r.holder.items[j].length * g.u * g.ks)
            var ph = b
            repeat(6) { ph -= (q * sin(ph) - 34f * (b - ph)) / (q * cos(ph) + 34f) }
            return ph
        }

        private fun distSeg(
            px: Float,
            py: Float,
            ax: Float,
            ay: Float,
            bx: Float,
            by: Float,
        ): Float {
            val dx = bx - ax
            val dy = by - ay
            val den = dx * dx + dy * dy
            val t =
                (((px - ax) * dx + (py - ay) * dy) / (if (den == 0f) 1f else den)).coerceIn(0f, 1f)
            return hypot(px - ax - dx * t, py - ay - dy * t)
        }
    }
}
