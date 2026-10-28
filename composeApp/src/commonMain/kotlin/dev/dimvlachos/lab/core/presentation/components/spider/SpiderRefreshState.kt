package dev.dimvlachos.lab.core.presentation.components.spider

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/** Something the spider does that is heard or felt. */
internal enum class SpiderEvent {
    /** A finger pulled it past the threshold: its eyes go wide. Felt as well as heard. */
    ArmByFinger,
    /** The refresh button lowered it to the threshold. */
    Arm,
    /** A pull went back above the threshold. */
    Disarm,
    /** Six tiny steps: it peeks out, or scurries home. */
    Patter,
    /** A long run of steps as it climbs its thread. */
    Climb,
    /** It spins its little ball of silk. */
    Spin,
    /** The ball opens into the new posts. */
    Catch,
    /** It docks and the posts settle at the top. */
    Settle,
    /** Nothing new: it comes back empty-handed. */
    Empty,
}

/** What the spider is doing, which sets how its legs move and where it looks. */
internal enum class SpiderMood {
    Idle,
    Peek,
    Scurry,
    Spin,
    Reel,
    Climb,
    Empty,
}

internal enum class SpiderPhase {
    Idle,
    Pulling,
    Retreat,
    Refresh,
}

/**
 * The spider refresh, ported from the prototype's state, `step()`, `refresh()`, `retreat()` and its
 * gesture. The feed's posts are ids ([posts]); the next batches are [batches]. The prototype's
 * awaited sequences are the legs of one state machine, so everything moves in [step].
 *
 * Positions are in dp in the feed's box, x from its left and y from its top. Durations are 1.3× the
 * prototype's, for the softer motion the native builds use; what follows the finger is not slowed.
 */
