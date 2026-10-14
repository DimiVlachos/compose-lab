package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.presentation.components.magnet.SoftShadow
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A sent sheet as a slip of paper in a thread: trimmed to its [lines], with a little paper round
 * them, turned [tilt] degrees about its top left as a slip dropped on a table lies. Its letters lie
 * just as they were struck on the sheet. [unit] is the composer's unit, so the type is its size.
 */
@Composable
internal fun PaperSlip(
    lines: List<String>,
    tilt: Float,
    atlas: GlyphAtlas,
    unit: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LabTheme.colors
    val shadow = remember { SoftShadow() }
    val size = slipSize(lines)
    Spacer(
        modifier
            .size(unit * size.width, unit * size.height)
            .graphicsLayer {
                rotationZ = tilt
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .drawBehind {
                val u = unit.toPx()
                drawPaper(shadow, colors, this.size, u, lift = 0f)
                drawTyped(atlas, lines, 0f, 0f, u)
            }
    )
}

/** How big a slip of [lines] is, in units. */
internal fun slipSize(lines: List<String>): Size {
    val widest = lines.maxOfOrNull { it.trimEnd().length }?.coerceAtLeast(1) ?: 1
    return Size(
        widest * TypewriterDimens.CharWidth + TypewriterDimens.Margin * 2f,
        lines.size * TypewriterDimens.LineHeight +
            TypewriterDimens.TopMargin +
            TypewriterDimens.SlipBottom,
    )
}

// A slip's paper, [size] px, with its shadows on whatever it lies on, wider and softer the higher
// it is [lift]ed (0 lying down to 1 at the top of its flight), and a fine edge.
private fun DrawScope.drawPaper(
    shadow: SoftShadow,
    colors: AppColors,
    size: Size,
    u: Float,
    lift: Float,
) {
    with(shadow) {
        drawSoftShadow(0f, u, size.width, size.height + u, 1.5f * u, 0.14f + 0.06f * lift)
        val drop = lerp(4f, 16f, lift) * u
        drawSoftShadow(
            u * 2f,
            drop,
            size.width - u * 2f,
            size.height + drop - 2f * u,
            lerp(5f, 14f, lift) * u,
            0.18f + 0.08f * lift,
        )
    }
    drawRect(colors.typePaper, size = size)
    drawRect(colors.typePaperEdge, size = size, style = Stroke(0.6f * u))
}

/**
 * The sheets on their way from the typewriter into a thread, by message: each flies from where it
 * came free of the roller to its message's place, once that is laid out, trimming down to its slip
 * as it goes. A message's place shows nothing until its sheet has landed there.
 */
@Stable
internal class SlipFlights {
    private class Flight(val sheet: SentSheet, val tilt: Float) {
        val progress = Animatable(0f)
        var to: Rect? by mutableStateOf(null)
    }

    private val flights = mutableStateMapOf<Long, Flight>()

    /** Whether message [id]'s sheet is still in the air. */
    fun flying(id: Long): Boolean = id in flights

    /** Message [id]'s place has been laid out at [bounds], in the root. */
    internal fun laidOut(id: Long, bounds: Rect) {
        flights[id]?.to = bounds
    }

    /**
     * Flies [sheet] into message [id]'s place, where it lies turned [tilt] degrees, once that is
     * laid out. Returns once it has landed; if the place is never laid out, it simply appears
     * there.
     */
    suspend fun fly(id: Long, sheet: SentSheet, tilt: Float) {
        val flight = Flight(sheet, tilt)
        flights[id] = flight
        try {
            withTimeoutOrNull(TypewriterDimens.SlotWaitMs) {
                snapshotFlow { flight.to }.filterNotNull().first()
            } ?: return
            flight.progress.animateTo(
                1f,
                tween((TypewriterDimens.FlightSeconds * 1000).toInt(), easing = LinearEasing),
            )
        } finally {
            flights -= id
        }
    }

    /** Draws every sheet in the air, from a stage whose top left is at [origin] in the root. */
    internal fun DrawScope.drawFlights(
        atlas: GlyphAtlas,
        shadow: SoftShadow,
        colors: AppColors,
        u: Float,
        origin: Offset,
    ) {
        for (flight in flights.values) {
            val to = flight.to ?: continue
            val p = flight.progress.value
            val from = flight.sheet.bounds
            val e = ease(p)
            val es = easeOut(min(1f, p / TrimShare))
            val arc = sin(PI.toFloat() * p)
            val x = lerp(from.left, to.left, e) - origin.x
            val y = lerp(from.top, to.top, e) - TypewriterDimens.FlightArc * u * arc - origin.y
            val size = Size(lerp(from.width, to.width, es), lerp(from.height, to.height, es))
            val side = if (to.left > from.left) 1f else -1f
            val turn = lerp(0f, flight.tilt, e) + TypewriterDimens.FlightSwing * arc * side
            val grow = 1f + TypewriterDimens.FlightGrow * arc
            translate(x, y) {
                rotate(turn, Offset.Zero) {
                    scale(grow, grow, Offset.Zero) {
                        drawPaper(shadow, colors, size, u, lift = arc)
                        clipRect(0f, 0f, size.width, size.height) {
                            drawTyped(atlas, flight.sheet.lines, 0f, 0f, u)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        // The sheet has trimmed down to its slip by this share of the way.
        const val TrimShare = 0.55f
    }
}

/**
 * The place in a thread where message [id]'s slip lands: it tells [flights] where it is, and shows
 * nothing while its sheet is still in the air.
 */
internal fun Modifier.slipLanding(flights: SlipFlights, id: Long): Modifier = onGloballyPositioned {
    flights.laidOut(id, Rect(it.positionInRoot(), it.size.toSize()))
}
    .drawWithContent { if (!flights.flying(id)) drawContent() }

/**
 * Where [flights] are drawn, over a thread and its composer: fill the stage with it, over both. It
 * takes no touches.
 */
@Composable
internal fun SlipFlightStage(
    flights: SlipFlights,
    atlas: GlyphAtlas,
    unit: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LabTheme.colors
    val shadow = remember { SoftShadow() }
    val origin = remember { arrayOf(Offset.Zero) }
    Spacer(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { origin[0] = it.positionInRoot() }
            .drawBehind {
                with(flights) { drawFlights(atlas, shadow, colors, unit.toPx(), origin[0]) }
            }
    )
}
