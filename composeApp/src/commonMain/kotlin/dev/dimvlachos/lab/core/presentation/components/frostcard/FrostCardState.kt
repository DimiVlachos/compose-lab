package dev.dimvlachos.lab.core.presentation.components.frostcard

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** What the card does that is heard, felt or said. */
internal enum class FrostEvent {
    /** The switch (or a hold) set it freezing. */
    Freeze,

    /** The frost has covered the card: it is frozen. */
    Frozen,

    /** The last of the ice is gone: it is unfrozen. */
    Unfrozen,

    /** A tiny dry tick as the ice grows. */
    Crackle,

    /** A bead falls off the card's bottom edge. */
    Drip,
}

/**
 * The prototype's card, as numbers: [frozen] is what the switch says; [f] how far the frost has
 * grown (0 to 1); [r] the thawed hole's radius round ([cx], [cy]). Lengths are in dp, in the card's
 * own space (its top left is 0, 0). [step] moves it on, in the prototype's 1/120 s substeps, and
 * calls [onEvent] for what is heard and said; [hiss] is how loud the melting is (0 to 1).
 *
 * A press on the card ([pressStart], [pressMove], [pressEnd]) freezes a warm card after 0.45 s and
 * thaws a frozen one for as long as it rests there: the warm circle grows faster the longer it is
 * held and follows the finger. Let go early and the frost closes back in. Water beads run off the
 * edge of the hole, slide, merge and drip off the bottom.
 */
@Stable
internal class FrostCardState(private val random: Random = Random(7)) {
    var frozen by mutableStateOf(false)
        private set

    /** Bumped by every step that changes something drawn, for the canvas to read. */
    var frame by mutableIntStateOf(0)
        private set

    var reducedMotion = false

    var f = 0f
        private set

    var r = 0f
        private set

    var cx = 0f
        private set

    var cy = 0f
        private set

    var auto = false
        private set

    var hiss = 0f
        private set

    /** Seconds of simulated time, for the glints. */
    var time = 0f
        private set

    var onEvent: ((FrostEvent, Float) -> Unit)? = null

    var cw = 0f
        private set

    var ch = 0f
        private set

    // How far below the card's top a falling bead may go before it is gone (the screen's foot).
    private var fallLimit = 0f

    var growth: FrostGrowth? = null
        private set

    private var holdT = 0f
    private var said = SaidUnfrozen
    private var spawnAcc = 0f
    private var crackT = 0f
    private var hissLevel = 0f

    // The press: none, waiting to freeze, thawing, or spent.
    private var press = PressNone
    private var pressT = 0f
    private var px = 0f
    private var py = 0f

    // Whether the press is the script's: a real finger takes over from it, and the script can't
    // move or end a real one.
    private var pressScripted = false

    /** Whether something is still moving, so the loop needn't sleep. */
    val busy: Boolean
        get() =
            (frozen && f < 1f) ||
                press != PressNone ||
                (f > 0f && (auto || r > 0f)) ||
                drops > 0 ||
                streaks > 0 ||
                hissLevel > 1e-4f ||
                (f > 0f && growth?.sparks != 0)

    // ---- Water: beads and the wet streaks they leave, in fixed arrays ----
    var drops = 0
        private set

    val dx = FloatArray(MaxDrops)
    val dy = FloatArray(MaxDrops)
    val dr = FloatArray(MaxDrops)
    val dvy = FloatArray(MaxDrops)
    val dt = FloatArray(MaxDrops)
    val da = FloatArray(MaxDrops)
    val dht = FloatArray(MaxDrops)
    val dfa = FloatArray(MaxDrops)
    val dev = FloatArray(MaxDrops)
    val dseed = FloatArray(MaxDrops)
    val dly = FloatArray(MaxDrops)
    val dhang = BooleanArray(MaxDrops)
    val dfall = BooleanArray(MaxDrops)

    var streaks = 0
        private set

    val sx1 = FloatArray(MaxStreaks)
    val sy1 = FloatArray(MaxStreaks)
    val sx2 = FloatArray(MaxStreaks)
    val sy2 = FloatArray(MaxStreaks)
    val sw = FloatArray(MaxStreaks)
    val slife = FloatArray(MaxStreaks)

