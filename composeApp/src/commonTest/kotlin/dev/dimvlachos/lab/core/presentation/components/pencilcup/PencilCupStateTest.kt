package dev.dimvlachos.lab.core.presentation.components.pencilcup

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PencilCupStateTest {
    private fun cup(reduced: Boolean = false) =
        PencilCupState().apply {
            reducedMotion = reduced
            layout(412f, 800f)
            reset()
            for (i in 0 until CupTools.count) setLabelSize(i, 110f, 34f)
        }

    /** Steps [ms] at 60 fps, stopping early once [until] holds; returns whether it did. */
    private fun PencilCupState.run(ms: Int, until: () -> Boolean = { false }): Boolean {
        var t = 0
        while (t < ms) {
            if (until()) return true
            step(1f / 60f)
            t += 16
        }
        return until()
    }

    private fun PencilCupState.allRest() = tools.all { it.mode == ToolMode.Rest }

    private fun PencilCupState.fanned() =
        tools.withIndex().all { (i, t) ->
            t.mode == ToolMode.Fan && hypot(t.x - fanX(i), t.y - fanY(i)) < 6 * k
        }

    private fun PencilCupState.tap() {
        down(cupPoint.x, cupPoint.y)
        step(.06f)
        up(cupPoint.x, cupPoint.y)
    }

    @Test
    fun startsClosedWithEveryToolInTheCup() {
        val s = cup()
        assertFalse(s.isOpen)
        assertTrue(s.allRest())
        assertTrue(s.settled)
    }

    @Test
    fun aTapOpensTheFanAndTheLabelsWaitForTheirTools() {
        val s = cup()
        s.tap()
        s.run(400) { s.tools[CupTools.Note].mode == ToolMode.Fan }
        assertEquals(ToolMode.Fan, s.tools[CupTools.Note].mode)
        assertFalse(s.labelShown(CupTools.Note))
        assertTrue(s.run(1600) { s.fanned() })
        assertTrue(s.isOpen)
        s.run(300)
        assertTrue((0 until CupTools.count).all { s.labelShown(it) })
        // Once the fan has arrived it holds still, so the frame loop can sleep.
        assertTrue(s.run(3000) { s.settled })
    }

    @Test
    fun aSecondTapDropsThemBackWithAClatter() {
        val s = cup()
        s.tap()
        s.run(1600) { s.fanned() }
        val c0 = s.clatters
        s.tap()
        assertFalse(s.isOpen)
        assertTrue(s.run(2500) { s.allRest() })
        assertTrue(s.clatters >= c0 + 3, "${s.clatters}")
        assertTrue(s.run(3000) { s.settled })
    }

    @Test
    fun theLoupeStandsInsideTheMouthWithItsLensAboveTheRim() {
        val s = cup()
        val i = CupTools.Scan
        val a = s.restA(i)
        val l = CupTools.specs[i].length * s.k
        // Its lens, with the gold rim, and where its handle crosses the rim's height.
        val lensY = s.restY(i) - cos(a) * (l - 20 * s.k)
        val r = 21.6f * s.k
        assertTrue(lensY + r < s.rimY - s.rimDepth - s.k, "${lensY + r} vs ${s.rimY - s.rimDepth}")
        val handleX = s.restX(i) + sin(a) * (s.restY(i) - s.rimY) / cos(a)
        assertTrue(abs(handleX - s.cupX) + 6 * s.k < s.topWidth / 2 - 4 * s.k, "$handleX")
    }

    @Test
    fun pickingTucksTheToolIntoAPocketOnTheSheetAndDoneArcsItIntoTheCup() {
        val s = cup()
        val events = mutableListOf<CupEvent>()
        s.onEvent = { e, _, _ -> events += e }
        s.tap()
        s.run(1600) { s.fanned() }
        s.pick(CupTools.List)
        assertTrue(s.sheetOpen)
        assertEquals(CupTools.List, s.sheetTool)
        assertTrue(CupEvent.Pick in events)
        val t = s.tools[CupTools.List]
        val l = CupTools.specs[CupTools.List].length * s.k
        assertTrue(
            s.run(2500) {
                t.mode == ToolMode.Pocket &&
                    t.depth > l * PencilCupState.PocketIn - .5f &&
                    listOf(0, 1, 3).all { s.tools[it].mode == ToolMode.Rest }
            },
        )
        // The sheet is up, 64 % of the screen; half the tool is behind its top edge, half above, at
        // the top-left corner.
        val top = s.sheetTop()
        assertEquals(800f * .36f, top, .5f)
        assertTrue(t.tuck)
        assertTrue(t.y > top + 20 * s.k)
        assertTrue(t.y - cos(t.a) * l < top - 20 * s.k)
        assertEquals(PencilCupState.PocketX * s.k, t.x + sin(t.a) * (t.y - top) / cos(t.a), .5f)
        assertTrue(s.run(3000) { s.settled })
        val fromX = t.x
        val fromY = t.y
        assertEquals(CupTools.List, s.done())
        assertFalse(s.sheetOpen)
        // It lifts off at once, from where it sat.
        assertEquals(ToolMode.Arc, t.mode)
        assertEquals(fromX, t.x)
        assertEquals(fromY, t.y)
        var highest = Float.MAX_VALUE
        assertTrue(
            s.run(1500) {
                highest = minOf(highest, t.y)
                t.mode == ToolMode.Home
            },
        )
        // An arc up off the sheet, never from above the screen.
        assertTrue(highest > 0f && highest < fromY, "$highest")
        assertFalse(t.tuck)
        assertTrue(s.run(3000) { s.allRest() })
        assertTrue(CupEvent.Clink in events)
        assertTrue(s.sheetTop() >= 800f * 1.01f)
    }

    @Test
    fun pressAndReleaseOnAToolPicksItWithNoMoves() {
        val s = cup()
        s.down(s.cupPoint.x, s.cupPoint.y)
        s.step(.04f)
        val at = s.alongTool(CupTools.Note, .7f)
        s.up(at.x, at.y)
        assertTrue(s.sheetOpen)
        assertEquals(CupTools.Note, s.sheetTool)
    }

    @Test
    fun aDragLightsTheToolUnderTheFingerAndReleasingOnItsLabelPicksIt() {
        val s = cup()
        val lx = s.labelRight(CupTools.Clip) - 55f
        val ly = s.labelY(CupTools.Clip)
        s.down(s.cupPoint.x, s.cupPoint.y)
        s.step(.04f)
        s.move(lx, ly)
        assertEquals(CupTools.Clip, s.hover)
        s.up(lx, ly)
        assertEquals(-1, s.hover)
        assertEquals(CupTools.Clip, s.sheetTool)
        assertTrue(s.sheetOpen)
    }

    @Test
    fun draggingOutAndBackOntoTheCupClosesIt() {
        val s = cup()
        val (cx, cy) = s.cupPoint
        s.down(cx, cy)
        s.step(.04f)
        s.move(cx - 40 * s.k, cy - 90 * s.k)
        assertTrue(s.isOpen)
        s.run(200)
        s.up(cx, cy)
        assertFalse(s.isOpen)
        assertTrue(s.run(2500) { s.allRest() })
    }

    @Test
    fun aTapOnThePageClosesTheFan() {
        val s = cup()
        s.tap()
        s.run(1600) { s.fanned() }
        s.tapScrim(40f, 120f)
        assertFalse(s.isOpen)
        assertFalse(s.sheetOpen)
    }

    @Test
    fun theScissorsNeverLeanPastTheCupWall() {
        val s = cup()
        s.tap()
        s.run(1600) { s.fanned() }
        s.tap()
        var worst = 0f
        s.run(3000) {
            val t = s.tools[CupTools.Clip]
            if (t.mode == ToolMode.Rest || t.mode == ToolMode.Home) worst = maxOf(worst, t.a)
            false
        }
        assertTrue(worst <= .3f + 1e-4f, "$worst")
    }

    // Uneven frames, so substeps end at every phase of a fall.
    private val unevenFrames = floatArrayOf(1 / 60f, .0173f, 1 / 144f, .0211f, 1 / 50f, .0071f)

    /**
     * Steps uneven frames until every tool rests, failing if tool [i] sinks below where it rests,
     * or if its foot below the rim, or its centre line below the front lip, leaves the mouth.
     */
    private fun PencilCupState.dropsInThroughTheMouth(i: Int, phase: Int) {
        val t = tools[i]
        var n = 0
        while (n < 400 && !allRest()) {
            step(unevenFrames[(phase + n) % unevenFrames.size])
            n++
            if (t.mode != ToolMode.Home && t.mode != ToolMode.Rest) continue
            assertTrue(t.y <= restY(i) + 1f, "tool $i sank to ${t.y - restY(i)} below its rest")
            val len = CupTools.specs[i].length * k
            var d = 0f
            while (d <= len) {
                val x = t.x + sin(t.a) * d
                val y = t.y - cos(t.a) * d
                if (y <= rimY + if (d > 0f) rimDepth else 0f) break
                val u = ((y - rimY) / (botY - rimY)).coerceIn(0f, 1f)
                val half = (topWidth + (footWidth - topWidth) * u) / 2
                assertTrue(
                    abs(x - cupX) <= half - if (d > 0f) 6 * k else 0f,
                    "tool $i at $d along it is ${abs(x - cupX) - half} past the wall",
                )
                d += 4 * k
            }
        }
        assertTrue(allRest())
    }

    @Test
    fun everyToolDropsBackInThroughTheMouth() {
        for (phase in unevenFrames.indices) {
            val s = cup()
            s.tap()
            s.run(1600) { s.fanned() }
            s.tap()
            s.dropsInThroughTheMouth(CupTools.Clip, phase)
        }
    }

    @Test
    fun aToolArcingBackFromItsSheetFallsStraightInAndNeverSinks() {
        for (i in 0 until CupTools.count) {
            for (phase in unevenFrames.indices) {
                val s = cup()
                s.tap()
                s.run(1600) { s.fanned() }
                s.pick(i)
                s.run(1500) { s.tools[i].mode == ToolMode.Pocket && s.allRestBut(i) }
                s.done()
                s.dropsInThroughTheMouth(i, phase)
            }
        }
    }

    private fun PencilCupState.allRestBut(i: Int) =
        tools.withIndex().all { (j, t) -> j == i || t.mode == ToolMode.Rest }

    @Test
    fun reducedMotionJumpsToEachPose() {
        val s = cup(reduced = true)
        s.tap()
        assertTrue(s.fanned())
        assertTrue((0 until CupTools.count).all { s.labelShown(it) })
        s.tap()
        assertTrue(s.allRest())
        s.pick(CupTools.Note)
        s.run(600)
        assertEquals(ToolMode.Pocket, s.tools[CupTools.Note].mode)
        assertEquals(0f, s.sheetSlide)
        s.done()
        s.step(.016f)
        assertTrue(s.allRest())
    }

    @Test
    fun resetPutsEverythingBack() {
        val s = cup()
        s.tap()
        s.run(500)
        s.pick(CupTools.Scan)
        s.run(200)
        s.reset()
        assertEquals(PencilCupState.SheetDown, s.sheetSlide)
        assertFalse(s.isOpen)
        assertFalse(s.sheetOpen)
        assertTrue(s.allRest())
        assertTrue(s.settled)
    }
}
