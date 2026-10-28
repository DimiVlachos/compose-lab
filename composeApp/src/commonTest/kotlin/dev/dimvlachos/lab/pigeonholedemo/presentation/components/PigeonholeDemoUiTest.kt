package dev.dimvlachos.lab.pigeonholedemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState
import dev.dimvlachos.lab.pigeonholedemo.PigeonholeDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class PigeonholeDemoUiTest {
    @Test
    fun theInboxListsSevenMessagesWithTheirSendersAndSubjects() = runComposeUiTest {
        lateinit var mail: PigeonholeState
        setContent {
            mail = rememberLarkspurMail()
            Phone { PigeonholeDemo(DemoState(), mail) }
        }
        waitForIdle()
        onNodeWithTag("sub").assertTextEquals("Larkspur Mail · 7 messages")
        val label =
            onNodeWithTag("row:m1")
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertEquals(listOf("Corner Market, Your receipt from Corner Market, 9:41 AM"), label)
        // Compose resources draw an Android-style \' as typed: no message may carry an escape.
        for (m in mail.rows) assertTrue('\\' !in m.item.subj && '\\' !in m.item.prev)
    }

    @Test
    fun aScreenReadersMoveToRaisesTheRackAndACubbyTakesTheMessage() = runComposeUiTest {
        lateinit var mail: PigeonholeState
        setContent {
            mail = rememberLarkspurMail()
            Phone { PigeonholeDemo(DemoState(), mail) }
        }
        waitForIdle()
        val move =
            onNodeWithTag("row:m3")
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .first {
                    it.label == "Move to…"
                }
        runOnIdle { move.action() }
        mainClock.advanceTimeBy(800)
        waitForIdle()
        assertTrue(mail.rackUp)
        onNodeWithTag("cub:family").assertContentDescriptionEquals("Family, 2 messages")
        onNodeWithTag("cub:family").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        assertEquals("m3", mail.messages("family").first().id)
        onNodeWithTag("sub").assertTextEquals("Larkspur Mail · 6 messages")
        assertFalse(mail.rackUp)
    }

    @Test
    fun theRackIconOpensAFolderAndBackGoesHome() = runComposeUiTest {
        lateinit var mail: PigeonholeState
        setContent {
            mail = rememberLarkspurMail()
            Phone { PigeonholeDemo(DemoState(), mail) }
        }
        waitForIdle()
        onNodeWithTag("folders").performClick()
        mainClock.advanceTimeBy(600)
        onNodeWithTag("cub:school").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        assertEquals("school", mail.view)
        onNodeWithTag("sub").assertTextEquals("1 message")
        onNodeWithTag("back").performClick()
        mainClock.advanceTimeBy(2_500)
        waitForIdle()
        assertEquals(PigeonholeState.Inbox, mail.view)
        assertTrue(mail.asBegun)
    }

    @Test
    fun theScriptPostsOpensAndPullsTheReceiptBackEndingAsItBegan() = runComposeUiTest {
        val state = DemoState()
        lateinit var mail: PigeonholeState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            mail = rememberLarkspurMail()
            Phone { PigeonholeDemo(state, mail, reducedMotion = false) }
        }
        waitForIdle()
        scope.launch { state.tapControl(PigeonholeDemos.Post) }
        mainClock.advanceTimeBy(2_400)
        assertTrue(mail.carry!!.letter.f > .9f, "folded over the rack")
        mainClock.advanceTimeBy(1_800)
        assertEquals("m1", mail.messages("receipts").first().id)
        scope.launch { state.tapControl(PigeonholeDemos.OpenRack) }
        mainClock.advanceTimeBy(1_300)
        assertTrue(mail.rackUp)
        scope.launch { state.tapControl(PigeonholeDemos.OpenReceipts) }
        mainClock.advanceTimeBy(3_000)
        assertEquals("receipts", mail.view)
        scope.launch { state.tapControl(PigeonholeDemos.Back) }
        mainClock.advanceTimeBy(3_300)
        assertEquals(PigeonholeState.Inbox, mail.view)
        scope.launch { state.tapControl(PigeonholeDemos.OpenRack) }
        mainClock.advanceTimeBy(1_300)
        scope.launch { state.tapControl(PigeonholeDemos.PullBack) }
        mainClock.advanceTimeBy(3_500)
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertEquals("m1", mail.rows.first().id)
        assertTrue(mail.asBegun, "the clip ends where it began")
    }

    // A phone's stage under the demo screen's bar.
    @Composable
    private fun Phone(content: @Composable () -> Unit) {
        Box(Modifier.size(412.dp, 820.dp)) { content() }
    }
}
