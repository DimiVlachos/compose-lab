package dev.dimvlachos.lab.core.presentation.components.pencilcup

import kotlin.math.hypot
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
    fun pickingFliesTheToolOffAndDoneDropsItBackFromAbove() {
        val s = cup()
        val events = mutableListOf<CupEvent>()
        s.onEvent = { e, _, _ -> events += e }
        s.tap()
        s.run(1600) { s.fanned() }
        s.pick(CupTools.List)
        assertTrue(s.sheetOpen)
        assertEquals(CupTools.List, s.sheetTool)
        assertTrue(CupEvent.Pick in events)
        assertTrue(
            s.run(2500) {
                s.tools[CupTools.List].mode == ToolMode.Away &&
                    listOf(0, 1, 3).all { s.tools[it].mode == ToolMode.Rest }
            },
        )
        assertEquals(CupTools.List, s.done())
        assertFalse(s.sheetOpen)
        s.run(300)
        // It starts above the screen.
        assertTrue(s.tools[CupTools.List].y < 60f, "${s.tools[CupTools.List].y}")
        assertTrue(s.run(3000) { s.allRest() })
        assertTrue(CupEvent.Clink in events)
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
        assertEquals(ToolMode.Away, s.tools[CupTools.Note].mode)
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
        s.reset()
        assertFalse(s.isOpen)
        assertFalse(s.sheetOpen)
        assertTrue(s.allRest())
        assertTrue(s.settled)
    }
}
