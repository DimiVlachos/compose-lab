package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun whatIsTypedInTheFieldIsStruckOnTheSheet() = runComposeUiTest {
        val machine = machine()
        onAllNodes(hasSetTextAction()).onFirst().performTextInput("hello there")
        mainClock.advanceTimeBy(500)
        assertEquals(listOf("hello there"), machine.state.sheet.lines)
        assertTrue(machine.state.inked)
    }

    @Test
    fun aScreenReadersSendActionSendsTheSheet() = runComposeUiTest {
        val machine = machine()
        val field = onAllNodes(hasSetTextAction()).onFirst()
        field.performTextInput("see you")
        mainClock.advanceTimeBy(300)
        val send =
            field.fetchSemanticsNode().config[SemanticsActions.CustomActions].first {
                it.label == "Send"
            }
        runOnUiThread { send.action() }
        mainClock.advanceTimeBy(1_000)
        assertEquals(listOf("see you"), machine.sent.map { it.text })
        assertEquals("", machine.state.field.text.toString())
    }

    @Test
    fun ctrlAndReturnSendsAndReturnAloneStartsALine() = runComposeUiTest {
        val machine = machine()
        val field = onAllNodes(hasSetTextAction()).onFirst()
        field.performTextInput("hi")
        mainClock.advanceTimeBy(300)
        field.performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Enter) } }
        mainClock.advanceTimeBy(1_000)
        assertEquals(listOf("hi"), machine.sent.map { it.text })
    }

    @Test
    fun aDragUpTheSheetPullsItOutAndAShortOneSlidesBack() = runComposeUiTest {
        val machine = machine()
        onAllNodes(hasSetTextAction()).onFirst().performTextInput("ahoy")
        mainClock.advanceTimeBy(500)
        val unit = 400f / TypewriterDimens.StageUnits
        // On the sheet, just over the platen, where the paper shows.
        fun onSheet(height: Float, density: Float): androidx.compose.ui.geometry.Offset {
            val u = unit * density
            val machineTop = height - TypewriterDimens.MachineHeight * u
            return androidx.compose.ui.geometry.Offset(
                (machine.state.carriage + 40f) * u,
                machineTop + (TypewriterDimens.StrikeY - 10f) * u,
            )
        }
        onNodeWithTag("machine").performTouchInput {
            val from = onSheet(height.toFloat(), density)
            down(from)
            for (i in 1..6) moveTo(from.copy(y = from.y - i * 4.dp.toPx()))
            up()
        }
        mainClock.advanceTimeBy(1_500)
        assertTrue(machine.sent.isEmpty(), "a short pull sent the sheet")
        assertEquals(0f, machine.state.shown, 0.5f)
        onNodeWithTag("machine").performTouchInput {
            val from = onSheet(height.toFloat(), density)
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
        val field = onAllNodes(hasSetTextAction()).onFirst()
        field.performTextInput("abcdefghijklmnopqrstuvw")
        mainClock.advanceTimeBy(2_000)
        assertEquals(before, compositions)
        assertNull(machine.sent.firstOrNull())
    }
}
