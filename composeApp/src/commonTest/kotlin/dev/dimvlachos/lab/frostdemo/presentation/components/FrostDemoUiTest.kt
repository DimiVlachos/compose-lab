package dev.dimvlachos.lab.frostdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.dimvlachos.lab.core.audio.FakeMicrophone
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.audio.noise
import dev.dimvlachos.lab.core.audio.silence
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.frost.Breath
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.frost_hint_blow
import dev.dimvlachos.lab.resources.frost_hint_hold
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class FrostDemoUiTest {
    @Test
    fun scriptedWipesDrawStrokesAndTheLoopRefrosts() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        val frost = FrostState()
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            // The clip's own 4:5 frame, so the script's points land where they say.
            LabTheme {
                Box(Modifier.size(400.dp, 500.dp)) {
                    FrostDemo(state, frost, MicAccess.Unavailable)
                }
            }
        }
        mainClock.advanceTimeByFrame()
        runOnUiThread {
            state.select(1)
            scope!!.launch {
                state.wipe(listOf(Offset(0.1f, 0.5f), Offset(0.9f, 0.5f)), 400.milliseconds)
            }
        }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(600)

        assertEquals(1, frost.strokes.size)
        val stroke = frost.strokes.single()
        assertTrue(
            (stroke.first() - Offset(0.1f, 0.5f)).getDistance() < 0.001f,
            "${stroke.first()}",
        )
        assertTrue(stroke.last().x > 0.89f, "the finger reaches the end: $stroke")

        runOnUiThread { state.select(0) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeByFrame()

        assertTrue(frost.strokes.isEmpty())
    }

    @Test
    fun aScriptedBreathFogsTheGlassOver() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        val frost = FrostState()
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            LabTheme {
                Box(Modifier.size(400.dp, 500.dp)) {
                    FrostDemo(state, frost, MicAccess.Unavailable)
                }
            }
        }
        mainClock.advanceTimeByFrame()
        // Wiped once the demo is up: on its first composition the demo starts from fresh frost.
        runOnUiThread { frost.beginStroke(Offset(0.5f, 0.5f)) }
        runOnUiThread { scope!!.launch { state.breathe(2_400.milliseconds, 1f) } }
        mainClock.advanceTimeBy(2_600)

        assertTrue(frost.strokes.isEmpty(), "the wipe is fogged over: ${frost.marks}")
    }

    // Test hosts are not guaranteed to be resumed; the microphone only listens when they are.
    private class ResumedOwner : LifecycleOwner {
        override val lifecycle =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private val blowHint = runBlocking { getString(Res.string.frost_hint_blow) }
    private val holdHint = runBlocking { getString(Res.string.frost_hint_hold) }

    private fun ComposeUiTest.showDemo(state: DemoState, frost: FrostState, micAccess: MicAccess) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp).testTag("demo")) {
                        FrostDemo(state, frost, micAccess)
                    }
                }
            }
        }
    }

    private fun micHearing(frames: Flow<FloatArray>) = MicAccess.Granted(FakeMicrophone(frames))

    @Test
    fun aBlowIntoTheMicrophoneFogsTheGlass() = runComposeUiTest {
        val random = Random(3)
        val frost = FrostState()
        showDemo(
            DemoState(),
            frost,
            micHearing(
                flow {
                    repeat(20) { emit(noise(0.003f, random)) }
                    repeat(12) { emit(noise(0.3f, random)) }
                    awaitCancellation()
                }
            ),
        )
        waitForIdle()

        val fog = frost.marks.last() as Breath
        assertTrue(fog.level > 0f, "the fog rose: ${fog.level}")
        onNodeWithText(blowHint).assertDoesNotExist()
    }

    @Test
    fun aListeningMicrophoneInvitesABlow() = runComposeUiTest {
        showDemo(DemoState(), FrostState(), micHearing(flow { awaitCancellation() }))
        onNodeWithText(blowHint).assertExists()
    }

    @Test
    fun aMicrophoneThatFailsFallsBackToHolding() = runComposeUiTest {
        showDemo(
            DemoState(),
            FrostState(),
            micHearing(flow { throw IllegalStateException("busy") }),
        )
        onNodeWithText(holdHint).assertExists()
    }

    @Test
    fun aMicrophoneThatHearsOnlySilenceFallsBackToHolding() = runComposeUiTest {
        showDemo(
            DemoState(),
            FrostState(),
            micHearing(
                flow {
                    repeat(80) { emit(silence()) }
                    awaitCancellation()
                }
            ),
        )
        onNodeWithText(holdHint).assertExists()
    }

    @Test
    fun withoutAMicrophoneHoldingTheGlassFogsIt() = runComposeUiTest {
        mainClock.autoAdvance = false
        val frost = FrostState()
        showDemo(DemoState(), frost, MicAccess.Unavailable)
        mainClock.advanceTimeByFrame()
        onNodeWithText(holdHint).assertExists()

        onNodeWithTag("demo").performTouchInput { down(center) }
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag("demo").performTouchInput { up() }
        mainClock.advanceTimeByFrame()

        val fog = frost.marks.last() as Breath
        assertTrue(fog.level > 0.2f, "about 0.6 s of breath at 0.7: ${fog.level}")
    }

    @Test
    fun theRecordingShowsNoHints() = runComposeUiTest {
        showDemo(DemoState(recording = true), FrostState(), MicAccess.Unavailable)
        onNodeWithText(holdHint).assertDoesNotExist()
        onNodeWithText(blowHint).assertDoesNotExist()
    }
}
