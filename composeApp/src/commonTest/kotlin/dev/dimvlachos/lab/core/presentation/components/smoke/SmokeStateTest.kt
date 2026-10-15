package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmokeStateTest {
    private var deleted = 0
    private var archived = 0
    private var ticks = 0

    private fun state(): SmokeState =
        SmokeState().apply {
            register(
                SmokeRowState("a", present = true).apply {
                    left = SmokeAction("Delete", SmokeMaterial.Smoke) { deleted++ }
                    right = SmokeAction("Archive", SmokeMaterial.Mist) { archived++ }
                }
            )
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
        var dropped = 0
        assertTrue(smoke.swipeAway("a", SmokeSide.Left, delay = 0.2f) { dropped++ })
        smoke.rows["a"]!!.present = false
        smoke.run(0.5f)
        assertNull(smoke.plumeOf("a"))
        assertEquals(0, deleted)
        assertEquals(1, dropped, "the caller hears that its burn was dropped")
    }

    @Test
    fun aQueuedBurnThatStartsIsntDropped() {
        val smoke = state()
        var dropped = 0
        assertTrue(smoke.swipeAway("a", SmokeSide.Left, delay = 0.2f) { dropped++ })
        smoke.run(0.3f)
        assertNotNull(smoke.plumeOf("a"), "its turn came and it burns")
        assertEquals(0, dropped)
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

    @Test
    fun smokeStillInTheAirStaysThereWhileUndoWaitsForTheRow() {
        val smoke = state()
        smoke.swipe(dx = -200f)
        smoke.run(1.2f)
        val plume = smoke.plumeOf("a")!!
        assertEquals(PlumePhase.Burnt, plume.phase)
        assertTrue(plume in smoke.drawn)
        assertTrue(smoke.reform("a"))
        // Every frame until it is breathed back in, the smoke in the air is drawn.
        while (plume.phase == PlumePhase.Waiting) {
            smoke.advance(1 / 60f)
            assertTrue(plume in smoke.drawn, "the smoke vanished while undo waited")
        }
        assertEquals(PlumePhase.Reforming, plume.phase)
    }

    @Test
    fun aHeldRowTakenAwayLetsGoOfTheFingerAndIsDrawnBackIn() {
        val smoke = state()
        assertTrue(smoke.dragStart("a", SmokeSide.Left))
        smoke.plumeOf("a")!!.width = 300f
        smoke.dragTo("a", -200f, 0f)
        smoke.advance(1 / 60f)
        smoke.rows["a"]!!.present = false
        smoke.advance(1 / 60f)
        assertEquals(PlumePhase.Reforming, smoke.plumeOf("a")!!.phase)
        assertFalse(smoke.rows["a"]!!.dragging)
        // The finger's drag and lift that follow do nothing.
        smoke.dragTo("a", -300f, 0f)
        smoke.release("a", -3_000f)
        smoke.run(1.5f)
        assertEquals(0, deleted)
    }

    @Test
    fun smokeLetGoStillCountsAgainstTheBudget() {
        val smoke = state()
        val keys = (1..5).map { "row$it" }
        for (key in keys) {
            smoke.register(
                SmokeRowState(key, present = true).apply {
                    left = SmokeAction("Delete", SmokeMaterial.Smoke) {}
                }
            )
        }
        assertEquals(SmokeDimens.PerRow, smoke.budget())
        for (key in keys) assertTrue(smoke.swipeAway(key, SmokeSide.Left))
        smoke.run(1f)
        assertEquals(SmokeDimens.LeastPerRow, smoke.budget(), "five rows in the air thin the next")
        keys.forEach(smoke::forget)
        assertEquals(SmokeDimens.LeastPerRow, smoke.budget(), "let go, they are still in the air")
        smoke.run(SmokeMotion.longestLife)
        assertEquals(SmokeDimens.PerRow, smoke.budget())
    }

    @Test
    fun aRowThatIsAllSmokeStopsDrawingAgain() {
        val smoke = state()
        val row = smoke.rows["a"]!!
        smoke.swipe(dx = -200f)
        smoke.run(1f)
        assertTrue(row.gone)
        val tick = row.tick
        smoke.run(0.5f)
        assertEquals(tick, row.tick, "a closed row isn't drawn again each frame")
    }

    @Test
    fun clearBringsEveryRowBackAndTheNextBurnDrawsAgain() {
        val smoke = state()
        smoke.swipe(dx = -200f)
        smoke.run(0.5f)
        smoke.clear()
        assertNull(smoke.plumeOf("a"))
        assertTrue(smoke.drawn.isEmpty())
        assertFalse(smoke.rows["a"]!!.gone)
        assertTrue(smoke.swipeAway("a", SmokeSide.Left))
        smoke.advance(1 / 60f)
        assertTrue(smoke.drawn.isNotEmpty(), "the frame loop fills and draws again")
    }
}
