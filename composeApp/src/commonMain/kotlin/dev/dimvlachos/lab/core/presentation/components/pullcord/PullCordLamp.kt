package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.pullcord_cord
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

/**
 * A pendant lamp hanging over a screen, [across] its width, with a pull cord: pull the bead down
 * and it clicks, switching the lamp and the screen's look; tug it sideways and it only swings. Let
 * go, the cord springs back up and sways, and the shade with it. [content] is the screen in one
 * look or the other, lit or not; as the lamp switches, the new look spreads out from the bulb over
 * the old, and lit, the lamp throws a cone of warm light down over it.
 *
 * While the new look spreads, [content] is composed twice, once in each look, so keep its state
 * outside it. [onSwitch] hears each switch, after a tick of haptics. Nothing recomposes while the
 * cord swings or the light spreads: only a switch does. The lamp fills the space it is given.
 */
@Composable
public fun PullCordLamp(
    state: PullCordState,
    modifier: Modifier = Modifier,
    across: Float = 0.5f,
    onSwitch: (lit: Boolean) -> Unit = {},
    content: @Composable (lit: Boolean) -> Unit,
) {
    ReportComposition()
    val haptics = LocalHapticFeedback.current
    val currentOnSwitch by rememberUpdatedState(onSwitch)
    DisposableEffect(state, haptics) {
        state.onSwitch = { lit ->
            // A key's click: a light impact on iOS, and a click on every Android this runs on.
            // The toggle haptics would say on or off, but Android only plays them from 14 on.
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            currentOnSwitch(lit)
        }
        onDispose { state.onSwitch = null }
    }
    // The cord and the shade step on a frame at a time while they move or are held, and the loop
    // sleeps once they hang still.
    LaunchedEffect(state) {
        while (true) {
            snapshotFlow { state.awake }.first { it }
            var last = withFrameNanos { it }
            while (state.awake) {
                withFrameNanos { now ->
                    val seconds = (now - last) / 1_000_000_000f
                    state.advance(seconds.coerceIn(0f, PullCordDimens.MostFrameSeconds))
                    last = now
                }
            }
        }
    }
    SideEffect { state.hangAcross(across) }
    val painter = remember { LampPainter() }
    val colors = LabTheme.colors
    val density = LocalDensity.current.density
    val label = stringResource(Res.string.pullcord_cord)
    val lit = state.lit
    // While a look spreads, the one under it shows round its circle; once it has spread, the
    // look the lamp is on shows alone.
    val revealing = state.revealing
    val top = state.revealTop
    Box(
        modifier
            .onSizeChanged { state.place(it.width, density) }
            .pointerInput(state) {
                awaitEachGesture {
                    // Seen before the screen sees it: a finger on the bead is the cord's, any other
                    // is left to the screen. Every finger that comes down is looked at, so a thumb
                    // resting on the screen doesn't keep the bead from being taken.
                    var down: PointerInputChange? = null
                    while (down == null) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        down =
                            event.changes.firstOrNull {
                                it.changedToDown() && state.grab(it.position)
                            }
                        if (down == null && event.changes.none { it.pressed })
                            return@awaitEachGesture
                    }
                    down.consume()
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            // Where the finger lifts counts too: a flick's last move can stop
                            // short of the click.
                            state.dragTo(change.position)
                            if (!change.pressed) break
                        }
                    } finally {
                        state.release()
                    }
                }
            }
    ) {
        val looks = if (revealing) listOf(!top, top) else listOf(lit)
        for (look in looks) {
            // Keyed by its part, not its look: the screen the lamp is on keeps its place, and its
            // focus, as it changes look; the one going is only ever a picture of the other.
            key(if (look == lit) LiveLook else GoingLook) {
                Box(
                    Modifier.fillMaxSize()
                        .then(if (look == lit) Modifier else Modifier.clearAndSetSemantics {})
                        .drawWithContent {
                            val draw: () -> Unit = {
                                drawContent()
                                if (look) with(painter) { drawLight(state, colors) }
                            }
                            if (state.revealing && look == state.revealTop) {
                                with(painter) { clipToReveal(state) { draw() } }
                            } else {
                                draw()
                            }
                        }
                ) {
                    // In a layer of its own, so a frame of the cord or the light redraws the
                    // screen's recorded picture rather than drawing it all again.
                    Box(Modifier.fillMaxSize().graphicsLayer()) { content(look) }
                }
            }
        }
        // The lamp over everything, in a layer of its own, so its frames redraw it alone.
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawBehind {
                with(painter) { drawLamp(state, colors) }
            }
        )
        // The bead, for a screen reader and for tests: a switch that follows the bead.
        Spacer(
            Modifier.offset {
                    val at = state.bead
                    val half = PullCordDimens.GrabRadius.toPx()
                    IntOffset((at.x - half).roundToInt(), (at.y - half).roundToInt())
                }
                .size(PullCordDimens.GrabRadius * 2)
                .semantics {
                    contentDescription = label
                    role = Role.Switch
                    toggleableState = ToggleableState(lit)
                    onClick {
                        state.toggle()
                        true
                    }
                }
        )
    }
}

private const val LiveLook = "live"
private const val GoingLook = "going"
