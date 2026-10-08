package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
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
 * [width] × [height] table under a [header], its last row centred too, with room for a [caption]
 * under each card. The cards grow to at most FanScale times their size, less if that wouldn't fit,
 * and keep [gap] apart and from the edges.
 */
internal class FanGrid(
    private val count: Int,
    private val width: Float,
    private val height: Float,
    private val cardWidth: Float,
    private val cardHeight: Float,
    private val gap: Float,
    private val caption: Float = 0f,
    private val header: Float = 0f,
) {
    private val columns = count.coerceIn(1, MagnetDimens.FanColumns)
    private val rows = ((count + columns - 1) / columns).coerceAtLeast(1)

    val scale: Float =
        min(
                MagnetDimens.FanScale,
                min(
                    (width - gap * (columns + 1)) / (columns * cardWidth),
                    (height - header - gap * (rows + 1) - rows * caption) / (rows * cardHeight),
                ),
            )
            .coerceAtLeast(0.1f)

    /**
     * Where the first row's cards begin: the header just over them, and the two centred together on
     * the table, so a short grid doesn't leave its header stranded at the top.
     */
    val top: Float =
        header + (height - header - (rows * (cardHeight * scale + caption) + (rows - 1) * gap)) / 2f

    /** The middle of the [index]th card's cell. */
    fun cell(index: Int): Offset {
        val w = cardWidth * scale
        val h = cardHeight * scale
        val row = index / columns
        val column = index % columns
        val inRow = if (row == rows - 1) count - row * columns else columns
        val rowWidth = inRow * w + (inRow - 1) * gap
        val rowHeight = h + caption
        return Offset(
            (width - rowWidth) / 2f + w / 2f + column * (w + gap),
            top + h / 2f + row * (rowHeight + gap),
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
        // A socket the shape of the horseshoe that rests in it, a little bigger all round.
        val thickness = MagnetDimens.HorseshoeThickness.toPx()
        val socket = Stroke(thickness + 6.dp.toPx())
        for (magnet in state.magnets) {
            val slot = magnet.slot * density
            val legEnd = shapeHorseshoe(slot)
            drawPath(leftHalf, colors.stripWell, style = socket)
            drawPath(rightHalf, colors.stripWell, style = socket)
            val bend = MagnetDimens.HorseshoeWidth.toPx() / 2f - thickness / 2f
            val tip = MagnetDimens.HorseshoeTip.toPx() + 3.dp.toPx()
            for (x in floatArrayOf(slot.x - bend, slot.x + bend)) {
                drawRect(
                    colors.stripWell,
                    Offset(x - socket.width / 2f, legEnd),
                    Size(socket.width, tip),
                )
            }
        }
    }

    /** The cards: loose ones first, then the stuck, then a held one, each layer over the last. */
    fun DrawScope.drawCards(state: MagnetState, thumbs: List<ImageBitmap>, colors: AppColors) {
        state.frame
        val fanning = state.fanShown != null && state.fan > 0f
        val bodies = state.bodies.bodies
        for (layer in 0..2) {
            for (i in bodies.indices) {
                val body = bodies[i]
                if (body.layer() != layer) continue
                // Drawn by the grid instead, over its scrim.
                if (fanning && state.inFan(i)) continue
                // Shaded while a magnet is out that doesn't pull it, so the matches stand out.
                val shade =
                    if (layer == 0 && state.dim > 0f && !body.feelsAny(state.magnets)) {
                        MagnetDimens.DimShade * state.dim
                    } else {
                        0f
                    }
                drawCard(thumbs[i], body.at * density, body.tilt, 1f, colors, shade)
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
        for (i in state.magnets.indices) {
            val magnet = state.magnets[i]
            val centre = magnet.at * density
            val lift = if (magnet.held) MagnetDimens.HeldLift else 1f
            val face = state.tags[i].color.takeOrElse { colors.magnetRed }
            drawHorseshoe(centre, lift, face, colors, if (magnet.held) 6.dp.toPx() else 3.dp.toPx())
            val above = MagnetDimens.HorseshoeTop.toPx() * lift
            val below = (MagnetDimens.HorseshoeLegEnd + MagnetDimens.HorseshoeTip).toPx() * lift
            val label = labels[i]
            val padX = MagnetDimens.BadgePadX.toPx()
            val padY = MagnetDimens.BadgePadY.toPx()
            val labelTop =
                Offset(
                    centre.x - label.size.width / 2f,
                    centre.y + below + MagnetDimens.LabelGap.toPx(),
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
                Offset(
                    centre.x - w / 2f,
                    max(0f, centre.y - above - MagnetDimens.BadgeGap.toPx() - h),
                )
            drawRoundRect(colors.badge, topLeft, Size(w, h), CornerRadius(h / 2f))
            // Ringed in its magnet's colour, so the count reads as that magnet's.
            drawRoundRect(
                face,
                topLeft,
                Size(w, h),
                CornerRadius(h / 2f),
                style = Stroke(MagnetDimens.BadgeRing.toPx()),
            )
            drawText(badge, topLeft = topLeft + Offset(padX, padY))
        }
    }

    // The two halves of a horseshoe, made again for each magnet drawn.
    private val leftHalf = Path()
    private val rightHalf = Path()

    // Lays the two halves of a horseshoe over [centre] into the paths, along the middle of its
    // bar, and says where its legs end and its tips begin.
    private fun DrawScope.shapeHorseshoe(centre: Offset): Float {
        val thickness = MagnetDimens.HorseshoeThickness.toPx()
        val bend = MagnetDimens.HorseshoeWidth.toPx() / 2f - thickness / 2f
        val archY = centre.y - MagnetDimens.HorseshoeTop.toPx() + thickness / 2f + bend
        val legEnd = centre.y + MagnetDimens.HorseshoeLegEnd.toPx()
        val arch = Rect(centre.x - bend, archY - bend, centre.x + bend, archY + bend)
        leftHalf.rewind()
        leftHalf.moveTo(centre.x - bend, legEnd)
        leftHalf.lineTo(centre.x - bend, archY)
        leftHalf.arcTo(arch, 180f, 90f, false)
        rightHalf.rewind()
        rightHalf.moveTo(centre.x, archY - bend)
        rightHalf.arcTo(arch, 270f, 90f, false)
        rightHalf.lineTo(centre.x + bend, legEnd)
        return legEnd
    }

    // A horseshoe magnet standing over [centre]: a thick U whose left half wears [face] and right
    // half a deeper shade of it, as the classic magnet's two poles do, with steel tips at the foot
    // of its legs, a glint along its arch, and its shadow under it, [drop] down. Held, it lifts.
    private fun DrawScope.drawHorseshoe(
        centre: Offset,
        lift: Float,
        face: Color,
        colors: AppColors,
        drop: Float,
    ) {
        val thickness = MagnetDimens.HorseshoeThickness.toPx()
        val bend = MagnetDimens.HorseshoeWidth.toPx() / 2f - thickness / 2f
        val archY = centre.y - MagnetDimens.HorseshoeTop.toPx() + thickness / 2f + bend
        val tip = MagnetDimens.HorseshoeTip.toPx()
        val legEnd = shapeHorseshoe(centre)
        val bar = Stroke(thickness)
        scale(lift, pivot = centre) {
            translate(drop / 2f, drop) {
                drawPath(leftHalf, colors.cardShadow, style = bar)
                drawPath(rightHalf, colors.cardShadow, style = bar)
                for (x in floatArrayOf(centre.x - bend, centre.x + bend)) {
                    drawRect(
                        colors.cardShadow,
                        Offset(x - thickness / 2f, legEnd),
                        Size(thickness, tip),
                    )
                }
            }
            drawPath(leftHalf, face, style = bar)
            drawPath(rightHalf, lerp(face, Color.Black, PoleShade), style = bar)
            for (x in floatArrayOf(centre.x - bend, centre.x + bend)) {
                drawRect(
                    colors.magnetSteel,
                    Offset(x - thickness / 2f, legEnd),
                    Size(thickness, tip),
                )
                drawLine(
                    colors.clipSheen,
                    Offset(x - thickness / 2f, legEnd + 1.dp.toPx()),
                    Offset(x + thickness / 2f, legEnd + 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            // Light along the outside of the arch, where it catches the room.
            val glint = bend + thickness / 2f - 3.dp.toPx()
            drawArc(
                colors.magnetSheen,
                startAngle = 200f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(centre.x - glint, archY - glint),
                size = Size(glint * 2f, glint * 2f),
                alpha = 0.4f,
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }

    /** A magnet's photos, fanned out of its cluster into a grid over a scrim, or folding back. */
    fun DrawScope.drawFan(
        state: MagnetState,
        thumbs: List<ImageBitmap>,
        titles: List<TextLayoutResult>,
        header: TextLayoutResult?,
        colors: AppColors,
    ) {
        state.frame
        val tag = state.fanShown ?: return
        val t = smooth(state.fan)
        if (t <= 0f) return
        val tableBottom = state.tableHeight * density
        drawRect(colors.fanScrim, size = Size(size.width, tableBottom), alpha = 0.7f * t)
        val scale = lerp(1f, state.fanScale(), t)
        val captionGap = MagnetDimens.FanCaptionGap.toPx()
        for (i in state.bodies.bodies.indices) {
            if (!state.inFan(i)) continue
            val body = state.bodies.bodies[i]
            val centre = state.photoCentre(i) * density
            drawCard(thumbs[i], centre, lerp(body.tilt, 0f, t), scale, colors)
            // Its title under it, coming in with the grid.
            val title = titles[i]
            val below = centre.y + MagnetDimens.CardHeight.toPx() * scale / 2f + captionGap
            drawText(title, topLeft = Offset(centre.x - title.size.width / 2f, below), alpha = t)
        }
        // Over the grid, whose photos these are and how many: the magnet's badge is under it.
        if (header != null) {
            val face =
                state.tags.firstOrNull { it.id == tag }?.color?.takeOrElse { colors.magnetRed }
                    ?: colors.magnetRed
            val padX = MagnetDimens.BadgePadX.toPx()
            val padY = MagnetDimens.BadgePadY.toPx()
            val w = header.size.width + padX * 2f
            val h = header.size.height + padY * 2f
            val headerRoom = MagnetDimens.FanHeader.toPx()
            val gridTop = state.fanTop() * density
            val top = Offset((size.width - w) / 2f, gridTop - headerRoom + (headerRoom - h) / 2f)
            drawRoundRect(colors.badge, top, Size(w, h), CornerRadius(h / 2f), alpha = t)
            drawRoundRect(
                face,
                top,
                Size(w, h),
                CornerRadius(h / 2f),
                alpha = t,
                style = Stroke(MagnetDimens.BadgeRing.toPx()),
            )
            drawText(header, topLeft = top + Offset(padX, padY), alpha = t)
        }
    }

    private fun DrawScope.drawCard(
        image: ImageBitmap,
        centre: Offset,
        tilt: Float,
        scale: Float,
        colors: AppColors,
        shade: Float = 0f,
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
                if (shade > 0f) {
                    drawRoundRect(
                        colors.fanScrim,
                        Offset(-w / 2f, -h / 2f),
                        Size(w, h),
                        corner,
                        alpha = shade,
                    )
                }
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
internal fun smooth(t: Float): Float = t * t * (3f - 2f * t)

// How much deeper the horseshoe's second pole is than its first.
private const val PoleShade = 0.3f

private const val DegreesPerRadian = 57.29578f

/**
 * Draws a photo opened from the grid: grown out of its cell into a large rounded photo, the whole
 * picture rather than the card's crop, over a deeper scrim, with its [title] and [caption] under
 * it. Drawn in the table's own layer, as the grid is.
 */
internal class OpenPainter {
    private val clip = Path()

    fun DrawScope.drawOpen(
        state: MagnetState,
        images: List<ImageBitmap>,
        title: TextLayoutResult?,
        caption: TextLayoutResult?,
        colors: AppColors,
    ) {
        state.frame
        val index = state.openIndex
        if (index < 0) return
        val t = smooth(state.openness)
        if (t <= 0f) return
        val image = images[index]
        val tableBottom = state.tableHeight * density
        drawRect(colors.fanScrim, size = Size(size.width, tableBottom), alpha = 0.85f * t)
        // From its card in the grid...
        val scale = state.fanScale()
        val from = state.photoCentre(index) * density
        val fromW = MagnetDimens.CardWidth.toPx() * scale
        val fromH = MagnetDimens.CardHeight.toPx() * scale
        // ...to the whole photo, as wide as the table allows and as tall as its shape asks, with
        // room under it for its words.
        val words =
            (title?.size?.height ?: 0) +
                (caption?.size?.height ?: 0) +
                MagnetDimens.OpenTextGap.toPx() * 2f
        val margin = MagnetDimens.OpenMargin.toPx()
        val aspect = image.height.toFloat() / max(1, image.width)
        var toW = size.width - margin * 2f
        var toH = toW * aspect
        val room = tableBottom - margin * 2f - words
        if (toH > room) {
            toH = max(1f, room)
            toW = toH / aspect
        }
        val toTop = (tableBottom - toH - words) / 2f
        val to = Offset(size.width / 2f, toTop + toH / 2f)
        val centre = lerp(from, to, t)
        val w = lerp(fromW, toW, t)
        val h = lerp(fromH, toH, t)
        val corner = lerp(MagnetDimens.CardCorner.toPx() * scale, MagnetDimens.OpenCorner.toPx(), t)
        val topLeft = Offset(centre.x - w / 2f, centre.y - h / 2f)
        clip.rewind()
        clip.addRoundRect(
            RoundRect(topLeft.x, topLeft.y, topLeft.x + w, topLeft.y + h, CornerRadius(corner))
        )
        clipPath(clip) {
            // Cropped to the shape it has on its way, so it never stretches.
            val cropScale = max(w / image.width, h / image.height)
            val cropW = (w / cropScale).roundToInt().coerceIn(1, image.width)
            val cropH = (h / cropScale).roundToInt().coerceIn(1, image.height)
            drawImage(
                image,
                srcOffset = IntOffset((image.width - cropW) / 2, (image.height - cropH) / 2),
                srcSize = IntSize(cropW, cropH),
                dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                dstSize = IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.Medium,
            )
        }
        // Its words fade in under it once it has nearly arrived.
        val words01 = ((t - 0.6f) / 0.4f).coerceIn(0f, 1f)
        var y = toTop + toH + MagnetDimens.OpenTextGap.toPx()
        for (text in listOf(title, caption)) {
            if (text == null) continue
            drawText(
                text,
                topLeft = Offset((size.width - text.size.width) / 2f, y),
                alpha = words01,
            )
            y += text.size.height + MagnetDimens.OpenTextGap.toPx() / 2f
        }
    }
}
