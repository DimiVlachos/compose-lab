package dev.dimvlachos.lab

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import dev.dimvlachos.lab.core.platform.platformLabel
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class AppSmokeTest {
    @Test
    fun showsTheCatalogWhenNoDemoIsRequested() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun unknownDemoFallsBackToTheCatalog() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { App(initialDemoId = "navbar.nope", record = false, label = false) }
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun knownDemoOpensDirectly() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { App(initialDemoId = "navbar.all", record = false, label = false) }
        onNodeWithText("compose-lab").assertDoesNotExist()
        onNodeWithText("Nav bar").assertExists()
    }

    @Test
    fun theCatalogOpensEachDemoDirectly() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithText("Nav bar").assertExists()
        onNodeWithText("Fogged mirror").assertExists()
        onNodeWithText("Profile gallery").performClick()
        onNodeWithText("compose-lab").assertDoesNotExist()
        onNodeWithText("Stop").assertExists()
        onAllNodesWithText("Back").onFirst().performClick()
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun systemBackFromADemoReturnsHome() = runComposeUiTest {
        mainClock.autoAdvance = false
        val input = DirectNavigationEventInput()
        setContent {
            val owner = rememberNavigationEventDispatcherOwner(parent = null)
            DisposableEffect(owner) {
                owner.navigationEventDispatcher.addInput(input)
                onDispose { owner.navigationEventDispatcher.removeInput(input) }
            }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                App(initialDemoId = "morph.app", record = false, label = false)
            }
        }
        runOnUiThread { input.backCompleted() }
        mainClock.advanceTimeByFrame()
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun labelShowsThePlatformNameOnTheStage() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { App(initialDemoId = "navbar.all", record = true, label = true) }
        val expected = runBlocking { getString(platformLabel) }
        onNodeWithText(expected).assertExists()
    }

    @Test
    fun theFoggedMirrorFolderOpensItsVersionsAndBackStepsOut() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithText("Fogged mirror").performClick()
        onNodeWithText("Bathroom").assertExists()
        onNodeWithText("Your reflection").assertExists()
        onNodeWithText("compose-lab").assertDoesNotExist()

        onNodeWithText("Bathroom").performClick()
        onNodeWithText("Your reflection").assertDoesNotExist()
        onAllNodesWithText("Back").onFirst().performClick()
        onNodeWithText("Your reflection").assertExists()

        onAllNodesWithText("Back").onFirst().performClick()
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun systemBackFromTheFolderReturnsHome() = runComposeUiTest {
        val input = DirectNavigationEventInput()
        setContent {
            val owner = rememberNavigationEventDispatcherOwner(parent = null)
            DisposableEffect(owner) {
                owner.navigationEventDispatcher.addInput(input)
                onDispose { owner.navigationEventDispatcher.removeInput(input) }
            }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                App(initialDemoId = null, record = false, label = false)
            }
        }
        onNodeWithText("Fogged mirror").performClick()
        // The folder screen enables back only once it has composed.
        waitForIdle()
        runOnUiThread { input.backCompleted() }
        waitForIdle()
        onNodeWithText("compose-lab").assertExists()
    }
}
