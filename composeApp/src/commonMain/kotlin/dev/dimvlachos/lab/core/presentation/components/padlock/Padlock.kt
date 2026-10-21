package dev.dimvlachos.lab.core.presentation.components.padlock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.random.Random
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first

/** The clips the padlock plays. */
internal val PadlockSounds =
    setOf(Sound.PadlockClunk, Sound.PadlockPop, Sound.PadlockRattle, Sound.PadlockSwing)

/**
 * A brass padlock that is a password's strength meter and its show/hide switch, as the prototype
 * draws it: the shackle lowers and turns to steel as [state]'s level rises and drops home with a
 * clunk at strong; tapping the lock ([onToggle]) swings the keyhole cover aside to show the
 * password. It fills its space and draws itself centred in it. [label] names the switch for screen
 * readers.
 */
@Composable
internal fun Padlock(
    state: PadlockState,
    onToggle: (Boolean) -> Unit,
    label: String,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, haptics) {
        val listener: (PadlockEvent, Float) -> Unit = { event, strength ->
            when (event) {
                PadlockEvent.Clunk -> {
                    sounds.play(Sound.PadlockClunk, .6f + .4f * strength)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                PadlockEvent.Pop -> {
                    sounds.play(Sound.PadlockPop)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                PadlockEvent.Rattle ->
                    sounds.play(Sound.PadlockRattle, .8f, .9f + Random.nextFloat() * .22f)
                PadlockEvent.Swing -> sounds.play(Sound.PadlockSwing, .7f)
                PadlockEvent.Notch -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
        state.onEvent = listener
        try {
            awaitCancellation()
        } finally {
            if (state.onEvent === listener) state.onEvent = null
        }
    }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                // At rest the loop sleeps, until typing or a tap sets the springs moving again.
                if (state.settled) {
                    val seen = state.wake
                    snapshotFlow { state.wake }.first { it != seen }
                    last = 0L
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't blow the springs up.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.step(dt)
                }
            }
        }
    }
    val dark = LabTheme.isDark
    val grain = remember { padlockGrain() }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val box = PadlockBox(maxWidth.value, maxHeight.value)
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.textMeasurer = measurer
                c.fonts = Mono
                c.scale(density, density)
                c.drawPadlock(
                    state,
                    size.width / density,
                    size.height / density,
                    dark,
                    grain,
                    density / PrototypePixelRatio,
                )
            }
        }
        Box(
            Modifier.offset(box.hitLeft.dp, box.hitTop.dp)
                .size(box.hitWidth.dp, box.hitHeight.dp)
                .testTag("padlock")
                .semantics { contentDescription = label }
                .toggleable(
                    value = state.peek,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Switch,
                    onValueChange = onToggle,
                ),
        )
    }
}

/** The family the stamp's `mono` font names. */
private val Mono = mapOf("mono" to FontFamily.Monospace)

/** The browser's pixel ratio the prototype was tuned at, which its shadow sizes are given in. */
private const val PrototypePixelRatio = 2f
