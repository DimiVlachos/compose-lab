package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PopUpDemoUiTest {
    @Test
    fun theTourStartsOnTheShutCoverWithAnOpenButton() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("A pop-up guide to the islands. Open the cover to begin.")
            .assertIsDisplayed()
        onNodeWithText("Open").assertHasClickAction()
    }

    @Test
    fun openingTheCoverShowsTheFirstSpreadsCaption() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Open").performClick()
        mainClock.advanceTimeBy(3_000)
        onNodeWithText("Find your island").assertIsDisplayed()
        onNodeWithText("Next").assertHasClickAction()
    }

    @Test
    fun theScriptsSelectTurnsTheBook() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent { LabTheme { PopUpDemo(state) } }
        mainClock.advanceTimeBy(500)
        state.select(2)
        mainClock.advanceTimeBy(4_000)
        onNodeWithText("Plan the crossing").assertIsDisplayed()
    }
}
