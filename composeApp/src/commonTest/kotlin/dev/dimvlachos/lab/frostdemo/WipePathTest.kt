package dev.dimvlachos.lab.frostdemo

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class WipePathTest {
    // Samples at even time steps: bunched up where the finger is slow.
    private val path = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(9f, 0f))

    @Test
    fun theEndsAreTheFirstAndLastSamples() {
        assertEquals(Offset(0f, 0f), pointAt(path, 0f))
        assertEquals(Offset(9f, 0f), pointAt(path, 1f))
    }

    @Test
    fun timeNotDistanceDecidesWhereTheFingerIs() {
        // Halfway through the time is the middle sample, though it is near the start by distance.
        assertEquals(Offset(1f, 0f), pointAt(path, 0.5f))
        assertEquals(Offset(5f, 0f), pointAt(path, 0.75f))
    }

    @Test
    fun aOnePointPathStaysPut() {
        assertEquals(Offset(2f, 2f), pointAt(listOf(Offset(2f, 2f)), 0.6f))
    }
}
