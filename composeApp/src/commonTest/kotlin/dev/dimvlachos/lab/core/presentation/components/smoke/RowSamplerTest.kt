package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RowSamplerTest {
    private val card = 0xFFFFFFFF.toInt()
    private val text = 0xFF1D2126.toInt()
    private val width = 300
    private val height = 60

    // A white card with a dark block of "text" across x 20 to 120, y 20 to 40.
    private fun row(): IntArray =
        IntArray(width * height) { i ->
            val x = i % width
            val y = i / width
            if (x in 20 until 120 && y in 20 until 40) text else card
        }

    @Test
    fun inkKeepsItsColourAndItsPlace() {
        val particles = RowSampler.sample(row(), width, height, budget = 2_000, seed = 7)
        assertEquals(card, particles.palette[0])
        var ink = 0
        for (i in 0 until particles.count) {
            if (!particles.isInk(i)) continue
            ink++
            assertEquals(text, particles.palette[particles.colour[i].toInt()])
            assertTrue(particles.x[i] in 20f..120f && particles.y[i] in 20f..40f)
        }
        assertTrue(ink > 1_000, "only $ink ink particles")
    }

    @Test
    fun theBudgetHoldsAndColumnsFollowX() {
        val particles = RowSampler.sample(row(), width, height, budget = 1_200, seed = 7)
        assertTrue(particles.count in 900..1_400, "${particles.count} particles")
        for (i in 0 until particles.count) {
            // Its pixel's column: a particle lies anywhere within its pixel.
            val column = particles.x[i].toInt() * SmokeDimens.Columns / width
            assertEquals(column.coerceAtMost(SmokeDimens.Columns - 1), particles.column[i].toInt())
        }
    }

    @Test
    fun theSameSeedCutsTheSameParticles() {
        val a = RowSampler.sample(row(), width, height, budget = 800, seed = 3)
        val b = RowSampler.sample(row(), width, height, budget = 800, seed = 3)
        assertEquals(a.count, b.count)
        assertTrue(a.x.contentEquals(b.x) && a.seed.contentEquals(b.seed))
    }

    @Test
    fun clearPixelsAreSkipped() {
        val pixels = row()
        for (i in 0 until width * 10) pixels[i] = 0
        val particles = RowSampler.sample(pixels, width, height, budget = 2_000, seed = 1)
        for (i in 0 until particles.count) assertTrue(particles.y[i] >= 10f)
    }
}
