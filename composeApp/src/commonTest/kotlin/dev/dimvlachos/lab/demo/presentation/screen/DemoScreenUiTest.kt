package dev.dimvlachos.lab.demo.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_navbar
import dev.dimvlachos.lab.resources.demo_navbar_description
import dev.dimvlachos.lab.resources.ic_demo_navbar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class DemoScreenUiTest {
    private val demo =
        Demo(
            id = "test.stage",
            title = Res.string.demo_navbar,
            script = demoScript {},
            description = Res.string.demo_navbar_description,
            icon = Res.drawable.ic_demo_navbar,
            tags = setOf(DemoTag.Motion),
            content = { Box(Modifier.fillMaxSize().testTag("stage")) },
        )

    @Test
    fun onThePhoneTheStageFillsEverythingBelowTheBar() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { DemoScreen(demo, record = false, label = null, onBack = {}) } }
        mainClock.advanceTimeBy(100)
        val stage = onNodeWithTag("stage").getBoundsInRoot()
        val root = onRoot().getBoundsInRoot()
        assertEquals(root.bottom, stage.bottom, "the stage reaches the bottom of the screen")
        assertEquals(root.right - root.left, stage.right - stage.left)
        assertTrue(stage.top > root.top, "the stage starts below the bar")
    }

    @Test
    fun whenRecordingTheStageKeepsTheClipsFourByFive() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { DemoScreen(demo, record = true, label = null, onBack = null) } }
        mainClock.advanceTimeBy(100)
        val stage = onNodeWithTag("stage").getBoundsInRoot()
        val ratio = (stage.right - stage.left) / (stage.bottom - stage.top)
        assertEquals(0.8f, ratio, 0.01f)
    }

    // A demo whose tour selects the second item after a moment, then runs on for a second.
    private fun touredDemo(selections: MutableList<Int>, replaying: MutableList<Boolean>) =
        Demo(
            id = "test.toured",
            title = Res.string.demo_navbar,
            script = demoScript { at(100.milliseconds, lasts = 1_000.milliseconds) { select(1) } },
            description = Res.string.demo_navbar_description,
            icon = Res.drawable.ic_demo_navbar,
            tags = setOf(DemoTag.Motion),
            content = { state ->
                selections += state.selectedIndex
                replaying += state.replay
                Box(Modifier.fillMaxSize().testTag("stage"))
            },
        )

    private fun ComposeUiTest.showTouredDemo(
        selections: MutableList<Int> = mutableListOf(),
        replaying: MutableList<Boolean> = mutableListOf(),
    ) = setContent {
        LabTheme {
            DemoScreen(touredDemo(selections, replaying), record = false, label = null, onBack = {})
        }
    }

    @Test
    fun aDemoOpensStillAndItsTourWaitsForWatch() = runComposeUiTest {
        val selections = mutableListOf<Int>()
        showTouredDemo(selections)
        mainClock.advanceTimeBy(2_000)

        assertTrue(selections.none { it == 1 }, "nothing has played: $selections")
        onNodeWithText("Watch").performClick()
        // Past the script's opening hold and its first step.
        mainClock.advanceTimeBy(1_000)
        assertTrue(selections.any { it == 1 }, "Watch plays the tour: $selections")
    }

    @Test
    fun whileTheTourPlaysItSaysSoAndHowToTakeOver() = runComposeUiTest {
        showTouredDemo()
        onNodeWithText(TakeOver).assertDoesNotExist()

        onNodeWithText("Watch").performClick()
        mainClock.advanceTimeBy(300)

        onNodeWithText(TakeOver).assertExists()
        onNodeWithText("Stop").assertExists()
    }

    @Test
    fun aTouchOnTheDemoTakesItOverFromTheTour() = runComposeUiTest {
        val replaying = mutableListOf<Boolean>()
        showTouredDemo(replaying = replaying)
        onNodeWithText("Watch").performClick()
        mainClock.advanceTimeBy(300)
        assertEquals(true, replaying.last(), "the tour plays: $replaying")

        onNodeWithTag("stage").performTouchInput { click(center) }
        mainClock.advanceTimeBy(300)

        assertEquals(false, replaying.last(), "the user's now: $replaying")
        onNodeWithText(TakeOver).assertDoesNotExist()
        onNodeWithText("Watch").assertExists()
    }

    @Test
    fun theTourPlaysOnceThenTheDemoIsTheUsers() = runComposeUiTest {
        val selections = mutableListOf<Int>()
        val replaying = mutableListOf<Boolean>()
        showTouredDemo(selections, replaying)
        onNodeWithText("Watch").performClick()

        mainClock.advanceTimeBy(5_000)

        assertEquals(false, replaying.last(), "handed back: $replaying")
        onNodeWithText("Watch").assertExists()
        selections.clear()
        mainClock.advanceTimeBy(3_000)
        assertTrue(selections.none { it == 1 }, "no second run: $selections")
    }

    @Test
    fun aRecordingStillPlaysTheTourByItself() = runComposeUiTest {
        val selections = mutableListOf<Int>()
        setContent {
            LabTheme {
                DemoScreen(
                    touredDemo(selections, mutableListOf()),
                    record = true,
                    label = null,
                    onBack = null,
                )
            }
        }
        mainClock.advanceTimeBy(4_000)

        assertTrue(selections.any { it == 1 }, "$selections")
    }

    @Test
    fun stoppingTheTourHandsTheDemoBackToTheUser() = runComposeUiTest {
        val replaying = mutableListOf<Boolean>()
        showTouredDemo(replaying = replaying)
        onNodeWithText("Watch").performClick()
        mainClock.advanceTimeBy(300)
        assertEquals(true, replaying.last(), "replaying: $replaying")

        onNodeWithText("Stop").performClick()
        mainClock.advanceTimeBy(300)
        assertEquals(false, replaying.last(), "the user's again: $replaying")
    }

    private companion object {
        const val TakeOver = "Playing the tour · touch to take over"
    }
}
