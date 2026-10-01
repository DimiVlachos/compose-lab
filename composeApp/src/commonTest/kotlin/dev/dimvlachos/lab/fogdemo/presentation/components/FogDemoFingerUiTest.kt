package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.camera.FakeMirrorCamera
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// The clip shows where its finger touches, as a phone's "show taps" does: a soft disc that follows
// the scripted wipe, gone once the finger lifts. Clear glass over a magenta camera, so the disc
// shows plainly.
@OptIn(ExperimentalTestApi::class)
class FogDemoFingerUiTest {
    private var scope: CoroutineScope? = null

    private fun ComposeUiTest.showScripted(state: DemoState) {
        mainClock.autoAdvance = false
        setContent {
            scope = rememberCoroutineScope()
            LabTheme {
                Box(Modifier.size(200.dp, 250.dp).testTag("demo")) {
                    FogDemo(
                        state,
                        FogState(startClear = true),
                        CameraAccess.Granted(FakeMirrorCamera()),
                    )
                }
            }
        }
        mainClock.advanceTimeByFrame()
    }

    // The middle of the glass, where the wipe's finger is halfway through.
    private fun ComposeUiTest.middleOfTheGlass(): Color {
        val pixels = onNodeWithTag("demo").captureToImage().toPixelMap()
        val glass = wallGlassOn(Size(pixels.width.toFloat(), pixels.height.toFloat()))
        return pixels[glass.center.x.toInt(), glass.center.y.toInt()]
    }

    private fun Color.isMagenta() = abs(red - 1f) < 0.1f && green < 0.1f && abs(blue - 1f) < 0.1f

    private fun ComposeUiTest.wipeDownTheMiddle(state: DemoState) {
        runOnUiThread {
            scope!!.launch {
                state.wipe(listOf(Offset(0.5f, 0.1f), Offset(0.5f, 0.9f)), 1_000.milliseconds)
            }
        }
        mainClock.advanceTimeBy(500)
    }

    @Test
    fun theClipsFingerShowsWhereItTouches() = runComposeUiTest {
        val state = DemoState(recording = true)
        showScripted(state)
        assertTrue(middleOfTheGlass().isMagenta(), "nothing before the touch")

        wipeDownTheMiddle(state)
        val touching = middleOfTheGlass()
        assertTrue(!touching.isMagenta() && touching.green > 0.2f, "the finger: $touching")

        mainClock.advanceTimeBy(1_500)
        assertTrue(middleOfTheGlass().isMagenta(), "gone once it lifts")
    }

    @Test
    fun theReplaysFingerShowsToo() = runComposeUiTest {
        val state = DemoState(replay = true)
        showScripted(state)
        wipeDownTheMiddle(state)
        assertTrue(!middleOfTheGlass().isMagenta(), "the finger")
    }
}
