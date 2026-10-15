package dev.dimvlachos.lab.core.presentation.components.typewriter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypewriterKeysTest {
    @Test
    fun theLettersAreQwertyWithShiftDeleteAndReturn() {
        val rows = TypewriterLayout.Letters
        assertEquals("qwertyuiop", rows[0].joinToString("") { it.id })
        assertEquals(KeyAction.Shift, rows[2].first().action)
        assertEquals(KeyAction.Backspace, rows[2].last().action)
        assertEquals(KeyAction.Return, rows[3].last().action)
        assertTrue(rows[3].any { it.action == KeyAction.Space })
    }

    @Test
    fun theFiguresHoldTheDigitsAndTheStops() {
        for (char in "0123456789.,?!'-:") {
            assertTrue(TypewriterLayout.pageOf(char) != null, "no key types $char")
        }
    }

    @Test
    fun aCapitalTakesShiftAndAStopTakesThePage() {
        assertEquals(
            listOf("Shift", "h"),
            keysFor('H', figures = false, shifted = false).map { it.name },
        )
        assertEquals(listOf("h"), keysFor('H', figures = false, shifted = true).map { it.name })
        assertEquals(
            listOf("Page", "?"),
            keysFor('?', figures = false, shifted = false).map { it.name },
        )
        assertEquals(
            listOf("Page", "a"),
            keysFor('a', figures = true, shifted = false).map { it.name },
        )
        assertEquals(listOf("Space"), keysFor(' ', figures = true, shifted = false).map { it.name })
        assertTrue(keysFor('é', figures = false, shifted = false).isEmpty())
    }

    @Test
    fun shiftHoldsForOneLetter() {
        val state = TypewriterState()
        state.press(TypeKey(KeyAction.Shift))
        state.press(TypeKey(KeyAction.Type('o')))
        state.press(TypeKey(KeyAction.Type('k')))
        assertEquals("Ok", state.field.text.toString())
        assertFalse(state.keyboard.shifted)
    }

    @Test
    fun shiftHoldsThroughARefusedKey() {
        val state = TypewriterState()
        "a\nb\nc\n".forEach { state.type(it) }
        repeat(18) { state.type('x') }
        state.press(TypeKey(KeyAction.Shift))
        state.press(TypeKey(KeyAction.Type('y')))
        assertTrue(state.keyboard.shifted)
    }

    @Test
    fun aKeyOffAFullSheetJamsAndTypesNothing() {
        val state = TypewriterState()
        "a\nb\nc\n".forEach { state.type(it) }
        repeat(18) { state.type('x') }
        assertFalse(state.type('y'))
        assertTrue(state.jammed)
        assertEquals("a\nb\nc\n" + "x".repeat(18), state.field.text.toString())
    }

    private val TypeKey.name: String
        get() =
            when (val action = action) {
                is KeyAction.Type -> action.char.toString()
                else -> action.toString()
            }
}
