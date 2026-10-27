package dev.dimvlachos.lab.core.presentation.components.coinsorter

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoinSorterStateTest {
    // The prototype's eight lanterns: price, rating and metal.
    private val items =
        listOf(
            CoinItem(3499, 4.7, Metal.Nickel),
            CoinItem(5800, 4.8, Metal.Copper),
            CoinItem(1250, 4.4, Metal.Brass),
            CoinItem(7995, 4.6, Metal.Nickel),
            CoinItem(2199, 4.5, Metal.Brass),
            CoinItem(4400, 4.9, Metal.Copper),
            CoinItem(2749, 4.2, Metal.Nickel),
            CoinItem(9600, 4.3, Metal.Brass),
        )

    // The prototype's screen at 412 by 915: its toolbar starts 129.475 down.
    private fun state(w: Float = 412f, h: Float = 915f) =
        CoinSorterState(items).apply {
            layout(w, h, 129.475f)
            chipX = w - 16 - 120
            chipW = 120f
        }

    private fun CoinSorterState.runOut(ms: Int = 12000, each: (CoinSorterState) -> Unit = {}) {
        var t = 0
        while (running && t < ms) {
            step(1 / 60f)
            each(this)
            t += 16
        }
    }

    @Test
    fun theTrayIsLaidOutAsThePrototypes() {
        val s = state()
        assertEquals(1.1444f, s.k, .001f)
        assertTrue(
            s.coinD.indices.all { i ->
                s.coinD[i] < s.slotW[i] && (i == 0 || s.coinD[i] > s.slotW[i - 1])
            },
        )
        assertTrue(
            s.slotX[7] + s.slotW[7] / 2 < s.trayX1 - 12 * s.k,
            "the slots end before the stop",
        )
        assertTrue(
            s.coinD.indices.all { s.tubeBottom - s.coinD[it] > s.railY(s.slotX[it]) + 4 * s.k },
        )
        assertTrue(
            s.railY0 - s.coinD[7] > s.trayTop - 1,
            "the biggest coin rolls under the channel top",
        )
        assertTrue(s.rowY(7) + s.rowH <= 915f - 4)
    }

    @Test
    fun theFunnelHeapNeverRisesAboveTheRim() {
        val s = state()
        for (d in s.coinD) for (q in 1..7) assertTrue(s.pileY(q, d / 2) - d / 2 >= s.hopTop)
    }

    @Test
    fun aShortScreenKeepsTheLastRowOnScreen() {
        val s = state(360f, 640f)
        assertEquals(4f, s.rowGap)
        assertTrue(s.rowH >= 38f)
        assertTrue(s.slotX[7] + s.slotW[7] / 2 < s.trayX1 - 12 * s.k)
    }

    @Test
    fun lowToHighSendsEveryRowThroughTheTrayAndPutsTheCheapestFirst() {
        val s = state()
        var clinks = 0
        var thunks = 0
        var sawFly = false
        var sawRamp = false
        var sawTube = false
        var sawWell = false
        var overlap = false
        var wrongSlot = false
        s.onEvent = { e, _, _ ->
            if (e == CoinEvent.Clink) clinks++
            if (e == CoinEvent.Thunk) thunks++
        }
        assertTrue(s.pick(CoinSort.Low.ordinal))
        assertTrue(s.running)
        // The smallest coin carries a short price.
        assertEquals("$12", s.rows.first { it.slot == 0 }.face)
        assertEquals("", s.rows.first { it.slot == 0 }.cents)
        assertTrue(s.rows.all { it.face.isNotEmpty() })
        s.runOut { st ->
            val ramp = st.rows.filter { it.mode == CoinMode.Ramp }
            if (st.rows.any { it.mode == CoinMode.Fly }) sawFly = true
            if (ramp.isNotEmpty()) sawRamp = true
            if (st.rows.any { it.mode == CoinMode.Tube }) sawTube = true
            if ((0 until 8).any { st.wellOn(it) }) sawWell = true
            for (a in ramp.indices) for (b in a + 1 until ramp.size) {
                if (abs(ramp[a].x - ramp[b].x) < ramp[a].d / 2 + ramp[b].d / 2 - .5f) overlap = true
            }
            for (r in st.rows) {
                if (
                    (r.mode == CoinMode.Drop || r.mode == CoinMode.Tube) &&
                        abs(r.x - st.slotX[r.slot]) > .5f
                )
                    wrongSlot = true
            }
        }
        assertFalse(s.running)
        assertTrue(sawFly && sawRamp && sawTube && sawWell)
        assertFalse(overlap, "coins on the rail never overlap")
        assertFalse(wrongSlot, "each coin drops through its own slot")
        assertTrue(clinks >= 8)
        assertEquals(8, thunks)
        assertEquals(CoinSort.Low, s.sort)
        assertEquals(listOf(2, 4, 6, 0, 5, 1, 3, 7), s.order)
        assertTrue(s.rows.all { it.mode == CoinMode.Row && it.row == s.order.indexOf(it.index) })
        assertFalse((0 until 8).any { s.wellOn(it) })
    }

    @Test
    fun highToLowAndTopRatedDealTheBigCoinsToTheTop() {
        val s = state()
        s.pick(CoinSort.High.ordinal)
        s.runOut()
        assertEquals(7, s.order[0])
        s.pick(CoinSort.Top.ordinal)
        assertEquals("★4.9", s.rows[5].face)
        s.runOut()
        assertEquals(5, s.order[0])
        s.pick(CoinSort.Best.ordinal)
        s.runOut()
        assertEquals((0 until 8).toList(), s.order)
        assertTrue(s.asBegun)
    }

    @Test
    fun theSameSortDoesNothing() {
        val s = state()
        var already = false
        s.onEvent = { e, _, _ -> if (e == CoinEvent.Already) already = true }
        assertFalse(s.pick(CoinSort.Best.ordinal))
        assertTrue(already)
        assertFalse(s.running)
    }

    @Test
    fun theChipTapsOpenAndClosedAndWontOpenMidSort() {
        val s = state()
        val cx = s.chipX + 60
        val cy = s.chipY + 17
        s.pressDown(cx, cy)
        s.pressUp(cx, cy)
        assertTrue(s.menuOpen)
        s.pressDown(cx, cy)
        s.pressUp(cx, cy)
        assertFalse(s.menuOpen)
        s.pick(CoinSort.Low.ordinal)
        s.pressDown(cx, cy)
        s.pressUp(cx, cy)
        assertFalse(s.menuOpen)
    }

    @Test
    fun pressAndReleaseOnAnOptionPicksItEvenWithNoMoves() {
        val s = state()
        val oy = s.optY(CoinSort.High.ordinal) + 21
        val ox = s.menuX + 100
        s.pressDown(s.chipX + 60, s.chipY + 17)
        s.pressUp(ox, oy)
        assertTrue(s.running)
        assertEquals(CoinSort.High, s.pending)
    }

    @Test
    fun aDragLightsTheOptionAndACancelOverItPicksIt() {
        val s = state()
        val oy = s.optY(CoinSort.Top.ordinal) + 21
        val ox = s.menuX + 100
        s.pressDown(s.chipX + 60, s.chipY + 17)
        s.pressMove(s.chipX + 40, s.chipY + 47)
        s.pressMove(ox, oy)
        assertTrue(s.menuOpen)
        assertEquals(CoinSort.Top.ordinal, s.hot)
        s.pressCancel()
        assertEquals(CoinSort.Top, s.pending)
    }

    @Test
    fun aCancelAwayFromTheOptionsClosesTheMenu() {
        val s = state()
        s.pressDown(s.chipX + 60, s.chipY + 17)
        s.pressMove(20f, 600f)
        assertTrue(s.menuOpen)
        s.pressCancel()
        assertFalse(s.menuOpen)
        assertFalse(s.running)
    }

    @Test
    fun onAnyFrameRateACoinReachesItsOwnTubeAndNeverSinksThroughItsFloor() {
        val frames = floatArrayOf(1 / 60f, .0173f, 1 / 144f, .0211f, 1 / 50f, .0071f)
        for (slot in 0 until 8) for (s0 in frames.indices) {
            val s = state()
            // Its slot set by a sort that is then stopped from dealing: only the rail and drop run.
            val r = s.rows[0]
            r.slot = slot
            r.d = s.coinD[slot]
            r.mode = CoinMode.Ramp
            r.x = s.hopCx
            r.y = s.railY(s.hopCx) - r.d / 2
            r.vx = 330 * s.k
            r.passed = 0
            var n = 0
            while (n < 900 && r.mode != CoinMode.Tube) {
                s.step(frames[(s0 + n) % frames.size])
                assertTrue(r.y <= s.tubeBottom - r.d / 2 + .01f)
                n++
            }
            assertEquals(CoinMode.Tube, r.mode)
            assertEquals(s.slotX[slot], r.x, .01f)
            assertEquals(slot, r.passed)
        }
    }

    @Test
    fun reducedMotionMovesTheRowsStraightToTheirLines() {
        val s = state()
        s.reducedMotion = true
        var sorted = false
        s.onEvent = { e, _, _ -> if (e == CoinEvent.Sorted) sorted = true }
        assertTrue(s.pick(CoinSort.Low.ordinal))
        assertFalse(s.running)
        assertTrue(sorted)
        assertEquals(listOf(2, 4, 6, 0, 5, 1, 3, 7), s.order)
        assertTrue(s.settled)
    }

    @Test
    fun resetMidSortPutsEverythingBack() {
        val s = state()
        s.pick(CoinSort.Low.ordinal)
        repeat(30) { s.step(1 / 60f) }
        assertTrue(s.rows.any { it.mode != CoinMode.Row })
        s.reset()
        assertTrue(s.asBegun)
        repeat(30) { s.step(1 / 60f) }
        assertTrue(s.rows.all { it.mode == CoinMode.Row })
    }

    @Test
    fun aRowNarrowsToItsCoinAsItTips() {
        val s = state()
        s.pick(CoinSort.Low.ordinal)
        val pose = FloatArray(3)
        // Row 0 tips at once: near the end of its tip it is almost edge-on and narrowing to its
        // coin.
        repeat(47) { s.step(1 / 240f) }
        s.rowPose(0, pose)
        assertTrue(pose[0] > 75f, "${pose[0]}")
        assertTrue(pose[2] < .3f, "${pose[2]}")
    }

    @Test
    fun ratingsPrintAsThePrototypes() {
        assertEquals("4.7", CoinSorterState.rating(4.7))
        assertEquals("5", CoinSorterState.rating(5.0))
    }
}
