package dev.dimvlachos.lab.core.presentation.components.paperbag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/**
 * The paper bag's motion, stepped as the prototype steps it: a landing squash and a wobble on two
 * quick springs, a bulge that follows how full it is, the grease-pencil number being written in,
 * and how far it is tipped open. Its clock, [now], runs only as [tick] is called, so a slow-motion
 * or test caller controls time; [frame] changes whenever something moved, for drawing to follow.
 *
 * Nothing here is read in composition: [pose] is for drawing.
 */
@Stable
internal class PaperBagState(private val random: Random = Random.Default) {
    /** Milliseconds of the bag's own clock. */
    var now = 0.0
        private set

    /** Bumped every tick in which anything moved: drawing reads it to redraw. */
    var frame by mutableLongStateOf(0L)
        private set

    /** Bumped by everything that sets the bag moving, so a clock at rest knows to start again. */
    var wakes by mutableIntStateOf(0)
        private set

    /** Whether motion is cut short: springs rest, the bag opens and closes at once. */
    var reducedMotion = false

    /** Whether the bag is open, or opening: what [open] eases toward. */
    var isOpen by mutableStateOf(false)
        private set

    /** How far open the bag is, from 0 in the corner to 1 tipped open in the sheet. */
    var open = 0f
        private set

    private var sq = 0f
    private var sqV = 0f
    private var wob = 0f
    private var wobV = 0f
    private var bulge = 0f
    private var numP = 1f
    private var prevCount = 0
    private var count = 0
    private val lifts = HashMap<Int, Double>()

    /** Something dropped in: the bag squashes and tips, and [count] is written over the old. */
    fun landed(count: Int) {
        wakes++
        rewrite(count)
        if (reducedMotion) return
        sqV += 7.5f
        wobV += (if (random.nextBoolean()) -1 else 1) * 4f
    }

    /** The entry [key] starts rising out; it is gone after [LiftMs]. */
    fun lift(key: Int) {
        wakes++
        lifts[key] = now
    }

    /** What rose out has gone: the bag settles a little and [count] is written over the old. */
    fun lifted(key: Int, count: Int) {
        wakes++
        lifts.remove(key)
        rewrite(count)
        if (!reducedMotion) sqV -= 2f
    }

    /** Nothing more fits: the bag shakes its head. */
    fun refuse() {
        wakes++
        if (!reducedMotion) wobV += 3f
    }

    /** Tips the bag open, or sets it back in the corner. */
    fun openTo(open: Boolean) {
        wakes++
        isOpen = open
        if (reducedMotion) this.open = if (open) 1f else 0f
    }

    /** Puts the bag back as it starts: empty, closed and still. */
    fun reset() {
        wakes++
        sq = 0f
        sqV = 0f
        wob = 0f
        wobV = 0f
        bulge = 0f
        numP = 1f
        prevCount = 0
        count = 0
        lifts.clear()
        isOpen = false
        open = 0f
        frame++
    }

    private fun rewrite(count: Int) {
        prevCount = this.count
        this.count = count
        numP = if (reducedMotion) 1f else 0f
    }

    /** How far the entry [key] has risen out of the bag, eased, up to 1.3. */
    fun rise(key: Int): Float {
        val t0 = lifts[key] ?: return 0f
        return ease(((now - t0) / LiftMs).toFloat().coerceIn(0f, 1f)) * 1.3f
    }

    /** Runs the bag's clock on by [dt] seconds, in the prototype's 1/240 s steps. */
    fun tick(dt: Float) {
        var acc = dt
        while (acc > 0f) {
            val d = minOf(Step, acc)
            step(d)
            now += d * 1000.0
            acc -= d
        }
        frame++
    }

    private fun step(dt: Float) {
        // Squash spring (landing) and wobble spring (tilt), both settle quickly.
        sqV += (-220f * sq - 13f * sqV) * dt
        sq += sqV * dt
        wobV += (-160f * wob - 9f * wobV) * dt
        wob += wobV * dt
        val want = ((count - 1) / 6f).coerceIn(0f, 1f)
        bulge += (want - bulge) * minOf(1f, dt * 6f)
        numP = minOf(1f, numP + dt / .28f)
        val target = if (isOpen) 1f else 0f
        val k = if (isOpen) 7f else 8f
        open += (target - open) * minOf(1f, dt * k)
        if (kotlin.math.abs(target - open) < .002f) open = target
        if (reducedMotion) {
            sq = 0f
            sqV = 0f
            wob = 0f
            wobV = 0f
            bulge = want
            open = target
        }
    }

    /** The bag as drawn now, holding [items]. */
    fun pose(items: List<BagItem>): BagPose =
        BagPose(
            bulge = bulge,
            tip = ease(open),
            sq = sq,
            wob = wob,
            items = items,
            count = count,
            prevCount = prevCount,
            numP = numP,
        )

    /** Whether anything is still moving, so the clock can rest. */
    val moving: Boolean
        get() {
            val want = ((count - 1) / 6f).coerceIn(0f, 1f)
            return kotlin.math.abs(sq) > 1e-4f ||
                kotlin.math.abs(sqV) > 1e-3f ||
                kotlin.math.abs(wob) > 1e-4f ||
                kotlin.math.abs(wobV) > 1e-3f ||
                kotlin.math.abs(bulge - want) > 1e-3f ||
                numP < 1f ||
                open != (if (isOpen) 1f else 0f) ||
                lifts.isNotEmpty()
        }

    internal companion object {
        /** How long a lift out of the bag takes, in milliseconds. */
        const val LiftMs = 620.0

        private const val Step = 1f / 240f
    }
}

/** The prototype's ease in and out. */
internal fun ease(p: Float): Float =
    if (p < .5f) 2 * p * p else 1 - ((-2 * p + 2) * (-2 * p + 2)) / 2
