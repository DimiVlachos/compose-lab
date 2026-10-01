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
import dev.dimvlachos.lab.core.audio.BlowDetector
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.audio.MicFrameSeconds
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.DripDriver
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.FoggedWindow
import dev.dimvlachos.lab.core.presentation.components.fog.MistDriver
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.fogdemo.BreathDriver
import dev.dimvlachos.lab.fogdemo.FogDemos
import dev.dimvlachos.lab.fogdemo.clipFrameToWindow
import dev.dimvlachos.lab.fogdemo.newFogDemoState
import dev.dimvlachos.lab.fogdemo.pointAt
import dev.dimvlachos.lab.fogdemo.scriptedBreathStrength
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fog_hint_blow
import dev.dimvlachos.lab.resources.fog_hint_hold
import dev.dimvlachos.lab.resources.fog_hint_wipe
import dev.dimvlachos.lab.resources.ic_mic
import dev.dimvlachos.lab.resources.ic_photo_camera
import dev.dimvlachos.lab.resources.mirror_view
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// A held finger breathes steadily, a little softer than a firm blow.
private const val HoldStrength = 0.7f

private val HintIconSize = 18.dp

// About 2 s of nothing but zeros: the microphone is taken by something else, a call perhaps.
private const val SilentMicFrames = 60

private val log = Logger.withTag("FogDemo")

