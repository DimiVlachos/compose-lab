package dev.dimvlachos.lab.pencilcupdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.pencilcup.CupTools
import dev.dimvlachos.lab.core.presentation.components.pencilcup.PencilCupState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.pencilcupdemo.PencilCupDemos
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PencilCupDemoUiTest {
    private fun ComposeUiTest.show(
        cup: PencilCupState,
        state: DemoState = DemoState(),
        play: Boolean = false,
    ) {
        setContent {
            LabTheme {
                Box(Modifier.size(412.dp, 800.dp)) {
                    PencilCupDemo(state, cup, reducedMotion = false)
                }
                if (play) LaunchedEffect(Unit) { PencilCupDemos.all.single().script.play(state) }
            }
        }
    }

    private fun ComposeUiTest.advanceUntil(what: String, most: Long = 6_000, done: () -> Boolean) {
        var waited = 0L
        while (!done()) {
            assertTrue(waited < most, "waited $most ms for $what")
            mainClock.advanceTimeByFrame()
            waited += 16
        }
    }

    @Test
    fun tappingTheCupFansTheToolsOutWithTheirLabels() = runComposeUiTest {
        val cup = PencilCupState()
        show(cup)
        onNodeWithText("Fieldbook · 6 notes").assertExists()
        onNodeWithTag("pencil-cup").performTouchInput {
            down(center)
            up()
        }
        advanceUntil("the labels") { (0 until CupTools.count).all { cup.labelShown(it) } }
        assertTrue(cup.isOpen)
        onNodeWithText("New checklist").assertExists()
    }

    @Test
    fun pickingALabelOpensItsSheetAndDoneAddsTheNote() = runComposeUiTest {
        val cup = PencilCupState()
        show(cup)
        onNodeWithTag("pencil-cup").performTouchInput {
            down(center)
            up()
        }
        advanceUntil("the labels") { cup.labelShown(CupTools.List) }
        mainClock.advanceTimeBy(300)
        onNodeWithTag("pencil-label-${CupTools.List}").performTouchInput {
            down(center)
            up()
        }
        assertTrue(cup.sheetOpen)
        mainClock.advanceTimeBy(600)
        onNodeWithText("Add an item").assertExists()
        onNodeWithTag("pencil-done").performClick()
        assertFalse(cup.sheetOpen)
        mainClock.advanceTimeBy(800)
        onNodeWithText("Fieldbook · 7 notes").assertExists()
        onNodeWithText("Checklist").assertExists()
    }

    @Test
    fun theCardsUnderANewNoteGlideDownRatherThanJump() = runComposeUiTest {
        val cup = PencilCupState()
        show(cup)
        onNodeWithTag("pencil-cup").performTouchInput {
            down(center)
            up()
        }
        advanceUntil("the labels") { cup.labelShown(CupTools.List) }
        mainClock.advanceTimeBy(300)
        onNodeWithTag("pencil-label-${CupTools.List}").performTouchInput {
            down(center)
            up()
        }
        mainClock.advanceTimeBy(600)
        val first = {
            onNodeWithText("Saturday soccer carpool", useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
                .top
        }
        val before = first()
        mainClock.autoAdvance = false
        onNodeWithTag("pencil-done").performClick()
        mainClock.advanceTimeByFrame()
        val step = first() - before
        assertTrue(step < 4.dp, "the first card jumped ${step} on the first frame")
        mainClock.advanceTimeBy(800)
        assertTrue(first() - before > 40.dp, "the first card made room for the new note")
    }

    private fun ComposeUiTest.openListSheet(cup: PencilCupState) {
        onNodeWithTag("pencil-cup").performTouchInput {
            down(center)
            up()
        }
        advanceUntil("the labels") { cup.labelShown(CupTools.List) }
        mainClock.advanceTimeBy(300)
        onNodeWithTag("pencil-label-${CupTools.List}").performTouchInput {
            down(center)
            up()
        }
        mainClock.advanceTimeBy(600)
        assertTrue(cup.sheetOpen)
    }

    @Test
    fun aTapOutsideTheSheetClosesItWithoutANewNote() = runComposeUiTest {
        val cup = PencilCupState()
        show(cup)
        openListSheet(cup)
        onRoot().performTouchInput {
            down(Offset(center.x, 40f))
            up()
        }
        assertFalse(cup.sheetOpen)
        advanceUntil("the checklist tool back in the cup") { cup.settled }
        onNodeWithText("Fieldbook · 6 notes").assertExists()
        onNodeWithText("Checklist").assertDoesNotExist()
    }

    @Test
    fun escapeClosesTheSheetWithoutANewNote() = runComposeUiTest {
        val cup = PencilCupState()
        show(cup)
        openListSheet(cup)
        onNodeWithTag("pencil-sheet").performKeyInput { pressKey(Key.Escape) }
        assertFalse(cup.sheetOpen)
        mainClock.advanceTimeBy(800)
        onNodeWithText("Fieldbook · 6 notes").assertExists()
        onNodeWithText("Checklist").assertDoesNotExist()
    }

    @Test
    fun theScriptEndsAsItBegan() = runComposeUiTest {
        val cup = PencilCupState()
        val state = DemoState()
        show(cup, state, play = true)
        advanceUntil("the note's sheet", most = 9_000) { cup.sheetOpen }
        assertTrue(cup.sheetTool == CupTools.Note)
        advanceUntil("Done", most = 4_000) { !cup.sheetOpen }
        mainClock.advanceTimeBy(4_000)
        onNodeWithText("Fieldbook · 6 notes").assertExists()
        assertTrue(cup.settled)
        assertFalse(cup.isOpen)
    }

    @Test
    fun theCupsTouchTargetFollowsTheCupWhenTheScreenChangesSize() = runComposeUiTest {
        val cup = PencilCupState()
        var height by mutableStateOf(800.dp)
        setContent {
            LabTheme {
                Box(Modifier.size(412.dp, height)) {
                    PencilCupDemo(DemoState(), cup, reducedMotion = false)
                }
            }
        }
        val before = onNodeWithTag("pencil-cup").getUnclippedBoundsInRoot().top
        height = 640.dp
        waitForIdle()
        val after = onNodeWithTag("pencil-cup").getUnclippedBoundsInRoot().top
        assertTrue(after < before - 50.dp, "before $before, after $after")
        assertTrue(abs((after.value) - cup.cupTop) < 1f, "${after.value} vs ${cup.cupTop}")
    }
}
