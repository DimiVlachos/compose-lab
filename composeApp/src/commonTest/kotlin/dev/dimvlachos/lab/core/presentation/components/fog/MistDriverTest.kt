package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// A steamy bathroom mists its mirror back over, slowly, once the hand has stopped wiping.
class MistDriverTest {
    private val step = 1 / 60f

    private fun MistDriver.run(seconds: Float) {
        var time = 0f
        while (time < seconds) {
            advance(step)
            time += step
        }
    }

    private fun FogState.wipes() = marks.filterIsInstance<WipeStroke>().filter { it.clarity >= 1f }

    private fun FogState.mists() = marks.filterIsInstance<Mist>()

    @Test
    fun unwipedGlassNeedsNoMist() {
        val fog = FogState()
        val mist = MistDriver(fog)
        assertFalse(mist.needed)
        mist.run(30f)
        assertTrue(fog.marks.isEmpty())
    }

    @Test
    fun theMistWaitsForTheHandThenRisesSlowly() {
        val fog = FogState()
        fog.beginStroke(Offset(0.2f, 0.5f)).also { fog.extendStroke(it, Offset(0.8f, 0.5f)) }
        val mist = MistDriver(fog)
        assertTrue(mist.needed)

        mist.run(1.5f)
        assertTrue(fog.mists().isEmpty(), "not while the hand may still be wiping")
        mist.run(1f)
        val rising = fog.mists().single()
        mist.run(10f)
        assertTrue(rising.amount in 0.3f..0.5f, "a slow rise: ${rising.amount}")
    }

    @Test
    fun aMistedMirrorIsFreshFogAgain() {
        val fog = FogState()
        fog.beginStroke(Offset(0.2f, 0.5f)).also { fog.extendStroke(it, Offset(0.8f, 0.5f)) }
        val mist = MistDriver(fog)
        mist.run(30f)
        assertTrue(fog.marks.isEmpty(), "${fog.marks}")
        assertFalse(mist.needed)
    }

    @Test
    fun wipingAgainClearsTheMistAndItStartsOverAfterwards() {
        val fog = FogState()
        fog.beginStroke(Offset(0.2f, 0.5f)).also { fog.extendStroke(it, Offset(0.8f, 0.5f)) }
        val mist = MistDriver(fog)
        mist.run(8f)
        val first = fog.mists().single()

        val again = fog.beginStroke(Offset(0.2f, 0.3f))
        repeat(60) {
            fog.extendStroke(again, Offset(0.2f + it * 0.01f, 0.3f))
            mist.advance(step)
        }
        assertEquals(first, fog.mists().single(), "no new mist while the hand is wiping")
        assertTrue(fog.marks.last() === again, "the new wipe is on top, clear")

        mist.run(3f)
        assertEquals(2, fog.mists().size, "misting over the new wipe too")
        mist.run(30f)
        assertTrue(fog.wipes().isEmpty(), "${fog.marks}")
    }

    @Test
    fun aDropsTrailDoesNotHoldTheMistBack() {
        val fog = FogState()
        fog.beginStroke(Offset(0.2f, 0.5f)).also { fog.extendStroke(it, Offset(0.8f, 0.5f)) }
        val mist = MistDriver(fog)
        mist.run(5f)
        val rising = fog.mists().single()
        val before = rising.amount
        fog.beginStroke(Offset(0.5f, 0.1f), clarity = 0.9f)
        mist.run(2f)
        assertTrue(rising.amount > before, "still rising: ${rising.amount}")
    }
}
