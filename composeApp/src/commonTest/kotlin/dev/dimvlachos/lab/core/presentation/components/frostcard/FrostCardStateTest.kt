package dev.dimvlachos.lab.core.presentation.components.frostcard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrostCardStateTest {
    // The card as the prototype lays it out on a 360 dp wide phone: 328 by 206.8.
    private fun card(): Pair<FrostCardState, MutableList<FrostEvent>> {
        val s = FrostCardState()
        val heard = mutableListOf<FrostEvent>()
        s.onEvent = { e, _ -> heard += e }
        s.layout(328f, 206.8f, 600f)
        return s to heard
    }

    private fun FrostCardState.run(seconds: Float) {
        var t = 0f
        while (t < seconds) {
            step(1f / 60f)
            t += 1f / 60f
        }
    }

    @Test
    fun theSwitchFreezesTheCardFromTheEdgesInAboutThreeSeconds() {
        val (s, heard) = card()
        s.toggle()
        assertTrue(s.frozen)
        assertEquals(FrostEvent.Freeze, heard.first())
        s.run(1.4f)
        assertTrue(s.f in .4f..0.6f, "halfway at ${s.f}")
        assertTrue(s.iced(2f, 2f), "the corner freezes first")
        assertFalse(FrostEvent.Frozen in heard)
        s.run(1.5f)
        assertEquals(1f, s.f)
        assertEquals(1, heard.count { it == FrostEvent.Frozen })
        assertTrue(FrostEvent.Crackle in heard, "ice forming ticks")
    }

    @Test
    fun aThumbHeldOnTheFrozenCardThawsItAndItIsUnfrozen() {
        val (s, heard) = card()
        s.toggle()
        s.run(3f)
        s.pressStart(130f, 115f)
        s.run(.5f)
        assertTrue(s.r > 20f, "a warm circle opens under the thumb: ${s.r}")
        assertFalse(s.iced(130f, 115f))
        assertTrue(s.drops > 0, "the melt beads into water")
        s.run(3f)
        assertFalse(s.frozen)
        assertEquals(0f, s.f)
        assertEquals(
            FrostEvent.Unfrozen,
            heard.last { it != FrostEvent.Drip && it != FrostEvent.Crackle },
        )
        s.pressEnd()
        s.run(4f)
        assertTrue(FrostEvent.Drip in heard, "heavier beads drip off the bottom")
    }

    @Test
    fun lettingGoEarlyLetsTheFrostCloseBackInAndTheCardStaysFrozen() {
        val (s, _) = card()
        s.toggle()
        s.run(3f)
        s.pressStart(160f, 100f)
        s.run(.4f)
        val open = s.r
        s.pressEnd()
        s.run(.5f)
        assertTrue(s.r < open, "the hole closes from its edge")
        s.run(3f)
        assertEquals(0f, s.r)
        assertTrue(s.frozen)
        assertEquals(1f, s.f)
    }

    @Test
    fun theThawFollowsTheThumb() {
        val (s, _) = card()
        s.toggle()
        s.run(3f)
        s.pressStart(100f, 100f)
        s.pressMove(200f, 120f)
        s.run(.6f)
        assertTrue(s.cx > 180f && s.cy > 110f, "at ${s.cx}, ${s.cy}")
    }

    @Test
    fun switchingOffThawsItFromTheMiddleByItself() {
        val (s, heard) = card()
        s.toggle()
        s.run(3f)
        s.toggle()
        assertFalse(s.frozen)
        assertTrue(s.auto)
        assertEquals(164f, s.cx)
        s.run(4f)
        assertEquals(0f, s.f)
        assertEquals(
            FrostEvent.Unfrozen,
            heard.last { it == FrostEvent.Unfrozen || it == FrostEvent.Frozen },
        )
    }

    @Test
    fun holdingTheWarmCardFreezesItButATapDoesNot() {
        val (s, _) = card()
        s.pressStart(100f, 100f)
        s.run(.3f)
        s.pressEnd()
        assertFalse(s.frozen)
        s.pressStart(100f, 100f)
        s.run(.5f)
        assertTrue(s.frozen)
        s.pressEnd()
    }

    @Test
    fun withReducedMotionItFreezesAndThawsAtOnce() {
        val (s, heard) = card()
        s.reducedMotion = true
        s.toggle()
        assertEquals(1f, s.f)
        s.step(1f / 60f)
        assertTrue(FrostEvent.Frozen in heard)
        s.toggle()
        assertEquals(0f, s.f)
        assertEquals(0, s.drops)
    }

    @Test
    fun theFrostGrowsTheSameEveryTimeSortedByWhenTheFrontReachesIt() {
        val a = FrostGrowth(328f, 206.8f)
        val b = FrostGrowth(328f, 206.8f)
        assertEquals(a.count, b.count)
        assertTrue(a.count > 5000, "${a.count} segments")
        assertEquals(a.x1.toList(), b.x1.toList())
        for (i in 1 until a.count) assertTrue(a.born[i] >= a.born[i - 1])
        assertTrue(a.born.last() <= 1f)
        assertTrue(a.sparks > 0)
        assertTrue(a.gen.count { it == 0 } > 100, "main spines")
    }

    @Test
    fun theFernsAreThePrototypes() {
        // The prototype's growFrost(), run in node for a 328 by 206.8 card (and a wider one): the
        // same number of segments and glints, and the same segment in the same place, born at the
        // same moment.
        val g = FrostGrowth(328f, 206.8f)
        assertEquals(17667, g.count)
        assertEquals(152, g.sparks)
        assertEquals(9.113745f, g.x1[100], 1e-3f)
        assertEquals(.10255f, g.born[100], 1e-4f)
        assertEquals(82, g.gw)
        assertEquals(52, g.gh)
        val wide = FrostGrowth(380f, 239.6f)
        assertEquals(20666, wide.count)
        assertEquals(155, wide.sparks)
    }
}
