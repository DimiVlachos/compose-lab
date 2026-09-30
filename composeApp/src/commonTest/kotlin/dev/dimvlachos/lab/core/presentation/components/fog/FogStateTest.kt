package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The strokes are observable lists, which compare by identity; the points are what count.
private fun FogState.points() = strokes.map { it.toList() }

class FogStateTest {
    @Test
    fun startsFullyFogged() {
        assertTrue(FogState().strokes.isEmpty())
    }

    @Test
    fun aDragExtendsTheStrokeItBegan() {
        val fog = FogState()
        val stroke = fog.beginStroke(Offset(0.1f, 0.1f))
        fog.extendStroke(stroke, Offset(0.2f, 0.1f))
        fog.extendStroke(stroke, Offset(0.3f, 0.2f))

        assertEquals(
            listOf(listOf(Offset(0.1f, 0.1f), Offset(0.2f, 0.1f), Offset(0.3f, 0.2f))),
            fog.points(),
        )
    }

    @Test
    fun twoFingersAtOnceKeepTheirOwnStrokes() {
        // The script's finger and a real one, moving in turn: neither joins onto the other.
        val fog = FogState()
        val scripted = fog.beginStroke(Offset(0.1f, 0.3f))
        val real = fog.beginStroke(Offset(0.1f, 0.8f))
        fog.extendStroke(scripted, Offset(0.5f, 0.3f))
        fog.extendStroke(real, Offset(0.5f, 0.8f))

        assertEquals(
            listOf(
                listOf(Offset(0.1f, 0.3f), Offset(0.5f, 0.3f)),
                listOf(Offset(0.1f, 0.8f), Offset(0.5f, 0.8f)),
            ),
            fog.points(),
        )
    }

    @Test
    fun aFingerStillMovingWhenTheGlassFogsWipesNothingMore() {
        val fog = FogState()
        val stroke = fog.beginStroke(Offset(0.1f, 0.1f))
        fog.clear()
        fog.extendStroke(stroke, Offset(0.2f, 0.2f))

        assertTrue(fog.strokes.isEmpty())
    }

    @Test
    fun clearRestoresTheFog() {
        val fog = FogState()
        fog.beginStroke(Offset(0.1f, 0.1f))
        fog.clear()

        assertTrue(fog.strokes.isEmpty())
    }

    @Test
    fun wipesAndBreathsKeepTheOrderTheyWereMadeIn() {
        val fog = FogState()
        val first = fog.beginStroke(Offset(0.1f, 0.1f))
        val breath = fog.beginBreath()
        val second = fog.beginStroke(Offset(0.2f, 0.2f))

        assertEquals(listOf<FogMark>(first, breath, second), fog.marks.toList())
    }

    @Test
    fun aBreathRisesButNeverFalls() {
        val fog = FogState()
        val breath = fog.beginBreath()
        fog.setBreathLevel(breath, 0.4f)
        fog.setBreathLevel(breath, 0.2f)

        assertEquals(0.4f, breath.level)
    }

    @Test
    fun aBreathReachingTheTopFogsOverEverythingBeforeIt() {
        val fog = FogState()
        fog.beginStroke(Offset(0.1f, 0.1f))
        val earlier = fog.beginBreath()
        fog.setBreathLevel(earlier, 0.3f)
        val breath = fog.beginBreath()
        val after = fog.beginStroke(Offset(0.5f, 0.5f))

        fog.setBreathLevel(breath, 1f)

        assertEquals(listOf<FogMark>(after), fog.marks.toList())
    }

    @Test
    fun aBreathDroppedByClearStaysGone() {
        val fog = FogState()
        val breath = fog.beginBreath()
        fog.clear()
        fog.setBreathLevel(breath, 0.5f)

        assertTrue(fog.marks.isEmpty())
    }

    @Test
    fun aWindowCanStartClear() {
        val evaporation = FogState(startClear = true).marks.single() as Evaporation
        assertEquals(1f, evaporation.amount)
    }

    @Test
    fun aFullBreathFogsClearGlass() {
        val fog = FogState(startClear = true)
        fog.setBreathLevel(fog.beginBreath(), 1f)

        assertTrue(fog.marks.isEmpty())
    }

    @Test
    fun anEvaporationClearsEverythingBeforeItOnceComplete() {
        val fog = FogState()
        fog.beginStroke(Offset(0.1f, 0.1f))
        fog.setBreathLevel(fog.beginBreath(), 0.5f)
        val evaporation = fog.beginEvaporation()

        fog.setEvaporationAmount(evaporation, 0.5f)
        assertEquals(3, fog.marks.size)

        fog.setEvaporationAmount(evaporation, 1f)
        assertEquals(listOf<FogMark>(evaporation), fog.marks.toList())
    }

    @Test
    fun anEvaporationNeverFogsBack() {
        val fog = FogState()
        val evaporation = fog.beginEvaporation()
        fog.setEvaporationAmount(evaporation, 0.6f)
        fog.setEvaporationAmount(evaporation, 0.3f)

        assertEquals(0.6f, evaporation.amount)
    }

    @Test
    fun evaporatingAtOnceLeavesClearGlass() {
        val fog = FogState()
        fog.beginStroke(Offset(0.1f, 0.1f))
        fog.evaporate()

        assertEquals(1f, (fog.marks.single() as Evaporation).amount)
    }
}
