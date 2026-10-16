package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import dev.dimvlachos.lab.core.platform.ShakeImpulses
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer

/** The sphere's radius and centre as fractions of the globe's width. */
private const val RadiusOfWidth = 0.42f
private const val CenterYOfWidth = 0.46f
private const val RadiansToDegrees = 57.3f

/**
 * A glass snow globe holding [scene]. Drag it about, shake the phone, or press it, and the water
 * swirls the glitter up: [onShaken] once the shake is hard enough, [onSettled] once the glitter has
 * calmed and no refresh is [refreshPending]. A change of [scene] crossfades inside the glass. With
 * [reducedMotion] a shake still refreshes but the glitter stays down.
 */
@Composable
internal fun SnowGlobe(
    scene: GlobeScene,
    refreshPending: Boolean,
    onShaken: () -> Unit,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier,
    state: SnowGlobeState = rememberSnowGlobeState(),
    sounds: SoundPlayer = SilentSoundPlayer,
    shakeSensorEnabled: Boolean = true,
    description: String = "",
    reducedMotion: Boolean = false,
) {
    ReportSnowGlobeComposition()
    val pending by rememberUpdatedState(refreshPending)
    val shaken by rememberUpdatedState(onShaken)
    val settled by rememberUpdatedState(onSettled)

    SideEffect { state.reducedMotion = reducedMotion }
    LaunchedEffect(state, scene) { state.showScene(scene) }
    LaunchedEffect(state, sounds) {
        state.onSwish = { volume -> sounds.play(Sound.GlobeSwish, volume) }
        try {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't blow the spring up.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                    last = now
                    state.step(dt, pending, { shaken() }, { settled() })
                }
            }
        } finally {
            state.onSwish = null
        }
    }
    ShakeImpulses(shakeSensorEnabled) { state.nudge(it) }

    var size by remember { mutableStateOf(IntSize.Zero) }
    val radius = size.width * RadiusOfWidth
    val center = Offset(size.width / 2f, size.width * CenterYOfWidth)
    val glass =
        remember(size) { if (size == IntSize.Zero) null else glassRenderEffect(center, radius) }

    Box(
        modifier
            .aspectRatio(0.86f)
            .onSizeChanged { size = it }
            .testTag("snowglobe")
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick {
                    state.shake()
                    true
                }
            }
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.Spacebar)) {
                    state.shake()
                    true
                } else false
            }
            .focusable()
            .pointerInput(state) {
                detectDragGestures(
                    onDragEnd = { state.release() },
                    onDragCancel = { state.release() },
                ) { change, drag ->
                    change.consume()
                    // The pointer scope's own size, so a resize never leaves a stale radius here.
                    val r = (size.width * RadiusOfWidth).coerceAtLeast(1f)
                    state.drag(drag / r)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            drawTable(center, radius)
        }
        Spacer(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    state.frame
                    translationX = state.offset.x * radius * 0.12f
                    translationY = state.offset.y * radius * 0.06f
                    rotationZ = state.tilt * RadiansToDegrees
                    transformOrigin = TransformOrigin(0.5f, 0.9f)
                    renderEffect = glass
                }
                .drawBehind {
                    state.frame
                    drawInterior(state, center, radius)
                    if (glass == null) drawGlassFallback(center, radius)
                }
        )
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            drawGlassHighlights(center, radius, state.tilt)
            drawBase(center, radius)
        }
    }
}
