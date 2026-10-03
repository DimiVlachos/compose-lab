package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The stage paper planes fly over: lay it over everything they cross. The planes are folded from
 * [paper]; the letters they drop are drawn from the text they carry, as it is laid out where it
 * lands.
 */
@Composable
fun PaperPlane(state: PaperPlaneState, paper: Color, modifier: Modifier = Modifier) {
    val inside = remember(paper) { lerp(paper, Color.Black, PaperPlaneDimens.InsideShade) }
    Spacer(
        modifier
            .onGloballyPositioned { state.stage = Rect(it.positionInRoot(), it.size.toSize()) }
            .drawBehind {
                val flights = state.flights
                val origin = state.stage.topLeft
                // The letters fall under the planes that let them go.
                for (flight in flights) drawDrops(flight, origin)
                for (flight in flights) {
                    if (!flight.flying) continue
                    val placed = flight.placement().shiftedBy(-origin)
                    with(flight.painter) { drawDart(flight.plan, placed, paper, inside) }
                }
            }
    )
}

// Each letter let go so far: out from under the plane where it was let go, carried on a little
// way, slowed by the air, falling faster as it goes, tumbling out of its turn as it grows back to
// its size, and down into its place with a small dip as it settles.
private fun DrawScope.drawDrops(flight: Flight, origin: Offset) {
    val clock = flight.clock.value
    val at = flight.textOrigin() - origin
    val land = PaperPlaneDimens.LandAt
    for (drop in flight.drops) {
        if (clock < drop.letGo) continue
        val p = ((clock - drop.letGo) / PaperPlaneDimens.FallMs).coerceAtMost(1f)
        val from = drop.from - origin
        val home = at + drop.box.center
        val drift = 1f - (1f - p) * (1f - p)
        val fall = if (p < land) (p / land).let { it * it } else 1f
        val dip =
            if (p < land) 0f
            else PaperPlaneDimens.Settle * density * sin(PI.toFloat() * (p - land) / (1f - land))
        val now = Offset(lerp(from.x, home.x, drift), lerp(from.y, home.y, fall) + dip)
        val grown = ease(p / land)
        val scale = lerp(PaperPlaneDimens.LetterEndScale, 1f, grown)
        val turn = drop.spin * (1f - grown)
        val alpha = ease(p / 0.15f)
        withTransform({
            rotate(turn, now)
            scale(scale, scale, now)
            translate(now.x - home.x, now.y - home.y)
        }) {
            translate(at.x, at.y) { with(flight.letters) { drawLetter(drop.box, alpha) } }
        }
    }
}

/**
 * The place [key]'s message lands in, in a list: shut while its letters go into the button and its
 * plane flies round, and opening as the plane comes down to it. Its content is laid out at its full
 * height at the foot of the place the whole while, so the message lies where it will stay as the
 * rest of the list moves up to make room, and the plane flies to where it will be.
 */
fun Modifier.planeLanding(state: PaperPlaneState, key: Any): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = (placeable.height * state.opening(key)).roundToInt()
        layout(placeable.width, height) { placeable.place(0, height - placeable.height) }
    }

/**
 * A send button's paper plane: the folded dart itself, seen from right above with its nose to the
 * right, in [color], lit as it is in the air. Thrown, it is this dart that lifts off: the plane
 * leaves the button as exactly what the button showed.
 */
@Composable
fun PaperPlaneIcon(color: Color, modifier: Modifier = Modifier) {
    val painter = remember { DartPainter() }
    val inside = remember(color) { lerp(color, Color.Black, PaperPlaneDimens.InsideShade) }
    Spacer(
        modifier.drawBehind {
            val placement = iconPlacement(Rect(Offset.Zero, size))
            with(painter) { drawDart(dartPlan, placement, color, inside) }
        }
    )
}

/** The dart every icon shows and every plane is: folded the once, the first time it is needed. */
internal val dartPlan: FoldPlan by lazy { planFolds(DartSheet) }

/**
 * Where the dart drawn by a [PaperPlaneIcon] laid out at [icon] lies: as long as the icon's share
 * of its width, seen close up three-quarters from behind, its nose up and away to the right, and
 * its outline in the middle of the icon. A plane thrown from the icon starts from just here.
 */
internal fun iconPlacement(icon: Rect): Placement {
    val length = icon.width * PaperPlaneDimens.IconLength
    val scale = length / dartPlan.length
    fun at(where: Offset) =
        Placement(
            where,
            PaperPlaneDimens.IconHeading,
            PaperPlaneDimens.TakeoffRoll,
            scale,
            PaperPlaneDimens.IconEye * length,
            pitch = PaperPlaneDimens.TakeoffPitch,
        )
    val outline = dartOutline(at(Offset.Zero))
    val middle =
        Offset(
            (outline.minOf { it.x } + outline.maxOf { it.x }) / 2f,
            (outline.minOf { it.y } + outline.maxOf { it.y }) / 2f,
        )
    return at(icon.center - middle)
}

/** The corners of every facet of the dart at [placement], on the stage as the eye sees them. */
internal fun dartOutline(placement: Placement): List<Offset> {
    val pose = Pose(placement)
    val eye = placement.eye
    val centre = placement.at
    return dartPlan.facets.flatMapIndexed { i, facet ->
        val space = pose.place(dartRest[i])
        facet.polygon.map {
            val q = space.at(it)
            // A point nearer the eye lies further out from the middle of its view.
            centre + (Offset(q.x, q.y) - centre) * (eye / (eye - q.z))
        }
    }
}

private val dartRest: List<FacetSpace> by lazy { dartPlan.folded() }

/**
 * How far the dart drawn by a [PaperPlaneIcon] laid out at [icon] (in the root) can grow about the
 * icon's middle and still lie within [field], [inset] inside its edge, up to [most]: never less
 * than its own size.
 */
fun iconRoom(icon: Rect, field: RoundRect, inset: Float, most: Float): Float {
    val within =
        RoundRect(
            field.left + inset,
            field.top + inset,
            field.right - inset,
            field.bottom - inset,
            field.topLeftCornerRadius.shrunk(inset),
            field.topRightCornerRadius.shrunk(inset),
            field.bottomRightCornerRadius.shrunk(inset),
            field.bottomLeftCornerRadius.shrunk(inset),
        )
    val corners = dartOutline(iconPlacement(icon))
    fun fits(scale: Float) = corners.all {
        within.contains(icon.center + (it - icon.center) * scale)
    }
    if (fits(most)) return most
    if (!fits(1f)) return 1f
    // The field is convex and holds the icon's middle: once a size fits, every smaller one does,
    // each corner nearer the middle along a line inside it, so the largest is found by halving.
    var lo = 1f
    var hi = most
    repeat(24) {
        val mid = (lo + hi) / 2f
        if (fits(mid)) lo = mid else hi = mid
    }
    return lo
}

private fun CornerRadius.shrunk(by: Float) =
    CornerRadius((x - by).coerceAtLeast(0f), (y - by).coerceAtLeast(0f))
