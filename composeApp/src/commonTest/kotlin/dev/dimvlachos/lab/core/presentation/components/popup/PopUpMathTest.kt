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
    fun aTurningLeafSitsBetweenTheTwoSpreadsItSeparates() {
        val out = IntArray(8)
        val n = PopUpMath.drawOrder(floatArrayOf(pi, pi / 2f), out)
        val order = out.take(n)
        val leaf = order.indexOf(1)
        val first = order.indexOf(PopUpMath.Spread)
        val second = order.indexOf(PopUpMath.Spread + 1)
        assertTrue(first >= 0 && second >= 0)
        assertTrue((first < leaf) != (second < leaf))
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
