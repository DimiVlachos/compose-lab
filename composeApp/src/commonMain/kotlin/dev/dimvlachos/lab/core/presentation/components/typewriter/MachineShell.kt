package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.magnet.SoftShadow
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The parts of the typewriter that never move, drawn once into a layer of their own: the enamel
 * shell, low and rounded, seen a little from above, its deck widening towards the front; the well
 * the keys stand in, stepped up a tier a row towards the back; the opening onto the type basket
 * with its fan of resting bars; the bell's brass dome; and the front lip with the badge. The enamel
 * has a fine orange-peel grain and a soft studio light across it, and a soft shadow on the table.
 */
internal class MachineShell {
    private val shadow = SoftShadow()
    private val shellPath = Path()
    private val wellPath = Path()
    private val openingPath = Path()

    // The enamel's orange peel: a small tile of fine light and dark specks, repeated over it.
    private val peel: ImageBitmap by lazy { orangePeel() }

    /**
     * Draws the shell for a stage [size] wide: the machine's own top left at [origin] (its units
     * [u] px), its keys from [keysTop] to [keysBottom] in [rows] rows, the lip under them down to
     * the foot of the stage, lettered with [badge] and [model].
     */
    fun DrawScope.drawShell(
        colors: AppColors,
        u: Float,
        origin: Offset,
        keysTop: Float,
        keysBottom: Float,
        keysWidth: Float,
        rows: Int,
        badge: TextLayoutResult?,
        model: TextLayoutResult?,
    ) {
        // The shell stands in the middle of the stage; the basket under the printing point.
        val cx = origin.x + TypewriterDimens.StageUnits / 2f * u
        val side = TypewriterDimens.ShellSide.toPx()
        val front =
            minOf(
                size.width - side * 2f,
                maxOf(keysWidth + side * 6f, TypewriterDimens.DeckWidth * u),
            )
        val back = minOf(front, TypewriterDimens.DeckWidth * u)
        val top = origin.y + TypewriterDimens.BodyTop * u
        val bottom = size.height
        val shoulder = TypewriterDimens.ShoulderCorner * u
        shellPath.reset()
        shellPath.moveTo(cx - back / 2f + shoulder, top)
        shellPath.lineTo(cx + back / 2f - shoulder, top)
        shellPath.cubicTo(
            cx + back / 2f,
            top,
            cx + front / 2f,
            top + shoulder * 0.35f,
            cx + front / 2f,
            top + shoulder * 1.4f,
        )
        shellPath.lineTo(cx + front / 2f, bottom)
        shellPath.lineTo(cx - front / 2f, bottom)
        shellPath.lineTo(cx - front / 2f, top + shoulder * 1.4f)
        shellPath.cubicTo(
            cx - front / 2f,
            top + shoulder * 0.35f,
            cx - back / 2f,
            top,
            cx - back / 2f + shoulder,
            top,
        )
        shellPath.close()
        // The machine's soft shadow on the table, and the darker one right under its edges.
        with(shadow) {
            drawSoftShadow(
                cx - front / 2f,
                top + 6f * u,
                cx + front / 2f,
                bottom + 20f * u,
                14f * u,
                TableShadow,
            )
            drawSoftShadow(
                cx - front / 2f + u,
                top + 3f * u,
                cx + front / 2f - u,
                bottom,
                4f * u,
                ContactShadow,
            )
        }
        // The deck: lit at its back, where it curves away under the platen, deep at the front.
        drawPath(
            shellPath,
            Brush.verticalGradient(
                0f to colors.typeEnamelLight,
                0.08f to colors.typeEnamel,
                1f to colors.typeEnamelDeep,
                startY = top,
                endY = keysTop,
            ),
        )
        clipPath(shellPath) {
            // The sides curve down and away: darker towards each edge.
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = EdgeFall),
                    0.12f to Color.Transparent,
                    0.88f to Color.Transparent,
                    1f to Color.Black.copy(alpha = EdgeFall),
                    startX = cx - front / 2f,
                    endX = cx + front / 2f,
                ),
                Offset(cx - front / 2f, top),
                Size(front, bottom - top),
            )
            // A soft studio light, high on the left, and its long reflection across the deck.
            drawOval(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = StudioLight), Color.Transparent),
                    center = Offset(cx - front * 0.22f, top + (keysTop - top) * 0.3f),
                    radius = front * 0.42f,
                ),
                Offset(cx - front * 0.64f, top - (keysTop - top) * 0.3f),
                Size(front * 0.84f, (keysTop - top) * 1.2f),
            )
            // The orange peel of sprayed enamel.
            drawRect(
                ShaderBrush(ImageShader(peel, TileMode.Repeated, TileMode.Repeated)),
                Offset(cx - front / 2f, top),
                Size(front, bottom - top),
                alpha = PeelAlpha,
            )
            // Light along the back edge, where the shell turns over towards the platen.
            drawPath(shellPath, Color.White.copy(alpha = RimLight), style = Stroke(2f * u))
        }
        drawOpening(colors, u, origin)
        drawBell(colors, u, origin)
        drawWell(colors, cx, front, keysTop, keysBottom, rows)
        drawLip(colors, cx, front, keysBottom, bottom, badge, model)
    }

    // The opening in the deck onto the type basket: dark inside, its bars resting in a fan about
    // the printing point, their type at their outer ends, on a chrome segment.
    private fun DrawScope.drawOpening(colors: AppColors, u: Float, origin: Offset) {
        val cx = origin.x + TypewriterDimens.StrikeX * u
        val cy = origin.y + TypewriterDimens.BasketY * u
        val rx = TypewriterDimens.OpeningWidth / 2f * u
        val ry = TypewriterDimens.OpeningDepth * u
        openingPath.reset()
        openingPath.addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry))
        // Only where the deck is: its back edge cuts the opening off behind.
        val deck = origin.y + TypewriterDimens.BodyTop * u
        clipRect(cx - rx, deck, cx + rx, cy + ry) {
            clipPath(openingPath) {
                drawRect(
                    Brush.radialGradient(
                        listOf(
                            colors.typePlaten,
                            colors.typeRibbonBlack,
                            lerp(colors.typeRibbonBlack, colors.typeEnamelDeep, 0.3f),
                        ),
                        center = Offset(cx, cy),
                        radius = ry,
                    ),
                    Offset(cx - rx, cy - ry),
                    Size(rx * 2f, ry * 2f),
                )
                val bars = TypewriterDimens.BasketBars
                for (i in 0 until bars) {
                    val a = basketAngle(i / (bars - 1f))
                    val c = cos(a)
                    val s = sin(a)
                    val inner =
                        Offset(
                            cx + c * TypewriterDimens.BasketInner * u,
                            cy + s * TypewriterDimens.BasketInner * u,
                        )
                    val outer =
                        Offset(
                            cx + c * TypewriterDimens.BasketOuter * u,
                            cy + s * TypewriterDimens.BasketOuter * u,
                        )
                    drawLine(
                        colors.typeChrome.copy(alpha = BarAlpha),
                        inner,
                        outer,
                        strokeWidth = 1.3f * u,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        colors.typeChromeSheen.copy(alpha = BarGlintAlpha),
                        inner,
                        outer,
                        strokeWidth = 0.5f * u,
                    )
                    drawCircle(colors.typePlaten, 1.4f * u, outer)
                }
                // The segment the bars hang in.
                drawArc(
                    colors.typeChrome,
                    startAngle = TypewriterDimens.BasketFrom - 6f,
                    sweepAngle = TypewriterDimens.BasketTo - TypewriterDimens.BasketFrom + 12f,
                    useCenter = false,
                    topLeft =
                        Offset(
                            cx - TypewriterDimens.BasketInner * u,
                            cy - TypewriterDimens.BasketInner * u,
                        ),
                    size =
                        Size(
                            TypewriterDimens.BasketInner * 2f * u,
                            TypewriterDimens.BasketInner * 2f * u,
                        ),
                    style = Stroke(2.2f * u),
                )
                // The opening's own shadow under its rim.
                drawOval(
                    Brush.radialGradient(
                        0.75f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.6f),
                        center = Offset(cx, cy),
                        radius = rx,
                    ),
                    Offset(cx - rx, cy - ry),
                    Size(rx * 2f, ry * 2f),
                )
            }
        }
        // Its rim catching the light at the front.
        drawArc(
            Color.White.copy(alpha = RimLight),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(1.2f * u),
        )
    }

    // The bell: a small brass dome on the deck, lit from above.
    private fun DrawScope.drawBell(colors: AppColors, u: Float, origin: Offset) {
        val c = Offset(origin.x + TypewriterDimens.BellX * u, origin.y + TypewriterDimens.BellY * u)
        val r = TypewriterDimens.BellRadius * u
        with(shadow) {
            drawSoftShadow(c.x - r, c.y - r * 0.4f, c.x + r, c.y + r * 1.1f, 2f * u, 0.45f)
        }
        drawCircle(
            Brush.radialGradient(
                listOf(
                    Color.White,
                    colors.typeBrass,
                    lerp(colors.typeBrass, colors.typePlaten, 0.55f),
                ),
                center = c - Offset(r * 0.35f, r * 0.4f),
                radius = r * 1.3f,
            ),
            r,
            c,
        )
        drawCircle(colors.typeChromeSheen, r * 0.18f, c)
    }

    // The well the keys stand in, a step up for each row towards the back; dark under the keys,
    // shadowed where the deck overhangs it.
    private fun DrawScope.drawWell(
        colors: AppColors,
        cx: Float,
        front: Float,
        keysTop: Float,
        keysBottom: Float,
        rows: Int,
    ) {
        val inset = TypewriterDimens.ShellSide.toPx() * 1.5f
        val left = cx - front / 2f + inset
        val right = cx + front / 2f - inset
        val corner = TypewriterDimens.WellCorner.toPx()
        wellPath.reset()
        wellPath.addRoundRect(
            RoundRect(Rect(left, keysTop, right, keysBottom), CornerRadius(corner))
        )
        drawPath(
            wellPath,
            Brush.verticalGradient(
                listOf(
                    colors.typeRibbonBlack,
                    lerp(colors.typeRibbonBlack, colors.typeEnamelDeep, 0.35f),
                ),
                startY = keysTop,
                endY = keysBottom,
            ),
        )
        clipPath(wellPath) {
            // Each row's step: a lit edge where it rises, and its shadow on the row in front.
            val rowHeight = (keysBottom - keysTop) / rows
            for (i in 1 until rows) {
                val y = keysTop + rowHeight * i
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = StepShadow), Color.Transparent),
                        startY = y,
                        endY = y + rowHeight * 0.4f,
                    ),
                    Offset(left, y),
                    Size(right - left, rowHeight * 0.4f),
                )
                drawLine(
                    Color.White.copy(alpha = StepLight),
                    Offset(left, y),
                    Offset(right, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            // The deck overhanging the back of the well.
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = OverhangShadow), Color.Transparent),
                    startY = keysTop,
                    endY = keysTop + rowHeight * 0.5f,
                ),
                Offset(left, keysTop),
                Size(right - left, rowHeight * 0.5f),
            )
        }
        drawPath(wellPath, Color.White.copy(alpha = RimLight), style = Stroke(1.2.dp.toPx()))
    }

    // The lip under the keys, the shell curving down and under towards the table, and the badge
    // in chrome on it.
    private fun DrawScope.drawLip(
        colors: AppColors,
        cx: Float,
        front: Float,
        keysBottom: Float,
        bottom: Float,
        badge: TextLayoutResult?,
        model: TextLayoutResult?,
    ) {
        val top = keysBottom + 2.dp.toPx()
        val left = cx - front / 2f
        drawRect(
            Brush.verticalGradient(
                0f to colors.typeEnamelLight,
                0.15f to colors.typeEnamel,
                1f to lerp(colors.typeEnamelDeep, colors.typePlaten, 0.4f),
                startY = top,
                endY = bottom,
            ),
            Offset(left, top),
            Size(front, bottom - top),
        )
        drawRect(
            ShaderBrush(ImageShader(peel, TileMode.Repeated, TileMode.Repeated)),
            Offset(left, top),
            Size(front, bottom - top),
            alpha = PeelAlpha,
        )
        drawLine(
            Color.White.copy(alpha = LipLight),
            Offset(left + 8.dp.toPx(), top + 1.dp.toPx()),
            Offset(left + front - 8.dp.toPx(), top + 1.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
        )
        val lipHeight = TypewriterDimens.FrontLip.toPx()
        if (badge != null) {
            val width = badge.size.width + (model?.size?.width?.plus(lipHeight * 0.5f) ?: 0f)
            val y = top + (lipHeight - badge.size.height) / 2f
            val x = cx - width / 2f
            // Lettered in relief: a dark edge under the lettering, then the lettering.
            drawText(badge, Color.Black.copy(alpha = 0.35f), Offset(x, y + 1.dp.toPx()))
            drawText(badge, colors.typeBadge, Offset(x, y))
            if (model != null) {
                val mx = x + badge.size.width + lipHeight * 0.5f
                val my = top + (lipHeight - model.size.height) / 2f
                drawText(model, colors.typeBadge.copy(alpha = 0.75f), Offset(mx, my))
            }
        }
    }

    private companion object {
        const val TableShadow = 0.4f
        const val ContactShadow = 0.5f
        const val EdgeFall = 0.22f
        const val StudioLight = 0.2f
        const val RimLight = 0.3f
        const val LipLight = 0.28f
        const val PeelAlpha = 0.55f
        const val BarAlpha = 0.85f
        const val BarGlintAlpha = 0.5f
        const val StepShadow = 0.45f
        const val StepLight = 0.08f
        const val OverhangShadow = 0.6f
        const val PeelSize = 64

        // A tile of the enamel's orange peel: fine specks, a little lighter and a little darker.
        fun orangePeel(): ImageBitmap {
            val image = ImageBitmap(PeelSize, PeelSize)
            val canvas = Canvas(image)
            val paint = Paint()
            for (i in 0 until PeelSize * PeelSize / 6) {
                val x = noise(i, 31) * PeelSize
                val y = noise(i, 32) * PeelSize
                val light = noise(i, 33) < 0.5f
                paint.color =
                    if (light) Color.White.copy(alpha = 0.05f + 0.05f * noise(i, 34))
                    else Color.Black.copy(alpha = 0.05f + 0.06f * noise(i, 34))
                canvas.drawCircle(Offset(x, y), 0.6f + 0.8f * noise(i, 35), paint)
            }
            return image
        }
    }
}

/** The angle, in radians, at which the bar for a key [place] across the basket (0 to 1) lies. */
internal fun basketAngle(place: Float): Float {
    val from = TypewriterDimens.BasketTo
    val to = TypewriterDimens.BasketFrom
    return ((from + (to - from) * place) * PI / 180.0).toFloat()
}
