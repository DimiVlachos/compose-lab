package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class WipeDabsTest {
    @Test
    fun aTapIsOneDab() {
        assertEquals(listOf(Offset(4f, 4f)), wipeDabs(listOf(Offset(4f, 4f)), spacing = 2f))
    }

    @Test
    fun aSegmentIsFilledAtEvenSpacing() {
        assertEquals(
            listOf(
                Offset(0f, 0f),
                Offset(2.5f, 0f),
                Offset(5f, 0f),
                Offset(7.5f, 0f),
                Offset(10f, 0f),
            ),
            wipeDabs(listOf(Offset(0f, 0f), Offset(10f, 0f)), spacing = 2.5f),
        )
    }

    @Test
    fun spacingCarriesAcrossPointsSoSlowDragsDoNotBunch() {
        // Pointer events 3 px apart with 2 px spacing: dabs at 0, 2, 4, 6, not restarting at 3.
        assertEquals(
            listOf(Offset(0f, 0f), Offset(2f, 0f), Offset(4f, 0f), Offset(6f, 0f)),
            wipeDabs(listOf(Offset(0f, 0f), Offset(3f, 0f), Offset(6f, 0f)), spacing = 2f),
        )
    }

    @Test
    fun repeatedPointsAddNoDabs() {
        assertEquals(
            listOf(Offset(1f, 1f)),
            wipeDabs(listOf(Offset(1f, 1f), Offset(1f, 1f), Offset(1f, 1f)), spacing = 2f),
        )
    }
}
