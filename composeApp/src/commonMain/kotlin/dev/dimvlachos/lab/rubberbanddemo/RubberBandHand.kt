package dev.dimvlachos.lab.rubberbanddemo

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.rubberband.RubberBandState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The script's hand on the chat, as the prototype's demo moves its finger: it taps the field and
 * types into it (without focusing it, so no keyboard comes up over the clip), taps Send, and grabs,
 * pulls and lets go of the band through the same gesture a finger makes. Points are in the screen's
 * dp; [at], [shown] and [down] are for drawing the fingertip.
 */
@Stable
internal class RubberBandHand(
    private val chat: RubberBandState,
    private val field: TextFieldState,
) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    /** Plays the scripted step [control], one of [RubberBandDemos]'. */
    suspend fun play(control: String) {
        val w = chat.width
        val h = chat.height
        when (control) {
            RubberBandDemos.TypeFirst -> {
                at = Offset(w * .5f, h * .55f)
                // The fingertip fades in as it sets off, as the prototype's 0.2 s transition.
                coroutineScope {
                    launch { show() }
                    tapField(.4f)
                }
                move(Offset(w * .62f, h * .7f), RubberBandDemos.AsideMs)
                type(RubberBandDemos.Typed)
            }
            RubberBandDemos.Send -> tapSend()
            RubberBandDemos.PullBack -> pullBack()
            RubberBandDemos.Fix -> {
                tapField(.7f)
                move(Offset(w * .62f, h * .7f), RubberBandDemos.AsideMs)
                field.edit { if (length > 0) replace(length - 1, length, "") }
                delay(260)
                type(RubberBandDemos.Fixed)
            }
            RubberBandDemos.SendAgain -> {
                tapSend()
                delay(RubberBandDemos.LiftAfterMs)
                hide()
            }
            RubberBandDemos.Reply -> {
                chat.showTyping()
                delay(RubberBandDemos.TypingMs)
                chat.reply(RubberBandDemos.Replied)
            }
        }
    }

    /**
     * Fades the thread out, puts the opening thread back with an empty field, and fades it in
     * again.
     */
    suspend fun fadeBack() {
        stop()
        animate(RubberBandDemos.FadeMs) { chat.threadAlpha = 1 - it }
        chat.reset()
        field.clearText()
        chat.threadAlpha = 0f
        animate(RubberBandDemos.FadeMs) { chat.threadAlpha = it }
        chat.threadAlpha = 1f
    }

    // Oops, it's 8:30: grab the bubble near its left end and pull it down and a little left.
    private suspend fun pullBack() {
        val t = chat.tether ?: return
        val r = chat.bubbleRect(t) ?: return
        val k = chat.k
        val g = Offset(r.left + 26 * k, r.top + r.height * .62f)
        move(g, RubberBandDemos.ToBubbleMs)
        down = true
        chat.down(g.x, g.y, scripted = true)
        delay(RubberBandDemos.GripMs)
        animate(RubberBandDemos.PullMs) {
            val e = RubberBandState.ease(it)
            at = Offset(g.x - 18 * k * e, g.y + 84 * k * e)
            chat.move(at.x, at.y, scripted = true)
        }
        delay(RubberBandDemos.HoldPulledMs)
        down = false
        chat.releaseScripted()
    }

    private suspend fun tapField(across: Float) {
        val f = chat.fieldBox
        tap(Offset(f.left + f.width * across, f.center.y))
    }

    private suspend fun tapSend() {
        tap(chat.sendBox.center)
        if (chat.send(field.text.toString())) field.clearText()
    }

    private suspend fun tap(to: Offset) {
        move(to, RubberBandDemos.TapMs)
        down = true
        delay(RubberBandDemos.HoldMs)
        down = false
    }

    // Each key is due at its time from the start, read off the frame clock, so slow frames type
    // several keys at once rather than falling behind the script's next step.
    private suspend fun type(text: String) {
        val start = withFrameNanos { it }
        var due = 0L
        var i = 0
        while (i < text.length) {
            val ms = withFrameNanos { (it - start) / 1_000_000L }
            while (i < text.length && ms >= due) {
                val ch = text[i]
                field.edit { append(ch) }
                due += RubberBandDemos.keyMs(ch)
                i++
            }
        }
    }

    private suspend fun show() = animate(200) { shown = it }

    private suspend fun hide() {
        val from = shown
        animate(200) { shown = from * (1 - it) }
    }

    /** Lets go and vanishes at once, when the script is stopped. */
    fun stop() {
        down = false
        chat.dropScripted()
        shown = 0f
        chat.threadAlpha = 1f
    }

    private suspend fun move(to: Offset, ms: Long) {
        val from = at
        animate(ms) {
            val p = RubberBandState.ease(it)
            at = Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p)
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
