package dev.dimvlachos.lab.core.presentation.components.jackolantern

import androidx.compose.ui.graphics.toPixelMap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.cssColor
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What the lantern's painter draws, read back from its pixels. */
class JackOLanternPainterTest {
    private fun drawn(lit: Boolean) =
        JackOLanternState(lit).run {
            layout(200f, 200f, 100f, 97f)
            val c = Canvas2D.create(200, 200)
            c.drawLantern(this, LanternPaints())
            c.bitmap.toPixelMap()
        }

    @Test
    fun mixRgbRoundsEachChannelAsThePrototypes() {
        // mix('#ffb24a', '#b05a1c', .5): rgb(216, 134, 51).
        val m = mixRgb(cssColor("#ffb24a"), cssColor("#b05a1c"), .5f)
        assertEquals(216, (m.red * 255).roundToInt())
        assertEquals(134, (m.green * 255).roundToInt())
        assertEquals(51, (m.blue * 255).roundToInt())
    }

    @Test
    fun byDayThePumpkinIsOrangeAndItsFaceDark() {
        val px = drawn(lit = false)
        // A cheek, between the eye and the grin's corner.
        val cheek = px[100 - 18, 100 + 4]
        assertTrue(
            cheek.alpha > .95f && cheek.red > cheek.green && cheek.green > cheek.blue,
            "$cheek",
        )
        // Inside the nose: the dark hollow.
        val nose = px[100, 101]
        assertTrue(nose.red < .35f, "$nose")
        // Clear of it, nothing.
        assertEquals(0f, px[10, 10].alpha)
    }

    @Test
    fun litTheFaceGlowsWithTheFlame() {
        val px = drawn(lit = true)
        val grin = px[100, 110]
        assertTrue(grin.red > .9f && grin.green > .6f, "$grin")
    }

    @Test
    fun theNightReachesPastTheFarthestCornerWithItsSoftEdge() {
        assertEquals(0f, nightRadius(0f, 360f, 40f, 412f, 800f))
        val full = nightRadius(1f, 360f, 40f, 412f, 800f)
        // From the top right, the bottom left corner is farthest: hypot(360, 760) + 70 + 20.
        assertEquals(931f, full, 1f)
    }
}
