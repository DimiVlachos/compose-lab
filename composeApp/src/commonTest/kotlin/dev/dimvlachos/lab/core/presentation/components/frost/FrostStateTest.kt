package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The strokes are observable lists, which compare by identity; the points are what count.
private fun FrostState.points() = strokes.map { it.toList() }

class FrostStateTest {
    @Test
    fun startsFullyFrosted() {
        assertTrue(FrostState().strokes.isEmpty())
    }

    @Test
    fun aDragExtendsTheStrokeItBegan() {
        val frost = FrostState()
        val stroke = frost.beginStroke(Offset(0.1f, 0.1f))
        frost.extendStroke(stroke, Offset(0.2f, 0.1f))
        frost.extendStroke(stroke, Offset(0.3f, 0.2f))

        assertEquals(
            listOf(listOf(Offset(0.1f, 0.1f), Offset(0.2f, 0.1f), Offset(0.3f, 0.2f))),
            frost.points(),
        )
    }

    @Test
    fun twoFingersAtOnceKeepTheirOwnStrokes() {
        // The script's finger and a real one, moving in turn: neither joins onto the other.
        val frost = FrostState()
        val scripted = frost.beginStroke(Offset(0.1f, 0.3f))
        val real = frost.beginStroke(Offset(0.1f, 0.8f))
        frost.extendStroke(scripted, Offset(0.5f, 0.3f))
        frost.extendStroke(real, Offset(0.5f, 0.8f))

        assertEquals(
            listOf(
                listOf(Offset(0.1f, 0.3f), Offset(0.5f, 0.3f)),
                listOf(Offset(0.1f, 0.8f), Offset(0.5f, 0.8f)),
            ),
            frost.points(),
        )
    }

    @Test
    fun aFingerStillMovingWhenTheGlassRefrostsWipesNothingMore() {
        val frost = FrostState()
        val stroke = frost.beginStroke(Offset(0.1f, 0.1f))
        frost.clear()
        frost.extendStroke(stroke, Offset(0.2f, 0.2f))

        assertTrue(frost.strokes.isEmpty())
    }

    @Test
    fun clearRestoresTheFrost() {
        val frost = FrostState()
        frost.beginStroke(Offset(0.1f, 0.1f))
        frost.clear()

        assertTrue(frost.strokes.isEmpty())
    }

    @Test
    fun wipesAndBreathsKeepTheOrderTheyWereMadeIn() {
        val frost = FrostState()
        val first = frost.beginStroke(Offset(0.1f, 0.1f))
        val breath = frost.beginBreath()
        val second = frost.beginStroke(Offset(0.2f, 0.2f))

        assertEquals(listOf<FrostMark>(first, breath, second), frost.marks.toList())
    }

    @Test
    fun aBreathRisesButNeverFalls() {
        val frost = FrostState()
        val breath = frost.beginBreath()
        frost.setBreathLevel(breath, 0.4f)
        frost.setBreathLevel(breath, 0.2f)

        assertEquals(0.4f, breath.level)
    }

    @Test
    fun aBreathReachingTheTopFrostsOverEverythingBeforeIt() {
        val frost = FrostState()
        frost.beginStroke(Offset(0.1f, 0.1f))
        val earlier = frost.beginBreath()
        frost.setBreathLevel(earlier, 0.3f)
        val breath = frost.beginBreath()
        val after = frost.beginStroke(Offset(0.5f, 0.5f))

        frost.setBreathLevel(breath, 1f)

        assertEquals(listOf<FrostMark>(after), frost.marks.toList())
    }

    @Test
    fun aBreathDroppedByClearStaysGone() {
        val frost = FrostState()
        val breath = frost.beginBreath()
        frost.clear()
        frost.setBreathLevel(breath, 0.5f)

        assertTrue(frost.marks.isEmpty())
    }
}
