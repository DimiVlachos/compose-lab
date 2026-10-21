package dev.dimvlachos.lab.core.presentation.components.smoke

import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundLoop
import dev.dimvlachos.lab.core.platform.SoundPlayer
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
                    start = SmokeAction("Delete", SmokeMaterial.Smoke) { deleted++ }
                    end = SmokeAction("Archive", SmokeMaterial.Mist) { archived++ }
                },
            )
            onThreshold = { ticks++ }
        }

    private fun SmokeState.swipe(
        dx: Float,
        velocity: Float = 0f,
        side: SmokeSide = SmokeSide.Start,
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
        assertEquals(PlumePhase.Returning, smoke.plumeOf("a")!!.phase)
        smoke.run(1f)
        assertNull(smoke.plumeOf("a"), "the row must be whole again")
        assertEquals(0, deleted)
        assertEquals(0, ticks)
        assertEquals(1f, smoke.rows["a"]!!.height)
    }

    @Test
    fun pastAThirdItBurnsAwayCallsBackOnceAndCloses() {
        val smoke = state()
        smoke.swipe(dx = -120f)
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
        smoke.swipe(dx = 200f, side = SmokeSide.End)
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
        assertTrue(smoke.dragStart("a", SmokeSide.Start))
        assertFalse(smoke.swipeAway("a", SmokeSide.Start, delay = 0.1f), "held: no burn queued")
        assertFalse(smoke.swipeAway("a", SmokeSide.Start), "held: no burn now")
        smoke.release("a", 0f)
        smoke.run(1f)
        assertTrue(smoke.swipeAway("a", SmokeSide.Start, delay = 0.2f))
        assertTrue(smoke.isWaiting("a"))
        assertFalse(smoke.dragStart("a", SmokeSide.Start), "waiting its turn in a wave")
        assertFalse(smoke.swipeAway("a", SmokeSide.Start, delay = 0.3f), "queued twice")
    }

    @Test
    fun burningARowNowTakesThePlaceOfItsQueuedBurn() {
        val smoke = state()
        assertTrue(smoke.swipeAway("a", SmokeSide.Start, delay = 0.2f))
        assertTrue(smoke.swipeAway("a", SmokeSide.Start))
        assertFalse(smoke.isWaiting("a"))
        smoke.plumeOf("a")!!.width = 300f
        smoke.run(2f)
        assertEquals(1, deleted, "the row burns once")
    }

    @Test
    fun aQueuedBurnFindsTheRowGoneAndLetsItBe() {
        val smoke = state()
        var dropped = 0
        assertTrue(smoke.swipeAway("a", SmokeSide.Start, delay = 0.2f) { dropped++ })
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
        assertTrue(smoke.swipeAway("a", SmokeSide.Start, delay = 0.2f) { dropped++ })
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
        assertTrue(smoke.dragStart("a", SmokeSide.Start))
        smoke.plumeOf("a")!!.width = 300f
        smoke.dragTo("a", -200f, 0f)
        smoke.advance(1 / 60f)
        smoke.rows["a"]!!.present = false
        smoke.advance(1 / 60f)
        assertEquals(PlumePhase.Returning, smoke.plumeOf("a")!!.phase)
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
                    start = SmokeAction("Delete", SmokeMaterial.Smoke) {}
                },
            )
        }
        assertEquals(SmokeDimens.PerRow, smoke.budget())
        for (key in keys) assertTrue(smoke.swipeAway(key, SmokeSide.Start))
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
        assertTrue(smoke.swipeAway("a", SmokeSide.Start))
        smoke.advance(1 / 60f)
        assertTrue(smoke.drawn.isNotEmpty(), "the frame loop fills and draws again")
    }

    // A burn loop that says whether it is still playing.
    private class Loops : SoundPlayer {
        var playing = 0
        var volume = 0f

        override fun play(sound: Sound, volume: Float, rate: Float) = Unit

        override fun loop(sound: Sound, volume: Float): SoundLoop {
            playing++
            return object : SoundLoop {
                override fun setVolume(volume: Float) {
                    this@Loops.volume = volume
                }

                override fun stop() {
                    playing--
                }
            }
        }
    }

    @Test
    fun theBurnLoopStopsInTheBackgroundAndWithTheOverlay() {
        val loops = Loops()
        val smoke = state().apply { sound = loops }
        assertTrue(smoke.swipeAway("a", SmokeSide.Start))
        smoke.plumeOf("a")!!.width = 3_000f
        smoke.advance(1 / 60f)
        assertEquals(1, loops.playing)
        smoke.quiet()
        assertEquals(0, loops.playing, "the app went to the background")
        smoke.advance(1 / 60f)
        assertEquals(1, loops.playing, "it starts again with the burn")
        smoke.clear()
        assertEquals(0, loops.playing, "the overlay went")
    }

    // Takes hold of row "a", 300 px wide, [at] px from its left edge, to [side].
    private fun SmokeState.hold(side: SmokeSide = SmokeSide.Start, at: Float = Float.NaN) {
        assertTrue(dragStart("a", side, at))
        plumeOf("a")!!.width = 300f
    }

    // Drags the finger on row "a" to [dx] px from where it took hold, over a frame.
    private fun SmokeState.drag(dx: Float, velocity: Float = 0f) {
        dragTo("a", dx, velocity)
        advance(1 / 60f)
    }

    @Test
    fun theFrontStaysRightUnderTheFinger() {
        val smoke = state()
        smoke.hold()
        smoke.drag(-90f)
        assertEquals(0.3f, smoke.plumeOf("a")!!.front.burnt, 1e-5f, "one to one, no lead")
        smoke.drag(-150f)
        assertEquals(0.5f, smoke.plumeOf("a")!!.front.burnt, 1e-5f)
    }

    @Test
    fun aFingerTakingHoldInsideTheRowIsCaughtUpWithThenFollowed() {
        val smoke = state()
        // 60 px in from the right edge, the one a left swipe burns from.
        smoke.hold(at = 240f)
        smoke.drag(-15f)
        val front = smoke.plumeOf("a")!!.front
        assertTrue(front.burnt > 0.1f && front.burnt < 0.3f, "catching up: ${front.burnt}")
        // Once the finger has gone half as far again, the front is right under it, on and back.
        for (dx in floatArrayOf(-60f, -150f, -100f, -70f)) {
            smoke.drag(dx)
            assertEquals((60f - dx) / 300f, front.burnt, 1e-5f, "finger $dx px across")
        }
        // Back where it took hold, nothing is burning.
        smoke.drag(0f)
        assertEquals(0f, front.burnt)
    }

    @Test
    fun drawingTheFingerBackBringsTheRowBackAsItsSmokeLands() {
        val smoke = state()
        smoke.hold()
        smoke.drag(-150f)
        smoke.run(0.5f)
        smoke.drag(-30f)
        val plume = smoke.plumeOf("a")!!
        val front = plume.front
        assertEquals(PlumePhase.Held, plume.phase, "still the finger's")
        assertEquals(0.1f, front.burnt, 1e-5f)
        // The smoke is flowing back down: the row is still cut behind the finger.
        assertTrue(front.shownFrom(smoke.now) > 0.4f)
        smoke.run(SmokeDimens.RewindMost + 0.05f)
        assertEquals(0.1f, front.shownFrom(smoke.now), 1e-5f, "landed, the row is back")
        for (c in 0 until SmokeDimens.Columns) {
            val burning = (SmokeDimens.Columns - c - 0.5f) / SmokeDimens.Columns < 0.1f
            assertEquals(burning, front.burning(c), "column $c")
        }
    }

    @Test
    fun scrubbingManyTimesThenLettingGoShortBringsItAllBack() {
        val smoke = state()
        smoke.hold()
        repeat(60) {
            smoke.drag(-180f)
            smoke.run(0.1f)
            smoke.drag(-20f)
            smoke.run(0.1f)
        }
        smoke.drag(-60f)
        smoke.release("a", 0f)
        assertEquals(PlumePhase.Returning, smoke.plumeOf("a")!!.phase)
        smoke.run(SmokeDimens.RewindMost + 0.1f)
        assertNull(smoke.plumeOf("a"), "the row must be whole again")
        assertEquals(0, deleted)
        smoke.run(SmokeMotion.longestLife)
        assertFalse(smoke.awake, "the frame loop sleeps again")
    }

    @Test
    fun theTickComesEachWayPastTheCommitPointButNotWhileHoveringThere() {
        val smoke = state()
        smoke.hold()
        smoke.drag(-105f) // 0.35
        assertEquals(1, ticks)
        // Hovering just either side of a third: no more ticks, and it stays armed.
        for (dx in floatArrayOf(-97f, -102f, -95f, -104f, -96f)) smoke.drag(dx)
        assertEquals(1, ticks)
        // Back well short of it: a tick, and a release now would draw it back in.
        smoke.drag(-80f)
        assertEquals(2, ticks)
        smoke.drag(-110f)
        assertEquals(3, ticks)
        // Let go hovering just short of a third, once past it: it burns, as the tick said.
        smoke.drag(-97f)
        smoke.release("a", 0f)
        assertEquals(PlumePhase.Burning, smoke.plumeOf("a")!!.phase)
    }

    @Test
    fun letGoAfterComingBackShortOfAThirdItIsDrawnBackIn() {
        val smoke = state()
        smoke.hold()
        smoke.drag(-200f)
        smoke.drag(-60f)
        smoke.release("a", 0f)
        assertEquals(PlumePhase.Returning, smoke.plumeOf("a")!!.phase)
        smoke.run(1f)
        assertNull(smoke.plumeOf("a"))
        assertEquals(0, deleted)
    }

    @Test
    fun flungBackPastAThirdItIsDrawnBackIn() {
        val smoke = state()
        smoke.hold()
        smoke.drag(-200f)
        smoke.release("a", 3_000f)
        assertEquals(PlumePhase.Returning, smoke.plumeOf("a")!!.phase)
    }

    @Test
    fun theArchiveAndARightToLeftRowScrubTheSameWay() {
        val smoke = state()
        smoke.hold(SmokeSide.End)
        smoke.drag(150f)
        smoke.run(0.5f)
        val front = smoke.plumeOf("a")!!.front
        assertFalse(front.fromRight)
        assertEquals(0.5f, front.burnt, 1e-5f)
        smoke.drag(30f)
        assertEquals(0.1f, front.burnt, 1e-5f)
        assertTrue(front.shownFrom(smoke.now) > 0.4f)
        smoke.release("a", 0f)
        smoke.run(1f)
        assertNull(smoke.plumeOf("a"))
        assertEquals(0, archived)

        val rtl = state()
        rtl.rows["a"]!!.rtl = true
        rtl.hold(SmokeSide.Start)
        rtl.drag(150f)
        val mirrored = rtl.plumeOf("a")!!.front
        assertFalse(mirrored.fromRight, "a right-to-left delete burns from the left edge")
        assertEquals(0.5f, mirrored.burnt, 1e-5f)
        rtl.drag(30f)
        assertEquals(0.1f, mirrored.burnt, 1e-5f)
    }

    @Test
    fun aRowLetGoShortCanBeTakenAgainAndItsSmokeCarriesOn() {
        val smoke = state()
        smoke.hold()
        smoke.drag(-90f)
        smoke.run(0.5f)
        smoke.release("a", 0f)
        val plume = smoke.plumeOf("a")!!
        smoke.advance(1 / 60f)
        assertTrue(smoke.dragStart("a", SmokeSide.Start))
        assertTrue(smoke.plumeOf("a") === plume, "the same smoke, still in the air")
        assertEquals(PlumePhase.Held, plume.phase)
        assertFalse(smoke.dragStart("a", SmokeSide.End), "not the other way while it holds")
    }

    @Test
    fun theBurnLoopAllButFallsSilentAsTheFingerDrawsTheFrontBack() {
        val loops = Loops()
        val smoke = state().apply { sound = loops }
        smoke.hold()
        smoke.drag(-150f, velocity = -800f)
        val burning = loops.volume
        assertTrue(burning > SmokeDimens.HoldVolume * SmokeDimens.LoopVolume)
        smoke.drag(-60f, velocity = 800f)
        assertTrue(loops.volume < burning / 4f, "drawn back, it hushes: ${loops.volume}")
        assertEquals(1, loops.playing)
    }
}
