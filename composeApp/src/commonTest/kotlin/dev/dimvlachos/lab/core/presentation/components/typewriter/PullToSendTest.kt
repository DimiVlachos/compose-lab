package dev.dimvlachos.lab.core.presentation.components.typewriter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PullToSendTest {
    @Test
    fun thePaperGivesLessTheFurtherItIsPulledAndNeverPastItsMost() {
        val near = PullToSend.shown(20f, inked = true)
        val far = PullToSend.shown(200f, inked = true)
        assertTrue(near < 20f && near > 17f, "$near")
        assertTrue(far < 200f && far > near)
        assertTrue(PullToSend.shown(10_000f, inked = true) <= TypewriterDimens.MostPull)
        assertEquals(0f, PullToSend.shown(-30f, inked = true))
    }

    @Test
    fun aBlankSheetBarelyMoves() {
        assertTrue(PullToSend.shown(300f, inked = false) <= TypewriterDimens.EmptyPull)
    }

    @Test
    fun letGoPastTheThresholdItIsSentAndShortOfItItSlidesBack() {
        assertTrue(PullToSend.sends(TypewriterDimens.SendPull + 1f, 0f, inked = true))
        assertFalse(PullToSend.sends(TypewriterDimens.SendPull - 1f, 0f, inked = true))
    }

    @Test
    fun aFlingSendsItOnceItHasStartedToComeOut() {
        val fast = TypewriterDimens.FlingSpeed + 100f
        assertTrue(PullToSend.sends(TypewriterDimens.FlingPull + 1f, fast, inked = true))
        assertFalse(PullToSend.sends(TypewriterDimens.FlingPull - 1f, fast, inked = true))
        assertFalse(PullToSend.sends(TypewriterDimens.FlingPull + 1f, fast / 2f, inked = true))
    }

    @Test
    fun aBlankSheetIsNeverSent() {
        assertFalse(PullToSend.sends(TypewriterDimens.MostPull, 5_000f, inked = false))
    }

    @Test
    fun theRatchetClicksAtEachToothEitherWay() {
        val step = TypewriterDimens.RatchetStep
        assertEquals(0, PullToSend.ticks(1f, step - 1f))
        assertEquals(1, PullToSend.ticks(step - 1f, step + 1f))
        assertEquals(3, PullToSend.ticks(0.5f, step * 3 + 0.5f))
        assertEquals(3, PullToSend.ticks(step * 3 + 0.5f, 0.5f))
    }
}
