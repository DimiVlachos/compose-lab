package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlitterFieldTest {
    private val dt = 1f / 120f

    private fun GlitterField.run(seconds: Float, energy: Float, from: Float = 0f): Float {
        var t = from
        repeat((seconds / dt).toInt()) {
            step(dt, t, energy, swirl = 1f)
            t += dt
        }
        return t
    }

    private fun GlitterField.meanY() = (0 until count).sumOf { y[it].toDouble() } / count

    @Test
    fun itStartsAtRestOnTheFigurine() {
        val g = GlitterField(SnowGlobeDimens.Flakes, seed = 7)
        for (i in 0 until g.count) {
            assertTrue(g.resting[i])
            assertEquals(g.restY(g.x[i]), g.y[i], 1e-5f)
        }
    }

    @Test
    fun flakesStayInsideTheSphereUnderAViolentFlow() {
        val g = GlitterField(SnowGlobeDimens.Flakes, seed = 7)
        g.run(seconds = 4f, energy = SnowGlobeDimens.MaxEnergy)
        val r = SnowGlobeDimens.InnerRadius
        for (i in 0 until g.count) {
            assertTrue(g.x[i] * g.x[i] + g.y[i] * g.y[i] <= r * r + 1e-4f, "flake $i escaped")
        }
    }

    @Test
    fun aStrongFlowLiftsTheGlitter() {
        val g = GlitterField(SnowGlobeDimens.Flakes, seed = 7)
        val before = g.meanY()
        g.run(seconds = 1f, energy = 1f)
        assertTrue(g.meanY() < before - 0.2, "mean y ${g.meanY()} vs $before")
    }

    @Test
    fun withNoFlowEveryFlakeComesToRestOnItsSurface() {
        val g = GlitterField(SnowGlobeDimens.Flakes, seed = 7)
        val t = g.run(seconds = 1.5f, energy = 1f)
        g.run(seconds = 6f, energy = 0f, from = t)
        for (i in 0 until g.count) {
            assertTrue(g.resting[i], "flake $i still falling at y=${g.y[i]}")
            assertTrue(abs(g.y[i] - g.restY(g.x[i])) < 1e-3f)
        }
    }

    @Test
    fun goldSettlesOnTheRoofs() {
        val g = GlitterField(SnowGlobeDimens.Flakes, seed = 7)
        val t = g.run(seconds = 1.5f, energy = 1f)
        g.run(seconds = 6f, energy = 0f, from = t)
        val onRoofs =
            (0 until g.count).count { i ->
                val x = g.x[i]
                (x in Figurine.CottageA.left..Figurine.CottageA.right ||
                    x in Figurine.CottageB.left..Figurine.CottageB.right) && g.y[i] < 0.25f
            }
        assertTrue(onRoofs >= 5, "only $onRoofs flakes on the roofs")
    }

    @Test
    fun glintsAreBetweenZeroAndOne() {
        val g = GlitterField(SnowGlobeDimens.Flakes, seed = 7)
        g.run(seconds = 0.5f, energy = 1f)
        for (i in 0 until g.count) assertTrue(g.glint(i) in 0f..1f)
    }
}
