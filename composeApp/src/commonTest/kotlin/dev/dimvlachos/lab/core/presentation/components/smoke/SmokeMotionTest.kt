package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmokeMotionTest {
    private val out = FloatArray(2)
    private val scratch = FloatArray(2)

    private fun at(
        age: Float,
        material: SmokeMaterial = SmokeMaterial.Smoke,
        seed: Float = 0.37f,
    ): Pair<Float, Float> {
        val life = SmokeMotion.life(seed, ink = true, material)
        SmokeMotion.place(120f, 300f, seed, true, age, life, material, -1f, 2.5f, out, scratch)
        return out[0] to out[1]
    }

    @Test
    fun aParticlesPlaceDependsOnlyOnWhereItStartedAndItsAge() {
        val later = at(1.3f)
        at(0.4f)
        at(2.2f, SmokeMaterial.Mist)
        assertEquals(later, at(1.3f))
    }

    @Test
    fun atNoAgeAParticleIsWhereItsPixelWas() {
        assertEquals(120f to 300f, at(0f))
    }

    @Test
    fun smokeRisesAndMistDriftsTheWayItWasSwiped() {
        val (_, smokeY) = at(1.5f)
        assertTrue(smokeY < 300f - 60f, "smoke rose only to $smokeY")
        val life = SmokeMotion.life(0.37f, ink = true, SmokeMaterial.Mist)
        SmokeMotion.place(
            120f,
            300f,
            0.37f,
            true,
            1.5f,
            life,
            SmokeMaterial.Mist,
            1f,
            2.5f,
            out,
            scratch,
        )
        val mistRise = 300f - out[1]
        assertTrue(out[0] > 120f + 40f, "mist drifted only to ${out[0]}")
        assertTrue(mistRise < 300f - smokeY, "mist rose $mistRise, as far as smoke")
    }

    @Test
    fun reformingRunsAParticlesAgeBackToNothing() {
        val life = 3f
        assertEquals(1f, SmokeMotion.ageAt(column = 1.25f, jitter = 0.25f, life = life))
        // Faded by the time undo began: breathed back from half its life, then younger.
        val begin = SmokeMotion.ageAt(5f, 0f, life, reform = 0f)
        assertEquals(life * SmokeDimens.ReformFrom, begin)
        assertEquals(begin / 2f, SmokeMotion.ageAt(5f, 0f, life, reform = 0.5f))
        assertEquals(0f, SmokeMotion.ageAt(5f, 0f, life, reform = 1f))
        // A particle still in the air when undo began starts back from where it is: no jump.
        val young = SmokeMotion.ageAt(1.5f, 0f, life, reform = 0f)
        assertEquals(SmokeMotion.ageAt(1.5f, 0f, life), young)
        assertEquals(young / 2f, SmokeMotion.ageAt(1.5f, 0f, life, reform = 0.5f))
        // A particle that hadn't lifted off when reforming began has nothing to bring back.
        assertTrue(SmokeMotion.ageAt(0.05f, 0.07f, life, reform = 0.5f) < 0f)
    }

    @Test
    fun theCurlIsTheRotatedSlopeOfTheField() {
        val e = 1e-3f
        val curl = FloatArray(2)
        for ((x, y) in listOf(0.3f to 0.7f, 2.4f to -1.2f, -5.1f to 3.3f)) {
            CurlNoise.curl(x, y, curl)
            val ddx = (CurlNoise.value(x + e, y) - CurlNoise.value(x - e, y)) / (2 * e)
            val ddy = (CurlNoise.value(x, y + e) - CurlNoise.value(x, y - e)) / (2 * e)
            assertTrue(abs(curl[0] - ddy) < 2e-2f, "curl x at $x, $y: ${curl[0]} vs $ddy")
            assertTrue(abs(curl[1] + ddx) < 2e-2f, "curl y at $x, $y: ${curl[1]} vs ${-ddx}")
        }
    }
}
