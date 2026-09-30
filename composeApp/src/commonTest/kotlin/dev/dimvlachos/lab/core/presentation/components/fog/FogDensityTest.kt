package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FogDensityTest {
    private fun alphas(seed: Int): List<Float> {
        val pixels = fogDensityMap(seed = seed).toPixelMap()
        return (0 until pixels.height).flatMap { y ->
            (0 until pixels.width).map { x -> pixels[x, y].alpha }
        }
    }

    @Test
    fun theFogIsThickerInSomePlacesThanOthers() {
        val alphas = alphas(seed = 11)
        assertTrue(alphas.max() - alphas.min() > 0.2f, "spread ${alphas.min()}..${alphas.max()}")
    }

    @Test
    fun theSameGlassFogsTheSameWayEveryTime() {
        assertEquals(alphas(seed = 11), alphas(seed = 11))
    }
}
