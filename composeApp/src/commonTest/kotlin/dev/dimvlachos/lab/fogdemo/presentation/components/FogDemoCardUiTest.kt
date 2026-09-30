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
import dev.dimvlachos.lab.core.audio.FakeMicrophone
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.camera.FakeMirrorCamera
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.mirror_card_allow
import dev.dimvlachos.lab.resources.mirror_card_body_what
import dev.dimvlachos.lab.resources.mirror_card_body_what_hold
import dev.dimvlachos.lab.resources.mirror_card_camera_blocked
import dev.dimvlachos.lab.resources.mirror_card_camera_why
import dev.dimvlachos.lab.resources.mirror_card_mic_why
import dev.dimvlachos.lab.resources.mirror_card_open_settings
import dev.dimvlachos.lab.resources.mirror_card_title
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

// One card for both: the camera is the mirror, the microphone hears the breath. Android can show
// one permission dialog at a time, so Allow asks for each in turn.
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
    private val openSettings = text(Res.string.mirror_card_open_settings)
    private val cameraWhy = text(Res.string.mirror_card_camera_why)
    private val micWhy = text(Res.string.mirror_card_mic_why)
    private val cameraBlocked = text(Res.string.mirror_card_camera_blocked)

    private var camera by mutableStateOf<CameraAccess>(CameraAccess.Unavailable)
    private var mic by mutableStateOf<MicAccess>(MicAccess.Unavailable)
    private val asked = mutableListOf<String>()

    private fun askableCamera() = CameraAccess.Askable { asked += "camera" }

    private fun askableMic() = MicAccess.Askable { asked += "mic" }

    private fun grantedCamera() = CameraAccess.Granted(FakeMirrorCamera())

    private fun grantedMic() = MicAccess.Granted(FakeMicrophone(flow { awaitCancellation() }))

    private fun ComposeUiTest.showDemo() {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 700.dp)) {
                        FogDemo(DemoState(), FogState(), mic, camera)
                    }
                }
            }
        }
    }

    @Test
    fun theCardExplainsTheCameraAndTheMicrophone() = runComposeUiTest {
        camera = askableCamera()
        mic = askableMic()
        showDemo()

        onNodeWithText(title).assertExists()
        onNodeWithText(cameraWhy).assertExists()
        onNodeWithText(micWhy).assertExists()
    }

    @Test
    fun allowAsksForTheCameraThenTheMicrophone() = runComposeUiTest {
        camera = askableCamera()
        mic = askableMic()
        showDemo()

        onNodeWithText(allow).performTouchInput { click() }
        waitForIdle()
        assertEquals(listOf("camera"), asked)

        camera = grantedCamera()
        waitForIdle()
        assertEquals(listOf("camera", "mic"), asked)

        mic = grantedMic()
        waitForIdle()
        onNodeWithText(title).assertDoesNotExist()
    }

    @Test
    fun theCameraWaitsForTheMicrophonesAnswerBeforeStarting() = runComposeUiTest {
        // Android's microphone dialog pauses the app: a camera started under it would blink on
        // and off again.
        val fake = FakeMirrorCamera()
        camera = askableCamera()
        mic = askableMic()
        showDemo()

        onNodeWithText(allow).performTouchInput { click() }
        camera = CameraAccess.Granted(fake)
        waitForIdle()
        assertEquals(0, fake.starts)

        mic = grantedMic()
        waitForIdle()
        assertEquals(1, fake.running)
    }

    @Test
    fun refusingBothClosesTheCard() = runComposeUiTest {
        camera = askableCamera()
        mic = askableMic()
        showDemo()

        onNodeWithText(allow).performTouchInput { click() }
        camera = askableCamera()
        waitForIdle()
        mic = askableMic()
        waitForIdle()

        onNodeWithText(title).assertDoesNotExist()
    }

    @Test
    fun aSecondTapWhileAndroidAsksDoesNotAskAgain() = runComposeUiTest {
        camera = askableCamera()
        mic = askableMic()
        showDemo()

        onNodeWithText(allow).performTouchInput { click() }
        onNodeWithText(allow).performTouchInput { click() }
        waitForIdle()

        assertEquals(listOf("camera"), asked)
    }

    @Test
    fun withTheCameraGrantedOnlyTheMicrophoneIsLeftToAskFor() = runComposeUiTest {
        camera = grantedCamera()
        mic = askableMic()
        showDemo()

        onNodeWithText(micWhy).assertExists()
        onNodeWithText(cameraWhy).assertDoesNotExist()
        onNodeWithText(allow).performTouchInput { click() }
        waitForIdle()
        assertEquals(listOf("mic"), asked)
    }

    @Test
    fun withoutAMicrophoneTheCardStillAsksForTheCamera() = runComposeUiTest {
        camera = askableCamera()
        showDemo()

        onNodeWithText(cameraWhy).assertExists()
        onNodeWithText(micWhy).assertDoesNotExist()
    }

    @Test
    fun aCameraRefusedForGoodKeepsTheCardOnItsSettings() = runComposeUiTest {
        var settings = 0
        camera = askableCamera()
        mic = grantedMic()
        showDemo()

        onNodeWithText(allow).performTouchInput { click() }
        camera = CameraAccess.Blocked { settings++ }
        waitForIdle()

        onNodeWithText(cameraBlocked).assertExists()
        onNodeWithText(openSettings).performTouchInput { click() }
        assertEquals(1, settings)
    }

    @Test
    fun noCardWithBothGranted() = runComposeUiTest {
        camera = grantedCamera()
        mic = grantedMic()
        showDemo()

        onNodeWithText(title).assertDoesNotExist()
    }

    @Test
    fun inLandscapeTheCardScrollsSoAllowCanStillBeTapped() = runComposeUiTest {
        camera = askableCamera()
        mic = askableMic()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(700.dp, 320.dp)) {
                        FogDemo(DemoState(), FogState(), mic, camera)
                    }
                }
            }
        }

        onNodeWithText(allow).performScrollTo().performTouchInput { click() }
        waitForIdle()
        assertEquals(listOf("camera"), asked)
    }

    @Test
    fun withoutAMicrophoneTheCardSaysToHoldNotBlow() = runComposeUiTest {
        camera = askableCamera()
        showDemo()
        onNodeWithText(text(Res.string.mirror_card_body_what_hold)).assertExists()
        onNodeWithText(text(Res.string.mirror_card_body_what)).assertDoesNotExist()
    }
}
