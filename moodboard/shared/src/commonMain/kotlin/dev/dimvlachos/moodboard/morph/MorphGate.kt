package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.flow.first

// Outlasts Android's 400 ms parallax push (no morph, so it waits out this timeout), so a tap
// right after opening a board can't cut the slide off mid-flight.
private const val MorphStartTimeoutMs = 420L

// Twice the longest morph (the 500 ms open): only a transition that never ends reaches it.
private const val MorphSettleTimeoutMs = 1_000L

/**
 * Runs morph requests (open, close, tab switch) one at a time. A request that arrives while a morph
 * is under way waits and runs the moment it lands, instead of reversing it mid-flight: an
 * interrupted shared transition can leave an extra entry for its key behind, and every later morph
 * of that element then draws a stray copy. The gate closes on the request itself, not on
 * isTransitionActive, which only turns true a frame or two later, once the target has composed
 * (Builder Brigade's morph hold). A request that starts no morph releases it after the start
 * timeout. Waiting requests run in arrival order, so two quick backs pop twice.
 */
class MorphGate
internal constructor(
    internal val holding: MutableState<Boolean>,
    internal val pending: ArrayDeque<() -> Unit>,
) {
    /** True while a morph (or a push that starts none) is running and requests are queued. */
    val isHolding: Boolean
        get() = holding.value

    fun run(request: () -> Unit) {
        if (holding.value) {
            pending.addLast(request)
        } else {
            holding.value = true
            request()
        }
    }

    /**
     * Drops waiting requests. Called when the screen changes outside the gate (Search clearing its
     * results on Back), so a push queued for the old content can't land on the new one.
     */
    fun cancelPending() = pending.clear()
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.rememberMorphGate(): MorphGate {
    val gate = remember(this) { MorphGate(mutableStateOf(false), ArrayDeque()) }
    val holding = gate.holding
    LaunchedEffect(gate) {
        // A restarted effect must not inherit a gate left shut by the one it replaced.
        holding.value = false
        gate.pending.clear()
        while (true) {
            snapshotFlow { holding.value }.first { it }
            // Timed in frames, the clock the morph itself runs on.
            val requestedAt = withFrameMillis { it }
            while (!isTransitionActive) {
                if (withFrameMillis { it } - requestedAt >= MorphStartTimeoutMs) break
            }
            // Bounded: a shared transition left active (an interrupted one can) must not hold every
            // later tap, tab switch and Back forever.
            if (isTransitionActive) {
                val settleStartedAt = withFrameMillis { it }
                while (isTransitionActive) {
                    if (withFrameMillis { it } - settleStartedAt >= MorphSettleTimeoutMs) break
                }
            }
            // A waiting request starts the next morph and keeps the gate closed for it.
            val next = gate.pending.removeFirstOrNull()
            if (next == null) holding.value = false else next()
        }
    }
    return gate
}
