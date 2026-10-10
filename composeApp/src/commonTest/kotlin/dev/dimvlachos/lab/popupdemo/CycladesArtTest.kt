package dev.dimvlachos.lab.popupdemo

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import dev.dimvlachos.lab.core.presentation.components.popup.PieceMotion
import dev.dimvlachos.lab.core.presentation.components.popup.PopUpArt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CycladesArtTest {
    private val density = Density(2f)

    private fun raster(painter: Painter): ImageBitmap {
        val size = painter.intrinsicSize
        return PopUpArt.raster(
            painter,
            (size.width * 2).toInt(),
            (size.height * 2).toInt(),
            density,
        )
    }

    @Test
    fun theBookHasThreeSpreadsWithTabsOnTheLastTwo() = runComposeUiTest {
        lateinit var book: CycladesBook
        setContent { book = rememberCycladesBook() }
        assertEquals(3, book.spreads.size)
        assertNull(book.spreads[0].tab)
        assertNotNull(book.spreads[1].tab)
        assertNotNull(book.spreads[2].tab)
    }

    @Test
    fun theBoatRidesItsTabAndTheSailsSpin() = runComposeUiTest {
        lateinit var book: CycladesBook
        setContent { book = rememberCycladesBook() }
        assertTrue(book.spreads[1].pieces.any { it.motion is PieceMotion.RidesTab })
        assertTrue(book.spreads[2].pieces.any { it.motion is PieceMotion.Spins })
    }

    @Test
    fun everyPagePieceAndTheCoverPaints() = runComposeUiTest {
        lateinit var book: CycladesBook
        setContent { book = rememberCycladesBook() }
        val painters =
            listOf(book.cover) +
                book.spreads.flatMap { listOf(it.near, it.far) + it.pieces.map { p -> p.art } }
        for (painter in painters) {
            val pixels = raster(painter).toPixelMap()
            val inked =
                (0 until pixels.width step 4).any { x ->
                    (0 until pixels.height step 4).any { y -> pixels[x, y].alpha > 0.5f }
                }
            assertTrue(inked, "$painter painted nothing")
        }
    }

    @Test
    fun theChapelsDomeIsBlue() = runComposeUiTest {
        lateinit var book: CycladesBook
        setContent { book = rememberCycladesBook() }
        val chapel = book.spreads[0].pieces.first { it.height == 150f }
        val pixels = raster(chapel.art).toPixelMap()
        // The dome's middle: 74 across, 14 above the drum at 150 - 84.
        val dome = pixels[74 * 2, (150 - 84 - 12) * 2]
        assertTrue(dome.blue > dome.red + 0.15f, "$dome")
    }
}
