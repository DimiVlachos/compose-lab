package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan
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
        // Leaning somewhere on the loop.
        assertTrue((0..100).maxOf { abs(path.bank(it / 100f, 2.4f, 0.03f)) } > 10f)
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
            val bank = path.bank(t, 2.4f, 0.03f)
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
    fun itBanksAsAGliderDoesLevelOnTheStraightAndSteeperTheFasterItTurns() {
        val path = wide
        val pieces = path.pieces
        // Halfway over the message, flying straight: wings level.
        val over = (pieces[0].length + pieces[1].length + pieces[2].length / 2f) / path.length
        assertEquals(0f, path.bank(over, 3f, 0.03f), 0.5f)
        // Round the loop, tan bank = speed² × curvature / gravity: twice as fast, four times it.
        val loop = 0.8f * pieces[0].length / path.length
        val slow = path.bank(loop, 0.1f, 0.001f).toRadians()
        val fast = path.bank(loop, 0.2f, 0.001f).toRadians()
        assertTrue(abs(tan(slow)) > 1e-3f, "no lean at all in the loop")
        assertEquals(4f * tan(slow), tan(fast), 0.02f * abs(tan(fast)))
        // Never past a glider's steepest.
        assertEquals(PaperPlaneDimens.MaxBank, abs(path.bank(loop, 100f, 0.01f)), 0.01f)
    }

    @Test
    fun pitchedUpItsNoseRisesTowardsTheEyeStillAlongItsHeading() {
        fun nose(pitch: Float) =
            Pose(Placement(Offset.Zero, 30f, PaperPlaneDimens.RestRoll, 1f, 1f, pitch = pitch))
                .direction(Vec3(1f, 0f, 0f))
        val up = nose(20f)
        assertEquals(sin(20f.toRadians()), up.z, 1e-4f)
        assertEquals(30f, atan2(up.y, up.x).toDegrees(), 0.01f)
        assertTrue(nose(-20f).z < 0f)
        assertEquals(0f, nose(0f).z, 1e-5f)
    }

    @Test
    fun paperStaysWhiteWhicheverWayItTurnsAndShadesOnlyBetweenItsFolds() {
        val light =
            Vec3(PaperPlaneDimens.LightX, PaperPlaneDimens.LightY, PaperPlaneDimens.LightZ).unit()
        // Out in the open, paper flat on the screen is its own colour; turned any way at all, it
        // is never much darker: the room lights it from every side.
        assertEquals(0f, brightness(Vec3(0f, 0f, 1f), light), 1e-5f)
        assertEquals(0f, brightness(Vec3(0f, 0f, -1f), light), 1e-5f)
        assertTrue(brightness(light, light) > 0f)
        for (k in 0 until 64) {
            val a = k * 0.7f
            val n = Vec3(cos(a) * sin(a * 1.3f), sin(a) * sin(a * 1.3f), cos(a * 1.3f)).unit()
            assertTrue(brightness(n, light) > -0.25f, "open paper grey at $n")
        }
        // Where its own folds stand over it, it falls into shade.
        assertTrue(brightness(Vec3(0f, 0f, 1f), light, open = 0.5f) < -0.4f)
        // The dart's open faces see most of the room; the faces inside the V between its folds,
        // and the keel under its wings, see clearly less of it.
        val open = plan.lighting.flatMap { listOf(it.front, it.back) }
        assertTrue(open.max() > 0.9f, "nothing in the open: ${open.max()}")
        assertTrue(open.min() < 0.7f, "nothing in the folds' shade: ${open.min()}")
        assertTrue(open.all { it in 0f..1f })
    }

    @Test
    fun theIconIsTheDartThatLeavesIt() {
        val icon = Rect(100f, 100f, 126f, 126f)
        val placement = iconPlacement(icon)
        val outline = dartOutline(placement)
        val left = outline.minOf { it.x }
        val right = outline.maxOf { it.x }
        val top = outline.minOf { it.y }
        val bottom = outline.maxOf { it.y }
        // In the middle of the icon, and within it.
        assertEquals(icon.center.x, (left + right) / 2f, 1e-3f)
        assertEquals(icon.center.y, (top + bottom) / 2f, 1e-3f)
        assertTrue(right - left <= 26f && bottom - top <= 26f, "${right - left} × ${bottom - top}")
        // A plane thrown from it starts from just where the dart lies, as long as it is.
        val takeoff = planeTakeoff(icon)
        assertEquals(placement.at, takeoff.center)
        assertEquals(placement.scale * plan.length, takeoff.length, 1e-3f)
    }

    @Test
    fun theIconGrowsOnlyAsFarAsTheFieldHasRoomForIt() {
        // A one-line field, a pill 48 tall, the button's 26-wide icon in the middle of its end.
        val field = RoundRect(Rect(0f, 0f, 400f, 48f), CornerRadius(24f))
        val icon = Rect(Offset(376f, 24f), 13f)
        val room = iconRoom(icon, field, inset = 3f, most = 3f)
        assertTrue(room > 1f, "no room at all")
        // At that size its corners touch the inset edge: no further, but nearly.
        val corners = dartOutline(iconPlacement(icon))
        fun fits(scale: Float) = corners.all { p ->
            val q = icon.center + (p - icon.center) * scale
            RoundRect(Rect(3f, 3f, 397f, 45f), CornerRadius(21f)).contains(q)
        }
        assertTrue(fits(room))
        assertTrue(!fits(room + 0.02f))
        // Held to the most it may grow, with room to spare; never shrunk, with none.
        assertEquals(1.1f, iconRoom(icon, field, inset = 3f, most = 1.1f), 1e-4f)
        val tight = RoundRect(Rect(370f, 18f, 382f, 30f), CornerRadius(6f))
        assertEquals(1f, iconRoom(icon, tight, inset = 0f, most = 3f), 1e-4f)
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
