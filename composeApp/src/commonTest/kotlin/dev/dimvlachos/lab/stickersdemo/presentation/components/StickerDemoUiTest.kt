package dev.dimvlachos.lab.stickersdemo.presentation.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.stickers.StarStickerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class StickerDemoUiTest {
    private fun ComposeUiTest.showDemo(reduced: Boolean = false): StarStickerState {
        val stickers = StarStickerState()
        setContent { StickerDemo(DemoState(), stickers, reducedMotion = reduced) }
        return stickers
    }

    @Test
    fun submitWaitsForARating() = runComposeUiTest {
        val stickers = showDemo()
        assertEquals(
            "No rating yet",
            onNodeWithTag("stickers")
                .fetchSemanticsNode()
                .config[SemanticsProperties.StateDescription],
        )
        onNodeWithTag("submit").assertIsNotEnabled()
        onNodeWithTag("stickers").performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        waitForIdle()
        assertEquals(4, stickers.target)
        onNodeWithTag("submit").assertIsEnabled()
    }

    @Test
    fun theLabelFollowsTheStarsAsTheyLand() = runComposeUiTest {
        val stickers = showDemo()
        mainClock.autoAdvance = false
        onNodeWithTag("stickers").performSemanticsAction(SemanticsActions.SetProgress) { it(5f) }
        mainClock.advanceTimeBy(500)
        // Only the first stars are down yet: the label, which reads what has landed, doesn't run
        // ahead to "Loved it!".
        assertEquals(5, stickers.target)
        assertTrue(stickers.landed in 1..2, "${stickers.landed} landed")
        mainClock.advanceTimeBy(4_000)
        assertEquals(5, stickers.landed)
    }

    @Test
    fun dragAcrossTheRowLaysDownAStarPerCentrePassed() = runComposeUiTest {
        val stickers = showDemo()
        onNodeWithTag("stickers").performTouchInput {
            swipe(
                androidx.compose.ui.geometry.Offset(left + 4f, top + 60f),
                androidx.compose.ui.geometry.Offset(centerX, top + 60f),
                durationMillis = 400,
            )
        }
        waitForIdle()
        assertEquals(3, stickers.target)
    }

    @Test
    fun theKeysStepTheRating() = runComposeUiTest {
        val stickers = showDemo()
        onNodeWithTag("stickers").requestFocus()
        onNodeWithTag("stickers").performKeyInput {
            pressKey(Key.Four)
            pressKey(Key.DirectionRight)
        }
        assertEquals(5, stickers.target)
        onNodeWithTag("stickers").performKeyInput { pressKey(Key.MoveHome) }
        assertEquals(0, stickers.target)
    }

    @Test
    fun aScreenReaderHearsTheScore() = runComposeUiTest {
        showDemo(reduced = true)
        onNodeWithTag("stickers").performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        waitForIdle()
        val node = onNodeWithTag("stickers").fetchSemanticsNode()
        assertEquals("4 stars, Really good", node.config[SemanticsProperties.StateDescription])
        assertEquals(
            listOf("How was everything?"),
            node.config[SemanticsProperties.ContentDescription],
        )
    }

    @Test
    fun submitThanksTheTruckForTheScore() = runComposeUiTest {
        showDemo(reduced = true)
        onNodeWithTag("stickers").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag("submit").performClick()
        onNodeWithText("Thanks! Lupita’s Taco Truck will see your 1 star.").assertExists()
    }
}
