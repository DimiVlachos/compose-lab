package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertTrue

class PopUpPainterTest {
    private val piece =
        PopUpPiece(
            ColorPainter(Color.Blue),
            PopUpSide.Near,
            fromGutter = 0f,
            x = -50f,
            width = 100f,
            height = 100f,
        )
    private val spreads =
        List(3) { PopUpSpread(ColorPainter(Color.White), ColorPainter(Color.Gray), listOf(piece)) }

    @Test
    fun artIsPaintedAtAboutTheSizeItIsSeen() {
        // A phone a thousand pixels wide sees about 2.9 px per book unit.
        val camera = BookCamera(1080f, 1300f)
        val art =
            PopUpPainter()
                .prepare(ColorPainter(Color.Red), spreads, camera, Density(3f), Color.White)
        assertTrue(
            art.quality <= camera.pxPerUnit * 1.25f,
            "${art.quality} for ${camera.pxPerUnit}",
        )
    }

    @Test
    fun aLargeBookCapsItsArt() {
        val camera = BookCamera(2400f, 2900f)
        val art =
            PopUpPainter()
                .prepare(ColorPainter(Color.Red), spreads, camera, Density(2f), Color.White)
        assertTrue(art.quality <= 3f, "${art.quality}")
        // A leaf is the largest bitmap: at most about 950 by 600.
        assertTrue(art.fronts.all { it.width * it.height <= 960 * 600 })
    }
}
