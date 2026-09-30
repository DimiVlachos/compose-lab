package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class FogDemoDripUiTest {
    private class Owner(state: Lifecycle.State = Lifecycle.State.RESUMED) : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = state }
    }

    private var scope: CoroutineScope? = null

    private fun ComposeUiTest.showDemo(state: DemoState, fog: FogState, owner: Owner = Owner()) {
        mainClock.autoAdvance = false
        setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        FogDemo(state, fog, MicAccess.Unavailable, CameraAccess.Unavailable)
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
    }

    private fun FogState.streaks() = marks.filterIsInstance<WipeStroke>().filter { it.clarity < 1f }

    @Test
    fun aDropRunsDownTheFoggedMirrorByItself() = runComposeUiTest {
        val fog = FogState()
        showDemo(DemoState(), fog)
        mainClock.advanceTimeBy(10_000)
        assertTrue(fog.streaks().isNotEmpty(), "${fog.marks}")
    }

    @Test
    fun theRecordingStartsNoDropsByItself() = runComposeUiTest {
        val fog = FogState()
        showDemo(DemoState(recording = true), fog)
        mainClock.advanceTimeBy(20_000)
        assertTrue(fog.streaks().isEmpty(), "${fog.marks}")
    }

    @Test
    fun theScriptsDripRunsInTheRecording() = runComposeUiTest {
        val fog = FogState()
        val state = DemoState(recording = true)
        showDemo(state, fog)
        runOnUiThread { scope!!.launch { state.drip(Offset(0.5f, 0.2f), 0.3f) } }
        mainClock.advanceTimeBy(4_000)
        assertTrue(fog.streaks().isNotEmpty(), "${fog.marks}")
    }

    @Test
    fun noDropsWhileTheDemoIsNotInFront() = runComposeUiTest {
        val fog = FogState()
        showDemo(DemoState(), fog, Owner(Lifecycle.State.STARTED))
        mainClock.advanceTimeBy(20_000)
        assertTrue(fog.streaks().isEmpty(), "${fog.marks}")
    }

    @Test
    fun aDropLeftMidRunCarriesOnOnReturn() = runComposeUiTest {
        val fog = FogState()
        val state = DemoState(recording = true)
        val owner = Owner()
        showDemo(state, fog, owner)
        runOnUiThread { scope!!.launch { state.drip(Offset(0.5f, 0.1f), 0.4f) } }
        mainClock.advanceTimeBy(1_000)
        runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        mainClock.advanceTimeBy(100)
        val points = fog.streaks().sumOf { it.points.size }

        mainClock.advanceTimeBy(3_000)
        assertTrue(fog.streaks().sumOf { it.points.size } == points, "paused while away")

        runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        mainClock.advanceTimeBy(3_000)
        assertTrue(fog.streaks().sumOf { it.points.size } > points, "carries on back in front")
    }
}
