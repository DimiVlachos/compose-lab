package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fog_density
import dev.dimvlachos.lab.resources.fog_detail
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.imageResource

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
 * Every finger wipes its own stroke, so several can wipe at once. With [onHoldChange] set, a finger
 * that stays still for [FogDimens.HoldDelayMillis] holds instead of wiping: `true` when the first
 * finger starts holding, `false` when the last lets go or the window goes away.
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
    val detail = imageResource(Res.drawable.fog_detail)
    val density = imageResource(Res.drawable.fog_density)
    val milky = remember { ColorFilter.colorMatrix(milkyColorMatrix()) }
    val edgeMask = remember { edgeFogMask() }
    Box(
        modifier.pointerInput(state, onHoldChange != null) {
            val holdEnabled = onHoldChange != null
            awaitEachGesture {
                // Each finger on its own: pending until it moves past touch slop, a wipe, or stays
                // still long enough, a hold. The glass breathes while any finger holds.
                val fingers = mutableMapOf<PointerId, Finger>()
                var holding = 0
                // The callback the hold started with lets it go: switching holding off mid-hold
                // clears the callback before the gesture is cancelled.
                var onHold: ((Boolean) -> Unit)? = null
                fun startHolding() {
                    if (holding++ == 0) {
                        onHold = holdChange
                        onHold?.invoke(true)
                    }
                }
                fun stopHolding() {
                    if (--holding == 0) {
                        onHold?.invoke(false)
                        onHold = null
                    }
                }
                var now = 0L
                try {
                    do {
                        val due =
                            if (holdEnabled) {
                                fingers.values.filterIsInstance<Finger.Pending>().minOfOrNull {
                                    it.holdAt
                                }
                            } else {
                                null
                            }
                        val event =
                            if (due == null) awaitPointerEvent()
                            else
                                withTimeoutOrNull((due - now).coerceAtLeast(0)) {
                                    awaitPointerEvent()
                                }
                        if (event == null) {
                            // A still finger sends nothing; its hold comes due on the clock.
                            now = due!!
                            fingers.entries
                                .filter { (_, finger) ->
                                    finger is Finger.Pending && finger.holdAt <= now
                                }
                                .forEach { entry ->
                                    entry.setValue(Finger.Holding)
                                    startHolding()
                                }
                            continue
                        }
                        for (change in event.changes) {
                            now = change.uptimeMillis
                            val finger = fingers[change.id]
                            when {
                                change.changedToDown() ->
                                    fingers[change.id] =
                                        Finger.Pending(
                                            change.position,
                                            change.uptimeMillis + FogDimens.HoldDelayMillis,
                                        )
                                !change.pressed -> {
                                    if (fingers.remove(change.id) == Finger.Holding) stopHolding()
                                }
                                finger is Finger.Pending &&
                                    (change.position - finger.down).getDistance() >
                                        viewConfiguration.touchSlop -> {
                                    val stroke = state.beginStroke(finger.down.fractionOf(size))
                                    state.extendStroke(stroke, change.position.fractionOf(size))
                                    fingers[change.id] = Finger.Wiping(stroke)
                                    change.consume()
                                }
                                finger is Finger.Wiping && change.positionChanged() -> {
                                    state.extendStroke(
                                        finger.stroke,
                                        change.position.fractionOf(size),
                                    )
                                    change.consume()
                                }
                            }
                        }
                    } while (fingers.isNotEmpty())
                } finally {
                    if (holding > 0) onHold?.invoke(false)
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
                    // One brush per width, centred on the origin and moved to each dab, not a
                    // gradient per dab.
                    val brush = softBrush(radius)
                    for (mark in state.marks) {
                        when (mark) {
                            is WipeStroke ->
                                when (val own = mark.radius?.toPx()) {
                                    null -> drawWipe(mark.points, radius, brush)
                                    else -> drawWipe(mark.points, own, softBrush(own))
                                }
                            is Breath -> drawFog(mark.level)
                            is Evaporation ->
                                // Clear glass keeps a ragged band of fog round its edges.
                                drawImage(
                                    edgeMask,
                                    dstSize =
                                        IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                    alpha = mark.amount,
                                    blendMode = BlendMode.DstOut,
                                    filterQuality = FilterQuality.High,
                                )
                        }
                    }
                }
        ) {
            Image(
                photo,
                null,
                // The film and the condensation draw over the blur's layer, not inside it, so the
                // droplets stay sharp.
                Modifier.matchParentSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(Color.White.copy(alpha = FogDimens.FilmAlpha))
                        // Real condensation from a photograph: its droplets, drops and trails as
                        // light and shade over the fog, then its thickness, thinner along the
                        // drips,
                        // where the sharp scene shows through as it does through real glass.
                        drawCovering(detail, BlendMode.Overlay)
                        drawCovering(density, BlendMode.DstIn)
                    }
                    .blur(FogDimens.FogBlur),
                contentScale = ContentScale.Crop,
                colorFilter = milky,
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

// Fog fills in what is missing rather than piling onto fog already there: under the breath's mask
// the glass becomes the fog's natural state, existing × (1 − mask) + fog × mask. So the existing
// layer is first cleared through the mask, then the fog, kept only where the mask is, is added.
private fun ContentDrawScope.drawFog(level: Float) {
    if (level <= 0f) return
    val bounds = Rect(Offset.Zero, size)
    drawIntoCanvas { canvas ->
        canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.DstOut })
        drawFogMask(level)
        canvas.restore()
        canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.Plus })
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

// One finger on the glass: undecided, wiping its own stroke, or holding still to breathe.
private sealed interface Finger {
    class Pending(val down: Offset, val holdAt: Long) : Finger

    class Wiping(val stroke: WipeStroke) : Finger

    data object Holding : Finger
}

// Draws [image] over the whole area, cropped to cover it rather than stretched.
private fun DrawScope.drawCovering(image: ImageBitmap, blendMode: BlendMode) {
    val (offset, cropped) = coverCrop(IntSize(image.width, image.height), size)
    drawImage(
        image,
        srcOffset = offset,
        srcSize = cropped,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        blendMode = blendMode,
        filterQuality = FilterQuality.High,
    )
}
