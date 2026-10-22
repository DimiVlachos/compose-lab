package dev.dimvlachos.lab.core.presentation.components.zipper

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZipperStateTest {
    private val events = mutableListOf<ZipperEvent>()

    private fun zipper(height: Float = 600f) =
        ZipperState().apply {
            layout(height)
            onEvent = { e, _ -> events += e }
        }

    // The list follows every request at once, as it does a frame later on screen.
    private fun ZipperState.run(ms: Int) {
        repeat(ms / 16) {
            takeScrollRequest()?.let { follow(it) }
            step(.016f)
        }
    }

    @Test
    fun itStartsZippedShutAtTheTop() {
        val z = zipper()
        assertEquals(0f, z.pos)
        assertEquals(0f, z.flip)
        assertEquals("A", z.letter)
        assertTrue(z.settled)
    }

    @Test
    fun theTrackLeavesRoomForThePullAndItsTab() {
        val z = zipper(600f)
        assertEquals(592f, z.trackBot)
        assertEquals(592f - 8f - 30f - 34f - 6f, z.range)
        assertEquals(8f, z.sliderTopFor(0f))
        assertEquals(8f + z.range, z.sliderTopFor(1f))
    }

    @Test
    fun holdingThePullLiftsTheTabAndShowsTheTag() {
        val z = zipper()
        z.down(z.pullCenter())
        z.run(450)
        assertTrue(z.held)
        assertTrue(z.flip > .9f, "flip ${z.flip}")
        assertTrue(z.tag > .8f, "tag ${z.tag}")
        assertEquals(ZipperEvent.Lift, events.first())
    }

    @Test
    fun aMoveInsideTheSlopDoesNotScroll() {
        val z = zipper()
        val c = z.pullCenter()
        z.down(c)
        z.move(c + 3f)
        assertNull(z.scrollRequest)
        assertEquals(0f, z.pos)
    }

    @Test
    fun oneBigFirstMoveCountsInFull() {
        val z = zipper()
        val c = z.pullCenter()
        z.down(c)
        z.move(c + z.range * .5f)
        assertTrue(abs(z.pos - .5f) < .03f, "pos ${z.pos}")
        assertEquals(z.pos, z.scrollRequest)
    }

    @Test
    fun lettingGoDropsTheTabFlatWithAClackAndHidesTheTag() {
        val z = zipper()
        val c = z.pullCenter()
        z.down(c)
        z.run(450)
        z.move(c + z.range * .9f)
        z.up(c + z.range * .9f)
        z.run(700)
        assertFalse(z.held)
        assertTrue(z.flip < .05f, "flip ${z.flip}")
        assertTrue(z.clacks >= 1)
        assertTrue(z.tag < .15f, "tag ${z.tag}")
        assertTrue(ZipperEvent.Clack in events)
    }

    @Test
    fun aTapOnTheTapeGlidesThePullThere() {
        val z = zipper()
        val y = z.trackTop + ZipperState.SliderHeight / 2f + z.range * .2f
        z.down(y)
        z.up(y)
        assertFalse(z.held)
        z.run(1200)
        assertTrue(abs(z.pos - .2f) < .03f, "pos ${z.pos}")
        assertNull(z.glide)
    }

    @Test
    fun draggingOffThePullGrabsItUnderTheFinger() {
        val z = zipper()
        val y = z.trackTop + ZipperState.SliderHeight / 2f + z.range * .3f
        z.down(y)
        z.move(y + 40f)
        z.move(y + 80f)
        assertTrue(z.held)
        assertTrue(abs(z.pos - z.posForY(y + 80f)) < .001f)
    }

    @Test
    fun theTeethRaspAsThePullPassesThem() {
        val z = zipper()
        val c = z.pullCenter()
        z.down(c)
        z.move(c + 60f)
        z.run(32)
        assertTrue(ZipperEvent.Teeth in events)
    }

    @Test
    fun aNewLetterTicksOnlyWhileHeld() {
        val z = zipper()
        z.showLetter("B")
        assertFalse(ZipperEvent.Letter in events)
        z.down(z.pullCenter())
        z.showLetter("C")
        assertTrue(ZipperEvent.Letter in events)
        assertEquals("C", z.letter)
    }

    @Test
    fun touchingTheListStopsAGlideAndLetsGo() {
        val z = zipper()
        z.glideTo(.8f)
        z.run(32)
        z.listTouched()
        assertNull(z.glide)
        z.down(z.pullCenter())
        z.listTouched()
        assertFalse(z.held)
    }

    @Test
    fun followingTheListMovesThePullWithoutTheTag() {
        val z = zipper()
        z.follow(.6f)
        z.run(250)
        assertEquals(.6f, z.pos)
        assertTrue(z.tag < .05f)
    }

    @Test
    fun underReducedMotionTheTabSnapsAndGlidesJump() {
        val z = zipper()
        z.reducedMotion = true
        z.down(z.pullCenter())
        z.step(.016f)
        assertEquals(1.2f, z.flip)
        assertEquals(1f, z.tag)
        z.up(0f)
        z.step(.016f)
        assertEquals(0f, z.flip)
        z.glideTo(.7f)
        z.step(.016f)
        assertEquals(.7f, z.pos)
        z.run(200)
        assertTrue(z.settled)
    }

    @Test
    fun afterLettingGoItComesToRest() {
        val z = zipper()
        val c = z.pullCenter()
        z.down(c)
        z.run(400)
        z.move(c + 100f)
        z.run(100)
        z.up(c + 100f)
        z.run(3000)
        assertTrue(z.settled, "flip ${z.flip} tag ${z.tag} sway ${z.sway}")
    }

    @Test
    fun theListScrollingWakesTheSpringsUntilTheyCatchUp() {
        val z = zipper()
        z.follow(.5f)
        z.step(0f)
        assertFalse(z.settled)
        // A jump that big is a hard stop: the tab hops and the tag sways before all is still.
        z.run(4000)
        assertTrue(z.settled, "flip ${z.flip} sway ${z.sway}")
    }

    @Test
    fun aKeyGlideDuringADragKeepsTheFingersHold() {
        val z = zipper()
        val c = z.pullCenter()
        z.down(c)
        z.glideTo(.5f)
        z.up(c)
        assertFalse(z.held)
    }

    @Test
    fun resetGoesBackToTheTop() {
        val z = zipper()
        z.follow(.5f)
        z.reset()
        assertEquals(0f, z.pos)
        assertEquals(0f, z.takeScrollRequest())
        assertEquals("A", z.letter)
    }
}
