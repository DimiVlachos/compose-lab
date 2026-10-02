package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.sin

/**
 * The stage paper planes fly over: lay it over everything they cross. The planes are folded from
 * [paper]; the letters they drop are drawn from the text they carry, as it is laid out where it
 * lands.
 */
@Composable
fun PaperPlane(state: PaperPlaneState, paper: Color, modifier: Modifier = Modifier) {
    val painters = remember { HashMap<Flight, DartPainter>() }
    val inside = remember(paper) { lerp(paper, Color.Black, PaperPlaneDimens.InsideShade) }
    Spacer(
        modifier
            .onGloballyPositioned { state.stage = Rect(it.positionInRoot(), it.size.toSize()) }
            .drawBehind {
                state.density = density
                val flights = state.flights
                painters.keys.retainAll(flights.toSet())
                val origin = state.stage.topLeft
                // The letters fall under the planes that let them go.
                for (flight in flights) drawDrops(flight, origin)
                for (flight in flights) {
                    if (!flight.flying) continue
                    val painter = painters.getOrPut(flight) { DartPainter() }
                    val placed = flight.placement().shiftedBy(-origin)
                    val folded = flight.plan.folds.size.toFloat()
                    with(painter) {
                        drawShadow(flight.plan, folded, placed)
                        drawPaper(flight.plan, folded, placed, paper, inside)
                    }
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
        val from = flight.placement(drop.letGo).at - origin
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
            clipRect(
                at.x + drop.box.left,
                at.y + drop.box.top,
                at.x + drop.box.right,
                at.y + drop.box.bottom,
            ) {
                drawText(flight.text, topLeft = at, alpha = alpha)
            }
        }
    }
}

/**
 * A send button's paper plane: the send glyph, a dart seen from above with its nose to the right,
 * in [color]. Thrown, it lifts off as the folded dart it outlines.
 */
@Composable
fun PaperPlaneIcon(color: Color, modifier: Modifier = Modifier) {
    val glyph = remember { Path() }
    Spacer(
        modifier.drawBehind {
            val k = size.width / GlyphUnits
            glyph.reset()
            Glyph.forEachIndexed { i, (x, y) ->
                if (i == 0) glyph.moveTo(x * k, y * k) else glyph.lineTo(x * k, y * k)
            }
            glyph.close()
            drawPath(glyph, color)
        }
    )
}

// The send glyph on a 24-unit square: tail top, nose, tail bottom, and the notch between.
private const val GlyphUnits = 24f
private val Glyph = listOf(2.01f to 21f, 23f to 12f, 2.01f to 3f, 2f to 10f, 17f to 12f, 2f to 14f)
