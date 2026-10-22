package dev.dimvlachos.lab.core.presentation.components.locket

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.TimeMark

/** Something the locket does that is heard or felt. */
internal enum class LocketEvent {
    /** The catch lets go and the lid springs open. */
    Open,
    /** The lid snaps shut. */
    Snap,
    /** A photo is picked up from Recent. */
    Lift,
    /** A photo settles behind the glass. */
    Place,
    /** A photo let go away from the locket slides back to its slot. */
    Back,
    /** The locket, swung by hand, is let go on its chain. */
    Chain,
}

/**
 * The locket's design units, as the prototype's: a 300 × 380 box, the chain hung from ([AX], [AY]),
 * the locket's centre [BC] under it, its rim [RX] × [RY] and the photo's oval [PRX] × [PRY].
 */
internal object Locket {
    const val AX = 150f
    const val AY = -16f
    const val CL = 92f
    const val BC = 164f
    const val RX = 50f
    const val RY = 62f
    const val PRX = 39f
    const val PRY = 51f

    /** Open stops at 150°, which keeps the swung lid inside the hero. */
    const val OPEN = 2.62f

    /**
     * The overshoot past [OPEN] stops here, so the lid's free edge stays clear of the box's side.
     */
    const val LIDMAX = 2.75f

    /** How far a press moves before it is a drag. */
    const val SLOP = 5f
    const val THUMB = 58f
    const val TY = 304f

    /** The swing keeps the whole locket, lid included, at least this far inside the box. */
    const val MARGIN = 12f

    /** The photos in Recent, in order. */
    const val PHOTOS = 4

    /** How long text, the selection and the header avatar crossfade, in ms. */
    const val FADE_MS = 250f

    /** The prototype's fade curve, smoothstep. */
    fun fadeEase(t: Float) = t * t * (3f - 2f * t)

    /** A Recent thumb's centre in the strip's own units. */
    fun tcx0(j: Int) = 16f + THUMB / 2f + j * 70f
}

/**
 * Where the locket's 300 × 380 box sits in a hero of [width] by [height] dp, as the prototype's
 * `layout()`: scaled by [s], no more than 1.45 times, and centred. The Recent strip never scales
 * below its dp size: it sits in its own frame, bottom-aligned with the box, at [sk] of the box's
 * units, so a thumb is [ts] box units and its centre is at ([tcx], [tcy]).
 */
internal class LocketBox(val width: Float, val height: Float) {
    val s = minOf(width / 300f, height / 380f, 1.45f).coerceAtLeast(1e-3f)
    val ox = (width - 300f * s) / 2f
    val oy = (height - 380f * s) / 2f
    val sk = min(max(s, 1f), width / 300f) / s
    val sdx = 150f - 150f * sk
    val sdy = 380f - 380f * sk
    val ts = Locket.THUMB * sk
    val tcy = sdy + (Locket.TY + Locket.THUMB / 2f) * sk

    fun tcx(j: Int) = sdx + Locket.tcx0(j) * sk

    /** A point in the hero, in dp, as box units. */
    fun toDesignX(x: Float) = (x - ox) / s

    fun toDesignY(y: Float) = (y - oy) / s
}

/** What a finger holds: a Recent photo, the open lid, or the locket on its chain. */
internal enum class DragKind {
    Photo,
    Lid,
    Swing,
}

/** A press on the hero, from where it went down, in box units. */
internal class LocketDrag(val kind: DragKind, val x0: Float, val y0: Float) {
    var j = -1
    var ox = 0f
    var oy = 0f
    var x = 0f
    var y = 0f
    var moved = false
    var grab = 0f
    var th0 = 0f
    var f0 = 0f
    // The last few samples of the lid's angle or the chain's, for the speed at release.
    val sampleT = FloatArray(Samples)
    val sampleV = FloatArray(Samples)
    var samples = 0

    fun sample(t: Float, v: Float) {
        if (samples == Samples) {
            sampleT.copyInto(sampleT, 0, 1)
            sampleV.copyInto(sampleV, 0, 1)
            samples--
        }
        sampleT[samples] = t
        sampleV[samples] = v
        samples++
    }

    private companion object {
        const val Samples = 10
    }
}

/** A photo moving on its own: sliding home, or trimming itself into the locket. */
internal class LocketFlight(
    val j: Int,
    val x: Float,
    val y: Float,
    val size: Float,
    val t0: Float,
    val ms: Float,
)

