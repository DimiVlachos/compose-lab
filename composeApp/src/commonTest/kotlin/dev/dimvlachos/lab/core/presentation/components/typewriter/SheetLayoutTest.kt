package dev.dimvlachos.lab.core.presentation.components.typewriter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SheetLayoutTest {
    @Test
    fun eachLetterStepsTheCarriageOneColumn() {
        val sheet = layOutSheet("hello")
        assertEquals(listOf("hello"), sheet.lines)
        assertEquals(0, sheet.line)
        assertEquals(5, sheet.column)
        assertEquals(listOf(0, 1, 2, 3, 4), sheet.glyphs.map { it.column })
    }

    @Test
    fun atTheMarginTheCarriageReturnsByItself() {
        val sheet = layOutSheet("abcdefghijklmnopqr")
        assertEquals(listOf("abcdefghijklmnopqr", ""), sheet.lines)
        assertEquals(1, sheet.line)
        assertEquals(0, sheet.column)
        // And on with the next letter on the new line, mid-word if it must.
        assertEquals(listOf("abcdefghijklmnopqr", "st"), layOutSheet("abcdefghijklmnopqrst").lines)
    }

    @Test
    fun aSpaceAfterTheBellReturnsTheCarriageAtTheEndOfTheWord() {
        assertEquals(listOf("see you at the", "port"), layOutSheet("see you at the port").lines)
        // Before the bell, a space is only a space.
        assertEquals(listOf("see you at"), layOutSheet("see you at").lines)
        assertEquals(10, layOutSheet("see you at").column)
    }

    @Test
    fun theSpaceThatEndsAWordAtTheMarginIsTakenUpByTheReturn() {
        val sheet = layOutSheet("abcdefghijklmnopqr s")
        assertEquals(listOf("abcdefghijklmnopqr", "s"), sheet.lines)
        assertEquals(19, sheet.glyphs.last().index)
    }

    @Test
    fun aLineBreakStartsANewLine() {
        val sheet = layOutSheet("hi\nthere")
        assertEquals(listOf("hi", "there"), sheet.lines)
        assertEquals(1, sheet.line)
        assertEquals(5, sheet.column)
    }

    @Test
    fun aLongMessageBreaksAtTheEndsOfItsWords() {
        assertEquals(
            listOf("on the 7:25 from", "boston. window", "seat, left side."),
            layOutSheet("on the 7:25 from boston. window seat, left side.").sent,
        )
    }

    @Test
    fun aSheetHoldsFourLinesAndLeavesOffWhatFollows() {
        val four = "a\nb\nc\nd"
        val sheet = layOutSheet(four)
        assertEquals(4, sheet.lines.size)
        assertFalse(sheet.overflow)
        assertTrue(layOutSheet("$four\ne").overflow)
        // The last line's margin fills the sheet; the next letter would fall off it.
        val full = "a\nb\nc\n" + "x".repeat(18)
        assertTrue(layOutSheet(full).full)
        assertFalse(layOutSheet(full).overflow)
        assertTrue(layOutSheet(full + "y").overflow)
    }

    @Test
    fun whatIsSentLeavesOffBlankLinesAtTheFoot() {
        assertEquals(listOf("abcdefghijklmnopqr"), layOutSheet("abcdefghijklmnopqr").sent)
        assertEquals(listOf("hi"), layOutSheet("hi\n\n").sent)
        assertFalse(layOutSheet("   ").inked)
        assertTrue(layOutSheet(" a ").inked)
    }

    @Test
    fun aSlipLaysItsLettersOutWhereTheSheetHadThem() {
        val sheet = layOutSheet("see you at the port")
        assertEquals(
            sheet.glyphs.map { it.line to it.column },
            glyphsOf(sheet.sent).map { it.line to it.column },
        )
        assertEquals(sheet.glyphs.map { it.strike }, glyphsOf(sheet.sent).map { it.strike })
    }

    @Test
    fun aLettersStrikeIsTheSameEveryTimeItIsDrawn() {
        val first = strikeOf(2, 7, 'q')
        repeat(5) { assertEquals(first, strikeOf(2, 7, 'q')) }
        assertNotEquals(first, strikeOf(2, 8, 'q'))
        assertNotEquals(first, strikeOf(2, 7, 'w'))
    }

    @Test
    fun aStrikeStaysCloseToItsPlace() {
        for (line in 0 until 4) {
            for (column in 0 until 18) {
                for (char in "aqz.,M") {
                    val strike = strikeOf(line, column, char)
                    assertTrue(kotlin.math.abs(strike.dx) <= TypewriterDimens.Wobble)
                    assertTrue(kotlin.math.abs(strike.dy) <= TypewriterDimens.Bob)
                    assertTrue(kotlin.math.abs(strike.tilt) <= TypewriterDimens.Tilt)
                    assertTrue(strike.ink in TypewriterDimens.LeastInk..1f)
                }
            }
        }
    }

    @Test
    fun aSlipIsAlwaysALittleTurnedAndTheSameEachTime() {
        for (id in 0L until 40L) {
            val tilt = slipTilt(id)
            assertEquals(tilt, slipTilt(id))
            assertTrue(
                kotlin.math.abs(tilt) in
                    TypewriterDimens.SlipLeastTilt..TypewriterDimens.SlipMostTilt
            )
        }
    }

    @Test
    fun onTheLastLineASpacePastTheBellIsOnlyASpace() {
        val last = "a\nb\nc\n" + "x".repeat(TypewriterDimens.BellColumn + 1) + " "
        val sheet = layOutSheet(last)
        assertFalse(sheet.full)
        assertEquals(3, sheet.line)
        assertEquals(TypewriterDimens.BellColumn + 2, sheet.column)
        // The columns left still take letters, up to the margin.
        val rest = "y".repeat(TypewriterDimens.Columns - sheet.column)
        assertFalse(layOutSheet(last + rest).overflow)
        assertTrue(layOutSheet(last + rest).full)
    }

    @Test
    fun whatFitsOnTheSheetIsKept() {
        assertEquals(5, layOutSheet("hello").kept)
        val long = "x".repeat(200)
        assertEquals(TypewriterDimens.Columns * TypewriterDimens.MaxLines, layOutSheet(long).kept)
        assertEquals(7, layOutSheet("a\nb\nc\nd\ne").kept)
    }
}
