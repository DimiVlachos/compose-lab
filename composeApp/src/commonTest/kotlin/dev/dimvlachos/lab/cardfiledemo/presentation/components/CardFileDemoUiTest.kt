package dev.dimvlachos.lab.cardfiledemo.presentation.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import dev.dimvlachos.lab.cardfiledemo.CardFileDemos
import dev.dimvlachos.lab.cardfiledemo.ContactDeck
import dev.dimvlachos.lab.cardfiledemo.DemoContacts
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.cardfile.CH
import dev.dimvlachos.lab.core.presentation.components.cardfile.CardFileScene
import dev.dimvlachos.lab.core.presentation.components.cardfile.CardFileState
import dev.dimvlachos.lab.core.presentation.components.cardfile.PaperCard
import dev.dimvlachos.lab.core.presentation.components.cardfile.R0
import dev.dimvlachos.lab.core.presentation.components.cardfile.paperCards
import dev.dimvlachos.lab.core.presentation.components.cardfile.phiOf
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

    private fun ComposeUiTest.pullFront() {
        onNodeWithTag("cardfile").requestFocus()
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
    }

    private fun ComposeUiTest.mayaPulledOut(uri: UriHandler = FakeUri()): CardFileState {
        val deck = ContactDeck(DemoContacts)
        val state = CardFileState(initial = deck.indexOf("maya-brooks"))
        setContent {
            CompositionLocalProvider(LocalUriHandler provides uri) {
                CardFileDemo(DemoState(), deck, state)
            }
        }
        pullFront()
        return state
    }

    @Test
    fun callingMayaOpensTheDialerWithHerNumber() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        val file = CardFileState(initial = deck.indexOf("maya-brooks"))
        val uri = FakeUri()
        setContent {
            CompositionLocalProvider(LocalUriHandler provides uri) {
                CardFileDemo(DemoState(), deck, file)
            }
        }
        pullFront()
        onNodeWithTag("call").performClick()
        assertEquals(listOf("tel:6175550193"), uri.opened)
    }

    @Test
    fun messagingMayaOpensMessagesWithHerNumber() = runComposeUiTest {
        val uri = FakeUri()
        mayaPulledOut(uri)
        onNodeWithTag("message").performClick()
        assertEquals(listOf("sms:6175550193"), uri.opened)
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
    fun theSheetLetsGoOfAFiledCardAsItGoesBackNotWhenItLands() = runComposeUiTest {
        val state = mayaPulledOut()
        onNodeWithTag("call").assertExists()
        mainClock.autoAdvance = false
        // Escape files the pulled card.
        onNodeWithTag("cardfile").performKeyInput { pressKey(Key.Escape) }
        mainClock.advanceTimeBy(300)
        assertTrue(state.pulled != null, "the card is still on its way back")
        onNodeWithTag("call").assertDoesNotExist()
        mainClock.autoAdvance = true
        waitForIdle()
        onNodeWithText("Maya Brooks").assertExists()
    }

    @Test
    fun aPulledCardsHintSaysHowToFileIt() = runComposeUiTest {
        mayaPulledOut()
        onNodeWithText("Push the card down to file it").assertExists()
        onNodeWithText("Spin the cards · tap one to pull it out").assertDoesNotExist()
    }

    @Test
    fun aDividerAtTheFrontNamesItsLetterAndTheCardsBehindIt() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        setContent { CardFileDemo(DemoState(), deck, CardFileState(initial = 0)) }
        onNodeWithText("A").assertExists()
        onNodeWithText("2 cards behind this tab").assertExists()
        onNodeWithText("Spin the cards · tap one to pull it out").assertExists()
    }

    @Test
    fun theSheetCrossfadesFromOneNameToTheNext() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        val start = CardFileDemos.StartIndex
        val file = CardFileState(initial = start)
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            CardFileDemo(DemoState(), deck, file)
        }
        waitForIdle()
        onNodeWithText("Ben Hollis").assertExists()
        mainClock.autoAdvance = false
        scope.launch { file.spinTo(start + 1) }
        var frames = 0
        while (file.front == start && frames++ < 200) mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(48)
        // Just after the next card comes to the front, the last name is still fading out.
        onNodeWithText("Ben Hollis").assertExists()
        mainClock.autoAdvance = true
        waitForIdle()
        onNodeWithText("Ben Hollis").assertDoesNotExist()
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
        val stopped = file.position - CardFileDemos.StartIndex
        assertTrue(stopped > 0.05f && stopped < 0.95f, "between cards at $stopped")
        script.cancel()
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(file.front.toFloat(), file.position, 1e-3f)
    }

    @Test
    fun aCardTheUserFilesWhileTheScriptPullsItStaysFiled() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        val file = CardFileState(initial = deck.indexOf("maya-brooks"))
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
        assertEquals(deck.indexOf("maya-brooks"), file.pulled)
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
        // Half way up the front card, where the card file draws it on this stage.
        val node = onNodeWithTag("cardfile").fetchSemanticsNode()
        val d = density.density
        val scene =
            CardFileScene(paperCards(deck.cards) { PaperCard.Contact("", "", emptyList(), 1) })
        scene.layout(node.size.width / d, node.size.height / d, d)
        val at = Offset(scene.cx, scene.proj(R0 + CH * .5f, phiOf(0f)).y) * d
        onNodeWithTag("cardfile").performTouchInput { click(at) }
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
        assertEquals(deck.indexOf("maya-brooks"), file.front)
        scope.launch { state.tapControl(CardFileDemos.Pull) }
        waitForIdle()
        assertEquals(deck.indexOf("maya-brooks"), file.pulled)
        scope.launch {
            state.tapControl(CardFileDemos.File)
            state.resetFeed()
        }
        waitForIdle()
        assertNull(file.pulled)
        assertEquals(CardFileDemos.StartIndex, file.front)
    }

    @Test
    fun theSheetNamesTheFrontContactAndPulledOutOffersCallAndMessage() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        setContent {
            CardFileDemo(DemoState(), deck, CardFileState(initial = CardFileDemos.StartIndex))
        }
        onNodeWithText("Ben Hollis").assertExists()
        onNodeWithText("Hollis Auto Body · (802) 555-0119").assertExists()
        onNodeWithTag("call").assertDoesNotExist()
        pullFront()
        onNodeWithText("(802) 555-0119").assertExists()
        onNodeWithTag("call").assertExists()
        onNodeWithTag("message").assertExists()
    }

    @Test
    fun aTapOnTheSheetLeavesThePulledCardOut() = runComposeUiTest {
        val state = mayaPulledOut()
        // Her name on the sheet, under the desk: the sheet is not the desk.
        onNodeWithText("Maya Brooks").performClick()
        waitForIdle()
        assertEquals(ContactDeck(DemoContacts).indexOf("maya-brooks"), state.pulled)
        onNodeWithTag("call").assertExists()
    }
}
