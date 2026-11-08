package dev.dimvlachos.lab.sandmandalademo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandAt
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaState
import dev.dimvlachos.lab.sandmandalademo.SandMandalaDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class SandMandalaDemoUiTest {
    @Test
    fun theHistoryIsTenVisitsAndItsSandIsLaid() = runComposeUiTest {
        lateinit var history: SandMandalaState
        setContent {
            history = rememberSandMandala()
            Phone { SandMandalaDemo(DemoState(), history) }
        }
        waitForIdle()
        assertEquals(10, history.visits.size)
        assertEquals("The Overstory, used paperback", history.visits[0].title)
        assertTrue(history.n > 15_000, "${history.n}")
        assertEquals(SandAt.Rows, history.at)
    }

    // The buttons and a real finger: Clear history, a sweep of five sparse moves across the
    // mandala, and Undo, back to the rows.
    @Test
    fun clearSweepAndUndoByHand() = runComposeUiTest {
        lateinit var history: SandMandalaState
        setContent {
            history = rememberSandMandala()
            Phone { SandMandalaDemo(DemoState(), history, reducedMotion = false) }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithTag("clear").performSemanticsAction(SemanticsActions.OnClick)
        advance(5_000)
        assertEquals(SandAt.Mandala, history.at)
        val d = density.density
        val list = listTop(history)
        val x0 = (history.cx - history.r * .8f) * d
        val x1 = (history.cx + history.r * .8f) * d
        val y0 = (list + history.cy - history.r * .4f) * d
        val y1 = (list + history.cy + history.r * .4f) * d
        onRoot().performTouchInput {
            down(Offset(x0, y0))
            for (k in 1..5) moveTo(Offset(x0 + (x1 - x0) * k / 5f, y0 + (y1 - y0) * k / 5f))
            up()
        }
        advance(9_000)
        assertEquals(SandAt.Cleared, history.at)
        assertTrue(history.snackOn)
        onNodeWithTag("undo").performSemanticsAction(SemanticsActions.OnClick)
        advance(12_000)
        assertEquals(SandAt.Rows, history.at)
    }

    @Test
    fun theScriptClearsSweepsAndUndoes() = runComposeUiTest {
        val state = DemoState()
        lateinit var history: SandMandalaState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            history = rememberSandMandala()
            Phone { SandMandalaDemo(state, history, reducedMotion = false) }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        scope.launch {
            state.tapControl(SandMandalaDemos.Clear)
            state.tapControl(SandMandalaDemos.Sweep)
            state.tapControl(SandMandalaDemos.Undo)
        }
        advance(9_000)
        assertTrue(history.at == null || history.at != SandAt.Rows, "${history.at}")
        advance(18_000)
        assertEquals(SandAt.Rows, history.at)
        assertTrue(!history.snackOn)
    }

    // Where the list starts under the bar and the search field: the bar's 52 dp and the field's 46.
    private fun listTop(history: SandMandalaState) = if (history.short) 52f else 98f

    private fun androidx.compose.ui.test.ComposeUiTest.advance(ms: Long) {
        var left = ms
        while (left > 0) {
            mainClock.advanceTimeBy(minOf(500L, left))
            left -= 500
        }
        mainClock.advanceTimeByFrame()
    }

    @Composable
    private fun Phone(content: @Composable () -> Unit) {
        Box(Modifier.size(412.dp, 820.dp)) { content() }
    }
}
