package dev.dimvlachos.lab.core.presentation.components.spider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpiderRefreshStateTest {
    private val events = mutableListOf<SpiderEvent>()
    private var clock = 0L

    private fun state(reduced: Boolean = false) =
        SpiderRefreshState().apply {
            reducedMotion = reduced
            layout(412f, 700f)
            listHeight = 1100f
            onEvent = { events += it }
        }

    /** Steps [seconds] at 60 fps, measuring hanging cards as the feed composable would. */
    private fun SpiderRefreshState.run(seconds: Float) {
        repeat((seconds * 60).toInt()) {
            for (i in hanging.indices) cardMeasured(i, 120f)
            clock += 16
            step(1 / 60f)
        }
    }

    /** A finger drags from ([x], [y]) by [dy] in [n] moves, as sparse as a fast real swipe. */
    private fun SpiderRefreshState.drag(x: Float, y: Float, dy: Float, n: Int = 5) {
        down(x, y, real = true, atMs = clock)
        for (i in 1..n) {
            clock += 40
            move(x, y + dy * i / n, clock)
        }
    }

    @Test
    fun aShortPullPeeksAndScurriesHomeEmptyHanded() {
        val s = state()
        s.drag(200f, 100f, 92f)
        assertEquals(SpiderPhase.Pulling, s.phase)
        assertTrue(s.pull in 50f..60f, "pull ${s.pull}")
        assertFalse(s.armed)
        s.up(200f, 192f, clock)
        assertEquals(SpiderPhase.Retreat, s.phase)
        s.run(1.5f)
        assertEquals(SpiderPhase.Idle, s.phase)
        assertEquals(0f, s.pull)
        assertEquals(SpiderRefreshState.DefaultFeed, s.posts.toList())
        assertFalse(s.awake)
    }

    @Test
    fun aFullPullFetches() {
        val s = state()
        s.drag(200f, 100f, 218f)
        assertTrue(s.armed)
        assertEquals(listOf(SpiderEvent.ArmByFinger), events.filter { it.name.startsWith("Arm") })
        s.up(200f, 318f, clock)
        assertEquals(SpiderPhase.Refresh, s.phase)
        assertTrue(s.busy)
        s.run(2.5f)
        // The ball has opened into the batch, hanging below the spider.
        assertEquals(listOf(5, 6), s.hanging.toList())
        assertTrue(s.reeling)
        s.run(4f)
        assertEquals(SpiderPhase.Idle, s.phase)
        assertFalse(s.busy)
        assertEquals(listOf(5, 6, 0, 1, 2, 3, 4), s.posts.toList())
        assertTrue(s.hanging.isEmpty())
        assertEquals(setOf(5, 6), s.fresh)
        assertEquals(2, s.said)
        assertEquals(0f, s.listShift)
        assertTrue(s.spiderY <= s.hiddenY + .5f)
        assertEquals(
            listOf(SpiderEvent.Spin, SpiderEvent.Catch, SpiderEvent.Climb, SpiderEvent.Settle),
            events.filter {
                it in
                    setOf(
                        SpiderEvent.Spin,
                        SpiderEvent.Catch,
                        SpiderEvent.Climb,
                        SpiderEvent.Settle,
                    )
            },
        )
    }

    @Test
    fun theReelPushesTheListDownByTheHangingCardsSoNothingOverlaps() {
        val s = state()
        s.drag(200f, 100f, 218f)
        s.up(200f, 318f, clock)
        // Run to the middle of the climb.
        s.run(3.9f)
        assertTrue(s.reeling)
        val total = 2 * (120f + SpiderRefreshState.Gap)
        assertEquals(s.reelT - SpiderRefreshState.Pad + total, s.reelL, .01f)
    }

    @Test
    fun aCancelledTouchPastTheThresholdGoesHomeWithoutFetching() {
        val s = state()
        s.drag(200f, 100f, 260f)
        assertTrue(s.armed)
        s.cancel()
        assertEquals(SpiderPhase.Retreat, s.phase)
        s.run(2f)
        assertEquals(SpiderRefreshState.DefaultFeed, s.posts.toList())
        assertTrue(s.hanging.isEmpty())
    }

    @Test
    fun theButtonGlidesAScrolledFeedBackToTheTopAndFetches() {
        val s = state()
        s.drag(200f, 600f, -300f)
        s.up(200f, 300f, clock)
        s.run(2f)
        assertTrue(s.scroll > 250f, "scroll ${s.scroll}")
        s.refreshButton()
        assertTrue(s.busy)
        s.run(1.6f)
        assertEquals(0f, s.scroll)
        assertTrue(s.armed)
        assertEquals(SpiderEvent.Arm, events.last { it.name.startsWith("Arm") })
        s.run(6f)
        assertEquals(listOf(5, 6, 0, 1, 2, 3, 4), s.posts.toList())
    }

    @Test
    fun afterFourBatchesItComesBackEmptyHandedAndSaysSo() {
        val s = state()
        repeat(4) {
            s.refreshButton()
            s.run(7f)
        }
        assertEquals(11, s.posts.size)
        s.refreshButton()
        s.run(2.6f)
        assertTrue(s.toast)
        assertEquals(0, s.said)
        assertTrue(SpiderEvent.Empty in events)
        s.run(4f)
        assertEquals(SpiderPhase.Idle, s.phase)
        assertFalse(s.toast)
        assertEquals(11, s.posts.size)
        assertTrue(s.fresh.isEmpty())
    }

    @Test
    fun aFlickCoastsAndStopsAtTheEnd() {
        val s = state()
        s.drag(200f, 600f, -80f, n = 4)
        s.up(200f, 520f, clock)
        val before = s.scroll
        s.run(.6f)
        assertTrue(s.scroll > before + 20f, "${s.scroll} after $before")
        s.drag(200f, 600f, -500f, n = 4)
        s.up(200f, 100f, clock)
        s.run(3f)
        assertEquals(s.maxScroll, s.scroll)
        assertFalse(s.awake)
    }

    @Test
    fun aFingerCatchesTheSpiderOnItsWayHome() {
        val s = state()
        s.drag(200f, 100f, 70f)
        s.up(200f, 170f, clock)
        s.run(.1f)
        val p = s.pull
        assertTrue(p > 0f)
        s.down(200f, 200f, real = true, atMs = clock)
        assertEquals(SpiderPhase.Pulling, s.phase)
        clock += 40
        s.move(200f, 330f, clock)
        assertTrue(s.pull > p, "${s.pull} after $p")
    }

    @Test
    fun withReducedMotionTheRefreshIsAlmostInstant() {
        val s = state(reduced = true)
        s.refreshButton()
        s.run(.2f)
        assertEquals(listOf(5, 6, 0, 1, 2, 3, 4), s.posts.toList())
        assertEquals(SpiderPhase.Idle, s.phase)
    }

    @Test
    fun aTouchDuringARefreshIsIgnored() {
        val s = state()
        s.refreshButton()
        s.run(.5f)
        s.drag(200f, 100f, 200f)
        assertFalse(s.held)
        s.refreshButton()
        s.run(7f)
        assertEquals(7, s.posts.size)
    }

    @Test
    fun aScreenReaderScrollsWithinTheList() {
        val s = state()
        assertTrue(s.scrollBy(150f))
        assertEquals(150f, s.scroll)
        assertTrue(s.scrollBy(1000f))
        assertEquals(s.maxScroll, s.scroll)
        assertFalse(s.scrollBy(10f))
        s.refreshButton()
        assertFalse(s.scrollBy(-50f))
    }

    @Test
    fun resetBringsBackTheFirstFeed() {
        val s = state()
        s.refreshButton()
        s.run(7f)
        s.reset()
        assertEquals(SpiderRefreshState.DefaultFeed, s.posts.toList())
        assertTrue(s.fresh.isEmpty())
        assertEquals(-1, s.said)
        s.refreshButton()
        s.run(7f)
        assertEquals(listOf(5, 6, 0, 1, 2, 3, 4), s.posts.toList())
    }
}
