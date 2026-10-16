package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.test.Test
import kotlin.test.assertEquals

class GlobeLayoutTest {
    // The prototype's layout() at a 360 px wide screen, with the header ending at 150 and the day
    // cards starting at 646 (46 S above which the globe's band ends).
    @Test
    fun itPlacesTheGlobeAsThePrototypeDoes() {
        val layout = globeLayout(width = 360f, headerBottom = 146f, daysTop = 646f)
        // top = 146 + 4, bottom = 646 - 46, avail = 450; R = min(0.4 W, 450 / 2.38) = 144.
        assertEquals(1f, layout.scale, 1e-4f)
        assertEquals(144f, layout.radius, 1e-3f)
        assertEquals(180f, layout.cx, 1e-3f)
        assertEquals(150f + (450f - 2.36f * 144f) / 2f + 1.04f * 144f, layout.cy, 1e-3f)
        assertEquals(layout.cy + 1.31f * 144f + 12f, layout.hintTop, 1e-3f)
    }

    @Test
    fun aShortBandShrinksTheGlobeToFit() {
        val layout = globeLayout(width = 360f, headerBottom = 146f, daysTop = 446f)
        // avail = 250, so R = 250 / 2.38, and the globe all but fills the band.
        assertEquals(250f / 2.38f, layout.radius, 1e-3f)
        val r = layout.radius
        assertEquals(150f + (250f - 2.36f * r) / 2f + 1.04f * r, layout.cy, 1e-3f)
    }

    @Test
    fun theRadiusNeverFallsBelowForty() {
        assertEquals(
            40f,
            globeLayout(width = 360f, headerBottom = 146f, daysTop = 210f).radius,
            1e-3f,
        )
    }

    @Test
    fun aWiderScreenScalesTheSpacing() {
        val layout = globeLayout(width = 720f, headerBottom = 292f, daysTop = 1292f)
        assertEquals(2f, layout.scale, 1e-4f)
        assertEquals(288f, layout.radius, 1e-3f)
    }
}
