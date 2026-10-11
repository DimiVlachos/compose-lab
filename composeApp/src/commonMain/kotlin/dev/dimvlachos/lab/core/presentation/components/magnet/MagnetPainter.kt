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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
 * Where a magnet's [count] photos go when they fan out: a grid, three columns across or more on a
 * table too short for three, centred on a [width] × [height] table under a [header], its last row
 * centred too, with room for a [caption] under each card. The cards grow to at most FanScale times
 * their size, less if that wouldn't fit, and keep [gap] apart and from the edges.
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
    private val columns =
        fanColumns(count, width, height, cardWidth, cardHeight, gap, caption, header)
    private val rows = ((count + columns - 1) / columns).coerceAtLeast(1)

    val scale: Float =
        fanScale(columns, count, width, height, cardWidth, cardHeight, gap, caption, header)
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
 * How many columns a grid of [count] cards takes on a [width] × [height] table: up to FanColumns,
 * or, where the cards would come out bigger in a wider grid, as on a short wide table, that one.
 */
internal fun fanColumns(
    count: Int,
    width: Float,
    height: Float,
    cardWidth: Float,
    cardHeight: Float,
    gap: Float,
    caption: Float = 0f,
    header: Float = 0f,
): Int {
    var best = count.coerceIn(1, MagnetDimens.FanColumns)
    var bestScale =
        fanScale(best, count, width, height, cardWidth, cardHeight, gap, caption, header)
    for (columns in MagnetDimens.FanColumns + 1..count) {
        val scale =
            fanScale(columns, count, width, height, cardWidth, cardHeight, gap, caption, header)
        if (scale > bestScale) {
            best = columns
            bestScale = scale
        }
    }
    return best
}

