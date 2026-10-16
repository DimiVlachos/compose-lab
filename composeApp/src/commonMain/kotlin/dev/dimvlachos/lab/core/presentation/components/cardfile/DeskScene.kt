package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import dev.dimvlachos.lab.core.presentation.ui.CardFileColors

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
 * The chrome base and rod the cards hang on, and a knurled knob at each end. Everything but the
 * knurling is recorded once into a layer; each frame of a spin only redraws the knurling, turned by
 * [knobTurn], which is read in the draw lambda alone.
 */
internal fun Modifier.frame(rod: Offset, halfWidth: Float, knobTurn: () -> Float): Modifier =
    drawWithCache {
        val chrome =
            Brush.verticalGradient(
                listOf(
                    CardFileColors.chromeBright,
                    CardFileColors.chromeShade,
                    CardFileColors.chromeSheen,
                    CardFileColors.chromeDeep,
                ),
                rod.y - 8f,
                rod.y + 8f,
            )
        val left = Offset(rod.x - (halfWidth + 18f), rod.y)
        val right = Offset(rod.x + (halfWidth + 18f), rod.y)
        val knobs = arrayOf(left, right)
        val still = obtainGraphicsLayer()
        still.record {
            drawRoundRect(
                chrome,
                Offset(rod.x - halfWidth, rod.y - 6f),
                Size(halfWidth * 2, 12f),
                CornerRadius(6f),
            )
            // Feet.
            for (side in intArrayOf(-1, 1)) {
                val x = rod.x + side * (halfWidth - 14f)
                drawLine(
                    CardFileColors.chromeFoot,
                    Offset(x, rod.y),
                    Offset(x + side * 26f, rod.y + 70f),
                    6f,
                )
            }
            drawRoundRect(
                CardFileColors.base,
                Offset(rod.x - halfWidth - 12f, rod.y + 66f),
                Size(halfWidth * 2 + 24f, 14f),
                CornerRadius(7f),
            )
            drawCircle(chrome, 20f, left)
            drawCircle(chrome, 20f, right)
        }
        onDrawBehind {
            drawLayer(still)
            // Knobs, their knurling turning with the wheel.
            val turn = knobTurn()
            for (c in knobs) {
                for (k in 0 until 10) {
                    val y = c.y - 18f + ((k * 4f + turn * 4f) % 36f)
                    drawLine(CardFileColors.knurl, Offset(c.x - 16f, y), Offset(c.x + 16f, y), 1.2f)
                }
            }
        }
    }
