package dev.dimvlachos.lab.graveundodemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.graveundo.CardPose
import dev.dimvlachos.lab.core.presentation.components.graveundo.GravePhase
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveUndoState
import kotlinx.coroutines.delay

/**
 * The script's hand on the list, as the prototype's demo moves its finger: it swipes the first task
 * away through the same drag a finger makes, and taps the hand that comes up holding it. Points are
 * in the screen's dp; [at], [shown] and [down] are for drawing the fingertip.
 */
@Stable
internal class GraveUndoHand(private val list: GraveUndoState) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    private val card = CardPose()

    /** Plays the scripted step [control], one of [GraveUndoDemos]'. */
    suspend fun play(control: String) {
        list.scripted = true
        when (control) {
            GraveUndoDemos.Bury -> bury(0)
            GraveUndoDemos.BurySecond -> bury(1)
            GraveUndoDemos.Undo -> undo()
            GraveUndoDemos.UndoAtGrave -> undoAtGrave()
        }
    }

    // Once no task is flying, down on the [nth] task still on the list and not in the ground, three
    // quarters across, and drag it half the row's width left: past the line, so letting go buries
    // it.
    private suspend fun bury(nth: Int) {
        waitFor { list.graves.none { it.phase in Flying } }
        val item =
            list.items
                .filter { item -> !item.gone && list.graves.none { it.item === item } }
                .getOrNull(nth) ?: return
        val w = list.width
        val sx = w * .74f
        val sy = list.rowTop(item) + list.rowH / 2
        at = Offset(sx + 40, sy + 130)
        animate(200) { shown = it }
        move(Offset(sx, sy), GraveUndoDemos.ToRowMs - 200)
        down = true
        list.down(sx, sy, nowMs(), scripted = true)
        delay(GraveUndoDemos.GripMs)
        animate(GraveUndoDemos.DragMs) {
            val e = GraveUndoState.ease(it)
            at = Offset(sx - w * .52f * e, sy + 4 * e)
        }
        delay(GraveUndoDemos.HoldMs)
        down = false
        list.up(scripted = true)
        move(Offset(at.x - 10, at.y + 70), GraveUndoDemos.AsideMs - 200)
        hide()
    }

    // Wait for the hand to be up holding the task, let it be seen, then tap the card in its hand.
    private suspend fun undo() {
        waitFor { list.graves.any { it.phase == GravePhase.Held } }
        delay(GraveUndoDemos.ShowHandMs)
        val g = list.undoable() ?: return
        list.cardInHand(g, card)
        at = Offset(card.x + 50, card.y + 110)
        animate(200) { shown = it }
        move(Offset(card.x + 4, card.y + 30 * list.u), GraveUndoDemos.ToHandMs - 200)
        down = true
        delay(GraveUndoDemos.PressMs)
        down = false
        list.undo()
        move(Offset(at.x + 40, at.y + 90), GraveUndoDemos.UndoAsideMs - 200)
        hide()
    }

    // Wait for the task to land in the soil, then tap the headstone: the grave takes it back too,
    // while the hand is still on its way up.
    private suspend fun undoAtGrave() {
        waitFor { list.graves.any { it.phase == GravePhase.Rest } }
        delay(GraveUndoDemos.SeeRestMs)
        if (list.undoable() == null) return
        val stone = Offset(list.stoneX, list.soil - 22 * list.u)
        at = Offset(stone.x + 50, stone.y + 110)
        animate(200) { shown = it }
        move(stone, GraveUndoDemos.ToHandMs - 200)
        down = true
        delay(GraveUndoDemos.PressMs)
        down = false
        list.undo()
        move(Offset(at.x + 40, at.y + 90), GraveUndoDemos.UndoAsideMs - 200)
        hide()
    }

    // Polls [ready] for up to 9 s, the longest a grave takes to get anywhere.
    private suspend fun waitFor(ready: () -> Boolean) {
        var waited = 0L
        while (!ready() && waited < 9000L) {
            delay(40)
            waited += 40
        }
    }

    private suspend fun hide() {
        val from = shown
        animate(200) { shown = from * (1 - it) }
    }

    /** Lets go and vanishes at once, when the script is stopped. */
    fun stop() {
        down = false
        list.dropScripted()
        list.scripted = false
        shown = 0f
    }

    private var frameMs = 0.0

    private companion object {
        // A task on its way into the ground or back out of it: no new swipe while one is.
        val Flying = setOf(GravePhase.Fly, GravePhase.Undo, GravePhase.Land)
    }

    private fun nowMs() = frameMs

    // Moves the fingertip to [to] over [ms], eased, and drags the row with it while it is down.
    private suspend fun move(to: Offset, ms: Long) {
        val from = at
        animate(ms) {
            val p = GraveUndoState.ease(it)
            at = Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p)
        }
    }

    private suspend fun animate(ms: Long, frame: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { t ->
                p = ((t - start) / 1e6f / ms).coerceIn(0f, 1f)
                frameMs = t / 1e6
            }
            frame(p)
            if (down) list.move(at.x, at.y, frameMs, scripted = true)
        }
    }
}
