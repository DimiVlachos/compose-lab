package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import co.touchlab.kermit.Logger
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.DripDriver
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.FoggedWindow
import dev.dimvlachos.lab.core.presentation.components.fog.MistDriver
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.fogdemo.FogDemos
import dev.dimvlachos.lab.fogdemo.clipFrameToWindow
import dev.dimvlachos.lab.fogdemo.newFogDemoState
import dev.dimvlachos.lab.fogdemo.pointAt
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fog_hint_camera
import dev.dimvlachos.lab.resources.fog_hint_wipe
import dev.dimvlachos.lab.resources.ic_photo_camera
import dev.dimvlachos.lab.resources.mirror_view
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val HintIconSize = 18.dp

private val log = Logger.withTag("FogDemo")

@Composable
internal fun FogDemo(
    state: DemoState,
    fog: FogState = remember { newFogDemoState() },
    // No default: each version says which camera it uses, so the bathroom can never ask for one.
    cameraAccess: CameraAccess,
) {
    var window by remember { mutableStateOf(Size.Zero) }
    // Condensation running down the glass; in the clip, only the script's drops.
    val drips = remember(fog) { DripDriver(fog, wipeRadius = FogDemos.FingerBrush) }
    // Where the script's finger touches, shown in the clip: read only while drawing.
    val finger = remember { ScriptFinger() }
    drips.randomStarts = !state.recording && !state.replay
    val density = LocalDensity.current
    var cameraFailed by remember { mutableStateOf(false) }
    var wiped by remember { mutableStateOf(false) }
    val cameraNeed = cameraAccess.need()
    val canAsk = cameraNeed != Need.Nothing
    // Not over a clip the user asked to replay: they came to watch it.
    var cardOpen by remember { mutableStateOf(!state.replay) }
    val cardShown = cardOpen && canAsk && !state.recording

    // Allow asks for the camera and waits for the system's answer. A refusal in its dialog closes
    // the card for the still reflection; refused for good, the card stays, now offering settings.
    val currentCamera by rememberUpdatedState(cameraAccess)
    val scope = rememberCoroutineScope()
    var asking by remember { mutableStateOf<Job?>(null) }
    fun ask() {
        // A second tap while the system asks must not ask again.
        if (asking?.isActive == true) return
        // Undispatched: the dialog is asked for within the tap itself.
        asking =
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                (currentCamera as? CameraAccess.Askable)?.let { asked ->
                    asked.ask()
                    snapshotFlow { currentCamera }.first { it !== asked }
                }
                if (currentCamera !is CameraAccess.Blocked) cardOpen = false
            }
    }

    // The script's wipe plays its finger back sample by sample: the path already holds the hand's
    // speed, so the playback itself is linear. The path is drawn on the clip's glass, placed on
    // whatever glass this is.
    DisposableEffect(state, fog) {
        state.setWipeHandler { path, duration ->
            if (window.isEmpty()) return@setWipeHandler
            val start = clipFrameToWindow(path.first(), window)
            val stroke = fog.beginStroke(start, radius = FogDemos.FingerBrush)
            finger.at = start
            finger.shown = 1f
            // Stopped halfway, the finger lifts at once rather than staying on the glass.
            try {
                animate(
                    0f,
                    1f,
                    animationSpec =
                        tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
                ) { t, _ ->
                    val point = clipFrameToWindow(pointAt(path, t), window)
                    fog.extendStroke(stroke, point)
                    finger.at = point
                }
                // Lifted, the touch fades as a phone's "show taps" does.
                animate(1f, 0f, animationSpec = tween(FingerLiftMillis)) { shown, _ ->
                    finger.shown = shown
                }
            } finally {
                finger.shown = 0f
            }
        }
        // The script's drop starts in the clip's frame, placed on whatever window this is.
        state.setDripHandler { at, length ->
            if (!window.isEmpty()) drips.drip(clipFrameToWindow(at, window), length)
        }
        // The script's mist fogs the glass evenly back over, easing in and out.
        state.setMistHandler { duration ->
            val mist = fog.beginMist()
            animate(
                0f,
                1f,
                animationSpec =
                    tween(duration.inWholeMilliseconds.toInt(), easing = FastOutSlowInEasing),
            ) { amount, _ ->
                fog.setMistAmount(mist, amount)
            }
        }
        onDispose {
            state.setWipeHandler(null)
            state.setDripHandler(null)
            state.setMistHandler(null)
        }
    }

    // The front camera is the mirror, only while the demo is in front of the user: leaving turns
    // it off, and its light with it.
    val camera = (cameraAccess as? CameraAccess.Granted)?.camera?.takeUnless { cameraFailed }
    if (camera != null) {
        // Keyed on the camera, not the access around it: a new wrapper at a recomposition must not
        // restart it.
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(camera, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    camera.run()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "no camera, so the mirror shows its still reflection" }
                    cameraFailed = true
                }
            }
        }
    }

    // The first wipe, remembered: the mist may fog it over, but the mirror has been found. A
    // drop's part-clear streak is not a wipe.
    LaunchedEffect(fog) {
        snapshotFlow { fog.marks.any { it is WipeStroke && it.clarity >= 1f } }.first { it }
        wiped = true
    }

    // The still-steamy room mists wiped glass back over by itself, slowly, while the demo is in
    // front of the user; never while the script plays, which mists it itself.
    if (!state.recording && !state.replay) {
        val mist = remember(fog) { MistDriver(fog) }
        val mistLifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(mist, mistLifecycle) {
            mistLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    snapshotFlow { mist.needed }.first { it }
                    var previous = withFrameNanos { it }
                    while (mist.needed) {
                        withFrameNanos { now ->
                            mist.advance((now - previous) / 1_000_000_000f)
                            previous = now
                        }
                    }
                }
            }
        }
    }

    // Drops run only while the demo is in front of the user. Between drops, once the last has
    // relaxed into its resting shape, there is nothing to draw, so the loop sleeps until the next
    // is due rather than asking for frames.
    val dripLifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(drips, dripLifecycle) {
        dripLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                // No glass yet to drip on: wait for one rather than go round and round.
                if (window.isEmpty()) {
                    snapshotFlow { window.isEmpty() }.first { !it }
                    continue
                }
                if (!drips.needsFrames) {
                    val wait = drips.secondsToNextStart
                    // The frame clock's time, to count the sleep towards the next drop however
                    // it ends.
                    val asleepSince = withFrameNanos { it }
                    // Asleep until the next drop is due, or until a wipe or the mist takes one
                    // off the glass, or the script starts one: whichever comes first.
                    // A plain delay: it follows the frame clock, as a timeout's timer would not.
                    var due by mutableStateOf(false)
                    coroutineScope {
                        val timer = wait?.let {
                            launch {
                                delay((it * 1000).toLong().coerceAtLeast(1))
                                due = true
                            }
                        }
                        snapshotFlow { due || drips.wantsFrames }.first { it }
                        timer?.cancel()
                    }
                    drips.advance(withFrameNanos { (it - asleepSince) / 1_000_000_000f })
                    continue
                }
                var previous = withFrameNanos { it }
                while (drips.needsFrames) {
                    withFrameNanos { now ->
                        drips.advance((now - previous) / 1_000_000_000f)
                        previous = now
                    }
                }
            }
        }
    }

    val surface = LabTheme.colors.surface
    val startingGlass = remember(surface) { ColorPainter(surface) }
    // You, cut out of the camera's picture, in front of the bathroom rather than your own room.
    val bathroom = imageResource(Res.drawable.mirror_view)
    val inTheBathroom =
        remember(camera, bathroom) { camera?.let { BackdropPainter(it.mirror, bathroom) } }
    Box(Modifier.fillMaxSize()) {
        // The fog is the glass of a mirror on the bathroom wall.
        MirrorOnWall { glassModifier ->
            Box(glassModifier) {
                FoggedWindow(
                    // A camera still starting shows plain glass: the still is for when there is
                    // none.
                    photo =
                        when {
                            camera == null -> painterResource(Res.drawable.mirror_view)
                            camera.showing && inTheBathroom != null -> inTheBathroom
                            else -> startingGlass
                        },
                    state = fog,
                    modifier =
                        Modifier.fillMaxSize().onSizeChanged {
                            window = it.toSize()
                            drips.glass =
                                with(density) { DpSize(it.width.toDp(), it.height.toDp()) }
                        },
                    brushRadius = FogDemos.FingerBrush,
                    beads = { drips.beads },
                )
                // The script's touches, over the glass, as a phone shows taps.
                Box(Modifier.fillMaxSize().drawBehind { drawFinger(finger) })
            }
        }
        // First the wipe hint, until the mirror is found; then, while the camera can still be
        // asked for, a pill back to the card.
        val hint =
            when {
                // Not over a clip being recorded or replayed: it is there to be watched.
                state.recording || state.replay || cardShown -> null
                !wiped -> Res.string.fog_hint_wipe
                canAsk -> Res.string.fog_hint_camera
                else -> null
            }
        Crossfade(
            targetState = hint,
            modifier = Modifier.align(Alignment.BottomCenter).padding(LabTheme.spacing.mediumLarge),
        ) { shown ->
            if (shown != null) {
                Row(
                    Modifier.background(
                            LabTheme.colors.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(percent = 50),
                        )
                        .then(if (canAsk) Modifier.clickable { cardOpen = true } else Modifier)
                        .padding(
                            horizontal = LabTheme.spacing.mediumLarge,
                            vertical = LabTheme.spacing.small,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A way back to the card, where there is something to ask for.
                    if (canAsk) {
                        Icon(
                            painterResource(Res.drawable.ic_photo_camera),
                            contentDescription = null,
                            tint = LabTheme.colors.accent,
                            modifier = Modifier.size(HintIconSize),
                        )
                    }
                    Text(
                        stringResource(shown),
                        color = LabTheme.colors.textPrimary,
                        style = LabTheme.typography.body,
                    )
                }
            }
        }
        // While the card is up the glass takes no touches, so reaching for a button cannot wipe it.
        // Being hit is enough to keep touches off the glass beneath; consuming them as well would
        // take a real finger's small movements from the card's own buttons, which then cancel the
        // tap.
        if (cardShown) {
            Box(
                Modifier.matchParentSize().pointerInput(Unit) {
                    awaitPointerEventScope { while (true) awaitPointerEvent() }
                },
                contentAlignment = Alignment.Center,
            ) {
                MirrorPermissionCard(
                    camera = cameraNeed,
                    onAllow = ::ask,
                    onOpenSettings = {
                        (cameraAccess as? CameraAccess.Blocked)?.openSettings?.invoke()
                    },
                    onNotNow = { cardOpen = false },
                )
            }
        }
    }
}

private fun CameraAccess.need() =
    when (this) {
        is CameraAccess.Askable -> Need.Ask
        is CameraAccess.Blocked -> Need.Settings
        else -> Need.Nothing
    }

/** Where the script's finger is on the glass, as a fraction of it, and how plainly it shows. */
@Stable
private class ScriptFinger {
    var at by mutableStateOf(Offset.Zero)
    var shown by mutableFloatStateOf(0f)
}

// A fingertip, as a phone's "show taps" marks one: a soft pale disc with a brighter ring.
private fun DrawScope.drawFinger(finger: ScriptFinger) {
    val shown = finger.shown
    if (shown <= 0f) return
    val centre = Offset(finger.at.x * size.width, finger.at.y * size.height)
    val radius = FingerMarkRadius.toPx()
    drawCircle(Color.Black.copy(alpha = 0.12f * shown), radius + 1.dp.toPx(), centre)
    drawCircle(Color.White.copy(alpha = 0.45f * shown), radius, centre)
    drawCircle(
        Color.White.copy(alpha = 0.85f * shown),
        radius,
        centre,
        style = Stroke(width = 2.dp.toPx()),
    )
}

private val FingerMarkRadius = 18.dp
private const val FingerLiftMillis = 250
