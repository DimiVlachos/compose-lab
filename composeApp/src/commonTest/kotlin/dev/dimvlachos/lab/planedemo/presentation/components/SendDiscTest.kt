package dev.dimvlachos.lab.planedemo.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendDiscTest {
    // A 26 dp dart on a 38 dp disc, in a field 24 dp from the button's middle to its edge.
    private fun at(scale: Float, room: Float = 24f) =
        sendDiscSizes(scale, dart = 13f, disc = 19f, room = room, inset = 3f)

    @Test
    fun theDartGrowsInsideTheDiscWhileTheDiscStaysAsItIs() {
        assertEquals(SendDiscSizes(disc = 19f, dart = 13f), at(1f))
        val half = at(1.3f)
        assertEquals(19f, half.disc)
        assertEquals(16.9f, half.dart, 1e-4f)
    }

    @Test
    fun onceTheDartFillsTheDiscTheyGrowTogether() {
        val filled = at(1.55f)
        assertEquals(20.15f, filled.dart, 1e-4f)
        assertEquals(filled.dart, filled.disc)
    }

    @Test
    fun bothStopAtTheFieldsEdge() {
        for (scale in listOf(1.7f, 2f, 3f)) {
            val big = at(scale)
            assertEquals(21f, big.disc, 1e-4f, "at $scale")
            assertEquals(21f, big.dart, 1e-4f, "at $scale")
        }
    }

    @Test
    fun inAFieldTooShortForTheDiscItStillKeepsInside() {
        val tight = at(1f, room = 18f)
        assertTrue(tight.disc <= 15f && tight.dart <= 15f, "$tight")
    }

    @Test
    fun aButtonScaledToNothingShowsNothing() {
        assertEquals(0f, at(0f).dart)
    }
}
