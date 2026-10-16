package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SantoriniSceneTest {
    // Printed from the prototype in Node, from the same mulberry(7) and the same cliff.
    @Test
    fun theVillageMatchesThePrototypesSeed() {
        assertEquals(21, houses.size)
        assertEquals(-0.5995318098738789f, houses[0].x, 1e-5f)
        assertEquals(0.059097912878729404f, houses[0].w, 1e-5f)
        assertEquals(-0.10981725536515007f, houses[0].base, 1e-5f)
        assertEquals(-0.5297939897477626f, houses[1].x, 1e-5f)
        assertEquals(0.0924911044947803f, houses[1].w, 1e-5f)
        assertEquals(-0.42978391719795755f, houses[2].x, 1e-5f)
        assertEquals(listOf(0, 0, 0), houses.take(3).map { it.row })
    }

    @Test
    fun topAtFollowsTheCliffPoints() {
        // The cliff's points from the village eastwards, where x only grows (the west face
        // overhangs).
        for ((x, y) in TOPP.drop(5).take(18)) assertEquals(y, topAt(x), 1e-6f)
        assertEquals((-.155f + -.152f) / 2f, topAt(-.27f), 1e-6f)
    }

    @Test
    fun glitterRestsOnTheVillageAndOnTheSeaAwayFromIt() {
        val scenes = SantoriniScenes.paint()
        // Over the village it rests well above the sea; out past the cliff, on the sea.
        assertTrue(scenes.floorAt(0f) < -0.1f, "${scenes.floorAt(0f)}")
        assertEquals(FLOOR, scenes.floorAt(0.95f))
        assertEquals(FLOOR, scenes.floorAt(-0.95f))
    }

    @Test
    fun eachWeatherIsPaintedFullAndDifferent() {
        val scenes = SantoriniScenes.paint()
        val sun = scenes.scenes.getValue(GlobeScene.Sunny).toPixelMap()
        val shower = scenes.scenes.getValue(GlobeScene.Shower).toPixelMap()
        // Sky at the top middle: opaque, and the shower's greyer than the sun's.
        assertEquals(1f, sun[T / 2, T / 8].alpha, 0.01f)
        assertTrue(
            shower[T / 2, T / 8].blue - shower[T / 2, T / 8].red <
                sun[T / 2, T / 8].blue - sun[T / 2, T / 8].red
        )
    }
}
