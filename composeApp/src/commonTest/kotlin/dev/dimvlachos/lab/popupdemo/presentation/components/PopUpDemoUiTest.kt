package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun theEndCaptionGoesOnceTheBookTurnsBack() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent { LabTheme { PopUpDemo(state) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Skip").performClick()
        mainClock.advanceTimeBy(4_000)
        onNodeWithText("Καλό ταξίδι!").assertIsDisplayed()
        // The tour plays on and shuts the book: the cover's caption is back.
        state.select(3)
        mainClock.advanceTimeBy(100)
        state.select(0)
        mainClock.advanceTimeBy(5_000)
        onNodeWithText("A pop-up guide to the islands. Open the cover to begin.")
            .assertIsDisplayed()
    }

    @Test
    fun theCaptionAndTheBookStayPutAsTheCaptionChanges() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        // A phone's width, so the longer captions wrap onto more lines than the short ones.
        setContent { LabTheme { Box(Modifier.size(360.dp, 760.dp)) { PopUpDemo(state) } } }
        mainClock.advanceTimeBy(500)
        state.select(1)
        mainClock.advanceTimeBy(3_000)
        val first = onNodeWithText("Find your island").getUnclippedBoundsInRoot().top
        state.select(3)
        mainClock.advanceTimeBy(4_000)
        val last = onNodeWithText("Ride the meltemi").getUnclippedBoundsInRoot().top
        assertEquals(first, last)
    }
}
