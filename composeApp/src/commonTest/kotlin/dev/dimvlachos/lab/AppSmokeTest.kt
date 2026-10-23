package dev.dimvlachos.lab

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import dev.dimvlachos.lab.core.platform.InMemorySoundSettingStore
import dev.dimvlachos.lab.core.platform.InMemoryThemeModeStore
import dev.dimvlachos.lab.core.platform.platformLabel
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
        onNodeWithText("Watch").assertExists()
        onNodeWithContentDescription("Back").performClick()
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
        // Home slides back in under the demo as it slides off.
        mainClock.advanceTimeBy(1_000)
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun aBackSwipeSlidesTheDemoOffUnderTheFingerAndCanBeCalledOff() = runComposeUiTest {
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
        // Halfway through a swipe, home is back behind the demo, and the demo is still there.
        runOnUiThread {
            input.backStarted(NavigationEvent(NavigationEvent.EDGE_LEFT, progress = 0f))
            input.backProgressed(NavigationEvent(NavigationEvent.EDGE_LEFT, progress = 0.5f))
        }
        mainClock.advanceTimeBy(100)
        onNodeWithText("compose-lab").assertExists()
        onNodeWithText("Watch").assertExists()
        // Called off, it slides back over home, and home is gone again.
        runOnUiThread { input.backCancelled() }
        mainClock.advanceTimeBy(1_000)
        onNodeWithText("compose-lab").assertDoesNotExist()
        onNodeWithText("Watch").assertExists()
        // Swiped all the way and let go, it is home.
        runOnUiThread {
            input.backStarted(NavigationEvent(NavigationEvent.EDGE_LEFT, progress = 0f))
            input.backProgressed(NavigationEvent(NavigationEvent.EDGE_LEFT, progress = 0.8f))
            input.backCompleted()
        }
        mainClock.advanceTimeBy(1_000)
        onNodeWithText("compose-lab").assertExists()
        onNodeWithText("Watch").assertDoesNotExist()
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
        onNodeWithContentDescription("Back").performClick()
        onNodeWithText("Your reflection").assertExists()

        onNodeWithContentDescription("Back").performClick()
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

    @Test
    fun theSearchFindsADemoInsideItsFolderAndBackClosesIt() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithContentDescription("Search").performClick()
        onNode(hasSetTextAction()).performTextInput("bath")
        waitForIdle()
        onNodeWithText("Bathroom").assertExists()
        onNodeWithText("in Fogged mirror").assertExists()
        onNodeWithText("Nav bar").assertDoesNotExist()
        onNodeWithText("compose-lab").assertDoesNotExist()
        onNodeWithContentDescription("Back").performClick()
        waitForIdle()
        onNodeWithText("compose-lab").assertExists()
        onNodeWithText("Nav bar").assertExists()
    }

    @Test
    fun aSearchWithNoMatchSaysSo() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithContentDescription("Search").performClick()
        onNode(hasSetTextAction()).performTextInput("zzqx")
        waitForIdle()
        onNodeWithText("No demos match your search").assertExists()
        onNodeWithContentDescription("Clear search").performClick()
        waitForIdle()
        onNodeWithText("Nav bar").assertExists()
    }

    @Test
    fun aDemoOpenedFromTheSearchGoesBackToIt() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithContentDescription("Search").performClick()
        onNode(hasSetTextAction()).performTextInput("plane")
        waitForIdle()
        onNodeWithText("Paper plane").performClick()
        waitForIdle()
        onNodeWithContentDescription("Back").performClick()
        waitForIdle()
        onNodeWithText("Paper plane").assertExists()
        onNodeWithText("Nav bar").assertDoesNotExist()
    }

    @Test
    fun systemBackClosesTheSearchAndClearsIt() = runComposeUiTest {
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
        onNodeWithContentDescription("Search").performClick()
        onNode(hasSetTextAction()).performTextInput("bath")
        waitForIdle()
        runOnUiThread { input.backCompleted() }
        waitForIdle()
        onNodeWithText("compose-lab").assertExists()
        onNodeWithText("Nav bar").assertExists()
        onNodeWithContentDescription("Search").performClick()
        waitForIdle()
        onNodeWithText("bath").assertDoesNotExist()
    }

    @Test
    fun aTagShowsOnlyItsDemosUntilItIsTappedAgain() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithText("Camera").performClick()
        waitForIdle()
        onNodeWithText("Your reflection").assertExists()
        onNodeWithText("Nav bar").assertDoesNotExist()
        onNodeWithText("Camera").performClick()
        waitForIdle()
        onNodeWithText("Nav bar").assertExists()
        onNodeWithText("Your reflection").assertDoesNotExist()
    }

    @Test
    fun theThemeButtonCyclesTheModesAndRemembersEach() = runComposeUiTest {
        val store = InMemoryThemeModeStore()
        setContent {
            LabApp(initialDemoId = null, record = false, label = false, themeStore = store)
        }
        onNodeWithContentDescription("Theme: follow system").performClick()
        onNodeWithContentDescription("Theme: light").assertExists()
        assertEquals(ThemeMode.Light, store.load())
        onNodeWithContentDescription("Theme: light").performClick()
        onNodeWithContentDescription("Theme: dark").assertExists()
        assertEquals(ThemeMode.Dark, store.load())
        onNodeWithContentDescription("Theme: dark").performClick()
        onNodeWithContentDescription("Theme: follow system").assertExists()
        assertEquals(ThemeMode.System, store.load())
    }

    @Test
    fun soundStartsOffAndTheHomeSwitchFlipsAndRemembersIt() = runComposeUiTest {
        val store = InMemorySoundSettingStore()
        setContent {
            LabApp(initialDemoId = null, record = false, label = false, soundStore = store)
        }
        onNodeWithContentDescription("Sound").assertIsOff()
        onNodeWithContentDescription("Sound").performClick()
        onNodeWithContentDescription("Sound").assertIsOn()
        assertTrue(store.load())
        onNodeWithContentDescription("Sound").performClick()
        onNodeWithContentDescription("Sound").assertIsOff()
        assertFalse(store.load())
    }

    @Test
    fun aDemosSwitchStartsFromTheAppsAndLeavesItAsItWas() = runComposeUiTest {
        val store = InMemorySoundSettingStore(on = true)
        setContent {
            LabApp(initialDemoId = null, record = false, label = false, soundStore = store)
        }
        onNodeWithText("Profile gallery").performClick()
        onNodeWithText("compose-lab").assertDoesNotExist()
        onNodeWithContentDescription("Sound").assertIsOn()
        onNodeWithContentDescription("Sound").performClick()
        onNodeWithContentDescription("Sound").assertIsOff()
        assertTrue(store.load(), "the app's switch stays on")
        onNodeWithContentDescription("Back").performClick()
        onNodeWithText("compose-lab").assertExists()
        onNodeWithContentDescription("Sound").assertIsOn()
        onNodeWithText("Profile gallery").performClick()
        onNodeWithContentDescription("Sound").assertIsOn()
    }

    @Test
    fun aSavedModeIsTheOneTheAppStartsIn() = runComposeUiTest {
        val store = InMemoryThemeModeStore(ThemeMode.Dark)
        setContent {
            LabApp(initialDemoId = null, record = false, label = false, themeStore = store)
        }
        onNodeWithContentDescription("Theme: dark").assertExists()
    }

    @Test
    fun theSearchFieldTakesFocusWhenOpenedButNotOnTheWayBackFromADemo() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithContentDescription("Search").performClick()
        waitForIdle()
        onNode(hasSetTextAction()).assertIsFocused()
        onNode(hasSetTextAction()).performTextInput("plane")
        onNodeWithText("Paper plane").performClick()
        waitForIdle()
        onNodeWithContentDescription("Back").performClick()
        waitForIdle()
        onNode(hasSetTextAction()).assertIsNotFocused()
    }
}
