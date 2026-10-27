package dev.dimvlachos.lab.core.presentation.components.photocopier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotocopierStateTest {
    private val lists =
        listOf(
            CopierItem(1, "Weekly meal plan", "Mon tacos", "12 items · Edited Sunday"),
            CopierItem(2, "Thanksgiving at the Parkers’", "Sweet potato", "9 items · Nov 27"),
            CopierItem(3, "Road trip: Asheville", "Pigeon Creek Diner", "4 stops · Shared"),
            CopierItem(4, "Grocery run", "Oat milk, eggs", "8 items · Today"),
            CopierItem(5, "Packing list: Lake Tahoe", "Fleece", "15 items · Edited Friday"),
            CopierItem(
                6,
                "Soccer snack schedule",
                "Our week is Oct 18",
                "Fall season · 6 families",
            ),
        )
    private val orig = lists.map { it.title }

    private fun copier(reduced: Boolean = false, w: Float = 412f, h: Float = 820f) =
        PhotocopierState(lists).apply {
            reducedMotion = reduced
            // The prototype's app bar and search take the top 133 dp.
            layout(w, h, 133f, h - 133f)
            reset()
        }

    /** Steps [ms] at 60 fps, stopping early once [until] holds; returns whether it did. */
    private fun PhotocopierState.run(ms: Int, until: () -> Boolean = { false }): Boolean {
        var t = 0
        while (t < ms) {
            if (until()) return true
            step(1f / 60f)
            t += 16
        }
        return until()
    }

    private fun PhotocopierState.titles() = cards.map { it.item.title }

    private fun PhotocopierState.idle() = !busy && fliers.isEmpty()

    private fun PhotocopierState.dup(i: Int) {
        duplicate(cards[i].item.id)
        assertTrue(run(9000) { idle() && copMode == CopierMode.Peek }, "the duplicate finishes")
    }

    private fun PhotocopierState.cardY(i: Int) = rectY(cards[i].item.id)

    @Test
    fun itStartsWithSixListsAndTheCopierOutOfSight() {
        val s = copier()
        assertEquals(orig, s.titles())
        assertTrue(s.copY > s.height)
        assertTrue(s.asBegun)
    }

    @Test
    fun aShortTapDoesNotOpenTheMenuButAHeldPressDoes() {
        val s = copier()
        val id = s.cards[0].item.id
        val y = s.cardY(0) + 30
        s.cardDown(id, 60f, y)
        s.run(120)
        s.cardUp(60f, y)
        s.run(500)
        assertEquals(0, s.menuFor)
        s.cardDown(id, 60f, y)
        s.run(400)
        assertEquals(0, s.menuFor, "not yet at 400 ms")
        s.run(150)
        assertEquals(id, s.menuFor, "open by 450 ms")
        s.cardUp(60f, y)
        s.run(80)
        assertEquals(id, s.menuFor, "lifting the finger keeps it open")
        s.closeMenu()
        s.tapMore(id)
        assertEquals(id, s.menuFor)
        assertTrue(s.menuX >= 12 && s.menuX + PhotocopierState.MenuW <= s.width)
        assertTrue(s.menuY + PhotocopierState.MenuH <= s.height)
    }

    @Test
    fun aPressThatMovesScrollsInsteadOfOpeningTheMenu() {
        val s = copier()
        val id = s.cards[0].item.id
        val y = s.cardY(0) + 30
        s.cardDown(id, 60f, y)
        s.cardMove(60f, y - 20)
        s.run(700)
        assertEquals(0, s.menuFor)
        s.cardCancel()
        assertFalse(s.busy)
    }

    @Test
    fun duplicateRunsTheCardThroughTheCopierAndTheCopyLandsUnderIt() {
        val s = copier()
        var sawFull = false
        var sawAway = false
        var sawScanShut = false
        var sawEmerge = false
        val heard = mutableListOf<CopierEvent>()
        s.onEvent = { heard += it }
        var said = ""
        s.onSay = { what, title -> said = "$what $title" }
        s.duplicate(s.cards[0].item.id)
        assertTrue(
            s.run(9000) {
                if (s.copMode == CopierMode.Full) sawFull = true
                if (s.cards[0].away) sawAway = true
                if (s.bar > 0 && s.lid < .02f && s.fliers.any { it.onGlass }) sawScanShut = true
                if (s.fliers.any { it.inSlot }) sawEmerge = true
                s.idle() && s.copMode == CopierMode.Peek
            },
        )
        assertTrue(sawFull, "the copier rises")
        assertTrue(sawAway, "the original leaves a dashed outline")
        assertTrue(sawScanShut, "the bar sweeps with the lid shut on the original")
        assertTrue(sawEmerge, "the copy slides out of the slot")
        assertEquals(listOf(orig[0], orig[0] + " (copy)") + orig.drop(1), s.titles())
        assertEquals("Just now · 12 items", s.cards[1].item.meta)
        assertTrue(s.snackOn, "Undo shows")
        assertTrue(s.cards.none { it.ghost || it.away })
        assertEquals(0f, s.lid)
        assertEquals("Duplicated Weekly meal plan", said)
        for (e in
            listOf(CopierEvent.Rise, CopierEvent.Thunk, CopierEvent.Hum, CopierEvent.Landed)) {
            assertTrue(e in heard, "$e heard")
        }
        val copy = s.cards[1]
        assertEquals(1f, copy.warm.value(s.now), "warm as it lands")
        s.run(4500)
        assertEquals(0f, copy.warm.value(s.now), "then cools")
        assertEquals(0f, copy.speck.value(s.now))
    }

    @Test
    fun undoFeedsTheCopyBackAndTheListClosesUp() {
        val s = copier()
        s.dup(0)
        val original = s.undo()
        assertEquals(1, original)
        assertTrue(s.run(4000) { s.idle() && s.cards.size == 6 && s.copMode == CopierMode.Hidden })
        assertEquals(orig, s.titles())
        assertFalse(s.snackOn)
        // Once the Undo window's own timer has run out.
        assertTrue(s.run(6000) { s.asBegun && s.settled }, "and everything comes to rest")
    }

    @Test
    fun aDragIntoTheSlotFeedsTheCopyAndALetGoElsewherePutsItBack() {
        val s = copier()
        s.dup(0)
        var id = s.cards[1].item.id
        assertTrue(s.canFeed(id))
        assertFalse(s.canFeed(s.cards[0].item.id), "an original can't be fed")
        var y = s.cardY(1)
        s.cardDown(id, 80f, y + 30)
        s.cardMove(100f, s.slotY - 10)
        assertEquals(1f, s.hot, "over the slot")
        s.cardUp(100f, s.slotY - 10)
        assertTrue(s.run(4000) { s.idle() && s.cards.size == 6 })
        assertEquals(orig, s.titles())

        s.dup(0)
        id = s.cards[1].item.id
        y = s.cardY(1)
        s.cardDown(id, 80f, y + 30)
        s.cardMove(90f, y + 100)
        s.cardUp(90f, y + 100)
        assertTrue(s.run(1500) { s.fliers.isEmpty() })
        assertEquals(7, s.cards.size)
        assertFalse(s.cards[1].ghost)
        assertTrue(s.snackOn, "the Undo window starts over")
        // A swipe with no moves in between still feeds it.
        s.cardDown(id, 80f, y + 30)
        s.cardUp(80f, s.slotY - 8)
        assertTrue(s.run(4000) { s.idle() && s.cards.size == 6 })

        s.dup(0)
        id = s.cards[1].item.id
        y = s.cardY(1)
        // A cancelled drag finishes from where the finger was.
        s.cardDown(id, 80f, y + 30)
        s.cardMove(80f, s.slotY - 12)
        s.cardCancel()
        assertTrue(s.run(4000) { s.idle() && s.cards.size == 6 })
        assertEquals(orig, s.titles())
    }

    @Test
    fun aCopyOfACopyIsCopyTwoAndTheCopierLeavesWhenTheWindowEnds() {
        val s = copier()
        s.dup(0)
        s.dup(1)
        assertEquals(orig[0] + " (copy)", s.cards[1].item.title)
        assertEquals(orig[0] + " (copy 2)", s.cards[2].item.title)
        assertTrue(s.run(7000) { s.copMode == CopierMode.Hidden })
        assertFalse(s.snackOn)
        s.run(300)
        val id = s.cards[1].item.id
        assertFalse(s.canFeed(id), "once it has gone a drag doesn't feed")
        s.cardDown(id, 60f, s.cardY(1) + 30)
        s.cardUp(60f, s.height - 20)
        assertEquals(8, s.cards.size)
        assertFalse(s.busy)
    }

    @Test
    fun pinMovesTheListToTheTop() {
        val s = copier()
        s.scroll = 60f
        val last = s.cards.last().item
        s.pin(last.id)
        assertEquals(last.title, s.cards[0].item.title)
        assertEquals(0f, s.scroll)
        assertTrue(s.cards.any { it.slide(s.now) != 0f }, "the cards slide, not jump")
        s.run(600)
        assertTrue(s.cards.all { it.slide(s.now) == 0f })
    }

    @Test
    fun aLowListScrollsClearOfTheRaisedCopierAndEasesBackAfter() {
        val s = copier(w = 360f, h = 640f)
        val id = s.cards.last().item.id
        s.duplicate(id)
        s.run(300)
        val clear = s.copTarget(CopierMode.Full) - 44
        assertTrue(s.rectY(id) + 2 * s.cardH + 8 <= clear + 1, "the original and its copy's slot")
        assertTrue(s.run(9000) { s.idle() && s.copMode == CopierMode.Peek })
        val copy = s.cards.last()
        assertTrue(s.rectY(copy.item.id) + s.cardH < s.copTarget(CopierMode.Peek), "copy seen")
        val padded = s.scroll
        assertTrue(s.run(7000) { s.copMode == CopierMode.Hidden })
        s.run(600)
        assertEquals(120f, s.padBottom, "the extra room goes")
        assertTrue(s.scroll < padded && s.scroll <= s.maxScroll, "eased back, no blank tail")
    }

    @Test
    fun theListStillScrollsDuringTheUndoWindow() {
        val s = copier(w = 360f, h = 640f)
        s.dup(0)
        assertTrue(s.feedable)
        assertTrue(s.scrollBy(60f) > 0f)
    }

    @Test
    fun theMenuClosesWhenTheListScrollsUnderIt() {
        val s = copier(w = 360f, h = 640f)
        s.tapMore(s.cards[1].item.id)
        s.scrollBy(1f)
        assertTrue(s.menuFor != 0, "a nudge leaves it")
        s.scrollBy(4f)
        assertEquals(0, s.menuFor)
    }

    @Test
    fun withReducedMotionTheCopyJustAppearsAndUndoRemovesIt() {
        val s = copier(reduced = true)
        s.duplicate(s.cards[0].item.id)
        assertEquals(7, s.cards.size)
        assertTrue(s.snackOn)
        assertFalse(s.busy)
        s.undo()
        assertEquals(orig, s.titles())
    }

    @Test
    fun resetMidRunLeavesNothingBehind() {
        val s = copier()
        s.duplicate(s.cards[0].item.id)
        s.run(1800)
        assertTrue(s.busy)
        s.reset()
        s.run(3000)
        assertTrue(s.asBegun)
        assertTrue(s.cards.none { it.away || it.ghost })
        assertEquals(0f, s.lid)
        assertFalse(s.snackOn)
    }

    @Test
    fun theCopierFitsTheScreenAndTheCardFitsItsGlass() {
        for ((w, h) in listOf(360f to 640f, 412f to 820f)) {
            val s = copier(w = w, h = h)
            assertTrue(s.bx >= 0 && s.bx + s.bw <= w)
            assertTrue(s.gs > .5f && s.cardH * s.gs < s.gh - 10 && s.cardW * s.gs < s.gw - 10)
        }
    }
}
