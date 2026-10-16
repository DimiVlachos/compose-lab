package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlitterFieldTest {
    private val dt = 1f / 120f
    private val scenes = SantoriniScenes.shared

    private fun field() = GlitterField(scenes::floorAt)

    /** Steps the glitter in water stirred to [turb], as a shake leaves it, for [seconds]. */
    private fun GlitterField.run(seconds: Float, turb: Float, from: Float = 0f): Float {
        var t = from
        repeat((seconds / dt).toInt()) {
            step(dt, t, turb = turb, swirl = turb * .7f, am = turb * 300f, aN = 0f, tilt = 0f)
            t += dt
        }
        return t
    }

    private fun GlitterField.meanY() = (0 until count).sumOf { y[it].toDouble() } / count

    private fun GlitterField.restingPlace(i: Int): Float {
        val floor = scenes.floorAt(x[i])
        return floor + if (floor >= FLOOR) dy[i] else 0f
    }

    @Test
    fun itStartsAsThePrototypeSeedsIt() {
        val g = field()
        assertEquals(320, g.count)
        // Ten drift neutrally and sixteen more are still falling; the rest lie where they landed.
        for (i in 0 until 26) assertTrue(!g.rest[i], "flake $i should be in the water")
        for (i in 26 until g.count) {
            assertTrue(g.rest[i])
            assertEquals(g.restingPlace(i), g.y[i], 1e-5f)
        }
        assertTrue((0 until 10).all { g.neutral[it] })
    }

    @Test
    fun flakesStayInsideTheSphereUnderAViolentFlow() {
        val g = field()
        g.run(seconds = 4f, turb = 1.7f)
        for (i in 0 until g.count) {
            assertTrue(hypot(g.x[i], g.y[i]) <= .9f + 1e-4f, "flake $i escaped")
        }
    }

    @Test
    fun aStrongFlowLiftsTheGlitter() {
        val g = field()
        val before = g.meanY()
        g.run(seconds = 1f, turb = 1f)
        assertTrue(g.meanY() < before - 0.2, "mean y ${g.meanY()} vs $before")
    }

    @Test
    fun inStillWaterEveryFlakeComesToRestOnItsSurface() {
        val g = field()
        val t = g.run(seconds = 1.5f, turb = 1f)
        g.run(seconds = 8f, turb = 0f, from = t)
        // The neutral ten hang in the water by design.
        for (i in 10 until g.count) {
            assertTrue(g.rest[i], "flake $i still falling at y=${g.y[i]}")
            assertTrue(abs(g.y[i] - g.restingPlace(i)) < 1e-3f)
        }
    }

    @Test
    fun goldSettlesOnTheRoofs() {
        val g = field()
        val t = g.run(seconds = 1.5f, turb = 1f)
        g.run(seconds = 8f, turb = 0f, from = t)
        val onRoofs =
            (10 until g.count).count { i ->
                houses.any { h -> g.x[i] in h.x..h.x + h.w } && g.y[i] < FLOOR - .05f
            }
        assertTrue(onRoofs >= 10, "only $onRoofs flakes on the roofs")
    }

    @Test
    fun brightnessStaysInItsRange() {
        val g = field()
        g.run(seconds = 0.5f, turb = 1f)
        for (i in 0 until g.count) assertTrue(
            g.brightness(i, 0.5f, tilt = .1f, shift = .2f) in .28f..1f
        )
    }
}
