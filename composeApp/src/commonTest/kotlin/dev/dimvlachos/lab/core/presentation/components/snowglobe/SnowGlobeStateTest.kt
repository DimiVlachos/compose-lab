package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.platform.shakeImpulse
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
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

    private fun stateWithGlitter() =
        SnowGlobeState().apply { useFloor(SantoriniScenes.shared::floorAt) }

    @Test
    fun aShakeStirsTheWaterAndItCalmsAgain() {
        val s = SnowGlobeState()
        s.shake()
        s.run(1.1f)
        assertTrue(s.turb > .6f, "turb ${s.turb}")
        s.run(6f)
        assertTrue(s.turb < .34f, "turb ${s.turb}")
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
        s.run(1f, pending = false)
        assertEquals(1, settled)
    }

    @Test
    fun aShakeDuringAPendingRefreshDoesNotShakeAgain() {
        val s = SnowGlobeState()
        s.shake()
        s.run(1.2f, pending = true)
        s.shake()
        s.run(1.2f, pending = true)
        assertEquals(1, shaken)
    }

    @Test
    fun aRockStirsButNeverRefreshes() {
        val s = SnowGlobeState()
        s.rock(1)
        s.run(0.2f)
        assertTrue(s.turb > .1f)
        s.run(4f)
        assertEquals(0, shaken)
        assertEquals(0, settled)
    }

    @Test
    fun draggingTheGlassBackAndForthShakesIt() {
        val s = SnowGlobeState()
        s.drag(60f)
        repeat(12) { i ->
            s.run(0.08f)
            s.drag(if (i % 2 == 0) -120f else 120f)
        }
        s.release()
        s.run(0.1f)
        assertEquals(1, shaken)
    }

    @Test
    fun aSlowDragIsNotAShake() {
        val s = SnowGlobeState()
        repeat(3) {
            s.drag(40f)
            s.run(0.6f)
            s.drag(-40f)
            s.run(0.6f)
        }
        s.release()
        s.run(1f)
        assertEquals(0, shaken)
    }

    @Test
    fun theGlassSpringsBackAfterAFling() {
        val s = SnowGlobeState()
        s.drag(300f)
        s.run(0.05f)
        s.release()
        s.run(3f)
        assertTrue(abs(s.offset.x) < 0.5f && abs(s.offset.y) < 0.5f, "${s.offset}")
    }

    @Test
    fun aLongFlingCannotRunTheGlassAway() {
        val s = SnowGlobeState()
        s.drag(2000f)
        var most = 0f
        repeat(120) {
            s.run(1f / 120f)
            most = maxOf(most, abs(s.offset.x))
        }
        // The prototype's rubber band: 74 px at most, give or take the spring's overshoot.
        assertTrue(most <= 80f, "the glass ran to $most")
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
    fun theHintFollowsTheShake() {
        val s = SnowGlobeState()
        s.drag(60f)
        assertEquals(GlobeHint.Idle, s.hint)
        var sawKeepShaking = false
        var sawLetItSettle = false
        repeat(12) { i ->
            s.run(0.08f)
            sawKeepShaking = sawKeepShaking || s.hint == GlobeHint.KeepShaking
            sawLetItSettle = sawLetItSettle || s.hint == GlobeHint.LetItSettle
            s.drag(if (i % 2 == 0) -120f else 120f)
        }
        assertTrue(sawKeepShaking && sawLetItSettle)
        s.release()
        s.run(0.05f)
        assertEquals(GlobeHint.Settling, s.hint)
        s.run(8f)
        assertEquals(GlobeHint.Idle, s.hint)
    }

    @Test
    fun withReducedMotionTheGlitterStaysPutButTheShakeStillRefreshes() {
        val s = stateWithGlitter()
        s.reducedMotion = true
        val g = checkNotNull(s.glitter)
        val before = g.y.copyOf()
        s.shake()
        repeat(960) {
            s.run(1f / 120f)
            assertEquals(Offset.Zero, s.offset)
            assertEquals(0f, s.tilt)
        }
        assertEquals(1, shaken)
        assertEquals(1, settled)
        assertTrue(before.contentEquals(g.y))
    }

    @Test
    fun withReducedMotionARockLeavesTheGlobeStill() {
        val s = SnowGlobeState()
        s.reducedMotion = true
        s.rock(1)
        s.run(1f)
        assertEquals(0f, s.turb)
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

    /**
     * The prototype's demo shake: the finger swings 64 px at 3.4 Hz for 1.5 s, easing in and out.
     */
    private fun SnowGlobeState.scriptedShake() {
        val frame = 1f / 60f
        var previous = 0f
        val frames = (1.5f / frame).toInt()
        for (i in 0..frames) {
            val p = i.toFloat() / frames
            val ease = if (p < .85f) 1f else 1f - smoothstep((p - .85f) / .15f)
            val x =
                64f *
                    sin(p * 2f * PI.toFloat() * 3.4f) *
                    sin(min(1f, p * 1.6f) * PI.toFloat() / 2f) *
                    ease
            drag(x - previous)
            previous = x
            step(frame, false, onShaken = { shaken++ }, onSettled = { settled++ })
        }
        release()
    }

    private fun smoothstep(t: Float) = t * t * (3 - 2 * t)

    @Test
    fun theScriptedShakeCloudsTheWaterAndItClearsAgain() {
        val s = stateWithGlitter()
        s.scriptedShake()
        val g = checkNotNull(s.glitter)
        val airborne = (0 until g.count).count { !g.rest[it] }
        assertTrue(airborne >= g.count * 0.6f, "only $airborne of ${g.count} in the water")
        assertEquals(1, shaken)

        var restedAfter = -1f
        var t = 0f
        while (t < 12f) {
            s.step(1f / 60f, false, onShaken = { shaken++ }, onSettled = { settled++ })
            t += 1f / 60f
            if ((10 until g.count).all { g.rest[it] }) {
                restedAfter = t
                break
            }
        }
        assertTrue(restedAfter > 2f, "settled too soon, after $restedAfter s")
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
        repeat(600) { i -> if (i % 30 < 6) s.feed(0.5f, 5f) else s.feed(0.3f, 1f) }
        assertEquals(0, shaken)
    }

    @Test
    fun aRealShakeOfThePhoneRefreshes() {
        val s = SnowGlobeState()
        repeat(36) { i -> s.feed(if (i % 6 < 3) 12f else -12f, 2f) }
        repeat(480) { s.feed(0f, 0f) }
        assertEquals(1, shaken)
        assertEquals(1, settled)
    }

    @Test
    fun aResetPutsTheGlitterBackAndForgetsTheShake() {
        val s = stateWithGlitter()
        val g = checkNotNull(s.glitter)
        val seeded = g.y.copyOf()
        s.shake()
        s.run(0.6f)
        s.reset()
        assertTrue(seeded.contentEquals(g.y))
        assertEquals(0f, s.turb)
        s.run(8f)
        assertEquals(0, shaken)
    }
}
