package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset

/**
 * The marks on a [FrostedWindow], in the order they were made: wipes that clear the frost and
 * breaths that fog it back over. The order is the point: a wipe after a breath clears its fog, a
 * breath after a wipe covers it. A window can start clear, as if already thawed. Wipe points are
 * fractions of the window, so the marks stay put when the window changes size.
 */
@Stable
class FrostState(startClear: Boolean = false) {
    private val _marks =
        mutableStateListOf<FrostMark>().apply { if (startClear) add(Thaw().apply { amount = 1f }) }

    val marks: List<FrostMark>
        get() = _marks

    /** Each wipe's points, oldest first. */
    val strokes: List<List<Offset>>
        get() = _marks.filterIsInstance<WipeStroke>().map { it.points }

    /**
     * Starts a stroke and returns it. Each finger extends only its own, so a real finger and the
     * script's can wipe at once without joining up.
     */
    fun beginStroke(at: Offset): WipeStroke {
        val stroke = WipeStroke(mutableStateListOf(at))
        _marks += stroke
        return stroke
    }

    /** Adds [to] to [stroke]; a stroke from before the last [clear] stays gone. */
    fun extendStroke(stroke: WipeStroke, to: Offset) {
        stroke.points += to
    }

    /** Starts a breath with its fog at the bottom edge, and returns it. */
    fun beginBreath(): Breath {
        val breath = Breath()
        _marks += breath
        return breath
    }

    /**
     * Raises [breath]'s fog to [level], a fraction of the window height from the bottom. Fog never
     * sinks, and a breath already dropped stays gone. At 1 the fog covers the window, which is then
     * fresh frost: the breath and every mark before it are dropped.
     */
    fun setBreathLevel(breath: Breath, level: Float) {
        val index = _marks.indexOf(breath)
        if (index < 0 || level <= breath.level) return
        breath.level = level
        if (level >= 1f) _marks.removeRange(0, index + 1)
    }

    /** Starts the frost melting away evenly over the whole window, and returns the thaw. */
    fun beginThaw(): Thaw {
        val thaw = Thaw()
        _marks += thaw
        return thaw
    }

    /**
     * Melts [thaw] to [amount], from 0, untouched, to 1, clear glass. It never freezes back, and a
     * thaw already dropped stays gone. At 1 nothing before it shows: the thaw is all that is left.
     */
    fun setThawAmount(thaw: Thaw, amount: Float) {
        val index = _marks.indexOf(thaw)
        if (index < 0 || amount <= thaw.amount) return
        thaw.amount = amount.coerceAtMost(1f)
        if (amount >= 1f) _marks.removeRange(0, index)
    }

    /** Clears the whole window at once. */
    fun thaw() {
        _marks.clear()
        _marks += Thaw().apply { amount = 1f }
    }

    /** Frosts the whole window over again. */
    fun clear() {
        _marks.clear()
    }
}

/** A wipe or a breath on a [FrostState]. */
sealed interface FrostMark

/** One finger's stroke on a [FrostState], from [FrostState.beginStroke]. */
class WipeStroke internal constructor(internal val points: SnapshotStateList<Offset>) : FrostMark

/** The frost melting away evenly, from [FrostState.beginThaw]; at 1 the glass is clear. */
class Thaw internal constructor() : FrostMark {
    var amount by mutableFloatStateOf(0f)
        internal set
}

/** Fog rising from the bottom of a [FrostState], from [FrostState.beginBreath]. */
class Breath internal constructor() : FrostMark {
    var level by mutableFloatStateOf(0f)
        internal set
}
