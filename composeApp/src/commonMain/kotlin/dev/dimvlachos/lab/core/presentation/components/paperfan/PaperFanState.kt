package dev.dimvlachos.lab.core.presentation.components.paperfan

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/** Something a fan does that is heard or felt. */
internal enum class PaperFanEvent {
    /** The fan lifts out of its row and flicks open. */
    Fwap,
    /** The leading guard clicks home, fully open. */
    Click,
    /** An open fan starts to fold. */
    Swish,
    /** A folding fan's guards clack together. */
    Clack,
}

/**
 * The paper fans' springs, ported from the prototype's `items` and `step()`. For each question:
 * whether it is [isOpen], how far its fan has travelled out of the row ([travel], 0 in the row to 1
 * open over the answer) and each rib's spread ([spread], 0 shut to 1 open; rib [Ribs] is the
 * leading guard).
 *
 * Only one answer is out at a time. A closing fan folds in place before it travels home, and a new
 * fan waits for that, so the answer areas trade height ([heightFraction]) and never hold more than
 * one open panel between them.
 */
@Stable
internal class PaperFanState(val count: Int) {
    var reducedMotion = false

    /** Hears what fan `index` does. */
    var onEvent: ((PaperFanEvent, Int) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something sets the springs moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    private val openFlags = mutableStateListOf<Boolean>().apply { repeat(count) { add(false) } }

    private val items = Array(count) { Item() }

    private class Item {
        var tr = 0f
        var trV = 0f
        val s = FloatArray(Ribs + 1) { Rest }
        val sv = FloatArray(Ribs + 1)
        var dragging = false
        var dragF = 1f
        var clicked = false
        var clacked = true
        var fwapped = true
    }

    private val folding = BooleanArray(count)

    /** Whether question [i]'s answer is out (or on its way out). Read as state. */
    fun isOpen(i: Int): Boolean = openFlags[i]

    /** How far fan [i] has travelled from its row to its open place, 0 to 1 (springs overshoot). */
    fun travel(i: Int): Float = items[i].tr

    /** Rib [rib]'s spread on fan [i]: 0 shut, 1 open. */
    fun spread(i: Int, rib: Int): Float = items[i].s[rib]

    /** Whether a finger holds fan [i]. */
    fun dragging(i: Int): Boolean = items[i].dragging

    /**
     * How much of a full answer area question [i] shows: `1.12 tr − .06`, so a closing and an
     * opening panel on mirrored springs always sum to at most one open panel.
     */
    fun heightFraction(i: Int): Float = (items[i].tr * 1.12f - .06f).coerceIn(0f, 1f)

    /** Taps question [i]: its fan opens, and any other open one folds away; or, open, it folds. */
    fun toggle(i: Int) {
        val on = !openFlags[i]
        for (o in 0 until count) if (o != i && openFlags[o]) setOpen(o, false)
        setOpen(i, on)
    }

    /** Folds every fan shut and back into its row at once. */
    fun reset() {
        for (it in items) {
            it.tr = 0f
            it.trV = 0f
            it.s.fill(Rest)
            it.sv.fill(0f)
            it.dragging = false
            it.clicked = false
            it.clacked = true
            it.fwapped = true
        }
        for (i in 0 until count) openFlags[i] = false
        wake++
        frame++
    }

    private fun setOpen(i: Int, on: Boolean) {
        if (openFlags[i] == on) return
        openFlags[i] = on
        val it = items[i]
        wake++
        // The fwap plays when the fan actually lifts, which can be a beat later if another fan is
        // still folding.
        if (on) {
            it.clicked = false
            it.fwapped = false
        } else {
            it.clacked = false
            emit(PaperFanEvent.Swish, i)
        }
        if (reducedMotion) {
            it.tr = if (on) 1f else 0f
            it.s.fill(if (on) 1f else Rest)
            frame++
        }
    }

    /**
     * A finger comes down on open fan [i]: it takes the leading guard where it is. False when the
     * fan isn't open far enough to be held.
     */
    fun dragStart(i: Int): Boolean {
        val it = items[i]
        if (!openFlags[i] || it.tr < .9f) return false
        it.dragging = true
        it.dragF = it.s[Ribs].coerceIn(0f, 1f)
        wake++
        return true
    }

    /** The finger holding fan [i] has the leading guard at [fraction] of open (see [dragFraction]). */
    fun dragTo(i: Int, fraction: Float) {
        val it = items[i]
        if (!it.dragging) return
        it.dragF = fraction.coerceIn(0f, 1f)
        wake++
    }

    /**
     * The finger lifts from fan [i]. If it [moved] the fan under half open, the fan folds shut;
     * otherwise it springs back open.
     */
    fun dragEnd(i: Int, moved: Boolean) {
        val it = items[i]
        if (!it.dragging) return
        it.dragging = false
        wake++
        if (moved && it.dragF < .5f) {
            it.clacked = false
            openFlags[i] = false
            emit(PaperFanEvent.Swish, i)
        } else if (moved) {
            it.clicked = it.dragF > .96f
        }
    }

    /**
     * Whether every fan has come to rest where it is aimed and no finger holds one: nothing would
     * move on the next frame, so the frame loop can stop until [wake] changes.
     */
    val settled: Boolean
        get() {
            for (i in 0 until count) {
                val it = items[i]
                if (it.dragging) return false
                val open = openFlags[i]
                if (abs(it.trV) > RestSpeed || abs(it.tr - (if (open) 1f else 0f)) > RestGap) {
                    return false
                }
                val aim = if (open) 1f else Rest
                for (r in 0..Ribs) {
                    if (abs(it.sv[r]) > RestSpeed || abs(it.s[r] - aim) > RestGap) return false
                }
            }
            return true
        }

    /** Advances [seconds] in small fixed substeps, which keep the stiff rib springs stable. */
    fun step(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            val h = min(left, Substep)
            substep(h)
            left -= h
        }
        frame++
    }

