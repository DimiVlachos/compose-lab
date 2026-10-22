package dev.dimvlachos.lab.core.presentation.components.keyhook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.archivo_extrabold
import dev.dimvlachos.lab.resources.jetbrains_mono_semibold
import kotlin.random.Random
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.Font

/** The clips the key rack plays. */
internal val KeyRackSounds = setOf(Sound.KeyTink, Sound.KeyClack, Sound.KeyLift)

// The tink's four pitches, as the prototype picks them, over the clip's own 3150 Hz.
private val TinkRates = floatArrayOf(1f, 3720f / 3150f, 4380f / 3150f, 5050f / 3150f)

/**
 * A walnut key rack by the door that is an account switcher, as the prototype draws it: whoever is
 * signed in has their keys clipped to the D-ring under the account card at the top of the space;
 * everyone else's hang on their own hooks. Drag a set down onto the rack to sign out, or carry one
 * up to the card to sign in; a tap toggles a set. It fills its space, which should be the screen's
 * hero, with the account card ([RackGeometry.CardBottom] dp tall) drawn under it at its top.
 *
 * [labels] names each set's button for screen readers and keys, by [KeyRackState.rings]' order. A
 * real finger calls [onTouch], so a script can let go.
 */
@Composable
internal fun KeyRack(
    state: KeyRackState,
    labels: List<String>,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
    onTouch: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, haptics) {
        val listener: (KeyRackEvent, Float) -> Unit = { event, k ->
            when (event) {
                KeyRackEvent.Tink -> {
                    val rate = TinkRates[Random.nextInt(4)] * (.97f + Random.nextFloat() * .06f)
                    sounds.play(Sound.KeyTink, k, rate)
                }
                KeyRackEvent.Clack -> {
                    sounds.play(Sound.KeyClack, k)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                KeyRackEvent.Lift -> {
                    sounds.play(Sound.KeyLift, .8f)
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                }
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
                // At rest the loop sleeps, until a touch or a script sets the keys moving again.
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
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val archivo = Font(Res.font.archivo_extrabold, FontWeight.ExtraBold)
    val mono = Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold)
    val fonts =
        remember(archivo, mono) {
            mapOf(TagFont to FontFamily(archivo), PlateFont to FontFamily(mono))
        }
    val density = LocalDensity.current.density
    BoxWithConstraints(modifier) {
        val geo =
            remember(maxWidth, maxHeight) {
                RackGeometry(maxWidth.value, maxHeight.value, state.holders.size)
            }
        val wood =
            remember(geo, density) { bakeWood(geo.boardR - geo.boardL, 66f * geo.u, density) }
        // The sets' buttons, where each hangs, under the canvas: touches go to the rack, and the
        // buttons are for screen readers and keys, as the prototype's.
        val carried = state.carriedIndex
        for (r in state.rings) {
            val home = carried == r.index
            val x = if (home) geo.carryX else geo.hookX(r.index)
            val y = if (home) geo.carryY else geo.hookY
            val uk = geo.u * geo.ks
            Box(
                Modifier.offset((x - 22f * uk).dp, (y - 6f * geo.u).dp)
                    .size((44f * uk).dp, (72f * uk).dp)
                    .testTag("keys-${r.index}")
                    .semantics { contentDescription = labels[r.index] }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                    ) {
                        onTouch()
                        state.toggle(r)
                    },
            )
        }
        Canvas(
            Modifier.fillMaxSize()
                .onSizeChanged { state.layout(it.width / density, it.height / density) }
                .pointerInput(state) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        onTouch()
                        // A finger on the rack takes over from the script, which lets go first.
                        if (state.grabIsScripted) state.letGo()
                        if (state.grabbed != null) return@awaitEachGesture
                        val p = down.position / density
                        val r = state.hitTest(p.x, p.y) ?: return@awaitEachGesture
                        down.consume()
                        state.startGrab(r, p.x, p.y)
                        var at = p
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                at = change.position / density
                                state.moveGrab(at.x, at.y)
                                change.consume()
                                if (change.changedToUpIgnoreConsumed()) break
                            }
                        } finally {
                            if (state.grabbed === r && !state.grabIsScripted) {
                                state.endGrab(at.x, at.y)
                            }
                        }
                    }
                },
        ) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.textMeasurer = measurer
                c.fonts = fonts
                c.scale(density, density)
                c.drawKeyRack(state, dark, density, wood)
            }
        }
    }
}
