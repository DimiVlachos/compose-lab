package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FigurineTest {
    @Test
    fun theLanternCapIsTheHighestRestingPlace() {
        val cap = Figurine.restHeight(-0.30f)
        for (i in -90..90) {
            val x = i / 100f
            assertTrue(Figurine.restHeight(x) >= cap - 1e-6f, "x=$x is above the lantern cap")
        }
    }

    @Test
    fun roofRidgesArePeaks() {
        // Each ridge is higher (smaller y) than the eaves on either side of it.
        assertTrue(Figurine.restHeight(0.19f) < Figurine.restHeight(0.05f))
        assertTrue(Figurine.restHeight(0.19f) < Figurine.restHeight(0.33f))
        assertTrue(Figurine.restHeight(0.53f) < Figurine.restHeight(0.43f))
        assertTrue(Figurine.restHeight(0.53f) < Figurine.restHeight(0.63f))
    }

    @Test
    fun theGalleryIsALedgeAroundTheLantern() {
        assertEquals(Figurine.Lighthouse.galleryTop, Figurine.restHeight(-0.40f), 1e-6f)
        assertEquals(Figurine.Lighthouse.galleryTop, Figurine.restHeight(-0.20f), 1e-6f)
    }

    @Test
    fun awayFromTheBuildingsFlakesRestOnTheDune() {
        assertEquals(Figurine.duneTop(-0.7f), Figurine.restHeight(-0.7f), 1e-6f)
        assertEquals(Figurine.duneTop(0.8f), Figurine.restHeight(0.8f), 1e-6f)
    }
}
