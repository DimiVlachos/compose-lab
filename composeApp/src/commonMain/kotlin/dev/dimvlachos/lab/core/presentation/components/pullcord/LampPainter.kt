package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.tan

/**
 * Draws a [LampRig]: its paths, strokes and brushes are kept from frame to frame, so a swinging
 * cord allocates next to nothing. Everything is drawn in dp, scaled up to the screen's pixels.
 */
internal class LampPainter {
    private val cord = Path()
    private val shade = Path()
    private val circle = Path()
    private var shadeBuilt = false
    private val cordStroke = Stroke(PullCordDimens.CordWidth.value, cap = StrokeCap.Round)

    // The brushes, made about the origin and moved to the bulb as it swings: made again only when
    // the colours or the light's reach change, never for a frame. The light is drawn at full
    // glow; its brightness is its layer's alpha.
    private var brushColors: AppColors? = null
    private var brushReach = -1f
    private var cone: Brush = SolidColor(Color.Transparent)
    private var coneMask: Brush = SolidColor(Color.Transparent)
    private var halo: Brush = SolidColor(Color.Transparent)
    private var sheen: Brush = SolidColor(Color.Transparent)
    private var underRim: Brush = SolidColor(Color.Transparent)

    private fun brushes(colors: AppColors, reach: Float) {
        if (colors == brushColors && reach == brushReach) return
        brushColors = colors
        brushReach = reach
        // Light thins with distance as a curve, not in straight steps: many stops along it, so
        // the eye finds no ring where one step meets the next.
        cone =
            Brush.radialGradient(
                *falloff(colors.lampLight, PullCordDimens.ConeGlow) { t ->
                    (1f - t) * (1f - t) / (1f + ConeFalloff * t)
                },
                center = Offset.Zero,
                radius = reach,
            )
        // Straight down is a quarter of the way round from the right, clockwise. The cone's sides
        // ease out over a wide soft band, rather than a straight ramp with a corner at each end.
        val half = PullCordDimens.ConeHalfAngle
        val inner = (half - half * Feather) / Turn
        val outer = (half + half * Feather) / Turn
        coneMask =
            Brush.sweepGradient(
                *sides(Down - outer, Down - inner, Down + inner, Down + outer),
                center = Offset.Zero,
            )
        // The bulb's own glow: soft all the way out, with no edge for it to end at, so it melts
        // into the cone rather than sitting on it as a disc.
        halo =
            Brush.radialGradient(
                *falloff(colors.bulbLit, PullCordDimens.HaloGlow) { t ->
                    exp(-HaloSpread * t * t) * (1f - t)
                },
                center = Offset.Zero,
                radius = PullCordDimens.HaloRadius.value,
            )
        val rim = PullCordDimens.ShadeRim.value / 2f
        // Clear above the rim, opaque a little below it: what keeps the light under the shade.
        underRim =
            Brush.verticalGradient(
                *Array(Smoothness + 1) { i ->
                    val t = i / Smoothness.toFloat()
                    t to Color.Black.copy(alpha = t * t * (3f - 2f * t))
                },
                startY = 0f,
                endY = PullCordDimens.RimFade.value,
            )
        sheen =
            Brush.horizontalGradient(
                0f to colors.lampShadeSheen.copy(alpha = 0f),
                0.28f to colors.lampShadeSheen.copy(alpha = 0.8f),
                0.55f to colors.lampShadeSheen.copy(alpha = 0f),
                startX = -rim,
                endX = rim,
            )
    }

    /**
     * Draws [content] clipped to a circle spreading from where the bulb was, [state]'s reveal of
     * the way to the screen's farthest corner: [inside] it, or outside it.
     */
    fun DrawScope.clipToReveal(
        state: PullCordState,
        inside: Boolean,
        content: DrawScope.() -> Unit,
    ) {
        val from = state.revealFrom
        val farthest =
            max(
                max(hypot(from.x, from.y), hypot(size.width - from.x, from.y)),
                max(
                    hypot(from.x, size.height - from.y),
                    hypot(size.width - from.x, size.height - from.y),
                ),
            )
        val radius = farthest * state.reveal.value
        circle.rewind()
        circle.addOval(Rect(from, radius))
        clipPath(circle, if (inside) ClipOp.Intersect else ClipOp.Difference) { content() }
    }

