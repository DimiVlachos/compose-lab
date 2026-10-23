package dev.dimvlachos.lab.paperfandemo.presentation.components

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.paperfan.PaperFanState
import dev.dimvlachos.lab.paperfandemo.PaperFanDemos
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class PaperFanDemoUiTest {
    private fun state(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun tappingAQuestionOpensItsFanAndReadsItsAnswer() = runComposeUiTest {
        val fans = PaperFanState(5)
        setContent { PaperFanDemo(DemoState(), fans) }
        onNodeWithTag("question-1").assert(state("Collapsed"))
        onNodeWithTag("question-1").performClick()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        assertTrue(fans.isOpen(1))
        assertTrue(fans.travel(1) > .95f, "travel ${fans.travel(1)}")
        onNodeWithTag("question-1").assert(state("Expanded"))
        onNodeWithTag("answer-1")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf(
                        "Send anything back within 60 days, worn or not. Print a free label " +
                            "from your order page or drop it at any store.",
                    ),
                ),
            )
    }

    @Test
    fun anotherQuestionFoldsTheFirstAway() = runComposeUiTest {
        val fans = PaperFanState(5)
        setContent { PaperFanDemo(DemoState(), fans) }
        onNodeWithTag("question-0").performClick()
        mainClock.advanceTimeBy(1_500)
        onNodeWithTag("question-4").performClick()
        mainClock.advanceTimeBy(1_800)
        waitForIdle()
        assertFalse(fans.isOpen(0))
        assertTrue(fans.isOpen(4))
        assertTrue(fans.heightFraction(0) == 0f)
    }

    @Test
    fun theScriptTapsSweepsAndResetsThroughTheFans() = runComposeUiTest {
        val state = DemoState()
        val fans = PaperFanState(5)
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            PaperFanDemo(state, fans)
        }
        waitForIdle()
        scope.launch { state.tapControl(PaperFanDemos.tap(3)) }
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        assertTrue(fans.isOpen(3))
        scope.launch { state.tapControl(PaperFanDemos.fold(3)) }
        mainClock.advanceTimeBy(6_000)
        waitForIdle()
        assertFalse(fans.isOpen(3))
        scope.launch { state.resetFeed() }
        waitForIdle()
        assertTrue(fans.settled)
    }
}
