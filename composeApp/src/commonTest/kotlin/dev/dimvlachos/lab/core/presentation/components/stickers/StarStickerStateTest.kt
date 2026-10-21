package dev.dimvlachos.lab.core.presentation.components.stickers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StarStickerStateTest {
    private val layout = StickerLayout(width = 324f)

    private fun StarStickerState.run(seconds: Float, reduced: Boolean = false): List<StickerEvent> {
        val events = mutableListOf<StickerEvent>()
        var t = 0f
        while (t < seconds) {
            events += step(StickerStep, reduced)
            t += StickerStep
        }
        return events
    }

    @Test
    fun theStarsLandOneAfterAnotherNotAllAtOnce() {
        val s = StarStickerState()
        s.ask(5)
        s.run(0.15f)
        assertTrue(s.pos(0) > 0f)
        assertEquals(0f, s.pos(1), "the second waits for the first to be under way")
        s.run(3f)
        assertTrue((0 until 5).all { s.pos(it) == 1f })
    }

    @Test
    fun goingBackLiftsTheLastStarFirst() {
        val s = StarStickerState()
        s.ask(5)
        s.run(4f)
        s.ask(3)
        s.run(0.1f)
        assertTrue(s.pos(4) < 1f)
        assertEquals(1f, s.pos(3), "the fourth waits until the fifth is far enough off")
        s.run(3f)
        assertEquals(listOf(1f, 1f, 1f, 0f, 0f), (0 until 5).map { s.pos(it) })
    }

    @Test
    fun theLabelCountsTheStarsThatHaveLandedNotTheDragsTarget() {
        val s = StarStickerState()
        s.ask(5)
        s.run(0.5f)
        assertEquals(5, s.target)
        assertTrue(s.landed < 5, "only ${s.landed} are down yet")
        s.run(4f)
        assertEquals(5, s.landed)
        s.ask(1)
        s.run(4f)
        assertEquals(1, s.landed)
    }

    @Test
    fun peelsPressesAndSettlesAreReported() {
        val s = StarStickerState()
        s.ask(1)
        val down = s.run(2f)
        assertEquals(listOf(StickerEvent.Peel(0), StickerEvent.Press(0)), down)
        s.ask(0)
        val up = s.run(2f)
        assertEquals(listOf(StickerEvent.Peel(0), StickerEvent.Settle(0)), up)
    }

    @Test
    fun reducedMotionPlacesTheStarsQuickly() {
        val s = StarStickerState()
        s.ask(3)
        s.run(0.6f, reduced = true)
        assertEquals(3, s.landed)
    }

    @Test
    fun aDragCountsTheStarCentresPassedAThirdOfASlotEarly() {
        val sp = layout.spacing
        assertEquals(0, layout.countAt(layout.slotX(0) - sp * .4f))
        assertEquals(1, layout.countAt(layout.slotX(0) - sp * .3f))
        assertEquals(3, layout.countAt(layout.slotX(2) + sp * .1f))
        assertEquals(5, layout.countAt(layout.slotX(4) + sp))
    }

    @Test
    fun aTapPicksTheNearestStar() {
        assertEquals(1, layout.nearestAt(-50f))
        assertEquals(4, layout.nearestAt(layout.slotX(3) + 3f))
        assertEquals(5, layout.nearestAt(1000f))
    }

    @Test
    fun aStarRestsOnTheSheetFliesUpAndLandsInItsSlot() {
        val s = StarStickerState()
        val onSheet = poseOf(layout, s, 0, 0f, now = 0f, reduced = false)
        assertEquals(layout.sheetX(0), onSheet.x, 0.01f)
        assertEquals(layout.sheetY(0), onSheet.y, 0.01f)
        val inSlot = poseOf(layout, s, 0, 1f, now = 0f, reduced = false)
        assertEquals(layout.slotX(0), inSlot.x, 0.01f)
        assertEquals(layout.slotY, inSlot.y, 0.01f)
        val mid = poseOf(layout, s, 0, .5f, now = 0f, reduced = false)
        assertTrue(
            mid.y < (layout.sheetY(0) + layout.slotY) / 2f - 20f,
            "it arcs above the straight way up",
        )
        assertTrue(mid.lift > .9f)
    }

    @Test
    fun aPeelingStarRisesAwayFromAFingerOnTheSheet() {
        val s = StarStickerState()
        val peeling = poseOf(layout, s, 0, .25f, now = 0f, reduced = false)
        assertTrue(peeling.y < layout.sheetY(0) - 2f, "lifted ${layout.sheetY(0) - peeling.y}")
        assertTrue(peeling.a > .4f)
    }
}
