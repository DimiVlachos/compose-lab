package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.flow.first

private const val MorphStartTimeoutMs = 300L

/**
 * Runs morph requests (open, close) one at a time. A request that arrives while a morph is under
 * way waits and runs the moment it lands, instead of reversing it mid-flight: an interrupted shared
 * transition can leave an extra entry for its key behind, and every later morph of that element
 * then draws a stray copy. The gate closes on the request itself, not on isTransitionActive, which
 * only turns true a frame or two later, once the target has composed.
 * A request that starts no morph releases it after the start timeout.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.rememberMorphGate(): (() -> Unit) -> Unit {
    val holding = remember { mutableStateOf(false) }
    val pending = remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(this) {
        while (true) {
            snapshotFlow { holding.value }.first { it }
            // Timed in frames, the clock the morph itself runs on.
            val requestedAt = withFrameMillis { it }
            while (!isTransitionActive) {
                if (withFrameMillis { it } - requestedAt >= MorphStartTimeoutMs) break
            }
            snapshotFlow { isTransitionActive }.first { !it }
            val next = pending.value
            pending.value = null
            // A waiting request starts the next morph and keeps the gate closed for it.
            if (next == null) holding.value = false else next()
        }
    }
    return remember(this) {
        { request ->
            if (holding.value) {
                pending.value = request
            } else {
                holding.value = true
                request()
            }
        }
    }
}
