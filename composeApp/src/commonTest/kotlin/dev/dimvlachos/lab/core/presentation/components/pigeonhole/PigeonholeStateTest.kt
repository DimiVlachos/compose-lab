package dev.dimvlachos.lab.core.presentation.components.pigeonhole

import dev.dimvlachos.lab.pigeonholedemo.LarkspurFolders
import dev.dimvlachos.lab.pigeonholedemo.LarkspurMail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PigeonholeStateTest {
    private val sounds = mutableListOf<PigeonholeSound>()
    private val said = mutableListOf<PigeonholeSay>()

    private fun state(listH: Float = 640f) =
        PigeonholeState(
                LarkspurFolders.map { Cubby(it, it.replaceFirstChar(Char::uppercase)) },
                LarkspurMail,
            )
            .apply {
                layout(412f, 820f, 140f, listH)
                onSound = { s, _ -> sounds += s }
                onSay = { said += it }
            }

    private fun PigeonholeState.run(ms: Int) {
        repeat(ms / 16) { tick(.016f) }
    }

    private fun PigeonholeState.idle() {
        var n = 0
        while ((letters.isNotEmpty() || carry != null || !settled) && n++ < 800) tick(.016f)
        run(600)
    }

    private fun PigeonholeState.ids(folder: String) = messages(folder).map { it.id }

    // A row's centre and a cubby's centre on the rack where it ends up.
    private fun PigeonholeState.row(id: String) = assertNotNull(rowCenter(id))

    private fun PigeonholeState.drag(id: String, toX: Float, toY: Float, moves: Int = 5) {
        val (ax, ay) = row(id)
        rowDown(id, ax, ay)
        for (i in 1..moves) {
            ptrMove(ax + (toX - ax) * i / moves, ay + (toY - ay) * i / moves)
            run(40)
        }
    }

    @Test
    fun itStartsInTheInboxWithSevenMessagesAndTheRackHidden() {
        val s = state()
        assertEquals(PigeonholeState.Inbox, s.view)
        assertEquals(7, s.rows.size)
        assertFalse(s.rackUp)
        assertTrue(s.rackTop() >= s.height)
    }

    @Test
    fun theBackLetterOfAStackStillFitsInsideItsCubby() {
        val s = state()
        val top = s.upTop()
        assertTrue(
            s.slotY(0, 2f, top) - PigeonholeState.LH * s.k * .86f / 2 > s.cubY(0, top) + 2 * s.k,
        )
    }

    @Test
    fun draggingAMessageToReceiptsPostsItThere() {
        val s = state()
        val n0 = s.messages("receipts").size
        s.drag("m1", s.cubCenterX(1), s.cubCenterY(1))
        assertTrue(s.rackUp)
        assertEquals("m1", s.carry?.letter?.mail?.id)
        assertTrue(s.isHidden("m1"), "its row keeps an empty slot while it is carried")
        s.run(1500)
        assertTrue(s.carry!!.letter.f > .95f, "over the rack it is folded into a letter")
        assertEquals(1, s.carry!!.hover)
        assertTrue(PigeonholeSound.Lift in sounds && PigeonholeSound.Fold in sounds)
        assertTrue(PigeonholeSound.Tick in sounds)
        s.ptrUp(s.cubCenterX(1), s.cubCenterY(1))
        s.idle()
        assertEquals("m1", s.ids("receipts").first())
        assertEquals(n0 + 1, s.cubCount(1))
        assertEquals(6, s.rows.size)
        assertNull(s.rowCenter("m1"), "the inbox closes the gap")
        assertEquals(PigeonholeState.Inbox, s.messages("receipts").first().home)
        assertEquals(0, s.messages("receipts").first().idx)
        assertFalse(s.rackUp, "the rack slides back down")
        assertTrue(PigeonholeSay.Moved("Receipts") in said)
        assertTrue(PigeonholeSound.Tock in sounds)
    }

    @Test
    fun theHoveringLetterWaitsAboveTheCubbysFrame() {
        val s = state()
        s.drag("m1", s.cubCenterX(4), s.cubCenterY(4))
        s.run(1500)
        val l = s.carry!!.letter
        assertEquals(4, s.carry!!.hover)
        val bottom = l.y + PigeonholeState.LH * s.k * l.s / 2
        assertTrue(bottom <= s.cubY(4, s.rackTop()) - 3 * s.k, "$bottom")
    }

    @Test
    fun aPressAndAFarReleaseWithNoMovesStillPosts() {
        val s = state()
        val (ax, ay) = s.row("m2")
        s.rowDown("m2", ax, ay)
        s.run(30)
        s.ptrUp(s.cubCenterX(0), s.cubCenterY(0))
        s.idle()
        assertEquals("m2", s.ids("work").first())
        assertNull(s.rowCenter("m2"))
    }

    @Test
    fun aQuickTouchDragUpScrollsTheListAndPicksNothingUp() {
        val s = state(listH = 400f)
        val (ax, ay) = s.row("m3")
        s.rowDown("m3", ax, ay, touch = true, now = 0.0)
        s.ptrMove(ax + 2, ay - 30, now = 40.0)
        assertTrue(s.scrollingFor(null))
        s.ptrUp(ax + 3, ay - 70)
        s.run(60)
        assertNull(s.carry)
        assertFalse(s.rackUp)
        assertFalse(s.isHidden("m3"))
    }

    @Test
    fun aTouchDragDownAtTheTopPicksUpButOnceScrolledItScrolls() {
        val s = state(listH = 400f)
        val (ax, ay) = s.row("m2")
        s.rowDown("m2", ax, ay, touch = true, now = 0.0)
        s.ptrMove(ax, ay + 30, now = 40.0)
        assertEquals("m2", s.carry?.letter?.mail?.id)
        s.ptrUp(ax, ay + 30)
        s.idle()
        s.scrollBy(60f)
        val (bx, by) = s.row("m3")
        s.rowDown("m3", bx, by, touch = true, now = 0.0)
        s.ptrMove(bx, by + 30, now = 40.0)
        assertTrue(s.scrollingFor(null))
        s.ptrUp(bx, by + 30)
        // Held first, it picks up even scrolled.
        s.rowDown("m3", bx, by, touch = true, now = 0.0)
        s.ptrMove(bx, by + 30, now = 400.0)
        assertEquals("m3", s.carry?.letter?.mail?.id)
    }

    @Test
    fun letGoOverTheListAndItUnfoldsBackIntoItsSlot() {
        val s = state()
        val (ax, ay) = s.row("m3")
        s.rowDown("m3", ax, ay)
        s.ptrMove(ax + 20, ay + 30)
        s.ptrMove(ax + 30, ay + (s.upTop() - ay) * .75f)
        s.run(160)
        val f = s.carry!!.letter.f
        assertTrue(f > .1f && f < .99f, "partway to the rack it is partly folded: $f")
        s.ptrUp(ax + 30, ay + 60)
        s.idle()
        assertFalse(s.isHidden("m3"))
        assertEquals(2, s.ids(PigeonholeState.Inbox).indexOf("m3"))
        assertTrue(PigeonholeSay.NotMoved in said)
    }

    @Test
    fun theCardTurnsToPaperWithTheFold() {
        val s = state()
        val l = Letter(s.rows.first())
        s.letters += l
        s.tick(.001f)
        assertEquals(0f, l.pm)
        l.f = .17f
        s.tick(.001f)
        assertTrue(l.pm > .4f && l.pm < .6f, "half way through the narrowing: ${l.pm}")
        l.f = .4f
        s.tick(.001f)
        assertEquals(1f, l.pm)
        l.f = 0f
        s.tick(.001f)
        assertEquals(0f, l.pm)
    }

    @Test
    fun tappingACubbyOpensItsFolderHandingOverEveryLetterInOneFrame() {
        val s = state()
        s.toggleRack()
        assertTrue(s.rackUp)
        s.run(500)
        val n = s.messages("receipts").size
        s.cubDown(1, s.cubCenterX(1), s.cubCenterY(1))
        s.ptrUp(s.cubCenterX(1), s.cubCenterY(1))
        var frames = 0
        while (s.letters.isEmpty() && frames++ < 60) s.tick(.016f)
        assertEquals(n, s.letters.count { it.cub == 1 }, "the whole stack at once")
        val shown = arrayOfNulls<Mail>(3)
        assertEquals(0, s.cubVisible(1, shown), "the cubby is empty while its folder is open")
        s.idle()
        assertEquals("receipts", s.view)
        assertEquals(n, s.rows.size)
        assertTrue(s.rows.none { s.isHidden(it.id) })
        assertFalse(s.rackUp)
        assertTrue(PigeonholeSay.Opened("Receipts", n) in said)
    }

    @Test
    fun inAFolderItsOwnCubbyIsNotATargetAndBackFilesTheRowsAway() {
        val s = state()
        s.toggleRack()
        s.run(500)
        s.cubTap(1)
        s.idle()
        s.toggleRack()
        s.run(500)
        val (ax, ay) = s.row("r1")
        s.rowDown("r1", ax, ay)
        s.ptrMove(ax, ay + 60)
        s.ptrMove(s.cubCenterX(1), s.cubCenterY(1))
        s.ptrUp(s.cubCenterX(1), s.cubCenterY(1))
        s.idle()
        assertEquals("r1", s.ids("receipts").first())
        assertFalse(s.isHidden("r1"))
        assertEquals("receipts", s.view)
        s.back()
        assertTrue(s.rackUp)
        s.idle()
        assertEquals(PigeonholeState.Inbox, s.view)
        assertEquals(7, s.rows.size)
        assertEquals(4, s.cubCount(1))
        assertEquals(4, s.cubVisible(1, arrayOfNulls(6)), "every letter landed back in its cubby")
    }

    @Test
    fun aLetterPulledOutOfACubbyLandsBackInTheInboxWhereItWas() {
        val s = state()
        s.drag("m1", s.cubCenterX(1), s.cubCenterY(1))
        s.ptrUp(s.cubCenterX(1), s.cubCenterY(1))
        s.idle()
        s.toggleRack()
        s.run(500)
        val sx = s.slotX(1)
        val sy = s.slotY(1, 0f, s.upTop())
        s.cubDown(1, sx, sy)
        s.ptrMove(sx, sy - 40)
        assertEquals("m1", s.carry?.letter?.mail?.id)
        assertEquals(4, s.cubCount(1))
        s.ptrMove(sx - 20, s.listTop + 100)
        s.ptrMove(206f, s.listTop + 40)
        s.run(1500)
        assertTrue(s.carry!!.letter.f < .05f, "out over the list it unfolds")
        s.ptrUp(206f, s.listTop + 40)
        s.idle()
        assertEquals("m1", s.ids(PigeonholeState.Inbox).first())
        assertFalse(s.isHidden("m1"))
        assertEquals(4, s.messages("receipts").size)
        assertTrue(s.asBegun)
    }

    @Test
    fun aCancelledDragFinishesFromItsLastPoint() {
        val s = state()
        val (ax, ay) = s.row("m4")
        s.rowDown("m4", ax, ay)
        s.ptrMove(ax, ay + 80)
        s.ptrMove(s.cubCenterX(3), s.cubCenterY(3))
        s.ptrCancel()
        s.idle()
        assertEquals("m4", s.ids("travel").first())
    }

    @Test
    fun theKeyboardPicksUpPostsAndPutsBack() {
        val s = state()
        val focus = mutableListOf<PigeonholeFocus>()
        s.onFocus = { focus += it }
        s.kbPick("m5")
        assertTrue(s.carry!!.kb && s.rackUp && s.isHidden("m5"))
        assertEquals(PigeonholeFocus.Cub(0), focus.last())
        s.run(1500)
        assertTrue(s.carry!!.letter.f > .95f)
        s.focusCub(4)
        s.cubTap(4)
        s.idle()
        assertEquals("m5", s.ids("school").first())
        s.kbPick("m6")
        assertTrue(s.escape())
        s.idle()
        assertFalse(s.isHidden("m6"))
        assertTrue("m6" in s.ids(PigeonholeState.Inbox))
        assertEquals(PigeonholeFocus.Row("m6"), focus.last())
    }

    @Test
    fun aScriptedDragLetGoMidwayDropsWhereItIs() {
        val s = state()
        val (ax, ay) = s.row("m1")
        s.rowDown("m1", ax, ay, scripted = true)
        s.ptrMove(ax, ay + 40, scripted = true)
        s.ptrMove(s.cubCenterX(1), s.cubCenterY(1), scripted = true)
        // A real finger's moves don't steer the script's letter.
        s.ptrMove(0f, 0f)
        assertEquals(1, s.carry!!.hover)
        s.releaseScripted()
        s.idle()
        assertNull(s.carry)
        assertTrue(s.letters.isEmpty())
        assertFalse(s.rackUp)
    }

    @Test
    fun reducedMotionSettlesAtOnce() {
        val s = state().apply { reducedMotion = true }
        s.toggleRack()
        assertEquals(s.upTop(), s.rackTop())
        s.cubTap(2)
        s.run(200)
        assertEquals("family", s.view)
        assertTrue(s.letters.isEmpty())
    }

    @Test
    fun aSecondFingerNeitherStealsTheLetterNorEndsTheCarryWithDone() {
        val s = state()
        val (ax, ay) = s.row("m1")
        s.rowDown("m1", ax, ay, owner = 1)
        s.ptrMove(ax, ay + 40, owner = 1)
        s.rowDown("m2", ax, ay + 85, owner = 2)
        s.ptrMove(s.cubCenterX(0), s.cubCenterY(0), owner = 2)
        s.ptrUp(s.cubCenterX(0), s.cubCenterY(0), owner = 2)
        s.done()
        assertEquals("m1", s.carry?.letter?.mail?.id)
        assertTrue(s.rackUp)
        s.ptrMove(s.cubCenterX(1), s.cubCenterY(1), owner = 1)
        s.ptrUp(s.cubCenterX(1), s.cubCenterY(1), owner = 1)
        s.idle()
        assertEquals("m1", s.ids("receipts").first())
        assertEquals(3, s.messages("work").size)
    }

    @Test
    fun aCancelledPressOnACubbyOpensNothing() {
        val s = state()
        s.toggleRack()
        s.run(500)
        s.cubDown(2, s.cubCenterX(2), s.cubCenterY(2), owner = 1)
        s.ptrCancel(owner = 1)
        s.idle()
        assertEquals(PigeonholeState.Inbox, s.view)
    }

    @Test
    fun thePenWritesTheFirstNameOrTheSurnameAfterATitle() {
        assertEquals("Dana", PigeonholeState.penName("Dana Whitfield"))
        assertEquals("Rivera", PigeonholeState.penName("Ms. Rivera"))
        assertEquals("Mom", PigeonholeState.penName("Mom"))
        assertTrue(PigeonholeState.hash("Dana Whitfield") >= 0, "a stamp index, never negative")
    }

    @Test
    fun aPressOnTheRaisedRackDoesNotReachTheRowBehindIt() {
        val s = state()
        // The middle of the first row that the raised rack will cover.
        val y = s.rows.map { s.row(it.id).second }.first { it > s.upTop() + 4 }
        assertNotNull(s.rowAt(206f, y), "with the rack down, a row is there")
        s.toggleRack()
        s.run(500)
        assertTrue(s.onRack(y))
        assertNull(s.rowAt(206f, y))
    }

    @Test
    fun aPressWaitingAsAFolderClosesIsSpentRatherThanPickingUp() {
        val s = state()
        s.toggleRack()
        s.run(500)
        s.cubTap(1)
        s.idle()
        val (ax, ay) = s.row("r1")
        s.rowDown("r1", ax, ay)
        s.back()
        s.run(100)
        s.ptrMove(ax + 40, ay + 120)
        assertNull(s.carry, "nothing in hand while the rows go home")
        s.ptrUp(ax + 40, ay + 120)
        s.idle()
        assertEquals(PigeonholeState.Inbox, s.view)
        assertEquals(7, s.rows.size)
        assertTrue(s.letters.isEmpty())
        assertTrue("r1" in s.ids("receipts"))
    }

    @Test
    fun theSystemBackPutsTheRackDownThenClosesTheFolder() {
        val s = state()
        assertFalse(s.backable, "on the inbox at rest Back leaves the screen")
        s.toggleRack()
        s.run(500)
        assertTrue(s.backable)
        s.systemBack()
        assertFalse(s.rackUp)
        s.toggleRack()
        s.run(500)
        s.cubTap(1)
        s.idle()
        assertEquals("receipts", s.view)
        assertTrue(s.backable)
        s.systemBack()
        s.idle()
        assertEquals(PigeonholeState.Inbox, s.view)
        assertFalse(s.backable)
    }

    @Test
    fun rowsGrowWithTheFontSettingAndTheListScrollsFurther() {
        val s = state()
        val max0 = s.maxScroll()
        s.layout(412f, 820f, 140f, 640f, fontScale = 1.3f)
        assertEquals(PigeonholeState.RowH + 3 * PigeonholeState.LineH * .3f, s.rowH, .01f)
        assertTrue(s.maxScroll() > max0)
        val (_, y) = s.row("m2")
        assertEquals(140f + s.rowH + PigeonholeState.RowGap + s.rowH / 2, y, .01f)
    }

    @Test
    fun aLetterGoingToARowTheRackCoversSlidesInBehindItsEdge() {
        val s =
            PigeonholeState(
                    LarkspurFolders.map { Cubby(it, it.replaceFirstChar(Char::uppercase)) },
                    LarkspurMail,
                )
                .apply { layout(412f, 600f, 140f, 460f) }
        s.toggleRack()
        s.run(600)
        s.cubTap(1)
        var behind = false
        var n = 0
        while ((s.busy() || s.letters.isNotEmpty()) && n++ < 400) {
            s.tick(.016f)
            for (l in s.letters) {
                if (l.below.isNaN()) continue
                // Shown only above the rack's edge, and never drawn over the rack.
                assertEquals(s.rackTop(), l.below, .01f)
                if (l.y > l.below) behind = true
            }
        }
        assertTrue(behind, "a letter went in behind the rack's edge")
        assertTrue(s.letters.isEmpty())
        assertTrue(s.rows.none { s.isHidden(it.id) })
        assertFalse(s.rackUp)
    }

    @Test
    fun backBringsTheRowsTheRackCoversUpFromBehindItsEdge() {
        val s =
            PigeonholeState(
                    LarkspurFolders.map { Cubby(it, it.replaceFirstChar(Char::uppercase)) },
                    LarkspurMail,
                )
                .apply { layout(412f, 600f, 140f, 460f) }
        s.toggleRack()
        s.run(600)
        s.cubTap(1)
        s.idle()
        val last = s.rows.last().id
        s.back()
        s.tick(.016f)
        // Nothing lifts on top of the rack as it comes up.
        assertTrue(s.letters.isEmpty())
        var cameUp = false
        var n = 0
        while ((s.busy() || s.letters.isNotEmpty()) && n++ < 400) {
            s.tick(.016f)
            val l = s.letters.firstOrNull { it.mail.id == last } ?: continue
            if (!l.below.isNaN()) {
                assertEquals(s.rackTop(), l.below, .01f)
                if (l.y + s.rowH / 2 <= l.below) cameUp = true
            }
        }
        assertTrue(cameUp, "the last row came up clear of the rack's edge")
        assertEquals(PigeonholeState.Inbox, s.view)
        assertTrue(s.letters.isEmpty())
    }

    @Test
    fun aLetterPulledOutOfACubbyStaysBehindItsRailUntilItIsClear() {
        val s = state()
        s.toggleRack()
        s.run(500)
        val cx = s.cubCenterX(1)
        val cy = s.cubCenterY(1)
        s.cubDown(1, cx, cy)
        var n = 0
        var y = cy
        while (s.carry == null && n++ < 20) {
            y -= 2f
            s.ptrMove(cx, y)
            if (s.carry == null) s.tick(.016f)
        }
        val l = s.carry!!.letter
        // Just picked up, it is drawn as the cubby drew it: cut off at the rail's top.
        assertEquals(s.railTop(1, s.rackTop()), l.below, .01f)
        s.ptrMove(cx, cy - 200f)
        s.tick(.016f)
        assertTrue(l.below.isNaN(), "lifted clear, it is drawn whole")
        assertEquals(-1, l.behindRail)
    }

    private fun PigeonholeState.busy() = !idle
}
