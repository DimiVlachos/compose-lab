package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize

/**
 * A window of [photo] behind fogged glass that a finger wipes clear.
 *
 * The fog, a blurred copy of the photo under a pale film and grain, lives in its own offscreen
 * layer, and each wipe erases from that layer alone with [BlendMode.DstOut], leaving the sharp
 * photo showing through. DstOut rather than Clear: Clear zeroes a pixel whatever the brush's alpha,
 * so it cannot give a soft edge, while DstOut removes fog in step with the brush's fading rim.
 *
 * Breaths fog the glass back over: each draws the fog again through a mask rising from the bottom.
 * Wipes and breaths replay in the order they were made, so a later wipe clears fresh fog too.
 *
 * With [onHoldChange] set, a press that stays still for [FogDimens.HoldDelayMillis] reports `true`
 * instead of wiping, and `false` when it lifts or the window goes away.
 */
@Composable
fun FoggedWindow(
    photo: Painter,
    state: FogState,
    modifier: Modifier = Modifier,
    brushRadius: Dp = FogDimens.BrushRadius,
    onHoldChange: ((Boolean) -> Unit)? = null,
) {
    // A caller's lambda changes on every recomposition; the gesture reads the latest without
    // restarting.
    val holdChange by rememberUpdatedState(onHoldChange)
    val noise = remember { fogNoiseTile() }
    val grain =
        remember(noise) { ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated)) }
    Box(
        modifier.pointerInput(state, onHoldChange != null) {
            val holdEnabled = onHoldChange != null
            awaitEachGesture {
                val down = awaitFirstDown()
                val start =
                    if (holdEnabled) {
                        withTimeoutOrNull(FogDimens.HoldDelayMillis) { awaitDragOrLift(down) }
                            ?: PressStart.Held
                    } else {
                        awaitDragOrLift(down)
                    }
                when (start) {
                    PressStart.Held -> {
                        // The callback the hold started with lets it go: switching holding off
                        // mid-hold clears the callback before the gesture is cancelled.
                        val onHold = holdChange
                        onHold?.invoke(true)
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            onHold?.invoke(false)
                        }
                    }
                    PressStart.Lifted -> Unit
                    is PressStart.Dragged -> {
                        val stroke = state.beginStroke(down.position.fractionOf(size))
                        state.extendStroke(stroke, start.change.position.fractionOf(size))
                        drag(start.change.id) { change ->
                            state.extendStroke(stroke, change.position.fractionOf(size))
                            change.consume()
                        }
                    }
                }
            }
        }
    ) {
        Image(photo, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val radius = brushRadius.toPx()
                    // One brush, centred on the origin and moved to each dab, not a gradient per
                    // dab.
                    val brush = softBrush(radius)
                    for (mark in state.marks) {
                        when (mark) {
                            is WipeStroke -> drawWipe(mark.points, radius, brush)
                            is Breath -> drawFog(mark.level)
                            is Evaporation ->
                                drawRect(
                                    Color.Black.copy(alpha = mark.amount),
                                    blendMode = BlendMode.DstOut,
                                )
                        }
                    }
                }
        ) {
            Image(
                photo,
                null,
                // The film and grain draw over the blur's layer, not inside it, so the grain stays
                // sharp.
                Modifier.matchParentSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(Color.White.copy(alpha = FogDimens.TintAlpha))
                        drawRect(grain)
                    }
                    .blur(FogDimens.FogBlur),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun DrawScope.drawWipe(stroke: List<Offset>, radius: Float, brush: Brush) {
    val pixels = stroke.map { Offset(it.x * size.width, it.y * size.height) }
    for (dab in wipeDabs(pixels, radius * FogDimens.DabSpacingRatio)) {
        translate(dab.x, dab.y) {
            drawCircle(brush, radius, Offset.Zero, blendMode = BlendMode.DstOut)
        }
    }
}

// The fog drawn again, kept only where the fog mask is: the mask goes into a layer, then the
// fog is drawn into a nested layer that SrcIn composites onto the mask alone.
private fun ContentDrawScope.drawFog(level: Float) {
    if (level <= 0f) return
    val bounds = Rect(Offset.Zero, size)
    drawIntoCanvas { canvas ->
        canvas.saveLayer(bounds, Paint())
        drawFogMask(level)
        canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.SrcIn })
        drawContent()
        canvas.restore()
        canvas.restore()
    }
}

// Each dab is faint on its own: along a stroke some eight overlap, and DstOut compounds them, so a
// full-strength dab would harden the edge the gradient is there to soften.
private fun softBrush(radius: Float) =
    Brush.radialGradient(
        0f to Color.Black.copy(alpha = 0.6f),
        0.4f to Color.Black.copy(alpha = 0.45f),
        1f to Color.Transparent,
        center = Offset.Zero,
        radius = radius,
    )

private fun Offset.fractionOf(size: IntSize) = Offset(x / size.width, y / size.height)

private sealed interface PressStart {
    data object Held : PressStart

    data object Lifted : PressStart

    class Dragged(val change: PointerInputChange) : PressStart
}

// Waits for the press to move past touch slop, a drag, or to lift first.
private suspend fun AwaitPointerEventScope.awaitDragOrLift(down: PointerInputChange): PressStart =
    awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
        ?.let { PressStart.Dragged(it) } ?: PressStart.Lifted
