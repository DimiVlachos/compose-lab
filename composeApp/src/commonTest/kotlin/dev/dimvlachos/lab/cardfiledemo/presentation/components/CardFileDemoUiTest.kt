package dev.dimvlachos.lab.cardfiledemo.presentation.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class CardFileDemoUiTest {
    private class FakeUri(private val fail: Boolean = false) : UriHandler {
        val opened = mutableListOf<String>()

        override fun openUri(uri: String) {
            if (fail) throw IllegalStateException("no dialer")
            opened += uri
        }
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
    fun aCallThatCantOpenSaysSo() = runComposeUiTest {
        val deck = ContactDeck(DemoContacts)
        val file = CardFileState(initial = deck.indexOf("brooks-maya"))
        setContent {
            CompositionLocalProvider(LocalUriHandler provides FakeUri(fail = true)) {
                CardFileDemo(DemoState(), deck, file)
            }
        }
        onNodeWithTag("card-brooks-maya").performClick()
        waitForIdle()
        onNodeWithTag("call").performClick()
        onNodeWithTag("call-failed").assertExists()
    }

    @Test
    fun enterOnADividerSpinsToItsFirstContact() = runComposeUiTest {
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
