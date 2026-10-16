package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Canvas2DTest {
    private fun near(expected: Color, actual: Color, tolerance: Float = 0.03f) {
        val ok =
            abs(expected.red - actual.red) < tolerance &&
                abs(expected.green - actual.green) < tolerance &&
                abs(expected.blue - actual.blue) < tolerance &&
                abs(expected.alpha - actual.alpha) < tolerance
        assertTrue(ok, "expected $expected, got $actual")
    }

    @Test
    fun mulberryMatchesThePrototypesGenerator() {
        val seven = mulberry(7)
        assertEquals(0.011704753153026, seven(), 1e-12)
        assertEquals(0.061958257574588, seven(), 1e-12)
        assertEquals(0.976907632779330, seven(), 1e-12)
        assertEquals(0.699028705712408, seven(), 1e-12)
        val seventeen = mulberry(17)
        assertEquals(0.677150296047330, seventeen(), 1e-12)
        assertEquals(0.192656921222806, seventeen(), 1e-12)
    }

    @Test
    fun cssColoursParseAsTheBrowserReadsThem() {
        assertEquals(Color(red = 0xef, green = 0xe1, blue = 0xc8), cssColor("#efe1c8"))
        assertEquals(Color(red = 0xff, green = 0xff, blue = 0xff), cssColor("#fff"))
        assertEquals(
            Color(red = 20, green = 24, blue = 58, alpha = (0.34f * 255).toInt()),
            cssColor("rgba(20,24,58,.34)"),
        )
        assertEquals(Color(red = 255, green = 238, blue = 210), cssColor("rgb(255, 238, 210)"))
    }

    @Test
    fun aFilledRectLandsUnderTheTransform() {
        val c = Canvas2D.create(8, 8)
        c.setTransform(4f, 0f, 0f, 4f, 4f, 4f) // [-1, 1] across the bitmap, as the prototype paints
        c.fillStyle = "#ff0000"
        c.fillRect(0f, 0f, 1f, 1f) // the bottom-right quarter
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[6, 6])
        near(Color.Transparent, px[1, 1])
    }

    @Test
    fun anArcFillsACircle() {
        val c = Canvas2D.create(20, 20)
        c.beginPath()
        c.arc(10f, 10f, 8f, 0f, TAU)
        c.fillStyle = "rgba(0,0,255,1)"
        c.fill()
        val px = c.bitmap.toPixelMap()
        near(Color.Blue, px[10, 10])
        near(Color.Transparent, px[1, 1])
    }

    @Test
    fun anArcAfterLinesContinuesTheSameShape() {
        // A box's top edge, then the bottom half of a circle: one D shape, as the browser fills it.
        val c = Canvas2D.create(20, 20)
        c.beginPath()
        c.moveTo(2f, 0f)
        c.lineTo(18f, 0f)
        c.arc(10f, 10f, 8f, 0f, PI.toFloat())
        c.closePath()
        c.fillStyle = "#0000ff"
        c.fill()
        val px = c.bitmap.toPixelMap()
        // Below the diagonal a separate arc contour would leave the box's lower left empty.
        near(Color.Blue, px[5, 8])
        near(Color.Blue, px[10, 15])
    }

    @Test
    fun aClipHoldsUntilRestore() {
        val c = Canvas2D.create(10, 10)
        c.save()
        c.beginPath()
        c.rect(0f, 0f, 5f, 10f)
        c.clip()
        c.fillStyle = "#00ff00"
        c.fillRect(0f, 0f, 10f, 10f)
        c.restore()
        c.fillStyle = "#0000ff"
        c.fillRect(8f, 0f, 2f, 10f)
        val px = c.bitmap.toPixelMap()
        near(Color.Green, px[2, 5])
        near(Color.Blue, px[9, 5])
        near(Color.Transparent, px[6, 5])
    }

    @Test
    fun aLinearGradientRunsBetweenItsPoints() {
        val c = Canvas2D.create(10, 2)
        val g = c.createLinearGradient(0f, 0f, 10f, 0f)
        g.addColorStop(0f, "#000000")
        g.addColorStop(1f, "#ffffff")
        c.fillStyle = g
        c.fillRect(0f, 0f, 10f, 2f)
        val px = c.bitmap.toPixelMap()
        assertTrue(px[1, 1].red < 0.2f && px[8, 1].red > 0.8f)
    }

    @Test
    fun globalAlphaAndLighterBehaveAsInTheBrowser() {
        val c = Canvas2D.create(4, 4)
        c.fillStyle = "#400000"
        c.fillRect(0f, 0f, 4f, 4f)
        c.globalCompositeOperation = "lighter"
        c.globalAlpha = 0.5f
        c.fillStyle = "#800000"
        c.fillRect(0f, 0f, 4f, 4f)
        val red = c.bitmap.toPixelMap()[2, 2].red
        // 0x40 + 0x80 * 0.5 = 0x80.
        assertEquals(0x80 / 255f, red, 0.03f)
    }

    @Test
    fun destinationOutErases() {
        val c = Canvas2D.create(4, 4)
        c.fillStyle = "#ffffff"
        c.fillRect(0f, 0f, 4f, 4f)
        c.globalCompositeOperation = "destination-out"
        c.fillRect(0f, 0f, 2f, 4f)
        val px = c.bitmap.toPixelMap()
        near(Color.Transparent, px[1, 2])
        near(Color.White, px[3, 2])
    }
}
