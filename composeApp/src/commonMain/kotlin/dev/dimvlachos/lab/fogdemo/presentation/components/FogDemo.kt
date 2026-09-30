package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
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
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.FoggedWindow
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
import dev.dimvlachos.lab.resources.ic_mic
import dev.dimvlachos.lab.resources.window_view
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// A held finger breathes steadily, a little softer than a firm blow.
private const val HoldStrength = 0.7f

private val HintIconSize = 18.dp

// How long the fog takes to evaporate at the loop's end.
private const val EvaporateMillis = 1_000

// About 2 s of nothing but zeros: the microphone is taken by something else, a call perhaps.
private const val SilentMicFrames = 60

private val log = Logger.withTag("FogDemo")

@Composable
internal fun FogDemo(
    state: DemoState,
    fog: FogState = remember { newFogDemoState() },
    micAccess: MicAccess = rememberMicAccess(enabled = !state.recording),
) {
    var window by remember { mutableStateOf(Size.Zero) }
    val driver = remember(fog) { BreathDriver(fog) }
    var micFailed by remember { mutableStateOf(false) }
    var breathed by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    val listening = micAccess is MicAccess.Granted && !micFailed
    val canAsk = micAccess is MicAccess.Askable || micAccess is MicAccess.Blocked
    // Not over a clip the user asked to replay: they came to watch it.
    var cardOpen by remember { mutableStateOf(!state.replay) }
    var asked by remember { mutableStateOf(false) }
    val cardShown = cardOpen && canAsk && !state.recording

    // The system's answer: a refusal in its dialog closes the card for holding; refused for good,
    // the card stays, now offering settings.
    LaunchedEffect(micAccess) {
        if (!asked) return@LaunchedEffect
        asked = false
        if (micAccess !is MicAccess.Blocked) cardOpen = false
    }

    // The script's wipe plays its finger back sample by sample: the path already holds the hand's
    // speed, so the playback itself is linear. The path is drawn in the clip's frame, placed on
    // whatever window this is.
    DisposableEffect(state, fog, driver) {
        state.setWipeHandler { path, duration ->
            if (window.isEmpty()) return@setWipeHandler
            val stroke = fog.beginStroke(clipFrameToWindow(path.first(), window))
            animate(
                0f,
                1f,
                animationSpec = tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
            ) { t, _ ->
                fog.extendStroke(stroke, clipFrameToWindow(pointAt(path, t), window))
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

    // Back on 0, the loop's start, the fog evaporates to clear glass. Only on a return to 0: the
    // glass already starts clear, and an evaporation then would race a microphone already fogging
    // it.
    LaunchedEffect(state, fog) {
        snapshotFlow { state.selectedIndex }
            .drop(1)
            .filter { it == 0 }
            .collectLatest {
                val evaporation = fog.beginEvaporation()
                animate(0f, 1f, animationSpec = tween(EvaporateMillis)) { amount, _ ->
                    fog.setEvaporationAmount(evaporation, amount)
                }
            }
    }

    Box(Modifier.fillMaxSize()) {
        FoggedWindow(
            photo = painterResource(Res.drawable.window_view),
            state = fog,
            modifier = Modifier.fillMaxSize().onSizeChanged { window = it.toSize() },
            brushRadius = FogDemos.ScrubBrush,
            onHoldChange =
                if (listening || state.recording || cardShown) null else { held -> holding = held },
        )
        // The blow hint goes after the first breath; the hold pill stays while it is the way back
        // to
        // the card.
        val hint =
            when {
                state.recording || cardShown -> null
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
                    // A way back to the card, where there is a microphone to ask for.
                    if (canAsk) {
                        Icon(
                            painterResource(Res.drawable.ic_mic),
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
                MicPermissionCard(
                    blocked = micAccess is MicAccess.Blocked,
                    onAllow = {
                        asked = true
                        (micAccess as? MicAccess.Askable)?.ask?.invoke()
                    },
                    onOpenSettings = { (micAccess as? MicAccess.Blocked)?.openSettings?.invoke() },
                    onNotNow = { cardOpen = false },
                )
            }
        }
    }
}
