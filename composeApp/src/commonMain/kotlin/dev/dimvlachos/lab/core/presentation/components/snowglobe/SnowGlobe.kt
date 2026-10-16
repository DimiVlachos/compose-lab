package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.dimvlachos.lab.core.platform.ShakeImpulses
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import kotlin.math.roundToInt

/**
 * How far the globe moves for the glass's spring [SnowGlobeState.offset], in radii per globe unit,
 * and how far it leans, in degrees per radian of [SnowGlobeState.tilt]: at the peak of a hard shake
 * (offset about 0.5, tilt at its 0.12 limit) about 0.05 r and 3°, a carried globe, not a sliding
 * lens.
 */
private const val ShiftOfOffset = 0.1f
private const val TiltDegreesOfRadians = 25f

/**
 * A glass snow globe holding [scene]. Drag it about, shake the phone, or press it, and the water
 * swirls the glitter up: [onShaken] once the shake is hard enough, [onSettled] once the glitter has
 * calmed and no refresh is [refreshPending]. A change of [scene] crossfades inside the glass. With
 * [reducedMotion] a shake still refreshes but the glitter stays down. [stateDescription], the
 * conditions it shows, is read out after its [description].
 *
 * It fills [modifier]'s bounds as the room the globe stands in, and puts the globe where [layout]
 * says, or, without one, as large as fits in the middle.
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
    layout: GlobeLayout? = null,
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

    var size by remember { mutableStateOf(IntSize.Zero) }
    val place =
        layout
            ?: globeLayout(
                size.width.toFloat(),
                headerBottom = -4f * size.width / 360f,
                daysTop = size.height + 46f * size.width / 360f,
            )
    val radius = place.radius
    val center = Offset(place.cx, place.cy)
    val glass =
        remember(size, place.cx, place.cy, radius) {
            if (size == IntSize.Zero) null else glassRenderEffect(center, radius)
        }

    Box(modifier.onSizeChanged { size = it }) {
        // The globe and its base take touches, focus and the globe's semantics; the room doesn't.
        Box(
            Modifier.offset { IntOffset(place.boxLeft.roundToInt(), place.boxTop.roundToInt()) }
                .size(
                    with(LocalDensity.current) {
                        DpSize(place.boxWidth.toDp(), place.boxHeight.toDp())
                    }
                )
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
                    if (
                        e.type == KeyEventType.KeyUp &&
                            (e.key == Key.Enter || e.key == Key.Spacebar)
                    ) {
                        state.shake()
                        true
                    } else false
                }
                .focusable()
                .pointerInput(state, radius) {
                    detectDragGestures(
                        onDragEnd = { state.release() },
                        onDragCancel = { state.release() },
                    ) { change, drag ->
                        change.consume()
                        state.drag(drag / radius.coerceAtLeast(1f))
                    }
                }
        )
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
                transformOrigin =
                    TransformOrigin(
                        place.cx / size.width.coerceAtLeast(1).toFloat(),
                        place.pivotY / size.height.coerceAtLeast(1).toFloat(),
                    )
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
