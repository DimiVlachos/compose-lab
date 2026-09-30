package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp

/**
 * The marks on a [FoggedWindow], in the order they were made: wipes that clear the fog and breaths
 * that fog it back over. The order is the point: a wipe after a breath clears its fog, a breath
 * after a wipe covers it. A window can start clear, as if already cleared. Wipe points are
 * fractions of the window, so the marks stay put when the window changes size.
 */
@Stable
class FogState(startClear: Boolean = false) {
    private val _marks =
        mutableStateListOf<FogMark>().apply {
            if (startClear) add(Evaporation().apply { amount = 1f })
        }

    val marks: List<FogMark>
        get() = _marks

    /** Each wipe's points, oldest first. */
    val strokes: List<List<Offset>>
        get() = _marks.filterIsInstance<WipeStroke>().map { it.points }

    /**
     * Starts a stroke and returns it. Each finger extends only its own, so a real finger and the
     * script's can wipe at once without joining up. [clarity] is how much of the fog it clears: 1
     * wipes it away, less leaves a wet film, as a running drop does.
     */
    fun beginStroke(at: Offset, radius: Dp? = null, clarity: Float = 1f): WipeStroke {
        val stroke = WipeStroke(mutableStateListOf(at), radius, clarity)
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
     * fresh fog: the breath and every mark before it are dropped.
     */
    fun setBreathLevel(breath: Breath, level: Float) {
        val index = _marks.indexOf(breath)
        if (index < 0 || level <= breath.level) return
        breath.level = level
        if (level >= 1f) _marks.removeRange(0, index + 1)
    }

    /** Starts the fog evaporating evenly over the whole window, and returns the evaporation. */
    fun beginEvaporation(): Evaporation {
        val evaporation = Evaporation()
        _marks += evaporation
        return evaporation
    }

    /**
     * Clears [evaporation] to [amount], from 0, untouched, to 1, clear glass. It never fogs back,
     * and an evaporation already dropped stays gone. At 1 nothing before it shows: the evaporation
     * is all that is left.
     */
    fun setEvaporationAmount(evaporation: Evaporation, amount: Float) {
        val index = _marks.indexOf(evaporation)
        if (index < 0 || amount <= evaporation.amount) return
        evaporation.amount = amount.coerceAtMost(1f)
        if (amount >= 1f) _marks.removeRange(0, index)
    }

    /** Takes [mark] off the glass, as if it had never been made. */
    internal fun remove(mark: FogMark) {
        _marks.remove(mark)
    }

    /** Clears the whole window at once. */
    fun evaporate() {
        _marks.clear()
        _marks += Evaporation().apply { amount = 1f }
    }

    /** Fogs the whole window over again. */
    fun clear() {
        _marks.clear()
    }
}

/** A wipe or a breath on a [FogState]. */
sealed interface FogMark

/**
 * One finger's stroke on a [FogState], from [FogState.beginStroke]; [radius] its own brush, a
 * fingertip say, or null for the window's; [clarity] how much of the fog it clears.
 */
class WipeStroke
internal constructor(
    internal val points: SnapshotStateList<Offset>,
    val radius: Dp? = null,
    val clarity: Float = 1f,
) : FogMark

/** The fog evaporating evenly, from [FogState.beginEvaporation]; at 1 the glass is clear. */
class Evaporation internal constructor() : FogMark {
    var amount by mutableFloatStateOf(0f)
        internal set
}

/** Fog rising from the bottom of a [FogState], from [FogState.beginBreath]. */
class Breath internal constructor() : FogMark {
    var level by mutableFloatStateOf(0f)
        internal set
}
