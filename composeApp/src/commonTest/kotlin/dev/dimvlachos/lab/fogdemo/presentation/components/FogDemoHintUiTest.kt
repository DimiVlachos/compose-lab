package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
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
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fog_hint_camera
import dev.dimvlachos.lab.resources.fog_hint_wipe
import dev.dimvlachos.lab.resources.mirror_card_not_now
import dev.dimvlachos.lab.resources.mirror_card_title
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

// A fully fogged mirror first asks to be wiped; after that, a pill stays only while the camera can
// still be asked for, as the way back to the card.
@OptIn(ExperimentalTestApi::class)
class FogDemoHintUiTest {
    // The hints do not wait on the room: paused, its mist stays off rather than running on through
    // every wait for idle.
    private class PausedOwner : LifecycleOwner {
        override val lifecycle =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.STARTED }
    }

    private val wipeHint = runBlocking { getString(Res.string.fog_hint_wipe) }
    private val cameraHint = runBlocking { getString(Res.string.fog_hint_camera) }
    private val notNow = runBlocking { getString(Res.string.mirror_card_not_now) }
    private val cardTitle = runBlocking { getString(Res.string.mirror_card_title) }

    private fun ComposeUiTest.showDemo(camera: CameraAccess, fog: FogState = FogState()) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides PausedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp).testTag("demo")) {
                        FogDemo(DemoState(), fog, camera)
                    }
                }
            }
        }
    }

    @Test
    fun theFoggedMirrorFirstInvitesAWipe() = runComposeUiTest {
        showDemo(CameraAccess.Unavailable)

        onNodeWithText(wipeHint).assertExists()
        onNodeWithText(cameraHint).assertDoesNotExist()
    }

    @Test
    fun theWipeHintStaysGoneAfterTheMistFogsTheWipeOver() = runComposeUiTest {
        val fog = FogState()
        showDemo(CameraAccess.Unavailable, fog)

        onNodeWithTag("demo").performTouchInput {
            swipe(percentOffset(0.25f, 0.4f), percentOffset(0.75f, 0.4f))
        }
        waitForIdle()
        runOnUiThread { fog.setMistAmount(fog.beginMist(), 1f) }
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
        showDemo(CameraAccess.Unavailable, fog)

        onNodeWithText(wipeHint).assertExists()
    }

    @Test
    fun afterAWipeWithNothingToAskForTheMirrorShowsNoHint() = runComposeUiTest {
        showDemo(CameraAccess.Unavailable)

        onNodeWithTag("demo").performTouchInput {
            swipe(percentOffset(0.25f, 0.4f), percentOffset(0.75f, 0.4f))
        }
        waitForIdle()

        onNodeWithText(wipeHint).assertDoesNotExist()
        onNodeWithText(cameraHint).assertDoesNotExist()
    }

    @Test
    fun beforeAWipeTheWipeHintIsTheWayBackToTheCard() = runComposeUiTest {
        showDemo(CameraAccess.Askable {})
        onNodeWithText(notNow).performTouchInput { click() }
        onNodeWithText(cardTitle).assertDoesNotExist()

        onNodeWithText(wipeHint).performTouchInput { click() }
        onNodeWithText(cardTitle).assertExists()
    }

    @Test
    fun afterAWipeACameraStillToAskForLeavesAPillBackToTheCard() = runComposeUiTest {
        showDemo(CameraAccess.Askable {})
        onNodeWithText(notNow).performTouchInput { click() }
        onNodeWithTag("demo").performTouchInput {
            swipe(percentOffset(0.25f, 0.4f), percentOffset(0.75f, 0.4f))
        }
        waitForIdle()
        onNodeWithText(cardTitle).assertDoesNotExist()

        onNodeWithText(cameraHint).performTouchInput { click() }
        onNodeWithText(cardTitle).assertExists()
    }

    @Test
    fun aCameraTurnedOffInSettingsAlsoLeavesAPillBackToTheCard() = runComposeUiTest {
        showDemo(CameraAccess.Blocked {})
        onNodeWithText(notNow).performTouchInput { click() }
        onNodeWithTag("demo").performTouchInput {
            swipe(percentOffset(0.25f, 0.4f), percentOffset(0.75f, 0.4f))
        }
        waitForIdle()
        onNodeWithText(cardTitle).assertDoesNotExist()

        onNodeWithText(cameraHint).performTouchInput { click() }
        onNodeWithText(cardTitle).assertExists()
    }

    @Test
    fun aReplayedClipShowsNoHints() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides PausedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 500.dp)) {
                        FogDemo(DemoState(replay = true), FogState(), CameraAccess.Askable {})
                    }
                }
            }
        }
        onNodeWithText(wipeHint).assertDoesNotExist()
        onNodeWithText(cameraHint).assertDoesNotExist()
    }
}
