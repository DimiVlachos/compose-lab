package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlassShaderSourceTest {
    @Test
    fun itDeclaresEveryUniformThePrototypeSets() {
        for (name in
            listOf(
                "uniform shader content;",
                "uniform float2 center;",
                "uniform float radius;",
                "uniform float uPx;",
                "uniform float uWinW;",
                "uniform float uWinH;",
                "uniform float uMull;",
                "uniform float3 uL;",
                "uniform float3 uEnvTop;",
                "uniform float3 uEnvBot;",
                "uniform float3 uWin;",
                "uniform float3 uCaus;",
                "uniform float3 uRim;",
                "uniform float2 uBub;",
                "half4 main(float2 fragCoord)",
            )) {
            assertTrue(name in GlassShaderSource, "missing $name")
        }
    }

    @Test
    fun theBubbleRidesAtTheTopAndLeansAgainstTheTilt() {
        val still = glassBubble(0f)
        assertEquals(0f, still.x, 1e-6f)
        assertEquals(-.9f, still.y, 1e-6f)
        val leaning = glassBubble(.2f)
        assertTrue(leaning.x < 0f, "a lean to the right sends the bubble left: $leaning")
        assertEquals(.9f, hypot(leaning.x, leaning.y), 1e-5f)
    }

    @Test
    fun theRoomLightsTheGlassAsThePrototypeDoes() {
        val sunnyDark =
            glassLook(
                Offset(100f, 100f),
                50f,
                dark = true,
                GlobeScene.Sunny,
                GlobeScene.Sunny,
                1f,
                0f,
            )
        // #261d26 × 1.4 and #18131a × 1.1, over 255.
        assertEquals(0x26 / 255f * 1.4f, sunnyDark.envTop[0], 1e-4f)
        assertEquals(0x1a / 255f * 1.1f, sunnyDark.envBot[2], 1e-4f)
        assertEquals(.15f, sunnyDark.winW)
        assertEquals(0f, sunnyDark.mull)
        val showerLight =
            glassLook(
                Offset(100f, 100f),
                50f,
                dark = false,
                GlobeScene.Shower,
                GlobeScene.Shower,
                1f,
                0f,
            )
        assertEquals(.34f * .55f, showerLight.caustics[0], 1e-5f)
        assertEquals(1f, showerLight.mull)
        assertEquals(1f / 50f, showerLight.px)
    }
}
