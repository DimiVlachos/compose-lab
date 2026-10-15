package dev.dimvlachos.lab.core.presentation.components.smoke

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlinx.coroutines.flow.first

/**
 * A list whose rows go up in smoke. Put [SmokeRow]s in [content], a scrolling column say, with the
 * same [state]: swipe a row left and it burns from its right edge as the finger goes; once a third
 * of it has burnt, let go and it burns the rest of the way into fine smoke that curls up over the
 * list, still carrying the shapes of its words a moment, while the list closes under it; let go
 * short of that and the smoke is drawn back in. A row can go to the right too, in
 * [SmokeMaterial.Mist], cooler and drifting aside, for an archive say. Undo ([SmokeState.reform])
 * breathes the smoke back into its row.
 *
 * The smoke is a few thousand particles a row, each where a pixel of the row was: its path is a
 * pure function of where it started and how old it is, so nothing is simulated from frame to frame,
 * and it is drawn over [content] in a few batched calls of points a frame, in layers blurred more
 * the older the smoke. The frame loop runs only while something moves or smoke is in the air, and
 * sleeps once it has gone.
 *
 * A tick of haptics marks a swipe burning past the point where it commits, and a soft click the
 * commit itself, as the finger lets go. The burn loop stops when the app goes to the background.
 */
@Composable
public fun SmokeOverlay(
    state: SmokeState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    ReportComposition()
    val haptics = LocalHapticFeedback.current
    DisposableEffect(state, haptics) {
        state.onThreshold = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
        state.onCommitted = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick) }
        onDispose {
            state.onThreshold = {}
            state.onCommitted = {}
            state.clear()
        }
    }
    // In the background the burn loop stops: a sound shouldn't outlive the screen it belongs to.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.quiet() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val graphics = LocalGraphicsContext.current
    SideEffect {
        state.scope = scope
        state.density = density
        state.graphics = graphics
    }
    LaunchedEffect(state) { state.fillOffMainThread() }
    LaunchedEffect(state) {
        while (true) {
            snapshotFlow { state.awake }.first { it }
            var last = withFrameNanos { it }
            while (state.awake) {
                withFrameNanos { now ->
                    state.advance((now - last) / 1_000_000_000f)
                    last = now
                }
            }
        }
    }
    val colors = LabTheme.colors
    val smokeColors =
        remember(colors) {
            SmokeColors(
                smoke = colors.smoke,
                mist = colors.mist,
                ember = colors.ember,
                spark = colors.emberSpark,
                glowAdds = colors.isDark,
            )
        }
    Box(
        modifier
            .onPlaced { state.overlay = it }
            // Delete let up on any row, or focus gone from the list, ends a held Delete: the next
            // press burns again.
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyUp && isDelete(it.key)) state.deleteHeld = false
                false
            }
            .onFocusChanged { if (!it.hasFocus) state.deleteHeld = false }
    ) {
        content()
        SmokeLayerOf(state, SmokeLayer.Haze, smokeColors, blur = SmokeDimens.HazeBlur)
        SmokeLayerOf(state, SmokeLayer.Soft, smokeColors, blur = SmokeDimens.SoftBlur)
        SmokeLayerOf(state, SmokeLayer.Crisp, smokeColors, blur = 0.dp)
        SmokeLayerOf(state, SmokeLayer.Glow, smokeColors, blur = 0.dp)
    }
}

// One layer of the smoke, over the whole overlay, in a graphics layer of its own blurred by
// [blur]: only it is drawn again each frame, and nothing under it.
@Composable
private fun BoxScope.SmokeLayerOf(
    state: SmokeState,
    layer: SmokeLayer,
    colors: SmokeColors,
    blur: Dp,
) {
    Spacer(
        Modifier.matchParentSize()
            .graphicsLayer {
                if (blur > 0.dp) {
                    val radius = blur.toPx()
                    renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                }
            }
            .drawBehind {
                state.frame
                if (state.drawn.isEmpty()) return@drawBehind
                with(state.painter) { draw(state.drawn, layer, colors) }
            }
    )
}
