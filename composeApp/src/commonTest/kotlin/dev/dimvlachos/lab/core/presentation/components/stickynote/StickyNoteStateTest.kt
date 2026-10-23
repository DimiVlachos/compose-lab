package dev.dimvlachos.lab.core.presentation.components.stickynote

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The prototype's selftest, run on the model. */
class StickyNoteStateTest {
    private val items = List(6) { Reminder("Note $it", "Meta $it", it) }
    private val events = mutableListOf<StickyNoteEvent>()
    private var now = 0.0

    // A Pixel 9 Pro's stage under the demo screen's bar, with the app bar's foot at 66.
    private fun notes(w: Float = 412f, h: Float = 760f) =
        StickyNoteState(items, Random(3)).also { s ->
            s.layout(w, h, 76f)
            s.onEvent = { e, _, _ -> events += e }
        }

    private fun StickyNoteState.run(ms: Int) {
        var left = ms
        while (left > 0) {
            step(16 / 1000f)
            now += 16
            left -= 16
        }
    }

    private fun StickyNoteState.swipe(i: Int, by: Float, ms: Int) {
        val x0 = if (by < 0) width - mx - 50 else mx + 50
        val y = slotY(i) + rh * .5f
        val n = max(4, (ms / 16f).roundToInt())
        down(x0, y, now)
        for (k in 1..n) {
            run(16)
            move(x0 + by * k / n, y, now)
        }
        now += 8
        up()
    }

    private fun StickyNoteState.tap(x: Float, y: Float) {
        down(x, y, now)
        run(60)
        up()
    }

    @Test
    fun aShortSwipeSpringsBackAndSmoothsDown() {
        val s = notes()
        s.swipe(0, -70f, 320)
        s.run(150)
        assertTrue(s.rows[0].state == RowState.Return || s.rows[0].state == RowState.Rest)
        s.run(800)
        assertEquals(6, s.rows.size)
        assertEquals(0f, s.rows[0].dx)
        assertEquals(RowState.Rest, s.rows[0].state)
        assertTrue(StickyNoteEvent.Pat in events)
    }

    @Test
    fun aLongLeftSwipeArchivesToDoneAndRightSnoozesToLater() {
        val s = notes()
        val first = s.rows[0].item
        s.swipe(0, -240f, 480)
        s.run(1300)
        assertEquals(5, s.rows.size)
        assertEquals(listOf(first), s.archive.stack.map { it.item })
        s.swipe(0, 240f, 480)
        s.run(1300)
        assertEquals(4, s.rows.size)
        assertEquals(1, s.snooze.stack.size)
        assertTrue(StickyNoteEvent.Tick in events)
        assertEquals(2, events.count { it == StickyNoteEvent.Rip })
        assertEquals(2, events.count { it == StickyNoteEvent.Stick })
        assertTrue(s.settled)
    }

    @Test
    fun tappingDonePutsItBackInPlace() {
        val s = notes()
        val first = s.rows[0].item
        s.swipe(0, -240f, 480)
        s.run(1300)
        s.tap(s.padCenterX(NoteAction.Archive), s.padCenterY())
        s.run(1200)
        assertEquals(6, s.rows.size)
        assertTrue(s.archive.stack.isEmpty())
        assertEquals(first, s.rows[0].item)
        assertFalse(s.rows[0].hidden)
    }

    @Test
    fun aFastFlickPeelsItOffFromAShortPull() {
        val s = notes()
        s.swipe(1, -70f, 60)
        s.run(1300)
        assertEquals(1, s.archive.stack.size)
        assertEquals(5, s.rows.size)
    }

    @Test
    fun aKeyCommitsAndALandedNoteKeepsItsFlightAngle() {
        val s = notes()
        repeat(3) {
            s.commit(s.rows[0], NoteAction.Archive, fromKey = true)
            s.run(1300)
        }
        assertEquals(3, s.rows.size)
        assertTrue(s.archive.stack.all { abs(it.rot) in .02f..0.05f && it.rot < 0 })
        assertTrue(s.archive.stack.all { abs(it.ox) <= 5f })
    }

