package dev.dimvlachos.lab.stickynotedemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.stickynote.StickyNoteState
import dev.dimvlachos.lab.stickynotedemo.StickyNoteDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class StickyNoteDemoUiTest {
    @Test
    fun theListShowsSixNotesWithTheirTitlesAndMeta() = runComposeUiTest {
        lateinit var notes: StickyNoteState
        setContent {
            notes = rememberStickyNotes()
            Phone { StickyNoteDemo(DemoState(), notes) }
        }
        waitForIdle()
        onNodeWithTag("count").assertTextEquals("6 left")
        assertEquals("Call Dr. Patel's office", notes.items[0].title)
        // Compose resources draw an Android-style \' as typed: no note may carry an escape.
        for (item in notes.items) {
            assertTrue('\\' !in item.title && '\\' !in item.meta, "escaped: ${item.title}")
        }
        val label =
            onNodeWithTag("note:Call Dr. Patel's office")
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertEquals(listOf("Call Dr. Patel's office, 9:30 AM · Health"), label)
    }

    @Test
    fun aScreenReadersArchiveSendsTheNoteToDoneAndThePadPutsItBack() = runComposeUiTest {
        lateinit var notes: StickyNoteState
        setContent {
            notes = rememberStickyNotes()
            Phone { StickyNoteDemo(DemoState(), notes) }
        }
        waitForIdle()
        val node = onNodeWithTag("note:Pick up dry cleaning")
        val archive =
            node.fetchSemanticsNode().config[SemanticsActions.CustomActions].first {
                it.label == "Archive"
            }
        runOnIdle { archive.action() }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        onNodeWithTag("count").assertTextEquals("5 left")
        assertEquals(1, notes.archive.stack.size)
        onNodeWithTag("pad:archive").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        onNodeWithTag("count").assertTextEquals("6 left")
        assertTrue(notes.asBegun)
    }

    @Test
    fun theScriptPeelsThroughTheGestureAndEndsAsItBegan() = runComposeUiTest {
        val state = DemoState()
        lateinit var notes: StickyNoteState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            notes = rememberStickyNotes()
            Phone { StickyNoteDemo(state, notes, reducedMotion = false) }
        }
        waitForIdle()
        scope.launch { state.tapControl(StickyNoteDemos.ShortPull) }
        mainClock.advanceTimeBy(1_200)
        assertTrue(notes.rows[1].dx < -40f, "dx ${notes.rows[1].dx}")
        mainClock.advanceTimeBy(1_600)
        assertEquals(0f, notes.rows[1].dx)
        scope.launch { state.tapControl(StickyNoteDemos.Archive) }
        mainClock.advanceTimeBy(2_600)
        assertEquals(1, notes.archive.stack.size)
        scope.launch { state.tapControl(StickyNoteDemos.Snooze) }
        mainClock.advanceTimeBy(3_000)
        assertEquals(1, notes.snooze.stack.size)
        scope.launch { state.tapControl(StickyNoteDemos.TapLater) }
        mainClock.advanceTimeBy(2_000)
        scope.launch { state.tapControl(StickyNoteDemos.TapDone) }
        mainClock.advanceTimeBy(2_000)
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(notes.asBegun)
        onNodeWithTag("count").assertTextEquals("6 left")
    }

    @Test
    fun anEmptyPadSitsWhereItIsDrawnAndATapOnlyBumpsIt() = runComposeUiTest {
        lateinit var notes: StickyNoteState
        setContent {
            notes = rememberStickyNotes()
            Phone { StickyNoteDemo(DemoState(), notes) }
        }
        waitForIdle()
        val b = onNodeWithTag("pad:snooze").fetchSemanticsNode().boundsInRoot
        // Placed where the pad is drawn, under the list on the right.
        assertTrue(b.left > 200f && b.top > 600f, "pad at $b")
        onNodeWithTag("pad:snooze").performClick()
        waitForIdle()
        assertTrue(notes.asBegun)
    }

    // A phone's stage, so a 214 pull is past 42 % of the note as on the phone.
    @Composable
    private fun Phone(content: @Composable () -> Unit) {
        Box(Modifier.size(412.dp, 820.dp)) { content() }
    }
}
