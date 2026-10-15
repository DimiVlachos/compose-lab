package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.saveable.SaverScope
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypewriterStateTest {
    private class Heard : TypewriterFeel {
        val events = mutableListOf<Pair<Float, String>>()
        var clock: () -> Float = { 0f }

        override fun strike(variant: Int, space: Boolean) {
            events += clock() to if (space) "space" else "strike"
        }

        override fun bell() {
            events += clock() to "bell"
        }

        override fun carriageReturn() {
            events += clock() to "return"
        }

        override fun bang() {
            events += clock() to "bang"
        }

        override fun tape() {
            events += clock() to "tape"
        }

        override fun ratchet(pulling: Boolean) {
            events += clock() to if (pulling) "ratchet pulled" else "ratchet"
        }

        override fun release() {
            events += clock() to "release"
        }

        override fun jam() {
            events += clock() to "jam"
        }

        fun count(name: String) = events.count { it.second == name }

        fun first(name: String) = events.first { it.second == name }.first
    }

    private fun machine(): Pair<TypewriterState, Heard> {
        val state = TypewriterState()
        val heard = Heard()
        heard.clock = { state.now }
        state.feel = heard
        return state to heard
    }

    // Types [text] a key at a time, a frame or two apart, as a hand would.
    private fun TypewriterState.type(text: String) {
        for (char in text) {
            val now = field.text.toString() + char
            field.setTextAndPlaceCursorAtEnd(now)
            typedNow(now)
            run(0.03f)
        }
    }

    private fun TypewriterState.run(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            advance(minOf(left, 1f / 60f))
            left -= 1f / 60f
        }
    }

    @Test
    fun eachLetterStrikesAndTheCarriageStepsOnAColumn() {
        val (state, heard) = machine()
        state.type("hey")
        state.run(0.5f)
        assertEquals(3, heard.count("strike"))
        assertEquals(TypewriterState.columnX(3), state.carriage, 0.05f)
    }

    @Test
    fun aStruckLetterLandsOnThePaperAMomentAfterTheKey() {
        val (state, _) = machine()
        state.typedNow("a")
        val glyph = state.sheet.glyphs.single()
        assertFalse(state.landed(glyph))
        state.run(TypewriterDimens.LandSeconds + 0.02f)
        assertTrue(state.landed(glyph))
    }

    @Test
    fun theBellRingsOnceFiveLettersBeforeTheMargin() {
        val (state, heard) = machine()
        state.type("abcdefghijkl")
        assertEquals(0, heard.count("bell"))
        state.type("m")
        assertEquals(1, heard.count("bell"))
        state.type("nop")
        assertEquals(1, heard.count("bell"))
    }

    @Test
    fun atTheMarginTheLeverIsPushedAndTheCarriageBangsHomeAsItsSoundDoes() {
        val (state, heard) = machine()
        state.type("abcdefghijklmnopqr")
        val pushed = heard.first("return")
        state.run(1f)
        assertEquals(1, heard.count("return"))
        val bang = heard.first("bang") - pushed
        assertTrue(abs(bang - TypewriterDimens.ReturnBang) < 0.03f, "banged after $bang s")
        assertEquals(TypewriterDimens.HomeX, state.carriage, 0.05f)
        // The paper has fed a line.
        assertEquals(TypewriterDimens.LineHeight, state.feed, 0.05f)
    }

    @Test
    fun backspaceRunsTheCarriageBackAndTheTapeLiftsTheLetterOff() {
        val (state, heard) = machine()
        state.type("cat")
        state.run(0.5f)
        state.field.setTextAndPlaceCursorAtEnd("ca")
        state.typedNow("ca")
        assertEquals(1, heard.count("tape"))
        assertEquals(listOf('t'), state.lifting.map { it.glyph.char })
        state.run(0.5f)
        assertTrue(state.lifting.isEmpty())
        assertEquals(TypewriterState.columnX(2), state.carriage, 0.05f)
    }

    @Test
    fun backspacingOverAReturnRollsThePaperBackALine() {
        val (state, _) = machine()
        state.type("abcdefghijklmnopqr")
        state.run(1f)
        state.typedNow("abcdefghijklmnopq")
        state.run(1f)
        assertEquals(0f, state.feed, 0.05f)
        assertEquals(TypewriterState.columnX(17), state.carriage, 0.05f)
    }

    @Test
    fun onceStillTheMachineSleeps() {
        val (state, _) = machine()
        state.type("hi")
        assertTrue(state.awake)
        state.run(2f)
        assertFalse(state.awake)
    }

    @Test
    fun aShortPullSlidesBackIn() {
        val (state, heard) = machine()
        var sent: SentSheet? = null
        state.onSent = { sent = it }
        state.type("hello")
        state.run(0.5f)
        assertTrue(state.grab())
        repeat(3) {
            state.drag(10f)
            state.run(0.05f)
        }
        state.release(0f)
        state.run(2f)
        assertNull(sent)
        assertEquals(0f, state.shown, 0.01f)
        assertTrue(heard.count("ratchet pulled") > 0)
        assertEquals(SheetPhase.Idle, state.phase)
    }

    @Test
    fun aFullPullSendsTheSheetAndAFreshOneFeedsIn() {
        val (state, heard) = machine()
        var sent: SentSheet? = null
        state.onSent = { sent = it }
        state.type("see you at the port")
        state.run(1f)
        assertTrue(state.grab())
        state.drag(140f)
        state.release(0f)
        assertEquals(SheetPhase.Zipping, state.phase)
        assertEquals(1, heard.count("release"))
        state.run(0.3f)
        val sheet = assertNotNull(sent)
        assertEquals("see you at the port", sheet.text)
        assertEquals(listOf("see you at the", "port"), sheet.lines)
        assertEquals("", state.field.text.toString())
        assertFalse(state.inked)
        state.run(2f)
        assertEquals(SheetPhase.Idle, state.phase)
        assertEquals(TypewriterDimens.HomeX, state.carriage, 0.05f)
        // Clearing the field lifts nothing off the fresh sheet.
        state.typedNow(state.field.text)
        assertEquals(0, heard.count("tape"))
    }

    @Test
    fun aFlingSendsAShortPull() {
        val (state, _) = machine()
        var sent = false
        state.onSent = { sent = true }
        state.type("hi")
        state.grab()
        state.drag(30f)
        state.release(TypewriterDimens.FlingSpeed + 200f)
        state.run(0.5f)
        assertTrue(sent)
    }

    @Test
    fun aBlankSheetIsNotSent() {
        val (state, _) = machine()
        var sent = false
        state.onSent = { sent = true }
        state.type("   ")
        assertFalse(state.send())
        state.grab()
        state.drag(400f)
        state.release(5_000f)
        state.run(1f)
        assertFalse(sent)
    }

    @Test
    fun sendPullsTheSheetOutByItself() {
        val (state, _) = machine()
        var sent: SentSheet? = null
        state.onSent = { sent = it }
        state.type("ok")
        assertTrue(state.send())
        state.run(TypewriterDimens.AutoPullSeconds + TypewriterDimens.ZipSeconds + 0.1f)
        assertEquals("ok", sent?.text)
    }

    @Test
    fun aBlankSheetCannotBeSentByDeletingWhileItIsPulledOut() {
        val (state, _) = machine()
        var sent: SentSheet? = null
        state.onSent = { sent = it }
        state.type('a')
        assertTrue(state.send())
        // Deleted as it starts out of the roller, by itself: refused, so what is sent is the
        // letter.
        state.backspace()
        assertFalse(state.type('b'))
        assertEquals("a", state.field.text.toString())
        state.run(TypewriterDimens.AutoPullSeconds + TypewriterDimens.ZipSeconds + 0.1f)
        assertEquals("a", sent?.text)
    }

    @Test
    fun deletingASpaceOrALineStillRunsTheTape() {
        val (state, heard) = machine()
        state.type('a')
        state.type(' ')
        state.backspace()
        assertEquals(1, heard.count("tape"))
        state.type('\n')
        state.backspace()
        assertEquals(2, heard.count("tape"))
        // The space the return took up at the margin, too.
        "abcdefghijklmnopqr ".forEach { state.type(it) }
        state.backspace()
        assertEquals(3, heard.count("tape"))
    }

    @Test
    fun aSecondFingerCannotTakeAHeldSheet() {
        val (state, _) = machine()
        state.type('a')
        assertTrue(state.grab())
        assertFalse(state.grab())
    }

    @Test
    fun aScreenReaderHearsWhatWasTypedAndDeletedSinceItLastHeard() {
        val (state, _) = machine()
        "cat".forEach { state.type(it) }
        state.backspace()
        assertEquals(Speech("ca", "", jam = false), state.speech)
        state.speech = Speech.Quiet
        state.backspace()
        state.backspace()
        assertEquals(Speech("", "ca", jam = false), state.speech)
    }

    @Test
    fun aFullSheetJamsAndSaysSo() {
        val (state, heard) = machine()
        state.jam()
        assertTrue(state.jammed)
        assertEquals(1, heard.count("jam"))
        state.run(TypewriterDimens.JamSeconds + 0.1f)
        assertFalse(state.jammed)
    }

    @Test
    fun theReturnsLeverHoldIsLongerTheShorterTheTrip() {
        val full = TypewriterState.travelTime(TypewriterDimens.Columns * TypewriterDimens.CharWidth)
        val short = TypewriterState.travelTime(TypewriterDimens.CharWidth)
        assertTrue(short < full)
        assertTrue(full < TypewriterDimens.ReturnBang - TypewriterDimens.LeverShortest)
    }

    @Test
    fun aMachineBegunWithTextKeepsOnlyWhatItsSheetAndKeysCanTake() {
        val state = TypewriterState("x".repeat(200))
        assertEquals(TypewriterDimens.Columns * TypewriterDimens.MaxLines, state.text.length)
        assertEquals(state.sheet.lines.joinToString(""), state.text)
        // A character the machine has no key for is left out.
        assertEquals("caf ok", TypewriterState("caf\u00e9 ok").text)
    }

    @Test
    fun clearLiftsEverythingOff() {
        val (state, heard) = machine()
        state.type("hey")
        state.run(0.5f)
        state.clear()
        assertEquals("", state.text)
        assertEquals(1, heard.count("tape"))
        assertFalse(state.inked)
    }

    @Test
    fun deleteTakesTheSpaceAReturnSwallowedWithTheLetterBeforeIt() {
        val (state, heard) = machine()
        val line = "abcdefghijklmnopqr"
        state.type("$line ")
        state.run(1f)
        assertEquals(1, state.sheet.line)
        assertEquals(0, state.sheet.column)
        state.backspace()
        state.run(1f)
        assertEquals(line.dropLast(1), state.text)
        assertEquals(0, state.sheet.line)
        assertEquals(1, heard.count("tape"))
    }

    @Test
    fun savedAndMadeAgainTheMachineKeepsItsTextShiftAndPageAndMakesNoSound() {
        val state = TypewriterState()
        "see you".forEach { state.type(it) }
        state.press(TypeKey(KeyAction.Page))
        state.press(TypeKey(KeyAction.Shift))
        val saved = with(TypewriterState.Saver) { SaverScope { true }.save(state) }!!
        val restored = TypewriterState.Saver.restore(saved)!!
        assertEquals("see you", restored.text)
        assertTrue(restored.keyboard.figures)
        assertTrue(restored.keyboard.shifted)
        assertEquals(state.sheet.lines, restored.sheet.lines)
        val heard = Heard()
        restored.feel = heard
        // The composer hands the machine its text again as it opens.
        restored.typedNow(restored.text)
        restored.run(1f)
        assertTrue(heard.events.isEmpty(), "made again, it sounded ${heard.events}")
        assertEquals(TypewriterState.columnX(7), restored.carriage, 0.05f)
    }
}