/** How much [count] cards grow in a grid [columns] across that fits a [width] × [height] table. */
internal fun fanScale(
    columns: Int,
    count: Int,
    width: Float,
    height: Float,
    cardWidth: Float,
    cardHeight: Float,
    gap: Float,
    caption: Float,
    header: Float,
): Float {
    val rows = ((count + columns - 1) / columns).coerceAtLeast(1)
    return min(
        MagnetDimens.FanScale,
        min(
            (width - gap * (columns + 1)) / (columns * cardWidth),
            (height - header - gap * (rows + 1) - rows * caption) / (rows * cardHeight),
        ),
    )
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
        // A nail for each magnet to hang from, driven into the rail.
        for (magnet in state.magnets) {
            drawNailHead(magnet.slot * density - Offset(0f, MagnetDimens.NailDrop.toPx()), colors)
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
                val shade = MagnetDimens.DimShade * body.shade
                drawCard(thumbs[i], body.at * density, body.tilt, 1f, body.lift, colors, shade)
            }
        }
    }

    /**
     * The magnets and the grid, in the order that keeps them whole: the grid over the magnets as it
     * fans out and while it is out, but under them as it folds back, so its photos land under the
     * magnets they return to instead of covering them and then dropping under. Folding, the magnets
     * are shaded as the scrim fading over the rest is, so they don't flash out from under it.
     */
    fun DrawScope.drawMagnetsAndFan(
        state: MagnetState,
        labels: List<TextLayoutResult>,
        badges: List<TextLayoutResult?>,
        thumbs: List<ImageBitmap>,
        titles: List<TextLayoutResult>,
        header: TextLayoutResult?,
        colors: AppColors,
    ) {
        val folding = state.fanShown != null && state.fannedOut == null && state.fan > 0f
        if (!folding) {
            drawMagnets(state, labels, badges, colors)
            drawFan(state, thumbs, titles, header, colors)
            return
        }
        drawFan(state, thumbs, titles, header, colors)
        val scrim = ScrimAlpha * smooth(state.fan)
        val light = 1f - scrim
        // On the dark table the scrim all but darkens them; on the light one it pales them, so
        // they take on as much of its colour as it lays over the rest.
        val wash =
            if (colors.isDark) Color.Black
            else
                Color(
                    colors.fanScrim.red * scrim,
                    colors.fanScrim.green * scrim,
                    colors.fanScrim.blue * scrim,
                )
        drawIntoCanvas { canvas ->
            canvas.saveLayer(
                Rect(Offset.Zero, size),
                Paint().apply {
                    colorFilter = ColorFilter.lighting(Color(light, light, light), wash)
                },
            )
            drawMagnets(state, labels, badges, colors)
            canvas.restore()
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
            val nail = centre - Offset(0f, MagnetDimens.NailDrop.toPx())
            // Hanging from its nail, it swings about it; the nail's head sits over the arch.
            rotate(if (magnet.out) 0f else magnet.hang.angle * DegreesPerRadian, pivot = nail) {
                drawHorseshoe(
                    centre,
                    lift,
                    face,
                    colors,
                    if (magnet.held) 6.dp.toPx() else 3.dp.toPx(),
                )
            }
            if (!magnet.out && (magnet.slot - magnet.at).getDistance() < 1f)
                drawNailHead(nail, colors)
            val above = MagnetDimens.HorseshoeTop.toPx() * lift
            val below = (MagnetDimens.HorseshoeLegEnd + MagnetDimens.HorseshoeTip).toPx() * lift
            val label = labels[i]
            val padX = MagnetDimens.BadgePadX.toPx()
            val padY = MagnetDimens.BadgePadY.toPx()
            val labelTop =
                Offset(
                    leftAcross(centre.x, label.size.width.toFloat(), size.width),
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
            // Joined, the two are one search with one count, shown once, over the pair.
            var over = centre
            if (state.joined) {
                val partner =
                    state.magnets.withIndex().firstOrNull { it.index != i && it.value.out }
                if (partner != null) {
                    if (partner.index < i) continue
                    over = (centre + partner.value.at * density) / 2f
                }
            }
            val w = badge.size.width + padX * 2f
            val h = badge.size.height + padY * 2f
            // Kept on the table by a magnet at its top edge.
            val topLeft =
                Offset(
                    leftAcross(over.x, w, size.width),
                    max(0f, over.y - above - MagnetDimens.BadgeGap.toPx() - h),
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

    // The strokes a horseshoe's shadow is drawn with, one a pass, kept while they stay as wide.
    private val shadowStrokes = arrayOfNulls<Stroke>(MagnetShadowShares.size)

    private fun shadowStroke(pass: Int, width: Float): Stroke {
        val kept = shadowStrokes[pass]
        if (kept != null && kept.width == width) return kept
        return Stroke(width).also { shadowStrokes[pass] = it }
    }

    // The two halves of a horseshoe, made again for each magnet drawn.
    private val leftHalf = Path()
    private val rightHalf = Path()

    // A nail's head at [at], with its shadow on the rail and a glint on its dome.
    private fun DrawScope.drawNailHead(at: Offset, colors: AppColors) {
        val r = MagnetDimens.NailRadius.toPx()
        // A soft shadow: a faint wide disc, and a darker one close under the head.
        drawCircle(colors.cardShadow, r * 1.35f, at + Offset(r * 0.3f, r * 0.7f), alpha = 0.4f)
        drawCircle(colors.cardShadow, r * 1.05f, at + Offset(r * 0.2f, r * 0.45f), alpha = 0.7f)
        drawCircle(colors.magnetSteel, r, at)
        drawCircle(colors.clipSheen, r * 0.4f, at - Offset(r * 0.3f, r * 0.3f))
    }

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
            // Its shadow softened by drawing it a few times, each wider and fainter, so its edge
            // fades out instead of stopping: further for a magnet held up off the table.
            val soft = drop * MagnetSoftness
            translate(drop * CardShadow.Slant * 2f, drop) {
                for (pass in MagnetShadowShares.indices) {
                    val grow = soft * pass
                    val share =
                        colors.cardShadow.copy(
                            alpha = colors.cardShadow.alpha * MagnetShadowShares[pass]
                        )
                    val stroke = shadowStroke(pass, thickness + grow)
                    drawPath(leftHalf, share, style = stroke)
                    drawPath(rightHalf, share, style = stroke)
                    for (x in floatArrayOf(centre.x - bend, centre.x + bend)) {
                        drawRect(
                            share,
                            Offset(x - (thickness + grow) / 2f, legEnd),
                            Size(thickness + grow, tip + grow / 2f),
                        )
                    }
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
        val tableBottom = state.overlayHeight * density
        drawRect(colors.fanScrim, size = Size(size.width, tableBottom), alpha = ScrimAlpha * t)
        val scale = lerp(1f, state.fanScale(), t)
        val captionGap = MagnetDimens.FanCaptionGap.toPx()
        for (i in state.bodies.bodies.indices) {
            if (!state.inFan(i)) continue
            // The photo opened from this card is drawn instead of it.
            if (i == state.openIndex && state.openness > 0f) continue
            val body = state.bodies.bodies[i]
            val centre = state.photoCentre(i) * density
            val lift = lerp(body.lift, FanLift, t)
            drawCard(thumbs[i], centre, lerp(body.tilt, 0f, t), scale, lift, colors)
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
            // It comes in last and goes first, so it never lingers over the magnets as they fold.
            val shown = t * t
            drawRoundRect(colors.badge, top, Size(w, h), CornerRadius(h / 2f), alpha = shown)
            drawRoundRect(
                face,
                top,
                Size(w, h),
                CornerRadius(h / 2f),
                alpha = shown,
                style = Stroke(MagnetDimens.BadgeRing.toPx()),
            )
            drawText(header, topLeft = top + Offset(padX, padY), alpha = shown)
        }
    }

    private val shadows = SoftShadow()

    private fun DrawScope.drawCard(
        image: ImageBitmap,
        centre: Offset,
        tilt: Float,
        scale: Float,
        lift: Float,
        colors: AppColors,
        shade: Float = 0f,
    ) {
        val w = MagnetDimens.CardWidth.toPx() * scale
        val h = MagnetDimens.CardHeight.toPx() * scale
        val corner = CornerRadius(MagnetDimens.CardCorner.toPx() * scale)
        val border = MagnetDimens.CardBorder.toPx() * scale
        drawCardShadow(shadows, centre, w, h, tilt, scale, lift, colors)
        translate(centre.x, centre.y) {
            rotate(tilt * DegreesPerRadian, pivot = Offset.Zero) {
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
                // Shaded over all of it, its clip too.
                if (shade > 0f) {
                    drawRoundRect(
                        colors.fanScrim,
                        Offset(-w / 2f, -h / 2f),
                        Size(w, h),
                        corner,
                        alpha = shade,
                    )
                    drawRoundRect(
                        colors.fanScrim,
                        clipTop,
                        Size(clipW, clipH),
                        CornerRadius(clipH / 3f),
                        alpha = shade,
                    )
                }
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

/**
 * Where the left edge of something [width] wide goes to be centred on [centre], kept on a stage
 * [stage] wide: a magnet's label and badge, in a corner, stay whole on the screen.
 */
internal fun leftAcross(centre: Float, width: Float, stage: Float): Float =
    (centre - width / 2f).coerceIn(0f, max(0f, stage - width))

// Eased in and out, so the grid starts and lands softly.
internal fun smooth(t: Float): Float = t * t * (3f - 2f * t)

/**
 * How strong the card's shadow is under a photo opening from it, by how far it has opened: the
 * card's own at the start, fading out as the photo grows, gone once it is fully open.
 */
internal fun openShadowAlpha(openness: Float): Float = (1f - openness).coerceIn(0f, 1f)

/**
 * How much of the card's paper border a photo opening from it still shows, by how far it has
 * opened: all of it at the start, thinning to none once it is fully open.
 */
internal fun openBorder(openness: Float): Float = (1f - openness).coerceIn(0f, 1f)

/**
 * How strongly the card's clip shows on a photo opening from it: fully at the start, gone once it
 * is OpenClipGone of the way open, so the clip comes off quickly as the photo lifts.
 */
internal fun openClipAlpha(openness: Float): Float = (1f - openness / OpenClipGone).coerceIn(0f, 1f)

private const val OpenClipGone = 0.4f

/**
 * The soft shadow of a card [w] × [h] px whose middle is at [centre], turned by [tilt], at [scale]
 * times its own size and [lift]ed off the table as far as [CardShadow] says, at [alpha] of its full
 * strength: the wide shadow first, then the contact one over it.
 */
internal fun DrawScope.drawCardShadow(
    shadows: SoftShadow,
    centre: Offset,
    w: Float,
    h: Float,
    tilt: Float,
    scale: Float,
    lift: Float,
    colors: AppColors,
    alpha: Float = 1f,
) {
    val strength = colors.cardShadow.alpha * alpha
    val unit = density * scale
    for (layer in 0..1) {
        val ambient = layer == 0
        val blur = if (ambient) CardShadow.ambientBlur(lift) else CardShadow.contactBlur(lift)
        val drop = if (ambient) CardShadow.ambientDrop(lift) else CardShadow.contactDrop(lift)
        val share = if (ambient) CardShadow.ambientAlpha(lift) else CardShadow.contactAlpha(lift)
        val spread = if (ambient) CardShadow.ambientSpread(lift) * unit else 0f
        // Cast down from the light, then turned with the card about its own middle.
        translate(centre.x + drop * CardShadow.Slant * unit, centre.y + drop * unit) {
            rotate(tilt * DegreesPerRadian, pivot = Offset.Zero) {
                with(shadows) {
                    drawSoftShadow(
                        -w / 2f - spread,
                        -h / 2f - spread,
                        w / 2f + spread,
                        h / 2f + spread,
                        blur * unit,
                        strength * share,
                    )
                }
            }
        }
    }
}

// A horseshoe's shadow is drawn in passes, each this share of the card shadow's strength and
// MagnetSoftness of its drop wider than the last.
private val MagnetShadowShares = floatArrayOf(0.45f, 0.3f, 0.2f, 0.12f)
private const val MagnetSoftness = 0.9f

// How far a card in the grid, or one opening from it, is lifted for its shadow.
private const val FanLift = 0.5f

// How dark the scrim behind a fanned-out grid is, fully out.
private const val ScrimAlpha = 0.7f

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
    private val shadows = SoftShadow()

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
        val tableBottom = state.overlayHeight * density
        drawRect(colors.fanScrim, size = Size(size.width, tableBottom), alpha = 0.85f * t)
        // From its card in the grid...
        // The card it grows from is as big as the grid draws it, folding or not.
        val scale = lerp(1f, state.fanScale(), smooth(state.fan))
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
        // Short and wide, it keeps less room over and under it, so it opens as big as it can.
        val marginY = if (state.wide) MagnetDimens.OpenTextGap.toPx() else margin
        val aspect = image.height.toFloat() / max(1, image.width)
        var toW = size.width - margin * 2f
        var toH = toW * aspect
        val room = tableBottom - marginY * 2f - words
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
        state.openedBounds = Rect(topLeft, Size(w, h))
        // The card's shadow comes along, lifting and fading out as the photo grows, so it does not
        // drop away on the first frame, nor come back all at once as the photo lands on its card.
        val shadowAlpha = openShadowAlpha(t)
        if (shadowAlpha > 0f) {
            drawCardShadow(
                shadows,
                centre,
                w,
                h,
                0f,
                1f,
                lerp(FanLift, 1f, t),
                colors,
                alpha = shadowAlpha,
            )
        }
        // The card's paper border thins into the growing photo, so the picture grows out of the
        // print rather than jumping to its edges, and comes back as it closes.
        val border = MagnetDimens.CardBorder.toPx() * scale * openBorder(t)
        if (border > 0f) {
            drawRoundRect(colors.cardPaper, topLeft, Size(w, h), CornerRadius(corner))
        }
        val inner = max(0f, corner - border)
        clip.rewind()
        clip.addRoundRect(
            RoundRect(
                topLeft.x + border,
                topLeft.y + border,
                topLeft.x + w - border,
                topLeft.y + h - border,
                CornerRadius(inner),
            )
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
        // Its clip comes off as it starts to open, and back on as it lands on its card again.
        val clipAlpha = openClipAlpha(t)
        if (clipAlpha > 0f) {
            val clipW = MagnetDimens.ClipWidth.toPx() * scale
            val clipH = MagnetDimens.ClipHeight.toPx() * scale
            val clipTop = Offset(centre.x - clipW / 2f, topLeft.y - clipH / 2f)
            drawRoundRect(
                colors.clipMetal,
                clipTop,
                Size(clipW, clipH),
                CornerRadius(clipH / 3f),
                alpha = clipAlpha,
            )
            drawLine(
                colors.clipSheen,
                clipTop + Offset(clipW * 0.2f, clipH * 0.3f),
                clipTop + Offset(clipW * 0.8f, clipH * 0.3f),
                strokeWidth = 1.dp.toPx() * scale,
                alpha = clipAlpha,
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
