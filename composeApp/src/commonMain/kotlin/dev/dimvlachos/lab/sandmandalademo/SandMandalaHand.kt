package dev.dimvlachos.lab.sandmandalademo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandAt
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaState
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandPlaces
import kotlinx.coroutines.delay

/**
 * The script's hand on the History screen, as the prototype's demo moves its finger: it taps Clear
 * history, sweeps the finished mandala through the same brush a finger makes, and taps Undo. Points
 * are in the screen's dp; [at], [shown] and [down] are for drawing the fingertip.
 */
@Stable
internal class SandMandalaHand(
    private val history: SandMandalaState,
    private val places: SandPlaces,
) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    /** The screen's size in dp, for where the finger rests between moves. */
    var screenW = 0f
    var screenH = 0f

    private var brushing = false

    // Set when a real finger or the script's end takes over: the steps still running do nothing
    // more.
    private var stopped = false

    /** Plays the scripted step [control], one of [SandMandalaDemos]'. */
    suspend fun play(control: String) {
        stopped = false
        history.scripted = true
        when (control) {
            SandMandalaDemos.Clear -> clear()
            SandMandalaDemos.Sweep -> sweep()
            SandMandalaDemos.Undo -> undo()
        }
    }

    // From the middle of the screen to Clear history, a press, and aside to the top right.
    private suspend fun clear() {
        waitFor { history.at == SandAt.Rows && history.n > 0 }
        if (history.at != SandAt.Rows || history.n == 0) return
        at = Offset(screenW * .62f, screenH * .55f)
        show()
        tap(places.clear)
        if (stopped) return
        history.clearHistory()
        move(Offset(screenW * .84f, screenH * .2f), SandMandalaDemos.AsideMs)
        hide()
    }

    // Once the mandala is finished and seen, one unhurried sweep from its top left to its bottom
    // right, curving through the centre.
    private suspend fun sweep() {
        waitFor { history.at == SandAt.Mandala }
        delay(SandMandalaDemos.SeeMandalaMs)
        if (history.at != SandAt.Mandala) return
        val o = places.list
        val cx = history.cx
        val cy = history.cy
        val r = history.r
        at = Offset(o.x + cx - r * .78f, o.y + cy - r * .5f)
        show()
        delay(SandMandalaDemos.RestMs)
        if (stopped) return
        down = true
        brushing = true
        history.brushDown(at.x - o.x, at.y - o.y, scripted = true)
        delay(SandMandalaDemos.GripMs)
        move(Offset(o.x + cx - r * .3f, o.y + cy - r * .08f), SandMandalaDemos.LegMs)
        move(Offset(o.x + cx + r * .25f, o.y + cy + r * .12f), SandMandalaDemos.LegMs)
        move(Offset(o.x + cx + r * .8f, o.y + cy + r * .42f), SandMandalaDemos.LegMs)
        down = false
        brushing = false
        history.brushUp(scripted = true)
        move(Offset(screenW * .82f, o.y + cy + r * .9f), SandMandalaDemos.SweepAsideMs)
        hide()
    }

    // Once the history is cleared and seen, tap Undo and go aside.
    private suspend fun undo() {
        waitFor { history.at == SandAt.Cleared && history.snackOn }
        delay(SandMandalaDemos.SeeClearedMs)
        if (!history.snackOn) return
        at = Offset(screenW * .5f, screenH * .6f)
        show()
        tap(places.undo)
        if (stopped) return
        history.undo()
        move(Offset(screenW * .5f, screenH * .78f), SandMandalaDemos.UndoAsideMs)
        hide()
    }

    private suspend fun tap(to: Offset) {
        move(to, SandMandalaDemos.ToButtonMs)
        down = true
        delay(SandMandalaDemos.PressMs)
        down = false
    }

    // Polls [ready] for up to 12 s, longer than any of the sand's moves.
    private suspend fun waitFor(ready: () -> Boolean) {
        var waited = 0L
        while (!ready() && waited < 12_000L) {
            delay(40)
            waited += 40
        }
    }

    private suspend fun show() {
        animate(SandMandalaDemos.ShowMs) { shown = it }
    }

    private suspend fun hide() {
        val from = shown
        animate(SandMandalaDemos.ShowMs) { shown = from * (1 - it) }
    }

    /** Lets go and vanishes at once, when the script is stopped or a real finger comes down. */
    fun stop() {
        stopped = true
        if (brushing) history.brushCancel(scripted = true)
        brushing = false
        down = false
        history.scripted = false
        shown = 0f
    }

    // Moves the fingertip to [to] over [ms], eased in and out, brushing the sand while it is down.
    private suspend fun move(to: Offset, ms: Long) {
        val from = at
        animate(ms) {
            val p = SandMandalaState.ease(it)
            at = Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p)
            if (brushing)
                history.brushMove(at.x - places.list.x, at.y - places.list.y, scripted = true)
        }
    }

    private suspend fun animate(ms: Long, frame: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { t -> p = ((t - start) / 1e6f / ms).coerceIn(0f, 1f) }
            frame(p)
        }
    }
}
