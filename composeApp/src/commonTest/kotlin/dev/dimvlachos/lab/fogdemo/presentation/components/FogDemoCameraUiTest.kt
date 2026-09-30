package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.camera.FakeMirrorCamera
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Clear glass, so the middle of the demo shows exactly what is behind it: the live mirror (a
// magenta camera) or the still reflection.
@OptIn(ExperimentalTestApi::class)
class FogDemoCameraUiTest {
    private class Owner(state: Lifecycle.State = Lifecycle.State.RESUMED) : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = state }
    }

    private fun ComposeUiTest.showMirror(
        access: () -> CameraAccess,
        owner: Owner = Owner(),
    ) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                LabTheme {
                    Box(Modifier.size(200.dp, 250.dp).testTag("demo")) {
                        FogDemo(
                            DemoState(),
                            FogState(startClear = true),
                            MicAccess.Unavailable,
                            access(),
                        )
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.middleIsMagenta(): Boolean {
        waitForIdle()
        val pixels = onNodeWithTag("demo").captureToImage().toPixelMap()
        val middle = pixels[pixels.width / 2, pixels.height / 2]
        return abs(middle.red - Color.Magenta.red) < 0.1f &&
            abs(middle.green - Color.Magenta.green) < 0.1f &&
            abs(middle.blue - Color.Magenta.blue) < 0.1f
    }

    @Test
    fun aGrantedCameraShowsTheLiveMirror() = runComposeUiTest {
        val camera = FakeMirrorCamera()
        showMirror({ CameraAccess.Granted(camera) })

        assertTrue(middleIsMagenta())
        assertEquals(1, camera.running)
    }

    @Test
    fun aCameraThatFailsLeavesTheStillReflection() = runComposeUiTest {
        val camera = FakeMirrorCamera(failure = IllegalStateException("in use"))
        showMirror({ CameraAccess.Granted(camera) })

        assertFalse(middleIsMagenta())
        assertEquals(1, camera.starts)
    }

    @Test
    fun withoutACameraTheMirrorShowsTheStillReflection() = runComposeUiTest {
        showMirror({ CameraAccess.Unavailable })
        assertFalse(middleIsMagenta())
    }

    @Test
    fun theCameraRunsOnlyWhileTheDemoIsInFront() = runComposeUiTest {
        val camera = FakeMirrorCamera()
        val owner = Owner(Lifecycle.State.STARTED)
        showMirror({ CameraAccess.Granted(camera) }, owner)
        waitForIdle()
        assertEquals(0, camera.running)

        runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        waitForIdle()
        assertEquals(1, camera.running)

        runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        waitForIdle()
        assertEquals(0, camera.running)
    }

    @Test
    fun aPausedMirrorKeepsYourLastFrameRatherThanFlashingTheStill() = runComposeUiTest {
        val camera = FakeMirrorCamera()
        val owner = Owner()
        showMirror({ CameraAccess.Granted(camera) }, owner)
        assertTrue(middleIsMagenta())

        runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        assertTrue(middleIsMagenta())
    }

    @Test
    fun recomposingKeepsTheSameCameraRunning() = runComposeUiTest {
        val camera = FakeMirrorCamera()
        var recomposition by mutableIntStateOf(0)
        // A fresh wrapper around the same camera at every recomposition, as on Android.
        showMirror({ recomposition.let { CameraAccess.Granted(camera) } })
        waitForIdle()
        recomposition++
        waitForIdle()

        assertEquals(1, camera.starts)
    }
}
