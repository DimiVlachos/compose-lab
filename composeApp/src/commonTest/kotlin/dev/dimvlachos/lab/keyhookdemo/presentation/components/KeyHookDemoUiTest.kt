package dev.dimvlachos.lab.keyhookdemo.presentation.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyRackState
import dev.dimvlachos.lab.keyhookdemo.Household
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class KeyHookDemoUiTest {
    @Test
    fun startsSignedInAsJordanWithHisShows() = runComposeUiTest {
        setContent { KeyHookDemo(DemoState()) }
        onNodeWithText("Welcome home, Jordan").assertExists()
        onNodeWithText("Harbor Lights").assertExists()
        onNodeWithTag("keys-0").assertContentDescriptionEquals("Sign out Jordan Miller")
        onNodeWithTag("keys-1").assertContentDescriptionEquals("Sign in as Ava Miller")
    }

    @Test
    fun theButtonsSignOutAndInAsTheKeysLand() = runComposeUiTest {
        val rack = KeyRackState(Household.people)
        setContent { KeyHookDemo(DemoState(), rack, reducedMotion = true) }
        onNodeWithTag("keys-0").performClick()
        waitForIdle()
        assertNull(rack.signedIn)
        onNodeWithText("Who’s home?").assertExists()
        onNodeWithText("Your shows appear here once you take your keys.").assertExists()
        onNodeWithTag("keys-3").performClick()
        waitForIdle()
        assertEquals("Grandpa Lou", rack.signedIn?.name)
        onNodeWithText("Welcome home, Lou").assertExists()
        onNodeWithText("Bass Lake Mornings").assertExists()
        onNodeWithTag("keys-3").assertContentDescriptionEquals("Sign out Grandpa Lou")
        onNodeWithTag("keys-0").assertContentDescriptionEquals("Sign in as Jordan Miller")
    }

    @Test
    fun withMotionTheSignInWaitsForTheKeysToLand() = runComposeUiTest {
        val rack = KeyRackState(Household.people)
        setContent { KeyHookDemo(DemoState(), rack, reducedMotion = false) }
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithTag("keys-1").performClick()
        mainClock.advanceTimeBy(100)
        assertEquals("Jordan Miller", rack.signedIn?.name)
        mainClock.advanceTimeBy(2_000)
        assertEquals("Ava Miller", rack.signedIn?.name)
        onNodeWithText("Comet Club").assertExists()
    }
}
