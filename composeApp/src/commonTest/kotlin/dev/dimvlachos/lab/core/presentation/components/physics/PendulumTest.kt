package dev.dimvlachos.lab.core.presentation.components.physics

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PendulumTest {
    private val step = 1f / 120f

    @Test
    fun pushedItSwingsAtItsOwnRateAndDiesAway() {
        val pendulum = Pendulum(hz = 2f, damping = 0.1f)
        pendulum.swing = 2f
        // Count the times it passes through straight down in two seconds: twice a swing.
        var crossings = 0
        var last = pendulum.angle
        repeat(240) {
            pendulum.step(step)
            if (last != 0f && (last > 0f) != (pendulum.angle > 0f)) crossings++
            last = pendulum.angle
        }
        assertTrue(crossings in 7..9, "it crossed $crossings times in two seconds at 2 Hz")
        repeat(1_200) { pendulum.step(step) }
        assertTrue(
            pendulum.isStill(0.002f, 0.01f),
            "angle ${pendulum.angle}, swing ${pendulum.swing}",
        )
    }

    @Test
    fun aTurnTiltsItAndItNeverGoesPastItsMost() {
        val pendulum = Pendulum(hz = 1f, damping = 0.5f, most = 0.3f)
        repeat(600) { pendulum.step(step, turn = 100f) }
        assertEquals(0.3f, pendulum.angle, 1e-4f)
        assertEquals(0f, pendulum.swing)
    }

    @Test
    fun hangingStraightAndStillItStaysSo() {
        val pendulum = Pendulum(hz = 1.5f, damping = 0.2f)
        repeat(120) { pendulum.step(step) }
        assertEquals(0f, pendulum.angle)
        assertTrue(abs(pendulum.swing) == 0f)
    }
}
