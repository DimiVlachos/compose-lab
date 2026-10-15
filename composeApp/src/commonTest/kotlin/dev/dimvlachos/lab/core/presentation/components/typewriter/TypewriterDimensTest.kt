package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypewriterDimensTest {
    @Test
    fun aUnitIsTheStagesWidthOverThreeHundredAndSixty() {
        assertEquals(1.dp, TypewriterDimens.fit(360.dp, 900.dp).unit)
        assertEquals(TypewriterDimens.BiggestUnit, TypewriterDimens.fit(1_200.dp, 2_000.dp).unit)
        assertEquals(TypewriterDimens.SmallestUnit, TypewriterDimens.fit(200.dp, 900.dp).unit)
    }

    @Test
    fun onAShortStageTheMachineAndItsKeysShrinkToFit() {
        val tall = TypewriterDimens.fit(440.dp, 2_000.dp)
        for (height in listOf(420.dp, 340.dp, 300.dp, 270.dp)) {
            val short = TypewriterDimens.fit(440.dp, height)
            assertTrue(short.unit < tall.unit, "${short.unit} on a $height stage")
            assertTrue(short.height <= height, "${short.height} on a $height stage")
            assertTrue(short.pitch >= TypewriterDimens.LeastKeyPitch)
        }
        // Too short for anything: as small as it goes.
        val tiny = TypewriterDimens.fit(440.dp, 100.dp)
        assertEquals(TypewriterDimens.ShortestUnit, tiny.unit)
        assertEquals(TypewriterDimens.LeastKeyPitch, tiny.pitch)
    }

    @Test
    fun aTallHintGetsTheRoomItNeeds() {
        val plain = TypewriterDimens.fit(412.dp, 900.dp)
        val big = TypewriterDimens.fit(412.dp, 900.dp, hint = 60.dp)
        assertTrue(big.hintRoom >= big.unit * TypewriterDimens.HintTop + 60.dp)
        assertEquals(plain.unit, big.unit)
        // On a short stage the machine makes room for it.
        val short = TypewriterDimens.fit(440.dp, 300.dp, hint = 60.dp)
        assertTrue(short.height <= 300.dp)
    }

    @Test
    fun theKeysAreAThumbsWidthApartAtMostAndFitTenAcross() {
        assertEquals(TypewriterDimens.MostKeyPitch, TypewriterDimens.fit(900.dp, 2_000.dp).pitch)
        val phone = TypewriterDimens.fit(412.dp, 900.dp)
        assertTrue(phone.pitch * 10 + TypewriterDimens.KeysSide * 2 <= 412.dp)
        // With room to spare, a row is as tall as a thumb's target.
        assertEquals(TypewriterDimens.KeyTarget, phone.rowHeight)
    }

    @Test
    fun onAStageTooShortForAllOfItTheLipRunsIntoTheFootAndThenTheHintGoes() {
        val lipped = TypewriterDimens.fit(440.dp, 245.dp, hint = 26.dp, foot = 24.dp)
        assertTrue(lipped.lip < TypewriterDimens.FrontLip)
        assertTrue(lipped.hinted)
        val bare = TypewriterDimens.fit(440.dp, 240.dp, hint = 40.dp, foot = 24.dp)
        assertTrue(!bare.hinted)
        assertTrue(bare.height <= 240.dp, "${bare.height}")
    }
}
