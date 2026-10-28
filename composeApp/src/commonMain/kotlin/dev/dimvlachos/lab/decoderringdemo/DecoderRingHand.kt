package dev.dimvlachos.lab.decoderringdemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingState
import dev.dimvlachos.lab.core.presentation.components.decoderring.RingButton
import kotlin.math.cos
import kotlin.math.sin

/**
 * The script's hand on the ring, as the prototype's demo moves its finger: it goes down on the
 * ring, twists round it and lets go through the same gesture a finger makes. Points are in the
 * screen's dp; [ring] gives the ring's centre there now. [at], [shown] and [down] draw the
 * fingertip.
 */
@Stable
internal class DecoderRingHand(
    private val state: DecoderRingState,
    private val ring: () -> Offset?,
    private val screen: () -> Offset?,
) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    private var held = -1

    /** Plays the scripted step [control], one of [DecoderRingDemos]'. */
    suspend fun play(control: String) {
        when (control) {
            DecoderRingDemos.Twist -> {
                // It shows low on the right, as the prototype's, and fades in as it glides over.
                screen()?.let { at = Offset(it.x * .62f, it.y * .78f) }
                twist(-2.6f, 4.6f, 1500, appear = true)
            }
            DecoderRingDemos.TwistBack -> twist(1.8f, -4.4f, 1400, appear = false)
        }
    }

    // The prototype's twist(): round the ring from angle a0 by sweep, at .78 of its radius.
    private suspend fun twist(a0: Float, sweepBy: Float, ms: Int, appear: Boolean) {
        // With reduced motion the finger only taps the ring, and the text changes at once.
        val sweep = if (state.reducedMotion) 0f else sweepBy
        fun point(a: Float): Offset? {
            val c = ring() ?: return null
            val r = RingButton / 2f * .78f
            return Offset(c.x + cos(a) * r, c.y + sin(a) * r)
        }
        val start = point(a0) ?: return
        val from = at
        animate(GlideMs) { p ->
            if (appear) shown = (p * GlideMs / FadeMs).coerceAtMost(1f)
            val e = DecoderRingState.ease(p)
            at = Offset(from.x + (start.x - from.x) * e, from.y + (start.y - from.y) * e)
        }
        shown = 1f
        down = true
        // A finger already on the ring keeps it.
        if (state.held) return
        held = rel(start)?.let { state.down(it.x, it.y) } ?: return
        wait(PressMs)
        animate(ms) { p ->
            val to = point(a0 + sweep * DecoderRingState.ease(p)) ?: return@animate
            at = to
            // A finger that took the ring over has it from here.
            val d = rel(to)
            if (d != null && state.holds(held)) state.move(d.x, d.y, RingButton / 2f)
        }
        wait(HoldMs)
        down = false
        val d = rel(at)
        if (d != null && state.holds(held)) state.up(d.x, d.y, RingButton / 2f)
        held = -1
    }

    private fun rel(p: Offset): Offset? = ring()?.let { p - it }

    /** Fades the fingertip out; a twist still held is let go first. */
    suspend fun hide() {
        if (state.holds(held)) state.release()
        held = -1
        down = false
        val from = shown
        animate(FadeMs) { shown = from * (1 - it) }
    }

    /**
     * Drops a held twist without letting it go and vanishes, when a key or a screen reader takes
     * over: what they do next decides where the wave goes.
     */
    fun drop() {
        state.drop(held)
        held = -1
        down = false
        shown = 0f
    }

    /** Lets go and vanishes at once, when the script is stopped. */
    fun stop() {
        // A stopped twist must not be left held, or the wave would wait for it halfway.
        if (state.holds(held)) state.release()
        held = -1
        down = false
        shown = 0f
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

    private companion object {
        const val GlideMs = 520
        const val FadeMs = 200
        const val PressMs = 140
        const val HoldMs = 90
    }
}
