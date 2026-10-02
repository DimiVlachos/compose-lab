package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaperPlaneMathTest {
    // A one-line bubble, as wide as most: about six times as long as it is tall.
    private val bubble = Size(600f, 100f)
    private val plan = planFolds(bubble)
    private val folds = plan.folds.size.toFloat()

    // Seen from far off, nose right, lying where the plan has it.
    private fun still(roll: Float = 0f) = Placement(plan.balance, 0f, roll, 1f, eye = 1e6f)

    private fun points(fold: Float, placement: Placement) =
        plan.facets.flatMap { facet ->
            val space = plan.space(facet, fold, placement)
            facet.polygon.map(space::at)
        }

    @Test
    fun aLongBubbleIsTuckedInUntilItIsShapedLikeWritingPaper() {
        assertEquals(2, tucksFor(Size(264f, 46f)))
        assertEquals(1, tucksFor(Size(264f, 68f)))
        // Already about the shape: folded as it is.
        assertEquals(0, tucksFor(Size(80f, 46f)))
    }

    @Test
    fun theFacetsCoverTheSheetOnce() {
        val total = plan.facets.sumOf { area(it.polygon).toDouble() }
        assertEquals(bubble.width * bubble.height.toDouble(), total, 1.0)
    }

    @Test
    fun flatItLiesExactlyWhereTheBubbleWas() {
        val at = Offset(40f, 900f)
        val placement = Placement(at + plan.balance, 0f, 0f, 1f, eye = 2000f)
        for (facet in plan.facets) {
            val space = plan.space(facet, 0f, placement)
            for (p in facet.polygon) {
                val q = space.at(p)
                assertTrue(abs(q.x - (at.x + p.x)) < 0.01f && abs(q.y - (at.y + p.y)) < 0.01f)
                assertEquals(0f, q.z, 0.01f)
            }
        }
    }

    @Test
    fun foldedBeforeItsWingsOpenItLiesFlatAsADartsSideOn() {
        // Every fold but the wings done: a flat stack no thicker than its sheets, above the spine
        // and
        // between the tucked tail and the nose.
        val h = bubble.height / 2f
        val tail = bubble.width - bubble.width / (1 shl tucksFor(bubble))
        for (q in points(folds - 1f, still())) {
            assertTrue(
                abs(q.z) <= plan.facets.size * PaperPlaneDimens.Thickness,
                "off the stack: ${q.z}",
            )
            assertTrue(q.y <= h + 0.5f, "below the spine: $q")
            assertTrue(q.x >= tail - 0.5f && q.x <= bubble.width + 0.5f, "beyond the paper: $q")
        }
    }

    @Test
    fun theCornersFoldTwiceIntoASharpNose() {
        // Side on, the dart is a long slim triangle: just behind the nose it is barely deep.
        val h = bubble.height / 2f
        val nearNose = points(folds - 1f, still()).filter { it.x > bubble.width - 0.25f * h }
        val depth = nearNose.maxOf { it.y } - nearNose.minOf { it.y }
        assertTrue(depth < 0.25f * h, "a blunt nose: $depth deep")
    }

    @Test
    fun theFinishedDartReadsAsAPaperPlaneNotANeedle() {
        // From above, as it flies: about one and a half to three times as long as its span.
        val top = points(folds, still(PaperPlaneDimens.RestRoll))
        val length = top.maxOf { it.x } - top.minOf { it.x }
        val span = top.maxOf { it.y } - top.minOf { it.y }
        assertTrue(length / span in 1.2f..3.2f, "length $length, span $span")
    }

    @Test
    fun paperTurnedAwayLandsUnderTheStackSoThePrintEndsOutside() {
        // The first corner fold turns the corners away from the eye, under all the paper they
        // were turned behind.
        val k =
            plan.folds.indexOfFirst { fold ->
                fold.creases.size == 2 && fold.creases[0].angle == 180f
            }
        val moved = plan.facets.filter { it.moves[k] != null }
        val stayed = plan.facets.filter { it.moves[k] == null }
        assertTrue(moved.isNotEmpty() && stayed.isNotEmpty())
        assertTrue(moved.maxOf { it.layers[k + 1] } < stayed.minOf { it.layers[k + 1] })
    }

    @Test
    fun thePlaneLeavesAlongTheGlyphsNoseSweepsLevelOverTheMessageAndLeavesOnTheRight() {
        val stage = Rect(0f, 100f, 1080f, 2200f)
        val from = Offset(1000f, 2120f)
        val sweepFrom = Offset(600f, 1940f)
        val sweepTo = Offset(950f, 1940f)
        val path = flightPath(from, PaperPlaneDimens.IconHeading, sweepFrom, sweepTo, 200f, stage)
        assertTrue((path.at(0f) - from).getDistance() < 0.01f)
        assertEquals(PaperPlaneDimens.IconHeading, path.heading(0f), 0.01f)
        // Level, left to right, all the way over the message.
        val pieces = path.pieces
        assertEquals(sweepFrom, pieces[2].p0)
        assertEquals(sweepTo, pieces[2].p3)
        for (i in 0..10) {
            val d = pieces[2].tangent(i / 10f)
            assertEquals(0f, d.y, 0.01f)
            assertTrue(d.x > 0f)
        }
        // Smooth into the sweep and out of it.
        assertEquals(0f, pieces[1].tangent(1f).y, 0.01f)
        assertEquals(0f, pieces[3].tangent(0f).y, 0.01f)
        // Out past the right of the stage at the end.
        assertTrue(path.at(1f).x >= stage.right + 199f)
        // Level wings as it leaves the button; leaning somewhere on the loop.
        assertEquals(0f, path.bank(0f), 0.01f)
        assertTrue((0..100).maxOf { abs(path.bank(it / 100f)) } > 10f)
        // It climbs the conversation, and stays on the stage till it leaves on the right.
        val points = (0..100).map { path.at(it / 100f) }
        assertTrue(points.minOf { it.y } < stage.top + 0.6f * stage.height, "too low a loop")
        assertTrue(points.all { it.y > stage.top })
    }

    @Test
    fun withTheKeyboardUpTheLoopStillTurnsBelowTheTop() {
        val stage = Rect(0f, 100f, 1080f, 2200f)
        val path =
            flightPath(
                Offset(1000f, 1200f),
                PaperPlaneDimens.IconHeading,
                Offset(600f, 1050f),
                Offset(900f, 1050f),
                200f,
                stage,
            )
        assertTrue((0..100).all { path.at(it / 100f).y > stage.top })
    }

    @Test
    fun theThrowSlowsToAnEvenPaceOverTheMessageAndSpeedsUpOut() {
        val pace =
            Pace(
                approach = 3000f,
                sweep = 600f,
                exit = 400f,
                approachMs = 1000f,
                sweepSpeed = 2f,
                launchSpeed = 6f,
                exitSpeed = 4f,
            )
        assertEquals(0f, pace.distance(0f))
        assertEquals(3000f, pace.distance(1000f), 0.5f)
        // Never goes back.
        val samples = (0..400).map { pace.distance(it * pace.totalMs / 400f) }
        assertTrue(samples.zipWithNext().all { (a, b) -> b >= a - 1e-3f })
        // Thrown fast, it comes in at the sweep's speed: no jolt as it starts letting go.
        val justBefore = (pace.distance(1000f) - pace.distance(990f)) / 10f
        assertEquals(2f, justBefore, 0.1f)
        assertEquals(2f, (pace.distance(1200f) - pace.distance(1100f)) / 100f, 0.001f)
        assertEquals(1300f, pace.overAt(1f), 0.01f)
        assertEquals(1150f, pace.overAt(0.5f), 0.01f)
        assertEquals(4000f, pace.distance(pace.totalMs), 0.5f)
        // Faster as it leaves.
        val leaving = (pace.distance(pace.totalMs) - pace.distance(pace.totalMs - 10f)) / 10f
        assertTrue(leaving > 3.5f)
    }
}
