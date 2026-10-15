package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TypewriterComposerUiTest {
    private class Machine(val state: TypewriterState, val sent: MutableList<SentSheet>)

    private fun androidx.compose.ui.test.ComposeUiTest.machine(probe: () -> Unit = {}): Machine {
        val state = TypewriterState()
        val sent = mutableListOf<SentSheet>()
        setContent {
            LabTheme {
                CompositionLocalProvider(LocalTypewriterCompositionProbe provides probe) {
                    Box(Modifier.size(400.dp, 700.dp)) {
                        TypewriterComposer(
                            state,
                            onSend = { sent += it },
                            Modifier.testTag("machine"),
                        )
                    }
                }
            }
        }
        waitForIdle()
        return Machine(state, sent)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.press(vararg names: String) {
        for (name in names) {
            onNodeWithContentDescription(name, useUnmergedTree = true).performClick()
            mainClock.advanceTimeBy(60)
        }
    }

    @Test
    fun theKeysTypeOnTheSheetAndThePhonesKeyboardHasNothingToOpen() = runComposeUiTest {
        val machine = machine()
        press("Shift", "H", "i", "Space", "t", "h", "e", "r", "e")
        mainClock.advanceTimeBy(500)
        assertEquals(listOf("Hi there"), machine.state.sheet.lines)
        assertTrue(machine.state.inked)
        // No text field: nothing the phone's keyboard could open for.
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun deleteRunsTheTapeAndNewLineFeedsALine() = runComposeUiTest {
        val machine = machine()
        press("o", "k", "Delete", "Delete", "n", "o", "New line", "y", "e", "s")
        mainClock.advanceTimeBy(500)
        assertEquals(listOf("no", "yes"), machine.state.sheet.lines)
    }

    @Test
    fun theFiguresPageTypesStopsAndTurnsBack() = runComposeUiTest {
        val machine = machine()
        press("o", "k", "Numbers and punctuation", "?", "Letters", "a")
        mainClock.advanceTimeBy(500)
        assertEquals(listOf("ok?a"), machine.state.sheet.lines)
    }

    @Test
    fun aHardwareKeyboardTypesAndCtrlReturnSends() = runComposeUiTest {
        val machine = machine()
        val node = onNodeWithTag("machine")
        node.performKeyInput {
            pressKey(Key.H)
            pressKey(Key.I)
        }
        mainClock.advanceTimeBy(300)
        assertEquals("hi", machine.state.field.text.toString())
        node.performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Enter) } }
        mainClock.advanceTimeBy(1_000)
        assertEquals(listOf("hi"), machine.sent.map { it.text })
    }

    @Test
    fun aHardwareKeyTheMachineHasNoKeyForTypesNothing() = runComposeUiTest {
        val machine = machine()
        val node = onNodeWithTag("machine")
        node.performKeyInput {
            pressKey(Key.Equals)
            pressKey(Key.Grave)
        }
        mainClock.advanceTimeBy(300)
        assertEquals("", machine.state.field.text.toString())
        // And one it has a key for still types after them.
        node.performKeyInput { pressKey(Key.A) }
        mainClock.advanceTimeBy(300)
        assertEquals("a", machine.state.field.text.toString())
    }

    @Test
    fun aScreenReadersSendActionSendsTheSheet() = runComposeUiTest {
        val machine = machine()
        press("s", "e", "e", "Space", "y", "o", "u")
        mainClock.advanceTimeBy(300)
        val sheet = onNodeWithContentDescription("Typewriter")
        val send =
            sheet.fetchSemanticsNode().config[SemanticsActions.CustomActions].first {
                it.label == "Send"
            }
        runOnUiThread { send.action() }
        mainClock.advanceTimeBy(1_000)
        assertEquals(listOf("see you"), machine.sent.map { it.text })
        assertEquals("", machine.state.field.text.toString())
    }

    // Where the machine's top is in the composer, [density] px to the dp: it stands on its keys,
    // which start just over their first row.
    private fun androidx.compose.ui.test.ComposeUiTest.machineTop(density: Float): Float {
        val u = 400f / TypewriterDimens.StageUnits * density
        val keysTop =
            onNodeWithContentDescription("q").fetchSemanticsNode().positionInRoot.y -
                onNodeWithTag("machine").fetchSemanticsNode().positionInRoot.y -
                6f * density
        return keysTop - TypewriterDimens.MachineHeight * u
    }

    @Test
    fun pushingTheLeverStartsANewLineAndLeavesTheSheetAlone() = runComposeUiTest {
        val machine = machine()
        runOnUiThread { "ok".forEach { machine.state.type(it) } }
        mainClock.advanceTimeBy(500)
        onNodeWithTag("machine").performTouchInput {
            val u = 400f / TypewriterDimens.StageUnits * density
            val knob =
                machine.state.leverKnob * u +
                    androidx.compose.ui.geometry.Offset(0f, machineTop(density))
            down(knob)
            for (i in 1..10) moveTo(
                knob.copy(x = knob.x + i * TypewriterDimens.LeverThrow / 10f * u)
            )
            up()
        }
        mainClock.advanceTimeBy(1_500)
        assertEquals("ok\n", machine.state.field.text.toString())
        assertTrue(machine.sent.isEmpty())
        assertEquals(0f, machine.state.shown, 0.01f)
    }

    @Test
    fun aScreenReadersNewLineActionOnTheLeverStartsALine() = runComposeUiTest {
        val machine = machine()
        val lever = onNodeWithContentDescription("Carriage return lever")
        val newLine =
            lever.fetchSemanticsNode().config[SemanticsActions.CustomActions].first {
                it.label == "New line"
            }
        runOnUiThread { newLine.action() }
        mainClock.advanceTimeBy(500)
        assertEquals("\n", machine.state.field.text.toString())
    }

    @Test
    fun aDragUpTheSheetPullsItOutAndAShortOneSlidesBack() = runComposeUiTest {
        val machine = machine()
        runOnUiThread { "ahoy".forEach { machine.state.type(it) } }
        mainClock.advanceTimeBy(500)
        val unit = 400f / TypewriterDimens.StageUnits
        // On the sheet, just over the platen, where the paper shows, over the keys.
        fun onSheet(density: Float): androidx.compose.ui.geometry.Offset {
            val u = unit * density
            // The machine stands on the keys, which start just over the first row.
            val keysTop =
                onNodeWithContentDescription("q").fetchSemanticsNode().positionInRoot.y -
                    onNodeWithTag("machine").fetchSemanticsNode().positionInRoot.y -
                    6f * density
            val machineTop = keysTop - TypewriterDimens.MachineHeight * u
            return androidx.compose.ui.geometry.Offset(
                (machine.state.carriage + 40f) * u,
                machineTop + (TypewriterDimens.StrikeY - 10f) * u,
            )
        }
        onNodeWithTag("machine").performTouchInput {
            val from = onSheet(density)
            down(from)
            for (i in 1..6) moveTo(from.copy(y = from.y - i * 4.dp.toPx()))
            up()
        }
        mainClock.advanceTimeBy(1_500)
        assertTrue(machine.sent.isEmpty(), "a short pull sent the sheet")
        assertEquals(0f, machine.state.shown, 0.5f)
        onNodeWithTag("machine").performTouchInput {
            val from = onSheet(density)
            down(from)
            for (i in 1..20) {
                advanceEventTime(30)
                moveTo(from.copy(y = from.y - i * 8.dp.toPx()))
            }
            up()
        }
        mainClock.advanceTimeBy(1_000)
        assertEquals(listOf("ahoy"), machine.sent.map { it.text })
    }

    @Test
    fun nothingRecomposesAsTheMachineMoves() = runComposeUiTest {
        var compositions = 0
        val machine = machine { compositions++ }
        val before = compositions
        runOnUiThread { "abcdefghijklmnopqrstuvw".forEach { machine.state.type(it) } }
        mainClock.advanceTimeBy(2_000)
        assertEquals(before, compositions)
        assertNull(machine.sent.firstOrNull())
    }

    @Test
    fun aMachineTakenAwayMidPullLetsGoOfTheSheet() = runComposeUiTest {
        val state = TypewriterState()
        var shown by mutableStateOf(true)
        setContent {
            LabTheme {
                Box(Modifier.size(400.dp, 700.dp)) {
                    if (shown) {
                        TypewriterComposer(state, onSend = {}, Modifier.testTag("machine"))
                    }
                }
            }
        }
        runOnUiThread { "ahoy".forEach { state.type(it) } }
        mainClock.advanceTimeBy(500)
        onNodeWithTag("machine").performTouchInput {
            val u = 400f / TypewriterDimens.StageUnits * density
            val keysTop =
                onNodeWithContentDescription("q").fetchSemanticsNode().positionInRoot.y -
                    onNodeWithTag("machine").fetchSemanticsNode().positionInRoot.y -
                    6f * density
            val machineTop = keysTop - TypewriterDimens.MachineHeight * u
            val from =
                androidx.compose.ui.geometry.Offset(
                    (state.carriage + 40f) * u,
                    machineTop + (TypewriterDimens.StrikeY - 10f) * u,
                )
            down(from)
            for (i in 1..6) moveTo(from.copy(y = from.y - i * 6.dp.toPx()))
        }
        mainClock.advanceTimeBy(100)
        // Held: no other finger may take it.
        assertFalse(state.grab())
        // The stage turns: the machine leaves under the finger.
        shown = false
        waitForIdle()
        assertTrue(state.grab(), "the sheet was left held")
        state.release(0f)
        state.advance(2f)
        repeat(240) { state.advance(1f / 60f) }
        assertFalse(state.awake, "the machine never went back to sleep")
    }
}
