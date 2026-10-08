package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MagnetFieldTest {
    private val magnet = Offset(0f, 0f)

    @Test
    fun thePullFallsWithTheSquareOfTheDistanceSoftenedUpClose() {
        val near = MagnetField.pull(Offset(100f, 0f), magnet, 1f).getDistance()
        val far = MagnetField.pull(Offset(200f, 0f), magnet, 1f).getDistance()
        val softening = MagnetDimens.Softening
        assertEquals(MagnetDimens.MagnetPull / (100f * 100f + softening), near, near * 1e-4f)
        assertEquals((200f * 200f + softening) / (100f * 100f + softening), near / far, 1e-3f)
    }

    @Test
    fun thePullPointsAtTheMagnetAndScalesWithTheMatch() {
        val full = MagnetField.pull(Offset(120f, 0f), magnet, 1f)
        val half = MagnetField.pull(Offset(120f, 0f), magnet, 0.5f)
        assertTrue(full.x < 0f && abs(full.y) < 1e-3f, "towards the magnet: $full")
        assertEquals(full.x / 2f, half.x, abs(full.x) * 1e-4f)
    }

    @Test
    fun noMatchOrOutOfReachFeelsNothing() {
        assertEquals(Offset.Zero, MagnetField.pull(Offset(100f, 0f), magnet, 0f))
        val beyond = MagnetDimens.Reach.value + 1f
        assertEquals(Offset.Zero, MagnetField.pull(Offset(beyond, 0f), magnet, 1f))
    }

    @Test
    fun rightOnTheMagnetThereIsNoPullAndNoNaN() {
        assertEquals(Offset.Zero, MagnetField.pull(magnet, magnet, 1f))
        val out = FloatArray(2)
        MagnetField.fieldAt(0f, 0f, floatArrayOf(0f), floatArrayOf(0f), 1, out)
        assertTrue(out[0].isFinite() && out[1].isFinite())
    }

    @Test
    fun twoMagnetsFieldsAdd() {
        val a = FloatArray(2)
        val b = FloatArray(2)
        val both = FloatArray(2)
        MagnetField.fieldAt(50f, 40f, floatArrayOf(0f), floatArrayOf(0f), 1, a)
        MagnetField.fieldAt(50f, 40f, floatArrayOf(160f), floatArrayOf(10f), 1, b)
        MagnetField.fieldAt(50f, 40f, floatArrayOf(0f, 160f), floatArrayOf(0f, 10f), 2, both)
        assertEquals(a[0] + b[0], both[0], 1e-5f)
        assertEquals(a[1] + b[1], both[1], 1e-5f)
    }

    @Test
    fun theFieldPointsAwayFromAMagnetAndIsAboutOneAtItsUnitDistance() {
        val out = FloatArray(2)
        val unit = MagnetDimens.FieldUnitDistance.value
        MagnetField.fieldAt(unit, 0f, floatArrayOf(0f), floatArrayOf(0f), 1, out)
        assertEquals(1f, out[0], 1e-3f)
        assertEquals(0f, out[1], 1e-5f)
    }

    @Test
    fun aPhotoMatchingTwoMagnetsEquallyRestsHalfwayBetweenThem() {
        val a = Offset(0f, 0f)
        val b = Offset(200f, 0f)
        fun net(at: Offset) = MagnetField.pull(at, a, 0.8f) + MagnetField.pull(at, b, 0.8f)
        assertTrue(net(Offset(100f, 0f)).getDistance() < 1e-2f, "balanced in the middle")
        // Off the line between them, either side, it is pulled back onto it. Along the line the
        // balance tips towards the nearer magnet, so sticking to both is what holds it there.
        assertTrue(net(Offset(100f, 60f)).y < 0f)
        assertTrue(net(Offset(100f, -60f)).y > 0f)
        assertTrue(abs(net(Offset(100f, 60f)).x) < 1e-2f)
    }
}
