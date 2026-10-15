package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlumeBatchesTest {
    private val batch = PlumeBatches.of(colour = 1, band = 3)

    private fun PlumeBatches.fillWith(points: Int) {
        clear(2)
        repeat(points) { add(batch, it.toFloat(), 1f) }
        seal()
    }

    // Every point past the batch's count lies off the screen, and those before it are its own.
    private fun PlumeBatches.assertSealed(points: Int) {
        val array = pointsOf(batch)
        assertEquals(points, countOf(batch))
        for (i in 0 until points) assertEquals(i.toFloat(), array[i * 2])
        for (k in points * 2 until array.size) {
            assertEquals(PlumeBatches.OffScreen, array[k], "float $k of ${array.size}")
        }
    }

    @Test
    fun whatIsPastTheCountLiesOffTheScreenAsTheBatchGrowsAndEmpties() {
        val batches = PlumeBatches()
        batches.fillWith(500)
        batches.assertSealed(500)
        batches.fillWith(120)
        batches.assertSealed(120)
        batches.fillWith(0)
        batches.assertSealed(0)
        batches.fillWith(300)
        batches.assertSealed(300)
    }

    @Test
    fun aBatchFarTooBigGivesItsRoomBack() {
        val batches = PlumeBatches()
        batches.fillWith(4_000)
        val big = batches.pointsOf(batch).size
        batches.fillWith(100)
        val small = batches.pointsOf(batch).size
        assertTrue(small < big / 4, "kept room for $small floats")
        assertTrue(small >= 200)
        batches.assertSealed(100)
    }

    @Test
    fun agesFallInBandsThatCoverALife() {
        assertEquals(0, PlumeBatches.bandOf(0f))
        assertEquals(PlumeBatches.Bands - 1, PlumeBatches.bandOf(0.999f))
        var last = 0
        for (i in 0..100) {
            val band = PlumeBatches.bandOf(i / 100f)
            assertTrue(band >= last)
            last = band
        }
    }
}
