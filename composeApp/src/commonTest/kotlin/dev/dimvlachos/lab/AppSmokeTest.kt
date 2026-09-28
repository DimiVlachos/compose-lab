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
        setContent { App(initialDemoId = "navbar.indicator", record = false, label = false) }
        onNodeWithText("compose-lab").assertDoesNotExist()
        onNodeWithText("Morphing indicator").assertExists()
    }

    @Test
    fun theCatalogListsSectionsAndEachSectionItsDemos() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithText("Nav bar").assertExists()
        onNodeWithText("4 demos").assertExists()
        onNodeWithText("Profile gallery").assertDoesNotExist()
        onNodeWithText("Image morph").performClick()
        onNodeWithText("Profile gallery").assertExists()
        onNodeWithText("Morphing indicator").assertDoesNotExist()
        onNodeWithText("Back").performClick()
        onNodeWithText("Nav bar").assertExists()
    }

    @Test
    fun backFromADemoReturnsToItsSection() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { App(initialDemoId = "morph.app", record = false, label = false) }
        onAllNodesWithText("Back").onFirst().performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithText("Bounds only").assertExists()
        onNodeWithText("Morphing indicator").assertDoesNotExist()
    }

    @Test
    fun systemBackStepsFromADemoToItsSectionThenHome() = runComposeUiTest {
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
        onNodeWithText("Bounds only").assertExists()
        runOnUiThread { input.backCompleted() }
        mainClock.advanceTimeByFrame()
        onNodeWithText("Image morph").assertExists()
        onNodeWithText("Bounds only").assertDoesNotExist()
    }

    @Test
    fun labelShowsThePlatformNameOnTheStage() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { App(initialDemoId = "navbar.indicator", record = true, label = true) }
        val expected = runBlocking { getString(platformLabel) }
        onNodeWithText(expected).assertExists()
    }
}
