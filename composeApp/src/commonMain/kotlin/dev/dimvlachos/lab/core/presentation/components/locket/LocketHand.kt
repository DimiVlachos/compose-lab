package dev.dimvlachos.lab.core.presentation.components.locket

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import kotlin.math.pow
import kotlinx.coroutines.delay

/**
 * A scripted fingertip on the locket, as the prototype's demo finger: it glides to a point, presses
 * and lets go through the same [LocketState.down], [LocketState.move] and [LocketState.up] a finger
 * works. Points are in the locket's box units. A script cut short lets go of what it held.
 */
@Stable
internal class LocketHand(private val state: LocketState) {
    var x by mutableFloatStateOf(250f)
        private set

    var y by mutableFloatStateOf(250f)
        private set

    /** How far the fingertip shows: .6 lifted, 1 pressed. */
    var alpha by mutableFloatStateOf(0f)
        private set

    private var pressed = false

    // Set once a real finger or a stopped script takes over: the hand presses nothing more until
    // it next appears, so a frame left in a cancelled script can't grab the user's gesture.
    private var stopped = false

    /** Shows the fingertip, lifted, at ([x], [y]). */
    suspend fun appear(x: Float, y: Float) {
        stopped = false
        this.x = x
        this.y = y
        animate(ScriptedTouch.UpMs.toFloat()) { alpha = it * .6f }
    }

    /** Glides to ([tx], [ty]) over [ms], dragging what it holds. */
    suspend fun glide(tx: Float, ty: Float, ms: Float) {
        val x0 = x
        val y0 = y
        animate(ms) { p ->
            val e = ease(p)
            x = x0 + (tx - x0) * e
            y = y0 + (ty - y0) * e
            if (pressed) state.move(x, y)
        }
    }

    private suspend fun press() {
        if (stopped) return
        pressed = true
        state.down(x, y)
        animate(ScriptedTouch.DownMs.toFloat()) { alpha = .6f + .4f * it }
    }

    private suspend fun release() {
        if (!pressed) return
        pressed = false
        state.up(x, y)
        animate(ScriptedTouch.DownMs.toFloat()) { alpha = 1f - .4f * it }
    }

    /** The prototype's `tapAt`: glides there in 480 ms, presses for 110 ms and lets go. */
    suspend fun tapAt(tx: Float, ty: Float) = guarded {
        glide(tx, ty, 480f)
        press()
        delay(ScriptedTouch.TapHoldMs)
        release()
    }

    /**
     * The prototype's `dragAlong`: glides to the first point in 560 ms, presses, waits 160 ms,
     * drags through the rest at [ms] a leg, waits 120 ms and lets go.
     */
    suspend fun dragAlong(points: List<Pair<Float, Float>>, ms: Float) = guarded {
        glide(points[0].first, points[0].second, 560f)
        press()
        delay(160)
        for (i in 1 until points.size) glide(points[i].first, points[i].second, ms)
        delay(120)
        release()
    }

    /** Fades the fingertip out. */
    suspend fun hide() {
        val a0 = alpha
        animate(ScriptedTouch.UpMs.toFloat()) { alpha = a0 * (1f - it) }
    }

    /** Lets go at once, when a script is stopped or a real finger takes over. */
    fun stop() {
        stopped = true
        if (pressed) state.cancel()
        pressed = false
        alpha = 0f
    }

    private suspend inline fun guarded(block: () -> Unit) {
        var done = false
        try {
            block()
            done = true
        } finally {
            if (!done) stop()
        }
    }

    private suspend fun animate(ms: Float, frame: (Float) -> Unit) {
        val start = withFrameNanos { it }
        var p = 0f
        while (p < 1f) {
            withFrameNanos { now -> p = ((now - start) / 1e6f / ms).coerceIn(0f, 1f) }
            frame(p)
        }
    }

    private fun ease(t: Float) = if (t < .5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f
}
