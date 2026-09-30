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
 * breath after a wipe covers it. Wipe points are fractions of the window, so the marks stay put
 * when the window changes size.
 */
@Stable
class FrostState {
    private val _marks = mutableStateListOf<FrostMark>()

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

    /** Frosts the whole window over again. */
    fun clear() {
        _marks.clear()
    }
}

/** A wipe or a breath on a [FrostState]. */
sealed interface FrostMark

/** One finger's stroke on a [FrostState], from [FrostState.beginStroke]. */
class WipeStroke internal constructor(internal val points: SnapshotStateList<Offset>) : FrostMark

/** Fog rising from the bottom of a [FrostState], from [FrostState.beginBreath]. */
class Breath internal constructor() : FrostMark {
    var level by mutableFloatStateOf(0f)
        internal set
}
