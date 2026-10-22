package dev.dimvlachos.lab.stickynotedemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.stickynote.NoteAction
import dev.dimvlachos.lab.core.presentation.components.stickynote.StickyNoteState
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import kotlin.math.abs
import kotlin.math.sin

/**
 * The script's hand on the list, as the prototype's demo moves its finger: it goes down, drags and
 * lets go through the same gesture a finger makes, so a scripted note peels exactly as one pulled
 * by hand. Points are in the list's dp; [at], [shown] and [down] are for drawing the fingertip.
 */
@Stable
internal class StickyNoteHand(private val notes: StickyNoteState) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    private var now = 0.0

    /** Plays the scripted step [control], one of [StickyNoteDemos]'. */
    suspend fun play(control: String) {
        when (control) {
            StickyNoteDemos.ShortPull -> {
                // Down 450 ms after it shows, as the prototype's, the fade-in included.
                appear(rowAt(1, -1))
                wait(450 - ScriptedTouch.UpMs)
                press()
                drag(-112f, 950)
                wait(450)
                lift()
            }
            StickyNoteDemos.Archive -> {
                move(rowAt(1, -1), 350)
                wait(150)
                press()
                drag(-214f, 780)
                wait(160)
                lift()
            }
            StickyNoteDemos.Snooze -> {
                move(rowAt(2, 1), 650)
                wait(250)
                press()
                drag(214f, 780)
                wait(140)
                lift()
            }
            StickyNoteDemos.TapLater -> tapPad(NoteAction.Snooze)
            StickyNoteDemos.TapDone -> tapPad(NoteAction.Archive)
        }
    }

    // The prototype's rowAt(): near the bottom corner of row i, on the side the pull starts from.
    private fun rowAt(i: Int, side: Int) =
        Offset(
            if (side < 0) notes.width - notes.mx - 5 else notes.mx + 5,
            notes.slotY(i) + notes.rh - 6,
        )

    private suspend fun tapPad(kind: NoteAction) {
        move(Offset(notes.padCenterX(kind), notes.padCenterY()), 750)
        wait(250)
        press()
        wait(90)
        lift()
    }

    private suspend fun appear(to: Offset) {
        at = to
        animate(ScriptedTouch.UpMs) { shown = it }
    }

    private suspend fun press() {
        clock()
        down = true
        notes.down(at.x, at.y, now, scripted = true)
    }

    private fun lift() {
        down = false
        notes.releaseScripted()
    }

    /** Fades the fingertip out; one held down lets go first. */
    suspend fun hide() {
        if (down) lift()
        val from = shown
        animate(ScriptedTouch.UpMs) { shown = from * (1 - it) }
    }

    /** Lets go and vanishes at once, when the script is stopped. */
    fun stop() {
        down = false
        notes.releaseScripted()
        shown = 0f
    }

    // The prototype's drag: across by [by], the finger rising with the corner, eased.
    private suspend fun drag(by: Float, ms: Int) {
        val sx = at.x
        val sy = at.y
        animate(ms) { t ->
            val p = StickyNoteState.ease(t)
            at =
                Offset(
                    sx + by * p,
                    sy - abs(by) * StickyNoteState.K * p + sin(p * 3) * 1.5f,
                )
            notes.move(at.x, at.y, now)
        }
    }

    private suspend fun move(to: Offset, ms: Int) {
        val from = at
        animate(ms) { t ->
            val p = StickyNoteState.ease(t)
            at = Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p)
        }
    }

    private suspend fun wait(ms: Int) = animate(ms) {}

    private suspend fun clock() = withFrameNanos { now = it / 1e6 }

    private suspend fun animate(ms: Int, frame: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { t ->
                now = t / 1e6
                p = ((t - start) / 1e6f / ms).coerceIn(0f, 1f)
            }
            frame(p)
        }
    }
}
