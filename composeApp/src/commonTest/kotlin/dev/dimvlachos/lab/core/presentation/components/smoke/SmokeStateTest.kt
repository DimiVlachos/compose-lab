package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmokeStateTest {
    private var deleted = 0
    private var archived = 0
    private var ticks = 0

    private fun state(): SmokeState =
        SmokeState().apply {
            rows["a"] =
                SmokeRowState("a", present = true).apply {
                    left = SmokeAction("Delete", SmokeMaterial.Smoke) { deleted++ }
                    right = SmokeAction("Archive", SmokeMaterial.Mist) { archived++ }
                }
            onThreshold = { ticks++ }
        }

    private fun SmokeState.swipe(
        dx: Float,
        velocity: Float = 0f,
        side: SmokeSide = SmokeSide.Left,
    ) {
        assertTrue(dragStart("a", side))
        plumeOf("a")!!.width = 300f
        dragTo("a", dx, velocity)
        advance(1 / 60f)
        release("a", velocity)
    }

    private fun SmokeState.run(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            advance(1 / 60f)
            left -= 1 / 60f
        }
    }

    @Test
    fun shortOfAThirdTheSmokeIsDrawnBackIn() {
        val smoke = state()
        smoke.swipe(dx = -60f)
        assertEquals(PlumePhase.Reforming, smoke.plumeOf("a")!!.phase)
        smoke.run(1f)
        assertNull(smoke.plumeOf("a"), "the row must be whole again")
        assertEquals(0, deleted)
        assertEquals(0, ticks)
        assertEquals(1f, smoke.rows["a"]!!.height)
    }

    @Test
    fun pastAThirdItBurnsAwayCallsBackOnceAndCloses() {
        val smoke = state()
        smoke.swipe(dx = -90f)
        assertEquals(1, ticks, "a tick as the drag passes the commit point")
        smoke.run(1.5f)
        assertEquals(1, deleted)
        val plume = smoke.plumeOf("a")!!
        assertEquals(PlumePhase.Burnt, plume.phase)
        assertTrue(smoke.rows["a"]!!.gone)
        assertEquals(0f, smoke.rows["a"]!!.height)
    }

    @Test
    fun aFlingBurnsItWhateverItsDistance() {
        val smoke = state()
        smoke.swipe(dx = -20f, velocity = -3_000f)
        assertEquals(PlumePhase.Burning, smoke.plumeOf("a")!!.phase)
    }

    @Test
    fun aRightSwipeArchivesInMistAndUndoBringsItBack() {
        val smoke = state()
        smoke.swipe(dx = 200f, side = SmokeSide.Right)
        assertEquals(SmokeMaterial.Mist, smoke.plumeOf("a")!!.material)
        smoke.run(1.5f)
        assertEquals(1, archived)
        assertTrue(smoke.reform("a"))
        smoke.run(SmokeDimens.ReformDelay + SmokeDimens.ReformUndo + 0.3f)
        assertNull(smoke.plumeOf("a"))
        assertFalse(smoke.rows["a"]!!.gone)
        assertEquals(1f, smoke.rows["a"]!!.height)
    }

    @Test
    fun forgottenSmokeCantBeBreathedBack() {
        val smoke = state()
        smoke.swipe(dx = -200f)
        smoke.run(1.5f)
        smoke.forget("a")
        assertFalse(smoke.reform("a"))
    }

    @Test
    fun theFrameLoopSleepsOnceTheSmokeHasGone() {
        val smoke = state()
        smoke.swipe(dx = -200f)
        smoke.run(SmokeMotion.longestLife + 1f)
        assertFalse(smoke.awake)
    }

    @Test
    fun aRowHeldOrWaitingCantBeQueuedOrTakenTwice() {
        val smoke = state()
        assertTrue(smoke.dragStart("a", SmokeSide.Left))
        assertFalse(smoke.swipeAway("a", SmokeSide.Left, delay = 0.1f), "held: no burn queued")
        assertFalse(smoke.swipeAway("a", SmokeSide.Left), "held: no burn now")
        smoke.release("a", 0f)
        smoke.run(1f)
        assertTrue(smoke.swipeAway("a", SmokeSide.Left, delay = 0.2f))
        assertTrue(smoke.isWaiting("a"))
        assertFalse(smoke.dragStart("a", SmokeSide.Left), "waiting its turn in a wave")
        assertFalse(smoke.swipeAway("a", SmokeSide.Left, delay = 0.3f), "queued twice")
    }

    @Test
    fun burningARowNowTakesThePlaceOfItsQueuedBurn() {
        val smoke = state()
        assertTrue(smoke.swipeAway("a", SmokeSide.Left, delay = 0.2f))
        assertTrue(smoke.swipeAway("a", SmokeSide.Left))
        assertFalse(smoke.isWaiting("a"))
        smoke.plumeOf("a")!!.width = 300f
        smoke.run(2f)
        assertEquals(1, deleted, "the row burns once")
    }

    @Test
    fun aQueuedBurnFindsTheRowGoneAndLetsItBe() {
        val smoke = state()
        assertTrue(smoke.swipeAway("a", SmokeSide.Left, delay = 0.2f))
        smoke.rows["a"]!!.present = false
        smoke.run(0.5f)
        assertNull(smoke.plumeOf("a"))
        assertEquals(0, deleted)
    }

    @Test
    fun forgottenSmokeStillInTheAirDriftsOnTillItFades() {
        val smoke = state()
        smoke.swipe(dx = -200f)
        smoke.run(1f)
        smoke.forget("a")
        assertNull(smoke.plumeOf("a"))
        smoke.advance(1 / 60f)
        assertTrue(smoke.drawn.isNotEmpty(), "the smoke must not pop")
        smoke.run(SmokeMotion.longestLife)
        assertTrue(smoke.drawn.isEmpty())
        assertFalse(smoke.awake)
    }
}
