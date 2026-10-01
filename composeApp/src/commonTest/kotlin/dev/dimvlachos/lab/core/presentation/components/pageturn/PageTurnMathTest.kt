package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.geometry.CornerRadius
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PageTurnMathTest {
    private val leafWidth = 900f
    private val height = 800f
    private val geometry =
        PageGeometry(
            spineX = 1000f,
            originY = 400f,
            perspectivePx = 4000f,
            corner = CornerRadius.Zero,
            page = leafWidth,
            height = height,
        )

    private fun assertNear(expected: Float, actual: Float, tolerance: Float = 0.01f) =
        assertEquals(expected, actual, tolerance)

    // Where wedge [index] draws the paper at [x] across it from its first ruling, at [y].
    private fun TurnFrame.pointAt(index: Int, x: Float, y: Float): Offset =
        matrix(index, geometry, Matrix()).map(Offset(index * stripWidth + x, y))

    private fun frame(
        t: Float,
        bendDirection: Float = 1f,
        restLift: Float = PageTurnDimens.RestLift,
        tilt: Float = 0f,
        grip: Grip? = null,
    ) =
        turnFrame(
            t,
            leafWidth,
            bendDirection,
            restLift = restLift,
            height = height,
            tilt = tilt,
            grip = grip,
        )

    @Test
    fun theLeafLiesFlatOnTheRightPageBeforeTheTurn() {
        val frame = frame(t = 0f, restLift = 0f)
        frame.poses.forEachIndexed { i, pose ->
            assertNear(0f, pose.angle)
            assertNear(i * frame.stripWidth, pose.hingeX, 0.1f)
            assertNear(0f, pose.hingeZ)
        }
    }

    @Test
    fun theLeafLiesFlatOnTheLeftPageAfterTheTurn() {
        val frame = frame(t = 1f, restLift = 0f)
        frame.poses.forEachIndexed { i, pose ->
            assertNear(-PI.toFloat(), pose.angle, 0.001f)
            assertNear(-i * frame.stripWidth, pose.hingeX, 0.1f)
            assertNear(0f, pose.hingeZ, 0.1f)
        }
    }

    @Test
    fun theChainKeepsTheLeafsLengthWhileItBends() {
        for (t in listOf(0.2f, 0.5f, 0.8f)) {
            val frame = frame(t)
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
        val frame = frame(t = 0.5f)
        assertTrue(frame.poses.drop(1).all { it.hingeZ < 0f })
        assertNear(1f, frame.lift)
    }

    @Test
    fun aReversedBendMirrorsTheBow() {
        val ahead = frame(t = 0.3f, bendDirection = 1f, restLift = 0f)
        val behind = frame(t = 0.3f, bendDirection = -1f, restLift = 0f)
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
            for (tilt in listOf(0f, 0.4f)) {
                val frame = frame(t, tilt = tilt)
                val top = frame.pointAt(0, 0f, 0f)
                val eye = frame.pointAt(0, 0f, geometry.originY)
                assertNear(geometry.spineX, top.x, 0.5f)
                assertNear(0f, top.y, 0.5f)
                assertNear(geometry.originY, eye.y, 0.5f)
            }
        }
    }

    @Test
    fun flatStripsDrawAtTheirPlaceOnTheRightPage() {
        val frame = frame(t = 0f, restLift = 0f)
        val corner = frame.pointAt(5, frame.stripWidth, 300f)
        assertNear(geometry.spineX + 6 * frame.stripWidth, corner.x, 0.5f)
        assertNear(300f, corner.y, 0.5f)
    }

    @Test
    fun aStripNearerTheReaderDrawsLarger() {
        // Standing straight up, unbowed, the leaf runs towards the eye: its far end grows away
        // from the eye's centre line.
        val upright = frame(t = 0.5f, bendDirection = 0f, restLift = 0f)
        val far = upright.matrix(0, geometry, Matrix()).map(Offset(400f, 0f))
        assertTrue(far.y < 0f, "top edge ${far.y}")
        assertNear(geometry.spineX, far.x, 0.5f)
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
        // Unbowed and flat, every strip stands at -pi t.
        assertTrue(frame(t = 1.5f / PI.toFloat(), bendDirection = 0f, restLift = 0f).facesReader(0))
        assertFalse(
            frame(t = 1.65f / PI.toFloat(), bendDirection = 0f, restLift = 0f).facesReader(0)
        )
    }

    @Test
    fun theLeafIsUnshadedAtRestAndDarkensAwayFromTheReader() {
        val rest = frame(t = 0f, restLift = 0f)
        val (restStart, restEnd) = stripShadeAlphas(rest, 3)
        assertNear(0f, restStart)
        assertNear(0f, restEnd)
        assertNear(0f, stripGlareAlpha(rest, 3))
        val up = frame(t = 0.5f)
        val (upStart, upEnd) = stripShadeAlphas(up, 8)
        assertTrue(upStart > 0.3f && upEnd > 0.3f, "$upStart $upEnd")
        assertTrue(upStart <= PageTurnDimens.ShadowMax && upEnd <= PageTurnDimens.ShadowMax)
    }

    @Test
    fun aCurledLeafCanReachBackOverTheSpineWhileItsSpineStripStillShowsItsFront() {
        // Pulled back past half way, bowed the other way: the strip at the spine has swung round
        // to show its front, but the rest of the leaf still arches over to the left.
        val curled = frame(t = 0.55f, bendDirection = -1f, restLift = 0f)
        assertTrue(curled.facesReader(0))
        assertTrue(reachesAcrossSpine(curled, rightOfFold = true))
        // Lying on its own side, or turning without curling back: it stays there.
        assertFalse(reachesAcrossSpine(frame(t = 0f), rightOfFold = true))
        assertFalse(reachesAcrossSpine(frame(t = 1f), rightOfFold = false))
        assertFalse(reachesAcrossSpine(frame(t = 0.3f, restLift = 0f), rightOfFold = true))
    }

    @Test
    fun theStitchesShowOnlyWhereTheCentreFoldIsOpen() {
        val centre = 4
        // A fold leaf in the air, by the angle of its strip at the spine.
        fun shown(leftTop: Int, rightTop: Int, vararg spines: Pair<Int, Float>) =
            stitchesShown(centre, leftTop, rightTop, spines.toMap())
        val flatRight = 0f
        val upright = -PI.toFloat() / 2f
        val flatLeft = -PI.toFloat()
        // The centre spread open: the thread shows.
        assertNear(1f, shown(leftTop = 3, rightTop = 4))
        // A spread either side: a leaf covers the fold.
        assertNear(0f, shown(leftTop = 2, rightTop = 3))
        assertNear(0f, shown(leftTop = 4, rightTop = 5))
        // The right leaf of the fold lifted, its spine strip still leaning back: open.
        assertNear(1f, shown(leftTop = 3, rightTop = 5, 4 to -0.6f))
        // Its spine strip past upright, over the fold, however far the leaf as a whole has gone.
        assertNear(0f, shown(leftTop = 3, rightTop = 5, 4 to upright - 0.4f))
        assertNear(0f, shown(leftTop = 3, rightTop = 5, 4 to flatLeft + 0.05f))
        // The left leaf of the fold arriving: hidden until its spine strip passes upright, then
        // open all the way down onto the left, so nothing changes as it lands.
        assertNear(0f, shown(leftTop = 2, rightTop = 4, 3 to flatRight - 0.3f))
        assertNear(1f, shown(leftTop = 2, rightTop = 4, 3 to upright - 0.6f))
        assertNear(1f, shown(leftTop = 2, rightTop = 4, 3 to flatLeft + 0.01f))
        // Turning elsewhere: hidden.
        assertNear(0f, shown(leftTop = 1, rightTop = 3, 2 to upright))
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
    fun aRestingPageRisesOutOfTheGutterCrestsAndFallsOntoItsStack() {
        assertNear(0f, restHeight(0f))
        val heights = (0..20).map { restHeight(it / 20f) }
        val crest = heights.indices.maxBy { heights[it] } / 20f
        assertTrue(crest in 0.3f..0.6f, "crest at $crest")
        assertTrue(heights.last() in 0.1f..heights.max(), "edge ${heights.last()}")
        val frame = frame(t = 0f)
        // Rising, the strips lean towards the reader (negative); past the crest, away.
        assertTrue(frame.poses.first().angle < 0f)
        assertTrue(frame.poses.last().angle > 0f)
        assertTrue(frame.poses.drop(1).all { it.hingeZ < 0f }, "the page stands above the spine")
    }

    @Test
    fun theLeftPageRestsInTheMirrorOfTheRight() {
        val right = frame(t = 0f)
        val left = frame(t = 1f)
        right.poses.zip(left.poses).forEach { (r, l) ->
            assertNear(-PI.toFloat() - r.angle, l.angle, 0.001f)
            assertNear(-r.hingeX, l.hingeX, 0.1f)
            assertNear(r.hingeZ, l.hingeZ, 0.1f)
        }
    }

    @Test
    fun aTurnStartsAndEndsInTheShapeOfTheRestingPages() {
        // A whisker into the turn the leaf has barely moved from the page under it: no snap.
        val rest = frame(t = 0f)
        val started = frame(t = 0.002f)
        rest.poses.zip(started.poses).forEach { (r, s) -> assertNear(r.angle, s.angle, 0.02f) }
        val landed = frame(t = 0.998f)
        frame(t = 1f).poses.zip(landed.poses).forEach { (r, s) ->
            assertNear(r.angle, s.angle, 0.02f)
        }
    }

    @Test
    fun deeperSheetsArchLessSoTheirEdgesReachFurtherOut() {
        val reaches =
            (0..6).map { depth ->
                val frame = frame(t = 0f, restLift = sheetLift(depth.toFloat()))
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

    @Test
    fun withoutATiltTheRulingsStandUpright() {
        val frame = frame(t = 0.3f)
        assertEquals(PageTurnDimens.Strips, frame.wedges)
        for (k in 0..frame.wedges) {
            assertNear(k * frame.stripWidth, frame.rulingX(k, 0f), 0.01f)
            assertNear(k * frame.stripWidth, frame.rulingX(k, height), 0.01f)
        }
    }

    @Test
    fun aTiltedLeafFoldsWithoutTearingOrStretching() {
        val frame = frame(t = 0.35f, tilt = 0.4f, grip = Grip(u = 0.95f, v = 0.05f))
        assertTrue(frame.wedges > PageTurnDimens.Strips, "more wedges reach the far corner")
        // Each ruling is drawn in the same place by the wedges either side of it: no tear.
        for (k in 1 until frame.wedges) {
            for (y in listOf(0f, height / 2f, height)) {
                val x = frame.rulingX(k, y)
                val before = frame.matrix(k - 1, geometry, Matrix()).map(Offset(x, y))
                val after = frame.matrix(k, geometry, Matrix()).map(Offset(x, y))
                assertNear(before.x, after.x, 0.5f)
                assertNear(before.y, after.y, 0.5f)
            }
        }
        // Within a wedge the paper keeps its size: two points stay as far apart as on the page.
        val a = Offset(frame.rulingX(10, 100f) + 5f, 100f)
        val b = Offset(frame.rulingX(10, 700f) + 5f, 700f)
        val (ax, ay, az) = frame.bookPoint(a.x, a.y)
        val (bx, by, bz) = frame.bookPoint(b.x, b.y)
        val apart =
            kotlin.math.sqrt((ax - bx) * (ax - bx) + (ay - by) * (ay - by) + (az - bz) * (az - bz))
        assertNear((a - b).getDistance(), apart, 0.5f)
    }

    @Test
    fun aLeanToTheTopLiftsTheTopCornerFirst() {
        // Held, the hand leads: the bow bends the paper on towards it, most at the far corner.
        val leaning = frame(t = 0.1f, bendDirection = -PageTurnDimens.HoldBow, tilt = 0.4f)
        val top = leaning.bookPoint(leafWidth, 0f).third
        val bottom = leaning.bookPoint(leafWidth, height).third
        assertTrue(top < bottom - 20f, "top corner up at $top, bottom at $bottom")
    }

    @Test
    fun thePaperSeenAtAPointIsTheOneDrawnThere() {
        val frame = frame(t = 0.3f, tilt = 0.3f, grip = Grip(0.9f, 0.1f))
        for (paper in listOf(Offset(300f, 200f), Offset(850f, 60f), Offset(600f, 700f))) {
            val seen = frame.project(paper.x, paper.y, geometry)
            val found = assertNotNull(frame.unproject(seen, geometry), "at $paper")
            assertNear(paper.x, found.x, 1f)
            assertNear(paper.y, found.y, 1f)
        }
        assertEquals(null, frame.unproject(Offset(geometry.spineX + 2_000f, 400f), geometry))
    }

    @Test
    fun aHeldPointFollowsTheFingerAcrossWithoutSnapping() {
        val grab = Offset(0.95f * leafWidth, height / 2f)
        val grip = Grip(0.95f, 0.5f)
        val start =
            frame(t = 0f, bendDirection = PageTurnDimens.BowRest).project(grab.x, grab.y, geometry)
        var last = 0f
        for (i in 1..30) {
            val target = start - Offset(i * 0.06f * leafWidth, 0f)
            val t =
                pinTurn(
                    grab,
                    target.x,
                    last,
                    PageTurnDimens.BowRest,
                    0f,
                    grip,
                    geometry,
                    liftedFrom = 0f,
                )
            assertTrue(t >= last - 0.001f && t - last < 0.15f, "step $i: $last then $t")
            last = t
            val seen =
                frame(t, PageTurnDimens.BowRest, grip = grip).project(grab.x, grab.y, geometry)
            // Under the fingertip: within a few percent of a page.
            assertTrue(
                abs(seen.x - target.x) < 0.06f * leafWidth,
                "step $i off by ${seen.x - target.x}",
            )
        }
        assertTrue(last > 0.75f, "carried over: $last")
    }

    @Test
    fun aSlantedPullLeansTheRulingsSquareToIt() {
        val pull = 0.3f * leafWidth
        assertNear(0f, pullTilt(Offset(-pull, 0f), geometry))
        assertTrue(pullTilt(Offset(-pull, pull), geometry) > 0.3f, "down and in: top first")
        assertTrue(pullTilt(Offset(-pull, -pull), geometry) < -0.3f, "up and in: bottom first")
        // A finger's first wobble leans nothing; a pull away from the spine leans nothing.
        assertTrue(abs(pullTilt(Offset(-2f, 2f), geometry)) < 0.01f)
        assertNear(0f, pullTilt(Offset(pull, pull), geometry))
    }

    @Test
    fun theLeanGrowsInAsTheLeafLeavesItsPageAndKeepsInBounds() {
        val corner = Grip(u = 1f, v = 0f)
        val down = Offset(-0.3f * leafWidth, 0.3f * leafWidth)
        assertNear(0f, leanFor(corner, down, t = 0f, geometry))
        val lean = leanFor(corner, down, t = 0.4f, geometry)
        assertTrue(lean > 0.3f && lean <= PageTurnDimens.TiltLimit, "lean $lean")
    }

    @Test
    fun paperTakenOffItsStackTrailsTheFingerAndThenCatchesUp() {
        val span = 100f
        assertNear(0f, liftLag(0f, span))
        assertNear(0f, liftLag(-20f, span))
        assertNear(0f, liftLag(3f * span, span))
        // Smooth all the way, and the paper never runs backwards.
        val samples = (0..400).map { it * 3f * span / 400f }
        samples.zipWithNext().forEach { (a, b) ->
            assertTrue(abs(liftLag(b, span) - liftLag(a, span)) < 1.5f, "a step at $a")
            assertTrue(b - liftLag(b, span) >= a - liftLag(a, span) - 1e-3f, "back at $a")
        }
    }

    @Test
    fun aHoldNearACornerLeansTheRulingsThatWay() {
        assertTrue(gripTilt(Grip(u = 1f, v = 0f)) > 0.3f)
        assertTrue(gripTilt(Grip(u = 1f, v = 1f)) < -0.3f)
        assertNear(0f, gripTilt(Grip(u = 1f, v = 0.5f)))
        assertTrue(abs(gripTilt(Grip(u = 0.1f, v = 0f))) < 0.05f, "no lean by the spine")
    }

    @Test
    fun theBowGathersAroundTheHandAndHasLittleLeverByTheSpine() {
        val held = 0.7f
        assertNear(0f, gripShare(0f, held))
        assertNear(1f, gripShare(1f, held))
        val slopeAtHand = gripShare(held + 0.05f, held) - gripShare(held - 0.05f, held)
        val slopeAway = gripShare(0.15f, held) - gripShare(0.05f, held)
        assertTrue(slopeAtHand > slopeAway * 1.5f, "$slopeAtHand vs $slopeAway")
        assertTrue(gripLever(0.1f) < gripLever(1f))
    }

    @Test
    fun aHandLeadsTheBowAndTheAirHoldsAFreeLeafBack() {
        assertNear(PageTurnDimens.BowRest, bowTarget(1f, velocity = 0f, held = false))
        assertNear(-PageTurnDimens.BowRest, bowTarget(-1f, velocity = 0f, held = false))
        assertNear(-PageTurnDimens.HoldBow, bowTarget(1f, velocity = 0f, held = true))
        // The faster it goes, the more the air pushes the paper back, up to a point.
        assertTrue(bowTarget(1f, velocity = 2f, held = false) > bowTarget(1f, 0.5f, held = false))
        assertTrue(bowTarget(1f, velocity = 2f, held = true) > bowTarget(1f, 0.5f, held = true))
        assertNear(
            PageTurnDimens.BowRest + PageTurnDimens.FlexMax,
            bowTarget(1f, velocity = 100f, held = false),
        )
    }

    @Test
    fun theSpringSwaysPastItsTargetAndSettles() {
        var spring = Spring(0f)
        var furthest = 0f
        repeat(120) {
            spring =
                spring.step(1f, 1f / 60f, PageTurnDimens.FlexFrequency, PageTurnDimens.FlexDamping)
            furthest = maxOf(furthest, spring.value)
        }
        assertTrue(furthest > 1.05f, "a sway past it: $furthest")
        assertNear(1f, spring.value, 0.01f)
    }
}
