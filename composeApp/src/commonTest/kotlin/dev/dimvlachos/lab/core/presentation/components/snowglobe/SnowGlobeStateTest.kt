package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnowGlobeStateTest {
    private val dt = 1f / 120f
    private var shaken = 0
    private var settled = 0

    private fun SnowGlobeState.run(seconds: Float, pending: Boolean = false) {
        repeat((seconds / dt).toInt()) {
            step(dt, pending, onShaken = { shaken++ }, onSettled = { settled++ })
        }
    }

    @Test
    fun energyBuildsWithAShakeAndFadesAway() {
        val s = SnowGlobeState()
        s.shake()
        s.run(0.05f)
        assertTrue(s.energy > SnowGlobeDimens.ShakeThreshold)
        s.run(6f)
        assertTrue(s.energy < SnowGlobeDimens.SettleThreshold)
    }

    @Test
    fun oneShakeFiresOnceAndSettlesOnce() {
        val s = SnowGlobeState()
        s.shake()
        s.run(8f)
        assertEquals(1, shaken)
        assertEquals(1, settled)
    }

    @Test
    fun settlingWaitsForTheRefresh() {
        val s = SnowGlobeState()
        s.shake()
        s.run(8f, pending = true)
        assertEquals(1, shaken)
        assertEquals(0, settled)
        assertTrue(s.energy >= SnowGlobeDimens.PendingFloor - 1e-3f)
        s.run(8f, pending = false)
        assertEquals(1, settled)
    }

    @Test
    fun aShakeDuringAPendingRefreshDoesNotShakeAgain() {
        val s = SnowGlobeState()
        s.shake()
        s.run(1f, pending = true)
        s.shake()
        s.run(1f, pending = true)
        assertEquals(1, shaken)
    }

    @Test
    fun aRockStirsButNeverRefreshes() {
        val s = SnowGlobeState()
        s.rock(1)
        s.run(4f)
        assertEquals(0, shaken)
        assertEquals(0, settled)
    }

    @Test
    fun draggingTheGlassBackAndForthShakesIt() {
        val s = SnowGlobeState()
        repeat(12) { i ->
            s.drag(Offset(if (i % 2 == 0) 0.25f else -0.25f, 0f))
            s.run(0.08f)
        }
        s.release()
        s.run(0.1f)
        assertEquals(1, shaken)
    }

    @Test
    fun theGlassSpringsBackAfterAFling() {
        val s = SnowGlobeState()
        s.drag(Offset(3f, -3f))
        s.run(0.05f)
        s.release()
        s.run(3f)
        assertTrue(abs(s.offset.x) < 0.01f && abs(s.offset.y) < 0.01f, "${s.offset}")
    }

    @Test
    fun aNewSceneCrossfadesIn() {
        val s = SnowGlobeState()
        s.showScene(GlobeScene.Shower)
        assertEquals(GlobeScene.Sunny, s.sceneFrom)
        assertEquals(GlobeScene.Shower, s.sceneTo)
        s.run(SnowGlobeDimens.SceneFadeSeconds + 0.1f)
        assertEquals(1f, s.fade)
        assertEquals(GlobeScene.Shower, s.sceneFrom)
    }

    @Test
    fun withReducedMotionTheGlitterStaysDownButTheShakeStillRefreshes() {
        val s = SnowGlobeState()
        s.reducedMotion = true
        s.shake()
        s.run(8f)
        assertEquals(1, shaken)
        assertEquals(1, settled)
        assertTrue((0 until s.glitter.count).all { s.glitter.resting[it] })
    }

    @Test
    fun withReducedMotionARockLeavesTheGlobeStill() {
        val s = SnowGlobeState()
        s.reducedMotion = true
        s.rock(1)
        s.run(1f)
        assertEquals(0f, s.energy)
        assertEquals(Offset.Zero, s.offset)
        assertEquals(0, shaken)
        assertEquals(0, settled)
    }
}
