package dev.dimvlachos.lab.frenchpressdemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.frenchpress.FrenchPressState
import dev.dimvlachos.lab.core.presentation.components.frenchpress.FrenchPressState.Companion.ease
import dev.dimvlachos.lab.core.presentation.components.frenchpress.FrenchPressState.Companion.lerp
import dev.dimvlachos.lab.core.presentation.components.frenchpress.PressMode
import kotlin.math.min

/**
 * The script's hand on the press, as the prototype's `runDemo()` moves its finger: it pushes the
 * knob through the same `pressDown/Move/Up` a finger does, riding the knob while the plunger holds
 * back, and taps Full article. A real touch, a key or the button takes over and the hand leaves.
 * Points are in the screen's dp; [at], [shown] and [down] are for drawing the fingertip.
 */
@Stable
internal class FrenchPressHand(private val press: FrenchPressState) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    private var turn = 0

    // Whether a real touch has taken over since this step began.
    private fun ok() = press.takenOver == turn

    /** Plays the scripted step [control], one of [FrenchPressDemos]'. */
    suspend fun play(control: String) {
        turn = press.takenOver
        when (control) {
            FrenchPressDemos.Push -> push()
            FrenchPressDemos.Full -> full()
        }
        if (!ok()) stop()
    }

    private suspend fun push() {
        appear(Offset(press.width * .5f, press.height * .55f))
        val kx = press.knobX
        val ky = press.knobScreenY
        move(Offset(kx, ky), 620)
        if (!ok()) return
        down = true
        press.pressDown(kx, ky, scripted = true)
        wait(140)
        if (!ok()) return
        // Push a little: the words start pouring in and the plunger pushes back until they're in.
        val t = press.travel
        animate(650) { if (ok()) pushTo(kx, ky + t * .32f * ease(it)) }
        if (!ok()) return
        // Keep the finger on the knob as it catches up.
        while (ok() && press.arrived < press.total) {
            at = Offset(kx, press.knobScreenY)
            frame()
        }
        if (!ok()) return
        wait(120)
        if (!ok()) return
        animate(820) { if (ok()) pushTo(kx, ky + t * lerp(.32f, 1f, ease(it))) }
        if (!ok()) return
        wait(160)
        if (!ok()) return
        down = false
        press.pressUp(kx, ky + t)
        wait(250)
        fade(0f)
    }

    private suspend fun full() {
        while (ok() && press.mode != PressMode.Brewed) frame()
        wait(2200)
        if (!ok()) return
        appear(Offset(press.width * .6f, press.fullY + 120 * press.k))
        move(Offset(press.fullX, press.fullY), 600)
        if (!ok()) return
        down = true
        wait(120)
        if (!ok()) return
        down = false
        press.showFull()
        wait(300)
        fade(0f)
        while (ok() && press.mode != PressMode.Idle) frame()
    }

    // The demo finger rides the knob: while the plunger is held back it stays on the knob
    // instead of sliding off below it.
    private fun pushTo(kx: Float, y: Float) {
        press.pressMove(kx, y)
        at = Offset(kx, min(y, press.knobScreenY))
    }

    private suspend fun appear(to: Offset) {
        at = to
        fade(1f)
    }

    private suspend fun fade(to: Float) {
        val from = shown
        animate(FadeMs) { shown = from + (to - from) * it }
    }

    /** Fades the fingertip out; one held down lets go first. */
    suspend fun hide() {
        if (down) {
            down = false
            press.releaseScripted()
        }
        fade(0f)
    }

    /** Lets go and vanishes at once, when the script is stopped or a finger takes over. */
    fun stop() {
        down = false
        press.releaseScripted()
        shown = 0f
    }

    private suspend fun move(to: Offset, ms: Int) {
        val from = at
        animate(ms) { p ->
            val e = ease(p)
            at = Offset(from.x + (to.x - from.x) * e, from.y + (to.y - from.y) * e)
        }
    }

    private suspend fun wait(ms: Int) = animate(ms) {}

    private suspend fun frame() = withFrameNanos {}

    private suspend fun animate(ms: Int, step: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { t -> p = ((t - start) / 1e6f / ms).coerceIn(0f, 1f) }
            step(p)
        }
    }

    private companion object {
        // The prototype's finger fades in and out over .2 s.
        const val FadeMs = 200
    }
}
