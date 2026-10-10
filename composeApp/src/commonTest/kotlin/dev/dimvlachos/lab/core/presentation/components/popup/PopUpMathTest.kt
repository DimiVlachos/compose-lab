package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import dev.dimvlachos.lab.core.presentation.components.perspective.Homography
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PopUpMathTest {
    private val pi = PI.toFloat()

    @Test
    fun aLeafAtZeroLiesOnTheNearStackAndAtPiOnTheFar() {
        assertVec(Vec3(0f, -1f, 0f), PopUpMath.across(0f))
        assertVec(Vec3(0f, 1f, 0f), PopUpMath.across(pi))
    }

    @Test
    fun aPieceStandsUprightWhenItsSpreadIsOpenFlat() {
        assertVec(Vec3(0f, 0f, 1f), PopUpMath.up(near = 0f, far = pi))
    }

    @Test
    fun aPieceLiesFlatWhenItsSpreadIsShut() {
        assertVec(PopUpMath.across(0f), PopUpMath.up(near = 0f, far = 0f))
    }

    @Test
    fun aPieceOnTheNearPageStandsAtItsDistanceFromTheGutter() {
        val piece = piece(PopUpSide.Near, fromGutter = 70f)
        assertVec(Vec3(5f, -70f, 0f), PopUpMath.base(piece, near = 0f, far = pi, x = 5f))
    }

    @Test
    fun aPieceOnTheFarPageStandsOnTheFarPage() {
        val piece = piece(PopUpSide.Far, fromGutter = 40f)
        assertVec(Vec3(5f, 40f, 0f), PopUpMath.base(piece, near = 0f, far = pi, x = 5f))
    }

    @Test
    fun aLeafShowsItsFrontUntilItPassesTheEye() {
        assertTrue(PopUpMath.leafFrontSeen(PopUpMath.CameraAngle - 0.01f))
        assertFalse(PopUpMath.leafFrontSeen(PopUpMath.CameraAngle + 0.01f))
    }

    @Test
    fun anUprightPieceShowsItsFrontAndAFoldedOneItsBack() {
        assertTrue(PopUpMath.pieceFrontSeen(near = 0f, far = pi))
        assertFalse(PopUpMath.pieceFrontSeen(near = 0f, far = 0.2f))
    }

    @Test
    fun paperFacingTheLightIsBrighterThanPaperTurnedFromIt() {
        val lit = PopUpMath.brightness(Vec3(0f, 0f, 1f))
        val away = PopUpMath.brightness(Vec3(0f, 1f, 0f))
        assertTrue(lit > away)
        assertTrue(lit <= 1f && away >= 0.4f)
    }

    @Test
    fun theOpenSpreadsPiecesAreDrawnLast() {
        // The cover turned over (π), leaf 1 down (0): spread 0 is open and holds the eye.
        val out = IntArray(8)
        val n = PopUpMath.drawOrder(floatArrayOf(pi, 0f), out)
        assertEquals(PopUpMath.Spread, out[n - 1])
    }

    @Test
    fun aSpreadBeingShutStaysDrawnBehindTheLeafShuttingIt() {
        // Leaf 1 a little past upright, shutting spread 0: its pieces don't vanish, they are drawn
        // before leaf 1, which covers them as it comes over. The eye looks into spread 1, last.
        val out = IntArray(8)
        val n = PopUpMath.drawOrder(floatArrayOf(pi, 100f * pi / 180f), out)
        val order = out.take(n)
        assertTrue(PopUpMath.Spread in order, "$order")
        assertTrue(order.indexOf(PopUpMath.Spread) < order.indexOf(1), "$order")
        assertEquals(PopUpMath.Spread + 1, order.last())
    }

    @Test
    fun aSpreadBehindTheRisingCoverIsDrawnUnderIt() {
        // The cover just lifting: spread 0 lies under it and is drawn first, so the cover hides it
        // and nothing pops into view as the cover passes the eye.
        val out = IntArray(8)
        val n = PopUpMath.drawOrder(floatArrayOf(0.4f, 0f), out)
        val order = out.take(n)
        assertTrue(order.indexOf(PopUpMath.Spread) in 0 until order.indexOf(0), "$order")
    }

    @Test
    fun aBookOfManySpreadsIsOrdered() {
        // Forty leaves fanned out, so every spread but the last (whose leaf lies on the board) is
        // a little open: 40 leaves and the board, and 39 spreads, to order.
        val angles = FloatArray(40) { pi * (39 - it) / 39f }
        val out = IntArray(2 * angles.size + 1)
        val n = PopUpMath.drawOrder(angles, out)
        assertEquals(80, n)
        assertTrue(out[n - 1] >= PopUpMath.Spread)
    }

    @Test
    fun aShutSpreadIsNotDrawn() {
        val out = IntArray(8)
        val n = PopUpMath.drawOrder(floatArrayOf(0f, 0f), out)
        assertFalse(out.take(n).any { it >= PopUpMath.Spread })
    }

    @Test
    fun everyLeafAndTheBoardAreDrawnOnce() {
        val out = IntArray(8)
        val n = PopUpMath.drawOrder(floatArrayOf(pi, 1f), out)
        val leaves = out.take(n).filter { it < PopUpMath.Spread }.sorted()
        assertEquals(listOf(0, 1, 2), leaves)
    }

    @Test
    fun theBoardIsUnderTheNearStack() {
        val out = IntArray(8)
        val n = PopUpMath.drawOrder(floatArrayOf(0f, 0f), out)
        val order = out.take(n)
        assertTrue(order.indexOf(2) < order.indexOf(1))
        assertTrue(order.indexOf(1) < order.indexOf(0))
    }

    @Test
    fun aShadowLandsOnItsPage() {
        val p = PopUpMath.ontoPage(Vec3(10f, -20f, 40f), pageAngle = 0f)
        assertEquals(0f, p.z, 1e-4f)
    }

    @Test
    fun aLeafLyingOnTheNearStackShadesOnlyItsOwnFootprint() {
        val c = PopUpMath.castOnTable(angle = 0f, width = 300f, depth = 190f)
        assertTrue(c.all { it.z == 0f && it.y <= 1e-3f && it.y >= -190.01f }, "$c")
    }

    @Test
    fun anUprightLeafCastsItsShadowAcrossTheFarSideAwayFromTheLight() {
        val c = PopUpMath.castOnTable(angle = pi / 2f, width = 300f, depth = 190f)
        assertTrue(c.all { it.z == 0f }, "$c")
        // The light is in front and to the left: the shadow falls behind the gutter and right.
        assertTrue(c.maxOf { it.y } > 50f, "$c")
        assertTrue(c.maxOf { it.x } > 150f, "$c")
    }

    @Test
    fun aLeafComingDownPullsItsShadowInWithIt() {
        // Shutting the book: a leaf lying on the far stack shades the far side. Lifting, its
        // raised edge throws the shadow a little further; then as it swings back over, the
        // shadow on the far side shrinks steadily, and well before it lies down it is gone.
        fun reach(a: Float) = PopUpMath.castOnTable(a, 300f, 190f).maxOf { it.y }
        assertEquals(190f, reach(pi), 0.5f)
        val coming = listOf(0.75f, 0.6f, 0.45f, 0.3f, 0.15f, 0f).map { reach(it * pi) }
        assertTrue(coming.zipWithNext().all { (a, b) -> b <= a }, "$coming")
        assertTrue(coming[1] < coming[0] && coming[2] < coming[1], "$coming")
        assertTrue(coming.last() <= 1e-3f, "$coming")
    }

    @Test
    fun shadowsFadeInOnlyNearlyOpen() {
        assertEquals(0f, PopUpMath.shadowFade(0f, 2f))
        assertEquals(1f, PopUpMath.shadowFade(0f, pi))
    }

    @Test
    fun theCameraDrawsTheGutterCentreAtItsCentre() {
        val camera = BookCamera(372f, 600f)
        val seen = camera.project(Vec3(0f, 0f, 0f))
        assertEquals(186f, seen.x, 1e-3f)
        assertEquals(246f, seen.y, 1e-3f)
    }

    @Test
    fun theFarPageIsDrawnHigherAndSmallerThanTheNear() {
        val camera = BookCamera(372f, 600f)
        val near = camera.project(Vec3(100f, -100f, 0f))
        val far = camera.project(Vec3(100f, 100f, 0f))
        assertTrue(far.y < near.y)
        assertTrue(far.x - 186f < near.x - 186f)
    }

    @Test
    fun aPlanesHomographyAgreesWithTheCamera() {
        val camera = BookCamera(372f, 600f)
        val h =
            camera.plane(
                Vec3(-50f, -30f, 10f),
                Vec3(1f, 0f, 0f),
                Vec3(0f, 0.6f, -0.8f),
                FloatArray(9),
            )
        val expected = camera.project(Vec3(-50f + 20f, -30f + 0.6f * 15f, 10f - 0.8f * 15f))
        val seen = Homography.project(h, 20f, 15f)
        assertEquals(expected.x, seen.x, 1e-2f)
        assertEquals(expected.y, seen.y, 1e-2f)
    }

    private fun piece(side: PopUpSide, fromGutter: Float) =
        PopUpPiece(
            art = ColorPainter(Color.Blue),
            side = side,
            fromGutter = fromGutter,
            x = 0f,
            width = 10f,
            height = 10f,
        )

    private fun assertVec(expected: Vec3, actual: Vec3) {
        assertEquals(expected.x, actual.x, 1e-4f, "x")
        assertEquals(expected.y, actual.y, 1e-4f, "y")
        assertEquals(expected.z, actual.z, 1e-4f, "z")
    }
}
