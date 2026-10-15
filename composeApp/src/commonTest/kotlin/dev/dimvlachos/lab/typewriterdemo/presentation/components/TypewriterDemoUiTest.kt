package dev.dimvlachos.lab.typewriterdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.typewriterdemo.TypewriterDemos
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TypewriterDemoUiTest {
    @Test
    fun theScriptSendsBothSlipsGetsBothRepliesAndTakesThemBack() = runComposeUiTest {
        mainClock.autoAdvance = false
        val demo = TypewriterDemos.all.single()
        val state = DemoState()
        var finished = false
        setContent {
            LabTheme {
                Box(Modifier.size(400.dp, 800.dp)) { TypewriterDemo(state) }
                LaunchedEffect(Unit) {
                    demo.script.play(state)
                    finished = true
                }
            }
        }
        val slips = listOf("see you at the station at nine.", "sunset at the lake?")
        val replies =
            listOf("Perfect. I’ll bring the blueberry pie.", "Deal. Sunset at the lake it is.")
        mainClock.advanceTimeBy(19_100)
        for (slip in slips) {
            assertEquals(1, onAllNodesWithContentDescription(slip).fetchSemanticsNodes().size, slip)
        }
        for (reply in replies) {
            assertEquals(1, onAllNodesWithText(reply).fetchSemanticsNodes().size, reply)
        }
        // The last reply came in at the thread's foot, and the thread kept it in sight.
        onNodeWithText(replies.last()).assertIsDisplayed()
        // The thread it began with is still there, up the thread out of sight: its three
        // messages, the two slips and the two replies, under the day.
        val thread = onNode(hasScrollToIndexAction()).fetchSemanticsNode()
        assertEquals(8, thread.config[SemanticsProperties.CollectionInfo].rowCount)
        mainClock.advanceTimeBy(demo.script.nominalDuration.inWholeMilliseconds - 19_100 + 2_000)
        assertEquals(true, finished)
        for (text in slips) {
            assertEquals(0, onAllNodesWithContentDescription(text).fetchSemanticsNodes().size, text)
        }
        for (text in replies) {
            assertEquals(0, onAllNodesWithText(text).fetchSemanticsNodes().size, text)
        }
    }
}
