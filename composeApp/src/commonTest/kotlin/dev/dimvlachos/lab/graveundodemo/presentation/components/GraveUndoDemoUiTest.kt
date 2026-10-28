package dev.dimvlachos.lab.graveundodemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.graveundo.GravePhase
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveUndoState
import dev.dimvlachos.lab.graveundodemo.GraveUndoDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class GraveUndoDemoUiTest {
    @Test
    fun theListShowsTheBakerysSevenTasks() = runComposeUiTest {
        lateinit var list: GraveUndoState
        setContent {
            list = rememberGraveUndo()
            Phone { GraveUndoDemo(DemoState(), list) }
        }
        waitForIdle()
        assertEquals(7, list.liveCount)
        assertEquals("Order 50 lb of flour", list.items[0].task.title)
        val label =
            onNodeWithTag("task:Email the Oct 31 orders")
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertEquals(listOf("Email the Oct 31 orders, Hollis & Grant Co., 4:30 PM"), label)
    }

    @Test
    fun aScreenReadersDeleteBuriesTheTaskAndTheGraveBringsItBack() = runComposeUiTest {
        lateinit var list: GraveUndoState
        setContent {
            list = rememberGraveUndo()
            Phone { GraveUndoDemo(DemoState(), list, reducedMotion = false) }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        val delete =
            onNodeWithTag("task:Restock caramel apples")
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .first { it.label == "Delete" }
        runOnIdle { delete.action() }
        advance(5_000)
        assertEquals(6, list.liveCount)
        assertEquals(GravePhase.Held, list.graves.single().phase)
        val grave = onNodeWithTag("grave").fetchSemanticsNode()
        assertEquals(
            listOf("Undo: bring back Restock caramel apples"),
            grave.config[SemanticsProperties.ContentDescription],
        )
        onNodeWithTag("grave").performSemanticsAction(SemanticsActions.OnClick)
        advance(4_000)
        assertEquals(7, list.liveCount)
        assertTrue(list.asBegun)
    }

    @Test
    fun theScriptBuriesThroughTheDragAndTheHandBringsItBack() = runComposeUiTest {
        val state = DemoState()
        lateinit var list: GraveUndoState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            list = rememberGraveUndo()
            Phone { GraveUndoDemo(state, list, reducedMotion = false) }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        scope.launch { state.tapControl(GraveUndoDemos.Bury) }
        advance(1_600)
        assertTrue(list.items[0].dx < -60f, "dx ${list.items[0].dx}")
        advance(1_400)
        assertEquals(1, list.graves.size)
        scope.launch { state.tapControl(GraveUndoDemos.Undo) }
        advance(9_000)
        assertEquals(7, list.liveCount)
        assertTrue(list.graves.isEmpty())
    }

    // The clock in half-second steps. The tests turn autoAdvance off, so
    // a finder's wait for idle doesn't run the list's loop on until the grave times out.
    private fun androidx.compose.ui.test.ComposeUiTest.advance(ms: Long) {
        var left = ms
        while (left > 0) {
            mainClock.advanceTimeBy(minOf(500L, left))
            left -= 500
        }
        // What the last step changed is composed on the next frame.
        mainClock.advanceTimeByFrame()
    }

    @Composable
    private fun Phone(content: @Composable () -> Unit) {
        Box(Modifier.size(412.dp, 820.dp)) { content() }
    }
}
