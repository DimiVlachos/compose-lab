package dev.dimvlachos.lab.core.presentation.components.frost

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize

/**
 * A window of [photo] behind frosted glass that a finger wipes clear.
 *
 * The frost, a blurred copy of the photo under a pale film and grain, lives in its own offscreen
 * layer, and each wipe erases from that layer alone with [BlendMode.DstOut], leaving the sharp
 * photo showing through. DstOut rather than Clear: Clear zeroes a pixel whatever the brush's alpha,
 * so it cannot give a soft edge, while DstOut removes frost in step with the brush's fading rim.
 */
@Composable
fun FrostedWindow(
    photo: Painter,
    state: FrostState,
    modifier: Modifier = Modifier,
    brushRadius: Dp = FrostDimens.BrushRadius,
) {
    val noise = remember { frostNoiseTile() }
    val grain =
        remember(noise) { ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated)) }
    Box(
        modifier.pointerInput(state) {
            var stroke: WipeStroke? = null
            detectDragGestures(
                onDragStart = { stroke = state.beginStroke(it.fractionOf(size)) },
                onDrag = { change, _ ->
                    stroke?.let { state.extendStroke(it, change.position.fractionOf(size)) }
                },
            )
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
                    for (stroke in state.strokes) {
                        val pixels = stroke.map { Offset(it.x * size.width, it.y * size.height) }
                        for (dab in wipeDabs(pixels, radius * FrostDimens.DabSpacingRatio)) {
                            translate(dab.x, dab.y) {
                                drawCircle(brush, radius, Offset.Zero, blendMode = BlendMode.DstOut)
                            }
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
                        drawRect(Color.White.copy(alpha = FrostDimens.TintAlpha))
                        drawRect(grain)
                    }
                    .blur(FrostDimens.FrostBlur),
                contentScale = ContentScale.Crop,
            )
        }
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
