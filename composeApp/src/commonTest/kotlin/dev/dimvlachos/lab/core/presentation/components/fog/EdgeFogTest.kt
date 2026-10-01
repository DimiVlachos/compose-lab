package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// How clear each part of "clear" glass is: 1 wiped clean, 0 fogged, with fog left round the edges.
class EdgeFogTest {
    private val mask = edgeFogMask(seed = 5).toPixelMap()

    private fun clearAt(x: Int, y: Int) = mask[x, y].alpha

    // How far in from an edge the fog reaches, in pixels, before the glass is mostly clear.
    private fun depthFromBottom(x: Int) =
        (0 until mask.height).first { clearAt(x, mask.height - 1 - it) > 0.5f }

    private fun depthFromTop(x: Int) = (0 until mask.height).first { clearAt(x, it) > 0.5f }

    private fun depthFromLeft(y: Int) = (0 until mask.width).first { clearAt(it, y) > 0.5f }

    @Test
    fun theMiddleOfTheGlassIsClear() {
        assertTrue(clearAt(mask.width / 2, mask.height / 2) > 0.95f)
    }

    @Test
    fun theCornersStayFogged() {
        val w = mask.width - 1
        val h = mask.height - 1
        listOf(0 to 0, w to 0, 0 to h, w to h).forEach { (x, y) ->
            assertTrue(clearAt(x, y) < 0.05f, "corner $x,$y: ${clearAt(x, y)}")
        }
    }

    @Test
    fun theFogCollectsDeepestAlongTheBottom() {
        val columns = (mask.width / 4..mask.width * 3 / 4)
        val bottom = columns.map { depthFromBottom(it) }.average()
        val top = columns.map { depthFromTop(it) }.average()
        assertTrue(bottom > top * 1.5, "bottom $bottom, top $top")
    }

    @Test
    fun theBandIsRaggedNotARuledFrame() {
        val depths = (mask.height / 5..mask.height * 4 / 5).map { depthFromLeft(it) }
        assertTrue(depths.max() - depths.min() >= 3, "left edge depths $depths")
    }

    @Test
    fun theSameGlassHasTheSameEdgesEveryTime() {
        val again = edgeFogMask(seed = 5).toPixelMap()
        assertEquals(mask[10, 100], again[10, 100])
        assertEquals(mask[mask.width / 2, mask.height - 4], again[mask.width / 2, mask.height - 4])
    }
}
