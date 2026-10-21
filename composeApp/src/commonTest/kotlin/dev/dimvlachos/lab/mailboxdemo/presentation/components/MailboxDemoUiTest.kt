package dev.dimvlachos.lab.mailboxdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.mailboxdemo.MailboxDemos
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class MailboxDemoUiTest {
    private var scope: CoroutineScope? = null

    private fun ComposeUiTest.show(state: DemoState = DemoState(), play: Boolean = false) {
        setContent {
            scope = rememberCoroutineScope()
            LabTheme {
                Box(Modifier.size(390.dp, 720.dp)) { MailboxDemo(state, reducedMotion = false) }
                if (play) LaunchedEffect(Unit) { MailboxDemos.all.single().script.play(state) }
            }
        }
    }

    private fun ComposeUiTest.advanceUntil(what: String, most: Long = 6_000, done: () -> Boolean) {
        var waited = 0L
        while (!done()) {
            assertTrue(waited < most, "waited $most ms for $what")
            mainClock.advanceTimeByFrame()
            waited += 16
        }
    }

    private fun ComposeUiTest.has(text: String) = runCatching {
        onNodeWithText(text).assertExists()
    }
        .isSuccess

    private fun ComposeUiTest.mailbox(label: String) = onNodeWithContentDescription(label)

    @Test
    fun theFeedShowsTheStreetAndAnEmptyMailbox() = runComposeUiTest {
        show()
        onNodeWithText("Elm Street").assertExists()
        onNodeWithText("Good morning, Maya").assertExists()
        mailbox("Inbox, no unread messages").assertExists()
    }

    @Test
    fun deliveredMailCountsOnTheMailboxAndTheInboxOpensAndCloses() = runComposeUiTest {
        val state = DemoState()
        show(state)
        mainClock.autoAdvance = false
        scope!!.launch { state.tapControl(MailboxDemos.Arrive) }
        advanceUntil("the letter to land") {
            runCatching { mailbox("Inbox, 1 unread").assertExists() }.isSuccess
        }
        mailbox("Inbox, 1 unread").performClick()
        advanceUntil("the inbox to open") { has("Inbox") }
        // Let the page finish sliding in.
        mainClock.advanceTimeBy(600)
        onNodeWithText("Dana Whitfield").assertExists()
        onNodeWithText("Dana Whitfield").performClick()
        advanceUntil("the letter to be read") { has("All caught up") }
        onNodeWithText("I have two tomato cages", substring = true).assertExists()
        onNodeWithContentDescription("Back to Elm Street").performClick()
        advanceUntil("the inbox to close") { has("Elm Street") }
        mailbox("Inbox, no unread messages").assertExists()
    }

    @Test
    fun pullingTheDoorDownPastHalfwayOpensTheInbox() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        mailbox("Inbox, no unread messages").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, 5.dp.toPx())) }
        }
        mainClock.advanceTimeBy(300)
        mailbox("Inbox, no unread messages").performTouchInput { up() }
        advanceUntil("the inbox to open") { has("Inbox") }
    }

    @Test
    fun theScriptPlaysThroughAndLeavesTheStreetAsItBegan() =
        runComposeUiTest(testTimeout = 3.minutes) {
            show(DemoState(recording = true), play = true)
            mainClock.autoAdvance = false
            mainClock.advanceTimeBy(9_000)
            onNodeWithText("Inbox").assertExists()
            mainClock.advanceTimeBy(9_000)
            onNodeWithText("Elm Street").assertExists()
            mailbox("Inbox, no unread messages").assertExists()
        }
}