    /**
     * The light the lamp throws down over the screen, at full glow, into a layer of its own that is
     * added to what is under it: a cone out of the shade, bright at the bulb and fading with
     * distance, its edges feathered by a conic mask, so it fades out sideways rather than stopping
     * at a line.
     */
    fun DrawScope.drawLight(state: PullCordState, colors: AppColors) {
        // Dark, it draws nothing, and needn't be drawn again as the cord swings.
        if (!state.lit && !state.revealing) return
        state.frame
        val rig = state.rig
        val density = state.density
        scale(density, density, Offset.Zero) {
            val width = size.width / density
            val height = size.height / density
            rotateRad(rig.tilt, rig.pivot) {
                val bulb = rig.pivot + Offset(0f, RimY)
                val reach = max(width, height) * ConeReach
                brushes(colors, reach)
                // The cone's edges meet just inside the shade, a little above the bulb: its light
                // comes out of the rim's middle and opens out from there, rather than leaving the
                // rim already as wide as the rim and needing cutting off flat beside it.
                val rise =
                    PullCordDimens.ShadeRim.value / 2f / tan(PullCordDimens.ConeHalfAngle) *
                        PullCordDimens.ApexInside
                translate(bulb.x, bulb.y) {
                    drawRect(cone, Offset(-reach, -rise), Size(2f * reach, reach))
                }
                // The mask reaches past the light on every side, so no sliver of the light's
                // edge is left uncovered by it.
                translate(bulb.x, bulb.y - rise) {
                    drawRect(
                        coneMask,
                        Offset(-reach - MaskMargin, -MaskMargin),
                        Size(2f * (reach + MaskMargin), reach + 2f * MaskMargin),
                        blendMode = BlendMode.DstIn,
                    )
                }
                // And none of it above the rim: the cone's point is up inside the shade, and its
                // sides would otherwise show past the shade's curve as a wedge of light rising to
                // the rod. Out of a real shade, the light starts at the rim and fades in under it.
                translate(bulb.x, bulb.y) {
                    drawRect(
                        underRim,
                        Offset(-reach - MaskMargin, -rise - MaskMargin),
                        Size(2f * (reach + MaskMargin), reach + rise + 2f * MaskMargin),
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
        }
    }

    /** The lamp itself, over everything: the canopy, rod, bulb, cord, shade and bead. */
    fun DrawScope.drawLamp(state: PullCordState, colors: AppColors) {
        state.frame
        val rig = state.rig
        val glow = state.brightness.value
        buildShade()
        brushes(colors, if (brushReach < 0f) 1f else brushReach)
        scale(state.density, state.density, Offset.Zero) {
            val pivot = rig.pivot
            drawRoundRect(
                colors.lampBrass,
                topLeft = pivot - Offset(PullCordDimens.CanopyWidth.value / 2f, 0f),
                size = Size(PullCordDimens.CanopyWidth.value, PullCordDimens.CanopyHeight.value),
                cornerRadius = CornerRadius(PullCordDimens.CanopyHeight.value / 2f),
            )
            rotateRad(rig.tilt, pivot) {
                drawLine(
                    colors.lampBrass,
                    pivot,
                    pivot + Offset(0f, PullCordDimens.Rod.value),
                    strokeWidth = RodWidth,
                )
                drawCircle(
                    lerp(colors.bulbOff, colors.bulbLit, glow),
                    PullCordDimens.BulbRadius.value,
                    pivot + Offset(0f, RimY),
                )
            }
            drawCord(rig, colors)
            rotateRad(rig.tilt, pivot) {
                // A glow about the bulb as well as the light it throws, under the shade, so the
                // shade stays dark against it.
                if (glow > 0f) {
                    // The bulb's glow below the rim only: above it, the shade hides the bulb, and
                    // its outside stays dark but for the rim's own edge catching the light.
                    // Narrowed to the rim's width where it meets it, so its cut-off top never shows
                    // past the rim's ends as a flat band.
                    translate(pivot.x, pivot.y + RimY) {
                        val radius = PullCordDimens.HaloRadius.value
                        val narrow = PullCordDimens.ShadeRim.value / 2f / radius * HaloNarrowing
                        clipRect(-radius, -PullCordDimens.RimCatch.value, radius, radius) {
                            scale(narrow, 1f, pivot = Offset.Zero) {
                                drawCircle(
                                    halo,
                                    radius,
                                    Offset.Zero,
                                    alpha = glow,
                                    blendMode = BlendMode.Plus,
                                )
                            }
                        }
                    }
                }
                drawShade(pivot + Offset(0f, PullCordDimens.Rod.value), colors)
            }
            drawBead(rig.bead, colors)
        }
    }

    // A smooth line through the cord's points: through the midpoints, curving at each point.
    private fun DrawScope.drawCord(rig: LampRig, colors: AppColors) {
        cord.rewind()
        val first = rig.point(0)
        cord.moveTo(first.x, first.y)
        for (i in 1 until rig.points - 1) {
            val at = rig.point(i)
            val next = rig.point(i + 1)
            cord.quadraticTo(at.x, at.y, (at.x + next.x) / 2f, (at.y + next.y) / 2f)
        }
        val last = rig.point(rig.points - 1)
        cord.lineTo(last.x, last.y)
        drawPath(cord, colors.lampCord, style = cordStroke)
    }

    // A brass bead with a glint on its upper left.
    private fun DrawScope.drawBead(at: Offset, colors: AppColors) {
        val radius = PullCordDimens.BeadRadius.value
        drawCircle(colors.lampBrass, radius, at)
        drawCircle(
            Color.White.copy(alpha = BeadGlint),
            radius * 0.3f,
            at - Offset(radius * 0.35f, radius * 0.35f),
        )
    }

    // The dome of the shade, its top at [top], with a sheen down its left and a brass rim.
    private fun DrawScope.drawShade(top: Offset, colors: AppColors) {
        val rim = PullCordDimens.ShadeRim.value / 2f
        val height = PullCordDimens.ShadeHeight.value
        val box = Rect(top.x - rim, top.y, top.x + rim, top.y + height)
        translate(top.x, top.y) {
            drawPath(shade, colors.lampShade)
            drawPath(shade, sheen)
        }
        drawLine(
            colors.lampBrass,
            Offset(box.left, box.bottom),
            Offset(box.right, box.bottom),
            strokeWidth = RimWidth,
            cap = StrokeCap.Round,
        )
        drawRoundRect(
            colors.lampBrass,
            topLeft = top - Offset(PullCordDimens.ShadeTop.value / 2f + 1f, CollarHeight / 2f),
            size = Size(PullCordDimens.ShadeTop.value + 2f, CollarHeight),
            cornerRadius = CornerRadius(CollarHeight / 2f),
        )
    }

    // The shade's outline about its top middle, made once: a narrow top flaring into a dome.
    private fun buildShade() {
        if (shadeBuilt) return
        shadeBuilt = true
        val top = PullCordDimens.ShadeTop.value / 2f
        val rim = PullCordDimens.ShadeRim.value / 2f
        val h = PullCordDimens.ShadeHeight.value
        shade.moveTo(-top, 0f)
        shade.cubicTo(-top - 6f, h * 0.45f, -rim, h * 0.5f, -rim, h)
        shade.lineTo(rim, h)
        shade.cubicTo(rim, h * 0.5f, top + 6f, h * 0.45f, top, 0f)
        shade.close()
    }
}

// Where the bulb sits, under the pivot as the shade hangs: at the rim, half out of the shade.
private val RimY = PullCordDimens.Rod.value + PullCordDimens.ShadeHeight.value

// The feathered share either side of the cone's edge, and where straight down is in a turn.
private const val Feather = 0.8f
private const val Turn = 2f * PI.toFloat()
private const val Down = 0.25f
private const val MaskMargin = 4f

// How many stops a soft gradient takes; how fast the cone's light thins with distance; how fast
// the bulb's glow falls away from it.
private const val Smoothness = 16
private const val ConeFalloff = 5f
private const val HaloSpread = 4f

// [color] at [peak] times f(t) of its alpha, at evenly spaced stops t from 0 to 1.
private inline fun falloff(
    color: Color,
    peak: Float,
    f: (Float) -> Float,
): Array<Pair<Float, Color>> =
    Array(Smoothness + 1) { i ->
        val t = i / Smoothness.toFloat()
        t to color.copy(alpha = peak * f(t))
    }

// A sweep mask: clear outside [from]..[to], opaque from [full] to [fade], easing in and out
// between, all in turns.
private fun sides(from: Float, full: Float, fade: Float, to: Float): Array<Pair<Float, Color>> {
    val ease = Smoothness / 2
    val stops = ArrayList<Pair<Float, Color>>()
    stops += 0f to Color.Transparent
    for (i in 0..ease) {
        val t = i / ease.toFloat()
        stops += (from + (full - from) * t) to Color.Black.copy(alpha = t * t * (3f - 2f * t))
    }
    for (i in 0..ease) {
        val t = i / ease.toFloat()
        stops += (fade + (to - fade) * t) to Color.Black.copy(alpha = 1f - t * t * (3f - 2f * t))
    }
    stops += 1f to Color.Transparent
    return stops.toTypedArray()
}

// The bulb's glow, where it meets the rim, as a share of the rim's width.
private const val HaloNarrowing = 0.9f

// How far the light reaches down the screen, as a share of its longer side: past its far edge.
private const val ConeReach = 1.4f
private const val RodWidth = 2f
private const val RimWidth = 2f
private const val CollarHeight = 6f
private const val BeadGlint = 0.55f
