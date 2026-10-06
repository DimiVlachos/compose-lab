package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.tan

/**
 * Draws a [LampRig]: its paths are kept from frame to frame, so a swinging cord allocates nothing.
 * Everything is drawn in dp, scaled up to the screen's pixels.
 */
internal class LampPainter {
    private val cord = Path()
    private val shade = Path()
    private val addLight = Paint().apply { blendMode = BlendMode.Plus }
    private val circle = Path()
    private var shadeBuilt = false

    /**
     * Draws [content] with the new look clipped to a circle spreading from where the bulb was,
     * [state]'s reveal of the way to the screen's farthest corner.
     */
    fun DrawScope.clipToReveal(state: PullCordState, content: DrawScope.() -> Unit) {
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
        clipPath(circle) { content() }
    }

    /**
     * The light the lamp throws down over the screen, added to what is there: a cone out of the
     * shade, bright at the bulb and fading with distance, its edges feathered by a conic mask, so
     * it fades out sideways rather than stopping at a line.
     */
    fun DrawScope.drawLight(state: PullCordState, colors: AppColors) {
        state.frame
        val glow = state.brightness.value
        if (glow <= 0f) return
        val rig = state.rig
        val density = state.density
        scale(density, density, Offset.Zero) {
            val bounds = Rect(0f, 0f, size.width / density, size.height / density)
            drawIntoCanvas { it.saveLayer(bounds, addLight) }
            rotateRad(rig.tilt, rig.pivot) {
                val bulb = rig.pivot + Offset(0f, RimY)
                val reach = max(bounds.width, bounds.height) * ConeReach
                val half = PullCordDimens.ConeHalfAngle
                // The cone's edges meet up inside the shade, so it leaves the rim as wide as the
                // rim.
                val apex = bulb - Offset(0f, PullCordDimens.ShadeRim.value / 2f / tan(half))
                val area = Rect(apex.x - reach, apex.y, apex.x + reach, apex.y + reach)
                drawRect(
                    Brush.radialGradient(
                        0f to colors.lampLight.copy(alpha = PullCordDimens.ConeGlow * glow),
                        0.35f to
                            colors.lampLight.copy(alpha = PullCordDimens.ConeGlow * 0.4f * glow),
                        1f to Color.Transparent,
                        center = bulb,
                        radius = reach,
                    ),
                    area.topLeft,
                    area.size,
                )
                // Straight down is a quarter of the way round from the right, clockwise. The
                // mask reaches past the light on every side, so no sliver of the light's edge is
                // left uncovered by it.
                val inner = (half - half * Feather) / Turn
                val outer = (half + half * Feather) / Turn
                val mask = area.inflate(MaskMargin)
                drawRect(
                    Brush.sweepGradient(
                        0f to Color.Transparent,
                        Down - outer to Color.Transparent,
                        Down - inner to Color.Black,
                        Down + inner to Color.Black,
                        Down + outer to Color.Transparent,
                        1f to Color.Transparent,
                        center = apex,
                    ),
                    mask.topLeft,
                    mask.size,
                    blendMode = BlendMode.DstIn,
                )
            }
            drawIntoCanvas { it.restore() }
        }
    }

    /** The lamp itself, over everything: the canopy, rod, bulb, cord, shade and bead. */
    fun DrawScope.drawLamp(state: PullCordState, colors: AppColors) {
        state.frame
        val rig = state.rig
        val glow = state.brightness.value
        buildShade()
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
                    val bulb = pivot + Offset(0f, RimY)
                    drawCircle(
                        Brush.radialGradient(
                            0f to colors.bulbLit.copy(alpha = PullCordDimens.HaloGlow * glow),
                            1f to Color.Transparent,
                            center = bulb,
                            radius = PullCordDimens.HaloRadius.value,
                        ),
                        PullCordDimens.HaloRadius.value,
                        bulb,
                        blendMode = BlendMode.Plus,
                    )
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
        drawPath(
            cord,
            colors.lampCord,
            style = Stroke(PullCordDimens.CordWidth.value, cap = StrokeCap.Round),
        )
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
            drawPath(
                shade,
                Brush.horizontalGradient(
                    0f to colors.lampShadeSheen.copy(alpha = 0f),
                    0.28f to colors.lampShadeSheen.copy(alpha = 0.8f),
                    0.55f to colors.lampShadeSheen.copy(alpha = 0f),
                    startX = -rim,
                    endX = rim,
                ),
            )
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
private const val Feather = 0.45f
private const val Turn = 2f * PI.toFloat()
private const val Down = 0.25f
private const val MaskMargin = 4f

// How far the light reaches down the screen, as a share of its longer side: past its far edge.
private const val ConeReach = 1.4f
private const val RodWidth = 2f
private const val RimWidth = 2f
private const val CollarHeight = 6f
private const val BeadGlint = 0.55f
