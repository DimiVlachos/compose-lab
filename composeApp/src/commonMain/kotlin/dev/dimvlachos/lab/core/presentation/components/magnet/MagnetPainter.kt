package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.presentation.ui.AppColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Where a magnet's [count] photos go when they fan out: a grid of up to three columns, centred on a
 * [width] × [height] table, its last row centred too. The cards grow to at most FanScale times
 * their size, less if that wouldn't fit, and keep [gap] apart and from the edges.
 */
internal class FanGrid(
    private val count: Int,
    private val width: Float,
    private val height: Float,
    private val cardWidth: Float,
    private val cardHeight: Float,
    private val gap: Float,
) {
    private val columns = count.coerceIn(1, MagnetDimens.FanColumns)
    private val rows = ((count + columns - 1) / columns).coerceAtLeast(1)

    val scale: Float =
        min(
                MagnetDimens.FanScale,
                min(
                    (width - gap * (columns + 1)) / (columns * cardWidth),
                    (height - gap * (rows + 1)) / (rows * cardHeight),
                ),
            )
            .coerceAtLeast(0.1f)

    /** The middle of the [index]th card's cell. */
    fun cell(index: Int): Offset {
        val w = cardWidth * scale
        val h = cardHeight * scale
        val row = index / columns
        val column = index % columns
        val inRow = if (row == rows - 1) count - row * columns else columns
        val rowWidth = inRow * w + (inRow - 1) * gap
        val gridHeight = rows * h + (rows - 1) * gap
        return Offset(
            (width - rowWidth) / 2f + w / 2f + column * (w + gap),
            (height - gridHeight) / 2f + h / 2f + row * (h + gap),
        )
    }
}

/**
 * Draws the magnet table: the steel and the strip, the photo cards with their clips and shadows,
 * the magnets with their labels and count badges, and a fanned-out cluster over a scrim. Each
 * frame's cards and magnets are read here, in drawing, never in composition.
 */
internal class MagnetPainter {
    /** The steel table, deepening towards its foot, and the rail with a well for each slot. */
    fun DrawScope.drawTable(state: MagnetState, colors: AppColors, steel: Brush) {
        val tableBottom = state.tableHeight * density
        drawRect(steel, size = Size(size.width, tableBottom))
        drawRect(
            colors.stripRail,
            Offset(0f, tableBottom),
            Size(size.width, size.height - tableBottom),
        )
        val well = MagnetDimens.MagnetRadius.toPx() + 3.dp.toPx()
        for (magnet in state.magnets) drawCircle(colors.stripWell, well, magnet.slot * density)
    }

    /** The cards: loose ones first, then the stuck, then a held one, each layer over the last. */
    fun DrawScope.drawCards(state: MagnetState, thumbs: List<ImageBitmap>, colors: AppColors) {
        state.frame
        val shown = state.fanShown
        val fanning = shown != null && state.fan > 0f
        val bodies = state.bodies.bodies
        for (layer in 0..2) {
            for (i in bodies.indices) {
                val body = bodies[i]
                if (body.layer() != layer) continue
                // Drawn by the grid instead, over its scrim.
                if (fanning && shown in body.stuckTo) continue
                drawCard(thumbs[i], body.at * density, body.tilt, 1f, colors)
            }
        }
    }

    /** The magnets, each with its label under it and, out of the strip, its count over it. */
    fun DrawScope.drawMagnets(
        state: MagnetState,
        labels: List<TextLayoutResult>,
        badges: List<TextLayoutResult?>,
        colors: AppColors,
    ) {
        state.frame
        val radius = MagnetDimens.MagnetRadius.toPx()
        for (i in state.magnets.indices) {
            val magnet = state.magnets[i]
            val centre = magnet.at * density
            val r = if (magnet.held) radius * MagnetDimens.HeldLift else radius
            val drop = (if (magnet.held) 6.dp else 3.dp).toPx()
            drawCircle(colors.cardShadow, r, centre + Offset(drop / 2f, drop))
            drawCircle(colors.magnetSteel, r, centre)
            drawCircle(colors.magnetRed, r * FaceShare, centre)
            drawCircle(
                colors.magnetSheen,
                r * 0.3f,
                centre + Offset(-r * 0.32f, -r * 0.32f),
                alpha = 0.35f,
            )
            val label = labels[i]
            val padX = MagnetDimens.BadgePadX.toPx()
            val padY = MagnetDimens.BadgePadY.toPx()
            val labelTop =
                Offset(
                    centre.x - label.size.width / 2f,
                    centre.y + r + MagnetDimens.LabelGap.toPx(),
                )
            // Out on the table the label lies over cards, so it gets the rail's dark behind it.
            if (magnet.out) {
                val labelHeight = label.size.height + padY * 2f
                drawRoundRect(
                    colors.stripRail,
                    labelTop - Offset(padX, padY),
                    Size(label.size.width + padX * 2f, labelHeight),
                    CornerRadius(labelHeight / 2f),
                    alpha = 0.8f,
                )
            }
            drawText(label, topLeft = labelTop)
            val badge = badges[i] ?: continue
            val w = badge.size.width + padX * 2f
            val h = badge.size.height + padY * 2f
            // Kept on the table by a magnet at its top edge.
            val topLeft =
                Offset(centre.x - w / 2f, max(0f, centre.y - r - MagnetDimens.BadgeGap.toPx() - h))
            drawRoundRect(colors.badge, topLeft, Size(w, h), CornerRadius(h / 2f))
            drawText(badge, topLeft = topLeft + Offset(padX, padY))
        }
    }

