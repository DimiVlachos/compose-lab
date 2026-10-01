package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.fog.Breath
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BreathDriverTest {
    private fun assertNear(expected: Float, actual: Float) =
        assertTrue(abs(expected - actual) < 0.001f, "expected $expected, got $actual")

    @Test
    fun theFogRisesWithTheBlowsStrength() {
        val fog = FogState()
        BreathDriver(fog, secondsToCover = 1.2f).advance(strength = 0.5f, seconds = 0.6f)

        assertNear(0.25f, (fog.marks.single() as Breath).level)
    }

    @Test
    fun noBlowLeavesTheGlassAlone() {
        val fog = FogState()
        BreathDriver(fog).advance(strength = 0f, seconds = 1f)

        assertTrue(fog.marks.isEmpty())
    }

    @Test
    fun blowingAgainContinuesTheSameFog() {
        val fog = FogState()
        val driver = BreathDriver(fog, secondsToCover = 1.2f)
        driver.advance(1f, 0.3f)
        driver.advance(0f, 1f)
        driver.advance(1f, 0.3f)

        assertNear(0.5f, (fog.marks.single() as Breath).level)
    }

    @Test
    fun aWipeMidBlowStartsNewFogFromTheBottomAndTheOldFogStays() {
        val fog = FogState()
        val driver = BreathDriver(fog, secondsToCover = 1.2f)
        driver.advance(1f, 0.6f)
        val first = fog.marks.single() as Breath
        val wipe = fog.beginStroke(Offset(0.5f, 0.8f))
        driver.advance(1f, 0.12f)

        assertEquals(3, fog.marks.size)
        assertSame(wipe, fog.marks[1])
        val second = fog.marks[2] as Breath
        assertNotSame(first, second)
        assertNear(0.5f, first.level)
        assertNear(0.1f, second.level)
    }

    @Test
    fun aLongEnoughBlowFogsTheGlassOver() {
        val fog = FogState()
        fog.beginStroke(Offset(0.5f, 0.5f))
        BreathDriver(fog, secondsToCover = 1.2f).advance(1f, 1.3f)

        assertTrue(fog.marks.isEmpty())
    }

    @Test
    fun aDropsStreakMidBreathDoesNotRestartTheFog() {
        val fog = FogState(startClear = true)
        val driver = BreathDriver(fog)
        driver.advance(1f, 0.3f)
        // A running drop's burst lays a thin, part-clear streak on the glass.
        fog.beginStroke(Offset(0.5f, 0.2f), clarity = 0.9f)
        driver.advance(1f, 0.3f)

        val breaths = fog.marks.filterIsInstance<Breath>()
        assertEquals(1, breaths.size, "one breath rising on: ${fog.marks}")
        assertTrue(breaths.single().level > 0.45f, "${breaths.single().level}")
    }
}
