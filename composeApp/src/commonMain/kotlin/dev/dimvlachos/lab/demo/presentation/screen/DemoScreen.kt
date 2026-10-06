package dev.dimvlachos.lab.demo.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.RecordingLog
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.platform.LockLandscape
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_back
import dev.dimvlachos.lab.resources.action_replay
import dev.dimvlachos.lab.resources.action_stop
import dev.dimvlachos.lab.resources.demo_navbar
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private val RecordPreRoll = 1_500.milliseconds

@Composable
internal fun DemoScreen(demo: Demo, record: Boolean, label: String?, onBack: (() -> Unit)?) {
    var runId by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(record || demo.autoplay) }
    val state = remember(runId) { DemoState(recording = record, replay = runId > 0) }
    if (demo.landscape) LockLandscape()
    LaunchedEffect(runId, playing) {
        if (!playing) return@LaunchedEffect
        if (record) {
            delay(RecordPreRoll)
            // One run before the start marker: the first play of a demo pays for shader
            // compilation and text and icon caches with a few long frames, and the recorder cuts
            // everything before the marker, so the recorded run is the second one.
            demo.script.play(state)
            RecordingLog.started(demo.id)
            demo.script.play(state)
            RecordingLog.done(demo.id)
        } else {
            // scripts end in their start state, so this loops cleanly
            while (true) demo.script.play(state)
        }
    }

    // The stage: the demo and, on a multi-platform capture, its platform label.
    val stage: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier.clipToBounds()) {
            key(runId) { demo.content(state) }
            if (label != null) {
                Text(
                    label,
                    color = LabTheme.colors.textMuted,
                    style = LabTheme.typography.subtitle,
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .padding(top = LabTheme.spacing.mediumLarge),
                )
            }
        }
    }
    Box(Modifier.fillMaxSize().background(LabTheme.colors.background)) {
        if (record) {
            // The clip's frame, centred; the recorder crops to it: 4:5, or 16:9 for a demo that
            // runs in landscape.
            val frame =
                if (demo.landscape) {
                    Modifier.fillMaxHeight().aspectRatio(16f / 9f)
                } else {
                    Modifier.fillMaxWidth().aspectRatio(4f / 5f)
                }
            stage(Modifier.align(Alignment.Center).then(frame))
        } else {
            // On the phone the demo is something to use, so it fills the screen under the bar:
            // a 4:5 frame would leave bands above and below it that look like the demo but never
            // reach it.
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                            )
                        )
                        .padding(horizontal = LabTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        TextButton(
                            onClick = onBack,
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor = LabTheme.colors.accent
                                ),
                        ) {
                            Text(stringResource(Res.string.action_back))
                        }
                    }
                    Text(
                        stringResource(demo.title),
                        color = LabTheme.colors.textPrimary,
                        style = LabTheme.typography.body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = LabTheme.spacing.small),
                    )
                    TextButton(
                        onClick = {
                            if (playing) {
                                playing = false
                                state.endReplay()
                            } else {
                                runId++
                                playing = true
                            }
                        },
                        colors =
                            ButtonDefaults.textButtonColors(contentColor = LabTheme.colors.accent),
                    ) {
                        Text(
                            stringResource(
                                if (playing) Res.string.action_stop else Res.string.action_replay
                            )
                        )
                    }
                }
                stage(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Preview
@Composable
private fun DemoScreenPreview() {
    LabTheme {
        val demo = remember {
            Demo(
                id = "preview.demo",
                title = Res.string.demo_navbar,
                script = demoScript {},
                content = { Box(Modifier.fillMaxSize().background(LabTheme.colors.surface)) },
            )
        }
        DemoScreen(demo = demo, record = false, label = "iOS", onBack = {})
    }
}
