package dev.dimvlachos.lab.core.presentation.components.birdcage

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The prototype's #selftest, ported: the same drags, in the same steps, with the same checks.
class BirdcageStateTest {
    private val s = BirdcageState()
    private val events = mutableListOf<BirdcageEvent>()
    private var now = 0f

    // The prototype's phone hero: the box drawn 1.37 times its units, a 6 px slop.
    private val px = 1.37f
    private val slop = 6f
    private val range = (HemDown - HemUp) * px

    init {
        s.onEvent = { events += it }
    }

    private fun run(ms: Float) {
        var left = ms
        while (left > 0f) {
            val h = min(left, 1000f / 60f)
            s.step(h / 1000f)
            now += h
            left -= h
        }
    }

    private fun min(a: Float, b: Float) = if (a < b) a else b

    /** A drag of [dy] px in [n] even moves over [ms], from the hem, as the self-test's `dragBy`. */
    private fun dragBy(dy: Float, n: Int, ms: Float) {
        val y = (s.hemY + 2f) * px
        s.down(y, now)
        for (i in 1..n) {
            run(ms / n)
            s.move(y + dy * i / n, now, px, slop)
        }
        run(20f)
        s.up(now)
    }

    @Test
    fun startsUnmutedWithTheCoverUp() {
        run(600f)
        assertFalse(s.muted)
        assertTrue(s.cover < .05f)
    }

    @Test
    fun aMessageMakesTheCanaryChirp() {
        assertTrue(s.deliver())
        assertEquals(1, s.chirps)
        assertEquals(0, s.silenced)
        assertEquals(listOf(BirdcageEvent.Chirp), events)
    }

    @Test
    fun aShortPullRollsBackUp() {
        dragBy(range * .2f, 8, 400f)
        run(900f)
        assertFalse(s.muted)
        assertTrue(s.cover < .08f, "cover ${s.cover}")
        assertEquals(BirdcageEvent.Swish, events.first())
    }

    @Test
    fun aLongPullDropsTheCoverMutesAndTheCanarySleeps() {
        dragBy(range * .75f, 12, 500f)
        run(1400f)
        assertTrue(s.muted)
        assertTrue(abs(s.cover - 1f) < .06f, "cover ${s.cover}")
        assertTrue(s.sleep > .8f, "sleep ${s.sleep}")
        assertEquals(listOf(BirdcageEvent.Swish, BirdcageEvent.Settle), events)
    }

    @Test
    fun mutedMessagesAreSilentAndCounted() {
        dragBy(range * .75f, 12, 500f)
        run(1400f)
        events.clear()
        assertFalse(s.deliver())
        assertFalse(s.deliver())
        assertEquals(0, s.chirps)
        assertEquals(2, s.silenced)
        assertEquals(listOf(BirdcageEvent.Rustle, BirdcageEvent.Rustle), events)
    }

    @Test
    fun aSmallPushUpFallsBackDownAndAFullOneUnmutesAndWakes() {
        dragBy(range * .75f, 12, 500f)
        run(1400f)
        s.deliver()
        s.deliver()
        dragBy(-range * .15f, 6, 300f)
        run(900f)
        assertTrue(s.muted)
        assertTrue(s.cover > .9f, "cover ${s.cover}")
        dragBy(-range * .7f, 10, 400f)
        run(1300f)
        assertFalse(s.muted)
        assertTrue(s.cover < .06f, "cover ${s.cover}")
        assertEquals(0, s.silenced)
        assertTrue(s.sleep < .2f, "sleep ${s.sleep}")
        assertTrue(s.shakeAt >= 0f)
    }

    @Test
    fun aSingleBigMoveKeepsItsDistancePastTheSlop() {
        val y = (s.hemY + 2f) * px
        s.down(y, now)
        run(30f)
        s.move(y + range * .7f, now, px, slop)
        run(400f)
        assertTrue(s.cover > .6f, "cover ${s.cover}")
        s.up(now)
        run(1300f)
        assertTrue(s.muted)
    }

    @Test
    fun aTapTogglesAndATapAgainTogglesBack() {
        s.down(100f, now)
        run(40f)
        s.up(now)
        run(1300f)
        assertTrue(s.muted)
        assertTrue(s.cover > .9f)
        s.down(100f, now)
        run(40f)
        s.up(now)
        run(1300f)
        assertFalse(s.muted)
        assertTrue(s.cover < .06f)
    }

    @Test
    fun aTouchTakenAwayBeforeItMovesIsNotATap() {
        s.down(100f, now)
        run(40f)
        s.cancel(now)
        run(1300f)
        assertFalse(s.muted)
        assertTrue(events.isEmpty())
    }

    @Test
    fun aSecondFingerTakesTheHemOver() {
        val first = s.down(100f, now)
        assertTrue(s.holds(first))
        val second = s.down(300f, now)
        assertFalse(s.holds(first))
        assertTrue(s.holds(second))
        s.up(now)
        assertFalse(s.holds(second))
    }

    @Test
    fun aFlickPastAQuarterStillDrops() {
        // 30% down in 80 ms is fast: projected on by its speed, it lands past halfway.
        dragBy(range * .3f, 4, 80f)
        run(1300f)
        assertTrue(s.muted)
    }

    @Test
    fun reducedMotionSettlesAtOnce() {
        s.reducedMotion = true
        s.toggle()
        s.step(1f / 60f)
        assertEquals(1f, s.cover)
        assertEquals(1f, s.sleep)
        assertEquals(0f, s.sway)
        s.deliver()
        s.step(1f / 60f)
        assertEquals(0f, s.sway)
    }

    @Test
    fun resetPutsItBackQuietly() {
        s.toggle()
        run(1300f)
        s.deliver()
        events.clear()
        s.reset()
        assertFalse(s.muted)
        assertEquals(0f, s.cover)
        assertEquals(0, s.silenced)
        assertEquals(-1f, s.shakeAt)
        assertTrue(events.isEmpty())
    }

    @Test
    fun theClothCrownClosesToAPoint() {
        assertTrue(halfW(ClothY0) < .01f)
    }

    @Test
    fun theHemEndsStayOnTheClothSidesAndSwingWithThem() {
        val hy = HemDown
        val l0 = clothSide(hy, -1, 0f, 0f)
        val l1 = clothSide(hy, -1, 2f, 0f)
        assertTrue(abs(hemX(hy, 0f, 0f, 2f, 0f) - l1) < .01f)
        assertTrue(abs(hemX(hy, 1f, 0f, 2f, 0f) - clothSide(hy, 1, 2f, 0f)) < .01f)
        assertTrue(l1 > l0)
        assertTrue(hemX(hy, .5f, 0f, 2f, 0f) > CX)
    }

    @Test
    fun theCageSitsOnTheBottomOfTheHero() {
        val box = BirdcageBox(412f, 520f)
        assertTrue(abs(box.top + BoxH * box.scale - 520f) < .01f)
        assertEquals(412f / BoxW, box.scale)
    }
}
