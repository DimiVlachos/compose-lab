package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.platform.shakeImpulse
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
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
    fun aLongFlingCannotRunTheGlassAway() {
        val s = SnowGlobeState()
        s.drag(Offset(3f, -3f))
        var most = 0f
        repeat(120) {
            s.run(1f / 120f)
            most = maxOf(most, s.offset.getDistance())
        }
        assertTrue(most <= 0.75f, "the glass ran to $most")
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
        repeat(960) {
            s.run(1f / 120f)
            assertEquals(Offset.Zero, s.offset)
        }
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

    @Test
    fun withReducedMotionAShakeOfThePhoneRefreshesWithoutMovingTheGlass() {
        val s = SnowGlobeState()
        s.reducedMotion = true
        repeat(36) { i ->
            s.nudge(Offset(if (i % 6 < 3) 12f else -12f, 2f))
            s.step(1f / 60f, false, onShaken = { shaken++ }, onSettled = { settled++ })
            assertEquals(Offset.Zero, s.offset)
        }
        assertEquals(1, shaken)
    }

    /** The demo's scripted shake: the finger swings 0.55 each way at 2.5 Hz for 1.5 s. */
    private fun SnowGlobeState.scriptedShake() {
        val frame = 0.016f
        var previous = 0f
        repeat((1.5f / frame).toInt()) { i ->
            val x = 0.55f * sin(i * frame * 2.5f * 2f * PI.toFloat())
            drag(Offset(x - previous, 0f))
            previous = x
            step(frame, false, onShaken = { shaken++ }, onSettled = { settled++ })
        }
        release()
    }

    @Test
    fun theScriptedShakeCloudsTheWaterAndItClearsAgain() {
        val s = SnowGlobeState()
        s.scriptedShake()
        val g = s.glitter
        val r = SnowGlobeDimens.InnerRadius
        val airborne = (0 until g.count).count { g.y[it] < g.restY(g.x[it]) - 0.05f }
        val meanY = (0 until g.count).sumOf { g.y[it].toDouble() } / g.count
        // Crowded against the glass above the figurine: where the refraction smears it to streaks.
        val onTheGlass =
            (0 until g.count).count {
                g.y[it] < 0.3f && g.x[it] * g.x[it] + g.y[it] * g.y[it] > (r - 0.1f) * (r - 0.1f)
            }
        assertTrue(airborne >= g.count * 0.6f, "only $airborne of ${g.count} in the water")
        assertTrue(meanY < -0.1, "the glitter sits low, mean y $meanY")
        assertTrue(onTheGlass < g.count * 0.25f, "$onTheGlass flakes crowd the glass")

        var restedAfter = -1f
        var t = 0f
        while (t < 6f) {
            s.step(1f / 60f, false, onShaken = { shaken++ }, onSettled = { settled++ })
            t += 1f / 60f
            if ((0 until g.count).all { g.resting[it] }) {
                restedAfter = t
                break
            }
        }
        assertTrue(restedAfter > 0f, "still falling 6 s after the shake")
        assertEquals(1, shaken)
        assertEquals(1, settled)
    }

    private fun SnowGlobeState.feed(x: Float, y: Float) {
        shakeImpulse(x, y)?.let(::nudge)
        step(1f / 60f, false, onShaken = { shaken++ }, onSettled = { settled++ })
    }

    @Test
    fun walkingWithThePhoneNeverRefreshes() {
        val s = SnowGlobeState()
        // A heel strike twice a second: six samples at 60 Hz of a pocket jolt, then the sway.
        repeat(600) { i ->
            if (i % 30 < 6) s.feed(0.5f, 5f) else s.feed(0.3f, 1f)
        }
        assertEquals(0, shaken)
    }

    @Test
    fun aRealShakeOfThePhoneRefreshes() {
        val s = SnowGlobeState()
        repeat(36) { i -> s.feed(if (i % 6 < 3) 12f else -12f, 2f) }
        repeat(480) { s.feed(0f, 0f) }
        assertEquals(1, shaken)
    }
}
