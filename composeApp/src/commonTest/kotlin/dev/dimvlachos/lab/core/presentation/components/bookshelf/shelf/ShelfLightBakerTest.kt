package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfBookSpec
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.layoutShelf
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.getPixels
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShelfLightBakerTest {
    private val layout =
        layoutShelf(
            412f,
            640f,
            listOf(
                (1..6).map { ShelfBookSpec(it, 30f, 210f, 140f) },
                (7..11).map { ShelfBookSpec(it, 28f, 220f, 140f) },
            ),
        )

    private fun Bitmap.average(shift: Int): Float {
        val px = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
        return px.map { (it shr shift) and 0xFF }.average().toFloat()
    }

    @Test
    fun theLightIsBakedAQuarterOfTheSceneAcross() {
        val maps = bakeShelfLight(layout, ShelfLighting.Day)

        assertEquals(ceil(412f / LIGHT_CELL).toInt() + 1, maps.light.width)
        assertEquals(ceil(640f / LIGHT_CELL).toInt() + 1, maps.light.height)
        assertEquals(maps.light.width, maps.leaves.width)
    }

    @Test
    fun duskIsDarkerWithAWarmerGlowThanDay() {
        val day = bakeShelfLight(layout, ShelfLighting.Day)
        val dusk = bakeShelfLight(layout, ShelfLighting.Dusk)

        assertTrue(dusk.light.average(8) < day.light.average(8))
        // The low sun's warmth is added on in the glow map, not multiplied in the light.
        assertTrue(dusk.glow.average(16) > day.glow.average(16), "glow red")
    }

    @Test
    fun theLeavesSwayGently() {
        val sways = (0 until 200).map { leafSway(it * 50L) }

        assertTrue(sways.distinct().size > 100)
        assertTrue(sways.all { abs(it) <= 2.2f })
    }

    @Test
    fun aBookTakenOutLeavesTheRestOfTheLightAsItWas() {
        val full = bakeShelfLight(layout, ShelfLighting.Day)
        val lifted = bakeShadowLift(layout, ShelfLighting.Day, full, bookId = 3)

        assertEquals(full.light.width, lifted.maps.light.width)
        assertEquals(full.light.height, lifted.restore.height)
        assertTrue(lifted.maps.leaves === full.leaves)
    }
}
