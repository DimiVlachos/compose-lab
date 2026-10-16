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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntSize
import dev.dimvlachos.lab.core.platform.ShakeImpulses
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer

/** The sphere's radius and centre as fractions of the globe's width. */
private const val RadiusOfWidth = 0.42f
private const val CenterYOfWidth = 0.46f
/**
 * How far the globe moves for the glass's spring [SnowGlobeState.offset], in radii per globe unit,
 * and how far it leans, in degrees per radian of [SnowGlobeState.tilt]: at the peak of a hard shake
 * (offset about 0.5, tilt at its 0.12 limit) about 0.05 r and 3°, a carried globe, not a sliding
 * lens.
 */
private const val ShiftOfOffset = 0.1f
private const val TiltDegreesOfRadians = 25f
/** The bottom of the walnut base, as a fraction of the globe's height: what it rocks on. */
private const val BaseBottomOfHeight = 0.865f

/**
 * A glass snow globe holding [scene]. Drag it about, shake the phone, or press it, and the water
 * swirls the glitter up: [onShaken] once the shake is hard enough, [onSettled] once the glitter has
 * calmed and no refresh is [refreshPending]. A change of [scene] crossfades inside the glass. With
 * [reducedMotion] a shake still refreshes but the glitter stays down. [stateDescription], the
 * conditions it shows, is read out after its [description].
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
    stateDescription: String = "",
) {
    ReportSnowGlobeComposition()
    val pending by rememberUpdatedState(refreshPending)
    val shaken by rememberUpdatedState(onShaken)
    val settled by rememberUpdatedState(onSettled)

    SideEffect { state.reducedMotion = reducedMotion }
    LaunchedEffect(state, scene) { state.showScene(scene) }
    LaunchedEffect(state, sounds) {
        val swish: (Float) -> Unit = { volume -> sounds.play(Sound.GlobeSwish, volume) }
        state.onSwish = swish
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
            // Leaving the screen lets go of the glass, so a hoisted state isn't left mid-drag.
            state.release()
            if (state.onSwish === swish) state.onSwish = null
        }
    }
    ShakeImpulses(shakeSensorEnabled) { state.nudge(it) }

    var globeSize by remember { mutableStateOf(IntSize.Zero) }
    val radius = globeSize.width * RadiusOfWidth
    val center = Offset(globeSize.width / 2f, globeSize.width * CenterYOfWidth)
    val glass =
        remember(globeSize) {
            if (globeSize == IntSize.Zero) null else glassRenderEffect(center, radius)
        }

    Box(
        modifier
            .aspectRatio(0.86f)
            .onSizeChanged { globeSize = it }
            .testTag("snowglobe")
            .semantics {
                role = Role.Button
                contentDescription = description
                if (stateDescription.isNotEmpty()) this.stateDescription = stateDescription
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
                    // PointerInputScope.size, current on every drag, unlike a radius captured when
                    // this block started (pointerInput is keyed on the state, not the size).
                    val r = (size.width * RadiusOfWidth).coerceAtLeast(1f)
                    state.drag(drag / r)
                }
            }
    ) {
        // The table doesn't move, so it redraws only when the globe is resized.
        Canvas(Modifier.fillMaxSize()) { drawTable(center, radius) }
        // The globe moves as one object, rocking on its base like a globe that is carried: the
        // water, the glass's highlights and the walnut base together. Its own layer, too, so its
        // per-frame redraws don't invalidate whatever holds the globe.
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                state.frame
                translationX = state.offset.x * radius * ShiftOfOffset
                translationY = state.offset.y * radius * ShiftOfOffset
                rotationZ = state.tilt * TiltDegreesOfRadians
                transformOrigin = TransformOrigin(0.5f, BaseBottomOfHeight)
            }
        ) {
            Spacer(
                Modifier.fillMaxSize()
                    .graphicsLayer { renderEffect = glass }
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
}
