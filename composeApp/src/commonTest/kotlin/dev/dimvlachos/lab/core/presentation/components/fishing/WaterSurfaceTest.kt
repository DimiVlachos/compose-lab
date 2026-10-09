package dev.dimvlachos.lab.core.presentation.components.fishing

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaterSurfaceTest {
    private fun surface() = WaterSurface(columns = 48, width = 360f)

    private fun WaterSurface.run(seconds: Float) {
        repeat((seconds / FishingDimens.StepSeconds).toInt()) { step(FishingDimens.StepSeconds) }
    }

    @Test
    fun calmWaterIsFlatAndStill() {
        val water = surface()
        assertTrue(water.still)
        assertEquals(0f, water.heightAt(180f))
    }

    @Test
    fun twoSurfacesGivenTheSameSplashStayIdentical() {
        val a = surface()
        val b = surface()
        a.disturb(x = 120f, push = 300f, spread = 24f)
        b.disturb(x = 120f, push = 300f, spread = 24f)
        a.run(1.5f)
        b.run(1.5f)
        assertContentEquals(a.heights, b.heights)
    }

    @Test
    fun aSplashSpreadsBothWaysBeforeItReachesTheFarEnds() {
        val water = surface()
        water.disturb(x = 180f, push = 300f, spread = 16f)
        assertFalse(water.still)
        water.run(0.08f)
        val left = abs(water.heightAt(140f))
        val right = abs(water.heightAt(220f))
        assertTrue(left > 0.01f, "nothing reached the left: $left")
        assertTrue(right > 0.01f, "nothing reached the right: $right")
        assertEquals(left, right, 1e-3f, "a splash in the middle spreads evenly")
        assertTrue(abs(water.heightAt(0f)) < left, "the far end moved first")
    }

    @Test
    fun aSplashSettlesFlatAndStill() {
        val water = surface()
        water.disturb(x = 90f, push = 400f, spread = 24f)
        water.run(6f)
        assertTrue(water.still)
        assertTrue(water.heights.all { abs(it) < 0.05f }, "still rippling")
    }

    @Test
    fun heightAtInterpolatesBetweenColumns() {
        val water = surface()
        water.heights[10] = 2f
        water.heights[11] = 4f
        val gap = 360f / (48 - 1)
        assertEquals(3f, water.heightAt(10.5f * gap), 1e-4f)
    }

    @Test
    fun aSplashOffTheSurfaceDisturbsItsEdge() {
        val water = surface()
        water.disturb(x = -50f, push = 300f, spread = 16f)
        water.run(0.05f)
        assertTrue(abs(water.heightAt(0f)) > 0.01f)
        val beyond = surface()
        beyond.disturb(x = 9_000f, push = 300f, spread = 16f)
        beyond.run(0.05f)
        assertTrue(abs(beyond.heightAt(360f)) > 0.01f)
    }

    @Test
    fun calmingStopsEveryRipple() {
        val water = surface()
        water.disturb(x = 180f, push = 300f, spread = 16f)
        water.run(0.2f)
        water.calm()
        assertTrue(water.still)
        assertTrue(water.heights.all { it == 0f })
    }

    @Test
    fun resizedTheRipplesKeepTheirShare() {
        val water = surface()
        water.heights[24] = 3f
        water.resize(720f)
        assertEquals(3f, water.heightAt(24 * 720f / 47), 1e-4f)
    }
}
