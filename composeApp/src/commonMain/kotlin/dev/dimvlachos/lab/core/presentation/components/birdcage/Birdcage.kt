package dev.dimvlachos.lab.core.presentation.components.birdcage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.playVaried
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.Pattern2D
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlinx.coroutines.awaitCancellation

/** The clips the birdcage plays. */
internal val BirdcageSounds =
    setOf(
        Sound.BirdcageChirp,
        Sound.BirdcageSettle,
        Sound.BirdcageLift,
        Sound.BirdcageRustle,
        Sound.BirdcageSwish,
    )

/**
 * A chat's mute switch as a little brass birdcage with a canary in it, as the prototype draws it.
 * Each message [BirdcageState.deliver]s makes the canary hop and sing; pulling the linen cover down
 * over the cage (a drag anywhere in this space; the hem follows the finger) mutes the chat and the
 * canary tucks its head and sleeps, and pushing it up wakes it. A tap toggles it. It fills its
 * space and stands on its bottom edge. [label] names the switch for screen readers.
 */
@Composable
internal fun Birdcage(
    state: BirdcageState,
    label: String,
    sounds: SoundPlayer,
    focusRing: Color,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, haptics) {
        val listener: (BirdcageEvent) -> Unit = { event ->
            when (event) {
                BirdcageEvent.Settle -> {
                    sounds.playVaried(Sound.BirdcageSettle)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                BirdcageEvent.Lift -> {
                    sounds.playVaried(Sound.BirdcageLift)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                BirdcageEvent.Chirp -> sounds.playVaried(Sound.BirdcageChirp)
                BirdcageEvent.Rustle -> sounds.playVaried(Sound.BirdcageRustle)
                BirdcageEvent.Swish -> sounds.playVaried(Sound.BirdcageSwish)
            }
        }
        state.onEvent = listener
        try {
            awaitCancellation()
        } finally {
            if (state.onEvent === listener) state.onEvent = null
        }
    }
    // The canary breathes and the cloth ripples all the time, as in the prototype, so the cage
    // steps every frame while the screen is in front.
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
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
    val weave = remember { Pattern2D(birdcageWeave()) }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(
        modifier.pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val t0 = down.uptimeMillis
                val pxPerUnit =
                    BirdcageBox(size.width / density, size.height / density).scale * density
                val slop = Slop.toPx()
                var released = false
                var at = 0f
                val id = state.down(down.position.y, 0f)
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        at = (change.uptimeMillis - t0).toFloat()
                        if (change.changedToUpIgnoreConsumed()) {
                            released = true
                            state.up(at)
                            break
                        }
                        state.move(change.position.y, at, pxPerUnit, slop)
                        if (state.dragging) change.consume()
                    }
                } finally {
                    // Taken away mid-gesture: the cover is let go where it is, without a fling.
                    if (!released && state.holds(id)) state.cancel(at)
                }
            }
        },
    ) {
        val box = BirdcageBox(maxWidth.value, maxHeight.value)
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.textMeasurer = measurer
                c.scale(density, density)
                c.drawBirdcage(
                    state,
                    size.width / density,
                    size.height / density,
                    dark,
                    weave,
                    density / PrototypePixelRatio,
                )
            }
        }
        // The switch, round the cage: it takes focus and Space or Enter, and a screen reader's
        // tap. A finger's tap goes to the cage under it, as in the prototype.
        var focused by remember { mutableStateOf(false) }
        Box(
            Modifier.offset(box.hitLeft.dp, box.hitTop.dp)
                .size(box.hitWidth.dp, box.hitHeight.dp)
                .drawBehind {
                    // The prototype's focus outline: 2 dp of the accent, 4 dp out, rounded 22 dp.
                    if (!focused) return@drawBehind
                    val w = 2.dp.toPx()
                    val out = 4.dp.toPx() + w / 2f
                    drawRoundRect(
                        focusRing,
                        Offset(-out, -out),
                        Size(size.width + out * 2f, size.height + out * 2f),
                        CornerRadius(22.dp.toPx() + out),
                        style = Stroke(w),
                    )
                }
                .onFocusChanged { focused = it.isFocused }
                .testTag("birdcage")
                .semantics {
                    role = Role.Switch
                    toggleableState = ToggleableState(state.muted)
                    contentDescription = label
                    onClick {
                        state.toggle()
                        true
                    }
                }
                .onKeyEvent {
                    val press = it.type == KeyEventType.KeyDown
                    if (press && (it.key == Key.Spacebar || it.key == Key.Enter)) {
                        state.toggle()
                        true
                    } else {
                        false
                    }
                }
                .focusable(),
        )
    }
}

/** How far a finger moves before it takes the hem: the prototype's 6 px. */
private val Slop = 6.dp

/** The browser's pixel ratio the prototype was tuned at, which its shadow sizes are given in. */
private const val PrototypePixelRatio = 2f
