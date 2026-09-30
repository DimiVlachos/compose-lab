package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
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
import dev.dimvlachos.lab.core.audio.FakeMicrophone
import dev.dimvlachos.lab.core.audio.MicAccess
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fog_hint_blow
import dev.dimvlachos.lab.resources.fog_hint_hold
import dev.dimvlachos.lab.resources.fog_hint_wipe
import kotlin.test.Test
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

// A fully fogged mirror first asks to be wiped; only then to be breathed on again.
@OptIn(ExperimentalTestApi::class)
class FogDemoHintUiTest {
    private class ResumedOwner : LifecycleOwner {
        override val lifecycle =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private val wipeHint = runBlocking { getString(Res.string.fog_hint_wipe) }
    private val blowHint = runBlocking { getString(Res.string.fog_hint_blow) }
    private val holdHint = runBlocking { getString(Res.string.fog_hint_hold) }

    private fun ComposeUiTest.showDemo(mic: MicAccess, fog: FogState = FogState()) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp).testTag("demo")) {
                        FogDemo(DemoState(), fog, mic, CameraAccess.Unavailable)
                    }
                }
            }
        }
    }

    private fun listening() = MicAccess.Granted(FakeMicrophone(flow { awaitCancellation() }))

    @Test
    fun theFoggedMirrorFirstInvitesAWipe() = runComposeUiTest {
        showDemo(listening())

        onNodeWithText(wipeHint).assertExists()
        onNodeWithText(blowHint).assertDoesNotExist()
    }

    @Test
    fun afterAWipeTheMirrorInvitesABlow() = runComposeUiTest {
        showDemo(listening())

        onNodeWithTag("demo").performTouchInput { swipe(centerLeft, centerRight) }
        waitForIdle()

        onNodeWithText(wipeHint).assertDoesNotExist()
        onNodeWithText(blowHint).assertExists()
    }

    @Test
    fun afterAWipeWithoutAMicrophoneItInvitesAHold() = runComposeUiTest {
        showDemo(MicAccess.Unavailable)

        onNodeWithTag("demo").performTouchInput { swipe(centerLeft, centerRight) }
        waitForIdle()

        onNodeWithText(wipeHint).assertDoesNotExist()
        onNodeWithText(holdHint).assertExists()
    }

    @Test
    fun theWipeHintStaysGoneAfterABreathFogsTheWipeOver() = runComposeUiTest {
        val fog = FogState()
        showDemo(MicAccess.Unavailable, fog)

        onNodeWithTag("demo").performTouchInput { swipe(centerLeft, centerRight) }
        waitForIdle()
        runOnUiThread { fog.setBreathLevel(fog.beginBreath(), 1f) }
        waitForIdle()

        onNodeWithText(wipeHint).assertDoesNotExist()
    }

    @Test
    fun aDropsStreakIsNotTheUsersFirstWipe() = runComposeUiTest {
        val fog = FogState()
        // A running drop leaves a thin, part-clear streak: not a wipe of the user's.
        fog.beginStroke(Offset(0.5f, 0.2f), clarity = 0.85f).also {
            fog.extendStroke(it, Offset(0.5f, 0.4f))
        }
        showDemo(MicAccess.Unavailable, fog)

        onNodeWithText(wipeHint).assertExists()
    }

    @Test
    fun aReplayedClipShowsNoHints() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        FogDemo(
                            DemoState(replay = true),
                            FogState(),
                            MicAccess.Unavailable,
                            CameraAccess.Unavailable,
                        )
                    }
                }
            }
        }
        onNodeWithText(wipeHint).assertDoesNotExist()
        onNodeWithText(holdHint).assertDoesNotExist()
        onNodeWithText(blowHint).assertDoesNotExist()
    }
}
