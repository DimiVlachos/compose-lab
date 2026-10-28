package dev.dimvlachos.lab.core.presentation.components.paintroller

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toPixelMap
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.cssColor
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What the roller's painter and the wet sheen draw, read back from their pixels. */
class PaintRollerPainterTest {
    private fun roller() =
        PaintRollerState().apply {
            layout(
                412f,
                915f,
                709.6f,
                List(5) { Rect(16f + it * 77.2f, 736.2f, 16f + it * 77.2f + 71.2f, 836.2f) },
                List(7) { Offset(200f, 300f) },
            )
        }

    @Test
    fun shadeMovesTowardBlackOrWhiteAsThePrototypes() {
        val blue = cssColor("#2b5fc7")
        // shade('#2b5fc7', .45): rgb(138, 167, 224).
        val light = shade(blue, .45f)
        assertEquals(138, (light.red * 255).roundToInt())
        assertEquals(167, (light.green * 255).roundToInt())
        assertEquals(224, (light.blue * 255).roundToInt())
        // A half rounds up, as Math.round does: 43 × .5 = 21.5 → 22.
        assertEquals(22, (shade(blue, -.5f).red * 255).roundToInt())
    }

    @Test
    fun theTraysAndTheRollerAreDrawnWhereTheStateSays() {
        val r = roller()
        val c = Canvas2D.create(412, 915)
        c.drawPaintRoller(r, dark = false, density = 1f)
        val px = c.bitmap.toPixelMap()
        // Each tray's well shows its paint; the roller rests on Blue Ridge, the screen above is
        // clear.
        val t = r.trays[2]
        val well = px[t.cx.toInt() + 12, (t.top + t.th * .8f).toInt()]
        assertTrue(well.green > well.red && well.green > well.blue, "$well")
        assertEquals(0f, px[200, 300].alpha)
        val roll = px[r.rx.toInt(), r.ry.toInt()]
        assertTrue(roll.alpha > .9f && roll.blue > roll.red, "$roll")
    }

    @Test
    fun theStippleIsSparseWhiteSpecksAndTheSameEachTime() {
        val a = Turbulence.fractal(3, 10.5, 20.5)
        assertEquals(a, Turbulence.fractal(3, 10.5, 20.5))
        // Stitched: the tile repeats every 64 px.
        assertEquals(Turbulence.fractal(3, .5, 7.5), Turbulence.fractal(3, 64.5, 7.5), 1e-9)
        val px = stippleTile(1f).toPixelMap()
        var lit = 0
        for (y in 0 until 64) for (x in 0 until 64) {
            val p = px[x, y]
            if (p.alpha > 0f) {
                lit++
                assertEquals(1f, p.red, .01f)
            }
        }
        assertTrue(lit in 200..2400, "$lit specks")
    }

    @Test
    fun theNapIsBakedOnceFromItsSeed() {
        assertEquals(256, RollerNap.width)
        assertEquals(128, RollerNap.height)
        val px = RollerNap.toPixelMap()
        var fibres = 0
        for (y in 0 until 128) for (x in 0 until 256) if (px[x, y].alpha > 0f) fibres++
        assertTrue(fibres > 2000, "$fibres")
    }
}
