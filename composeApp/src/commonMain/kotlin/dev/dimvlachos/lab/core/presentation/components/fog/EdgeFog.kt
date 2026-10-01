package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlin.random.Random

/**
 * How clear each part of "clear" glass is, in alpha: 1 wiped clean, 0 fogged. Real windows keep a
 * ragged band of condensation round their edges, deepest along the bottom and into the corners, so
 * clear glass is never a perfect sheet. Drawn once from [seed] and stretched over the window.
 */
internal fun edgeFogMask(
    width: Int = FogDimens.EdgeMaskWidth,
    height: Int = FogDimens.EdgeMaskHeight,
    seed: Int = 5,
): ImageBitmap {
    val random = Random(seed)
    val top = wobble(random)
    val bottom = wobble(random)
    val left = wobble(random)
    val right = wobble(random)
    val grain =
        List(FogDimens.EdgeGrainKnots) { List(FogDimens.EdgeGrainKnots) { random.nextFloat() } }
    val mask = ImageBitmap(width, height)
    val canvas = Canvas(mask)
    val paint = Paint()
    val pixel = Size(1f, 1f)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val u = x / (width - 1f)
            val v = y / (height - 1f)
            // A fine raggedness on top of each side's slow wobble.
            val rag = (grainAt(grain, u, v) - 0.5f) * FogDimens.EdgeRag
            val clear =
                sideClear(v + rag, FogDimens.EdgeDepthTop * top(u)) *
                    sideClear(1 - v + rag, FogDimens.EdgeDepthBottom * bottom(u)) *
                    sideClear(u + rag, FogDimens.EdgeDepthSide * left(v)) *
                    sideClear(1 - u + rag, FogDimens.EdgeDepthSide * right(v))
            paint.color = Color.White.copy(alpha = clear)
            canvas.drawRect(Rect(Offset(x.toFloat(), y.toFloat()), pixel), paint)
        }
    }
    return mask
}

// Fog along one side: fogged within its depth, fading to clear over a soft band beyond it.
private fun sideClear(distance: Float, depth: Float): Float {
    val t = ((distance - depth * 0.55f) / (depth * 0.8f)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

// A side's depth along its length: between about half and one and a half times its base depth.
private fun wobble(random: Random): (Float) -> Float {
    val knots = List(FogDimens.EdgeWobbleKnots) { random.nextFloat() }
    return { at -> 0.5f + knotsAt(knots, at) }
}

private fun knotsAt(knots: List<Float>, at: Float): Float {
    val position = at.coerceIn(0f, 1f) * (knots.size - 1)
    val i = position.toInt().coerceAtMost(knots.size - 2)
    val t = position - i
    val smooth = t * t * (3 - 2 * t)
    return knots[i] + (knots[i + 1] - knots[i]) * smooth
}

private fun grainAt(grain: List<List<Float>>, u: Float, v: Float): Float {
    val column = List(grain.size) { knotsAt(grain[it], u) }
    return knotsAt(column, v)
}
