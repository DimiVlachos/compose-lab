package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypewriterDimensTest {
    @Test
    fun aUnitIsTheStagesWidthOverThreeHundredAndSixty() {
        assertEquals(1.dp, TypewriterDimens.unit(360.dp, 800.dp))
        assertEquals(TypewriterDimens.BiggestUnit, TypewriterDimens.unit(1_200.dp, 2_000.dp))
        assertEquals(TypewriterDimens.SmallestUnit, TypewriterDimens.unit(200.dp, 800.dp))
    }

    @Test
    fun onAShortStageTheMachineShrinks() {
        val wide = TypewriterDimens.unit(900.dp)
        val short = TypewriterDimens.unit(900.dp, 400.dp)
        assertTrue(short < wide, "$short on a short stage, $wide on a tall one")
        assertEquals(TypewriterDimens.SmallestUnit, TypewriterDimens.unit(900.dp, 100.dp))
    }

    @Test
    fun theKeysAreAThumbsWidthApartAtMostAndFitTenAcross() {
        assertEquals(TypewriterDimens.MostKeyPitch, TypewriterDimens.keyPitch(900.dp, 2_000.dp))
        val phone = TypewriterDimens.keyPitch(412.dp, 900.dp)
        assertTrue(phone * 10 + TypewriterDimens.KeysSide * 2 <= 412.dp)
        assertTrue(TypewriterDimens.keyPitch(900.dp, 300.dp) < phone)
    }
}
