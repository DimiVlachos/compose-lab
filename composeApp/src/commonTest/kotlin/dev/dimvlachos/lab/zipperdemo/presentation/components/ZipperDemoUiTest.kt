package dev.dimvlachos.lab.zipperdemo.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.zipper.ZipperState
import dev.dimvlachos.lab.zipperdemo.ZipperDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class ZipperDemoUiTest {
    @Test
    fun draggingThePullScrollsTheListAndShowsTheLetter() = runComposeUiTest {
        val list = LazyListState()
        val zipper = ZipperState()
        setContent { ZipperDemo(DemoState(), list, zipper, reducedMotion = false) }
        waitForIdle()
        onNodeWithTag("zipper")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "A"))
        val grab = zipper.pullCenter()
        onNodeWithTag("zipper").performTouchInput {
            down(center.copy(y = grab * density))
            moveTo(center.copy(y = (grab + zipper.range * .5f) * density))
        }
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(zipper.held)
        assertTrue(list.firstVisibleItemIndex > 30, "index ${list.firstVisibleItemIndex}")
        assertTrue(zipper.letter > "H", zipper.letter)
        onNodeWithTag("zipper").performTouchInput { up() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertFalse(zipper.held)
        assertTrue(zipper.clacks >= 1)
    }

    @Test
    fun aScreenReaderCanSetIt() = runComposeUiTest {
        val list = LazyListState()
        val zipper = ZipperState()
        setContent { ZipperDemo(DemoState(), list, zipper, reducedMotion = true) }
        waitForIdle()
        onNodeWithTag("zipper").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(zipper.pos > .98f, "pos ${zipper.pos}")
        assertTrue(zipper.letter > "S", zipper.letter)
    }

    @Test
    fun theScriptDragsTheListAndResetsToTheTop() = runComposeUiTest {
        val state = DemoState()
        val list = LazyListState()
        val zipper = ZipperState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            ZipperDemo(state, list, zipper, reducedMotion = false)
        }
        waitForIdle()
        scope.launch {
            state.tapControl(ZipperDemos.Grab)
            state.tapControl(ZipperDemos.drag(.86f, 800))
        }
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        assertTrue(zipper.held)
        assertTrue(zipper.pos > .8f, "pos ${zipper.pos}")
        assertTrue(list.firstVisibleItemIndex > 60, "index ${list.firstVisibleItemIndex}")
        // Let go, a tap at the top of the tape glides the pull home.
        scope.launch {
            state.tapControl(ZipperDemos.Release)
            state.tapControl(ZipperDemos.tap(0f))
        }
        mainClock.advanceTimeBy(3_000)
        waitForIdle()
        assertFalse(zipper.held)
        assertTrue(zipper.pos < .05f, "pos ${zipper.pos}")
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertEquals(0, list.firstVisibleItemIndex)
        assertEquals("A", zipper.letter)
    }
}