@Composable
internal fun FogDemo(
    state: DemoState,
    fog: FogState = remember { newFogDemoState() },
    // No defaults: each version says which microphone and camera it uses, so the bathroom can
    // never ask for either.
    micAccess: MicAccess,
    cameraAccess: CameraAccess,
    // Whether the user can breathe on the glass: by blowing, or holding a finger still. Off, only
    // the clip's own breath fogs it over.
    breathing: Boolean = true,
) {
    var window by remember { mutableStateOf(Size.Zero) }
    val driver = remember(fog) { BreathDriver(fog) }
    // Condensation running down the glass; in the clip, only the script's drops.
    val drips = remember(fog) { DripDriver(fog, wipeRadius = FogDemos.FingerBrush) }
    // Where the script's finger touches, shown in the clip: read only while drawing.
    val finger = remember { ScriptFinger() }
    drips.randomStarts = !state.recording && !state.replay
    val density = LocalDensity.current
    var micFailed by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    var breathed by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    var wiped by remember { mutableStateOf(false) }
    val listening = micAccess is MicAccess.Granted && !micFailed
    val cameraNeed = cameraAccess.need()
    val micNeed = micAccess.need()
    val canAsk = cameraNeed != Need.Nothing || micNeed != Need.Nothing
    // Not over a clip the user asked to replay: they came to watch it.
    var cardOpen by remember { mutableStateOf(!state.replay) }
    val cardShown = cardOpen && canAsk && !state.recording

    // Allow asks for the camera, waits for the system's answer, then the microphone: Android shows
    // one permission dialog at a time. A refusal in its dialog closes the card for holding and the
    // still reflection; refused for good, the card stays, now offering settings.
    val currentCamera by rememberUpdatedState(cameraAccess)
    val currentMic by rememberUpdatedState(micAccess)
    val scope = rememberCoroutineScope()
    var asking by remember { mutableStateOf<Job?>(null) }
    var askingInTurn by remember { mutableStateOf(false) }
    fun askInTurn() {
        // A second tap while the system asks must not ask again.
        if (asking?.isActive == true) return
        // Undispatched: the first dialog is asked for within the tap itself.
        askingInTurn = true
        asking =
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    (currentCamera as? CameraAccess.Askable)?.let { asked ->
                        asked.ask()
                        snapshotFlow { currentCamera }.first { it !== asked }
                    }
                    (currentMic as? MicAccess.Askable)?.let { asked ->
                        asked.ask()
                        snapshotFlow { currentMic }.first { it !== asked }
                    }
                    if (
                        currentCamera !is CameraAccess.Blocked && currentMic !is MicAccess.Blocked
                    ) {
                        cardOpen = false
                    }
                } finally {
                    askingInTurn = false
                }
            }
    }

    // The script's wipe plays its finger back sample by sample: the path already holds the hand's
    // speed, so the playback itself is linear. The path is drawn on the clip's glass, placed on
    // whatever glass this is.
    DisposableEffect(state, fog, driver) {
        state.setWipeHandler { path, duration ->
            if (window.isEmpty()) return@setWipeHandler
            val start = clipFrameToWindow(path.first(), window)
            val stroke = fog.beginStroke(start, radius = FogDemos.FingerBrush)
            finger.at = start
            finger.shown = 1f
            animate(
                0f,
                1f,
                animationSpec = tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
            ) { t, _ ->
                val point = clipFrameToWindow(pointAt(path, t), window)
                fog.extendStroke(stroke, point)
                finger.at = point
            }
            // Lifted, the touch fades as a phone's "show taps" does.
            animate(1f, 0f, animationSpec = tween(FingerLiftMillis)) { shown, _ ->
                finger.shown = shown
            }
        }
        // The script's breath swells and fades through the same driver as a real one.
        state.setBreatheHandler { duration, strength ->
            val seconds = duration.inWholeMilliseconds / 1000f
            var previous = 0f
            animate(
                0f,
                1f,
                animationSpec = tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
            ) { t, _ ->
                driver.advance(scriptedBreathStrength(t, strength), (t - previous) * seconds)
                previous = t
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
            state.setBreatheHandler(null)
            state.setDripHandler(null)
            state.setMistHandler(null)
        }
    }

    // A blow on the microphone fogs the glass, only while the demo is in front of the user.
    if (micAccess is MicAccess.Granted && !micFailed) {
        // Keyed on the microphone, not the access around it: a new wrapper at a recomposition must
        // not restart listening, or a fresh detector would take a blow for the room's own level.
        val microphone = micAccess.microphone
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(microphone, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val detector = BlowDetector()
                var silentFrames = 0
                try {
                    microphone.frames.collect { frame ->
                        silentFrames = if (frame.all { it == 0f }) silentFrames + 1 else 0
                        check(silentFrames < SilentMicFrames) {
                            "the microphone hears only silence"
                        }
                        val strength = detector.process(frame)
                        if (strength > 0f) {
                            driver.advance(strength, MicFrameSeconds)
                            breathed = true
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "no microphone, so holding the glass breathes on it" }
                    micFailed = true
                }
            }
        }
    }

    // The front camera is the mirror, only while the demo is in front of the user: leaving turns
    // it off, and its light with it.
    val camera = (cameraAccess as? CameraAccess.Granted)?.camera?.takeUnless { cameraFailed }
    // Not while Allow is still asking: the microphone's dialog would pause a camera just started.
    if (camera != null && !askingInTurn) {
        // Keyed on the camera, not the access around it, like the microphone.
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

    // The first wipe, remembered: a breath may fog it over, but the mirror has been found. A
    // drop's part-clear streak is not a wipe.
    LaunchedEffect(fog) {
        snapshotFlow { fog.marks.any { it is WipeStroke && it.clarity >= 1f } }.first { it }
        wiped = true
    }

    // Without breath, the still-steamy room mists wiped glass back over by itself, slowly, while
    // the demo is in front of the user; never while the script plays, which mists it itself.
    if (!breathing && !state.recording && !state.replay) {
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
                    // Asleep until the next drop is due, or until a wipe or a breath takes one
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

    // Without a microphone, a held finger breathes on the glass for as long as it stays.
    LaunchedEffect(holding) {
        if (!holding) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                driver.advance(HoldStrength, (now - previous) / 1_000_000_000f)
                previous = now
            }
            breathed = true
        }
    }

    val surface = LabTheme.colors.surface
    val startingGlass = remember(surface) { ColorPainter(surface) }
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
                            camera.showing -> camera.mirror
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
                    onHoldChange =
                        if (!breathing || listening || state.recording || cardShown) null
                        else { held -> holding = held },
                )
                // The script's touches, over the glass, as a phone shows taps.
                Box(Modifier.fillMaxSize().drawBehind { drawFinger(finger) })
            }
        }
        // First the wipe hint, until the mirror is found. The blow hint goes after the first
        // breath; the hold pill stays while it is the way back to the card.
        val hint =
            when {
                // Not over a clip being recorded or replayed: it is there to be watched.
                state.recording || state.replay || cardShown -> null
                !wiped -> Res.string.fog_hint_wipe
                !breathing -> null
                listening -> if (breathed) null else Res.string.fog_hint_blow
                breathed && !canAsk -> null
                else -> Res.string.fog_hint_hold
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
                            painterResource(
                                if (cameraNeed != Need.Nothing) Res.drawable.ic_photo_camera
                                else Res.drawable.ic_mic
                            ),
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
                    mic = micNeed,
                    onAllow = ::askInTurn,
                    onOpenSettings = {
                        (cameraAccess as? CameraAccess.Blocked)?.openSettings?.invoke()
                            ?: (micAccess as? MicAccess.Blocked)?.openSettings?.invoke()
                    },
                    onNotNow = { cardOpen = false },
                    blowing = micAccess != MicAccess.Unavailable && !micFailed,
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

private fun MicAccess.need() =
    when (this) {
        is MicAccess.Askable -> Need.Ask
        is MicAccess.Blocked -> Need.Settings
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
