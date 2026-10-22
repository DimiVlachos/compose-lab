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
    fun lettingGoDropsTheTabFlatWithAClackAndTheTagHangsOn() {
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
        assertTrue(z.swing in ZipperTag.SwingMin..ZipperTag.SwingMax, "swing ${z.swing}")
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
    fun followingTheListMovesThePullWithoutLiftingTheTag() {
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
        assertEquals(ZipperTag.SwingHeld, z.swing)
        assertEquals(0f, z.swingV)
        z.up(0f)
        z.step(.016f)
        assertEquals(0f, z.flip)
        assertEquals(ZipperTag.SwingRest, z.swing)
        // A fling doesn't swing it: it hangs still.
        for (i in 1..20) {
            z.follow(i * .03f)
            z.step(.016f)
            assertEquals(0f, z.swingV)
        }
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
        z.run(6000)
        assertTrue(z.settled, "flip ${z.flip} tag ${z.tag} swing ${z.swing}")
    }

    @Test
    fun theListScrollingWakesTheSpringsUntilTheyCatchUp() {
        val z = zipper()
        z.follow(.5f)
        z.step(0f)
        assertFalse(z.settled)
        // A jump that big is a hard stop: the tab hops and the tag swings before all is still.
        z.run(6000)
        assertTrue(z.settled, "flip ${z.flip} swing ${z.swing}")
    }

    @Test
    fun theTagHangsStillAtRest() {
        val z = zipper()
        assertEquals(ZipperTag.SwingRest, z.swing)
        assertEquals(0f, z.swingV)
        z.run(500)
        assertEquals(ZipperTag.SwingRest, z.swing)
        assertTrue(z.settled)
    }

    @Test
    fun aFastDragSwingsTheTagOnItsThreadAndItSettles() {
        val z = zipper()
        val c = z.pullCenter()
        z.down(c)
        z.run(3000)
        // Held, it hangs a little higher, still.
        assertTrue(abs(z.swing - ZipperTag.SwingHeld) < .01f, "held swing ${z.swing}")
        var furthest = 0f
        for (i in 1..12) {
            z.move(c + i * 25f)
            z.run(16)
            furthest = maxOf(furthest, abs(z.swing - ZipperTag.SwingHeld))
        }
        repeat(40) {
            z.run(16)
            furthest = maxOf(furthest, abs(z.swing - ZipperTag.SwingHeld))
        }
        assertTrue(furthest > .1f, "it swung only ${furthest} rad")
        z.up(c + 300f)
        z.run(6000)
        assertTrue(z.settled, "swing ${z.swing} v ${z.swingV}")
        assertEquals(ZipperTag.SwingRest, z.swing)
    }

    @Test
    fun aFlingOfTheListSwingsTheTagAndItComesBackToRest() {
        val z = zipper()
        // The list flings down and slows to a stop, a frame at a time.
        var p = 0f
        var v = .03f
        var furthest = 0f
        while (v > .0005f) {
            p += v
            v *= .9f
            z.follow(p)
            z.step(.016f)
            furthest = maxOf(furthest, abs(z.swing - ZipperTag.SwingRest))
        }
        repeat(30) {
            z.step(.016f)
            furthest = maxOf(furthest, abs(z.swing - ZipperTag.SwingRest))
        }
        assertTrue(furthest > .05f, "it swung only ${furthest} rad")
        repeat(400) { z.step(.016f) }
        assertTrue(z.settled, "swing ${z.swing} v ${z.swingV}")
        assertEquals(ZipperTag.SwingRest, z.swing)
    }

    @Test
    fun yankedDownTheTagLagsUpAndStoppedItSwingsOnDown() {
        val z = zipper()
        val c = z.pullCenter()
        z.down(c)
        z.run(3000)
        assertEquals(ZipperTag.SwingHeld, z.swing)
        z.move(c + 200f)
        z.run(32)
        assertTrue(z.swing > ZipperTag.SwingHeld, "yanked down, it lags up: ${z.swing}")
        var peak = z.swing
        repeat(10) {
            z.run(16)
            peak = maxOf(peak, z.swing)
        }
        // The pull has stopped: it swings back on down past where it hung.
        var lowest = peak
        repeat(30) {
            z.run(16)
            lowest = minOf(lowest, z.swing)
        }
        assertTrue(lowest < ZipperTag.SwingHeld, "stopped, it swings on down: $lowest")
    }

    @Test
    fun aTallerListAreaLetsATagRestingOnTheEdgeHangAgain() {
        // Just above the bottom, the tag rests on the edge; twice as tall, there is room below it.
        val z = zipper()
        z.follow(.985f)
        z.run(6000)
        assertTrue(z.settled)
        assertTrue(z.swing > ZipperTag.SwingRest, "propped at ${z.swing}")
        z.layout(1200f)
        z.step(0f)
        assertFalse(z.settled)
        z.run(6000)
        assertEquals(ZipperTag.SwingRest, z.swing)
    }

    @Test
    fun theTagHangsWithinTheListWhereverThePullIs() {
        val z = zipper()
        val c = z.pullCenter()
        z.down(c)
        z.run(300)
        // Hard to the bottom, held, then let go there and back up to the top.
        for (target in floatArrayOf(1f, 0f)) {
            z.move(z.pullCenter())
            for (i in 0..30) {
                z.move(
                    z.sliderTopFor(z.pos) +
                        ZipperState.SliderHeight / 2f +
                        60f * (if (target > 0f) 1 else -1),
                )
                z.run(16)
                assertInside(z)
            }
            repeat(200) {
                z.run(16)
                assertInside(z)
            }
        }
        z.up(0f)
        repeat(300) {
            z.run(16)
            assertInside(z)
        }
    }

    @Test
    fun atTheBottomTheTagRestsOnTheEdgeInsteadOfHangingOff() {
        val z = zipper()
        z.follow(1f)
        z.run(6000)
        assertTrue(z.settled)
        // Its rest would hang it off the bottom: it rests higher, on the edge.
        assertTrue(z.swing > ZipperTag.SwingRest, "swing ${z.swing}")
        assertInside(z)
    }

    private fun assertInside(z: ZipperState) {
        val hy = ZipperTag.holeY(z.sliderTopFor(z.pos), z.flip)
        val k = ZipperTag.scale(z.tag)
        val top = hy + ZipperTag.extentY(z.swing, k, bottom = false)
        val bottom = hy + ZipperTag.extentY(z.swing, k, bottom = true)
        assertTrue(
            top >= 3f - 1e-3f && bottom <= z.height - 6f + 1e-3f,
            "tag from $top to $bottom in ${z.height}",
        )
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
