package dev.dimvlachos.lab.core.presentation.components.paintroller

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaintRollerStateTest {
    private val events = mutableListOf<RollerEvent>()

    // The prototype's screen at 412 × 915, as headless Chrome lays it out.
    private fun roller(reduced: Boolean = false) =
        PaintRollerState().apply {
            reducedMotion = reduced
            onEvent = { events += it }
            layout(
                412f,
                915f,
                709.6f,
                List(5) { Rect(16f + it * 77.2f, 736.2f, 16f + it * 77.2f + 71.2f, 836.2f) },
                listOf(
                    Offset(115.35f, 278.3f),
                    Offset(157f, 329.6f),
                    Offset(206f, 364.6f),
                    Offset(358f, 440.6f),
                    Offset(358f, 492.6f),
                    Offset(358f, 544.6f),
                    Offset(318f, 597f),
                ),
            )
        }

    private fun PaintRollerState.run(ms: Int) {
        repeat(ms / 16) { step(.016f) }
    }

    private fun PaintRollerState.runUntil(ms: Int, cond: () -> Boolean): Boolean {
        repeat(ms / 16) {
            if (cond()) return true
            step(.016f)
        }
        return cond()
    }

    private fun PaintRollerState.tap(i: Int) {
        val t = trays[i]
        trayDown(i, t.cx, t.top + 20)
        step(.03f)
        trayUp(t.cx, t.top + 20)
    }

    @Test
    fun itStartsInBlueRidgeWithTheRollerResting() {
        val r = roller()
        assertEquals(0, r.current)
        assertEquals(-1, r.coat)
        assertEquals(RollerMode.Rest, r.mode)
        assertTrue(r.rects.isEmpty())
        assertEquals(r.trays[0].cx, r.rx)
        assertTrue(r.settled)
    }

    @Test
    fun aTapDipsPaintsTheWallInTwoStrokesAndCommits() {
        val r = roller()
        r.tap(1)
        assertTrue(r.runUntil(600) { r.rollerPaint == 1 && r.coat == 1 })
        assertTrue(r.runUntil(1500) { r.rs > .95f && r.rects.isNotEmpty() })
        assertTrue(r.runUntil(1500) { r.coverage > .2f && r.coverage < .8f })
        assertEquals(0, r.current)
        assertTrue(r.runUntil(1200) { (0 until 7).any { r.wetCoat(it) > .6f } })
        // Another tray mid-roll is ignored.
        r.tap(3)
        assertEquals(1, r.coat)
        assertTrue(r.runUntil(3000) { r.current == 1 })
        assertEquals(-1, r.coat)
        assertTrue(r.rects.isEmpty())
        assertTrue(r.snackOn)
        assertEquals(1, r.said.first)
        assertTrue(RollerEvent.Dip in events && RollerEvent.Commit in events)
        assertTrue(r.runUntil(1500) { r.mode == RollerMode.Rest && !r.busy })
        assertEquals(r.trays[1].cx, r.rx, .5f)
        assertEquals(RollerEvent.Set, events.last())
        // The paint it wears dries, and the snackbar goes after five seconds.
        assertTrue(r.runUntil(4000) { (0 until 7).all { r.wetBase(it) < .01f } })
        assertTrue(r.runUntil(5000) { !r.snackOn })
        assertTrue(r.runUntil(500) { r.settled })
    }

    @Test
    fun undoRollsTheOldColourBackWithNoSnackbar() {
        val r = roller()
        r.tap(1)
        r.runUntil(6000) { r.current == 1 && !r.busy }
        r.undo()
        assertEquals(0, r.coat)
        assertTrue(r.runUntil(5000) { r.current == 0 && !r.busy })
        assertFalse(r.snackOn)
    }

    @Test
    fun undoWhileTheRollerGoesHomeWaitsForIt() {
        val r = roller()
        r.tap(1)
        r.runUntil(6000) { r.current == 1 }
        assertTrue(r.busy)
        r.undo()
        assertTrue(r.runUntil(2000) { r.coat == 0 })
        assertTrue(r.runUntil(5000) { r.current == 0 && !r.busy })
    }

    @Test
    fun aSparseDragPaintsASolidStripeAboveTheFingerAndFinishesOnRelease() {
        val r = roller()
        val t = r.trays[2]
        r.trayDown(2, t.cx, t.top + 20)
        r.step(.04f)
        val pts =
            listOf(
                r.colL to r.botY + r.grip,
                r.colL to r.topY + r.grip,
                r.colR to r.topY + r.grip,
                r.colR to (r.topY + r.botY) / 2 + r.grip,
            )
        r.trayMove(pts[0].first, pts[0].second)
        assertEquals(RollerMode.Lift, r.mode)
        for (p in pts.drop(1)) {
            r.run(160)
            r.trayMove(p.first, p.second)
        }
        r.run(200)
        val c = ((r.colL - 4) / 8).roundToInt()
        val row = ((r.paintBottom / 2 - 4) / 8).roundToInt()
        assertTrue(r.isPainted(c, row))
        assertTrue(abs(r.ry - (r.topY + r.botY) / 2) < 2f)
        assertTrue(r.rects.all { it.y0 < r.paintBottom })
        r.trayUp(pts[3].first, pts[3].second)
        assertTrue(r.runUntil(6000) { r.current == 2 && !r.busy })
    }

    @Test
    fun aDragStartedDuringTheDipNeverSnapsBackOntoTheTrays() {
        val r = roller()
        val t = r.trays[1]
        r.trayDown(1, t.cx, t.top + 20)
        r.run(48)
        val fy = 542f
        r.trayMove(t.cx, fy)
        var lowest = 0f
        repeat(30) {
            r.step(.016f)
            r.trayMove(t.cx, fy)
            if (it > 2) lowest = maxOf(lowest, r.ry)
        }
        // Once lifting, the roller only climbs toward the finger's reach.
        assertTrue(lowest < t.top, "the roller fell back to $lowest")
        assertTrue(abs(r.ry - (fy - r.grip)) < 2f)
    }

    @Test
    fun pressAndReleaseFarAwayWithNoMovesStillRepaints() {
        val r = roller()
        val t = r.trays[3]
        r.trayDown(3, t.cx, t.top + 20)
        r.step(.03f)
        r.trayUp(r.colL, r.topY + r.grip)
        assertTrue(r.runUntil(7000) { r.current == 3 && !r.busy })
    }

    @Test
    fun aSmallWobbleIsATap() {
        val r = roller()
        val t = r.trays[4]
        r.trayDown(4, t.cx, t.top + 20)
        r.trayMove(t.cx + 4, t.top + 23)
        r.trayUp(t.cx + 4, t.top + 23)
        assertTrue(r.runUntil(6000) { r.current == 4 && !r.busy })
    }

    @Test
    fun theCurrentColourDoesNothing() {
        val r = roller()
        r.tap(0)
        assertFalse(r.busy)
        assertEquals(-1, r.coat)
        assertEquals(RollerMode.Rest, r.mode)
        // Pressed and dragged away, it still does nothing.
        val t = r.trays[0]
        r.trayDown(0, t.cx, t.top + 20)
        r.trayUp(t.cx, 300f)
        assertFalse(r.busy)
        assertEquals(RollerMode.Rest, r.mode)
    }

    @Test
    fun arrowKeysPickTheNextColourRound() {
        val r = roller()
        assertEquals(4, r.pickNext(-1))
        assertTrue(r.runUntil(6000) { r.current == 4 && !r.busy })
        assertEquals(0, r.pickNext(1))
        assertTrue(r.runUntil(6000) { r.current == 0 && !r.busy })
    }

    @Test
    fun aDemoStoppedMidDragLetsGoAndFinishes() {
        val r = roller()
        val t = r.trays[1]
        r.trayDown(1, t.cx, t.top + 20, demo = true)
        r.run(240)
        r.trayMove(r.colL, r.botY + r.grip)
        r.run(200)
        r.releaseDemo(r.colL, r.botY + r.grip)
        assertFalse(r.dragging)
        assertTrue(r.runUntil(7000) { r.current == 1 && !r.busy })
    }

    @Test
    fun zigzagStrokesMergeIntoFewRectangles() {
        val r = roller()
        val t = r.trays[2]
        r.trayDown(2, t.cx, t.top + 20)
        r.trayMove(r.colL, r.topY + r.grip)
        r.run(300)
        // Back and forth along the top eight times, then up and down the left eight times.
        repeat(8) { pass ->
            repeat(20) {
                val f = if (pass % 2 == 0) it / 19f else 1 - it / 19f
                r.step(.016f)
                r.trayMove(r.colL + (r.colR - r.colL) * f, r.topY + r.grip)
            }
        }
        repeat(8) { pass ->
            repeat(20) {
                val f = if (pass % 2 == 0) it / 19f else 1 - it / 19f
                r.step(.016f)
                r.trayMove(r.colL, r.topY + (r.botY - r.topY) * f + r.grip)
            }
        }
        assertTrue(r.rects.size <= 40, "${r.rects.size} rectangles")
    }

    @Test
    fun reducedMotionSwitchesAtOnce() {
        val r = roller(reduced = true)
        r.tap(2)
        assertEquals(2, r.current)
        assertFalse(r.busy)
        assertEquals(RollerMode.Rest, r.mode)
        assertEquals(0f, r.wetBase(0))
    }

    @Test
    fun resetToDefaultRollsBlueRidgeBack() {
        val r = roller()
        r.tap(2)
        r.runUntil(6000) { r.current == 2 && !r.busy }
        r.resetToDefault()
        assertTrue(r.runUntil(6000) { r.current == 0 && !r.busy })
    }

    @Test
    fun aPressWhileBusyIsNoDragAndTheSnackbarLetsTheLoopSleep() {
        val r = roller()
        r.tap(1)
        r.run(200)
        val t = r.trays[3]
        assertFalse(r.trayDown(3, t.cx, t.top + 20))
        assertTrue(r.runUntil(6000) { r.current == 1 && !r.busy })
        assertTrue(r.snackOn)
        // Home and dry, the loop may sleep with the snackbar up; its own timer takes it down.
        assertTrue(r.runUntil(3000) { r.settled })
        assertTrue(r.snackOn)
        r.timeOutSnack(r.snackShown - 1)
        assertTrue(r.snackOn)
        r.timeOutSnack(r.snackShown)
        assertFalse(r.snackOn)
    }

    @Test
    fun aShortTrayKeepsItsArtClearOfItsName() {
        // A 393 dp phone on the tiny layout: 66 dp trays, 70.6 dp wide.
        val r =
            PaintRollerState().apply {
                layout(
                    393f,
                    588f,
                    480f,
                    List(5) { Rect(16f + it * 76.6f, 520f, 16f + it * 76.6f + 70.6f, 586f) },
                    List(7) { Offset(100f, 100f + it * 40f) },
                )
            }
        for ((t, b) in r.trays.zip(r.trayRects)) {
            assertTrue(t.top + t.th <= b.bottom - PaintRollerState.LabelRoom + .01f)
        }
        // Where there is room, the prototype's size.
        val roomy = roller()
        val w = roomy.trayRects[0].width - 4f
        assertEquals(minOf(w, 62f * roomy.k), roomy.trays[0].tw, .01f)
    }
}
