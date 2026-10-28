package dev.dimvlachos.lab.core.presentation.components.graveundo

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The prototype's selftest and the audit's fixes, run on the model. */
class GraveUndoStateTest {
    private val tasks = List(7) { GraveTask("Task $it", "Sub $it", "${it + 9} AM") }
    private val events = mutableListOf<GraveEvent>()
    private var now = 0.0

    // A Pixel 9 Pro's stage under the demo screen's bar: the list starts under the bakery's header.
    private fun list(w: Float = 412f, h: Float = 820f) =
        GraveUndoState(tasks, Random(3)).also { s ->
            s.layout(w, h, 130f, 0f, 1f)
            s.onEvent = { e, _ -> events += e }
        }

    private fun GraveUndoState.run(ms: Int) {
        var left = ms
        while (left > 0) {
            step(16 / 1000f)
            now += 16
            left -= 16
        }
    }

    // Five sparse moves, as a fast real swipe or the extension's drag sends.
    private fun GraveUndoState.swipe(i: Int, by: Float, ms: Int = 300) {
        val it = items[i]
        val x0 = width * .7f
        val y = rowTop(it) + rowH / 2
        down(x0, y, now)
        for (k in 1..5) {
            run(ms / 5)
            move(x0 + by * k / 5, y + k, now)
        }
        up()
    }

    private fun GraveUndoState.until(ms: Int, done: GraveUndoState.() -> Boolean) {
        var left = ms
        while (left > 0 && !done()) {
            run(16)
            left -= 16
        }
    }

    @Test
    fun aSwipePastTheLineBuriesTheTaskAndTheHandComesUpHoldingIt() {
        val s = list()
        s.swipe(2, -170f)
        assertEquals(GravePhase.Fly, s.graves.single().phase)
        assertEquals(6, s.liveCount)
        assertTrue(GraveEvent.Arm in events && GraveEvent.Buried in events)
        s.until(8000) { graves.any { it.phase == GravePhase.Held } }
        assertEquals(GravePhase.Held, s.graves.single().phase)
        assertTrue(GraveEvent.Thud in events && GraveEvent.Rise in events)
        assertTrue(s.handHitOn)
        assertEquals(1f, s.pillV, 1e-3f)
        assertEquals(1f, s.items[2].collapse, 1e-3f)
    }

