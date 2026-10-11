package dev.dimvlachos.lab.core.presentation.components.touch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A scripted fingertip, the same in every demo: it comes down this long, a tap holds it this long,
 * and it lifts this long, and it is this big round.
 */
internal object ScriptedTouch {
    const val DownMs = 90
    const val TapHoldMs = 110L
    const val UpMs = 260
    val Radius = 18.dp
}

/** The scripted fingertip at [center], [alpha] of the way down: a soft spot in [color], ringed. */
internal fun DrawScope.drawTouch(color: Color, center: Offset, alpha: Float) {
    if (alpha <= 0f) return
    val radius = ScriptedTouch.Radius.toPx()
    drawCircle(color.copy(alpha = FillAlpha * alpha), radius, center)
    drawCircle(
        color.copy(alpha = RingAlpha * alpha),
        radius,
        center,
        style = Stroke(RingWidth.toPx()),
    )
}

private const val FillAlpha = 0.28f
private const val RingAlpha = 0.7f
private val RingWidth = 1.5.dp
