package dev.dimvlachos.lab.core.audio

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Android's only clue to "will not ask again": whether it would show a rationale, before and after.
class RefusedForGoodTest {
    @Test
    fun aFirstRefusalInTheDialogIsNotForGood() {
        assertFalse(refusedForGood(rationaleBefore = false, rationaleAfter = true, refusals = 1))
    }

    @Test
    fun aFirstDialogDismissedByTappingOutsideIsNotForGood() {
        assertFalse(refusedForGood(rationaleBefore = false, rationaleAfter = false, refusals = 1))
    }

    @Test
    fun aSecondRefusalAfterARationaleIsForGood() {
        assertTrue(refusedForGood(rationaleBefore = true, rationaleAfter = false, refusals = 2))
    }

    @Test
    fun refusedWithoutADialogTwiceIsForGood() {
        // Already refused for good before this visit: Android answers at once, with no dialog.
        assertTrue(refusedForGood(rationaleBefore = false, rationaleAfter = false, refusals = 2))
    }
}
