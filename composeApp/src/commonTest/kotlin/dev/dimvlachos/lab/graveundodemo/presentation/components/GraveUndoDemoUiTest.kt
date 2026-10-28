package dev.dimvlachos.lab.graveundodemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
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

    @Test
    fun theScriptTapsTheGraveMidRiseAndBuriesASecondWhileTheFirstIsHeld() = runComposeUiTest {
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
        advance(3_000)
        scope.launch { state.tapControl(GraveUndoDemos.UndoAtGrave) }
        advance(6_000)
        assertEquals(7, list.liveCount)
        assertTrue(list.graves.isEmpty())

        val flour = list.items[0]
        scope.launch { state.tapControl(GraveUndoDemos.Bury) }
        advance(7_000)
        assertEquals(GravePhase.Held, list.graves.single().phase)
        scope.launch { state.tapControl(GraveUndoDemos.Bury) }
        advance(9_000)
        assertTrue(flour.gone)
        assertEquals(list.items[1], list.undoable()?.item)
    }

    // Real pointer input through the list's gesture: a swipe of five sparse moves that lets go
    // well past where its last move was, so only the lift-off's own position arms it.
    @Test
    fun aSparseRealSwipeBuriesFromWhereTheFingerLifts() = runComposeUiTest {
        lateinit var list: GraveUndoState
        setContent {
            list = rememberGraveUndo()
            Phone { GraveUndoDemo(DemoState(), list, reducedMotion = false) }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        val d = density.density
        val it = list.items[1]
        val y = (list.rowTop(it) + list.rowH / 2) * d
        onRoot().performTouchInput {
            down(Offset(300f * d, y))
            moveTo(Offset(290f * d, y))
            up()
        }
        advance(100)
        assertTrue(list.graves.isEmpty(), "a 10 dp drag springs back")
        onRoot().performTouchInput {
            down(Offset(300f * d, y))
            moveTo(Offset(290f * d, y))
            moveTo(Offset(150f * d, y + 2))
            up()
        }
        advance(100)
        assertEquals(it, list.graves.single().item)
        assertEquals(false, it.done)
    }

    // A cancelled touch reaches the list as every pointer up, already consumed (Compose's
    // onCancelPointerInput). The test's own cancel() isn't delivered on the JVM, so a parent marks
    // the lift-off consumed before the list sees it, which is the same event.
    @Test
    fun aCancelledTouchSpringsTheRowBack() = runComposeUiTest {
        lateinit var list: GraveUndoState
        setContent {
            list = rememberGraveUndo()
            Phone {
                Box(
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent(PointerEventPass.Initial)
                                e.changes.forEach { if (!it.pressed) it.consume() }
                            }
                        }
                    },
                ) {
                    GraveUndoDemo(DemoState(), list, reducedMotion = false)
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        val d = density.density
        val y = (list.rowTop(list.items[2]) + list.rowH / 2) * d
        onRoot().performTouchInput {
            down(Offset(300f * d, y))
            moveTo(Offset(250f * d, y))
            moveTo(Offset(100f * d, y))
            up()
        }
        advance(1_000)
        assertTrue(list.graves.isEmpty())
        assertEquals(0f, list.items[2].dx)
        assertEquals(7, list.liveCount)
    }

    @Test
    fun aRealTapOnTheHandBringsTheTaskBack() = runComposeUiTest {
        lateinit var list: GraveUndoState
        setContent {
            list = rememberGraveUndo()
            Phone { GraveUndoDemo(DemoState(), list, reducedMotion = false) }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        val d = density.density
        val y = (list.rowTop(list.items[0]) + list.rowH / 2) * d
        onRoot().performTouchInput {
            down(Offset(300f * d, y))
            moveTo(Offset(200f * d, y))
            moveTo(Offset(80f * d, y))
            up()
        }
        advance(5_000)
        assertTrue(list.handHitOn)
        val at =
            Offset(
                (list.handHitX + list.handHitW / 2) * d,
                (list.handHitY + 20 * list.u) * d,
            )
        onRoot().performTouchInput { click(at) }
        advance(4_000)
        assertEquals(7, list.liveCount)
        assertTrue(list.asBegun)
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
