package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.dimvlachos.lab.core.camera.CameraFrame
import dev.dimvlachos.lab.core.camera.MirrorFramePainter
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The person from the camera, standing in front of the bathroom.
class BackdropPainterTest {
    private fun filled(width: Int, height: Int, paint: (Canvas) -> Unit) =
        ImageBitmap(width, height).also { paint(Canvas(it)) }

    private fun rect(canvas: Canvas, x: Int, w: Int, h: Int, color: Color) =
        canvas.drawRect(
            x.toFloat(),
            0f,
            (x + w).toFloat(),
            h.toFloat(),
            Paint().apply { this.color = color },
        )

    // A red person on the left of an upright 2 × 1 frame, the mask keeping only them.
    private fun person(): MirrorFramePainter {
        val image =
            filled(2, 1) {
                rect(it, 0, 1, 1, Color.Red)
                rect(it, 1, 1, 1, Color.Blue)
            }
        val mask = filled(2, 1) { rect(it, 0, 1, 1, Color.Black) }
        return MirrorFramePainter().apply { show(CameraFrame(image, 2, 1, 0, mask = mask)) }
    }

    private fun Painter.render(size: Size, filter: ColorFilter? = null): (Float, Float) -> Color {
        val target = ImageBitmap(size.width.toInt(), size.height.toInt())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(target), size) {
            with(this@render) { draw(size, colorFilter = filter) }
        }
        val pixels = target.toPixelMap()
        return { x, y ->
            pixels[(x * (pixels.width - 1)).toInt(), (y * (pixels.height - 1)).toInt()]
        }
    }

    private fun Color.isNear(other: Color) =
        abs(red - other.red) < 0.1f &&
            abs(green - other.green) < 0.1f &&
            abs(blue - other.blue) < 0.1f

    private val bathroom = filled(4, 4) { rect(it, 0, 4, 4, Color.Green) }

    @Test
    fun thePersonStandsInFrontOfTheBathroom() {
        val at = BackdropPainter(person(), bathroom).render(Size(20f, 10f))
        // Mirrored, the person is on the right; the bathroom shows where they are not.
        assertTrue(at(0.9f, 0.5f).isNear(Color.Red), "the person: ${at(0.9f, 0.5f)}")
        assertTrue(at(0.1f, 0.5f).isNear(Color.Green), "the bathroom: ${at(0.1f, 0.5f)}")
    }

    @Test
    fun itIsAsLargeAsTheCamerasPicture() {
        assertEquals(Size(2f, 1f), BackdropPainter(person(), bathroom).intrinsicSize)
    }

    @Test
    fun theFogsFilterTintsBothThePersonAndTheBathroom() {
        val black = ColorFilter.tint(Color.Black)
        val at = BackdropPainter(person(), bathroom).render(Size(20f, 10f), black)
        assertTrue(at(0.9f, 0.5f).isNear(Color.Black), "the person: ${at(0.9f, 0.5f)}")
        assertTrue(at(0.1f, 0.5f).isNear(Color.Black), "the bathroom: ${at(0.1f, 0.5f)}")
    }

    @Test
    fun withNoMaskTheCamerasWholePictureShowsAndTheBathroomIsNotDrawn() {
        // A frame with a see-through corner, and no mask: nothing of the bathroom behind it.
        val image = filled(2, 1) { rect(it, 0, 1, 1, Color.Red) }
        val whole = MirrorFramePainter().apply { show(CameraFrame(image, 2, 1, 0)) }
        val at = BackdropPainter(whole, bathroom).render(Size(20f, 10f))
        assertTrue(at(0.9f, 0.5f).isNear(Color.Red), "the camera: ${at(0.9f, 0.5f)}")
        assertTrue(at(0.1f, 0.5f).alpha < 0.1f, "no bathroom: ${at(0.1f, 0.5f)}")
    }
}
