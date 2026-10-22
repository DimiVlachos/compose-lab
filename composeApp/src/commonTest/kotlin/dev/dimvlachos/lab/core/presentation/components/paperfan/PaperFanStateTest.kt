package dev.dimvlachos.lab.core.presentation.components.paperfan

import dev.dimvlachos.lab.core.presentation.components.paperfan.PaperFanState.Companion.Ribs
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaperFanStateTest {
    private val events = mutableListOf<Pair<PaperFanEvent, Int>>()

    private fun fans(reduced: Boolean = false) =
        PaperFanState(5).apply {
            reducedMotion = reduced
            onEvent = { e, i -> events += e to i }
        }

    /** Runs [ms] at 60 frames a second, calling [each] after every frame. */
    private fun PaperFanState.run(ms: Int, each: () -> Unit = {}) {
        repeat(ms * 60 / 1000) {
            step(1f / 60f)
            each()
        }
    }

    @Test
    fun allFansStartFoldedAndAtRest() {
        val f = fans()
        for (i in 0 until 5) {
            assertFalse(f.isOpen(i))
            assertEquals(0f, f.heightFraction(i))
            assertEquals(PaperFanState.Rest, f.spread(i, Ribs))
        }
        assertTrue(f.settled)
    }

    @Test
    fun aTapOpensTheFanAndItsAnswer() {
        val f = fans()
        f.toggle(1)
        f.run(1400)
        assertTrue(f.isOpen(1))
        assertTrue(f.travel(1) > .95f, "travel ${f.travel(1)}")
        assertTrue(f.spread(1, Ribs) > .95f)
        assertTrue(f.spread(1, 1) > .9f)
        assertTrue(f.heightFraction(1) > .95f)
        assertEquals(
            listOf(PaperFanEvent.Fwap to 1, PaperFanEvent.Click to 1),
            events,
        )
    }

    @Test
    fun openingAnotherFoldsTheFirstAndTheAnswersNeverTotalMoreThanOnePanel() {
        val f = fans()
        f.toggle(1)
        f.run(1400)
        events.clear()
        var most = 0f
        var fwapAtFrame = -1
        var frame = 0
        f.toggle(3)
        // The new fan waits for the old one to fold before it lifts, and only then fwaps.
        assertEquals(listOf(PaperFanEvent.Swish to 1), events)
        f.run(1600) {
            frame++
            if (fwapAtFrame < 0 && PaperFanEvent.Fwap to 3 in events) fwapAtFrame = frame
            most = maxOf(most, (0 until 5).sumOf { f.heightFraction(it).toDouble() }.toFloat())
        }
        assertTrue(fwapAtFrame > 3, "fwap at frame $fwapAtFrame")
        assertFalse(f.isOpen(1))
        assertTrue(f.travel(1) < .05f)
        assertEquals(0f, f.heightFraction(1))
        assertTrue(f.isOpen(3) && f.spread(3, Ribs) > .95f)
        assertTrue(most <= 1.003f, "most $most")
        assertTrue(PaperFanEvent.Clack to 1 in events)
    }

    @Test
    fun theFingerHoldsTheLeadingGuardAndPastHalfFoldsItShut() {
        val f = fans()
        f.toggle(3)
        f.run(1400)
        assertTrue(f.dragStart(3))
        for (n in 1..10) {
            f.dragTo(3, 1f - .085f * n)
            f.run(40)
            // The guard is held right under the finger, never trailing it.
            assertEquals(1f - .085f * n, f.spread(3, Ribs), 1e-5f)
        }
        assertTrue(f.spread(3, Ribs) < .4f)
        events.clear()
        f.dragEnd(3, moved = true)
        f.run(1500)
        assertFalse(f.isOpen(3))
        assertTrue(f.travel(3) < .05f)
        assertEquals(PaperFanEvent.Swish to 3, events.first())
    }

    @Test
    fun aShortPushSpringsBackOpen() {
        val f = fans()
        f.toggle(0)
        f.run(1400)
        assertTrue(f.dragStart(0))
        f.dragTo(0, .65f)
        f.run(160)
        f.dragEnd(0, moved = true)
        f.run(1200)
        assertTrue(f.isOpen(0))
        assertTrue(f.spread(0, Ribs) > .93f)
    }

    @Test
    fun aHeldFanFoldedByATapElsewhereIsLetGoAndTheAnswersStillTrade() {
        val f = fans()
        f.toggle(1)
        f.run(1400)
        assertTrue(f.dragStart(1))
        assertFalse(f.dragStart(1))
        events.clear()
        f.toggle(3)
        assertFalse(f.dragging(1))
        var most = 0f
        f.run(1600) { most = maxOf(most, f.heightFraction(1) + f.heightFraction(3)) }
        assertTrue(most <= 1.003f, "most $most")
        f.dragEnd(1, moved = true)
        assertEquals(1, events.count { it == PaperFanEvent.Swish to 1 })
    }

    @Test
    fun aFanStillOnItsWayCannotBeHeld() {
        val f = fans()
        f.toggle(2)
        f.run(100)
        assertFalse(f.dragStart(2))
    }

    @Test
    fun tappingTheOpenQuestionFoldsItAndTheLoopCanSleep() {
        val f = fans()
        f.toggle(2)
        f.run(1400)
        f.toggle(2)
        f.run(1600)
        assertFalse(f.isOpen(2))
        assertEquals(0f, f.heightFraction(2))
        f.run(1000)
        assertTrue(f.settled)
    }

    @Test
    fun reducedMotionOpensAndFoldsAtOnce() {
        val f = fans(reduced = true)
        f.toggle(1)
        assertEquals(1f, f.travel(1))
        assertEquals(1f, f.spread(1, Ribs))
        f.step(1f / 60f)
        assertTrue(PaperFanEvent.Fwap to 1 in events)
        f.toggle(4)
        f.step(1f / 60f)
        assertEquals(0f, f.travel(1))
        assertEquals(1f, f.travel(4))
        assertTrue(f.settled)
    }

    @Test
    fun theFingersAngleRoundTheRivetMapsToTheGuardsSpread() {
        assertEquals(1f, PaperFanState.dragFraction(10f, 0f), 1e-5f)
        assertEquals(.5f, PaperFanState.dragFraction(0f, -10f), 1e-5f)
        assertEquals(0f, PaperFanState.dragFraction(-10f, -0.0001f), 1e-4f)
        // Below the rivet: whichever side the finger is on.
        assertEquals(1f, PaperFanState.dragFraction(5f, 3f))
        assertEquals(0f, PaperFanState.dragFraction(-5f, 3f))
        val a = -PI.toFloat() * .25f
        assertEquals(
            .75f,
            PaperFanState.dragFraction(kotlin.math.cos(a), kotlin.math.sin(a)),
            1e-5f,
        )
    }
}
