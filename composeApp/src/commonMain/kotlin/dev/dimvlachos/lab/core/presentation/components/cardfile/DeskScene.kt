package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.CardFileColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The walnut desk at night and the lamp's warm pool on it, centred on [rod]. Its brushes are built
 * once per size, not per frame.
 */
internal fun Modifier.desk(rod: Offset): Modifier = drawWithCache {
    val wood =
        Brush.verticalGradient(
            listOf(CardFileColors.night, CardFileColors.woodMid, CardFileColors.woodNear)
        )
    val poolCentre = rod - Offset(0f, size.height * 0.1f)
    val poolRadius = size.minDimension * 0.75f
    val pool =
        Brush.radialGradient(
            listOf(CardFileColors.lampPool, CardFileColors.lampPoolEdge, Color.Transparent),
            poolCentre,
            poolRadius,
        )
    onDrawBehind {
        drawRect(wood)
        drawCircle(pool, poolRadius, poolCentre)
    }
}

/**
 * The chrome stand the wheel sits on: a plinth on the desk, its shadow, and a bent chrome foot up
 * to each end of the rod at [rod]. Drawn behind the cards, recorded once per size.
 */
internal fun Modifier.stand(rod: Offset, halfWidth: Float, baseDrop: Float): Modifier =
    drawWithCache {
        val baseY = rod.y + baseDrop
        val baseHalf = halfWidth + 10.dp.toPx()
        val baseHeight = 16.dp.toPx()
        val plinth =
            Brush.verticalGradient(
                listOf(CardFileColors.chromeSheen, CardFileColors.chromeDeep, CardFileColors.base),
                baseY,
                baseY + baseHeight,
            )
        val shadow =
            Brush.radialGradient(
                listOf(CardFileColors.deskShadow, Color.Transparent),
                Offset(rod.x, baseY + baseHeight),
                baseHalf * 1.1f,
            )
        val footWidth = 6.dp.toPx()
        val foot =
            Brush.horizontalGradient(
                listOf(
                    CardFileColors.chromeDeep,
                    CardFileColors.chromeBright,
                    CardFileColors.chromeShade,
                )
            )
        onDrawBehind {
            // The plinth's shadow on the desk, flattened into an ellipse.
            drawOval(
                shadow,
                Offset(rod.x - baseHalf * 1.1f, baseY + baseHeight - 10.dp.toPx()),
                Size(baseHalf * 2.2f, 26.dp.toPx()),
            )
            for (side in intArrayOf(-1, 1)) {
                val top = Offset(rod.x + side * (halfWidth - 4.dp.toPx()), rod.y)
                val bottom = Offset(rod.x + side * (halfWidth - 30.dp.toPx()), baseY + 2f)
                drawLine(CardFileColors.chromeShade, top, bottom, footWidth)
                drawLine(
                    CardFileColors.chromeBright,
                    top + Offset(-side * footWidth / 4, 0f),
                    bottom + Offset(-side * footWidth / 4, 0f),
                    footWidth / 3,
                )
            }
            drawRoundRect(
                plinth,
                Offset(rod.x - baseHalf, baseY),
                Size(baseHalf * 2, baseHeight),
                CornerRadius(baseHeight / 2),
            )
            drawRoundRect(
                CardFileColors.baseTop,
                Offset(rod.x - baseHalf + baseHeight / 2, baseY + 2f),
                Size(baseHalf * 2 - baseHeight, 2.dp.toPx()),
                CornerRadius(1.dp.toPx()),
            )
        }
    }

/**
 * The chrome rod through the cards' notches and a knurled knob at each end, seen side-on as a short
 * drum. Everything but the knurling is recorded once into a layer; each frame of a spin only
 * redraws the knurling, turned by [knobTurn] (in cards), which is read in the draw lambda alone.
 */
internal fun Modifier.rod(rod: Offset, halfWidth: Float, knobTurn: () -> Float): Modifier =
    drawWithCache {
        val thick = CardFileDimens.RodThickness.toPx()
        val knobW = CardFileDimens.KnobWidth.toPx()
        val knobH = CardFileDimens.KnobHeight.toPx()
        val chrome =
            Brush.verticalGradient(
                listOf(
                    CardFileColors.chromeBright,
                    CardFileColors.chromeShade,
                    CardFileColors.chromeSheen,
                    CardFileColors.chromeDeep,
                ),
                rod.y - thick / 2,
                rod.y + thick / 2,
            )
        val drum =
            Brush.verticalGradient(
                listOf(
                    CardFileColors.chromeDeep,
                    CardFileColors.chromeBright,
                    CardFileColors.chromeSheen,
                    CardFileColors.chromeShade,
                    CardFileColors.chromeDeep,
                ),
                rod.y - knobH / 2,
                rod.y + knobH / 2,
            )
        val knobs = floatArrayOf(rod.x - halfWidth - knobW / 2, rod.x + halfWidth + knobW / 2)
        val ridge = 1.5.dp.toPx()
        val still = obtainGraphicsLayer()
        still.record {
            drawRoundRect(
                chrome,
                Offset(rod.x - halfWidth, rod.y - thick / 2),
                Size(halfWidth * 2, thick),
                CornerRadius(thick / 2),
            )
            for (x in knobs) {
                drawRoundRect(
                    drum,
                    Offset(x - knobW / 2, rod.y - knobH / 2),
                    Size(knobW, knobH),
                    CornerRadius(knobW * 0.3f),
                )
                // The knob's rims.
                for (rim in floatArrayOf(-1f, 1f)) {
                    drawRect(
                        CardFileColors.chromeDeep,
                        Offset(
                            x + rim * knobW / 2 - (if (rim > 0) 3.dp.toPx() else 0f),
                            rod.y - knobH / 2 + 3f,
                        ),
                        Size(3.dp.toPx(), knobH - 6f),
                    )
                }
            }
        }
        onDrawBehind {
            drawLayer(still)
            // The knurling: ridges along the drum, rolling round as the wheel turns and bunching
            // toward its top and bottom edges, where the drum curves away.
            val turn = knobTurn()
            val count = 14
            for (x in knobs) {
                for (k in 0 until count) {
                    val phase = ((k + turn * 3f) / count) * 2f * PI.toFloat()
                    // Only the half facing the viewer shows.
                    if (cos(phase) <= 0f) continue
                    val y = rod.y + sin(phase) * (knobH / 2 - 4f)
                    val light = cos(phase)
                    drawLine(
                        CardFileColors.knurl,
                        Offset(x - knobW / 2 + 3.dp.toPx(), y),
                        Offset(x + knobW / 2 - 3.dp.toPx(), y),
                        ridge * (0.5f + light),
                    )
                    drawLine(
                        CardFileColors.knurlLight,
                        Offset(x - knobW / 2 + 3.dp.toPx(), y + ridge),
                        Offset(x + knobW / 2 - 3.dp.toPx(), y + ridge),
                        ridge * 0.6f * light,
                    )
                }
            }
        }
    }
