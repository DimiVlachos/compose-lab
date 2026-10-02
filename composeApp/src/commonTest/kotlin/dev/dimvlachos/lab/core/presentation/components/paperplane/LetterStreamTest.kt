package dev.dimvlachos.lab.core.presentation.components.paperplane

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LetterStreamTest {
    @Test
    fun aShortMessageGoesInAtASteadyPace() {
        assertEquals(PaperPlaneDimens.LetterGapMs, letterGap(8))
        assertEquals(0f, letterGap(1))
    }

    @Test
    fun aLongMessageIsInWithinTheStreamsTime() {
        for (count in listOf(10, 40, 300)) {
            val last = (count - 1) * letterGap(count) + PaperPlaneDimens.LetterMs
            assertTrue(last <= PaperPlaneDimens.StreamMs + 0.01f, "$count letters take $last ms")
        }
    }
}
