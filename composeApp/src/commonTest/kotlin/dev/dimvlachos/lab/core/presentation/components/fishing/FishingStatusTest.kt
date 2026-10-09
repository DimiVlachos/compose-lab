package dev.dimvlachos.lab.core.presentation.components.fishing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class FishingStatusTest {
    @Test
    fun aCatchOfNothingIsRefused() {
        assertFailsWith<IllegalArgumentException> { FishingOutcome.Caught(0) }
        assertFailsWith<IllegalArgumentException> { FishingOutcome.Caught(-1) }
        assertEquals(3, FishingOutcome.Caught(3).count)
    }

    @Test
    fun aFailureCarriesItsCause() {
        val cause = IllegalStateException("Network unreachable")
        assertSame(cause, FishingOutcome.Failed(cause).cause)
    }
}