    /**
     * The card is [w] by [h] dp, and the screen's foot is [below] dp under its top. A new size
     * grows the frost again.
     */
    fun layout(w: Float, h: Float, below: Float) {
        floor(below)
        if (w <= 0f || h <= 0f) return
        if (growth == null || cw != w || ch != h) {
            cw = w
            ch = h
            growth = FrostGrowth(w, h)
            frame++
        }
    }

    /** The screen's foot is [below] dp under the card's top: falling beads go as they pass it. */
    fun floor(below: Float) {
        fallLimit = below + 20f
    }

    /**
     * Frozen over and still: nothing moves but the glints, which the loop can draw at a calmer
     * rate.
     */
    val resting: Boolean
        get() =
            frozen &&
                f >= 1f &&
                press == PressNone &&
                !auto &&
                r == 0f &&
                drops == 0 &&
                streaks == 0 &&
                hissLevel < 1e-4f

    /** Flips the switch. */
    fun toggle() = freeze(!frozen)

    fun freeze(v: Boolean) {
        frozen = v
        if (v) {
            auto = false
            if (reducedMotion) {
                f = 1f
                r = 0f
            }
            onEvent?.invoke(FrostEvent.Freeze, 1f)
            // Frozen at once with reduced motion, so said at once: the prototype only said it when
            // the growing front reached the end, which never happens then.
            if (f >= 1f) settle()
        } else if (f > 0f) {
            // Switched off: thaw it from the middle by itself.
            if (r < 6f) {
                cx = cw / 2
                cy = ch * .55f
                r = 0f
            }
            auto = true
            holdT = max(holdT, .4f)
            if (reducedMotion) thawed()
        } else thawed()
        frame++
    }

    private fun thawed() {
        frozen = false
        auto = false
        f = 0f
        r = 0f
        holdT = 0f
        if (press == PressThaw) press = PressDone
        for (i in 0 until drops) dt[i] = max(dt[i], .25f)
        settle()
    }

    private fun settle() {
        val now =
            if (frozen) (if (f >= 1f) SaidFrozen else 0) else (if (f == 0f) SaidUnfrozen else 0)
        if (now != 0 && now != said) {
            said = now
            onEvent?.invoke(if (now == SaidFrozen) FrostEvent.Frozen else FrostEvent.Unfrozen, 1f)
        }
    }

    /** A finger down at ([x], [y]) in the card's dp; [scripted] when it is the demo's. */
    fun pressStart(x: Float, y: Float, scripted: Boolean = false) {
        pressScripted = scripted
        px = x
        py = y
        pressT = 0f
        press =
            when {
                f > 0f && (frozen || auto) -> PressThaw
                !frozen -> PressPending
                else -> PressDone
            }
        if (press == PressThaw && r < 8f) {
            cx = x.coerceIn(0f, cw)
            cy = y.coerceIn(0f, ch)
            holdT = 0f
        }
        frame++
    }

    fun pressMove(x: Float, y: Float, scripted: Boolean = false) {
        if (press == PressNone || scripted != pressScripted) return
        px = x
        py = y
    }

    fun pressEnd(scripted: Boolean = false) {
        if (scripted != pressScripted) return
        press = PressNone
        frame++
    }

    val pressing: Boolean
        get() = press != PressNone

    /** Moves everything on by [seconds], in substeps of at most 1/120 s. */
    fun step(seconds: Float) {
        var left = seconds
        while (left > 1e-6f) {
            val h = min(left, 1f / 120f)
            substep(h)
            left -= h
        }
        frame++
    }

    private fun rMax(): Float =
        maxOf(hypot(cx, cy), hypot(cw - cx, cy), hypot(cx, ch - cy), hypot(cw - cx, ch - cy)) *
            1.12f + 16f

    /** The hole's wobbly edge: its radius at angle [a], over [r]. */
    fun holeR(a: Float): Float =
        1 + .05f * sin(5 * a + 1.3f) + .035f * sin(9 * a + .4f) + .02f * sin(13 * a + 2.1f)

