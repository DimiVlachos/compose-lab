package dev.dimvlachos.lab.photocopierdemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.photocopier.CopierMode
import dev.dimvlachos.lab.core.presentation.components.photocopier.PhotocopierState
import dev.dimvlachos.lab.core.presentation.components.photocopier.PhotocopierState.Companion.ease
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import kotlin.math.PI
import kotlin.math.sin

/**
 * The script's hand on the lists, as the prototype's `runDemo()` moves its finger: it presses,
 * drags and lets go through the same calls a finger makes, so the scripted copy is fed exactly as
 * one dragged by hand. Points are in the screen's dp; [at], [shown] and [down] draw the fingertip.
 */
@Stable
internal class PhotocopierHand(private val copier: PhotocopierState) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    private val w
        get() = copier.width

    private val h
        get() = copier.height

    /** Plays the scripted step [control], one of [PhotocopierDemos]'. */
    suspend fun play(control: String) {
        when (control) {
            PhotocopierDemos.LongPress -> {
                // Long-press the meal plan.
                appear(Offset(w * .62f, h * .56f))
                val id = copier.cards[0].item.id
                move(Offset(copier.rectX() + copier.cardW * .42f, copier.rectY(id) + 36), 620)
                down = true
                copier.cardDown(id, at.x, at.y, scripted = true)
                wait(560)
                copier.holdScripted()
                wait(150)
                lift()
            }
            PhotocopierDemos.TapDuplicate -> {
                if (copier.menuFor == 0) return
                val item = PhotocopierState.MenuW - 12
                move(Offset(copier.menuX + 6 + item * .38f, copier.menuY + 6 + 22), 480)
                down = true
                wait(110)
                down = false
                copier.duplicate(copier.menuFor)
                wait(180)
                move(Offset(w * .84f, h * .2f), 700)
            }
            PhotocopierDemos.FeedBack -> {
                while (copier.busy || copier.copMode != CopierMode.Peek) wait(100)
                if (copier.cards.size < 2 || !copier.cards[1].item.isCopy) return
                // Push the copy back into the slot.
                val id = copier.cards[1].item.id
                move(Offset(copier.rectX() + copier.cardW * .5f, copier.rectY(id) + 36), 620)
                down = true
                copier.cardDown(id, at.x, at.y, scripted = true)
                wait(170)
                val x0 = at.x
                val y0 = at.y
                val x1 = w * .5f
                val y1 = copier.slotY - 16
                animate(820) { p ->
                    val e = ease(p)
                    at =
                        Offset(
                            x0 + (x1 - x0) * e + 16 * sin(PI.toFloat() * p),
                            y0 + (y1 - y0) * e,
                        )
                    copier.cardMove(at.x, at.y)
                }
                wait(120)
                lift()
                while (copier.busy) wait(100)
                wait(250)
                move(Offset(w * .62f, h * .56f), 500)
                hide()
            }
        }
    }

    private suspend fun appear(to: Offset) {
        at = to
        animate(ScriptedTouch.UpMs) { shown = it }
    }

    private fun lift() {
        down = false
        copier.cardUp(at.x, at.y)
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
        copier.releaseScripted()
        shown = 0f
    }

    private suspend fun move(to: Offset, ms: Int) {
        val from = at
        animate(ms) { t ->
            val p = ease(t)
            at = Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p)
        }
    }

    private suspend fun wait(ms: Int) = animate(ms) {}

    private suspend fun animate(ms: Int, frame: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { t -> p = ((t - start) / 1e6f / ms).coerceIn(0f, 1f) }
            frame(p)
        }
    }
}
