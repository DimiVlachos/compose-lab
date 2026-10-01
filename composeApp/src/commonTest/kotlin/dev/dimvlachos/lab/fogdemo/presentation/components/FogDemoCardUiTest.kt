package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.mirror_card_allow
import dev.dimvlachos.lab.resources.mirror_card_body_what
import dev.dimvlachos.lab.resources.mirror_card_camera_why
import dev.dimvlachos.lab.resources.mirror_card_title
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

// The card asks for the camera, which is the mirror; the rest of its behaviour is in FogDemoUiTest.
@OptIn(ExperimentalTestApi::class)
class FogDemoCardUiTest {
    private class ResumedOwner : LifecycleOwner {
        override val lifecycle =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private fun text(resource: org.jetbrains.compose.resources.StringResource) = runBlocking {
        getString(resource)
    }

    private val title = text(Res.string.mirror_card_title)
    private val allow = text(Res.string.mirror_card_allow)
    private val cameraWhy = text(Res.string.mirror_card_camera_why)

    private var camera by mutableStateOf<CameraAccess>(CameraAccess.Unavailable)
    private val asked = mutableListOf<String>()

    private fun askableCamera() = CameraAccess.Askable { asked += "camera" }

    private fun ComposeUiTest.showDemo() {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 700.dp)) {
                        FogDemo(DemoState(), FogState(), camera)
                    }
                }
            }
        }
    }

    @Test
    fun theCardExplainsTheCamera() = runComposeUiTest {
        camera = askableCamera()
        showDemo()

        onNodeWithText(title).assertExists()
        onNodeWithText(cameraWhy).assertExists()
    }

    @Test
    fun aSecondTapWhileAndroidAsksDoesNotAskAgain() = runComposeUiTest {
        camera = askableCamera()
        showDemo()

        onNodeWithText(allow).performTouchInput { click() }
        onNodeWithText(allow).performTouchInput { click() }
        waitForIdle()

        assertEquals(listOf("camera"), asked)
    }

    @Test
    fun inLandscapeTheCardScrollsSoAllowCanStillBeTapped() = runComposeUiTest {
        camera = askableCamera()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(700.dp, 320.dp)) {
                        FogDemo(DemoState(), FogState(), camera)
                    }
                }
            }
        }

        onNodeWithText(allow).performScrollTo().performTouchInput { click() }
        waitForIdle()
        assertEquals(listOf("camera"), asked)
    }

    @Test
    fun theCardSaysTheSteamComesBackByItself() = runComposeUiTest {
        camera = askableCamera()
        showDemo()
        onNodeWithText(text(Res.string.mirror_card_body_what)).assertExists()
    }
}
