package dev.dimvlachos.lab.core.presentation.components.sandmandala

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandMandalaStateTest {
    private val events = mutableListOf<SandEvent>()

    // The prototype's eleven visits under two headings, on a 412 × 640 dp list.
    private fun history(reduced: Boolean = false): SandMandalaState {
        val sites = intArrayOf(0, 3, 2, 1, 4, 5, 6, 7, 8, 3)
        val entries = buildList {
            add(SandEntry.Section("Today"))
            sites.take(5).forEach { add(SandEntry.Visit(SandVisit(it, "Title", "site.com"))) }
            add(SandEntry.Section("Yesterday"))
            sites.drop(5).forEach { add(SandEntry.Visit(SandVisit(it, "Title", "site.com"))) }
        }
        val s =
            SandMandalaState(
                entries,
                intArrayOf(
                    0xC2412D,
                    0x2D5FB8,
                    0x3F8F4A,
                    0xE3A126,
                    0x7A3F9A,
                    0x1F8F8A,
                    0x24345C,
                    0xD9682A,
                    0xB8325C,
                ),
            )
        s.reducedMotion = reduced
        s.onEvent = { events += it }
        assertTrue(s.layout(412f, 640f, dark = false, short = false))
        // Words: a 200 × 6 dp band of ink in each row.
        val w = 412
        val h = 640
        val text = IntArray(w * h)
        for (v in s.visits.indices) {
            val y0 = s.rowTop(v).toInt() + 14
            for (y in y0 until y0 + 6) for (x in 64 until 264) text[y * w + x] = 0xFF202227.toInt()
        }
        s.build(text, w, h)
        return s
    }

    private fun SandMandalaState.run(seconds: Float) {
        var t = 0f
        while (t < seconds) {
            step(1f / 60f)
            t += 1f / 60f
        }
    }

    // A stroke of five sparse moves from [x0], [y0] to [x1], [y1], as a fast real swipe sends.
    private fun SandMandalaState.stroke(x0: Float, y0: Float, x1: Float, y1: Float) {
        brushDown(x0, y0)
        for (k in 1..5) brushMove(x0 + (x1 - x0) * k / 5f, y0 + (y1 - y0) * k / 5f)
        brushUp()
    }

    @Test
    fun theRngIsTheProtoypesParkMiller() {
        val rr = SandMandalaState.parkMiller(1234)
        assertEquals(20739838.0 / 2147483647.0, rr(), 1e-12)
    }

    @Test
    fun theDesignHasACentreACreamRingAndAPalaceGate() {
        assertEquals(0, SandMandalaState.design(0.0, 0.0))
        assertEquals(SandMandalaState.Line, SandMandalaState.design(.945, 0.0))
        assertEquals(0, SandMandalaState.design(.97, 0.0))
        assertEquals(0, SandMandalaState.design(.5, 0.0))
        assertEquals(5, SandMandalaState.design(.52, .3))
    }

    @Test
    fun theMandalaIsPlacedAndFilledWithGrains() {
        val s = history()
        assertEquals(166f, s.r)
        assertEquals(206f, s.cx)
        assertTrue(s.n > 20_000, "${s.n}")
        assertTrue(s.gt in 2f..4f, "${s.gt}")
        assertEquals(SandAt.Rows, s.at)
        assertFalse(s.needsText)
    }

    @Test
    fun clearGathersIntoTheMandalaAndKeepSendsItHome() {
        val s = history()
        s.clearHistory()
        assertEquals(SandPhase.Gather, s.ph)
        assertNull(s.at)
        assertTrue(s.keeps)
        s.run(s.gt + .1f)
        assertEquals(SandAt.Mandala, s.at)
        assertTrue(s.hintOn)
        assertEquals(listOf(SandEvent.Formed), events)
        s.keep()
        s.run(s.gt + .1f)
        assertEquals(SandAt.Rows, s.at)
        assertEquals(SandEvent.Kept, events.last())
    }

    @Test
    fun keepPartWayTurnsTheGatherRound() {
        val s = history()
        s.clearHistory()
        s.run(1f)
        s.tapClear()
        assertEquals(-1, s.dir)
        s.run(1.2f)
        assertEquals(SandAt.Rows, s.at)
    }

    @Test
    fun aSparseSweepLetsTheMandalaGoAndUndoBringsItAllBack() {
        val s = history()
        s.clearHistory()
        s.run(s.gt + .1f)
        s.stroke(s.cx - s.r * .8f, s.cy - s.r * .5f, s.cx + s.r * .8f, s.cy + s.r * .42f)
        assertEquals(SandPhase.Sweep, s.ph)
        assertEquals(SandEvent.Swept, events.last())
        s.run(s.st + .5f + s.pt + .2f)
        assertEquals(SandAt.Cleared, s.at)
        assertTrue(s.snackOn)
        assertEquals(SandEvent.Cleared, events.last())
        s.undo()
        assertFalse(s.snackOn)
        s.run(s.pt + s.st + .8f + s.gt + .3f)
        assertEquals(SandAt.Rows, s.at)
        assertEquals(SandEvent.Restored, events.last())
    }

    @Test
    fun aShortSmudgeOnlyPushesTheSand() {
        val s = history()
        s.clearHistory()
        s.run(s.gt + .1f)
        s.stroke(s.cx - 20f, s.cy, s.cx + 30f, s.cy)
        assertEquals(SandAt.Mandala, s.at)
        assertTrue((0 until s.n).any { s.dx[it] != 0f })
    }

    @Test
    fun aBrushSidewaysAcrossTheRowsStartsTheClear() {
        val s = history()
        s.stroke(300f, 100f, 200f, 104f)
        assertEquals(SandPhase.Gather, s.ph)
        assertEquals(SandEvent.Brushed, events.first())
    }

    @Test
    fun theSnackbarGoesAfterEightSecondsAndLeavesTheRestoreLink() {
        val s = history(reduced = true)
        s.clearHistory()
        assertEquals(SandAt.Mandala, s.at)
        s.commit(1)
        assertEquals(SandAt.Cleared, s.at)
        assertTrue(s.snackOn)
        s.run(8.1f)
        assertFalse(s.snackOn)
        assertTrue(s.restoreOn)
        s.undo()
        assertEquals(SandAt.Rows, s.at)
        assertEquals(SandEvent.Restored, events.last())
    }
}
