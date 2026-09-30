package dev.dimvlachos.lab.core.camera

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The sensor's frame is sideways and unmirrored; the painter shows it upright, as a mirror would.
class MirrorFramePainterTest {
    /** Red on the left, blue on the right, then [padding] green columns the frame does not use. */
    private fun redBlueFrame(rotation: Int, padding: Int = 0): CameraFrame {
        val image = ImageBitmap(2 + padding, 1)
        val canvas = Canvas(image)
        fun column(x: Int, color: Color) =
            canvas.drawRect(x.toFloat(), 0f, x + 1f, 1f, Paint().apply { this.color = color })
        column(0, Color.Red)
        column(1, Color.Blue)
        for (x in 2 until 2 + padding) column(x, Color.Green)
        return CameraFrame(image, width = 2, height = 1, rotationDegrees = rotation)
    }

    private fun MirrorFramePainter.render(size: Size): (Float, Float) -> Color {
        val target = ImageBitmap(size.width.toInt(), size.height.toInt())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(target), size) {
            with(this@render) { draw(size) }
        }
        val pixels = target.toPixelMap()
        return { x, y ->
            pixels[(x * (pixels.width - 1)).toInt(), (y * (pixels.height - 1)).toInt()]
        }
    }

    private fun Color.isNear(other: Color) =
        kotlin.math.abs(red - other.red) < 0.1f &&
            kotlin.math.abs(green - other.green) < 0.1f &&
            kotlin.math.abs(blue - other.blue) < 0.1f

    @Test
    fun uprightTheFrameIsMirrored() {
        val painter = MirrorFramePainter().apply { show(redBlueFrame(rotation = 0)) }
        val at = painter.render(Size(20f, 10f))
        assertTrue(at(0.1f, 0.5f).isNear(Color.Blue), "left: ${at(0.1f, 0.5f)}")
        assertTrue(at(0.9f, 0.5f).isNear(Color.Red), "right: ${at(0.9f, 0.5f)}")
    }

    @Test
    fun aQuarterTurnStandsTheFrameUp() {
        val painter = MirrorFramePainter().apply { show(redBlueFrame(rotation = 90)) }
        val at = painter.render(Size(10f, 20f))
        assertTrue(at(0.5f, 0.1f).isNear(Color.Red), "top: ${at(0.5f, 0.1f)}")
        assertTrue(at(0.5f, 0.9f).isNear(Color.Blue), "bottom: ${at(0.5f, 0.9f)}")
    }

    @Test
    fun threeQuarterTurnsStandTheFrameUpTheOtherWay() {
        val painter = MirrorFramePainter().apply { show(redBlueFrame(rotation = 270)) }
        val at = painter.render(Size(10f, 20f))
        assertTrue(at(0.5f, 0.1f).isNear(Color.Blue), "top: ${at(0.5f, 0.1f)}")
        assertTrue(at(0.5f, 0.9f).isNear(Color.Red), "bottom: ${at(0.5f, 0.9f)}")
    }

    @Test
    fun theRowPaddingNeverShows() {
        val painter = MirrorFramePainter().apply { show(redBlueFrame(rotation = 0, padding = 2)) }
        val at = painter.render(Size(20f, 10f))
        for (x in listOf(0.05f, 0.3f, 0.7f, 0.95f)) {
            assertFalse(at(x, 0.5f).isNear(Color.Green), "padding at $x")
        }
    }

    @Test
    fun theSizeIsTheUprightFramesAndIgnoresThePadding() {
        val painter = MirrorFramePainter()
        assertEquals(Size.Unspecified, painter.intrinsicSize)
        painter.show(redBlueFrame(rotation = 0, padding = 2))
        assertEquals(Size(2f, 1f), painter.intrinsicSize)
        painter.show(redBlueFrame(rotation = 90))
        assertEquals(Size(1f, 2f), painter.intrinsicSize)
    }

    @Test
    fun itShowsOnlyWhileItHasAFrame() {
        val painter = MirrorFramePainter()
        assertFalse(painter.hasFrame)
        painter.show(redBlueFrame(rotation = 0))
        assertTrue(painter.hasFrame)
        painter.show(null)
        assertFalse(painter.hasFrame)
        // Nothing to draw, and nothing drawn.
        val at = painter.render(Size(4f, 2f))
        assertEquals(0f, at(0.5f, 0.5f).alpha)
    }

    @Test
    fun aGainBrightensADarkFrame() {
        val image = ImageBitmap(2, 1)
        Canvas(image).drawRect(0f, 0f, 2f, 1f, Paint().apply { color = Color(0.1f, 0.1f, 0.1f) })
        val painter = MirrorFramePainter()
        painter.show(CameraFrame(image, width = 2, height = 1, rotationDegrees = 0, gain = 4f))
        val at = painter.render(Size(20f, 10f))
        val middle = at(0.5f, 0.5f)
        assertTrue(middle.red in 0.35f..0.45f, "four times brighter: $middle")
    }
}