    @Test
    fun tappingTheHandPutsTheTaskBackWhereItWas() {
        val s = list()
        s.swipe(2, -170f)
        s.until(8000) { graves.any { it.phase == GravePhase.Held } }
        assertTrue(s.undo())
        assertEquals(GraveEvent.Restored, events.last())
        s.until(6000) { graves.isEmpty() && pillV == 0f }
        assertEquals(7, s.liveCount)
        assertEquals(0f, s.items[2].collapse)
        assertFalse(s.items[2].faceHidden)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), s.liveItems.map { it.index })
        assertTrue(s.asBegun)
    }

    @Test
    fun aShortSwipeSpringsBack() {
        val s = list()
        s.swipe(0, -60f)
        assertTrue(s.graves.isEmpty())
        s.run(800)
        assertEquals(0f, s.items[0].dx)
        assertEquals(0, s.items[0].side)
        assertEquals(7, s.liveCount)
    }

    @Test
    fun aFlickBuriesEvenShortOfTheLine() {
        val s = list()
        s.swipe(1, -80f, ms = 60)
        assertEquals(1, s.graves.size)
    }

    @Test
    fun leftAloneTheGraveSinksAndTheTaskIsGone() {
        val s = list()
        s.swipe(0, 170f)
        s.until(20000) { graves.isEmpty() && pillV == 0f }
        assertTrue(s.items[0].gone)
        assertEquals(6, s.liveCount)
        assertNull(s.lastGone)
    }

    // The audit's fix 2: burying a second task while the first sinks on its own leaves one name.
    @Test
    fun aSecondBurialWhileTheFirstSinksClearsTheFirstCarvingFast() {
        val s = list()
        s.swipe(0, -170f)
        s.until(20000) { graves.any { it.phase == GravePhase.Leave } }
        s.run(300)
        s.swipe(0, -170f)
        val first = s.graves.first { it.item === s.items[0] }
        s.run(1100)
        assertTrue(first.engr < .05f || first.phase == GravePhase.Gone, "${first.engr}")
        s.until(4000) { graves.last().engr >= 1f }
        assertEquals(1, s.graves.count { it.engr > .05f })
    }

    @Test
    fun aVerticalDragOnAShortScreenScrollsTheListInstead() {
        val s = list(w = 800f, h = 400f)
        val y = s.rowTop(s.items[1]) + 10
        s.down(400f, y, now)
        s.move(400f, y - 60, now + 16)
        s.up()
        assertTrue(s.scroll > 0f)
        assertTrue(s.graves.isEmpty())
    }

    @Test
    fun aRealTouchLetsGoOfTheScriptsDrag() {
        val s = list()
        val y = s.rowTop(s.items[0]) + 20
        s.down(300f, y, now, scripted = true)
        s.move(200f, y, now + 50, scripted = true)
        assertEquals(-100f, s.items[0].dx)
        s.down(300f, s.rowTop(s.items[3]) + 20, now + 60)
        s.run(800)
        assertEquals(0f, s.items[0].dx)
        assertTrue(s.graves.isEmpty())
    }

    @Test
    fun aTapOnARowIsATapNotADrag() {
        val s = list()
        val y = s.rowTop(s.items[4]) + 20
        s.down(30f, y, now)
        assertTrue(s.up())
        assertNotNull(s.tapped(30f, y))
        s.toggleDone(s.items[4])
        assertTrue(s.items[4].done)
        assertFalse(s.asBegun)
    }

    @Test
    fun withReducedMotionTheGraveJumpsToEachRestingState() {
        val s = list()
        s.reducedMotion = true
        s.swipe(0, -170f)
        s.run(100)
        assertEquals(GravePhase.Held, s.graves.single().phase)
        assertEquals(0, s.crumbs.n)
    }

    @Test
    fun undoAfterARowAboveHasGoneFliesHomeToWhereTheRowIsNow() {
        val s = list()
        s.swipe(0, -170f)
        s.until(20000) { graves.isEmpty() && pillV == 0f }
        assertTrue(s.items[0].gone)
        s.swipe(3, -170f)
        s.until(8000) { graves.any { it.phase == GravePhase.Held } }
        assertTrue(s.undo())
        val g = s.graves.single()
        assertEquals(s.rowTop(s.items[3]) + s.rowH / 2, g.toY, .01f)
        s.until(6000) { graves.isEmpty() }
        assertEquals(listOf(1, 2, 3, 4, 5, 6), s.liveItems.map { it.index })
        assertEquals(0f, s.items[3].collapse)
    }

    @Test
    fun noUndoOnceTheHandHasSunkAfterTheTimeout() {
        val s = list()
        s.swipe(1, -170f)
        s.until(20000) { graves.any { it.phase == GravePhase.Leave } }
        assertFalse(s.undo())
        assertNull(s.undoable())
        s.until(6000) { graves.isEmpty() }
        assertTrue(s.items[1].gone)
    }

    @Test
    fun aSecondBurialWhileTheFirstHandIsUpSendsTheFirstForGood() {
        val s = list()
        s.swipe(0, -170f)
        s.until(8000) { graves.any { it.phase == GravePhase.Held } }
        s.swipe(2, 170f)
        val first = s.graves.first { it.item === s.items[0] }
        assertEquals(GravePhase.Leave, first.phase)
        s.until(8000) { graves.size == 1 && graves[0].phase == GravePhase.Held }
        assertTrue(s.items[0].gone)
        assertEquals(s.items[2], s.undoable()?.item)
        assertEquals(5, s.liveCount)
    }
}