@Stable
internal class SpiderRefreshState(
    private val feed: List<Int> = DefaultFeed,
    private val batches: List<List<Int>> = DefaultBatches,
    private val random: Random = Random(7),
) {
    var reducedMotion = false

    /** Hears what the spider does. */
    var onEvent: ((SpiderEvent) -> Unit)? = null

    /** Bumped on every step and move, so a drawing that reads it redraws. */
    var frame by mutableIntStateOf(0)
        private set

    /** Whether anything moves, so the frame loop runs. */
    var awake by mutableStateOf(false)
        private set

    /** The feed's posts, top first. */
    val posts = mutableStateListOf<Int>().apply { addAll(feed) }

    /** The batch hanging below the spider on silk, while it is reeled in. */
    val hanging = mutableStateListOf<Int>()

    /** The posts marked New: the last batch, until the next refresh looks again. */
    var fresh by mutableStateOf(emptySet<Int>())
        private set

    /** Whether a refresh runs, so the button is off. */
    var busy by mutableStateOf(false)
        private set

    /** Whether "You're all caught up" shows. */
    var toast by mutableStateOf(false)
        private set

    /** What the live region says: -1 nothing, 0 caught up, else how many posts arrived. */
    var said by mutableIntStateOf(-1)
        private set

    var phase = SpiderPhase.Idle
        private set

    var mood = SpiderMood.Idle
        private set

    var armed = false
        private set

    /** The gap the pull opens above the list. */
    var pull = 0f
        private set

    var scroll = 0f
        private set

    var width = 0f
        private set

    var height = 0f
        private set

    /** The prototype's K: the spider's scale for the feed's width. */
    var k = 1f
        private set

    // The spider.
    var spiderY = -40f
        private set

    var swing = 0f
        private set

    var legPhase = 0f
        private set

    var pop = 0f
        private set

    var bundle = 0f
        private set

    var ballTurn = 0f
        private set

    var blink = 0f
        private set

    var lookX = 0f
        private set

    var lookY = 0f
        private set

    // The reel.
    var reeling = false
        private set

    var reelT = 0f
        private set

    var reelL = 0f
        private set

    var unfold = 0f
        private set

    var strand = 1f
        private set

    /** Each hanging card's offset below the first and its height, once measured. */
    val cardOffsets = FloatArray(MaxBatch)
    val cardHeights = FloatArray(MaxBatch)

    /** Glints running up the thread while it climbs: their y and alpha, [glints] of them. */
    val glintY = FloatArray(MaxGlints)
    val glintA = FloatArray(MaxGlints)
    var glints = 0
        private set

    /** How far the list is pushed down: the pull, or the reel's length while it reels. */
    val listShift: Float
        get() = if (reeling) reelL else pull

    /** The list's content height in dp, as laid out. */
    var listHeight = 0f

    val hiddenY: Float
        get() = -40f * k

    private var yOver = Float.NaN
    private var vs = 0f
    private var popV = 0f
    private var legSpeed = 1f
    private var time = 0f
    private var blinkAt = 2400f
    private var acc = 0f
    private var fling = 0f
    private var toastUntil = 0f
    private var batchIdx = 0
    private var grab: Grab? = null

    private var leg: Leg? = null
    private var legT = 0f
    private var legMs = 0f
    private var fromButton = false
    private var p0 = 0f
    private var s0 = 0f
    private var holdY = 0f
    private var t0 = 0f
    private var total = 0f

    private class Grab(val x0: Float, val y0: Float, val real: Boolean, var t: Long) {
        var lx = x0
        var ly = y0
        var moved = false
        var raw = 0f
        var vy = 0f
    }

    private enum class Leg {
        Lower,
        Spin,
        Wait,
        Shrink,
        Linger,
        GoHome,
        Hang,
        Unfold,
        Pause,
        Climb,
        Strand,
        Retreat,
    }

    /** Sizes the feed: [w] × [h] dp. */
    fun layout(w: Float, h: Float) {
        if (w == width && h == height) return
        width = w
        height = h
        k = (w / 360f).coerceIn(.9f, 1.15f) * 1.15f
        if (phase == SpiderPhase.Idle) spiderY = hiddenY
        frame++
    }

    /** The hanging card [i] measured [h] dp tall. */
    fun cardMeasured(i: Int, h: Float) {
        if (i in 0 until MaxBatch) cardHeights[i] = h
    }

    val maxScroll: Float
        get() = max(0f, listHeight - height)

    /** Whether a finger (or the script) holds the feed. */
    val held: Boolean
        get() = grab != null

    // ---------- Gesture (the demo drives the same calls) ----------

    /** A finger went down at ([x], [y]); [real] is a person's, not the script's. */
    fun down(x: Float, y: Float, real: Boolean, atMs: Long) {
        if (phase == SpiderPhase.Refresh) {
            grab = null
            return
        }
        fling = 0f
        val g = Grab(x, y, real, atMs)
        grab = g
        // Caught on its way home, it carries on from there.
        if (phase == SpiderPhase.Retreat) {
            leg = null
            phase = SpiderPhase.Pulling
            g.raw = unrubber(pull)
        }
        lookX = x
        lookY = y
        wake()
    }

    fun move(x: Float, y: Float, atMs: Long) {
        val g = grab ?: return
        // Past the slop the first delta still counts from where the finger went down.
        if (!g.moved && hypot(x - g.x0, y - g.y0) < Slop) return
        g.moved = true
        val dx = x - g.lx
        val dy = y - g.ly
        g.lx = x
        g.ly = y
        // The finger's speed, smoothed, for a fling when it lets go of the list.
        val gdt = max(8f, (atMs - g.t).toFloat())
        g.t = atMs
        g.vy = lerp(g.vy, dy / gdt * 1000f, .6f)
        lookX = x
        lookY = y
        if (g.raw > 0f || (scroll <= 0f && dy > 0f)) {
            val r = g.raw + dy
            if (r < 0f) {
                g.raw = 0f
                scroll = (scroll - r).coerceIn(0f, maxScroll)
            } else {
                g.raw = r
            }
            pull = rubber(g.raw)
            phase = if (pull > 0f) SpiderPhase.Pulling else SpiderPhase.Idle
            mood = SpiderMood.Peek
            vs -= dx.coerceIn(-60f, 60f) * .028f
            setArmed(pull >= Thresh, g.real)
        } else {
            scroll = (scroll - dy).coerceIn(0f, maxScroll)
        }
        wake()
        frame++
    }

    fun up(x: Float, y: Float, atMs: Long) {
        move(x, y, atMs)
        finish(cancel = false)
    }

    /** The touch was taken away: the spider just goes home, it never fetches. */
    fun cancel() = finish(cancel = true)

    private fun finish(cancel: Boolean) {
        val g = grab ?: return
        grab = null
        val had = g.raw > 0f || pull > 0f
        if (!had) {
            if (!cancel && abs(g.vy) > 120f) {
                fling = -g.vy
                wake()
            }
            return
        }
        if (pull >= Thresh && !cancel) startRefresh(button = false) else startRetreat()
    }

    /** Scrolls the list by [dy] dp, as a screen reader's scroll does; says whether it moved. */
    fun scrollBy(dy: Float): Boolean {
        if (phase == SpiderPhase.Refresh) return false
        val to = (scroll + dy).coerceIn(0f, maxScroll)
        if (to == scroll) return false
        fling = 0f
        scroll = to
        frame++
        return true
    }

    /** The app bar's refresh button: the same sequence, lowering the spider by itself. */
    fun refreshButton() {
        if (phase == SpiderPhase.Refresh) return
        grab = null
        startRefresh(button = true)
    }

    /** Back to the first five posts, nothing hanging, the spider home. */
    fun reset() {
        leg = null
        grab = null
        hanging.clear()
        posts.clear()
        posts.addAll(feed)
        fresh = emptySet()
        batchIdx = 0
        pull = 0f
        scroll = 0f
        fling = 0f
        phase = SpiderPhase.Idle
        mood = SpiderMood.Idle
        armed = false
        reeling = false
        yOver = Float.NaN
        swing = 0f
        vs = 0f
        pop = 0f
        popV = 0f
        bundle = 0f
        glints = 0
        spiderY = hiddenY
        toast = false
        toastUntil = 0f
        busy = false
        said = -1
        frame++
    }

    // ---------- Sequences ----------

    private fun startRetreat() {
        phase = SpiderPhase.Retreat
        mood = SpiderMood.Scurry
        emit(SpiderEvent.Patter)
        p0 = pull
        enter(Leg.Retreat, max(420f, p0 * 6f) * Soft)
    }

    private fun startRefresh(button: Boolean) {
        phase = SpiderPhase.Refresh
        busy = true
        fling = 0f
        fromButton = button
        p0 = pull
        s0 = scroll
        if (button) {
            mood = SpiderMood.Peek
            emit(SpiderEvent.Patter)
            // Scrolled down, the feed glides back to the top in the same move, so the new posts
            // arrive in view.
            enter(Leg.Lower, (if (s0 > 0f) 900f else 720f) * Soft)
        } else {
            enter(Leg.Lower, 360f * Soft)
        }
        wake()
    }

    private fun enter(next: Leg, ms: Float) {
        leg = next
        legT = 0f
        legMs = if (reducedMotion) 1f else ms
    }

    /** Runs the legs for [ms] of time, carrying what is left of one into the next. */
    private fun runLegs(ms: Float) {
        var left = ms
        while (true) {
            val l = leg ?: return
            if (l == Leg.Hang) {
                if (!hang()) return
                continue
            }
            val need = legMs - legT
            val used = min(left, need)
            legT += used
            left -= used
            update(l, (legT / legMs).coerceIn(0f, 1f))
            if (legT < legMs) return
            next(l)
        }
    }

    private fun update(l: Leg, p: Float) {
        when (l) {
            Leg.Lower -> {
                val e = ease(p)
                pull = lerp(p0, Hold, e)
                if (fromButton) scroll = s0 * (1 - e)
            }
            Leg.Spin -> bundle = ease(p)
            Leg.Shrink -> bundle = 1 - ease(p)
            Leg.GoHome -> pull = Hold * (1 - ease(p))
            Leg.Unfold -> {
                val e = ease(p)
                unfold = e
                reelL = lerp(Hold, t0 - Pad + total, e)
                bundle = 1 - e
            }
            Leg.Climb -> {
                val e = ease3(p)
                yOver = lerp(holdY, hiddenY - 6f, e)
                reelT = lerp(t0, Pad, e)
                reelL = reelT - Pad + total
            }
            Leg.Strand -> strand = 1 - ease(p)
            Leg.Retreat -> pull = p0 * (1 - ease(p))
            Leg.Wait,
            Leg.Linger,
            Leg.Pause,
            Leg.Hang -> Unit
        }
    }

    private fun next(l: Leg) {
        when (l) {
            Leg.Lower -> {
                if (fromButton) {
                    scroll = 0f
                    setArmed(true, false)
                }
                // It spins a little ball of silk while the posts load.
                mood = SpiderMood.Spin
                emit(SpiderEvent.Spin)
                enter(Leg.Spin, 560f * Soft)
            }
            Leg.Spin -> enter(Leg.Wait, 560f * Soft)
            Leg.Wait -> {
                fresh = emptySet()
                val batch = batches.getOrNull(batchIdx)
                if (batch == null) {
                    mood = SpiderMood.Empty
                    emit(SpiderEvent.Empty)
                    toast = true
                    toastUntil = time + ToastMs
                    said = 0
                    enter(Leg.Shrink, 420f * Soft)
                } else {
                    batchIdx++
                    cardHeights.fill(0f, 0, MaxBatch)
                    hanging.addAll(batch)
                    leg = Leg.Hang
                }
            }
            Leg.Shrink -> enter(Leg.Linger, 380f * Soft)
            Leg.Linger -> {
                mood = SpiderMood.Scurry
                emit(SpiderEvent.Patter)
                enter(Leg.GoHome, 720f * Soft)
            }
            Leg.GoHome -> home()
            Leg.Unfold -> enter(Leg.Pause, 140f * Soft)
            Leg.Pause -> {
                // It climbs its thread and reels everything up, the old posts moving down with the
                // new ones so nothing overlaps.
                mood = SpiderMood.Climb
                emit(SpiderEvent.Climb)
                enter(Leg.Climb, 1500f * Soft)
            }
            Leg.Climb -> {
                emit(SpiderEvent.Settle)
                enter(Leg.Strand, 460f * Soft)
            }
            Leg.Strand -> {
                val n = hanging.size
                posts.addAll(0, hanging)
                hanging.clear()
                reeling = false
                said = n
                home()
            }
            Leg.Retreat -> {
                leg = null
                pull = 0f
                phase = SpiderPhase.Idle
                mood = SpiderMood.Idle
                armed = false
            }
            Leg.Hang -> Unit
        }
    }

    /** Hangs the batch once its cards are measured: the ball opens into them. */
    private fun hang(): Boolean {
        val n = hanging.size
        for (i in 0 until n) if (cardHeights[i] <= 0f) return false
        var off = 0f
        for (i in 0 until n) {
            cardOffsets[i] = off
            off += cardHeights[i] + Gap
        }
        total = off
        holdY = spiderY
        t0 = holdY + 36f * k
        reelT = t0
        reelL = pull
        unfold = 0f
        strand = 1f
        reeling = true
        fresh = hanging.toSet()
        mood = SpiderMood.Reel
        emit(SpiderEvent.Catch)
        enter(Leg.Unfold, 620f * Soft)
        return true
    }

    private fun home() {
        leg = null
        pull = 0f
        yOver = Float.NaN
        phase = SpiderPhase.Idle
        mood = SpiderMood.Idle
        armed = false
        busy = false
    }

    private fun setArmed(on: Boolean, real: Boolean) {
        if (on == armed) return
        armed = on
        if (on) {
            popV += 7f
            emit(if (real) SpiderEvent.ArmByFinger else SpiderEvent.Arm)
        } else {
            emit(SpiderEvent.Disarm)
        }
    }

    private fun emit(e: SpiderEvent) {
        onEvent?.invoke(e)
    }

    private fun wake() {
        if (!awake) awake = true
    }

    // ---------- Physics ----------

    /** Moves everything on by [dt] seconds. */
    fun step(dt: Float) {
        acc += dt
        while (acc >= Step) {
            substep(Step)
            time += Step * 1000f
            acc -= Step
        }
        runLegs(dt * 1000f)
        spiderY = if (yOver.isNaN()) spiderYFor(pull) else yOver
        if (toast && time > toastUntil) toast = false
        frame++
        val still =
            phase == SpiderPhase.Idle &&
                grab == null &&
                fling == 0f &&
                !toast &&
                glints == 0 &&
                pull == 0f &&
                abs(swing) < 1e-4f &&
                abs(vs) < 1e-3f &&
                abs(pop) < 1e-4f &&
                abs(popV) < 1e-3f
        if (still) {
            swing = 0f
            vs = 0f
            pop = 0f
            popV = 0f
            awake = false
        }
    }

    private fun substep(dt: Float) {
        // The dangle: a damped pendulum, pushed by the finger's sideways moves.
        vs += (-46f * swing - 4.2f * vs) * dt
        swing = (swing + vs * dt).coerceIn(-.5f, .5f)
        popV += (-150f * pop - 13f * popV) * dt
        pop += popV * dt
        // Hanging in the gap it follows the pull exactly; climbing, it follows its own path.
        spiderY = if (yOver.isNaN()) spiderYFor(pull) else yOver
        val ls =
            when (mood) {
                SpiderMood.Idle -> 1f
                SpiderMood.Peek -> 2.2f
                SpiderMood.Scurry -> 16f
                SpiderMood.Spin -> 9f
                SpiderMood.Reel -> 5f
                SpiderMood.Climb -> 15f
                SpiderMood.Empty -> 1.4f
            }
        legSpeed = lerp(legSpeed, ls, 1 - exp(-dt * 8f))
        legPhase += legSpeed * dt
        ballTurn += (if (mood == SpiderMood.Spin) 7f else 1.5f) * dt
        if (time > blinkAt) {
            blink = 1f
            blinkAt = time + 2200f + random.nextFloat() * 2600f
        }
        blink = max(0f, blink - dt * 7f)
        // Glints that run up the thread while it climbs.
        if (mood == SpiderMood.Climb && glints < MaxGlints && random.nextFloat() < dt * 9f) {
            glintY[glints] = spiderY + 24f * k + random.nextFloat() * 30f
            glintA[glints] = 1f
            glints++
        }
        var n = 0
        for (i in 0 until glints) {
            val y = glintY[i] - 160f * dt
            val a = glintA[i] - dt * 1.6f
            if (a > 0f && y > -4f) {
                glintY[n] = y
                glintA[n] = a
                n++
            }
        }
        glints = n
        // A flung list coasts and slows, and stops dead at either end.
        if (fling != 0f) {
            scroll = (scroll + fling * dt).coerceIn(0f, maxScroll)
            fling *= exp(-dt * 3.2f)
            if (abs(fling) < 12f || scroll <= 0f || scroll >= maxScroll) fling = 0f
        }
    }

    /**
     * Where the spider hangs for a gap [p]: tucked away at 0, in the middle of the gap at the hold.
     */
    private fun spiderYFor(p: Float) = lerp(hiddenY, Hold * .5f, p / Hold)

    internal companion object {
        const val Pad = 10f
        const val Gap = 10f
        const val Thresh = 80f
        const val Hold = 80f
        const val Rub = 220f
        const val Slop = 6f
        const val Step = 1f / 120f
        const val MaxBatch = 4
        const val MaxGlints = 48

        /** The native builds' motion is this much slower than the prototype's. */
        const val Soft = 1.3f

        /** How long "You're all caught up" shows: the prototype's 2.2 s, softened. */
        const val ToastMs = 2200f * Soft

        /** The feed's first five posts and the four batches that arrive, by id. */
        val DefaultFeed = listOf(0, 1, 2, 3, 4)
        val DefaultBatches = listOf(listOf(5, 6), listOf(7), listOf(8, 9), listOf(10))

        fun rubber(raw: Float) = Rub * (1 - 1 / (1 + raw * .8f / Rub))

        fun unrubber(p: Float) = Rub * (p / max(1f, Rub - p)) / .8f

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        // Soft in-and-out curves only: nothing leaps off at full speed.
        fun ease(p: Float): Float = -(cos(PI.toFloat() * p.coerceIn(0f, 1f)) - 1) / 2

        fun ease3(p: Float): Float = if (p < .5f) 4 * p * p * p else 1 - (-2 * p + 2).pow(3) / 2
    }
}
