package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LampRigTest {
    private fun rig(): Pair<LampRig, MutableList<Unit>> {
        val clicks = mutableListOf<Unit>()
        val rig = LampRig(pivot = Offset(200f, 0f), onClick = { clicks += Unit })
        return rig to clicks
    }

    // A finger moved from where it took the bead by [by], in [steps] even moves over [seconds].
    private fun LampRig.drag(from: Offset, by: Offset, seconds: Float = 0.4f, steps: Int = 24) {
        for (i in 1..steps) {
            dragTo(from + by * (i / steps.toFloat()))
            advance(seconds / steps)
        }
    }

    @Test
    fun itStartsAtRestWithTheBeadHangingUnderTheShade() {
        val (rig, _) = rig()
        assertTrue(rig.atRest)
        assertEquals(rig.point(0).x, rig.bead.x, 1e-3f)
        assertTrue(rig.bead.y > rig.point(0).y + PullCordDimens.CordLength.value - 1f)
    }

    @Test
    fun aSecondFingerCannotTakeOverAPull() {
        val (rig, clicks) = rig()
        val at = rig.bead
        assertTrue(rig.grab(at))
        rig.drag(at, Offset(0f, PullCordDimens.ClickPull.value + 12f))
        assertEquals(1, clicks.size)
        // Another finger on the bead mid-pull is refused, so it can't arm a second click.
        assertFalse(rig.grab(rig.bead))
        rig.drag(at + Offset(0f, 60f), Offset(0f, 60f))
        assertEquals(1, clicks.size)
    }

    @Test
    fun aSidewaysSwingThatSagsAsFarDownStillOnlySwings() {
        val (rig, clicks) = rig()
        val at = rig.bead
        assertTrue(rig.grab(at))
        rig.drag(at, Offset(120f, PullCordDimens.ClickPull.value + 2f))
        assertEquals(0, clicks.size)
        rig.release()
    }

    @Test
    fun aFingerAwayFromTheBeadTakesNothing() {
        val (rig, _) = rig()
        assertFalse(rig.grab(rig.bead + Offset(80f, 0f)))
        assertFalse(rig.held)
    }

    @Test
    fun aPullPastTheClickClicksOnceAndOnlyOnce() {
        val (rig, clicks) = rig()
        val at = rig.bead
        assertTrue(rig.grab(at))
        rig.drag(at, Offset(0f, PullCordDimens.ClickPull.value - 4f))
        assertEquals(0, clicks.size, "not yet past the click")
        rig.drag(at + Offset(0f, PullCordDimens.ClickPull.value - 4f), Offset(0f, 40f))
        assertEquals(1, clicks.size)
        // Back up and down again in the same pull: the switch has already gone.
        rig.drag(at + Offset(0f, PullCordDimens.ClickPull.value + 36f), Offset(0f, -50f))
        rig.drag(at + Offset(0f, PullCordDimens.ClickPull.value - 14f), Offset(0f, 60f))
        assertEquals(1, clicks.size)
        rig.release()
        // A second pull clicks again.
        rig.advance(2f)
        val again = rig.bead
        assertTrue(rig.grab(again))
        rig.drag(again, Offset(0f, 80f))
        rig.release()
        assertEquals(2, clicks.size)
    }

    @Test
    fun theCordStretchesALittleAndHarderTheFurtherItIsPulled() {
        val (rig, _) = rig()
        val at = rig.bead
        val rest = rig.bead.y
        rig.grab(at)
        rig.drag(at, Offset(0f, 40f))
        val first = rig.bead.y - rest
        rig.drag(at + Offset(0f, 40f), Offset(0f, 40f))
        val second = rig.bead.y - rest - first
        assertTrue(first in 10f..40f, "the first 40 dp stretch it $first")
        assertTrue(second < first, "the next 40 dp stretch it $second, no less resisted")
        assertTrue(rig.bead.y - rest <= PullCordDimens.MostStretch.value + 0.5f)
    }

    @Test
    fun aSidewaysTugSwingsItWithoutClicking() {
        val (rig, clicks) = rig()
        val at = rig.bead
        rig.grab(at)
        rig.drag(at, Offset(90f, 6f))
        rig.release()
        var furthest = 0f
        repeat(120) {
            rig.advance(1f / 60f)
            furthest = maxOf(furthest, abs(rig.bead.x - rig.point(0).x))
        }
        assertEquals(0, clicks.size)
        assertTrue(furthest > 20f, "it swung only $furthest")
    }

    @Test
    fun theShadeTiltsTowardsThePullAndSwaysBackToLevel() {
        val (rig, _) = rig()
        val at = rig.bead
        rig.grab(at)
        rig.drag(at, Offset(0f, 70f))
        // The cord hangs from the shade's right-hand side: pulled down, that side dips.
        assertTrue(rig.tilt > 0.02f, "tilt ${rig.tilt}")
        assertTrue(rig.tilt < PullCordDimens.MostTilt)
        rig.release()
        var crossed = false
        repeat(120) {
            rig.advance(1f / 60f)
            if (rig.tilt < 0f) crossed = true
        }
        assertTrue(crossed, "let go, it sways past level")
        rig.advance(8f)
        assertTrue(abs(rig.tilt) < 0.005f)
    }

    @Test
    fun letGoItSpringsBackPastItsRestAndSettlesThere() {
        val (rig, _) = rig()
        val at = rig.bead
        val rest = rig.bead.y
        rig.grab(at)
        rig.drag(at, Offset(0f, 80f))
        rig.release()
        var highest = Float.MAX_VALUE
        repeat(60) {
            rig.advance(1f / 60f)
            highest = minOf(highest, rig.bead.y)
        }
        assertTrue(highest < rest - 4f, "it sprang back only to ${highest - rest} of its rest")
        rig.advance(12f)
        assertTrue(rig.atRest, "still moving")
        assertEquals(rest, rig.bead.y, 1f)
        assertEquals(rig.point(0).x, rig.bead.x, 0.5f)
    }

    @Test
    fun aRigAtRestAsksForNoMoreFrames() {
        val (rig, _) = rig()
        val at = rig.bead
        rig.grab(at)
        assertFalse(rig.atRest)
        rig.drag(at, Offset(30f, 20f))
        rig.release()
        assertFalse(rig.atRest)
        rig.advance(12f)
        assertTrue(rig.atRest)
    }

    @Test
    fun movingThePivotCarriesTheWholeLampAtRest() {
        val (rig, _) = rig()
        val bead = rig.bead
        rig.moveTo(Offset(260f, 0f))
        assertEquals(bead + Offset(60f, 0f), rig.bead)
        assertTrue(rig.atRest)
    }

    @Test
    fun aMoveMidPullStillClicksAStraightPull() {
        val (rig, clicks) = rig()
        val at = rig.bead
        assertTrue(rig.grab(at))
        // The lamp moves across under the finger, which stays put on the screen.
        rig.moveTo(rig.pivot + Offset(40f, 0f))
        rig.drag(at, Offset(0f, PullCordDimens.ClickPull.value + 10f))
        assertEquals(1, clicks.size)
    }

    @Test
    fun releaseWithoutGrabIsANoOp() {
        val (rig, _) = rig()
        val bead = rig.bead
        rig.release()
        rig.advance(1f / 60f)
        assertTrue(rig.atRest)
        assertFalse(rig.held)
        assertEquals(bead, rig.bead)
    }

    @Test
    fun aFlickAtSixtyHertzKeepsItsSpeed() {
        // The same quick flick sideways, 60 dp in a twentieth of a second, let go at full speed:
        // at 60 Hz, two steps to a frame, and at 120 Hz, one.
        fun flick(hz: Int): Float {
            val (rig, _) = rig()
            val at = rig.bead
            assertTrue(rig.grab(at))
            val frames = hz / 20
            for (i in 1..frames) {
                rig.dragTo(at + Offset(60f * i / frames, 0f))
                rig.advance(1f / hz)
            }
            rig.release()
            val before = rig.bead.x
            rig.advance(1f / 60f)
            return rig.bead.x - before
        }
        val sixty = flick(60)
        val oneTwenty = flick(120)
        assertTrue(oneTwenty > 1f, "at 120 Hz it carried on only $oneTwenty")
        assertTrue(sixty > oneTwenty * 0.6f, "at 60 Hz it carried on $sixty, at 120 Hz $oneTwenty")
    }

    @Test
    fun aLetGoStepsTheFingersLastMoveFirst() {
        val (rig, _) = rig()
        val at = rig.bead
        assertTrue(rig.grab(at))
        rig.drag(at, Offset(30f, 0f), seconds = 0.1f, steps = 6)
        // A last move, and let go before a frame has stepped it.
        rig.dragTo(at + Offset(60f, 0f))
        rig.release()
        assertFalse(rig.held)
        val before = rig.bead.x
        rig.advance(1f / 60f)
        assertTrue(
            rig.bead.x - before > 10f,
            "the last move carried it only ${rig.bead.x - before}",
        )
    }
}
