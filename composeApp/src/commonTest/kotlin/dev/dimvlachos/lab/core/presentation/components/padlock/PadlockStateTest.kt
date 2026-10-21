package dev.dimvlachos.lab.core.presentation.components.padlock

import kotlin.math.PI
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PadlockStateTest {
    private val events = mutableListOf<PadlockEvent>()

    private fun lock() = PadlockState(Random(1)).also { s -> s.onEvent = { e, _ -> events += e } }

    private fun PadlockState.run(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            step(1f / 60f)
            left -= 1f / 60f
        }
    }

    @Test
    fun aWeakPasswordLeavesTheShackleUpAndSwungOpenPastTheBody() {
        val s = lock()
        s.onText(StrengthLevel.Weak)
        s.run(1f)
        assertTrue(s.lift > 25f, "lift ${s.lift}")
        assertTrue(s.swing > 2.6f, "swing ${s.swing}")
        assertFalse(s.locked)
    }

    @Test
    fun eachWeakKeystrokeRattles() {
        val s = lock()
        s.onText(StrengthLevel.Weak)
        s.onText(StrengthLevel.Weak)
        assertEquals(2, events.count { it == PadlockEvent.Rattle })
    }

    @Test
    fun strongSwingsIntoLineThenDropsHomeWithOneClunk() {
        val s = lock()
        s.onText(StrengthLevel.Good)
        s.run(.6f)
        s.onText(StrengthLevel.Strong)
        assertTrue(s.falling)
        s.run(1.2f)
        assertTrue(s.locked)
        assertEquals(0f, s.lift)
        assertEquals(0f, s.swing)
        assertEquals(1, s.clunks)
        assertEquals(1, events.count { it == PadlockEvent.Clunk })
        assertTrue(s.steel > .9f)
        assertTrue(s.glintAt >= 0f)
    }

    @Test
    fun droppingBelowStrongSpringsItBackOpenWithAPop() {
        val s = lock()
        s.onText(StrengthLevel.Strong)
        s.run(1.2f)
        s.onText(StrengthLevel.Good)
        assertFalse(s.locked)
        assertTrue(PadlockEvent.Pop in events)
        s.run(.9f)
        assertTrue(s.lift > 6f, "lift ${s.lift}")
    }

    @Test
    fun aLevelChangeThatNeitherLocksNorPopsIsANotch() {
        val s = lock()
        s.onText(StrengthLevel.Weak)
        s.onText(StrengthLevel.Fair)
        s.onText(StrengthLevel.Good)
        s.onText(StrengthLevel.Good)
        assertEquals(2, events.count { it == PadlockEvent.Notch })
    }

    @Test
    fun peekSwingsTheCoverOpenAndBack() {
        val s = lock()
        s.showPassword(true)
        s.run(.7f)
        assertTrue(s.cover > .8f)
        s.showPassword(false)
        s.run(.7f)
        assertTrue(s.cover < .2f)
        assertEquals(2, events.count { it == PadlockEvent.Swing })
    }

    @Test
    fun reducedMotionSettlesAtOnce() {
        val s = lock()
        s.reducedMotion = true
        s.onText(StrengthLevel.Strong)
        assertTrue(s.locked)
        assertEquals(0f, s.lift)
        s.onText(StrengthLevel.Weak)
        assertEquals(PadlockState.Lift[1], s.lift)
        assertEquals(PI.toFloat(), s.swing)
        assertTrue(PadlockEvent.Rattle !in events)
    }

    @Test
    fun resetPutsItBackOpenAndEmpty() {
        val s = lock()
        s.onText(StrengthLevel.Strong)
        s.showPassword(true)
        s.run(1.2f)
        s.reset()
        assertEquals(StrengthLevel.Empty, s.level)
        assertFalse(s.locked)
        assertFalse(s.peek)
        assertEquals(PadlockState.Lift[0], s.lift)
        assertEquals(0f, s.cover)
    }

    @Test
    fun itSettlesAtRestSoTheFrameLoopCanSleep() {
        val s = lock()
        assertTrue(s.settled)
        val wake = s.wake
        s.onText(StrengthLevel.Strong)
        assertTrue(s.wake > wake)
        assertFalse(s.settled)
        s.run(3f)
        assertTrue(s.settled, "lift ${s.lift} steel ${s.steel} bounce ${s.bounce}")
        s.onText(StrengthLevel.Weak)
        s.run(.2f)
        assertFalse(s.settled)
        s.run(6f)
        assertTrue(s.settled, "rattle ${s.rattle} lift ${s.lift} swing ${s.swing}")
    }
}
