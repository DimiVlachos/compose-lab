package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.Breath
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.Mist
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.fogdemo.bathroom.BathroomMirror
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fog_hint_camera
import dev.dimvlachos.lab.resources.fog_hint_wipe
import dev.dimvlachos.lab.resources.mirror_card_title
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

// The bathroom is for wiping and watching the drops: no camera, and the room mists it back over.
@OptIn(ExperimentalTestApi::class)
class BathroomMirrorUiTest {
    private class ResumedOwner : LifecycleOwner {
        override val lifecycle =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private fun text(resource: org.jetbrains.compose.resources.StringResource) = runBlocking {
        getString(resource)
    }

    private var scope: CoroutineScope? = null

    private fun ComposeUiTest.showBathroom(state: DemoState, fog: FogState) {
        setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp).testTag("demo")) {
                        BathroomMirror(state, fog)
                    }
                }
            }
        }
    }

    @Test
    fun theBathroomAsksForNothing() = runComposeUiTest {
        showBathroom(DemoState(), FogState())
        onNodeWithText(text(Res.string.mirror_card_title)).assertDoesNotExist()
        onNodeWithText(text(Res.string.fog_hint_wipe)).assertExists()
    }

    @Test
    fun afterAWipeTheBathroomShowsNoHint() = runComposeUiTest {
        showBathroom(DemoState(), FogState())
        onNodeWithTag("demo").performTouchInput {
            swipe(percentOffset(0.25f, 0.4f), percentOffset(0.75f, 0.4f))
        }
        waitForIdle()

        onNodeWithText(text(Res.string.fog_hint_wipe)).assertDoesNotExist()
        onNodeWithText(text(Res.string.fog_hint_camera)).assertDoesNotExist()
    }

    @Test
    fun holdingTheBathroomGlassDoesNotBreatheOnIt() = runComposeUiTest {
        mainClock.autoAdvance = false
        val fog = FogState()
        showBathroom(DemoState(), fog)
        mainClock.advanceTimeByFrame()

        onNodeWithTag("demo").performTouchInput { down(center) }
        mainClock.advanceTimeBy(1_500)
        onNodeWithTag("demo").performTouchInput { up() }
        mainClock.advanceTimeByFrame()

        assertTrue(fog.marks.none { it is Breath }, "${fog.marks}")
    }

    @Test
    fun theClipsMistFogsTheWholeGlassBackOver() = runComposeUiTest {
        mainClock.autoAdvance = false
        val fog = FogState()
        fog.beginStroke(Offset(0.2f, 0.5f)).also { fog.extendStroke(it, Offset(0.8f, 0.5f)) }
        val state = DemoState(recording = true)
        showBathroom(state, fog)
        mainClock.advanceTimeByFrame()
        runOnUiThread { scope!!.launch { state.mist(1.seconds) } }
        mainClock.advanceTimeBy(600)
        assertTrue(fog.marks.any { it is WipeStroke }, "still misting: ${fog.marks}")

        mainClock.advanceTimeBy(800)
        assertTrue(fog.marks.none { it is WipeStroke }, "fogged right over: ${fog.marks}")
    }

    @Test
    fun theReplayDoesNotMistTheClipOverByItself() = runComposeUiTest {
        mainClock.autoAdvance = false
        val fog = FogState()
        fog.beginStroke(Offset(0.2f, 0.5f)).also { fog.extendStroke(it, Offset(0.8f, 0.5f)) }
        showBathroom(DemoState(replay = true), fog)
        mainClock.advanceTimeBy(10_000)
        assertTrue(fog.marks.none { it is Mist }, "only the script mists it: ${fog.marks}")
    }

    @Test
    fun theWipedBathroomMirrorMistsBackOverByItself() = runComposeUiTest {
        mainClock.autoAdvance = false
        val fog = FogState()
        showBathroom(DemoState(), fog)
        mainClock.advanceTimeByFrame()
        onNodeWithTag("demo").performTouchInput {
            swipe(percentOffset(0.25f, 0.4f), percentOffset(0.75f, 0.4f))
        }
        mainClock.advanceTimeByFrame()
        assertTrue(fog.marks.any { it is WipeStroke && it.clarity >= 1f })

        mainClock.advanceTimeBy(32_000)
        assertTrue(
            fog.marks.none { it is WipeStroke && it.clarity >= 1f },
            "fogged over again: ${fog.marks}",
        )
    }

    @Test
    fun theRecordingDoesNotMistTheClipOver() = runComposeUiTest {
        mainClock.autoAdvance = false
        val fog = FogState()
        fog.beginStroke(Offset(0.2f, 0.5f)).also { fog.extendStroke(it, Offset(0.8f, 0.5f)) }
        showBathroom(DemoState(recording = true), fog)
        mainClock.advanceTimeBy(10_000)
        assertTrue(fog.marks.none { it is Mist }, "${fog.marks}")
    }
}
