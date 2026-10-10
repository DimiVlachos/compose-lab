package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun readAgainGoesStraightToTheCoversCaption() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Skip").performClick()
        mainClock.advanceTimeBy(4_000)
        onNodeWithText("Read again").performClick()
        // While the leaves turn back, the caption is the cover's, never a spread passed on the way.
        repeat(12) {
            mainClock.advanceTimeBy(250)
            onNodeWithText("A pop-up guide to the islands. Open the cover to begin.")
                .assertIsDisplayed()
            onAllNodesWithText("Ride the meltemi").assertCountEquals(0)
            onAllNodesWithText("Plan the crossing").assertCountEquals(0)
            onAllNodesWithText("Find your island").assertCountEquals(0)
        }
    }

    @Test
    fun skipGoesStraightToTheEndCaption() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Skip").performClick()
        repeat(12) {
            mainClock.advanceTimeBy(250)
            onNodeWithText("Καλό ταξίδι!").assertIsDisplayed()
            onAllNodesWithText("Find your island").assertCountEquals(0)
            onAllNodesWithText("Plan the crossing").assertCountEquals(0)
        }
    }

    @Test
    fun theBookLiesOnASunlitSurfaceNotOnBlack() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                Box(Modifier.size(360.dp, 760.dp).testTag("screen")) { PopUpDemo(DemoState()) }
            }
        }
        mainClock.advanceTimeBy(500)
        val pixels = onNodeWithTag("screen").captureToImage().toPixelMap()
        // Beside the book, and down by the buttons.
        for ((x, y) in listOf(6 to pixels.height / 3, pixels.width / 2 to pixels.height - 30)) {
            val c = pixels[x, y]
            val light = (c.red + c.green + c.blue) / 3f
            assertTrue(light > 0.75f, "($x, $y) is $c")
        }
    }

    @Test
    fun theSkyFollowsTheSpreadFromMiddayBlueToSunsetWhileTheCaptionStaysLight() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent {
            LabTheme { Box(Modifier.size(360.dp, 760.dp).testTag("screen")) { PopUpDemo(state) } }
        }
        mainClock.advanceTimeBy(500)
        fun sky() = onNodeWithTag("screen").captureToImage().toPixelMap().let { it[6, 6] }
        fun caption() =
            onNodeWithTag("screen").captureToImage().toPixelMap().let {
                it[it.width / 2, it.height - 30]
            }
        state.select(1)
        mainClock.advanceTimeBy(4_000)
        val midday = sky()
        assertTrue(midday.blue > midday.red + 0.05f, "midday sky $midday")
        state.select(3)
        mainClock.advanceTimeBy(5_000)
        val sunset = sky()
        assertTrue(sunset.red > sunset.blue, "sunset sky $sunset")
        val light = caption()
        assertTrue((light.red + light.green + light.blue) / 3f > 0.85f, "caption area $light")
    }
}
