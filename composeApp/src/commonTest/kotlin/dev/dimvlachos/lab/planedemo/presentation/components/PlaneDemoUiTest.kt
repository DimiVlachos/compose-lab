package dev.dimvlachos.lab.planedemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.planedemo.PlaneDemos
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PlaneDemoUiTest {
    @Test
    fun theScriptSendsEachLineIntoTheConversationAndTakesThemBackAtTheEnd() = runComposeUiTest {
        mainClock.autoAdvance = false
        val demo = PlaneDemos.all.single()
        val state = DemoState()
        var finished = false
        setContent {
            LabTheme {
                // Its planes folded in the test's own time, not on another thread's.
                Box(Modifier.size(400.dp, 800.dp)) { PlaneDemo(state, EmptyCoroutineContext) }
                LaunchedEffect(Unit) {
                    demo.script.play(state)
                    finished = true
                }
            }
        }
        val lines = listOf("Folding something right now", "Incoming!")
        // Both landed before they are taken back: each line once, in the conversation, none still
        // on its way, nothing left typed.
        mainClock.advanceTimeBy(10_600)
        for (line in lines) {
            assertEquals(1, onAllNodesWithText(line).fetchSemanticsNodes().size, line)
        }
        assertEquals(0, onAllNodesWithTag(InFlightTag).fetchSemanticsNodes().size)
        assertEquals(1, onAllNodesWithText("Message").fetchSemanticsNodes().size)
        // And taken back by the end, so the next run starts from the same conversation.
        mainClock.advanceTimeBy(demo.script.nominalDuration.inWholeMilliseconds - 10_600 + 2_000)
        assertEquals(true, finished)
        for (line in lines) {
            assertEquals(0, onAllNodesWithText(line).fetchSemanticsNodes().size, line)
        }
    }

    @Test
    fun spacesAloneAreNothingToSend() = runComposeUiTest {
        setContent {
            LabTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    PlaneDemo(DemoState(), EmptyCoroutineContext)
                }
            }
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextInput("   ")
        waitForIdle()
        onNodeWithContentDescription("Send").assertIsNotEnabled()
        assertEquals(0, onAllNodesWithTag(InFlightTag).fetchSemanticsNodes().size)
    }
}
