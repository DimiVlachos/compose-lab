package dev.dimvlachos.lab.core.presentation.components.padlock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordStrengthTest {
    @Test
    fun anEmptyFieldIsWaiting() {
        assertEquals(StrengthLevel.Empty, checkPassword("").level)
    }

    @Test
    fun theScriptsPasswordClimbsEveryLevel() {
        assertEquals(StrengthLevel.Weak, checkPassword("maple").level)
        assertEquals(StrengthLevel.Fair, checkPassword("maple42").level)
        assertEquals(StrengthLevel.Fair, checkPassword("maple42-").level)
        assertEquals(StrengthLevel.Good, checkPassword("maple42-R").level)
        assertEquals(StrengthLevel.Good, checkPassword("maple42-Riv").level)
        assertEquals(StrengthLevel.Strong, checkPassword("maple42-Rive").level)
        assertEquals(StrengthLevel.Strong, checkPassword("maple42-River!").level)
    }

    @Test
    fun aCommonPasswordStaysWeakWhateverItsLength() {
        assertEquals(StrengthLevel.Weak, checkPassword("password").level)
        assertEquals(StrengthLevel.Weak, checkPassword("Baseball").level)
        assertTrue(checkPassword("QWERTY").common)
    }

    @Test
    fun lengthAloneReachesFairButNoFurther() {
        assertEquals(StrengthLevel.Fair, checkPassword("abcdefghijklmnop").level)
        assertEquals(StrengthLevel.Weak, checkPassword("abcdefg").level)
        assertEquals(StrengthLevel.Fair, checkPassword("abcde1").level)
    }

    @Test
    fun theRulesUnderTheFieldTrackLengthNumberAndMix() {
        val c = checkPassword("maple42")
        assertFalse(c.longEnough)
        assertTrue(c.hasNumber)
        assertFalse(c.hasMix)
        assertTrue(checkPassword("mapleR").hasMix)
        assertTrue(checkPassword("maple-").hasMix)
        assertTrue(checkPassword("maple42-River").longEnough)
    }

    @Test
    fun theTipNamesAtMostTwoNeedsInOrder() {
        assertEquals(listOf(Need.Characters(7), Need.Number), needs(checkPassword("maple")))
        assertEquals(listOf(Need.Characters(5), Need.Capital), needs(checkPassword("maple42")))
        assertEquals(listOf(Need.Characters(4), Need.Symbol), needs(checkPassword("maple42R")))
        assertEquals(listOf(Need.Number, Need.Capital), needs(checkPassword("abcdefghijklm")))
        assertEquals(emptyList(), needs(checkPassword("maple42-River!")))
    }
}
