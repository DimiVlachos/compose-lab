package dev.dimvlachos.lab.smokedemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
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

    private fun ComposeUiTest.show(
        state: DemoState = DemoState(),
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        setContent {
            LabTheme {
                CompositionLocalProvider(
                    LocalSmokeCompositionProbe provides { compositions++ },
                    LocalLayoutDirection provides direction,
                ) {
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
        smoke!!.swipeAway("maria", SmokeSide.Start)
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

    @Test
    fun holdingDeleteBurnsOneChatAndTheNextPressBurnsAgain() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        chat("Kyria Maria").performSemanticsAction(SemanticsActions.RequestFocus)
        // Held a second and a half: the key repeats, on whichever row focus has moved on to.
        chat("Kyria Maria").performKeyInput {
            keyDown(Key.Delete)
            advanceEventTime(1_500)
            keyUp(Key.Delete)
        }
        advanceUntil("the held key to burn it") { inbox!!.placeOf("maria") == ChatPlace.Deleted }
        mainClock.advanceTimeBy(1_500)
        val deleted = CycladesChats.all.filter { inbox!!.placeOf(it.id) == ChatPlace.Deleted }
        assertEquals(listOf("maria"), deleted.map { it.id }, "one press, one chat")
        onNodeWithText("Undo").performClick()
        advanceUntil("the chat to come back") { inbox!!.placeOf("maria") == ChatPlace.Inbox }
        advanceUntil("the smoke to settle", most = 8_000) { !smoke!!.awake }
        chat("Kyria Maria").performSemanticsAction(SemanticsActions.RequestFocus)
        chat("Kyria Maria").performKeyInput { pressKey(Key.Delete) }
        advanceUntil("the next press to burn it") { inbox!!.placeOf("maria") == ChatPlace.Deleted }
    }

    @Test
    fun systemBackLeavesTheArchiveThenTheScreen() = runComposeUiTest {
        val backInput = DirectNavigationEventInput()
        var left = 0
        setContent {
            val owner = rememberNavigationEventDispatcherOwner(parent = null)
            DisposableEffect(owner) {
                owner.navigationEventDispatcher.addInput(backInput)
                onDispose { owner.navigationEventDispatcher.removeInput(backInput) }
            }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                // The screen it is on: back that the demo doesn't take leaves it.
                NavigationBackHandler(
                    state = rememberNavigationEventState(NavigationEventInfo.None),
                    isBackEnabled = true,
                    onBackCompleted = { left++ },
                )
                LabTheme { Box(Modifier.size(400.dp, 800.dp)) { SmokeDemo(DemoState()) } }
            }
        }
        onNodeWithText("Archived · 2").performClick()
        waitForIdle()
        onNodeWithText("Archived").assertExists()
        runOnUiThread { backInput.backCompleted() }
        waitForIdle()
        onNodeWithText("Messages").assertExists()
        assertEquals(0, left, "back leaves the archive, not the screen")
        runOnUiThread { backInput.backCompleted() }
        waitForIdle()
        assertEquals(1, left, "in the inbox, back is the screen's")
    }

    @Test
    fun openingTheArchiveMidWaveEndsTheClearWithWhatBurnt() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        onNodeWithText("Clear read").performClick()
        mainClock.advanceTimeBy(150)
        // The rest of the wave's turns come with the inbox off the screen.
        onNodeWithText("Archived · 2").performClick()
        advanceUntil("the clear to end") { !inbox!!.isClearing }
        val cleared = inbox!!.last!!
        assertEquals(ChatChange.Cleared, cleared.change)
        assertTrue(cleared.ids.size in 1..4, "only what burnt: ${cleared.ids}")
        for (id in cleared.ids) assertEquals(ChatPlace.Deleted, inbox!!.placeOf(id), id)
        onNodeWithContentDescription("Back to messages").performClick()
        mainClock.advanceTimeBy(500)
        onNodeWithText("Clear read").assertIsEnabled()
        onNodeWithText("Undo").performClick()
        advanceUntil("the chats to come back") { inbox!!.readInInbox().size == 5 }
    }

    @Test
    fun undoingARestoreMidWaveEndsTheClearWithoutIt() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        onNodeWithText("Archived · 2").performClick()
        mainClock.advanceTimeBy(500)
        chat("Paros Kite Club").performCustomAccessibilityActionWithLabel("Restore")
        advanceUntil("the chat to be restored") { inbox!!.placeOf("kite") == ChatPlace.Inbox }
        onNodeWithContentDescription("Back to messages").performClick()
        mainClock.advanceTimeBy(500)
        onNodeWithText("Clear read").performClick()
        mainClock.advanceTimeByFrame()
        // The restore's Undo sends the kite club back to the archive before its turn in the wave.
        onNodeWithText("Undo").performClick()
        assertEquals(ChatPlace.Archived, inbox!!.placeOf("kite"))
        advanceUntil("the clear to end") { !inbox!!.isClearing }
        assertEquals(ChatChange.Cleared, inbox!!.last!!.change)
        assertEquals(5, inbox!!.last!!.ids.size)
        assertTrue("kite" !in inbox!!.last!!.ids)
    }

    @Test
    fun rightToLeftTheSwipesMirror() = runComposeUiTest {
        show(direction = LayoutDirection.Rtl)
        mainClock.autoAdvance = false
        onNodeWithText("Swipe right to delete, left to archive").assertExists()
        // Toward the start, to the right here, deletes.
        swipe("Kyria Maria", 0.6f)
        advanceUntil("the chat to burn away") { inbox!!.placeOf("maria") == ChatPlace.Deleted }
        // Toward the end, to the left, archives.
        swipe("Nikos", -0.6f)
        advanceUntil("the chat to go in mist") { inbox!!.placeOf("nikos") == ChatPlace.Archived }
        // The Delete key deletes, whichever way that is.
        chat("Anna").performSemanticsAction(SemanticsActions.RequestFocus)
        chat("Anna").performKeyInput { pressKey(Key.Delete) }
        advanceUntil("the key to burn it") { inbox!!.placeOf("anna") == ChatPlace.Deleted }
    }

    @Test
    fun aScreenReadersDeleteMovesFocusOnToTheNextChat() = runComposeUiTest {
        show()
        mainClock.autoAdvance = false
        chat("Kyria Maria").performCustomAccessibilityActionWithLabel("Delete")
        mainClock.advanceTimeByFrame()
        chat("Hydra Water Taxi").assertIsFocused()
    }
}
