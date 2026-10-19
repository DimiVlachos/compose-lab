package dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PageTurnMathTest {

    private val width = 400f
    private val height = 520f
    private val geometry = PageGeometry(300f, height * 0.46f, 2250f, width, height)

    @Test
    fun theLeafLiesFlatOnTheRightPageBeforeTheTurnAndOnTheLeftAfterIt() {
        val start = turnFrame(0f, width, height = height)
        val end = turnFrame(1f, width, height = height)
        for (x in floatArrayOf(0f, width / 3f, width)) {
            val (ax, _, az) = start.bookPoint(x, height / 2f)
            assertEquals(x, ax, TOLERANCE)
            assertEquals(0f, az, TOLERANCE)
            val (bx, _, bz) = end.bookPoint(x, height / 2f)
            assertEquals(-x, bx, TOLERANCE)
            assertEquals(0f, bz, TOLERANCE)
        }
    }

    @Test
    fun aFlatStripDrawsExactlyWhereItsPageLies() {
        val frame = turnFrame(0f, width, height = height)
        val seen = frame.project(width / 2f, height / 4f, geometry)
        assertEquals(geometry.spineX + width / 2f, seen.x, TOLERANCE)
        assertEquals(height / 4f, seen.y, TOLERANCE)
    }

    @Test
    fun theChainKeepsTheLeafSLengthWhileItBends() {
        val frame = turnFrame(0.4f, width, height = height)
        var length = 0f
        var previous = frame.bookPoint(0f, height / 2f)
        for (i in 1..80) {
            val point = frame.bookPoint(width * i / 80f, height / 2f)
            val dx = point.first - previous.first
            val dz = point.third - previous.third
            length += kotlin.math.sqrt(dx * dx + dz * dz)
            previous = point
        }
        assertEquals(width, length, width * 0.01f)
    }

    @Test
    fun aRestingPageArchesOnlyAtItsGutterAndItsOuterEdge() {
        for (t in floatArrayOf(0f, 1f)) {
            val frame = turnFrame(t, width, height = height, restLift = 1f)
            val side = if (t == 0f) 1f else -1f
            for (u in floatArrayOf(0.15f, 0.5f, 0.85f)) {
                val (x, _, z) = frame.bookPoint(width * u + frame.textureShift, height / 2f)
                assertEquals(side * width * u, x, TOLERANCE)
                assertEquals(0f, z, TOLERANCE)
            }
            assertEquals(0f, frame.bookPoint(0f, height / 2f).first, TOLERANCE)
            assertTrue(frame.bookPoint(0f, height / 2f).third > 1f)
            assertTrue(frame.bookPoint(width, height / 2f).third > 1f)
        }
    }

    @Test
    fun aRestingPageKeepsItsLength() {
        val frame = turnFrame(0f, width, height = height, restLift = 1f)
        var length = 0f
        var previous = frame.bookPoint(0f, height / 2f)
        for (i in 1..360) {
            val point = frame.bookPoint(frame.width * i / 360f, height / 2f)
            val dx = point.first - previous.first
            val dz = point.third - previous.third
            length += kotlin.math.sqrt(dx * dx + dz * dz)
            previous = point
        }
        assertEquals(frame.width, length, width * 0.002f)
        assertEquals(width, frame.bookPoint(frame.width, height / 2f).first, 2f)
    }

    @Test
    fun aHeldPointLiesWhereTheFrameDrawsIt() {
        val out = FloatArray(3)
        val grab = Offset(width * 0.9f, height * 0.4f)
        heldPoint(grab, 0f, width, 1f, height, 0f, null, out, restLift = 1f)
        val (x, _, z) =
            turnFrame(0f, width, height = height, restLift = 1f).bookPoint(grab.x, grab.y)
        assertEquals(x, out[0], TOLERANCE)
        assertEquals(z, out[2], TOLERANCE)
    }

    @Test
    fun stripsFaceTheReaderUntilTheyPassUpright() {
        assertTrue(turnFrame(0.1f, width, height = height).facesReader(0))
        assertFalse(turnFrame(0.9f, width, height = height).facesReader(0))
    }

    @Test
    fun thePaperSeenAtAPointIsThePaperDrawnThere() {
        val frame = turnFrame(0.3f, width, height = height, tilt = 0.3f, grip = Grip(0.9f, 0.1f))
        val paper = Offset(width * 0.7f, height * 0.3f)
        val seen = frame.project(paper.x, paper.y, geometry)
        val back = assertNotNullOffset(frame.unproject(seen, geometry))
        assertEquals(paper.x, back.x, 0.5f)
        assertEquals(paper.y, back.y, 0.5f)
    }

    @Test
    fun shadingIsInvisibleAtRest() {
        val frame = turnFrame(0f, width, height = height)
        for (i in 0 until frame.wedges) {
            val (a, b) = stripShadeAlphas(frame, i)
            assertEquals(0f, a, TOLERANCE)
            assertEquals(0f, b, TOLERANCE)
            assertEquals(0f, stripGlareAlpha(frame, i), TOLERANCE)
        }
    }

    @Test
    fun aFlickDecidesByItsDirectionAndASlowReleaseByHowFarItWent() {
        assertTrue(shouldCommit(0.1f, 2f))
        assertFalse(shouldCommit(0.9f, -2f))
        assertTrue(shouldCommit(0.5f, 0f))
        assertFalse(shouldCommit(0.3f, 0f))
    }

    @Test
    fun aReleaseDecidesByWhereItsFlingWouldCarryThePage() {
        assertTrue(shouldCommit(0.1f, 0.3f, flung = 0.6f))
        assertFalse(shouldCommit(0.5f, -0.3f, flung = 0.2f))
    }

    @Test
    fun paperTakenOffItsPageTrailsTheFingerAndThenCatchesUp() {
        val span = 50f
        assertEquals(0f, liftLag(0f, span), TOLERANCE)
        assertTrue(liftLag(span / 2f, span) < span / 2f)
        assertEquals(0f, liftLag(span * 4f, span), TOLERANCE)
    }

    @Test
    fun aHoldNearACornerLeansTheRulingsThatWay() {
        assertTrue(gripTilt(Grip(0.95f, 0.02f)) > 0f)
        assertTrue(gripTilt(Grip(0.95f, 0.98f)) < 0f)
        assertEquals(0f, gripTilt(Grip(0.95f, 0.5f)), TOLERANCE)
    }

    @Test
    fun theSpringSwaysPastItsTargetAndSettles() {
        var spring = Spring(0f)
        var overshot = false
        repeat(200) {
            spring = spring.step(1f, 1f / 60f, 2.4f, 0.4f)
            if (spring.value > 1f) overshot = true
        }
        assertTrue(overshot)
        assertTrue(abs(spring.value - 1f) < 0.01f)
    }

    private fun assertNotNullOffset(offset: Offset?): Offset {
        assertNotNull(offset)
        return offset ?: Offset.Zero
    }

    private companion object {
        const val TOLERANCE = 0.01f
    }
}