    private fun substep(dt: Float) {
        // A closing fan folds in place before it travels home; a new fan waits for that, then both
        // travel on the same spring, so the answer areas trade height.
        var waitFold = false
        for (i in 0 until count) {
            val it = items[i]
            folding[i] = !openFlags[i] && !it.dragging && it.s.max() >= .3f
            if (folding[i]) waitFold = true
        }
        for (i in 0 until count) {
            val it = items[i]
            val open = openFlags[i]
            val spreadT: Float
            val trT: Float
            if (it.dragging) {
                spreadT = it.dragF
                trT = 1f
            } else if (open) {
                trT = if (waitFold && it.tr < .02f) 0f else 1f
                spreadT = if (it.tr > .55f) 1f else 0f
                if (trT == 1f && !it.fwapped) {
                    it.fwapped = true
                    emit(PaperFanEvent.Fwap, i)
                }
            } else {
                spreadT = Rest
                trT = if (folding[i]) 1f else 0f
            }
            if (reducedMotion && !it.dragging) {
                it.tr = trT
                it.trV = 0f
                it.s.fill(spreadT)
                it.sv.fill(0f)
                continue
            }
            spring(it.tr, it.trV, trT, 140f, .86f, dt)
            it.tr = outX
            it.trV = outV
            for (r in 0..Ribs) {
                // While dragging, the leading guard is held by the finger; the inner ribs are
                // pulled along by the paper.
                if (it.dragging && r == Ribs) {
                    it.s[r] = it.dragF
                    it.sv[r] = 0f
                    continue
                }
                // The leading guard is flicked; the inner ribs are pulled along by the paper, so
                // they're softer and flutter.
                val f = r.toFloat() / Ribs
                val k = if (it.dragging) 520f else 150f + 330f * f
                val z =
                    if (it.dragging) .95f
                    else if (open) .4f + .2f * f
                    else .75f
                spring(it.s[r], it.sv[r], spreadT, k, z, dt)
                it.s[r] = outX
                it.sv[r] = outV
            }
            if (open && !it.clicked && it.s[Ribs] > .96f) {
                it.clicked = true
                emit(PaperFanEvent.Click, i)
            }
            if (!open && !it.clacked && it.s[Ribs] < Rest + .03f) {
                it.clacked = true
                emit(PaperFanEvent.Clack, i)
            }
        }
    }

    // The spring's result, kept in fields so a substep allocates nothing.
    private var outX = 0f
    private var outV = 0f

    /** One semi-implicit Euler step of a damped spring: stiffness [k], damping ratio [z]. */
    private fun spring(x: Float, v: Float, target: Float, k: Float, z: Float, dt: Float) {
        val c = 2f * sqrt(k) * z
        val nv = v + (k * (target - x) - c * v) * dt
        outV = nv
        outX = x + nv * dt
    }

    private fun emit(event: PaperFanEvent, i: Int) {
        onEvent?.invoke(event, i)
    }

    internal companion object {
        /** Ribs between the guards, guards included, less one: rib 0 is the back guard. */
        const val Ribs = 10

        /** A folded fan rests a hair ajar, so its pleats show as a slim wedge in the row. */
        const val Rest = .045f

        private const val Substep = 1f / 240f
        private const val RestSpeed = .02f
        private const val RestGap = .005f

        /**
         * Where a finger at ([dx], [dy]) from the fan's rivet holds the leading guard: 1 straight
         * out to the right, 0 round to the left, as the prototype's `dragAngle()`.
         */
        fun dragFraction(dx: Float, dy: Float): Float {
            val a = atan2(dy, dx)
            if (a > 0f) return if (dx > 0f) 1f else 0f
            return ((a + PI.toFloat()) / PI.toFloat()).coerceIn(0f, 1f)
        }
    }
}
