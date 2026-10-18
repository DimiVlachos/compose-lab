package dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val Rust = 0xFFC86440.toInt()

@OptIn(ExperimentalTestApi::class)
class GfxTest {
    private fun Matrix.map(x: Float, y: Float): Offset {
        val m = compose
        val w = m[0, 3] * x + m[1, 3] * y + m[3, 3]
        return Offset(
            (m[0, 0] * x + m[1, 0] * y + m[3, 0]) / w,
            (m[0, 1] * x + m[1, 1] * y + m[3, 1]) / w,
        )
    }

    @Test
    fun polyToPolyTakesEachSourceCornerToItsTarget() {
        val src = floatArrayOf(0f, 0f, 100f, 0f, 100f, 200f, 0f, 200f)
        val dst = floatArrayOf(30f, 12f, 160f, 40f, 150f, 230f, 20f, 190f)
        val matrix = Matrix()

        assertTrue(matrix.setPolyToPoly(src, 0, dst, 0, 4))
        for (i in 0 until 4) {
            val at = matrix.map(src[2 * i], src[2 * i + 1])
            assertTrue(
                abs(at.x - dst[2 * i]) < 0.01f && abs(at.y - dst[2 * i + 1]) < 0.01f,
                "corner $i at $at",
            )
        }
    }

    @Test
    fun polyToPolyWorksFromAnyQuadNotOnlyARectangle() {
        val src = floatArrayOf(10f, 5f, 90f, 15f, 80f, 70f, 5f, 60f)
        val dst = floatArrayOf(0f, 0f, 50f, 0f, 50f, 50f, 0f, 50f)
        val matrix = Matrix()

        assertTrue(matrix.setPolyToPoly(src, 0, dst, 0, 4))
        for (i in 0 until 4) {
            val at = matrix.map(src[2 * i], src[2 * i + 1])
            assertTrue(
                abs(at.x - dst[2 * i]) < 0.01f && abs(at.y - dst[2 * i + 1]) < 0.01f,
                "corner $i at $at",
            )
        }
    }

    @Test
    fun aLightingFilterMultipliesThenAdds() {
        val bitmap = createBitmap(1, 1)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { colorFilter = LightingColorFilter(0x808080, 0x100000) }
        canvas.drawBitmap(
            createBitmap(1, 1).also { Canvas(it).drawColor(Rust) },
            0f,
            0f,
            paint,
        )
        val px = IntArray(1).also { bitmap.getPixels(it, 0, 1, 0, 0, 1, 1) }[0]

        val red = (px shr 16) and 0xFF
        val green = (px shr 8) and 0xFF
        assertTrue(abs(red - (0xC8 * 128 / 255 + 0x10)) <= 2, "red $red")
        assertTrue(abs(green - 0x64 * 128 / 255) <= 2, "green $green")
    }

    @Test
    fun alphaIsTheColoursTopByte() {
        val paint = Paint().apply { color = 0xFF336699.toInt() }
        paint.alpha = 0x40

        assertEquals(0x40336699, paint.color)
        assertEquals(0x40 / 255f, Color(paint.color).alpha, 0.002f)
    }

    @Test
    fun centredTextIsCentredOnX() = runComposeUiTest {
        lateinit var measurer: TextMeasurer
        setContent { measurer = rememberTextMeasurer() }
        runOnIdle {
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface(FontFamily.Serif, measurer)
                    textSize = 40f
                    textAlign = Paint.Align.CENTER
                    color = 0xFF000000.toInt()
                }
            val width = paint.measureText("MMMM")
            val bitmap = createBitmap(400, 80)
            Canvas(bitmap).drawText("MMMM", 200f, 60f, paint)
            val px = IntArray(400 * 80).also { bitmap.getPixels(it, 0, 400, 0, 0, 400, 80) }
            val inked =
                (0 until 400).filter { x ->
                    (0 until 80).any { y -> px[y * 400 + x] ushr 24 > 128 }
                }

            assertTrue(width > 40f)
            assertTrue(
                abs((inked.first() + inked.last()) / 2f - 200f) < 4f,
                "${inked.first()}..${inked.last()}",
            )
        }
    }
}
