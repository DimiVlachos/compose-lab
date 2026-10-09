package dev.dimvlachos.lab.core.presentation.components.fishing

import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Biting
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Casting
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Idle
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Reeling
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Rising
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Snapped
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingPhase.Waiting
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FishingRigTest {
    private class Heard {
        var thresholds = 0
        var splashes = 0
        var bites = 0
        var snaps = 0
        val landed = mutableListOf<FishingOutcome>()
    }

    private fun rig(heard: Heard = Heard()) =
        FishingRig(width = 360f).apply {
            onThreshold = { heard.thresholds++ }
            onSplash = { heard.splashes++ }
            onBite = { heard.bites++ }
            onSnap = { heard.snaps++ }
            onLanded = { heard.landed += it }
        }

    // Frames of a 60 Hz screen: the rig steps itself at its own fixed rate inside them.
    private fun FishingRig.run(seconds: Float) {
        repeat((seconds * 60f).toInt()) { advance(1f / 60f) }
    }

    // Runs until [phase] is left, or [most] seconds, and says how long it took.
    private fun FishingRig.runWhile(phase: FishingPhase, most: Float = 20f): Float {
        var t = 0f
        while (this.phase == phase && t < most) {
            advance(1f / 60f)
            t += 1f / 60f
        }
        return t
    }

    // A pull past the threshold and let go: the cast starts as the refresh is asked for.
    private fun FishingRig.pullAndCast() {
        pullTo(0.5f)
        pullTo(1.2f)
        cast()
        pullTo(1f)
    }

    // Run on until the band has closed after an outcome: the caller's pull state falls to 0.
    private fun FishingRig.closeAndSettle(seconds: Float = 8f) {
        repeat((seconds * 60f).toInt()) {
            if (!busy) pullTo(0f)
            advance(1f / 60f)
        }
    }

    @Test
    fun crossingTheThresholdTicksOncePerPull() {
        val heard = Heard()
        val rig = rig(heard)
        rig.pullTo(0.4f)
        rig.pullTo(1.2f)
        rig.pullTo(0.8f)
        rig.pullTo(1.3f)
        assertEquals(1, heard.thresholds)
        rig.pullTo(0f)
        rig.pullTo(1.1f)
        assertEquals(2, heard.thresholds, "a new pull ticks again")
    }

    @Test
    fun theFurtherThePullTheMoreTheRodBends() {
        val rig = rig()
        rig.pullTo(0.3f)
        rig.run(0.1f)
        val light = rig.tip.y
        rig.pullTo(1f)
        rig.run(0.1f)
        val loaded = rig.tip.y
        rig.pullTo(1.8f)
        rig.run(0.1f)
        assertTrue(loaded > light, "the tip must dip as the pull grows: $light, $loaded")
        assertTrue(rig.tip.y > loaded)
    }

    @Test
    fun aCastLandsTheBobberOnTheWaterWithASplash() {
        val heard = Heard()
        val rig = rig(heard)
        rig.pullAndCast()
        assertEquals(Casting, rig.phase)
        val flight = rig.runWhile(Casting)
        assertEquals(FishingDimens.CastSeconds, flight, 0.05f)
        assertEquals(Waiting, rig.phase)
        assertEquals(1, heard.splashes)
        assertEquals(FishingDimens.WaterLevel.value, rig.bobber.y, 4f)
        assertTrue(rig.bobber.x > rig.tip.x, "it lands out beyond the rod's tip")
        assertFalse(rig.water.still)
    }

    @Test
    fun anOutcomeDuringTheCastWaitsForTheBobberAndTheMinimumWait() {
        val heard = Heard()
        val rig = rig(heard)
        rig.pullAndCast()
        rig.land(FishingOutcome.Caught(2))
        rig.runWhile(Casting)
        val floated = rig.runWhile(Waiting)
        assertEquals(FishingDimens.MinWaitSeconds, floated, 0.05f)
        assertEquals(Biting, rig.phase)
        assertEquals(1, heard.bites)
    }

    @Test
    fun aLongWaitKeepsBobbingAndNeverSettles() {
        val rig = rig()
        rig.pullAndCast()
        rig.runWhile(Casting)
        var highest = Float.MAX_VALUE
        var lowest = -Float.MAX_VALUE
        repeat(600) {
            rig.advance(1f / 60f)
            highest = minOf(highest, rig.bobber.y)
            lowest = maxOf(lowest, rig.bobber.y)
        }
        assertEquals(Waiting, rig.phase)
        assertFalse(rig.atRest)
        assertTrue(lowest - highest > 1f, "it must bob, it moved ${lowest - highest} dp")
        val level = FishingDimens.WaterLevel.value
        assertTrue(highest > level - 8f && lowest < level + 8f, "it floats: $highest..$lowest")
    }

    @Test
    fun aCatchBitesReelsInAndRises() {
        val heard = Heard()
        val rig = rig(heard)
        rig.pullAndCast()
        rig.runWhile(Casting)
        rig.land(FishingOutcome.Caught(2))
        assertEquals(2, rig.catchCount)
        assertEquals(0f, rig.riseFor(0), "a catch stays under water until it rises")
        rig.runWhile(Waiting)
        val phases = mutableListOf(rig.phase)
        while (rig.phase != Idle) {
            rig.advance(1f / 60f)
            if (rig.phase != phases.last()) phases += rig.phase
        }
        assertEquals(listOf(Biting, Reeling, Rising, Idle), phases)
        assertEquals(1, heard.bites)
        assertEquals(listOf<FishingOutcome>(FishingOutcome.Caught(2)), heard.landed)
        assertEquals(1f, rig.riseFor(0))
        assertEquals(1f, rig.riseFor(1))
        assertEquals(1f, rig.riseFor(5), "only the catch rises")
    }

    @Test
    fun aCatchsCardsRiseOneAfterAnother() {
        val rig = rig()
        rig.floatAtOnce()
        rig.land(FishingOutcome.Caught(2))
        rig.runWhile(Waiting)
        rig.runWhile(Biting)
        rig.runWhile(Reeling)
        assertEquals(Rising, rig.phase)
        rig.run(FishingDimens.RiseSeconds / 2f)
        assertTrue(rig.riseFor(0) > rig.riseFor(1), "the first card leads")
        assertTrue(rig.riseFor(1) in 0f..1f)
    }

    @Test
    fun nothingNewReelsInAnEmptyHook() {
        val heard = Heard()
        val rig = rig(heard)
        rig.floatAtOnce()
        rig.land(FishingOutcome.NothingNew)
        rig.runWhile(Waiting)
        assertEquals(Reeling, rig.phase)
        assertEquals(0, heard.bites)
        rig.runWhile(Reeling)
        assertEquals(Idle, rig.phase)
        assertEquals(listOf<FishingOutcome>(FishingOutcome.NothingNew), heard.landed)
        assertEquals(1f, rig.riseFor(0), "nothing rises")
        assertTrue(abs(rig.bobber.y - rig.tip.y) < FishingDimens.DangleLength.value * 1.5f)
    }

    @Test
    fun aFailureSnapsTheLineAndTheBobberDriftsOff() {
        val heard = Heard()
        val rig = rig(heard)
        rig.floatAtOnce()
        val floating = rig.bobber
        val cause = IllegalStateException("Network unreachable")
        rig.land(FishingOutcome.Failed(cause))
        rig.runWhile(Waiting)
        assertEquals(Snapped, rig.phase)
        assertEquals(1, heard.snaps)
        rig.run(FishingDimens.DriftSeconds * 0.9f)
        assertTrue(rig.bobber.x > floating.x + 20f, "it drifts off")
        rig.runWhile(Snapped)
        assertEquals(0f, rig.bobberAlpha)
        val end = rig.line[rig.line.size - 1]
        assertTrue(end.y > rig.tip.y, "the slack falls below the rod's tip")
        assertEquals(listOf<FishingOutcome>(FishingOutcome.Failed(cause)), heard.landed)
    }

    @Test
    fun aRetryAfterAFailureCastsAgain() {
        val heard = Heard()
        val rig = rig(heard)
        rig.floatAtOnce()
        rig.land(FishingOutcome.Failed(IllegalStateException()))
        rig.closeAndSettle()
        assertEquals(Idle, rig.phase)
        rig.pullAndCast()
        rig.runWhile(Casting)
        assertEquals(Waiting, rig.phase)
        assertEquals(1f, rig.bobberAlpha, "a new bobber on the new line")
        assertEquals(1, heard.splashes)
    }

    @Test
    fun twoNothingNewInARowBothReelIn() {
        val heard = Heard()
        val rig = rig(heard)
        repeat(2) {
            rig.pullAndCast()
            rig.land(FishingOutcome.NothingNew)
            rig.closeAndSettle()
        }
        assertEquals(
            listOf<FishingOutcome>(FishingOutcome.NothingNew, FishingOutcome.NothingNew),
            heard.landed,
        )
        assertEquals(2, heard.splashes)
    }

    @Test
    fun aCancelledRefreshReelsInWithoutAnOutcome() {
        val heard = Heard()
        val rig = rig(heard)
        rig.floatAtOnce()
        rig.cancel()
        assertEquals(Reeling, rig.phase)
        rig.closeAndSettle()
        assertEquals(Idle, rig.phase)
        assertTrue(heard.landed.isEmpty())
        assertEquals(0, heard.bites + heard.snaps)
    }

    @Test
    fun everyOutcomeSettlesToRest() {
        val outcomes =
            listOf(
                FishingOutcome.Caught(3),
                FishingOutcome.NothingNew,
                FishingOutcome.Failed(IllegalStateException()),
            )
        for (outcome in outcomes) {
            val rig = rig()
            rig.pullAndCast()
            rig.land(outcome)
            rig.closeAndSettle()
            assertTrue(rig.atRest, "$outcome left the rig moving")
            assertFalse(rig.busy)
        }
    }

    @Test
    fun aStateMadeMidRefreshFloatsAtOnce() {
        val heard = Heard()
        val rig = rig(heard)
        rig.floatAtOnce()
        assertEquals(Waiting, rig.phase)
        assertTrue(rig.busy)
        assertEquals(0, heard.splashes, "no cast, no splash")
    }

    @Test
    fun twoRigsGivenTheSameEventsMatch() {
        val a = rig()
        val b = rig()
        for (r in listOf(a, b)) {
            r.pullAndCast()
            r.run(1.3f)
            r.land(FishingOutcome.Caught(1))
            r.run(0.9f)
        }
        assertEquals(a.phase, b.phase)
        assertEquals(a.bobber, b.bobber)
        assertEquals(a.line[5], b.line[5])
    }
}
