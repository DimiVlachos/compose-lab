package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** The colours the book is drawn in. */
internal class BookInk(
    val shade: Color,
    val glare: Color,
    val gutter: Color,
    val thread: Color,
    val crease: Color,
)

/**
 * Draws the open book on a 2:1 canvas: each of [spreads] is one image across both pages. At rest
 * the open spread lies flat; mid-turn the pages either side of the leaf lie under it and the leaf
 * is drawn strip by strip, the right half of one spread on its front and the left half of the next
 * on its back.
 */
internal class BookPainter(private val ink: BookInk) {
    private val matrix = Matrix()
    private val bookClip = Path()
    private val edgeClip = Path()

    fun DrawScope.drawBook(spreads: List<ImageBitmap>, state: PageTurnState) {
        val pageWidth = size.width / 2f
        val spineX = pageWidth
        val corner = CornerRadius(PageTurnDimens.CornerFraction * size.height)
        bookClip.rewind()
        bookClip.addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner))

        val pair = state.pair
        if (pair == null) {
            clipPath(bookClip) { drawSpread(spreads[state.spread]) }
            drawStitches(spineX, alpha = 1f)
            drawCrease(spineX)
            return
        }
        val t = state.leafProgress.coerceIn(0f, 1f)
        val here = spreads[pair.leaf]
        val next = spreads[pair.leaf + 1]
        val frame = turnFrame(t, leafWidth = pageWidth, bendDirection = state.bendDirection)
        clipPath(bookClip) {
            drawHalf(here, rightHalf = false, atX = 0f)
            drawHalf(next, rightHalf = true, atX = spineX)
            drawGutterShades(spineX, pageWidth, frame.lift * frame.lift)
        }
        val over = stitchesOverLeaf(t)
        if (over < 1f) drawStitches(spineX, alpha = 1f - over)
        val perspectivePx = PageTurnDimens.Perspective.toPx()
        val originY = size.height * PageTurnDimens.OriginYFraction
        for (i in 0 until PageTurnDimens.Strips) {
            drawStrip(frame, i, front = here, back = next, spineX, originY, perspectivePx, corner)
        }
        if (over > 0f) drawStitches(spineX, alpha = over)
        drawCrease(spineX)
    }

    private fun DrawScope.drawSpread(image: ImageBitmap) {
        drawImage(
            image,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }

    private fun DrawScope.drawHalf(image: ImageBitmap, rightHalf: Boolean, atX: Float) {
        val half = image.width / 2
        drawImage(
            image,
            srcOffset = IntOffset(if (rightHalf) half else 0, 0),
            srcSize = IntSize(half, image.height),
            dstOffset = IntOffset(atX.roundToInt(), 0),
            dstSize = IntSize((size.width / 2f).roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }

    /**
     * One strip of the leaf, in its own space: (0, 0) at its hinge, [TurnFrame.stripWidth] wide and
     * the book's height tall, its outer edge always on the right. Facing the reader it shows the
     * front's slice as is; facing away the slice is mirrored, since the back is seen from behind.
     * Its image runs [PageTurnDimens.SeamPx] past its spine edge (back) or outer edge (front),
     * under the next strip or over the last one, so no gap opens between them.
     */
    private fun DrawScope.drawStrip(
        frame: TurnFrame,
        index: Int,
        front: ImageBitmap,
        back: ImageBitmap,
        spineX: Float,
        originY: Float,
        perspectivePx: Float,
        corner: CornerRadius,
    ) {
        val pose = frame.poses[index]
        val facing = stripFacesReader(pose)
        val image = if (facing) front else back
        val stripWidth = frame.stripWidth
        val outer = stripIsOuterEdge(index)
        val seam = if (outer) 0f else PageTurnDimens.SeamPx
        // In image pixels: a page is half the spread image, a slice a strip's share of it. Whole
        // pixels, as the flat pages use, so a strip starts exactly where its page lay.
        val half = image.width / 2
        val slice = half.toFloat() / PageTurnDimens.Strips
        val pageStart = if (facing) half else 0
        val srcWidth = (slice * stripSliceScale(stripWidth, seam)).roundToInt()
        // Kept inside its own page: the seam on the back's spine strip would reach the other half.
        val srcLeft =
            (pageStart + stripSliceStart(facing, index) * half)
                .roundToInt()
                .coerceIn(pageStart, (pageStart + half - srcWidth).coerceAtLeast(pageStart))
        // Where the image lands in the strip's space: the seam trails off the outer edge on the
        // front and off the spine edge on the back.
        val left = if (facing) 0f else -seam
        val right = stripWidth + if (facing) seam else 0f
        stripMatrix(pose, spineX, originY, perspectivePx, matrix)
        withTransform({ transform(matrix) }) {
            if (outer) {
                edgeClip.rewind()
                edgeClip.addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = stripWidth,
                        bottom = size.height,
                        topLeftCornerRadius = CornerRadius.Zero,
                        topRightCornerRadius = corner,
                        bottomRightCornerRadius = corner,
                        bottomLeftCornerRadius = CornerRadius.Zero,
                    )
                )
            }
            val draw: DrawScope.() -> Unit = {
                withTransform({
                    if (!facing) scale(-1f, 1f, pivot = Offset(stripWidth / 2f, 0f))
                    scale(stripWidth / slice, size.height / image.height, pivot = Offset.Zero)
                }) {
                    drawImage(
                        image,
                        srcOffset = IntOffset(srcLeft, 0),
                        srcSize = IntSize(srcWidth, image.height),
                        dstSize = IntSize(srcWidth, image.height),
                        filterQuality = FilterQuality.Medium,
                    )
                }
                val (shadeFrom, shadeTo) = stripShadeAlphas(frame, index)
                drawRect(
                    Brush.horizontalGradient(
                        listOf(ink.shade.copy(alpha = shadeFrom), ink.shade.copy(alpha = shadeTo)),
                        startX = 0f,
                        endX = stripWidth,
                    ),
                    topLeft = Offset(left, 0f),
                    size = Size(right - left, size.height),
                )
                val glare = stripGlareAlpha(frame, index)
                if (glare > 0f) {
                    drawRect(
                        ink.glare.copy(alpha = glare),
                        topLeft = Offset(left, 0f),
                        size = Size(right - left, size.height),
                    )
                }
            }
            if (outer) clipPath(edgeClip, block = draw) else draw()
        }
    }

    // The pages darken towards the spine while a leaf stands over them.
    private fun DrawScope.drawGutterShades(spineX: Float, pageWidth: Float, amount: Float) {
        if (amount <= 0f) return
        val width = pageWidth * PageTurnDimens.GutterWidthFraction
        val clear = ink.gutter.copy(alpha = 0f)
        drawRect(
            Brush.horizontalGradient(
                listOf(ink.gutter.copy(alpha = PageTurnDimens.GutterLeftAlpha * amount), clear),
                startX = spineX,
                endX = spineX - width,
            ),
            topLeft = Offset(spineX - width, 0f),
            size = Size(width, size.height),
        )
        drawRect(
            Brush.horizontalGradient(
                listOf(ink.gutter.copy(alpha = PageTurnDimens.GutterRightAlpha * amount), clear),
                startX = spineX,
                endX = spineX + width,
            ),
            topLeft = Offset(spineX, 0f),
            size = Size(width, size.height),
        )
    }

    private fun DrawScope.drawCrease(spineX: Float) {
        val width = size.width * PageTurnDimens.CreaseWidthFraction
        val clear = ink.crease.copy(alpha = 0f)
        drawRect(
            Brush.horizontalGradient(
                0f to clear,
                0.5f to ink.crease.copy(alpha = PageTurnDimens.CreaseAlpha),
                1f to clear,
                startX = spineX - width,
                endX = spineX + width,
            ),
            topLeft = Offset(spineX - width, 0f),
            size = Size(width * 2f, size.height),
        )
    }

    // Short threads down the spine, each on a softer, wider shadow of itself.
    private fun DrawScope.drawStitches(spineX: Float, alpha: Float) {
        val length = size.height * PageTurnDimens.StitchLengthFraction
        val underLength = length + PageTurnDimens.StitchUnderExtra.toPx()
        for (fraction in PageTurnDimens.StitchFractions) {
            val y = size.height * fraction
            drawLine(
                ink.shade.copy(alpha = PageTurnDimens.StitchShadowAlpha * alpha),
                start = Offset(spineX, y - underLength / 2f),
                end = Offset(spineX, y + underLength / 2f),
                strokeWidth = PageTurnDimens.StitchUnderWidth.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                ink.thread.copy(alpha = alpha),
                start = Offset(spineX, y - length / 2f),
                end = Offset(spineX, y + length / 2f),
                strokeWidth = PageTurnDimens.StitchWidth.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