/**
 * The locket's springs and gestures, ported from the prototype's `st`, `step()` and pointer code:
 * the lid's angle [a] on its hinge (0 shut, [Locket.OPEN] open), the chain's angle [th], the lift
 * of a carried photo, and the photos flying in and back. Points are in the 300 × 380 box's units;
 * [box] says where the Recent strip is.
 *
 * [now] is the locket's own clock in ms, which [step] advances; every animation is timed on it.
 */
@Stable
internal class LocketState(initialPhoto: Int = 2) {
    var reducedMotion = false

    /** Hears what the locket does. */
    var onEvent: ((LocketEvent) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something sets the locket moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    /** Where the strip is: set by the hero once it knows its size. */
    var box = LocketBox(300f, 380f)

    var now = 0f
        private set

    /** The photo in the locket, and the profile's. */
    var photo by mutableIntStateOf(initialPhoto)
        private set

    /** The photo trimming itself into the locket, if any, by index. */
    var placingPhoto by mutableIntStateOf(-1)
        private set

    /** Whether the locket is open, or opening: its lid is aimed open. */
    var isOpen by mutableStateOf(false)
        private set

    /** How many photos have been set in the locket. */
    var placed by mutableIntStateOf(0)
        private set

    var a = 0f
    var av = 0f
    var th = 0f
    var tw = 0f
    var lift = 0f
        private set

    var aTarget = 0f
        private set

    var drag: LocketDrag? = null
        private set

    var place: LocketFlight? = null
        private set

    val back = ArrayList<LocketFlight>()

    var snaps = 0
        private set

    var opens = 0
        private set

    /** When the snap's glint began, and the glass's sweep, on [now]; negative before the first. */
    var glint = -1f
        private set

    var glass = -1f
        private set

    /**
     * How far each Recent photo's accent border and check have faded in, 0 to 1: they fade out on
     * the old photo as they fade in on the new one, linearly over [Locket.FADE_MS].
     */
    val sel = FloatArray(Locket.PHOTOS).also { it[initialPhoto] = 1f }

    /** Bumped by [reset], which jumps rather than fades: text and the avatar snap with it. */
    var resets by mutableIntStateOf(0)
        private set

    // ---- Frames ----

    fun toWorldX(lx: Float, ly: Float) = Locket.AX + lx * cos(th) + ly * sin(th)

    fun toWorldY(lx: Float, ly: Float) = Locket.AY - lx * sin(th) + ly * cos(th)

    private fun localX(x: Float, y: Float): Float {
        val dx = x - Locket.AX
        val dy = y - Locket.AY
        return dx * cos(th) - dy * sin(th)
    }

    private fun localY(x: Float, y: Float): Float {
        val dx = x - Locket.AX
        val dy = y - Locket.AY
        return dx * sin(th) + dy * cos(th)
    }

    private fun inEll(x: Float, y: Float, cx: Float, cy: Float, rx: Float, ry: Float): Boolean {
        val u = (x - cx) / rx
        val v = (y - cy) / ry
        return u * u + v * v <= 1f
    }

    /** The lid's centre along the hinge's axis, as it turns. */
    fun lidCenter() = -Locket.RX + Locket.RX * cos(a)

    fun freeEdge() = -Locket.RX + 2f * Locket.RX * cos(a)

    fun overLocket(x: Float, y: Float, margin: Float): Boolean =
        inEll(localX(x, y), localY(x, y), 0f, Locket.BC, Locket.RX + margin, Locket.RY + margin)

    private fun overLid(x: Float, y: Float): Boolean =
        inEll(
            localX(x, y),
            localY(x, y),
            lidCenter(),
            Locket.BC,
            max(12f, Locket.RX * abs(cos(a))) + 6f,
            Locket.RY + 6f,
        )

    // At angle th an ellipse centred at local (cx, BC) with half-width hw reaches
    // x = AX + cx·cos th + BC·sin th ± √((hw·cos th)² + (RY·sin th)²).
    private fun edge(t: Float, cx: Float, hw: Float, side: Float) =
        Locket.AX + cx * cos(t) + Locket.BC * sin(t) + side * hypot(hw * cos(t), Locket.RY * sin(t))

    private inline fun solve(f: (Float) -> Float, k: Float): Float {
        var lo = -.75f
        var hi = .75f
        repeat(24) {
            val m = (lo + hi) / 2f
            if (f(m) < k) lo = m else hi = m
        }
        return (lo + hi) / 2f
    }

    /**
     * The chain angles that keep the whole locket [Locket.MARGIN] inside the box: on the left the
     * back with its knuckles, or the open lid with its catch; on the right the back with its catch
     * and shadow. Both edges move right as the angle grows, so each bound is found by bisection.
     */
    fun limitLo(): Float {
        val ca = cos(a)
        val lc = if (ca < 0f) lidCenter() else 0f
        val lw = if (ca < 0f) Locket.RX * -ca + 6f else Locket.RX + 3f
        return solve(
            { t -> min(edge(t, 0f, Locket.RX + 3f, -1f), edge(t, lc, lw, -1f)) },
            Locket.MARGIN,
        )
    }

    fun limitHi(): Float = HiLimit

    // The right bound is the back's alone, the same whatever the lid does: found once.
    private fun solveHi(): Float =
        solve({ t -> edge(t, 0f, Locket.RX + 14f, 1f) }, 300f - Locket.MARGIN)

    private fun thumbAt(x: Float, y: Float): Int {
        val b = box
        for (j in 0 until Locket.PHOTOS) {
            if (abs(x - b.tcx(j)) <= b.ts / 2f + 4f && abs(y - b.tcy) <= b.ts / 2f + 4f) return j
        }
        return -1
    }

    /** Whether photo [j] has left its slot: carried, flying in, or sliding home. */
    fun isAway(j: Int): Boolean {
        val d = drag
        if (d != null && d.kind == DragKind.Photo && d.j == j && d.moved) return true
        if (place?.j == j) return true
        for (b in back) if (b.j == j) return true
        return false
    }

    // ---- Actions ----

    fun open() {
        if (isOpen) return
        wake++
        aTarget = Locket.OPEN
        isOpen = true
        opens++
        if (reducedMotion) {
            a = Locket.OPEN
        } else {
            av = 6.5f
            tw -= .3f
        }
        emit(LocketEvent.Open)
    }

    fun close() {
        if (!isOpen) return
        wake++
        aTarget = 0f
        isOpen = false
        if (reducedMotion) {
            a = 0f
            snaps++
            emit(LocketEvent.Snap)
        } else {
            av = min(av, -1.5f)
        }
    }

    fun toggle() = if (isOpen) close() else open()

    /** Uses Recent photo [j] without a drag: it flies from its slot into the locket. */
    fun usePhoto(j: Int) {
        if (place != null) return
        startPlace(j, box.tcx(j), box.tcy, box.ts)
    }

    private fun startPlace(j: Int, x: Float, y: Float, size: Float) {
        // A photo dropped on the closed locket opens it on the way in.
        open()
        wake++
        place = LocketFlight(j, x, y, size, now, if (reducedMotion) 0f else 480f)
        placingPhoto = j
        emit(LocketEvent.Lift)
    }

    private fun finishPlace() {
        val j = place?.j ?: return
        place = null
        placingPhoto = -1
        photo = j
        placed++
        glass = now
        if (!reducedMotion) tw += .22f
        emit(LocketEvent.Place)
    }

    private fun flyBack(j: Int, x: Float, y: Float) {
        back += LocketFlight(j, x, y, 0f, now, if (reducedMotion) 0f else 320f)
        emit(LocketEvent.Back)
    }

    // ---- Input, in box units ----

    fun down(x: Float, y: Float) {
        // A press on a sleeping locket brings its clock up to now first, so its first sample is
        // timed where the finger came down.
        wakeUp()
        wake++
        val j = thumbAt(x, y)
        if (j >= 0 && place == null && back.none { it.j == j }) {
            drag =
                LocketDrag(DragKind.Photo, x, y).also {
                    it.j = j
                    it.ox = box.tcx(j) - x
                    it.oy = box.tcy - y
                    it.x = box.tcx(j)
                    it.y = box.tcy
                }
            return
        }
        if (isOpen && overLid(x, y)) {
            drag =
                LocketDrag(DragKind.Lid, x, y).also {
                    it.grab = freeEdge() - localX(x, y)
                    it.sample(now, a)
                }
            return
        }
        if (overLocket(x, y, 8f)) {
            drag =
                LocketDrag(DragKind.Swing, x, y).also {
                    it.th0 = th
                    it.f0 = atan2(x - Locket.AX, y - Locket.AY)
                    it.sample(now, th)
                }
            return
        }
        drag = null
    }

    fun move(x: Float, y: Float) {
        val d = drag ?: return
        wake++
        if (!d.moved && hypot(x - d.x0, y - d.y0) > Locket.SLOP) {
            d.moved = true
            if (d.kind == DragKind.Photo) emit(LocketEvent.Lift)
        }
        if (!d.moved) return
        // Every position comes from the offset taken at the press, so crossing the slop never
        // jumps.
        when (d.kind) {
            DragKind.Photo -> {
                d.x = x + d.ox
                d.y = y + d.oy
            }
            DragKind.Lid -> {
                val fe = (localX(x, y) + d.grab).coerceIn(-3f * Locket.RX, Locket.RX)
                a =
                    min(
                        Locket.LIDMAX,
                        acos(((fe + Locket.RX) / (2f * Locket.RX)).coerceIn(-1f, 1f)),
                    )
                av = 0f
                d.sample(now, a)
            }
            DragKind.Swing -> {
                val lo = limitLo()
                val hi = limitHi()
                th = (d.th0 + atan2(x - Locket.AX, y - Locket.AY) - d.f0).coerceIn(lo, hi)
                tw = 0f
                d.sample(now, th)
            }
        }
    }

    // The speed over the last ~100 ms of samples; a fast swipe may give only two, which is enough.
    private fun rate(d: LocketDrag): Float {
        var first = -1
        for (i in 0 until d.samples) {
            if (now - d.sampleT[i] <= 100f) {
                first = i
                break
            }
        }
        if (first < 0 || d.samples - first < 2) return 0f
        val last = d.samples - 1
        return (d.sampleV[last] - d.sampleV[first]) /
            max(.016f, (d.sampleT[last] - d.sampleT[first]) / 1000f)
    }

    fun up(x: Float, y: Float) {
        val d = drag ?: return
        drag = null
        wake++
        when (d.kind) {
            DragKind.Photo -> {
                if (!d.moved) {
                    usePhoto(d.j)
                    return
                }
                // Only where it is let go counts, so a quick swipe with few samples still lands.
                if (overLocket(d.x, d.y, 24f)) startPlace(d.j, d.x, d.y, box.ts * 1.15f * .9f)
                else flyBack(d.j, d.x, d.y)
            }
            DragKind.Lid -> {
                if (!d.moved) {
                    close()
                    return
                }
                val v = rate(d)
                if (a < 1.7f || v < -5f) {
                    av = min(v, -2f)
                    close()
                } else {
                    av = max(0f, v)
                }
            }
            DragKind.Swing -> {
                if (!d.moved) {
                    toggle()
                    return
                }
                // A flick can only swing it as far as the box allows: the speed is capped by the
                // pendulum's energy to that bound.
                val v = rate(d).coerceIn(-4f, 4f)
                val bound = if (v < 0f) limitLo() else limitHi()
                val vmax = sqrt(max(0f, 2f * Gravity * (cos(th) - cos(bound))))
                tw = if (reducedMotion) 0f else v.coerceIn(-vmax, vmax)
                emit(LocketEvent.Chain)
            }
        }
    }

    /** The touch was taken away: a carried photo slides home. */
    fun cancel() {
        val d = drag ?: return
        drag = null
        wake++
        if (d.kind == DragKind.Photo && d.moved) flyBack(d.j, d.x, d.y)
    }

    /** Back to the start: shut, still, with [initial] in it. */
    fun reset(initial: Int = 2) {
        a = 0f
        av = 0f
        aTarget = 0f
        isOpen = false
        th = 0f
        tw = 0f
        photo = initial
        for (j in sel.indices) sel[j] = if (j == initial) 1f else 0f
        resets++
        drag = null
        place = null
        placingPhoto = -1
        back.clear()
        glint = -1f
        glass = -1f
        lift = 0f
        placed = 0
        snaps = 0
        opens = 0
        wake++
        frame++
    }

    // ---- Time ----

    /**
     * Whether nothing moves: the lid and the chain at rest, no photo carried or flying, and the
     * glint and the glass's sweep over. The chain rests straight down, or, open, against the box's
     * side where the lid would cross it. The closed catch still glints now and then; see [restMs].
     */
    val settled: Boolean
        get() =
            drag == null &&
                place == null &&
                back.isEmpty() &&
                abs(a - aTarget) < RestGap &&
                abs(av) < RestSpeed &&
                abs(th - 0f.coerceIn(limitLo(), limitHi())) < RestGap &&
                abs(tw) < RestSpeed &&
                lift < RestGap &&
                sel.indices.all { sel[it] == if (it == photo) 1f else 0f } &&
                (glint < 0f || now - glint > GlintMs) &&
                (glass < 0f || now - glass > GlassMs)

    /**
     * How long, settled, the frame loop may sleep before something shows again: the closed catch's
     * glint comes round every 2π·520 ms. 0 while it shows; null while open, when nothing will.
     */
    fun restMs(): Float? {
        if (isOpen || a >= .02f) return null
        val tau = 2f * kotlin.math.PI.toFloat()
        val phase = ((now / 520f) % tau + tau) % tau
        // sin(phase)^8 > .02 between these phases.
        return when {
            phase in CatchOn..CatchOff -> 0f
            phase < CatchOn -> (CatchOn - phase) * 520f
            else -> (tau - phase + CatchOn) * 520f
        }
    }

    /** Lets [ms] pass at rest, without stepping the springs. */
    /** When the frame loop went to sleep, while it sleeps; [now] stands still meanwhile. */
    var sleepMark: TimeMark? = null

    /** Brings [now] up to the wall clock after a sleep, once. */
    fun wakeUp() {
        val mark = sleepMark ?: return
        sleepMark = null
        idle(mark.elapsedNow().inWholeMicroseconds / 1000f)
    }

    fun idle(ms: Float) {
        now += ms
        frame++
    }

    /** Advances [seconds] in small fixed substeps, which keep the stiff springs stable. */
    fun step(seconds: Float) {
        now += seconds * 1000f
        var left = seconds
        while (left > 0f) {
            val h = min(left, Substep)
            substep(h)
            left -= h
        }
        val pl = place
        if (pl != null && (pl.ms <= 0f || now - pl.t0 >= pl.ms)) finishPlace()
        back.removeAll { it.ms <= 0f || now - it.t0 >= it.ms }
        frame++
    }

    private fun substep(dt: Float) {
        val d = drag
        val lidHeld = d != null && d.kind == DragKind.Lid && d.moved
        if (reducedMotion) {
            if (!lidHeld) a = aTarget
            // Hanging straight, or, open, against the box's side, where it comes to rest.
            th = 0f.coerceIn(limitLo(), limitHi())
            tw = 0f
            av = 0f
            for (j in sel.indices) sel[j] = if (j == photo) 1f else 0f
            return
        }
        if (!lidHeld) {
            val opening = aTarget > 0f
            val k = if (opening) 150f else 420f
            val c = if (opening) 9f else 24f
            av += (k * (aTarget - a) - c * av) * dt
            a += av * dt
            if (a > Locket.LIDMAX) {
                a = Locket.LIDMAX
                av = -av * .3f
            }
            if (!opening && a <= 0f) {
                if (av < -.5f) {
                    snaps++
                    glint = now
                    tw += .9f
                    emit(LocketEvent.Snap)
                }
                a = 0f
                av = 0f
            }
        }
        val swingHeld = d != null && d.kind == DragKind.Swing && d.moved
        if (!swingHeld) {
            tw += (-Gravity * sin(th) - .9f * tw) * dt
            th += tw * dt
        }
        // The box's sides act as a soft wall for the pendulum.
        val lo = limitLo()
        val hi = limitHi()
        if (th < lo) {
            th = lo
            if (tw < 0f) tw = -tw * .3f
        } else if (th > hi) {
            th = hi
            if (tw > 0f) tw = -tw * .3f
        }
        val fade = dt * 1000f / Locket.FADE_MS
        for (j in sel.indices) sel[j] = (sel[j] + if (j == photo) fade else -fade).coerceIn(0f, 1f)
        val lifting = d != null && d.kind == DragKind.Photo && d.moved
        lift += ((if (lifting) 1f else 0f) - lift) * min(1f, dt * 18f)
    }

    private fun emit(event: LocketEvent) {
        onEvent?.invoke(event)
    }

    internal companion object {
        /** The pendulum's g/L, as the prototype's. */
        const val Gravity = 22f
        private const val Substep = 1f / 240f
        private val HiLimit = LocketState().solveHi()
        private const val RestSpeed = .004f
        private const val RestGap = .002f
        private const val GlintMs = 640f
        private const val GlassMs = 700f

        // sin^8 > .02 means sin > .613: phases .66 to 2.48 rad.
        private const val CatchOn = .6597f
        private const val CatchOff = 2.4819f
    }
}
