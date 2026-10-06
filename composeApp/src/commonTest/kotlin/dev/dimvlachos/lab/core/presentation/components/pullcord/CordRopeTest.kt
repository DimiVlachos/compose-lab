package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CordRopeTest {
    private val top = Offset(100f, 50f)

    private fun rope() = CordRope(points = 12, segment = 10f).apply { hang(top) }

    private fun CordRope.run(seconds: Float) {
        repeat((seconds / PullCordDimens.StepSeconds).toInt()) {
            pin(top)
            step(PullCordDimens.StepSeconds)
        }
    }

    // Held out at its full length, [angle] radians anticlockwise of straight down, until it hangs
    // there, and let go: as a hand would swing it out.
    private fun CordRope.swingOut(angle: Float) {
        val end = top + Offset(sin(angle), cos(angle)) * (10f * (size - 1))
        repeat(120) {
            pin(top)
            hold(end, stretch = 1f)
            step(PullCordDimens.StepSeconds)
        }
        letGo()
    }

    @Test
    fun itHangsStraightDownFromWhereItIsPinned() {
        val rope = rope()
        assertEquals(top, rope[0])
        for (i in 0 until rope.size) {
            assertEquals(top.x, rope[i].x, 1e-3f)
            assertEquals(top.y + i * 10f, rope[i].y, 1e-3f)
        }
    }

    @Test
    fun theTopStaysWhereItIsPinnedWhileTheRestSwings() {
        val rope = rope()
        rope.swingOut(0.8f)
        repeat(240) {
            rope.pin(top)
            rope.step(PullCordDimens.StepSeconds)
            assertEquals(top, rope[0])
        }
    }

    @Test
    fun noSegmentStretchesPastItsLengthAsItSwings() {
        val rope = rope()
        rope.swingOut(1.2f)
        repeat(360) {
            rope.pin(top)
            rope.step(PullCordDimens.StepSeconds)
            for (i in 1 until rope.size) {
                val length = (rope[i] - rope[i - 1]).getDistance()
                assertTrue(length <= 10f * 1.03f, "segment $i is $length long")
            }
        }
    }

    @Test
    fun aSwingDiesAwayAndTheCordComesToRestHangingStraightDown() {
        val rope = rope()
        rope.swingOut(-1f)
        rope.run(seconds = 8f)
        assertTrue(rope.still, "still moving")
        for (i in 0 until rope.size) {
            assertTrue(abs(rope[i].x - top.x) < 0.5f, "point $i hangs at x ${rope[i].x}")
        }
        assertTrue(rope[rope.size - 1].y > top.y + 108f, "the cord hangs its full length")
    }

    @Test
    fun aHeldEndGoesWhereItIsHeldAndTheCordStretchesToReachIt() {
        val rope = rope()
        val end = top + Offset(0f, 130f)
        repeat(60) {
            rope.pin(top)
            rope.hold(end, stretch = 130f / 110f)
            rope.step(PullCordDimens.StepSeconds)
        }
        assertEquals(end, rope[rope.size - 1])
        // Sagging a little under its own weight between the two, but close to even.
        for (i in 1 until rope.size) {
            val length = (rope[i] - rope[i - 1]).getDistance()
            assertEquals(130f / 11f, length, 0.5f)
        }
    }
}
