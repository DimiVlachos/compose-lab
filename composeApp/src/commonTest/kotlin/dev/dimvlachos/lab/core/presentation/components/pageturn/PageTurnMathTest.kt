package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageTurnMathTest {
    private val leafWidth = 900f
    private val spineX = 1000f
    private val originY = 400f
    private val perspective = 4000f

    private fun assertNear(expected: Float, actual: Float, tolerance: Float = 0.01f) =
        assertEquals(expected, actual, tolerance)

    private fun pointAt(pose: StripPose, x: Float, y: Float): Offset =
        stripMatrix(pose, spineX, originY, perspective, Matrix()).map(Offset(x, y))

    @Test
    fun theLeafLiesFlatOnTheRightPageBeforeTheTurn() {
        val frame = turnFrame(t = 0f, leafWidth = leafWidth, restLift = 0f)
        frame.poses.forEachIndexed { i, pose ->
            assertNear(0f, pose.angle)
            assertNear(i * frame.stripWidth, pose.hingeX, 0.1f)
            assertNear(0f, pose.hingeZ)
        }
    }

    @Test
    fun theLeafLiesFlatOnTheLeftPageAfterTheTurn() {
        val frame = turnFrame(t = 1f, leafWidth = leafWidth, restLift = 0f)
        frame.poses.forEachIndexed { i, pose ->
            assertNear(-PI.toFloat(), pose.angle, 0.001f)
            assertNear(-i * frame.stripWidth, pose.hingeX, 0.1f)
            assertNear(0f, pose.hingeZ, 0.1f)
        }
    }

    @Test
    fun theChainKeepsTheLeafsLengthWhileItBends() {
        for (t in listOf(0.2f, 0.5f, 0.8f)) {
            val frame = turnFrame(t, leafWidth)
            val last = frame.poses.last()
            val edgeX = last.hingeX + frame.stripWidth * cos(last.angle)
            val edgeZ = last.hingeZ + frame.stripWidth * sin(last.angle)
            val chord = hypot(edgeX, edgeZ)
            // A bent leaf's chord is shorter than the leaf, but never by more than the bow allows.
            assertTrue(chord < leafWidth, "t=$t chord $chord")
            assertTrue(chord > leafWidth * 0.9f, "t=$t chord $chord")
        }
    }

    @Test
    fun theLeafRisesTowardsTheReaderMidTurn() {
        val frame = turnFrame(t = 0.5f, leafWidth = leafWidth)
        assertTrue(frame.poses.drop(1).all { it.hingeZ < 0f })
        assertNear(1f, frame.lift)
    }

    @Test
    fun aReversedBendMirrorsTheBow() {
        val ahead = turnFrame(t = 0.3f, leafWidth = leafWidth, bendDirection = 1f, restLift = 0f)
        val behind = turnFrame(t = 0.3f, leafWidth = leafWidth, bendDirection = -1f, restLift = 0f)
        // Bent ahead, the outer strip lags the spine strip (a larger angle is less far round).
        assertTrue(ahead.poses.last().angle > ahead.poses.first().angle)
        assertTrue(behind.poses.last().angle < behind.poses.first().angle)
        val theta = -(PI * 0.3).toFloat()
        ahead.poses.zip(behind.poses).forEach { (a, b) ->
            assertNear(theta - (a.angle - theta), b.angle, 0.001f)
        }
    }

    @Test
    fun theSpineStripHingesOnTheSpineAtAnyAngle() {
        for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val pose = turnFrame(t, leafWidth).poses.first()
            val top = pointAt(pose, 0f, 0f)
            val eye = pointAt(pose, 0f, originY)
            assertNear(spineX, top.x, 0.5f)
            assertNear(0f, top.y, 0.5f)
            assertNear(originY, eye.y, 0.5f)
        }
    }

    @Test
    fun flatStripsDrawAtTheirPlaceOnTheRightPage() {
        val frame = turnFrame(t = 0f, leafWidth = leafWidth, restLift = 0f)
        val pose = frame.poses[5]
        val corner = pointAt(pose, frame.stripWidth, 300f)
        assertNear(spineX + 6 * frame.stripWidth, corner.x, 0.5f)
        assertNear(300f, corner.y, 0.5f)
    }

    @Test
    fun aStripNearerTheReaderDrawsLarger() {
        val lifted = StripPose(angle = -PI.toFloat() / 2f, hingeX = 0f, hingeZ = 0f)
        // Standing straight up, the strip runs towards the eye: its far end grows away from the
        // eye's centre line.
        val far = pointAt(lifted, 400f, 0f)
        assertTrue(far.y < 0f, "top edge ${far.y}")
        assertNear(spineX, far.x, 0.5f)
    }

    @Test
    fun theBackOfTheLeafTakesTheLeftPageFromItsFarEnd() {
        assertNear(0f, stripSliceStart(front = true, index = 0, strips = 18))
        assertNear(17f / 18f, stripSliceStart(front = false, index = 0, strips = 18))
        assertNear(0f, stripSliceStart(front = false, index = 17, strips = 18))
        assertTrue(stripIsOuterEdge(17, strips = 18))
        assertFalse(stripIsOuterEdge(16, strips = 18))
    }

    @Test
    fun stripsFaceTheReaderUntilTheyPassUpright() {
        assertTrue(stripFacesReader(StripPose(-1.5f, 0f, 0f)))
        assertFalse(stripFacesReader(StripPose(-1.65f, 0f, 0f)))
    }

    @Test
    fun theLeafIsUnshadedAtRestAndDarkensAwayFromTheReader() {
        val rest = turnFrame(t = 0f, leafWidth = leafWidth, restLift = 0f)
        val (restStart, restEnd) = stripShadeAlphas(rest, 3)
        assertNear(0f, restStart)
        assertNear(0f, restEnd)
        assertNear(0f, stripGlareAlpha(rest, 3))
        val up = turnFrame(t = 0.5f, leafWidth = leafWidth)
        val (upStart, upEnd) = stripShadeAlphas(up, 8)
        assertTrue(upStart > 0.3f && upEnd > 0.3f, "$upStart $upEnd")
        assertTrue(upStart <= PageTurnDimens.ShadowMax && upEnd <= PageTurnDimens.ShadowMax)
    }

    @Test
    fun theStaplesShowOnlyWhereTheCentreFoldIsOpen() {
        val centre = 4
        // The centre spread open: leaf 3 on top of the left, leaf 4 on top of the right.
        assertTrue(staplesShow(centre, leftTop = 3, rightTop = 4, flying = emptySet()))
        // A spread either side: a leaf covers the fold.
        assertFalse(staplesShow(centre, leftTop = 2, rightTop = 3, flying = emptySet()))
        assertFalse(staplesShow(centre, leftTop = 4, rightTop = 5, flying = emptySet()))
        // Turning onto the centre spread, or away from it: the fold shows under the leaf.
        assertTrue(staplesShow(centre, leftTop = 2, rightTop = 4, flying = setOf(3)))
        assertTrue(staplesShow(centre, leftTop = 3, rightTop = 5, flying = setOf(4)))
        // Turning elsewhere: hidden.
        assertFalse(staplesShow(centre, leftTop = 1, rightTop = 3, flying = setOf(2)))
    }

    @Test
    fun aFlickDecidesByItsDirectionWhereverThePageIs() {
        // Flicked back hard past half way: it falls back, it does not finish against the hand.
        assertFalse(shouldCommit(progress = 0.43f, velocity = -6.7f))
        assertFalse(shouldCommit(progress = 0.9f, velocity = -2f))
        // Flicked on before half way: it finishes.
        assertTrue(shouldCommit(progress = 0.1f, velocity = 2f))
        // A slow release goes by where the page is.
        assertTrue(shouldCommit(progress = 0.6f, velocity = -0.5f))
        assertFalse(shouldCommit(progress = 0.3f, velocity = 0.5f))
    }

    @Test
    fun aPageFinishesPastTheThresholdOrOnAFlick() {
        assertFalse(shouldCommit(progress = 0.3f, velocity = 0.2f))
        assertTrue(shouldCommit(progress = 0.5f, velocity = 0f))
        assertTrue(shouldCommit(progress = 0.1f, velocity = 2f))
        assertFalse(shouldCommit(progress = 0.1f, velocity = -2f))
    }

    @Test
    fun dragsAreMeasuredAgainstPartOfTheBook() {
        assertNear(1f, dragToProgress(620f, bookWidthPx = 1000f))
        assertNear(-0.5f, dragToProgress(-310f, bookWidthPx = 1000f))
    }

    @Test
    fun theBendFollowsTheDragAndFlipsGradually() {
        assertEquals(1f, bendTowards(forward = true, progressIncreasing = true))
        assertEquals(-1f, bendTowards(forward = true, progressIncreasing = false))
        assertEquals(-1f, bendTowards(forward = false, progressIncreasing = true))
        // Reversing a forward turn: a small step moves the bend part way, enough steps flip it.
        var bend = 1f
        bend = bendAfterDrag(bend, forward = true, delta = -0.02f)
        assertTrue(bend in 0f..0.99f, "bend $bend")
        repeat(10) { bend = bendAfterDrag(bend, forward = true, delta = -0.02f) }
        assertEquals(-1f, bend)
        assertEquals(-1f, bendAfterDrag(-1f, forward = true, delta = 0f))
        assertTrue(abs(bendAfterDrag(-1f, forward = true, delta = 0.075f)) < 0.001f)
    }

    @Test
    fun aRestingPageRisesOutOfTheGutterCrestsAndFallsOntoItsStack() {
        assertNear(0f, restHeight(0f))
        val heights = (0..20).map { restHeight(it / 20f) }
        val crest = heights.indices.maxBy { heights[it] } / 20f
        assertTrue(crest in 0.3f..0.6f, "crest at $crest")
        assertTrue(heights.last() in 0.1f..heights.max(), "edge ${heights.last()}")
        val frame = turnFrame(t = 0f, leafWidth = leafWidth)
        // Rising, the strips lean towards the reader (negative); past the crest, away.
        assertTrue(frame.poses.first().angle < 0f)
        assertTrue(frame.poses.last().angle > 0f)
        assertTrue(frame.poses.drop(1).all { it.hingeZ < 0f }, "the page stands above the spine")
    }

    @Test
    fun theLeftPageRestsInTheMirrorOfTheRight() {
        val right = turnFrame(t = 0f, leafWidth = leafWidth)
        val left = turnFrame(t = 1f, leafWidth = leafWidth)
        right.poses.zip(left.poses).forEach { (r, l) ->
            assertNear(-PI.toFloat() - r.angle, l.angle, 0.001f)
            assertNear(-r.hingeX, l.hingeX, 0.1f)
            assertNear(r.hingeZ, l.hingeZ, 0.1f)
        }
    }

    @Test
    fun aTurnStartsAndEndsInTheShapeOfTheRestingPages() {
        // A whisker into the turn the leaf has barely moved from the page under it: no snap.
        val rest = turnFrame(t = 0f, leafWidth = leafWidth)
        val started = turnFrame(t = 0.002f, leafWidth = leafWidth)
        rest.poses.zip(started.poses).forEach { (r, s) -> assertNear(r.angle, s.angle, 0.02f) }
        val landed = turnFrame(t = 0.998f, leafWidth = leafWidth)
        turnFrame(t = 1f, leafWidth = leafWidth).poses.zip(landed.poses).forEach { (r, s) ->
            assertNear(r.angle, s.angle, 0.02f)
        }
    }

    @Test
    fun deeperSheetsArchLessSoTheirEdgesReachFurtherOut() {
        val reaches =
            (0..6).map { depth ->
                val frame =
                    turnFrame(t = 0f, leafWidth = leafWidth, restLift = sheetLift(depth.toFloat()))
                val last = frame.poses.last()
                last.hingeX + frame.stripWidth * cos(last.angle)
            }
        reaches.zipWithNext().forEach { (upper, lower) -> assertTrue(lower > upper, "$reaches") }
        assertNear(PageTurnDimens.RestLift, sheetLift(0f))
        assertTrue(sheetLift(100f) > 0f, "the deepest sheet still bows")
    }

    @Test
    fun theStacksSettleSmoothlyAsALeafLeavesOneAndLandsOnTheOther() {
        assertNear(0f, stackRise(0f))
        assertNear(1f, stackRise(PageTurnDimens.SettleSpan))
        assertNear(1f, stackRise(1f))
        assertNear(0f, stackLand(1f - PageTurnDimens.SettleSpan))
        assertNear(1f, stackLand(1f))
        // Smooth: no step anywhere along the turn.
        val samples = (0..200).map { it / 200f }
        samples.zipWithNext().forEach { (a, b) ->
            assertTrue(abs(stackRise(b) - stackRise(a)) < 0.02f)
            assertTrue(abs(stackLand(b) - stackLand(a)) < 0.02f)
        }
    }

    @Test
    fun theGutterDarkensARestingPageAndLeavesALiftedOne() {
        assertTrue(gutterRestShade(0f, lift = 0f) > gutterRestShade(0.1f, lift = 0f))
        assertNear(0f, gutterRestShade(0.5f, lift = 0f))
        assertTrue(gutterRestShade(1f, lift = 0f) > 0f, "the edge curling onto its stack dims")
        assertNear(0f, gutterRestShade(0f, lift = 1f))
    }
}
