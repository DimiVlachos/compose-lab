package dev.dimvlachos.lab.core.presentation.components.locket

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The prototype's self-test, run against the ported state.
class LocketStateTest {
    private val events = mutableListOf<LocketEvent>()

    private fun locket() = LocketState().also { s -> s.onEvent = { events += it } }

    private var minLeft = 1e9f
    private var maxRight = -1e9f

    // Tracks how close the locket's rim and lid come to the box's sides, as the self-test does.
    private fun LocketState.sides() {
        val ca = cos(a)
        for (i in 0 until 48) {
            val t = i / 48f * 2f * kotlin.math.PI.toFloat()
            val bx = toWorldX(Locket.RX * cos(t), Locket.BC + Locket.RY * sin(t))
            val lx =
                toWorldX(lidCenter() + Locket.RX * abs(ca) * cos(t), Locket.BC + Locket.RY * sin(t))
            minLeft = minOf(minLeft, bx, lx)
            maxRight = maxOf(maxRight, bx, lx)
        }
    }

    private fun LocketState.run(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            step(1f / 60f)
            sides()
            left -= 1f / 60f
        }
    }

    private fun LocketState.tap(x: Float, y: Float) {
        down(x, y)
        run(.06f)
        up(x, y)
    }

    // A deliberately sparse drag: a few big jumps, like a fast swipe.
    private fun LocketState.drag(vararg pts: Pair<Float, Float>) {
        down(pts[0].first, pts[0].second)
        for (i in 1 until pts.size) {
            run(.016f)
            move(pts[i].first, pts[i].second)
        }
        run(.016f)
        up(pts.last().first, pts.last().second)
    }

    private fun LocketState.world(lx: Float, ly: Float) = toWorldX(lx, ly) to toWorldY(lx, ly)

    @Test
    fun startsClosedWithTheBlueRidgePhoto() {
        val s = locket()
        assertFalse(s.isOpen)
        assertEquals(0f, s.a)
        assertEquals(2, s.photo)
    }

    @Test
    fun theRecentThumbsNeverDrawBelowTheirDpSize() {
        for ((w, h) in listOf(412f to 517f, 360f to 302f, 300f to 380f, 280f to 400f)) {
            val b = LocketBox(w, h)
            val want = minOf(Locket.THUMB, Locket.THUMB * w / 300f / maxOf(b.s, 1f))
            assertTrue(b.ts * b.s >= want - .01f, "$w × $h: ${b.ts * b.s} < $want")
        }
    }

    @Test
    fun theWholeSelfTestStory() {
        val s = locket()
        s.run(.5f)
        s.tap(s.toWorldX(Locket.RX + 2f, Locket.BC), s.toWorldY(Locket.RX + 2f, Locket.BC))
        s.run(.9f)
        assertTrue(s.a > Locket.OPEN - .25f && s.opens == 1 && s.isOpen, "tapping the catch opens")

        val b = s.box
        s.drag(
            b.tcx(1) to b.tcy,
            b.tcx(1) to b.tcy - 90f,
            s.world(0f, Locket.BC + 30f),
            s.world(0f, Locket.BC),
        )
        s.run(.9f)
        assertTrue(s.photo == 1 && s.placed == 1 && s.place == null, "a sparse drag sets the photo")

        s.down(b.tcx(3) + 10f, b.tcy + 6f)
        s.run(.016f)
        s.move(b.tcx(3) + 100f, b.tcy - 114f)
        val d = assertNotNull(s.drag)
        assertTrue(
            d.moved && abs(d.x - (b.tcx(3) + 90f)) < .01f && abs(d.y - (b.tcy - 120f)) < .01f,
        )
        s.run(.016f)
        s.move(280f, 250f)
        s.run(.016f)
        s.up(280f, 250f)
        s.run(.6f)
        assertTrue(s.photo == 1 && s.back.isEmpty() && s.place == null, "it slides back")

        s.tap(s.toWorldX(-6f, Locket.BC + 14f), s.toWorldY(-6f, Locket.BC + 14f))
        s.run(.25f)
        assertTrue(s.a < .05f && s.snaps == 1, "tapping the open locket snaps it shut")
        assertTrue(abs(s.th) > .02f || abs(s.tw) > .2f, "the snap swings the chain")
        s.run(.4f)

        s.drag(b.tcx(0) to b.tcy, b.tcx(0) + 20f to b.tcy - 100f, s.world(0f, Locket.BC))
        s.run(1f)
        assertTrue(s.photo == 0 && s.isOpen && s.a > 2.4f, "a drop on the closed locket opens it")

        val fe = s.world(s.freeEdge() + 6f, Locket.BC)
        s.drag(fe, s.world(-60f, Locket.BC), s.world(30f, Locket.BC))
        s.run(.6f)
        assertTrue(!s.isOpen && s.a < .05f && s.snaps == 2, "pushing the lid across closes it")
        s.run(2.5f)

        s.drag(
            s.world(0f, Locket.BC),
            Locket.AX + 50f to Locket.AY + Locket.BC - 10f,
            Locket.AX + 90f to Locket.AY + Locket.BC - 30f,
        )
        s.run(.08f)
        val swung = abs(s.th)
        assertTrue(swung > .15f && !s.isOpen, "grabbing the closed locket swings it: $swung")
        s.run(6f)
        assertTrue(abs(s.th) < swung * .4f, "the swing dies down")
        assertTrue(
            minLeft >= Locket.MARGIN - .5f && maxRight <= 300f - Locket.MARGIN + .5f,
            "inside the box: $minLeft .. $maxRight",
        )
        assertTrue(s.a <= Locket.LIDMAX + 1e-6f)

        s.usePhoto(3)
        s.run(1.2f)
        assertTrue(s.photo == 3 && s.isOpen, "using a Recent photo")
        s.toggle()
        s.run(.6f)
        assertTrue(!s.isOpen && s.a < .05f)
        assertTrue(LocketEvent.Snap in events && LocketEvent.Place in events)
        assertTrue(LocketEvent.Chain in events && LocketEvent.Back in events)
    }

    @Test
    fun anOpenLocketComesToRestTiltedAgainstTheBoxsSide() {
        val s = locket()
        s.usePhoto(3)
        s.run(20f)
        assertTrue(s.settled, "th ${s.th}, tw ${s.tw}, a ${s.a}")
        assertTrue(s.th > 0f, "the open lid keeps it off the left edge: ${s.th}")
    }

    @Test
    fun theOvershootStopsAtLidMax() {
        val s = locket()
        s.open()
        var peak = 0f
        repeat(120) {
            s.step(1f / 60f)
            peak = maxOf(peak, s.a)
        }
        assertTrue(peak <= Locket.LIDMAX + 1e-6f && peak > Locket.OPEN, "peak $peak")
    }

    @Test
    fun reducedMotionSettlesAtOnceAndStillSnaps() {
        val s = locket()
        s.reducedMotion = true
        s.open()
        assertEquals(Locket.OPEN, s.a)
        s.usePhoto(0)
        s.step(1f / 60f)
        assertEquals(0, s.photo)
        assertNull(s.place)
        s.close()
        assertEquals(0f, s.a)
        assertEquals(1, s.snaps)
        // Once the glass's sweep over the new photo is done.
        s.run(.8f)
        assertTrue(s.settled)
    }

    @Test
    fun itSettlesAndTheClosedCatchSaysWhenItGlintsNext() {
        val s = locket()
        s.run(.1f)
        assertTrue(s.settled)
        val rest = assertNotNull(s.restMs())
        assertTrue(rest >= 0f && rest < 2f * kotlin.math.PI.toFloat() * 520f)
        s.open()
        s.run(3f)
        assertNull(s.restMs())
    }
}
