package dev.dimvlachos.lab.core.presentation.components.crtpip

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrtPipStateTest {
    // The prototype's phone: 412 × 915, the slot under a 56 dp bar, the nav bar 62 dp tall.
    private fun pip(reduced: Boolean = false) =
        CrtPipState().apply {
            reducedMotion = reduced
            layout(412f, 915f, 56f, 853f)
            reset()
        }

    /** Steps [ms] at 60 fps, stopping early once [until] holds; returns whether it did. */
    private fun CrtPipState.run(ms: Int, until: () -> Boolean = { false }): Boolean {
        var t = 0
        while (t < ms) {
            if (until()) return true
            step(1f / 60f)
            t += 16
        }
        return until()
    }

    private fun CrtPipState.swipeDown(by: Float = 130f) {
        playerDown(playerX, playerY - 10, now)
        step(.04f)
        playerUp(playerX, playerY - 10 + by, now)
    }

    private fun CrtPipState.toMini() {
        swipeDown()
        assertTrue(run(2000) { mode == PipMode.Mini })
    }

    private fun CrtPipState.near(c: String) = hypot(cabX - cornerX(c), cabY - cornerY(c)) < 1.5f

    private val order = listOf("squeeze", "line", "dot", "warm", "open")

    @Test
    fun startsOnTheFullPlayerAt16By9() {
        val s = pip()
        assertEquals(PipMode.Full, s.mode)
        assertEquals(232f, s.slotH)
        assertTrue(s.pose.picture)
        assertFalse(s.pose.cab)
    }

    @Test
    fun theShowPlaysAndStartsAt412() {
        val s = pip()
        assertEquals(252, s.second)
        s.run(1000)
        assertTrue(s.vt > 252.9f)
    }

    @Test
    fun theSetIs124DpWideOnA360DpPhone() {
        val s = CrtPipState().apply { layout(360f, 780f, 56f, 718f) }
        assertEquals(124f, s.cabW, .01f)
    }

    @Test
    fun aTapShowsTheControlsAndStaysFull() {
        val s = pip()
        s.playerDown(200f, 160f, s.now)
        s.step(.04f)
        s.playerUp(200f, 160f, s.now)
        s.step(.016f)
        assertEquals(PipMode.Full, s.mode)
        assertTrue(s.controlsOn)
        s.run(2700)
        assertFalse(s.controlsOn)
    }

    @Test
    fun draggingDownPreSqueezesAndLettingGoEarlySpringsBack() {
        val s = pip()
        s.playerDown(200f, 160f, 0.0)
        s.playerMove(200f, 200f, 30.0)
        assertTrue(s.pre > .1f && s.preY > 5f)
        s.step(.016f)
        assertTrue(s.pose.pictureSy < 1f)
        s.playerUp(200f, 200f, 150.0)
        assertEquals(PipMode.Full, s.mode)
        assertTrue(s.run(600) { s.pre == 0f && abs(s.preY) < .5f })
    }

    @Test
    fun aSwipeUpDoesNothing() {
        val s = pip()
        s.playerDown(200f, 160f, 0.0)
        s.playerMove(200f, 80f, 30.0)
        s.playerUp(200f, 80f, 90.0)
        assertEquals(PipMode.Full, s.mode)
    }

    @Test
    fun aSwipeDownWithNoMovesSwitchesItOffThroughEveryPhaseInOrder() {
        val s = pip()
        val heard = mutableListOf<PipEvent>()
        s.onEvent = { e, _ -> heard += e }
        s.swipeDown()
        assertEquals(PipMode.ToMini, s.mode)
        assertTrue(s.run(2000) { s.mode == PipMode.Mini })
        assertEquals(order, s.seen)
        assertEquals(
            listOf(PipEvent.Commit, PipEvent.Off, PipEvent.Fly, PipEvent.On),
            heard.filter { it != PipEvent.Snap },
        )
        assertEquals(0f, s.slotH)
        assertEquals(PipAnnouncement.Mini, s.announcement)
        assertTrue(s.pose.cab && s.pose.screen == PipScreen.Picture)
    }

    @Test
    fun theDotStaysOnScreenAndFliesToTheSet() {
        val s = pip()
        s.swipeDown()
        val trail = mutableListOf<Pair<Float, Float>>()
        s.run(2000) {
            if (s.pose.flight) {
                val p = s.pose
                val c = maxOf(p.fromX, p.toX) + 40 * s.k
                val cy = minOf(p.fromY, p.toY) + abs(p.toY - p.fromY) * .2f
                val e = CrtPipState.ease(p.flightF)
                trail +=
                    CrtPipState.bez(p.fromX, c, p.toX, e) to CrtPipState.bez(p.fromY, cy, p.toY, e)
            }
            s.mode == PipMode.Mini
        }
        assertTrue(trail.size > 5)
        assertTrue(trail.all { (x, y) -> x in 0f..s.width && y in 0f..s.height })
        val d = { p: Pair<Float, Float> -> hypot(p.first - s.setX, p.second - s.setY) }
        assertTrue(d(trail.last()) < d(trail.first()) * .6f)
    }

    @Test
    fun theSetFadesAsOnePieceAndNeverHangsSeeThroughAtFullSize() {
        val s = pip()
        s.toMini()
        s.setDown(s.setX, s.setY, s.now)
        s.step(.04f)
        s.setUp(s.setX, s.setY, s.now)
        assertEquals(PipMode.ToFull, s.mode)
        s.run(2000) {
            val p = s.pose
            if (p.cab && p.cabAlpha < 1f) assertTrue(p.cabScale < .55f, "faded at ${p.cabScale}")
            s.mode == PipMode.Full
        }
        assertEquals(order, s.seen)
        assertEquals(232f, s.slotH)
    }

    @Test
    fun aCancelledSwipeStillMinimizesFromItsLastPoint() {
        val s = pip()
        s.playerDown(200f, 150f, 0.0)
        s.playerMove(200f, 270f, 30.0)
        s.playerCancel(60.0)
        assertEquals(PipMode.ToMini, s.mode)
    }

    @Test
    fun aQuickFlickMinimizesOnSpeed() {
        val s = pip()
        s.playerDown(200f, 150f, 0.0)
        s.playerMove(200f, 170f, 1.0)
        s.playerMove(200f, 190f, 17.0)
        s.playerUp(200f, 200f, 25.0)
        assertEquals(PipMode.ToMini, s.mode)
    }

    @Test
    fun theSetRestsInTheBottomRightCornerAboveTheNavBar() {
        val s = pip()
        s.toMini()
        assertTrue(s.near("br"))
        assertTrue(s.cabY + s.cabH <= s.navTop)
        assertTrue(s.cabX + s.cabW <= s.width)
    }

    @Test
    fun draggingTheSetUpSpringsItIntoTheTopRightCornerClearOfTheTopBar() {
        val s = pip()
        s.toMini()
        val x = s.setX
        val y = s.setY
        s.setDown(x, y, 0.0)
        s.setMove(x, y - 60, 30.0)
        s.setMove(x, s.slotTop + 90, 60.0)
        s.setUp(x, s.slotTop + 90, 310.0)
        assertEquals(PipMode.Mini, s.mode)
        assertTrue(s.run(1500) { !s.spring })
        assertEquals("tr", s.corner)
        assertTrue(s.near("tr"))
        assertTrue(s.cabY - CrtPipState.Antenna * s.ck >= s.slotTop - .5f)
    }

    @Test
    fun aSparseDragLandsInTheNearestCorner() {
        val s = pip()
        s.toMini()
        s.setDown(s.setX, s.setY, 0.0)
        s.setUp(s.width * .3f, s.navTop - 40, 900.0)
        assertTrue(s.run(1500) { !s.spring })
        assertEquals("bl", s.corner)
        assertTrue(s.near("bl"))
    }

    @Test
    fun aFingerThatRestsBeforeLiftingDoesNotFling() {
        val s = pip()
        s.toMini()
        val x = s.setX
        val y = s.setY
        s.setDown(x, y, 0.0)
        s.setMove(x - 40, y, 16.0)
        s.setMove(x - 80, y, 32.0)
        s.setMove(x - 80, y, 400.0)
        s.setUp(x - 80, y, 410.0)
        assertTrue(s.run(1500) { !s.spring })
        assertEquals("br", s.corner)
    }

    @Test
    fun aCancelledDragSnapsFromItsLastPoint() {
        val s = pip()
        s.toMini()
        s.setDown(s.setX, s.setY, 0.0)
        s.setMove(s.setX + 10, s.setY - 40, 30.0)
        s.setMove(s.width * .8f, s.slotTop + 60, 60.0)
        s.setCancel(260.0)
        assertTrue(s.run(1500) { !s.spring })
        assertEquals("tr", s.corner)
    }

    @Test
    fun aFlickOffTheSideStopsPlaybackShowsTheToastAndUndoBringsItBack() {
        val s = pip()
        s.toMini()
        // Up to the top-right first, so Undo has a height to come back to.
        s.setDown(s.setX, s.setY, 0.0)
        s.setUp(s.width * .8f, s.slotTop + 60, 900.0)
        s.run(1500) { !s.spring }
        val x = s.setX
        val y = s.setY
        s.setDown(x, y, 0.0)
        s.setMove(x - 40, y, 16.0)
        s.setMove(x - 110, y + 4, 32.0)
        s.setUp(x - 160, y + 6, 48.0)
        assertEquals(PipMode.ToOff, s.mode)
        assertTrue(s.run(2000) { s.mode == PipMode.Stopped })
        assertEquals(0f, s.slotH)
        assertEquals(PipAnnouncement.Stopped, s.announcement)
        s.step(.016f)
        assertTrue(s.toastOn)
        val v = s.vt
        s.run(300)
        assertEquals(v, s.vt)
        s.undo()
        assertFalse(s.toastOn)
        assertTrue(s.toastInstant)
        assertTrue(s.run(1000) { s.mode == PipMode.Mini })
        assertEquals("tl", s.corner)
        assertTrue(s.near("tl"))
        s.run(400)
        assertTrue(s.vt > v + .3f)
    }

    @Test
    fun deleteStopsPlaybackAndTheToastTimesOut() {
        val s = pip()
        s.toMini()
        s.stopFromKeys()
        assertEquals(PipFocus.Undo, s.focusRequest)
        assertTrue(s.run(1500) { s.toastOn })
        assertTrue(s.run(4800) { !s.toastOn })
        assertEquals(PipMode.Stopped, s.mode)
        assertTrue(s.settled)
    }

    @Test
    fun keyboardFocusFollowsTheVideo() {
        val s = pip()
        s.minimize(keyboard = true)
        s.run(2000) { s.mode == PipMode.Mini }
        assertEquals(PipFocus.Set, s.focusRequest)
        s.focusRequest = PipFocus.None
        s.expand(keyboard = true)
        s.run(2000) { s.mode == PipMode.Full }
        assertEquals(PipFocus.Player, s.focusRequest)
    }

    @Test
    fun aScriptedDragCutShortSpringsBack() {
        val s = pip()
        s.playerDown(200f, 150f, s.now, scripted = true)
        s.playerMove(200f, 220f, s.now + 100)
        s.releaseScripted()
        assertFalse(s.dragging)
        assertTrue(s.run(600) { s.pre == 0f })
        assertEquals(PipMode.Full, s.mode)
    }

    @Test
    fun reducedMotionSwapsAtOnce() {
        val s = pip(reduced = true)
        s.swipeDown()
        assertEquals(PipMode.Mini, s.mode)
        assertEquals(0f, s.slotH)
        s.expand()
        assertEquals(PipMode.Full, s.mode)
        assertEquals(232f, s.slotH)
    }

    @Test
    fun resetGoesBackToTheFullPlayerAndTheUsualCorner() {
        val s = pip()
        s.toMini()
        s.stopFromKeys()
        s.run(1500)
        s.reset()
        assertEquals(PipMode.Full, s.mode)
        assertEquals("br", s.corner)
        assertEquals(232f, s.slotH)
        assertFalse(s.toastOn)
    }
}
