package dev.dimvlachos.lab.demo.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_navbar
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

    // A demo to be played with, not watched: its script waits for Replay on the phone.
    private fun handsOnDemo(selections: MutableList<Int>) =
        Demo(
            id = "test.handsOn",
            title = Res.string.demo_navbar,
            script = demoScript { at(100.milliseconds) { select(1) } },
            autoplay = false,
            content = { state -> selections += state.selectedIndex },
        )

    @Test
    fun aHandsOnDemoWaitsForReplayOnThePhone() = runComposeUiTest {
        val selections = mutableListOf<Int>()
        setContent {
            LabTheme {
                DemoScreen(handsOnDemo(selections), record = false, label = null, onBack = {})
            }
        }
        mainClock.advanceTimeBy(2_000)

        assertTrue(selections.none { it == 1 }, "the script has not run: $selections")
        onNodeWithText("Replay").performClick()
        mainClock.advanceTimeBy(2_000)
        assertTrue(selections.any { it == 1 }, "Replay runs it: $selections")
    }

    @Test
    fun aHandsOnDemoStillPlaysWhenRecorded() = runComposeUiTest {
        val selections = mutableListOf<Int>()
        setContent {
            LabTheme {
                DemoScreen(handsOnDemo(selections), record = true, label = null, onBack = null)
            }
        }
        mainClock.advanceTimeBy(4_000)

        assertTrue(selections.any { it == 1 }, "$selections")
    }

    @Test
    fun stoppingAReplayHandsTheDemoBackToTheUser() = runComposeUiTest {
        val replaying = mutableListOf<Boolean>()
        val demo =
            Demo(
                id = "test.replay",
                title = Res.string.demo_navbar,
                script = demoScript { at(100.milliseconds) { select(1) } },
                autoplay = false,
                content = { state -> replaying += state.replay },
            )
        setContent { LabTheme { DemoScreen(demo, record = false, label = null, onBack = {}) } }
        onNodeWithText("Replay").performClick()
        mainClock.advanceTimeBy(500)
        assertEquals(true, replaying.last(), "replaying: $replaying")

        onNodeWithText("Stop").performClick()
        mainClock.advanceTimeBy(500)
        assertEquals(false, replaying.last(), "the user's again: $replaying")
    }
}
