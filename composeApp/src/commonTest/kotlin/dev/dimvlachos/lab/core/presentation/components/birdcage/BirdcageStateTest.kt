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
        assertTrue(s.wakeAt >= 0f)
        assertTrue(s.lid < .05f, "lid ${s.lid}")
    }

    @Test
    fun theCoverNeverPassesItsEndAfterRelease() {
        // A fast flick down: projected on, it eases into place without bouncing past the bottom.
        dragBy(range * .75f, 4, 80f)
        var top = 0f
        repeat(120) {
            run(1000f / 60f)
            top = maxOf(top, s.cover)
        }
        assertTrue(s.muted)
        assertTrue(top <= 1f, "cover peaked at $top")
        assertTrue(s.peak <= 1f, "peak ${s.peak}")
        // And a fast push up never passes the top.
        dragBy(-range * .75f, 4, 80f)
        var low = 1f
        repeat(120) {
            run(1000f / 60f)
            low = minOf(low, s.cover)
        }
        assertFalse(s.muted)
        assertTrue(low >= 0f, "cover dipped to $low")
    }

    @Test
    fun wakingTheEyeOpensThenTheHeadRisesSmoothlyNeverPastUpright() {
        dragBy(range * .75f, 12, 500f)
        run(2000f)
        assertTrue(s.lid > .9f, "asleep with the eye shut: lid ${s.lid}")
        dragBy(-range * .7f, 10, 400f)
        var t = 0f
        var eyeAt = -1f
        var headAt = -1f
        var lowest = 1f
        var prev = 2f
        var headBack = false
        while (t < 1300f) {
            run(8f)
            t += 8f
            if (eyeAt < 0f && s.lid < .5f) eyeAt = t
            if (headAt < 0f && s.sleep < .8f) headAt = t
            if (s.wakeAt >= 0f && s.sleep > prev + 1e-6f) headBack = true
            prev = s.sleep
            lowest = minOf(lowest, s.sleep)
        }
        assertTrue(eyeAt >= 0f && headAt >= 0f && eyeAt <= headAt, "eye $eyeAt, head $headAt")
        // No overshoot and no pumping: the head only ever rises, and stops upright.
        assertTrue(lowest >= -1e-6f, "lowest $lowest")
        assertFalse(headBack, "the head went back down while waking")
        assertTrue(abs(s.sleep) < .02f, "sleep ${s.sleep}")
    }

    @Test
    fun aDroppedCoverFallsFasterAndFasterThenLandsSoftlyAndRipples() {
        s.toggle()
        val cs = mutableListOf<Float>()
        val vs = mutableListOf<Float>()
        var maxRipple = 0f
        repeat(140) {
            run(8f)
            cs += s.cover
            vs += s.coverV
            maxRipple = maxOf(maxRipple, s.ripple)
        }
        val early = cs.indexOfFirst { it > .1f }
        val mid = cs.indexOfFirst { it > .5f }
        assertTrue(vs[mid] > vs[early] * 1.5f, "speed at a tenth ${vs[early]}, at half ${vs[mid]}")
        // Slow in its last 3%: it brakes onto the base, never stopping dead or passing it.
        val last = cs.indices.filter { cs[it] > .97f && cs[it] < 1f }
        assertTrue(
            last.isNotEmpty() && last.all { vs[it] < 1.6f },
            "landing speeds ${last.map { vs[it] }}",
        )
        assertTrue(cs.all { it <= 1f })
        assertEquals(1f, s.cover)
        assertTrue(
            maxRipple > .1f && abs(s.ripple) < .05f,
            "ripple peaked $maxRipple, now ${s.ripple}",
        )
    }

    @Test
    fun aLiftedCoverEasesIntoTheTopAndGathersAtTheCrown() {
        s.toggle()
        run(1300f)
        s.toggle()
        val cs = mutableListOf<Float>()
        val vs = mutableListOf<Float>()
        var maxGather = 0f
        repeat(160) {
            run(8f)
            cs += s.cover
            vs += s.coverV
            maxGather = maxOf(maxGather, s.gather)
        }
        val last = cs.indices.filter { cs[it] < .05f && cs[it] > 0f }
        assertTrue(
            last.isNotEmpty() && last.all { -vs[it] < 1.1f },
            "arrival speeds ${last.map { vs[it] }}",
        )
        assertTrue(cs.all { it >= 0f })
        assertEquals(0f, s.cover)
        assertTrue(
            maxGather > .1f && s.gather < maxGather * .5f,
            "gather peaked $maxGather, now ${s.gather}",
        )
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
        assertEquals(1f, s.lid)
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
        assertEquals(-1f, s.wakeAt)
        assertEquals(0f, s.lid)
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
