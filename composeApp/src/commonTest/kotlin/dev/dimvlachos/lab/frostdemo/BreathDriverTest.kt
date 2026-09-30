package dev.dimvlachos.lab.frostdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.presentation.components.frost.Breath
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
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
        val frost = FrostState()
        BreathDriver(frost, secondsToCover = 1.2f).advance(strength = 0.5f, seconds = 0.6f)

        assertNear(0.25f, (frost.marks.single() as Breath).level)
    }

    @Test
    fun noBlowLeavesTheGlassAlone() {
        val frost = FrostState()
        BreathDriver(frost).advance(strength = 0f, seconds = 1f)

        assertTrue(frost.marks.isEmpty())
    }

    @Test
    fun blowingAgainContinuesTheSameFog() {
        val frost = FrostState()
        val driver = BreathDriver(frost, secondsToCover = 1.2f)
        driver.advance(1f, 0.3f)
        driver.advance(0f, 1f)
        driver.advance(1f, 0.3f)

        assertNear(0.5f, (frost.marks.single() as Breath).level)
    }

    @Test
    fun aWipeMidBlowStartsNewFogFromTheBottomAndTheOldFogStays() {
        val frost = FrostState()
        val driver = BreathDriver(frost, secondsToCover = 1.2f)
        driver.advance(1f, 0.6f)
        val first = frost.marks.single() as Breath
        val wipe = frost.beginStroke(Offset(0.5f, 0.8f))
        driver.advance(1f, 0.12f)

        assertEquals(3, frost.marks.size)
        assertSame(wipe, frost.marks[1])
        val second = frost.marks[2] as Breath
        assertNotSame(first, second)
        assertNear(0.5f, first.level)
        assertNear(0.1f, second.level)
    }

    @Test
    fun aLongEnoughBlowFrostsTheGlassOver() {
        val frost = FrostState()
        frost.beginStroke(Offset(0.5f, 0.5f))
        BreathDriver(frost, secondsToCover = 1.2f).advance(1f, 1.3f)

        assertTrue(frost.marks.isEmpty())
    }

    @Test
    fun aScriptedBreathSwellsAndFades() {
        assertEquals(0f, scriptedBreathStrength(0f, peak = 1f))
        assertNear(0.8f, scriptedBreathStrength(0.5f, peak = 0.8f))
        assertTrue(scriptedBreathStrength(1f, peak = 1f) < 0.001f)
    }
}
