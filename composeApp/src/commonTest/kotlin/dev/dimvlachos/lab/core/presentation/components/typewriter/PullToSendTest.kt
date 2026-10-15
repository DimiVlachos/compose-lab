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

    @Test
    fun thePaperMovesAsFastAsTheShownPullChanges() {
        for (inked in listOf(true, false)) {
            for (pull in listOf(1f, 20f, 80f, 200f)) {
                val h = 0.01f
                val slope =
                    (PullToSend.shown(pull + h, inked) - PullToSend.shown(pull - h, inked)) /
                        (2 * h)
                val speed = PullToSend.paperSpeed(pull, 300f, inked)
                kotlin.test.assertEquals(slope * 300f, speed, 0.5f, "at $pull, inked $inked")
            }
        }
        // At first the paper follows the finger one to one.
        kotlin.test.assertEquals(300f, PullToSend.paperSpeed(0f, 300f, inked = true), 0.01f)
    }

    @Test
    fun aFlightSetsOffAsFastAsTheSheetCameFreeAndLandsOnItsPlace() {
        val from = 900f
        val to = 300f
        val speed = -1_800f
        kotlin.test.assertEquals(from, flightAxis(from, to, speed, 0f), 0.01f)
        val h = 0.0005f
        val start = (flightAxis(from, to, speed, h) - flightAxis(from, to, speed, 0f)) / h
        kotlin.test.assertEquals(speed, start, kotlin.math.abs(speed) * 0.03f)
        kotlin.test.assertEquals(
            to,
            flightAxis(from, to, speed, TypewriterDimens.FlightSeconds),
            0.01f,
        )
        // Never flung far past its place.
        var t = 0f
        while (t <= TypewriterDimens.FlightSeconds) {
            val y = flightAxis(from, to, speed, t)
            kotlin.test.assertTrue(y > to - 160f && y < from + 1f, "$y at $t")
            t += 0.01f
        }
    }
}
