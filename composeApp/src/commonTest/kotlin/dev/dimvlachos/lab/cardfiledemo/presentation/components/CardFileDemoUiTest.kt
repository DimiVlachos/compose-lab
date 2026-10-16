package dev.dimvlachos.lab.cardfiledemo.presentation.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.dimvlachos.lab.cardfiledemo.CardFileDemos
import dev.dimvlachos.lab.cardfiledemo.ContactDeck
import dev.dimvlachos.lab.cardfiledemo.DemoContacts
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.cardfile.CardFileState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class CardFileDemoUiTest {
    private class FakeUri(private val failure: (() -> Throwable)? = null) : UriHandler {
        val opened = mutableListOf<String>()

        override fun openUri(uri: String) {
            failure?.let { throw it() }
            opened += uri
        }
    }

    private fun ComposeUiTest.mayaPulledOut(uri: UriHandler = FakeUri()): CardFileState {
        val deck = ContactDeck(DemoContacts)
        val state = CardFileState(initial = deck.indexOf("brooks-maya"))
        setContent {
            CompositionLocalProvider(LocalUriHandler provides uri) {
                CardFileDemo(DemoState(), deck, state)
            }
        }
        onNodeWithTag("card-brooks-maya").performClick()
        waitForIdle()
        return state
    }

    @Test
    fun callingMayaOpensTheDialerWithHerNumber() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        val file = CardFileState(initial = deck.indexOf("brooks-maya"))
        val uri = FakeUri()
        setContent {
            CompositionLocalProvider(LocalUriHandler provides uri) {
                CardFileDemo(DemoState(), deck, file)
            }
        }
        onNodeWithTag("card-brooks-maya").performClick()
        waitForIdle()
        onNodeWithTag("call").performClick()
        assertEquals(listOf("tel:4155550142"), uri.opened)
    }

    @Test
    fun messagingMayaOpensMessagesWithHerNumber() = runComposeUiTest {
        val uri = FakeUri()
        mayaPulledOut(uri)
        onNodeWithTag("message").performClick()
        assertEquals(listOf("sms:4155550142"), uri.opened)
    }

    @Test
    fun aCallThatCantOpenSaysSo() = runComposeUiTest {
        mayaPulledOut(FakeUri { IllegalStateException("no dialer") })
        onNodeWithTag("call").performClick()
        onNodeWithTag("call-failed").assertExists()
    }

    @Test
    fun aCallNothingCanTakeSaysSo() = runComposeUiTest {
        // What Android throws when no app can take the link.
        mayaPulledOut(FakeUri { IllegalArgumentException("no activity for tel:") })
        onNodeWithTag("call").performClick()
        onNodeWithTag("call-failed").assertExists()
    }

    @Test
    fun aPulledCardsHintSaysHowToFileIt() = runComposeUiTest {
        mayaPulledOut()
        onNodeWithText("Push it down or tap outside to file it").assertExists()
        onNodeWithText("Spin the cards · tap one to pull it out").assertDoesNotExist()
    }

    @Test
    fun aDividerAtTheFrontNamesItsLetter() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        setContent { CardFileDemo(DemoState(), deck, CardFileState(initial = 0)) }
        onNodeWithText("Letter A").assertExists()
        onNodeWithText("Spin the cards · tap one to pull it out").assertExists()
    }

    @Test
    fun stoppingTheScriptMidSpinSettlesOnAWholeCard() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        val file = CardFileState(initial = CardFileDemos.StartIndex)
        val state = DemoState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            CardFileDemo(state, deck, file)
        }
        waitForIdle()
        mainClock.autoAdvance = false
        val script = scope.launch { state.spinCards(1, 600.milliseconds) }
        mainClock.advanceTimeBy(250)
        val stopped = file.position
        assertTrue(stopped > 1.05f && stopped < 1.95f, "between cards at $stopped")
        script.cancel()
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(file.front.toFloat(), file.position, 1e-3f)
    }

    @Test
    fun aCardTheUserFilesWhileTheScriptPullsItStaysFiled() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        val file = CardFileState(initial = deck.indexOf("brooks-maya"))
        val state = DemoState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            CardFileDemo(state, deck, file)
        }
        waitForIdle()
        mainClock.autoAdvance = false
        scope.launch { state.tapControl(CardFileDemos.Pull) }
        mainClock.advanceTimeBy(100)
        assertEquals(deck.indexOf("brooks-maya"), file.pulled)
        // The user files it before the script's pull has finished; the script's step is cut short
        // by the user, not stopped, so it must not bring the card back out.
        scope.launch { file.file() }
        mainClock.autoAdvance = true
        waitForIdle()
        assertNull(file.pulled)
    }

    @Test
    fun tappingADividerSpinsToItsFirstContact() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        val file = CardFileState(initial = 0)
        setContent { CardFileDemo(DemoState(), deck, file) }
        onNodeWithTag("card-divider-A").performClick()
        waitForIdle()
        assertEquals(1, file.front)
        assertEquals(null, file.pulled)
    }

    @Test
    fun theScriptsStepsLeaveTheCardFiledOnTheFirstContact() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        val file = CardFileState(initial = CardFileDemos.StartIndex)
        val state = DemoState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            CardFileDemo(state, deck, file)
        }
        waitForIdle()
        scope.launch {
            repeat(3) { state.spinCards(1, 300.milliseconds) }
            state.spinCards(CardFileDemos.FlickToMaya, 300.milliseconds)
        }
        waitForIdle()
        assertEquals(deck.indexOf("brooks-maya"), file.front)
        scope.launch { state.tapControl(CardFileDemos.Pull) }
        waitForIdle()
        assertEquals(deck.indexOf("brooks-maya"), file.pulled)
        scope.launch {
            state.tapControl(CardFileDemos.File)
            state.resetFeed()
        }
        waitForIdle()
        assertNull(file.pulled)
        assertEquals(CardFileDemos.StartIndex, file.front)
    }
}
