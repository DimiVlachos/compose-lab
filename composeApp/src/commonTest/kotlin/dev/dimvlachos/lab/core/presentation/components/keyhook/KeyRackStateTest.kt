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

    @Test
    fun theGateSwingsOpenQuicklyAndSnapsShutOnceWithAClick() {
        val g = ClipGate()
        var t = 0f
        while (g.angle < .8f) {
            assertEquals(0f, g.step(1f / 240f, open = true, reduced = false))
            t += 1f / 240f
        }
        assertTrue(t < .15f, "open in ${t}s")
        repeat(60) { g.step(1f / 240f, open = true, reduced = false) }
        assertEquals(1f, g.angle, .02f)
        var snaps = 0
        var strength = 0f
        t = 0f
        var shutAt = -1f
        while (t < .5f) {
            val k = g.step(1f / 240f, open = false, reduced = false)
            if (k > 0f) {
                snaps++
                strength = k
                if (shutAt < 0f) shutAt = t
            }
            t += 1f / 240f
        }
        assertEquals(1, snaps)
        assertTrue(shutAt < .06f, "snapped shut after ${shutAt}s")
        assertTrue(strength > .9f, "a hard snap: $strength")
        assertTrue(g.shut)
    }

    @Test
    fun withReducedMotionTheGateIsOpenOrShutAndNeverClicks() {
        val g = ClipGate()
        assertEquals(0f, g.step(1f / 240f, open = true, reduced = true))
        assertEquals(1f, g.angle)
        assertEquals(0f, g.step(1f / 240f, open = false, reduced = true))
        assertEquals(0f, g.angle)
        assertTrue(g.shut)
    }

    @Test
    fun avasRingThreadsInThroughTheOpenGateWhichSnapsShutOnIt() {
        val s = rack()
        val ava = s.rings[1]
        val g = s.geometry!!
        s.toggle(ava)
        var onMs = 0f
        var maxGate = 0f
        s.run(2f) {
            maxGate = max(maxGate, s.gate.angle)
            if (ava.on && ava.mode != KeyRing.Mode.Rest) {
                onMs += 1000f / 60f
                // Threaded, the ring follows the clip's path exactly.
                assertEquals(g.clipX(ava.clip), ava.x, .01f)
                assertEquals(g.clipY(ava.clip), ava.y, .01f)
                // It is never in the gap unless the gate is open.
                if (ava.clip > .25f && ava.clip < .75f) {
                    assertTrue(s.gate.angle >= .8f, "gate ${s.gate.angle} at ${ava.clip}")
                }
            }
        }
        assertTrue(maxGate > .95f, "the gate opened to $maxGate")
        assertTrue(onMs in 250f..500f, "threaded in for $onMs ms")
        assertEquals(1, events.count { it == KeyRackEvent.ClipShut })
        assertTrue(ava.on && ava.clip == 0f && s.atCard(ava), "ava ${ava.on} ${ava.clip}")
        assertTrue(!s.rings[0].on && s.atHook(s.rings[0]), "jordan home")
        // Jordan's set left by the clip, and nothing clacks on the card.
        assertEquals(1, events.count { it == KeyRackEvent.Clack })
        s.run(20f)
        assertTrue(s.gate.shut, "shut")
        assertTrue(s.settled, "settled")
    }

    @Test
    fun takingTheKeysOffTheCardSlidesTheRingOutThroughTheGate() {
        val s = rack()
        val jordan = s.rings[0]
        val g = s.geometry!!
        s.startGrab(jordan, s.ringPointX(jordan), s.ringPointY(jordan))
        var left = false
        var opened = false
        s.run(.6f) {
            if (s.gate.angle > .8f) opened = true
            if (jordan.on) {
                assertEquals(g.clipX(jordan.clip), jordan.x, .01f)
                if (jordan.clip > .25f) assertTrue(s.gate.angle >= .8f)
            } else left = true
        }
        assertTrue(opened && left, "opened $opened, left $left")
        // Carried away, the gate shuts behind it, with a click but no haptic.
        s.moveGrab(W * .3f, H * .6f)
        s.run(.6f)
        assertTrue(s.gate.shut)
        assertTrue(KeyRackEvent.ClipShutEmpty in events)
        s.endGrab(W * .3f, H * .6f)
        s.run(2f)
        assertTrue(s.atHook(jordan))
    }

    @Test
    fun withReducedMotionTheClipTakesTheSetAtOnceAndClicks() {
        val s = rack(reduced = true)
        val jordan = s.rings[0]
        s.startGrab(jordan, s.ringPointX(jordan), s.ringPointY(jordan))
        assertTrue(!jordan.on)
        s.run(.1f) { assertTrue(s.gate.angle == 0f || s.gate.angle == 1f) }
        // Held right by the clip, it shows open.
        assertEquals(1f, s.gate.angle)
        s.endGrab(s.ringPointX(jordan), s.ringPointY(jordan))
        s.toggle(s.rings[2])
        assertTrue(s.rings[2].on && s.atCard(s.rings[2]))
        assertTrue(KeyRackEvent.ClipShut in events)
        s.run(.1f)
        assertEquals(0f, s.gate.angle)
    }

    @Test
    fun aSetLetGoRightByTheClipKeepsItsGateOpenUntilItIsOn() {
        val s = rack()
        val g = s.geometry!!
        s.toggle(s.rings[0])
        s.run(3f)
        val ava = s.rings[1]
        // Carried until her ring hangs just outside the clip's mouth: the gate swings open for it.
        val fx = s.ringPointX(ava)
        val fy = s.ringPointY(ava)
        val tx = g.clipX(1f)
        val ty = g.clipY(1f) + (fy - ava.y)
        s.startGrab(ava, fx, fy)
        for (i in 1..16) {
            s.moveGrab(fx + (tx - fx) * i / 16f, fy + (ty - fy) * i / 16f)
            s.run(.03f)
        }
        s.run(.6f)
        assertTrue(hypot(ava.x - g.clipX(1f), ava.y - g.clipY(1f)) < 36f * g.u, "close")
        assertTrue(s.gate.angle > .9f, "open ${s.gate.angle}")
        s.endGrab(tx, ty)
        events.clear()
        var shutBeforeOn = false
        s.run(2f) { if (!ava.on && s.gate.angle == 0f) shutBeforeOn = true }
        assertTrue(!shutBeforeOn, "the gate shut before the ring was on")
        assertEquals(listOf(KeyRackEvent.ClipShut), events.filter { it.isClip })
        assertTrue(ava.on && s.atCard(ava))
        assertSame(ava.holder, s.signedIn)
    }

    @Test
    fun aSetSentBeforeTheRackIsLaidOutBelongsWhereItWasSent() {
        val s = KeyRackState(Household.people, random = Random(7))
        s.toggle(s.rings[1])
        assertSame(s.rings[1].holder, s.signedIn)
        assertTrue(s.rings[1].on && !s.rings[0].on)
        s.layout(W, H)
        s.run(1f)
        assertTrue(s.atCard(s.rings[1]) && s.atHook(s.rings[0]))
        // Jordan's set, on its hook, comes off it rather than out of the clip.
        val jordan = s.rings[0]
        s.startGrab(jordan, s.ringPointX(jordan), s.ringPointY(jordan))
        s.run(.3f)
        assertTrue(
            !jordan.on && jordan.y > s.geometry!!.boardTop,
            "jordan ${jordan.x}, ${jordan.y}",
        )
    }

    private val KeyRackEvent.isClip
        get() = this == KeyRackEvent.ClipShut || this == KeyRackEvent.ClipShutEmpty

    private companion object {
        const val W = 412f
        const val H = 560f
    }
}
