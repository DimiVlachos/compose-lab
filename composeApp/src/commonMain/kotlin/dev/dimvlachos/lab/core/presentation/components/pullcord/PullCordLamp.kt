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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.pullcord_cord
import dev.dimvlachos.lab.resources.pullcord_light_off
import dev.dimvlachos.lab.resources.pullcord_light_on
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
 * [across] is where the lamp hangs, as a share of its width, 0 at the left edge to 1 at the right,
 * whatever the layout direction.
 *
 * [content] must paint an opaque background over all of it: the new look is drawn over the old, and
 * wherever it is see-through, the old look shows through it. While the new look spreads, [content]
 * is composed twice, once in each look, so keep its state outside it; only the look the lamp is on
 * takes touches and is read by a screen reader. [onSwitch] hears each switch, after a tick of
 * haptics. Nothing recomposes while the cord swings or the light spreads: only a switch, and the
 * end of its spread, recomposes. The lamp fills the space it is given.
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
        val hook: (Boolean) -> Unit = { lit ->
            // A key's click: a light impact on iOS, and a click on every Android this runs on.
            // The toggle haptics would say on or off, but Android only plays them from 14 on.
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            currentOnSwitch(lit)
        }
        state.onSwitch = hook
        // Only this lamp's own hook: another lamp may have hung its own on the state since.
        onDispose { if (state.onSwitch === hook) state.onSwitch = null }
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
    val on = stringResource(Res.string.pullcord_light_on)
    val off = stringResource(Res.string.pullcord_light_off)
    val lit = state.lit
    // While a look spreads, the one under it shows round its circle; once it has spread, the
    // look the lamp is on shows alone.
    val revealing = state.revealing
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
        // The look going under the live one, which is always on top, so it alone is hit by a
        // touch. When the going look should show over it, inside its circle as it shrinks back
        // into the bulb, the live look is clipped to outside that circle instead.
        val looks = if (revealing) listOf(!lit, lit) else listOf(lit)
        for (look in looks) {
            val live = look == lit
            // Keyed by its part, not its look: the screen the lamp is on keeps its place, and its
            // focus, as it changes look; the one going is only ever a picture of the other.
            key(if (live) LiveLook else GoingLook) {
                Box(
                    Modifier.fillMaxSize()
                        .then(if (live) Modifier else Modifier.clearAndSetSemantics {}.inert())
                        .drawWithContent {
                            if (live && state.revealing) {
                                val inside = state.revealTop == look
                                with(painter) {
                                    clipToReveal(state, inside) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                            } else {
                                drawContent()
                            }
                        }
                ) {
                    // In a layer of its own, so a frame of the cord or the light redraws the
                    // screen's recorded picture rather than drawing it all again.
                    Box(Modifier.fillMaxSize().graphicsLayer()) { content(look) }
                    if (look) {
                        // The light over the lit look, inside it so it is clipped with it. Its
                        // brightness is the layer's alpha, so a flicker only changes the layer;
                        // the cone is drawn again only as the lamp swings.
                        Spacer(
                            Modifier.fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    blendMode = BlendMode.Plus
                                    alpha = state.glow
                                }
                                .drawBehind { with(painter) { drawLight(state, colors) } }
                        )
                    }
                }
            }
        }
        // The lamp over everything, in a layer of its own, so its frames redraw it alone.
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawBehind {
                with(painter) { drawLamp(state, colors) }
            }
        )
        // The bead, for a screen reader and for tests: a switch where the bead hangs at rest. It
        // stays there as the cord swings, so it isn't placed again every frame, and a screen
        // reader's focus doesn't chase the bead about.
        Spacer(
            Modifier.offset {
                    val at = state.restingBead
                    val half = PullCordDimens.GrabRadius.toPx()
                    IntOffset((at.x - half).roundToInt(), (at.y - half).roundToInt())
                }
                .size(PullCordDimens.GrabRadius * 2)
                .semantics {
                    contentDescription = label
                    role = Role.Switch
                    toggleableState = ToggleableState(lit)
                    stateDescription = if (lit) on else off
                    onClick {
                        state.toggle()
                        true
                    }
                }
        )
    }
}

// Keeps any touch from what is under it: the going look is only ever a picture.
private fun Modifier.inert(): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }

private const val LiveLook = "live"
private const val GoingLook = "going"