    /** A magnet's photos, fanned out of its cluster into a grid over a scrim, or folding back. */
    fun DrawScope.drawFan(state: MagnetState, thumbs: List<ImageBitmap>, colors: AppColors) {
        state.frame
        val tag = state.fanShown ?: return
        val t = smooth(state.fan)
        if (t <= 0f) return
        val tableBottom = state.tableHeight * density
        drawRect(colors.fanScrim, size = Size(size.width, tableBottom), alpha = 0.7f * t)
        val bodies = state.bodies.bodies
        val count = bodies.count { tag in it.stuckTo }
        if (count == 0) return
        val grid =
            FanGrid(
                count,
                size.width,
                tableBottom,
                MagnetDimens.CardWidth.toPx(),
                MagnetDimens.CardHeight.toPx(),
                MagnetDimens.FanGap.toPx(),
            )
        var k = 0
        for (i in bodies.indices) {
            val body = bodies[i]
            if (tag !in body.stuckTo) continue
            val centre = lerp(body.at * density, grid.cell(k++), t)
            drawCard(thumbs[i], centre, lerp(body.tilt, 0f, t), lerp(1f, grid.scale, t), colors)
        }
    }

    private fun DrawScope.drawCard(
        image: ImageBitmap,
        centre: Offset,
        tilt: Float,
        scale: Float,
        colors: AppColors,
    ) {
        val w = MagnetDimens.CardWidth.toPx() * scale
        val h = MagnetDimens.CardHeight.toPx() * scale
        val corner = CornerRadius(MagnetDimens.CardCorner.toPx() * scale)
        val border = MagnetDimens.CardBorder.toPx() * scale
        translate(centre.x, centre.y) {
            rotate(tilt * DegreesPerRadian, pivot = Offset.Zero) {
                val shadow = 3.dp.toPx() * scale
                drawRoundRect(
                    colors.cardShadow,
                    Offset(-w / 2f + shadow / 2f, -h / 2f + shadow),
                    Size(w, h),
                    corner,
                )
                drawRoundRect(colors.cardPaper, Offset(-w / 2f, -h / 2f), Size(w, h), corner)
                drawImage(
                    image,
                    dstOffset =
                        IntOffset((-w / 2f + border).roundToInt(), (-h / 2f + border).roundToInt()),
                    dstSize =
                        IntSize((w - border * 2f).roundToInt(), (h - border * 2f).roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
                // The clip across the top edge, with a line of light along it.
                val clipW = MagnetDimens.ClipWidth.toPx() * scale
                val clipH = MagnetDimens.ClipHeight.toPx() * scale
                val clipTop = Offset(-clipW / 2f, -h / 2f - clipH / 2f)
                drawRoundRect(
                    colors.clipMetal,
                    clipTop,
                    Size(clipW, clipH),
                    CornerRadius(clipH / 3f),
                )
                drawLine(
                    colors.clipSheen,
                    clipTop + Offset(clipW * 0.2f, clipH * 0.3f),
                    clipTop + Offset(clipW * 0.8f, clipH * 0.3f),
                    strokeWidth = 1.dp.toPx() * scale,
                )
            }
        }
    }
}

/**
 * [source] drawn once into a [width] × [height] bitmap, centre-cropped to it: the cards draw this
 * small copy every frame instead of the full photo.
 */
internal fun thumbnail(source: ImageBitmap, width: Int, height: Int): ImageBitmap {
    val image = ImageBitmap(max(1, width), max(1, height))
    val scale = max(width / source.width.toFloat(), height / source.height.toFloat())
    val cropWidth = (width / scale).roundToInt().coerceIn(1, source.width)
    val cropHeight = (height / scale).roundToInt().coerceIn(1, source.height)
    Canvas(image)
        .drawImageRect(
            source,
            IntOffset((source.width - cropWidth) / 2, (source.height - cropHeight) / 2),
            IntSize(cropWidth, cropHeight),
            IntOffset.Zero,
            IntSize(image.width, image.height),
            Paint().apply { filterQuality = FilterQuality.Medium },
        )
    return image
}

// Eased in and out, so the grid starts and lands softly.
private fun smooth(t: Float): Float = t * t * (3f - 2f * t)

// The share of a magnet that is its red face, inside the steel rim.
private const val FaceShare = 0.78f

private const val DegreesPerRadian = 57.29578f