    /** Whether ([x], [y]) is under ice: the frost has reached it and it is outside the hole. */
    fun iced(x: Float, y: Float): Boolean {
        val g = growth ?: return false
        return f > 0f &&
            hypot(x - cx, y - cy) > r * holeR(atan2(y - cy, x - cx)) - 3 &&
            g.arrivedAt(x, y, f)
    }

    private fun substep(h: Float) {
        time += h
        if (frozen && f < 1f) {
            f = min(1f, f + h / 2.8f)
            crackle(h)
            if (f >= 1f) settle()
        }
        if (press == PressPending) {
            pressT += h
            if (pressT > .45f) {
                press = PressDone
                freeze(true)
            }
        }
        val thawing = f > 0f && (press == PressThaw || auto)
        if (thawing) {
            holdT += h
            if (press == PressThaw) {
                val k = 1 - exp(-h * 7)
                cx += (px.coerceIn(0f, cw) - cx) * k
                cy += (py.coerceIn(0f, ch) - cy) * k
            }
            if (reducedMotion) {
                if (holdT > .4f) r = rMax()
            } else {
                val dR = (40 + 250 * smooth(0f, 1.2f, holdT)) * h
                r += dR
                spawnDrops(dR)
            }
            if (r >= rMax()) thawed()
        } else {
            holdT = 0f
            if (r > 0f && f > 0f) r = max(0f, r - (if (reducedMotion) 999f else 46f) * h)
        }
        hissLevel += ((if (thawing) .05f else 0f) - hissLevel) * .06f
        hiss = hissLevel / .05f
        stepWater(h)
    }

    // Ice forming: tiny dry ticks, every 20 to 80 ms while the front is moving.
    private fun crackle(h: Float) {
        crackT -= h
        if (crackT > 0f) return
        crackT = .02f + random.nextFloat() * .06f
        onEvent?.invoke(FrostEvent.Crackle, .012f + random.nextFloat() * .03f)
    }

    private fun spawnDrops(dR: Float) {
        if (r < 8f || reducedMotion) return
        spawnAcc += dR * .3f
        while (spawnAcc >= 1f) {
            spawnAcc -= 1f
            if (drops > 150) continue
            val a = random.nextFloat() * TwoPi
            val rr = r * holeR(a) - 4 - random.nextFloat() * min(12f, r * .3f)
            val x = cx + cos(a) * rr
            val y = cy + sin(a) * rr
            if (x < 4 || y < 4 || x > cw - 4 || y > ch - 4) continue
            val i = drops++
            dx[i] = x
            dy[i] = y
            dr[i] = 1.1f + random.nextFloat() * random.nextFloat() * 3.3f
            dvy[i] = 0f
            dt[i] = 0f
            da[i] = 0f
            dhang[i] = false
            dht[i] = 0f
            dfall[i] = false
            dfa[i] = 0f
            dev[i] = 0f
            dseed[i] = random.nextFloat() * 6
            dly[i] = y
        }
    }

    private fun removeDrop(i: Int) {
        for (j in i until drops - 1) {
            dx[j] = dx[j + 1]
            dy[j] = dy[j + 1]
            dr[j] = dr[j + 1]
            dvy[j] = dvy[j + 1]
            dt[j] = dt[j + 1]
            da[j] = da[j + 1]
            dhang[j] = dhang[j + 1]
            dht[j] = dht[j + 1]
            dfall[j] = dfall[j + 1]
            dfa[j] = dfa[j + 1]
            dev[j] = dev[j + 1]
            dseed[j] = dseed[j + 1]
            dly[j] = dly[j + 1]
        }
        drops--
    }

    private fun addStreak(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) {
        if (streaks == MaxStreaks) removeStreak(0)
        val i = streaks++
        sx1[i] = x1
        sy1[i] = y1
        sx2[i] = x2
        sy2[i] = y2
        sw[i] = w
        slife[i] = 1.6f
    }

