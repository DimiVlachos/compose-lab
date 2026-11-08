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
 * The script's finger on the card screen, every way in and out: it holds the warm card until it
 * freezes, rests a thumb on it and lets go early so the ice closes back in, thaws it for good, then
 * flips the Freeze card switch on and off, which thaws it from the middle by itself. Points are in
 * the screen's dp; [at], [shown] and [down] are for drawing the fingertip.
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
            FrostCardDemos.Hold -> hold()
            FrostCardDemos.Peek -> peek()
            FrostCardDemos.Thaw -> thaw()
            FrostCardDemos.Freeze -> freeze()
            FrostCardDemos.Unfreeze -> unfreeze()
        }
    }

    // From low on the screen to the warm card, held past the half second that freezes it, and
    // aside as the frost grows.
    private suspend fun hold() {
        if (card.frozen) return
        val c = layout.card
        val hx = c.left + c.width * .62f
        val hy = c.top + c.height * .5f
        at = Offset(layout.width * .62f, layout.height * .9f)
        animate(200) { shown = it }
        move(Offset(hx, hy), FrostCardDemos.ToCardMs - 200)
        down = true
        pressing = true
        card.pressStart(hx - c.left, hy - c.top, scripted = true)
        delay(FrostCardDemos.HoldFreezeMs)
        down = false
        release()
        move(Offset(layout.width * .74f, layout.height * .86f), FrostCardDemos.LiftAsideMs)
        hide()
    }

    // Once it has frozen over, a thumb on the card long enough to open a warm hole, then lifted
    // early: the ice closes back in.
    private suspend fun peek() {
        if (!waitFrozen()) return
        val c = layout.card
        val hx = c.left + c.width * .3f
        val hy = c.top + c.height * .45f
        at = Offset(layout.width * .74f, layout.height * .86f)
        animate(200) { shown = it }
        move(Offset(hx, hy), FrostCardDemos.ToCardMs - 200)
        down = true
        pressing = true
        card.pressStart(hx - c.left, hy - c.top, scripted = true)
        delay(FrostCardDemos.PeekMs)
        down = false
        release()
        move(Offset(layout.width * .74f, layout.height * .86f), FrostCardDemos.LiftAsideMs)
        hide()
    }

    // From low on the screen to the switch, a press, the flip, and aside.
    private suspend fun freeze() {
        if (card.frozen) return
        flip()
    }

    // Once the switched frost has covered the card and been seen, the switch again: it thaws from
    // the middle by itself.
    private suspend fun unfreeze() {
        if (!waitFrozen()) return
        delay(FrostCardDemos.SeeFrozenMs)
        if (!card.frozen) return
        flip()
    }

    private suspend fun flip() {
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

    // Once the ice has closed back in, the thumb comes to rest on the card at (.4, .56) and
    // drifts a little, held until the card is clear, then lifts and goes aside.
    private suspend fun thaw() {
        if (!waitFrozen()) return
        // After the early lift, the hole has closed up again and been seen.
        var waited = 0L
        while (card.r > 0f && waited < 5000L) {
            delay(40)
            waited += 40
        }
        delay(FrostCardDemos.SeeClosedMs)
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

    // Waits up to 6 s for the card to be frozen all over, and says whether it is.
    private suspend fun waitFrozen(): Boolean {
        var waited = 0L
        while (!(card.frozen && card.f >= 1f) && waited < 6000L) {
            delay(40)
            waited += 40
        }
        return card.frozen && card.f >= 1f
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
