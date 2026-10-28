package dev.dimvlachos.lab.core.presentation.components.jackolantern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JackOLanternStateTest {
    private fun lantern(lit: Boolean = false) =
        JackOLanternState(lit).apply { layout(412f, 800f, 360f, 40f) }

    private fun JackOLanternState.run(ms: Int) {
        repeat(ms / 16) { advance(.016f) }
    }

    private fun JackOLanternState.tap(x: Float = px, y: Float = py) {
        down(x, y, 0.0)
        up(x, y, 120.0)
    }

    /** A flick through [points], [gap] ms apart, as a fast real swipe arrives: a few samples. */
    private fun JackOLanternState.flick(points: List<Pair<Float, Float>>, gap: Double = 16.0) {
        down(points[0].first, points[0].second, 0.0)
        for (i in 1 until points.size) move(points[i].first, points[i].second, i * gap)
        up(points.last().first, points.last().second, points.size * gap)
    }

    private fun JackOLanternState.across() =
        listOf(
            px - 120f to py + 40f,
            px - 70f to py + 25f,
            px - 20f to py + 10f,
            px + 30f to py,
            px + 80f to py - 10f,
        )

    @Test
    fun aTapOnThePumpkinLightsItAndTheNightSpreadsAfterTheFlameCatches() {
        val l = lantern()
        val heard = mutableListOf<LanternEvent>()
        l.onEvent = { heard += it }
        l.tap()
        assertTrue(l.lit)
        assertEquals(listOf(LanternEvent.Strike), heard)
        l.run(160)
        assertEquals(0f, l.spread(), "the light waits for the flame to catch")
        l.run(160)
        assertEquals(listOf(LanternEvent.Strike, LanternEvent.Catch), heard)
        l.run(3200)
        assertEquals(1f, l.spread())
        assertEquals(1f, l.flame())
        assertEquals(1, l.said)
    }

    @Test
    fun aTapAwayFromThePumpkinDoesNothing() {
        val l = lantern()
        l.tap(l.px - 120f, l.py + 120f)
        assertFalse(l.lit)
    }

    @Test
    fun aFastFlickOfFiveSamplesThroughTheFlameBlowsItOut() {
        val l = lantern(lit = true)
        val heard = mutableListOf<LanternEvent>()
        l.onEvent = { heard += it }
        l.flick(l.across())
        assertFalse(l.lit)
        assertEquals(listOf(LanternEvent.Blow), heard)
        l.run(300)
        assertTrue(l.puffs.any { it.live }, "smoke curls off the lid")
        l.run(3200)
        assertEquals(0f, l.spread())
        assertTrue(l.settled || l.puffs.any { it.live })
    }

    @Test
    fun aSlowDragPastTheFlameOnlyLeansIt() {
        val l = lantern(lit = true)
        l.flick(l.across(), gap = 400.0)
        assertTrue(l.lit)
        l.run(100)
        assertTrue(l.lean > 0f, "the flame leans away from the finger")
    }

    @Test
    fun aTapOnALitPumpkinShowsHowToBlowItOut() {
        val l = lantern(lit = true)
        l.tap()
        assertTrue(l.lit)
        assertTrue(l.hintOn)
        assertTrue(l.hintLit)
        l.run(2500)
        assertFalse(l.hintOn)
    }

    @Test
    fun relightingWhileTheNightDrawsBackTurnsItRoundWithNoHold() {
        val l = lantern(lit = true)
        l.flick(l.across())
        l.run(900)
        val s = l.spread()
        assertTrue(s in .2f..<.98f, "$s")
        l.tap()
        l.run(48)
        val a = l.spread()
        l.run(48)
        val b = l.spread()
        assertTrue(a >= s - .002f, "no further draw back: $s then $a")
        assertTrue(b > a, "already spreading again: $a then $b")
    }

    @Test
    fun theToggleBlowsOutWhenLitAndLightsWhenOut() {
        val l = lantern()
        l.toggle()
        assertTrue(l.lit)
        l.toggle()
        assertFalse(l.lit)
        assertEquals(2, l.said)
    }

    @Test
    fun reducedMotionSwitchesAtOnceWithNoSmoke() {
        val l = lantern(lit = true).apply { reducedMotion = true }
        l.toggle()
        l.run(32)
        assertEquals(0f, l.spread())
        assertTrue(l.puffs.none { it.live })
        l.toggle()
        l.run(32)
        assertEquals(1f, l.spread())
    }

    @Test
    fun theHintComesUpAfterAQuietWhileButNotDuringAScript() {
        val l = lantern()
        l.run(4000)
        assertTrue(l.hintOn)
        assertFalse(l.hintLit)
        val s = lantern().apply { scripted = true }
        s.run(5000)
        assertFalse(s.hintOn)
    }

    @Test
    fun setInstantPutsItLitOrOutWithNothingSaid() {
        val l = lantern()
        l.setInstant(true)
        assertTrue(l.lit)
        assertEquals(1f, l.spread())
        assertEquals(0, l.said)
    }
}
