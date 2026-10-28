package dev.dimvlachos.lab.spiderdemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderPhase
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderRefreshState
import kotlin.math.PI
import kotlin.math.sin

/**
 * The script's hand on the feed, as the prototype's demo moves its finger: it pulls through the
 * same calls a finger makes, and taps the refresh button. Points are in the screen's dp; [feed]
 * gives the feed's top left there now and [button] the refresh button's centre. [at], [shown] and
 * [down] draw the fingertip.
 */
@Stable
internal class SpiderHand(
    private val state: SpiderRefreshState,
    private val feed: () -> Offset?,
    private val button: () -> Offset?,
) {
    var at by mutableStateOf(Offset.Zero)
        private set

    var shown by mutableFloatStateOf(0f)
        private set

    var down by mutableStateOf(false)
        private set

    private var holding = false

    /** Plays the scripted step [control], one of [SpiderDemos]'. */
    suspend fun play(control: String) {
        val origin = feed() ?: return
        val x0 = state.width * .5f + 34f
        when (control) {
            // A short pull: the spider peeks out and scurries back.
            SpiderDemos.PullShort -> {
                if (shown <= 0f) at = origin + Offset(x0 + 30f, 330f)
                glide(origin + Offset(x0, 120f), 620)
                pull(origin, x0, 120f, 4f, 92f, 760, 0f, hold = 520)
            }
            // A full pull, swaying a little: its eyes go wide, and letting go fetches.
            SpiderDemos.Pull -> {
                if (shown <= 0f) at = origin + Offset(x0 + 30f, 330f)
                glide(origin + Offset(x0 - 8f, 104f), 560)
                pull(origin, x0 - 8f, 104f, 10f, 218f, 1150, -26f, hold = 560)
            }
            // The refresh button runs the same sequence.
            SpiderDemos.Button -> {
                val b = button() ?: return
                if (shown <= 0f) at = b + Offset(-60f, 260f)
                glide(b, 620)
                down = true
                wait(PressMs)
                down = false
                state.refreshButton()
            }
        }
        wait(LiftMs)
        hide()
        // The next step waits until the spider is home, however late it runs.
        while (state.phase != SpiderPhase.Idle) withFrameNanos {}
    }

    /** Glides the fingertip to [to] over [ms], fading it in if it was hidden. */
    private suspend fun glide(to: Offset, ms: Int) {
        val appear = shown <= 0f
        val from = at
        animate(ms) { p, _ ->
            if (appear) shown = (p * ms / FadeMs).coerceAtMost(1f)
            val e = SpiderRefreshState.ease(p)
            at = Offset(from.x + (to.x - from.x) * e, from.y + (to.y - from.y) * e)
        }
        shown = 1f
    }

    /**
     * The prototype's drag(): down at ([x], [y]) in the feed, then by ([dx], [dy]) with a sway,
     * held still for [hold] ms before it lets go.
     */
    private suspend fun pull(
        origin: Offset,
        x: Float,
        y: Float,
        dx: Float,
        dy: Float,
        ms: Int,
        sway: Float,
        hold: Int,
    ) {
        down = true
        state.down(x, y, real = false, atMs = withFrameNanos { it } / 1_000_000)
        holding = true
        animate(ms) { p, t ->
            val e = SpiderRefreshState.ease(p)
            val fx = x + dx * e + sway * sin(p * PI.toFloat())
            val fy = y + dy * e
            at = origin + Offset(fx, fy)
            if (holding && state.held) state.move(fx, fy, t)
        }
        wait(hold)
        down = false
        if (holding && state.held) {
            val p = at - (feed() ?: origin)
            state.up(p.x, p.y, withFrameNanos { it } / 1_000_000)
        }
        holding = false
    }

    private suspend fun hide() {
        down = false
        val from = shown
        animate(FadeMs) { p, _ -> shown = from * (1 - p) }
    }

    /**
     * Lets go and vanishes at once, when a finger or the script's end takes over: a pull the hand
     * still holds goes home without fetching, as the prototype's `stopDemo()`.
     */
    fun stop() {
        if (holding && state.held) state.cancel()
        holding = false
        down = false
        shown = 0f
    }

    private suspend fun wait(ms: Int) = animate(ms) { _, _ -> }

    /** Runs [frame] each frame for [ms] with the progress and the frame's time in ms. */
    private suspend fun animate(ms: Int, frame: (Float, Long) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            var now = 0L
            withFrameNanos { t ->
                p = ((t - start) / 1e6f / ms).coerceIn(0f, 1f)
                now = t / 1_000_000
            }
            frame(p, now)
        }
    }

    private companion object {
        const val FadeMs = 200
        const val PressMs = 120
        const val LiftMs = 380
    }
}
