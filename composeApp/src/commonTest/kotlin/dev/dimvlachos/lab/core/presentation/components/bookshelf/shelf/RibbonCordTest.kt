package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RibbonCordTest {

    private val frame = RibbonFrame(width = 300f, height = 450f, tail = 26f, depth = 12f)

    private fun RibbonCord.run(from: Float, to: Float, startMillis: Long, millis: Long): Long {
        val frames = (millis / FRAME_MS).toInt()
        for (k in 1..frames) {
            val t = k / frames.toFloat()
            update(frame, from + (to - from) * t, startMillis + k * FRAME_MS)
        }
        return startMillis + frames * FRAME_MS
    }

    private fun RibbonCord.hold(tuck: Float, startMillis: Long, millis: Long): Long =
        run(tuck, tuck, startMillis, millis)

    private fun RibbonCord.onPage(): List<Int> = (0 until size).filter { frame.inside(this[it]) }

    @Test
    fun aLaidRibbonLiesOnTheFrontOfThePage() {
        val cord = RibbonCord().apply { lay(frame) }

        assertTrue(cord.onPage().isNotEmpty())
        assertTrue((0 until cord.size).none(cord::isBehind))
    }

    @Test
    fun stowingPullsTheWholeRibbonBehindTheBook() {
        val cord = RibbonCord().apply { lay(frame) }

        val stowed = cord.run(0f, 1f, 0L, STOW_MS)
        cord.hold(1f, stowed, SETTLE_MS)

        val front = cord.onPage().filterNot(cord::isBehind)
        assertTrue(front.isEmpty(), "links left in front: $front")
    }

    @Test
    fun theRibbonGoesOverTheHeadGraduallyNeverInAJump() {
        val cord = RibbonCord().apply { lay(frame) }
        val frames = (STOW_MS / FRAME_MS).toInt()
        var front = cord.size
        var steepest = 0
        for (k in 1..frames) {
            cord.update(frame, k / frames.toFloat(), k * FRAME_MS)
            val now = (0 until cord.size).count { !cord.isBehind(it) }
            steepest = maxOf(steepest, front - now)
            front = now
        }

        assertTrue(steepest <= MOST_LINKS_PER_FRAME, "links lost in one frame: $steepest")
    }

    @Test
    fun unstowingLaysTheRibbonBackOnTheFrontOfThePage() {
        val cord = RibbonCord().apply { lay(frame) }
        var now = cord.run(0f, 1f, 0L, STOW_MS)
        now = cord.hold(1f, now, SETTLE_MS)

        now = cord.run(1f, LAST_TUCK, now, STOW_MS)

        val behind = (0 until cord.size).filter(cord::isBehind)
        assertTrue(behind.isEmpty(), "links left behind: $behind")
        val rest = RibbonCord().apply { lay(frame) }
        val far = (0 until cord.size).filter { (cord[it] - rest[it]).length() > NEAR_REST }
        assertTrue(far.isEmpty(), "links away from rest: $far")
        cord.update(frame, 0f, now + FRAME_MS)
        assertFalse((0 until cord.size).any(cord::isBehind))
    }

    private companion object {
        const val FRAME_MS = 16L
        const val STOW_MS = 1_400L
        const val SETTLE_MS = 2_000L
        const val LAST_TUCK = 0.01f
        const val NEAR_REST = 12f
        const val MOST_LINKS_PER_FRAME = 2
    }
}
