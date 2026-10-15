package dev.dimvlachos.lab.core.presentation.components.typewriter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypewriterLeverTest {
    private class Heard : TypewriterFeel {
        val events = mutableListOf<String>()

        override fun ratchet(pulling: Boolean) {
            events += if (pulling) "pawl" else "feed"
        }

        override fun carriageReturn() {
            events += "return"
        }

        override fun bang() {
            events += "bang"
        }

        override fun tape() {
            events += "tape"
        }

        override fun jam() {
            events += "jam"
        }

        fun count(name: String) = events.count { it == name }
    }

    private fun machine(text: String = ""): Pair<TypewriterState, Heard> {
        val state = TypewriterState()
        text.forEach { state.type(it) }
        state.run(1f)
        val heard = Heard()
        state.feel = heard
        return state to heard
    }

    private fun TypewriterState.run(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            advance(minOf(left, 1f / 60f))
            left -= 1f / 60f
        }
    }

    // Pushes the lever to [to] of its travel in a few steps, a frame apart.
    private fun TypewriterState.push(to: Float) {
        for (i in 1..6) {
            pushLever(to * i / 6f)
            run(1f / 60f)
        }
    }

    @Test
    fun pushedPastTheThresholdTheLeverFeedsALineAndTheCarriageBangsHome() {
        val (state, heard) = machine("see you")
        assertTrue(state.grabLever())
        state.push(TypewriterDimens.LeverCommit + 0.2f)
        state.releaseLever(0f)
        state.run(1.5f)
        assertEquals("see you\n", state.field.text.toString())
        assertEquals(1, state.sheet.line)
        assertEquals(TypewriterDimens.HomeX, state.carriage, 0.05f)
        assertEquals(1, heard.count("return"))
        assertEquals(1, heard.count("bang"))
    }

    @Test
    fun theLineSpacePawlClicksOnceAsItCatches() {
        val (state, heard) = machine("see you")
        state.grabLever()
        state.push(TypewriterDimens.LeverPawl - 0.05f)
        assertEquals(0, heard.count("pawl"))
        assertEquals(0, state.sheet.line)
        state.push(1f)
        assertEquals(1, heard.count("pawl"))
        assertEquals(1, state.sheet.line)
        state.releaseLever(0f)
        state.run(1f)
        assertEquals(1, heard.count("pawl"))
    }

    @Test
    fun pushedPastThePawlTheCarriageFollowsTheLever() {
        val (state, _) = machine("see you at the")
        val from = state.carriage
        state.grabLever()
        state.push(TypewriterDimens.LeverPawl + (1f - TypewriterDimens.LeverPawl) / 2f)
        state.run(0.3f)
        assertTrue(state.carriage > from + 10f, "the carriage stayed at ${state.carriage}")
        assertTrue(state.carriage < TypewriterDimens.HomeX - 10f)
    }

    @Test
    fun letGoShortTheLeverSpringsBackAndTheFeedIsUndone() {
        val (state, heard) = machine("see you")
        state.grabLever()
        state.push(TypewriterDimens.LeverPawl + 0.05f)
        assertEquals(1, state.sheet.line)
        state.releaseLever(0f)
        state.run(1f)
        assertEquals("see you", state.field.text.toString())
        assertEquals(0, state.sheet.line)
        assertEquals(TypewriterState.columnX(7), state.carriage, 0.05f)
        assertEquals(0f, state.leverPush, 0.01f)
        // Undone quietly: no tape, and no return.
        assertEquals(0, heard.count("tape"))
        assertEquals(0, heard.count("return"))
    }

    @Test
    fun aShortPushNeverPastThePawlChangesNothing() {
        val (state, _) = machine("ok")
        state.grabLever()
        state.push(0.1f)
        state.releaseLever(0f)
        state.run(1f)
        assertEquals("ok", state.field.text.toString())
    }

    @Test
    fun aFlingCompletesTheReturn() {
        val (state, heard) = machine("ok")
        state.grabLever()
        state.push(0.2f)
        state.releaseLever(TypewriterDimens.LeverFling + 1f)
        state.run(1.5f)
        assertEquals("ok\n", state.field.text.toString())
        assertEquals(1, heard.count("bang"))
    }

    @Test
    fun onAnEmptyLineItFeedsABlankLine() {
        val (state, _) = machine()
        state.grabLever()
        state.push(1f)
        state.releaseLever(0f)
        state.run(1f)
        assertEquals("\n", state.field.text.toString())
        assertEquals(1, state.sheet.line)
    }

    @Test
    fun onAFullSheetItJamsAndFeedsNothing() {
        val (state, heard) = machine("a\nb\nc\nd")
        state.grabLever()
        state.push(1f)
        state.releaseLever(0f)
        state.run(1f)
        assertEquals("a\nb\nc\nd", state.field.text.toString())
        assertEquals(1, heard.count("jam"))
        assertEquals(0, heard.count("return"))
    }

    @Test
    fun theLeverAndTheSheetTakeOneFingerAtATime() {
        val (state, _) = machine("hi")
        assertTrue(state.grabLever())
        assertFalse(state.grab())
        state.releaseLever(0f)
        assertTrue(state.grab())
        assertFalse(state.grabLever())
    }

    @Test
    fun theNewLineKeyStillSwingsTheLeverByItself() {
        val (state, heard) = machine("hi")
        state.press(TypeKey(KeyAction.Return))
        state.run(0.05f)
        assertTrue(state.leverPush > 0f)
        state.run(1f)
        assertEquals(1, heard.count("return"))
    }

    @Test
    fun whileTheLeverIsHeldKeysTypeNothingAndTheShortPushStillUndoesOnlyItsLine() {
        val (state, _) = machine("see you")
        state.grabLever()
        state.push(TypewriterDimens.LeverPawl + 0.1f)
        assertEquals("see you\n", state.field.text.toString())
        // A second finger on a key, or a hardware key, while the hand is on the lever.
        assertFalse(state.type('x'))
        state.backspace()
        assertEquals("see you\n", state.field.text.toString())
        state.push(TypewriterDimens.LeverPawl + 0.15f)
        state.releaseLever(0f)
        state.run(1f)
        assertEquals("see you", state.field.text.toString())
        assertEquals(TypewriterState.columnX(7), state.carriage, 0.05f)
    }

    @Test
    fun theShortPushsUndoLeavesALetterTypedAfterItsLine() {
        val (state, _) = machine("ok")
        state.grabLever()
        state.push(TypewriterDimens.LeverPawl + 0.1f)
        // Something past the keys put a letter after the line the pawl fed.
        state.field.edit { append('x') }
        state.typedNow(state.field.text)
        state.releaseLever(0f)
        state.run(1f)
        assertEquals("ok\nx", state.field.text.toString())
    }

    @Test
    fun whileTheLeverIsHeldTheSheetIsNotSentAndTheNextSheetStartsClean() {
        val (state, heard) = machine("hello")
        state.grabLever()
        state.push(0.2f)
        assertFalse(state.send())
        state.releaseLever(0f)
        state.run(1f)
        assertTrue(state.send())
        state.run(2f)
        assertEquals(SheetPhase.Idle, state.phase)
        // A fresh sheet: the lever takes a new push from nothing.
        assertTrue(state.grabLever())
        state.push(1f)
        state.releaseLever(0f)
        state.run(1.5f)
        assertEquals("\n", state.field.text.toString())
        assertEquals(0, heard.count("jam"))
    }

    @Test
    fun theMachineSleepsAfterALeverCycleAndAfterAJam() {
        val (state, _) = machine("see you")
        state.grabLever()
        state.push(1f)
        state.releaseLever(0f)
        state.run(2f)
        assertFalse(state.awake, "awake after a return")
        val (full, _) = machine("a\nb\nc\n" + "x".repeat(TypewriterDimens.Columns))
        assertFalse(full.type('e'))
        full.run(0.1f)
        assertTrue(full.awake)
        full.run(TypewriterDimens.JamSeconds + 1f)
        assertFalse(full.awake, "awake after a jam")
    }
}
