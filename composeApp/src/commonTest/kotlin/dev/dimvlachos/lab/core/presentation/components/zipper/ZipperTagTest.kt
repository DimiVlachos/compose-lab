package dev.dimvlachos.lab.core.presentation.components.zipper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipperTagTest {
    @Test
    fun theEyeletHangsWhereThePrototypePutsIt() {
        // Worked by hand from the prototype's tagPose at swing 1.35, scale .86: the eyelet 29.27 dp
        // left of the hole and 6.57 below, the frame turned -.2208 rad, its origin 10.91 left of
        // the eyelet and 2.45 below it.
        assertEquals(-29.2717f, ZipperTag.eyeletDx(1.35f), 1e-3f)
        assertEquals(6.5702f, ZipperTag.eyeletDy(1.35f), 1e-3f)
        assertEquals(-.220796f, ZipperTag.angle(1.35f), 1e-5f)
        assertEquals(-29.2717f - 10.9086f, ZipperTag.originDx(1.35f, .86f), 1e-3f)
        assertEquals(6.5702f + 2.4485f, ZipperTag.originDy(1.35f, .86f), 1e-3f)
    }

    @Test
    fun atRestItHangsLeftOfTheTapeAndClearOfMostOfTheText() {
        // The hole is 1 dp left of the zipper's middle; the tape is 16 dp each side of it, and the
        // rows' text stops 52 dp in from the list's right edge, 26 dp left of the zipper's middle.
        for (th in floatArrayOf(ZipperTag.SwingRest, ZipperTag.SwingHeld)) {
            for (k in floatArrayOf(ZipperTag.scale(0f), ZipperTag.scale(1f))) {
                val right = -1f + ZipperTag.extentX(th, k, right = true)
                val left = -1f + ZipperTag.extentX(th, k, right = false)
                assertTrue(right < -16f, "its right edge $right is on the tape")
                // Held and lifted it reaches about 50 dp into the text's column, where the old tag
                // that swung out on a hold reached 68; lying at rest, 40.
                assertTrue(left > -26f - 52f, "its left edge $left reaches far into the text")
            }
        }
    }

    @Test
    fun itNeverHangsOffTheListArea() {
        val height = 500f
        for (hy in listOf(10f, 40f, 250f, 470f, 490f)) {
            for (i in 0..20) {
                val th = ZipperTag.SwingMin + (ZipperTag.SwingMax - ZipperTag.SwingMin) * i / 20f
                val k = ZipperTag.scale(i / 20f)
                val fit = ZipperTag.fit(th, hy, k, height)
                val top = hy + ZipperTag.extentY(fit, k, bottom = false)
                val bottom = hy + ZipperTag.extentY(fit, k, bottom = true)
                assertTrue(top >= 0f && bottom <= height, "at $hy, $th: from $top to $bottom")
            }
        }
    }

    @Test
    fun withRoomToHangItStaysWhereItIs() {
        assertEquals(ZipperTag.SwingRest, ZipperTag.fit(ZipperTag.SwingRest, 200f, 1f, 600f))
    }

    @Test
    fun theHoleRisesAsTheTabStandsUp() {
        assertTrue(ZipperTag.holeY(100f, 1.2f) < ZipperTag.holeY(100f, 0f))
        assertEquals(100f + 30f - 6.5f + 34f - 7f, ZipperTag.holeY(100f, 0f))
    }
}
