package dev.dimvlachos.lab.smokedemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.smoke.LocalSmokeCompositionProbe
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeSide
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeState
import dev.dimvlachos.lab.core.presentation.components.smoke.rememberSmokeState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.smokedemo.ChatChange
import dev.dimvlachos.lab.smokedemo.ChatPlace
import dev.dimvlachos.lab.smokedemo.CycladesChats
import dev.dimvlachos.lab.smokedemo.CycladesInbox
import dev.dimvlachos.lab.smokedemo.SmokeDemos
import dev.dimvlachos.lab.smokedemo.rememberCycladesInbox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SmokeDemoUiTest {
    private var inbox: CycladesInbox? = null
    private var smoke: SmokeState? = null
    private var compositions = 0

    private fun ComposeUiTest.show(state: DemoState = DemoState()) {
        setContent {
            LabTheme {
                CompositionLocalProvider(LocalSmokeCompositionProbe provides { compositions++ }) {
                    Box(Modifier.size(400.dp, 800.dp)) {
                        val chats = rememberCycladesInbox()
                        val s = rememberSmokeState()
                        inbox = chats
                        smoke = s
                        SmokeDemo(state, chats, s)
                    }
                }
            }
        }
    }

    // Steps the clock a frame at a time until [done], failing after [most] ms with [what].
    private fun ComposeUiTest.advanceUntil(what: String, most: Long = 6_000, done: () -> Boolean) {
        var waited = 0L
        while (!done()) {
            assertTrue(waited < most, "waited $most ms for $what")
            mainClock.advanceTimeByFrame()
            waited += 16
        }
    }

    private fun ComposeUiTest.chat(name: String): SemanticsNodeInteraction =
        onNode(
            SemanticsMatcher("the chat with $name") { node ->
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any {
                    it.startsWith("$name, ")
                }
            }
        )

    private fun ComposeUiTest.swipe(name: String, share: Float) {
        chat(name).performTouchInput {
            val start = if (share < 0f) right - 30f else left + 30f
            down(Offset(start, centerY))
            repeat(20) { moveBy(Offset(width * share / 20f, 0f)) }
            up()
        }
    }

    @Test
    fun theListReadsAsTitledMessages() = runComposeUiTest {
        show()
        onNodeWithText("Messages").assertExists()
        onNodeWithText("Archived · 2").assertExists()
        onNodeWithText("Clear read").assertExists()
        chat("Kyria Maria").assertExists()
    }

    @Test
    fun aSwipePastAThirdBurnsAChatAndUndoBreathesItBack() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        swipe("Kyria Maria", -0.6f)
        advanceUntil("the chat to burn away") { inbox!!.placeOf("maria") == ChatPlace.Deleted }
        onNodeWithText("Deleted chat with Kyria Maria").assertExists()
        onNodeWithText("Undo").performClick()
        advanceUntil("the chat to come back") { inbox!!.placeOf("maria") == ChatPlace.Inbox }
        advanceUntil("the smoke to settle", most = 8_000) { !smoke!!.awake }
        chat("Kyria Maria").assertExists()
    }

    @Test
    fun aShortSwipeDrawsTheSmokeBackIn() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        swipe("Aegean Ferries", -0.15f)
        advanceUntil("the smoke to settle", most = 8_000) { !smoke!!.awake }
        assertEquals(ChatPlace.Inbox, inbox!!.placeOf("ferry"))
    }

    @Test
    fun theDeleteActionAndTheDeleteKeyBurnAChat() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        chat("Kyria Maria").performCustomAccessibilityActionWithLabel("Delete")
        advanceUntil("the action to burn it") { inbox!!.placeOf("maria") == ChatPlace.Deleted }
        chat("Anna").performSemanticsAction(SemanticsActions.RequestFocus)
        chat("Anna").performKeyInput { pressKey(Key.Delete) }
        advanceUntil("the key to burn it") { inbox!!.placeOf("anna") == ChatPlace.Deleted }
    }

    @Test
    fun aRightSwipeArchivesAndTheArchiveRestores() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        swipe("Nikos", 0.6f)
        advanceUntil("the chat to go in mist") { inbox!!.placeOf("nikos") == ChatPlace.Archived }
        onNodeWithText("Archived chat with Nikos").assertExists()
        advanceUntil("the archive's count") {
            onAllNodes(hasContentDescription("Archived · 3") or hasTextExactly("Archived · 3"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        onNodeWithText("Archived · 3").performClick()
        mainClock.advanceTimeBy(500)
        onNodeWithText("Archived").assertExists()
        chat("Nikos").performCustomAccessibilityActionWithLabel("Restore")
        advanceUntil("the chat to be restored") { inbox!!.placeOf("nikos") == ChatPlace.Inbox }
        onNodeWithText("Restored chat with Nikos").assertExists()
    }

    @Test
    fun nothingRecomposesWhileARowBurns() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(100)
        smoke!!.swipeAway("maria", SmokeSide.Left)
        mainClock.advanceTimeBy(64)
        val burning = compositions
        mainClock.advanceTimeBy(200)
        assertEquals(burning, compositions, "the burn must not recompose the list")
        assertEquals(ChatPlace.Inbox, inbox!!.placeOf("maria"), "still burning")
    }

    @Test
    fun theScriptPlaysTwiceAndEndsWithTheListAsItBegan() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState(replay = true)
        var runs = 0
        setContent {
            LabTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    val chats = rememberCycladesInbox()
                    inbox = chats
                    SmokeDemo(state, chats, rememberSmokeState())
                }
                LaunchedEffect(Unit) {
                    repeat(2) {
                        SmokeDemos.all.single().script.play(state)
                        runs++
                    }
                }
            }
        }
        repeat(2) { run ->
            advanceUntil("Kyria Maria's chat to burn in run ${run + 1}", most = 4_000) {
                inbox!!.placeOf("maria") == ChatPlace.Deleted
            }
            advanceUntil("the clear's wave in run ${run + 1}", most = 16_000) {
                inbox!!.readInInbox().isEmpty()
            }
            advanceUntil("run ${run + 1} to end", most = 14_000) { runs == run + 1 }
            for (chat in CycladesChats.all) {
                assertEquals(CycladesChats.start[chat.id], inbox!!.placeOf(chat.id), chat.id)
            }
            assertNull(inbox!!.last, "nothing left to undo")
        }
    }

    @Test
    fun clearReadIgnoresASecondTapWhileItsWaveBurns() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        onNodeWithText("Clear read").performClick()
        mainClock.advanceTimeBy(200)
        onNodeWithText("Clear read").assertIsNotEnabled()
        advanceUntil("the wave to end") { inbox!!.last != null }
        assertEquals(5, inbox!!.last!!.ids.size)
        onNodeWithText("Undo").performClick()
        advanceUntil("the chats to come back") { inbox!!.readInInbox().size == 5 }
    }

    @Test
    fun deletingAChatOfTheWaveBeforeItsTurnBurnsItOnceAndKeepsTheClearsUndo() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        onNodeWithText("Clear read").performClick()
        mainClock.advanceTimeByFrame()
        // Anna is last in the wave: Delete her straight away.
        chat("Anna").performSemanticsAction(SemanticsActions.RequestFocus)
        chat("Anna").performKeyInput { pressKey(Key.Delete) }
        advanceUntil("the wave to end") { inbox!!.last != null }
        mainClock.advanceTimeBy(1_000)
        assertEquals(ChatChange.Cleared, inbox!!.last!!.change)
        assertEquals(5, inbox!!.last!!.ids.size, "Anna is in the clear, once")
        onNodeWithText("Undo").performClick()
        advanceUntil("the chats to come back") { inbox!!.readInInbox().size == 5 }
    }
}