    private fun removeStreak(i: Int) {
        for (j in i until streaks - 1) {
            sx1[j] = sx1[j + 1]
            sy1[j] = sy1[j + 1]
            sx2[j] = sx2[j + 1]
            sy2[j] = sy2[j + 1]
            sw[j] = sw[j + 1]
            slife[j] = slife[j + 1]
        }
        streaks--
    }

    private fun stepWater(h: Float) {
        var i = drops - 1
        while (i >= 0) {
            da[i] = min(1f, da[i] + h * 4)
            dt[i] += h
            if (dfall[i]) {
                dvy[i] += 1400 * h
                dy[i] += dvy[i] * h
                dfa[i] -= h * 2.4f
                da[i] = max(0f, dfa[i])
                if (dfa[i] <= 0f || dy[i] > fallLimit) removeDrop(i)
                i--
                continue
            }
            if (dhang[i]) {
                dht[i] += h
                dy[i] = ch - dr[i] * .4f + dht[i] * 6
                if (dht[i] > .38f) {
                    dfall[i] = true
                    dfa[i] = 1f
                    dvy[i] = 40f
                    onEvent?.invoke(FrostEvent.Drip, dr[i] / 4)
                }
                i--
                continue
            }
            val heavy = dr[i] > 2.9f && dt[i] > .25f
            if (heavy) {
                val term = 30 + dr[i] * 18
                val ny = dy[i] + max(1f, dvy[i]) * h
                if (iced(dx[i], ny + dr[i])) {
                    dvy[i] = 0f
                } else {
                    dvy[i] += (term - dvy[i]) * min(1f, h * 3)
                    dy[i] = ny
                    dx[i] += sin(dt[i] * 2.3f + dseed[i]) * .12f
                    if (dy[i] - dly[i] > 3) {
                        addStreak(dx[i], dly[i], dx[i], dy[i], dr[i] * 1.5f)
                        dly[i] = dy[i]
                    }
                    // Gather the beads it runs into.
                    var k = drops - 1
                    while (k >= 0) {
                        if (k != i && !dfall[k] && !dhang[k]) {
                            if (
                                kotlin.math.abs(dy[k] - dy[i]) < dr[i] + dr[k] &&
                                    kotlin.math.abs(dx[k] - dx[i]) < (dr[i] + dr[k]) * .85f
                            ) {
                                dr[i] = min(6f, cbrt(dr[i] * dr[i] * dr[i] + dr[k] * dr[k] * dr[k]))
                                removeDrop(k)
                                if (k < i) i--
                            }
                        }
                        k--
                    }
                    if (dy[i] + dr[i] >= ch - 1) {
                        dhang[i] = true
                        dht[i] = 0f
                        dvy[i] = 0f
                    }
                }
            } else if (f == 0f) {
                // Small beads on a warm card dry off.
                dev[i] += h
                if (dev[i] > 4) {
                    da[i] = max(0f, 1 - (dev[i] - 4) / 2.5f)
                    if (dev[i] > 6.5f) {
                        removeDrop(i)
                        i--
                        continue
                    }
                }
            }
            i--
        }
        var s = streaks - 1
        while (s >= 0) {
            slife[s] -= h * .7f
            if (slife[s] <= 0f) removeStreak(s)
            s--
        }
    }

    /** Back to a warm, dry card, as it began. */
    fun reset() {
        frozen = false
        f = 0f
        r = 0f
        auto = false
        holdT = 0f
        said = SaidUnfrozen
        spawnAcc = 0f
        press = PressNone
        drops = 0
        streaks = 0
        hissLevel = 0f
        hiss = 0f
        frame++
    }

    internal companion object {
        const val MaxDrops = 160
        const val MaxStreaks = 400
        private const val TwoPi = (2 * PI).toFloat()
        private const val SaidFrozen = 1
        private const val SaidUnfrozen = 2
        private const val PressNone = 0
        private const val PressPending = 1
        private const val PressThaw = 2
        private const val PressDone = 3

        /** The prototype's ease in and out. */
        fun ease(t: Float): Float = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).let { it * it } / 2
    }
}
