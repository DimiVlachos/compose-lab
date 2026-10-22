package dev.dimvlachos.lab.conchdemo.presentation.components

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import dev.dimvlachos.lab.conchdemo.ConchDemos
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.conch.ConchMode
import dev.dimvlachos.lab.core.presentation.components.conch.ConchState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class ConchDemoUiTest {
    private fun conch() = ConchState().apply { reset() }

    @Test
    fun holdingTheConchRecordsAndLettingGoSendsANote() = runComposeUiTest {
        mainClock.autoAdvance = false
        val conch = conch()
        setContent { ConchDemo(DemoState(), conch, reducedMotion = false) }
        onNodeWithTag("conch").performTouchInput { down(center) }
        mainClock.advanceTimeBy(1_500)
        assertEquals(ConchMode.Rec, conch.mode)
        assertTrue(conch.lift > .5f, "lift ${conch.lift}")
        onNodeWithTag("conch").performTouchInput { up() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertEquals(1, conch.sent)
        onNodeWithTag("note-mine").assertExists()
    }

    @Test
    fun slidingLeftCancels() = runComposeUiTest {
        mainClock.autoAdvance = false
        val conch = conch()
        setContent { ConchDemo(DemoState(), conch, reducedMotion = false) }
        onNodeWithTag("conch").performTouchInput {
            down(center)
            moveBy(Offset(-160f * density, 0f))
        }
        mainClock.advanceTimeBy(700)
        assertTrue(conch.armed)
        onNodeWithTag("conch").performTouchInput { up() }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        assertEquals(1, conch.cancels)
        assertEquals(0, conch.sent)
        onNodeWithTag("note-mine").assertDoesNotExist()
    }

    @Test
    fun tappingMayasNotePlaysAndPausesIt() = runComposeUiTest {
        mainClock.autoAdvance = false
        val conch = conch()
        setContent { ConchDemo(DemoState(), conch, reducedMotion = false) }
        onNodeWithTag("note-maya").assertContentDescriptionEquals("Maya's voice note, 0:04. Play")
        onNodeWithTag("note-maya").performClick()
        mainClock.advanceTimeBy(500)
        assertTrue(conch.notes[0].playing)
        onNodeWithTag("note-maya").assertContentDescriptionEquals("Maya's voice note, 0:04. Pause")
        onNodeWithTag("note-maya").performClick()
        assertFalse(conch.notes[0].playing)
    }

    @Test
    fun theScriptSendsCancelsAndPutsTheChatBack() = runComposeUiTest {
        val state = DemoState()
        mainClock.autoAdvance = false
        val conch = conch()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            ConchDemo(state, conch, reducedMotion = false)
        }
        waitForIdle()
        scope.launch { state.tapControl(ConchDemos.Hold) }
        mainClock.advanceTimeBy(1_000)
        assertEquals(ConchMode.Rec, conch.mode)
        mainClock.advanceTimeBy(2_000)
        scope.launch { state.tapControl(ConchDemos.Release) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertEquals(1, conch.sent)
        scope.launch { state.tapControl(ConchDemos.Hold) }
        mainClock.advanceTimeBy(1_500)
        scope.launch { state.tapControl(ConchDemos.Slide) }
        mainClock.advanceTimeBy(800)
        assertTrue(conch.armed)
        scope.launch { state.tapControl(ConchDemos.Release) }
        mainClock.advanceTimeBy(1_000)
        assertEquals(1, conch.cancels)
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertEquals(1, conch.notes.size)
        onNodeWithTag("note-mine").assertDoesNotExist()
    }
}
