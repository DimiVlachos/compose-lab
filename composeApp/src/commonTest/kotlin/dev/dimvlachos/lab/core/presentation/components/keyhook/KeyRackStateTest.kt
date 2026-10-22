package dev.dimvlachos.lab.core.presentation.components.keyhook

import dev.dimvlachos.lab.keyhookdemo.Household
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KeyRackStateTest {
    private val events = mutableListOf<KeyRackEvent>()

    private fun rack(reduced: Boolean = false) =
        KeyRackState(Household.people, random = Random(7)).apply {
            reducedMotion = reduced
            layout(W, H)
            onEvent = { e, _ -> events += e }
        }

    private fun KeyRackState.run(seconds: Float, each: () -> Unit = {}) {
        var t = 0f
        while (t < seconds) {
            step(1f / 60f)
            each()
            t += 1f / 60f
        }
    }

    private fun KeyRackState.near(r: KeyRing, x: Float, y: Float) = hypot(r.x - x, r.y - y) < 3f

    private fun KeyRackState.atHook(r: KeyRing) =
        near(r, geometry!!.hookX(r.index), geometry!!.hookY)

    private fun KeyRackState.atCard(r: KeyRing) = near(r, geometry!!.carryX, geometry!!.carryY)

    /** Drags [r] from its ring point to ([x], [y]) in 16 moves, as the prototype's selftest. */
    private fun KeyRackState.drag(r: KeyRing, x: Float, y: Float, each: () -> Unit = {}) {
        val fx = ringPointX(r)
        val fy = ringPointY(r)
        startGrab(r, fx, fy)
        run(.08f, each)
        for (i in 1..16) {
            val t = i / 16f
            moveGrab(fx + (x - fx) * t, fy + (y - fy) * t)
            run(.03f, each)
        }
        endGrab(x, y)
    }

    @Test
    fun startsSignedInAsJordanWithHisKeysOnTheCard() {
        val s = rack()
        assertEquals("Jordan Miller", s.signedIn?.name)
        assertEquals(0, s.carriedIndex)
        assertTrue(s.atCard(s.rings[0]))
        for (r in s.rings.drop(1)) assertTrue(s.atHook(r))
    }

    @Test
    fun hangingTheKeysUpSignsOutAndSetsThemSwinging() {
        val s = rack()
        val jordan = s.rings[0]
        var swing = 0f
        s.drag(jordan, s.geometry!!.hookX(0), s.geometry!!.boardTop + 30f) {
            swing = max(swing, abs(jordan.th))
        }
        assertTrue(swing > .12f, "carried keys swing: $swing")
        var landed = 0f
        s.run(1.4f) { landed = max(landed, abs(jordan.th)) }
        assertNull(s.signedIn)
        assertEquals(-1, s.carriedIndex)
        assertTrue(s.atHook(jordan))
        assertTrue(landed > .05f, "landing swings them: $landed")
        assertTrue(KeyRackEvent.Clack in events)
        assertTrue(KeyRackEvent.Lift in events)
    }

    @Test
    fun picksUpWithoutDropping() {
        val s = rack()
        val ava = s.rings[1]
        val y0 = ava.y
        s.startGrab(ava, s.ringPointX(ava), s.ringPointY(ava))
        var drop = 0f
        s.run(.3f) { drop = max(drop, ava.y - y0) }
        assertTrue(drop < 2f, "dropped $drop")
    }

    @Test
    fun aTapTakesAvasKeysAndSignsHerIn() {
        val s = rack()
        val ava = s.rings[1]
        s.startGrab(ava, s.ringPointX(ava), s.ringPointY(ava))
        s.run(.07f)
        s.endGrab(s.ringPointX(ava), s.ringPointY(ava))
        // Jordan's set is sent home first, so nobody is signed in until Ava's lands.
        assertEquals(1, s.carriedIndex)
        s.run(1.3f)
        assertEquals("Ava Miller", s.signedIn?.name)
        assertTrue(s.atCard(ava))
        assertTrue(s.atHook(s.rings[0]))
    }

    @Test
    fun takingSamsKeysSendsTheOldSetHomeAndADropAnywhereFindsItsOwnHook() {
        val s = rack()
        val sam = s.rings[2]
        s.drag(sam, W * .5f, s.geometry!!.cardBottom + 40f)
        s.run(1.5f)
        assertSame(Household.people[2], s.signedIn)
        assertTrue(s.atHook(s.rings[0]))
        s.drag(sam, s.geometry!!.hookX(3), s.geometry!!.boardTop + 40f)
        s.run(1.4f)
        assertTrue(s.atHook(sam))
        assertNull(s.signedIn)
    }

    @Test
    fun aSetDraggedLowStaysInsideTheRack() {
        val s = rack()
        val lou = s.rings[3]
        var low = Float.NEGATIVE_INFINITY
        val g = s.geometry!!
        s.drag(lou, g.hookX(3), H + 60f) { low = max(low, lou.y + 66f * g.u * g.ks - H) }
        assertTrue(low <= 2f, "past the bottom by $low")
    }

    @Test
    fun swingingSettlesAtTheRestAngleAndStaysFinite() {
        val s = rack()
        s.drag(s.rings[0], s.geometry!!.hookX(0), s.geometry!!.boardTop + 30f)
        s.run(12f)
        assertTrue(s.settled)
        for (r in s.rings) {
            assertTrue(abs(r.th) < .04f)
            for (j in r.ph.indices) {
                assertTrue(r.ph[j].isFinite())
                assertEquals(KeyRackState.restPh(s.geometry!!, r, j), r.ph[j], .01f)
            }
        }
    }

    @Test
    fun reducedMotionRestsAtTheSameFanAndSignsInAtOnce() {
        val s = rack(reduced = true)
        for (r in s.rings) for (j in r.ph.indices) {
            assertEquals(KeyRackState.restPh(s.geometry!!, r, j), r.ph[j], 1e-5f)
        }
        s.toggle(s.rings[1])
        assertEquals("Ava Miller", s.signedIn?.name)
        assertTrue(KeyRackEvent.Clack in events)
        s.run(.3f)
        assertTrue(s.atCard(s.rings[1]))
        assertTrue(s.atHook(s.rings[0]))
        assertTrue(s.settled)
    }

    @Test
    fun theKeysGrowIntoATallHeroAndTheHintEasesDown() {
        val short = RackGeometry(360f, 330f, 4)
        val tall = RackGeometry(412f, 610f, 4)
        assertEquals(1.1f, short.ks, 1e-4f)
        assertEquals(1.75f, tall.ks, 1e-4f)
        assertEquals(1.45f + 30f / 150f, RackGeometry(412f, 490f, 4).ks, 1e-4f)
        // A short wall keeps the hint under the card; a tall one eases it toward the middle.
        assertEquals(short.cardBottom + 16f, short.hintTop(36f), 1e-3f)
        val free = tall.boardTop - (tall.cardBottom + 16f) - 36f
        assertTrue(tall.hintTop(36f) > tall.cardBottom + 16f)
        assertTrue(tall.hintTop(36f) <= tall.cardBottom + 16f + free / 2f + 1e-3f)
    }

    @Test
    fun resetPutsJordanBackOnTheCard() {
        val s = rack()
        s.toggle(s.rings[1])
        s.run(2f)
        s.reset()
        assertEquals("Jordan Miller", s.signedIn?.name)
        assertTrue(s.atCard(s.rings[0]))
        assertTrue(s.atHook(s.rings[1]))
    }

    @Test
    fun aScriptCannotMoveOrDropTheSetAFingerHolds() {
        val s = rack()
        val ava = s.rings[1]
        s.startGrab(ava, s.ringPointX(ava), s.ringPointY(ava))
        s.moveGrab(10f, 10f, scripted = true)
        s.endGrab(10f, 10f, scripted = true)
        assertSame(ava, s.grabbed)
        s.endGrab(s.ringPointX(ava), s.ringPointY(ava))
        assertNull(s.grabbed)
    }

    @Test
    fun takingHoldOfASecondSetLetsGoOfTheFirst() {
        val s = rack()
        val sam = s.rings[2]
        val lou = s.rings[3]
        s.startGrab(sam, s.ringPointX(sam), s.ringPointY(sam), scripted = true)
        s.run(.5f)
        s.startGrab(lou, s.ringPointX(lou), s.ringPointY(lou))
        assertSame(lou, s.grabbed)
        s.run(2f)
        assertEquals(KeyRing.Mode.Rest, sam.mode)
    }

    private companion object {
        const val W = 412f
        const val H = 560f
    }
}
