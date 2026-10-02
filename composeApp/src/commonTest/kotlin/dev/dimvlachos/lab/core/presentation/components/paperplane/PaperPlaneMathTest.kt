package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaperPlaneMathTest {
    // The sheet every plane is folded from.
    private val sheet = DartSheet
    private val plan = planFolds(sheet)
    private val folds = plan.folds.size.toFloat()

    // Seen from far off, nose right, lying where the plan has it.
    private fun still(roll: Float = 0f) = Placement(plan.balance, 0f, roll, 1f, eye = 1e6f)

    private fun points(fold: Float, placement: Placement) =
        plan.facets.flatMap { facet ->
            val space = plan.space(facet, fold, placement)
            facet.polygon.map(space::at)
        }

    @Test
    fun theFacetsCoverTheSheetOnce() {
        val total = plan.facets.sumOf { area(it.polygon).toDouble() }
        assertEquals(sheet.width * sheet.height.toDouble(), total, 1.0)
    }

    @Test
    fun flatItLiesExactlyWhereTheSheetWas() {
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
        // and on the paper.
        val h = sheet.height / 2f
        for (q in points(folds - 1f, still())) {
            assertTrue(
                abs(q.z) <= plan.facets.size * PaperPlaneDimens.Thickness,
                "off the stack: ${q.z}",
            )
            assertTrue(q.y <= h + 0.5f, "below the spine: $q")
            assertTrue(q.x >= -0.5f && q.x <= sheet.width + 0.5f, "beyond the paper: $q")
        }
    }

    @Test
    fun theCornersFoldTwiceIntoASharpNose() {
        // Side on, the dart is a long slim triangle: just behind the nose it is barely deep.
        val h = sheet.height / 2f
        val nearNose = points(folds - 1f, still()).filter { it.x > sheet.width - 0.25f * h }
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
    fun paperTurnedAwayLandsUnderTheStackSoTheFrontEndsInside() {
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
    fun aGrownFacetNeverReachesFarPastItsCorners() {
        // Grown to overlap its neighbours by a seam, a facet's sharp corners are cut off, not
        // drawn out into needles past the nose.
        val reach = 2f * PaperPlaneDimens.Seam + 0.01f
        for (facet in plan.facets) {
            for (p in facet.polygon.grown(PaperPlaneDimens.Seam)) {
                val nearest = facet.polygon.minOf { (it - p).getDistance() }
                assertTrue(nearest <= reach, "a corner drawn out $nearest past the paper")
            }
        }
    }

    @Test
    fun theFacetsAreDrawnInTheOrderTheEyeSeesThem() {
        // Every pose the plane flies in, every way round: at each point of the dart, the facet
        // drawn last there is the one nearest the eye there.
        var agree = 0
        var seen = 0
        for (roll in listOf(-102f, -84f, -62f, -40f, -22f)) {
            for (heading in listOf(0f, 60f, 180f, 250f)) {
                val placement = Placement(Offset(500f, 500f), heading, roll, 3f, 1920f, 285f)
                val spaces = plan.folded().map(Pose(placement)::place)
                val order = plan.drawOrder(spaces, Vec3(500f, 500f, placement.eye))
                val views = spaces.map { it.projection(placement.at, placement.eye) }
                val outlines =
                    plan.facets.mapIndexed { i, facet -> facet.polygon.map(views[i]::project) }
                val all = outlines.flatten()
                var y = all.minOf { it.y }
                while (y < all.maxOf { it.y }) {
                    var x = all.minOf { it.x }
                    while (x < all.maxOf { it.x }) {
                        val p = Offset(x, y)
                        val under = outlines.indices.filter { outlines[it].holds(p) }
                        val onAnEdge = outlines.any { it.nearAnEdge(p) }
                        if (under.isNotEmpty() && !onAnEdge) {
                            seen++
                            val nearest = under.maxBy { spaces[it].at(views[it].unproject(p)).z }
                            if (under.maxBy { order.indexOf(it) } == nearest) agree++
                        }
                        x += 2f
                    }
                    y += 2f
                }
            }
        }
        assertTrue(seen > 1000, "only $seen points seen")
        assertTrue(agree >= 0.99f * seen, "$agree of $seen drawn as seen")
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

    // A throw over a long message, from the button at the foot of a phone's stage.
    private val stage = Rect(0f, 100f, 1080f, 2200f)
    private val throwFrom = Offset(1000f, 2120f)
    private val wide =
        flightPath(throwFrom, 0f, Offset(140f, 1940f), Offset(950f, 1940f), 200f, stage)

    private fun paceOf(path: FlightPath) =
        Pace(
            approach = path.pieces[0].length + path.pieces[1].length,
            sweep = path.pieces[2].length,
            exit = path.pieces[3].length,
            approachMs = PaperPlaneDimens.ApproachMs,
            sweepSpeed = 2.4f,
            launchSpeed = PaperPlaneDimens.ThrowSpeed * 3f,
            exitSpeed = 2.4f * PaperPlaneDimens.ExitBoost,
        )

    @Test
    fun comingInOverALongMessageItNeverLeavesTheLeftOfTheStage() {
        val points = (0..400).map { wide.at(it / 400f) }
        val left = points.minOf { it.x }
        assertTrue(left >= stage.left, "out to $left")
    }

    @Test
    fun onTheScreenItKeepsItsPaceFromOnePieceOfTheThrowToTheNext() {
        val pace = paceOf(wide)
        // How far it goes on the screen in the millisecond after [ms].
        fun speed(ms: Float) =
            (wide.at(pace.distance(ms + 1f) / wide.length) -
                    wide.at(pace.distance(ms) / wide.length))
                .getDistance()
        // Thrown off the button at its throw's speed (as hard as it can be and still come round
        // in time), not crawling off it.
        val thrown = min(PaperPlaneDimens.ThrowSpeed * 3f, 3f * pace.approach / pace.approachMs)
        assertEquals(thrown, speed(0f), 0.15f * thrown)
        // No jolt where one piece meets the next: at the top of the loop, and into the sweep.
        val joins = listOf(wide.pieces[0].length, wide.pieces[0].length + wide.pieces[1].length)
        for (join in joins) {
            var ms = 0f
            while (pace.distance(ms) < join) ms += 1f
            val before = speed(ms - 4f)
            val after = speed(ms + 3f)
            assertTrue(abs(after / before - 1f) < 0.06f, "$before then $after at $join")
        }
    }

    @Test
    fun itLeansIntoItsTurnsNotOutOfThem() {
        val path = flightPath(throwFrom, 0f, Offset(600f, 1940f), Offset(950f, 1940f), 200f, stage)
        var leaned = 0
        for (i in 5..95) {
            val t = i / 100f
            val bank = path.bank(t)
            if (abs(bank) < 5f) continue
            leaned++
            val h = path.heading(t).toRadians()
            // The side the heading swings to: the inside of the turn.
            val inside = if (bank < 0f) Offset(sin(h), -cos(h)) else Offset(-sin(h), cos(h))
            // Which way the wings' top faces, flat on the screen, with the lean and without.
            fun up(roll: Float) =
                Pose(Placement(Offset.Zero, path.heading(t), roll, 1f, 1f))
                    .direction(Vec3(0f, -1f, 0f))
            val leaning = up(PaperPlaneDimens.RestRoll - bank)
            val level = up(PaperPlaneDimens.RestRoll)
            val towards = { v: Vec3 -> v.x * inside.x + v.y * inside.y }
            assertTrue(towards(leaning) > towards(level), "leaning out at $t")
        }
        assertTrue(leaned > 10)
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

// Whether a convex outline holds [p], either way round.
private fun List<Offset>.holds(p: Offset): Boolean {
    var sign = 0f
    for (i in indices) {
        val a = this[i]
        val b = this[(i + 1) % size]
        val cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
        if (cross == 0f) continue
        if (sign == 0f) sign = cross else if (sign * cross < 0f) return false
    }
    return sign != 0f
}

// Within a pixel of one of its edges, where either side may show.
private fun List<Offset>.nearAnEdge(p: Offset): Boolean = indices.any { i ->
    val a = this[i]
    val b = this[(i + 1) % size]
    val e = b - a
    val l2 = e.x * e.x + e.y * e.y
    if (l2 == 0f) return@any (p - a).getDistance() < 1f
    val t = (((p.x - a.x) * e.x + (p.y - a.y) * e.y) / l2).coerceIn(0f, 1f)
    (a + e * t - p).getDistance() < 1f
}

// The sheet's point a projection takes to [p]: its inverse, up to scale.
private fun FloatArray.unproject(p: Offset): Offset {
    val (a, b, c) = Triple(this[0], this[1], this[2])
    val (d, e, f) = Triple(this[3], this[4], this[5])
    val (g, h, i) = Triple(this[6], this[7], this[8])
    val x = (e * i - f * h) * p.x + (c * h - b * i) * p.y + (b * f - c * e)
    val y = (f * g - d * i) * p.x + (a * i - c * g) * p.y + (c * d - a * f)
    val w = (d * h - e * g) * p.x + (b * g - a * h) * p.y + (a * e - b * d)
    return Offset(x / w, y / w)
}
