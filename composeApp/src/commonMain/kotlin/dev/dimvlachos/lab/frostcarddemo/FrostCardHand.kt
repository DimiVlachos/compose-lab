package dev.dimvlachos.lab.frostcarddemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.frostcard.FrostCardLayout
import dev.dimvlachos.lab.core.presentation.components.frostcard.FrostCardState
import kotlinx.coroutines.delay

/**
 * The script's finger on the card screen, as the prototype's demo moves it: it taps the Freeze card
 * switch, then rests a thumb on the frozen card, drifting a little, until the card is clear. Points
 * are in the screen's dp; [at], [shown] and [down] are for drawing the fingertip.
 */
@Stable
internal class FrostCardHand(
    private val card: FrostCardState,
    private val layout: FrostCardLayout,
    private val toggle: () -> Unit,
) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    private var pressing = false

    /** Plays the scripted step [control], one of [FrostCardDemos]'. */
    suspend fun play(control: String) {
        when (control) {
            FrostCardDemos.Freeze -> freeze()
            FrostCardDemos.Thaw -> thaw()
        }
    }

    // From low on the screen to the switch, a press, the flip, and aside.
    private suspend fun freeze() {
        if (card.frozen) return
        val w = layout.width
        val h = layout.height
        at = Offset(w * .62f, h * .9f)
        animate(200) { shown = it }
        move(layout.switch, FrostCardDemos.ToSwitchMs - 200)
        down = true
        delay(FrostCardDemos.PressMs)
        toggle()
        down = false
        move(Offset(w * .74f, h * .86f), FrostCardDemos.AsideMs)
        hide()
    }

    // Once the card has frozen over, the thumb comes to rest on it at (.4, .56) and drifts a
    // little, held until the card is clear, then lifts and goes aside.
    private suspend fun thaw() {
        var waited = 0L
        while (card.frozen && card.f < 1f && waited < 6000L) {
            delay(40)
            waited += 40
        }
        if (!card.frozen) return
        val c = layout.card
        val hx = c.left + c.width * .4f
        val hy = c.top + c.height * .56f
        at = Offset(layout.width * .74f, layout.height * .86f)
        animate(200) { shown = it }
        move(Offset(hx, hy), FrostCardDemos.ToCardMs - 200)
        down = true
        pressing = true
        card.pressStart(hx - c.left, hy - c.top, scripted = true)
        move(Offset(hx + c.width * .06f, hy - c.height * .04f), FrostCardDemos.DriftMs)
        var held = 0L
        while (card.frozen && held < FrostCardDemos.HoldMaxMs) {
            delay(80)
            held += 80
        }
        down = false
        release()
        move(Offset(layout.width * .74f, layout.height * .86f), FrostCardDemos.LiftAsideMs)
        hide()
    }

    private fun release() {
        if (pressing) card.pressEnd(scripted = true)
        pressing = false
    }

    private suspend fun hide() {
        val from = shown
        animate(200) { shown = from * (1 - it) }
    }

    /** Lets go and vanishes at once, when the script is stopped. */
    fun stop() {
        down = false
        release()
        shown = 0f
    }

    // Moves the fingertip to [to] over [ms], eased, and drags the thumb with it while it is down.
    private suspend fun move(to: Offset, ms: Long) {
        val from = at
        animate(ms) {
            val p = FrostCardState.ease(it)
            at = Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p)
            if (pressing)
                card.pressMove(at.x - layout.card.left, at.y - layout.card.top, scripted = true)
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