    @Test
    fun flightsStayInsideTheSideMarginsAndMeetThePadAtItsAngleAndOffset() {
        for ((w, h) in listOf(412f to 760f, 360f to 640f, 412f to 915f)) {
            val s = notes(w, h)
            for (kind in NoteAction.entries) {
                for (out in listOf(true, false)) {
                    val sign = kind.sign
                    val row = NoteRow(items[1], s.slotY(1))
                    val f =
                        Flight(
                            items[1],
                            kind,
                            out,
                            640f,
                            s.slotY(1),
                            sign * s.noteW * .62f,
                            sign * .05f,
                            sign * 5f,
                            1,
                            if (out) null else row,
                        )
                    var e = 0f
                    while (e <= 1.0001f) {
                        val p = s.flightPose(f, e)
                        assertTrue(
                            p.cx - p.hw >= p.lo - .01f && p.cx + p.hw <= p.hi + .01f,
                            "$kind $out $e",
                        )
                        assertTrue(p.lo >= s.mx - 8 && p.hi <= w - s.mx + 8, "$kind $out bounds $e")
                        e += .02f
                    }
                    val end = s.flightPose(f, if (out) 1f else 0f)
                    assertEquals(s.padCenterX(kind) + f.ox, end.cx, .01f)
                    assertEquals(s.padTopY(kind), end.cy, .01f)
                    assertEquals(f.rot, end.rot, 1e-6f)
                }
            }
        }
    }

    @Test
    fun padsSitJustUnderTheListAndRowsGrowOnTallScreens() {
        for (h in listOf(760f, 915f)) {
            val s = notes(412f, h)
            val band = s.padY() - 20 - (s.slotY(6) - s.gap)
            assertTrue(band in 10f..40.5f, "band $band at $h")
            assertTrue(s.rh in 54f..96f)
        }
        assertEquals(96f, notes(412f, 1100f).rh)
    }

    @Test
    fun theDemosTwoNotesGoBackInOrderWhenLaterIsTappedBeforeDone() {
        val s = notes()
        val order = s.rows.map { it.item }
        s.swipe(1, -214f, 780)
        s.run(1150)
        s.swipe(2, 214f, 780)
        s.run(1150)
        assertEquals(4, s.rows.size)
        s.tap(s.padCenterX(NoteAction.Snooze), s.padCenterY())
        s.run(900)
        s.tap(s.padCenterX(NoteAction.Archive), s.padCenterY())
        // The last note's flutter dies away in about 1.5 s.
        s.run(2400)
        assertEquals(order, s.rows.map { it.item })
        assertTrue(
            s.settled,
            "${s.flights.size} ${s.ghosts.size} ${s.archive.bump} ${s.snooze.bump} " +
                s.rows.joinToString { "${it.state} ${it.flut} ${it.y} ${it.ty}" },
        )
    }

    @Test
    fun tappingANoteLiftsItsCornerAndLaysItDown() {
        val s = notes()
        val r = s.rows[2]
        s.tap(s.mx + 40, s.slotY(2) + 10)
        s.run(200)
        assertTrue(r.dx < -10f, "dx ${r.dx}")
        s.run(600)
        assertEquals(0f, r.dx)
        assertEquals(RowState.Rest, r.state)
        assertTrue(StickyNoteEvent.Peek in events)
    }

    @Test
    fun reducedMotionCommitsAndLandsAtOnce() {
        val s = notes()
        s.reducedMotion = true
        s.commit(s.rows[0], NoteAction.Snooze, fromKey = true)
        assertEquals(1, s.snooze.stack.size)
        assertTrue(s.flights.isEmpty())
        s.undo(NoteAction.Snooze)
        assertEquals(6, s.rows.size)
        assertFalse(s.rows[0].hidden)
        s.run(16)
        assertTrue(s.settled)
    }

    @Test
    fun theScriptsHandNeverTakesOverAFingersHold() {
        val s = notes()
        val x = s.width - s.mx - 5
        s.down(x, s.slotY(0) + 40, now)
        s.move(x - 20, s.slotY(0) + 40, now + 30)
        s.move(x - 60, s.slotY(0) + 40, now + 60)
        val dx = s.rows[0].dx
        s.down(x, s.slotY(1) + 40, now + 70, scripted = true)
        s.move(x - 200, s.slotY(1) + 40, now + 80, scripted = true)
        assertEquals(dx, s.rows[0].dx)
        assertEquals(0f, s.rows[1].dx)
        assertFalse(s.scriptedTouch)
    }

    @Test
    fun aScriptedHoldIsLetGoButARealOneIsNot() {
        val s = notes()
        s.down(s.width - s.mx - 5, s.slotY(0) + 40, now, scripted = true)
        s.move(s.width - s.mx - 60, s.slotY(0) + 40, now + 100, scripted = true)
        assertTrue(s.scriptedTouch)
        s.releaseScripted()
        assertFalse(s.scriptedTouch)
        assertEquals(RowState.Return, s.rows[0].state)
        s.down(s.width - s.mx - 5, s.slotY(1) + 40, now)
        s.releaseScripted()
        s.move(s.width - s.mx - 20, s.slotY(1) + 40, now + 50)
        s.move(s.width - s.mx - 60, s.slotY(1) + 40, now + 100)
        assertTrue(s.rows[1].dx < -40f)
    }
}
