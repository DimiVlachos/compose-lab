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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.ShakeImpulses
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DegreesPerRadian = (180 / kotlin.math.PI).toFloat()

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
    // The globe moves only while the app is in front: in the background the loop and the shake
    // sensor stop, and the first frame back moves nothing.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val resumed by lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(state, sounds, lifecycle) {
        val swish: (Float) -> Unit = { volume -> sounds.play(Sound.GlobeSwish, volume) }
        state.onSwish = swish
        try {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var last = 0L
                try {
                    while (true) {
                        withFrameNanos { now ->
                            // The first frame moves nothing; a stall can't blow the spring up.
                            val dt =
                                if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                            last = now
                            state.step(dt, pending, { shaken() }, { settled() })
                        }
                    }
                } finally {
                    // Pausing or leaving lets go of the glass, so it isn't left mid-drag.
                    state.release()
                }
            }
        } finally {
            if (state.onSwish === swish) state.onSwish = null
        }
    }
    ShakeImpulses(shakeSensorEnabled && resumed.isAtLeast(Lifecycle.State.RESUMED)) {
        state.nudge(it)
    }

    // Painting the three weathers takes a moment, so it happens off the main thread, once.
    val scenes by
        produceState<SantoriniScenes?>(null) {
            value = withContext(Dispatchers.Default) { SantoriniScenes.shared }
        }
    LaunchedEffect(state, scenes) { scenes?.let { state.useFloor(it::floorAt) } }
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
    // The physics runs in the prototype's CSS pixels.
    val density = LocalDensity.current.density
    SideEffect { state.configure(place.scale / density, radius / density) }
    val glass = remember { compileGlassShader() }
    val dark = LabTheme.isDark
    val measurer = rememberTextMeasurer()
    val densityNow = LocalDensity.current
    // The walnut base doesn't change, so it's painted once per size and theme.
    val base =
        remember(radius, place.scale, dark, measurer, densityNow) {
            if (radius < 1f) null else paintBase(radius, place.scale, dark, measurer, densityNow)
        }
    val boxAt = IntOffset(place.boxLeft.roundToInt(), place.boxTop.roundToInt())

    Box(modifier.onSizeChanged { size = it }) {
        // The globe and its base take touches, focus and the globe's semantics; the room doesn't.
        Box(
            Modifier.offset { boxAt }
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
                .pointerInput(state, density) {
                    detectDragGestures(
                        onDragEnd = { state.release() },
                        onDragCancel = { state.release() },
                    ) { change, drag ->
                        change.consume()
                        state.drag(drag.x / density)
                    }
                }
        )
        // The room: wall, table and the shadows and caustic the globe throws, which follow it.
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            drawIntoCanvas { canvas ->
                Canvas2D(canvas, this.size.width.toInt(), this.size.height.toInt())
                    .drawRoom(
                        this.size.width,
                        this.size.height,
                        place.cx,
                        place.cy,
                        radius,
                        dark,
                        state.sceneFrom,
                        state.sceneTo,
                        state.fade,
                        state.offset.x * density,
                        state.tilt,
                    )
            }
        }
        // The globe moves as one object, rocking on its base like a globe that is carried: the
        // water, the glass's highlights and the walnut base together. Its own layer, too, so its
        // per-frame redraws don't invalidate whatever holds the globe.
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                state.frame
                translationX = state.offset.x * density
                translationY = state.offset.y * density
                rotationZ = state.tilt * DegreesPerRadian
                transformOrigin =
                    TransformOrigin(
                        place.cx / size.width.coerceAtLeast(1).toFloat(),
                        place.pivotY / size.height.coerceAtLeast(1).toFloat(),
                    )
            }
        ) {
            // The base's top face and groove, behind the glass.
            Canvas(Modifier.fillMaxSize()) {
                base?.let { drawImage(it.back, topLeft = boxAt.toOffset()) }
            }
            // The prototype's glass shader over the interior, its bubble following the tilt and
            // its reflections the room's crossfade; or, where it can't run, the plain glass.
            Spacer(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        state.frame
                        renderEffect =
                            if (glass == null || size == IntSize.Zero) null
                            else
                                glass.effect(
                                    glassLook(
                                        center,
                                        radius,
                                        dark,
                                        state.sceneFrom,
                                        state.sceneTo,
                                        state.fade,
                                        state.tilt,
                                    )
                                )
                    }
                    .drawBehind {
                        state.frame
                        if (glass != null) {
                            drawInterior(state, scenes, center, radius)
                        } else {
                            drawInterior(state, scenes, center, radius, zoom = FallbackZoom)
                            drawGlassFallback(
                                center,
                                radius,
                                causticsOf(state.sceneTo),
                                glassBubble(state.tilt),
                            )
                        }
                    }
            )
            // The turned body and its plaque, in front.
            Canvas(Modifier.fillMaxSize()) {
                base?.let { drawImage(it.front, topLeft = boxAt.toOffset()) }
            }
        }
    }
}
