package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset

/**
 * The wipes on a [FrostedWindow], each a stroke of points given as fractions of the window, so the
 * wiped marks stay put when the window changes size.
 */
@Stable
class FrostState {
    private val _strokes = mutableStateListOf<SnapshotStateList<Offset>>()

    val strokes: List<List<Offset>>
        get() = _strokes

    /**
     * Starts a stroke and returns it. Each finger extends only its own, so a real finger and the
     * script's can wipe at once without joining up.
     */
    fun beginStroke(at: Offset): WipeStroke {
        val points = mutableStateListOf(at)
        _strokes += points
        return WipeStroke(points)
    }

    /** Adds [to] to [stroke]; a stroke from before the last [clear] stays gone. */
    fun extendStroke(stroke: WipeStroke, to: Offset) {
        stroke.points += to
    }

    /** Frosts the whole window over again. */
    fun clear() {
        _strokes.clear()
    }
}

/** One finger's stroke on a [FrostState], from [FrostState.beginStroke]. */
class WipeStroke internal constructor(internal val points: SnapshotStateList<Offset>)
