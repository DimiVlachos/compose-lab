package dev.dimvlachos.lab.frostdemo.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import co.touchlab.kermit.Logger
import dev.dimvlachos.lab.core.audio.BlowDetector
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.audio.MicFrameSeconds
import dev.dimvlachos.lab.core.audio.rememberMicAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import dev.dimvlachos.lab.core.presentation.components.frost.FrostedWindow
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.frostdemo.BreathDriver
import dev.dimvlachos.lab.frostdemo.FrostDemos
import dev.dimvlachos.lab.frostdemo.clipFrameToWindow
import dev.dimvlachos.lab.frostdemo.newFrostDemoState
import dev.dimvlachos.lab.frostdemo.pointAt
import dev.dimvlachos.lab.frostdemo.scriptedBreathStrength
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.frost_hint_blow
import dev.dimvlachos.lab.resources.frost_hint_hold
import dev.dimvlachos.lab.resources.photo_santorini
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// A held finger breathes steadily, a little softer than a firm blow.
private const val HoldStrength = 0.7f

// How long the frost takes to evaporate at the loop's end.
private const val EvaporateMillis = 1_000

// About 2 s of nothing but zeros: the microphone is taken by something else, a call perhaps.
private const val SilentMicFrames = 60

private val log = Logger.withTag("FrostDemo")

@Composable
internal fun FrostDemo(
    state: DemoState,
    frost: FrostState = remember { newFrostDemoState() },
    micAccess: MicAccess = rememberMicAccess(enabled = !state.recording),
) {
    var window by remember { mutableStateOf(Size.Zero) }
    val driver = remember(frost) { BreathDriver(frost) }
    var micFailed by remember { mutableStateOf(false) }
    var breathed by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    val listening = micAccess is MicAccess.Granted && !micFailed

    // The script's wipe plays its finger back sample by sample: the path already holds the hand's
    // speed, so the playback itself is linear. The path is drawn in the clip's frame, placed on
    // whatever window this is.
    DisposableEffect(state, frost, driver) {
        state.setWipeHandler { path, duration ->
            if (window.isEmpty()) return@setWipeHandler
            val stroke = frost.beginStroke(clipFrameToWindow(path.first(), window))
            animate(
                0f,
                1f,
                animationSpec = tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
            ) { t, _ ->
                frost.extendStroke(stroke, clipFrameToWindow(pointAt(path, t), window))
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
        onDispose {
            state.setWipeHandler(null)
            state.setBreatheHandler(null)
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

    // Back on 0, the loop's start, the frost evaporates to clear glass. Only on a return to 0: the
    // glass already starts clear, and a thaw then would race a microphone already fogging it.
    LaunchedEffect(state, frost) {
        snapshotFlow { state.selectedIndex }
            .drop(1)
            .filter { it == 0 }
            .collectLatest {
                val thaw = frost.beginThaw()
                animate(0f, 1f, animationSpec = tween(EvaporateMillis)) { amount, _ ->
                    frost.setThawAmount(thaw, amount)
                }
            }
    }

    Box(Modifier.fillMaxSize()) {
        FrostedWindow(
            photo = painterResource(Res.drawable.photo_santorini),
            state = frost,
            modifier = Modifier.fillMaxSize().onSizeChanged { window = it.toSize() },
            brushRadius = FrostDemos.ScrubBrush,
            onHoldChange = if (listening || state.recording) null else { held -> holding = held },
        )
        val hint =
            when {
                state.recording || breathed -> null
                listening -> Res.string.frost_hint_blow
                micAccess is MicAccess.Pending -> null
                else -> Res.string.frost_hint_hold
            }
        Crossfade(
            targetState = hint,
            modifier = Modifier.align(Alignment.BottomCenter).padding(LabTheme.spacing.mediumLarge),
        ) { shown ->
            if (shown != null) {
                Text(
                    stringResource(shown),
                    color = LabTheme.colors.textPrimary,
                    style = LabTheme.typography.body,
                    modifier =
                        Modifier.background(
                                LabTheme.colors.surface.copy(alpha = 0.7f),
                                RoundedCornerShape(percent = 50),
                            )
                            .padding(
                                horizontal = LabTheme.spacing.mediumLarge,
                                vertical = LabTheme.spacing.small,
                            ),
                )
            }
        }
    }
}
