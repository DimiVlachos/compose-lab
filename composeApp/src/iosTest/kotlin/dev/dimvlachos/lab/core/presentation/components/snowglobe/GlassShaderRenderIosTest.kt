package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

class GlassShaderRenderIosTest {
    // A 64 px globe of solid red water, seen through the glass.
    private fun render(): Bitmap {
        val look =
            glassLook(
                Offset(32f, 32f),
                30f,
                dark = false,
                GlobeScene.Sunny,
                GlobeScene.Sunny,
                1f,
                0f,
            )
        val filter = checkNotNull(glassImageFilter(look))
        val surface = Surface.makeRasterN32Premul(64, 64)
        val canvas = surface.canvas
        canvas.saveLayer(null, Paint().apply { imageFilter = filter })
        canvas.drawRect(Rect.makeWH(64f, 64f), Paint().apply { color = Color.RED })
        canvas.restore()
        val bitmap = Bitmap()
        bitmap.allocN32Pixels(64, 64)
        surface.readPixels(bitmap, 0, 0)
        return bitmap
    }

    @Test
    fun theMiddleLooksThroughTheWaterAndTheRimCatchesTheLight() {
        val px = render()
        val middle = px.getColor(32, 32)
        assertTrue(Color.getR(middle) > 200, "the middle is still red: ${Color.getR(middle)}")
        assertTrue(Color.getG(middle) < 60, "and not washed out: ${Color.getG(middle)}")
        // Near the rim, on the right where the prototype's rim light sits.
        val rim = px.getColor(32 + 29, 32)
        assertTrue(
            Color.getG(rim) > Color.getG(middle) + 40,
            "the rim is lighter: ${Color.getG(rim)}",
        )
        // Outside the sphere the glass leaves nothing.
        assertEquals(0, Color.getA(px.getColor(1, 1)))
    }
}
