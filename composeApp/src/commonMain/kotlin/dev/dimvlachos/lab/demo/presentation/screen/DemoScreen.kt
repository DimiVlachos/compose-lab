package dev.dimvlachos.lab.demo.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.RecordingLog
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.platform.LockLandscape
import dev.dimvlachos.lab.core.presentation.components.topbar.LabIconButton
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBar
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBarTitle
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_back
import dev.dimvlachos.lab.resources.action_replay
import dev.dimvlachos.lab.resources.action_stop
import dev.dimvlachos.lab.resources.demo_navbar
import dev.dimvlachos.lab.resources.ic_arrow_back
import dev.dimvlachos.lab.resources.ic_play_arrow
import dev.dimvlachos.lab.resources.ic_stop
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
            // The clip's frame, centred; the recorder crops to it: 4:5, 9:16 for a tall demo, or
            // 16:9 for a demo that runs in landscape.
            val frame =
                when {
                    demo.landscape -> Modifier.fillMaxHeight().aspectRatio(16f / 9f)
                    demo.tall -> Modifier.fillMaxWidth().aspectRatio(9f / 16f)
                    else -> Modifier.fillMaxWidth().aspectRatio(4f / 5f)
                }
            stage(Modifier.align(Alignment.Center).then(frame))
        } else {
            // On the phone the demo is something to use, so it fills the screen under the bar:
            // a 4:5 frame would leave bands above and below it that look like the demo but never
            // reach it.
            Column(Modifier.fillMaxSize()) {
                LabTopBar(
                    navigationIcon =
                        onBack?.let { back ->
                            {
                                LabIconButton(
                                    Res.drawable.ic_arrow_back,
                                    contentDescription = stringResource(Res.string.action_back),
                                    onClick = back,
                                )
                            }
                        },
                    actions = {
                        LabIconButton(
                            if (playing) Res.drawable.ic_stop else Res.drawable.ic_play_arrow,
                            contentDescription =
                                stringResource(
                                    if (playing) Res.string.action_stop
                                    else Res.string.action_replay
                                ),
                            onClick = {
                                if (playing) {
                                    playing = false
                                    state.endReplay()
                                } else {
                                    runId++
                                    playing = true
                                }
                            },
                            tint = LabTheme.colors.accent,
                        )
                    },
                ) {
                    LabTopBarTitle(stringResource(demo.title))
                }
                // The bar has the top: the demo keeps its controls clear of the sides and the
                // foot itself, through demoInsets.
                stage(
                    Modifier.fillMaxWidth()
                        .weight(1f)
                        .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                )
            }
        }
    }
}

@Composable
private fun DemoScreenSample() {
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

@Preview
@Composable
private fun DemoScreenLightPreview() {
    LabTheme(darkTheme = false) { DemoScreenSample() }
}

@Preview
@Composable
private fun DemoScreenDarkPreview() {
    LabTheme(darkTheme = true) { DemoScreenSample() }
}
