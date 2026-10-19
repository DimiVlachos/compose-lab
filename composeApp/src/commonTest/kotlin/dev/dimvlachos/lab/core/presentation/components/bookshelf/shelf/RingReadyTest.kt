package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RingReadyTest {
    // Each book's id is its place on the ring, so the windows read plainly.
    private val ids = (0 until 20).toList()

    @Test
    fun theRingWaitsForItsLightAndEveryNearbySpine() {
        val near = (5..15).toSet()

        assertFalse(ringReady(ids, centre = 10, hasLight = false, baked = ids.toSet()))
        assertFalse(ringReady(ids, centre = 10, hasLight = true, baked = near - 15))
        assertTrue(ringReady(ids, centre = 10, hasLight = true, baked = near))
    }

    @Test
    fun aRingNearItsEndNeedsOnlyTheSpinesItHas() {
        assertTrue(ringReady(ids, centre = 0, hasLight = true, baked = (0..5).toSet()))
    }
}
