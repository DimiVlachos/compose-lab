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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

// Colour stops per strip for the rest shade.
private const val RestShadeStops = 5

// How strongly each paper edge in a stack shows, in a repeating pattern.
private val StackLineAlphas = floatArrayOf(0.55f, 0.3f, 0.75f, 0.4f, 0.65f)

/** The colours the book is drawn in. */
internal class BookInk(
    val shade: Color,
    val glare: Color,
    val gutter: Color,
    val thread: Color,
    val crease: Color,
    val cover: Color,
    val coverLit: Color,
    val coverRule: Color,
    val coverSpine: Color,
    val pageEdge: Color,
    val pageEdgeLine: Color,
)

/**
 * The book's size against its pages: a page is square (a spread is 2:1), the stacks sit beside the
 * outer edges and the boards reach past them all round. Everything is a share of a page's width, so
 * the whole book is this wide for its height.
 */
internal val BookAspect: Float =
    (2f + 2f * (PageTurnDimens.CoverMarginXFraction + PageTurnDimens.StackFraction)) /
        (1f + 2f * PageTurnDimens.CoverMarginYFraction)

/**
 * Draws the open book: hardcover boards, a stack of paper under each side and the open spread on
 * top, each of [spreads] one image across both pages. Every page is a chain of strips: at rest the
 * two lie in the open book's curve, and mid-turn the pages either side of the leaf lie under it
 * while the leaf crosses between them, the right half of one spread on its front and the left half
 * of the next on its back.
 */
internal class BookPainter(private val ink: BookInk) {
    private val matrix = Matrix()
    private val edgeClip = Path()
    private val stackClip = Path()
    private var restWidth = -1f
    private lateinit var restRight: TurnFrame
    private lateinit var restLeft: TurnFrame

    fun DrawScope.drawBook(spreads: List<ImageBitmap>, state: PageTurnState) {
        val page =
            size.width /
                (2f + 2f * (PageTurnDimens.CoverMarginXFraction + PageTurnDimens.StackFraction))
        val side = page * (PageTurnDimens.CoverMarginXFraction + PageTurnDimens.StackFraction)
        val top = page * PageTurnDimens.CoverMarginYFraction
        drawCovers(page)
        val pair = state.pair
        val t = state.leafProgress.coerceIn(0f, 1f)
        val position = if (pair != null) pair.leaf + t else state.spread.toFloat()
        val (leftShare, rightShare) = stackShares(position, spreads.size)
        inset(left = side, top = top, right = side, bottom = top) {
            drawStacks(page * PageTurnDimens.StackFraction, leftShare, rightShare)
            drawPages(spreads, state, pair, t, page)
        }
    }

    private fun DrawScope.drawPages(
        spreads: List<ImageBitmap>,
        state: PageTurnState,
        pair: TurnPair?,
        t: Float,
        page: Float,
    ) {
        val spineX = size.width / 2f
        val corner = CornerRadius(PageTurnDimens.CornerFraction * size.height)
        val perspectivePx = PageTurnDimens.Perspective.toPx()
        val originY = size.height * PageTurnDimens.OriginYFraction
        if (page != restWidth) {
            restRight = turnFrame(0f, leafWidth = page)
            restLeft = turnFrame(1f, leafWidth = page)
            restWidth = page
        }
        fun leaf(frame: TurnFrame, front: ImageBitmap, back: ImageBitmap) {
            for (i in 0 until PageTurnDimens.Strips) {
                drawStrip(frame, i, front, back, spineX, originY, perspectivePx, corner)
            }
        }
        if (pair == null) {
            val open = spreads[state.spread]
            leaf(restLeft, open, open)
            leaf(restRight, open, open)
            drawStitches(spineX, alpha = 1f)
            drawCrease(spineX)
            return
        }
        val here = spreads[pair.leaf]
        val next = spreads[pair.leaf + 1]
        val frame = turnFrame(t, leafWidth = page, bendDirection = state.bendDirection)
        leaf(restLeft, here, here)
        leaf(restRight, next, next)
        drawGutterShades(spineX, page, frame.lift * frame.lift)
        val over = stitchesOverLeaf(t)
        if (over < 1f) drawStitches(spineX, alpha = 1f - over)
        leaf(frame, here, next)
        if (over > 0f) drawStitches(spineX, alpha = over)
        drawCrease(spineX)
    }

    // Two boards meeting at a darker spine, lit a little from above, with a gold line tooled in
    // around each; the pages cover most of it.
    private fun DrawScope.drawCovers(page: Float) {
        val radius = CornerRadius(page * PageTurnDimens.CoverCornerFraction)
        val leather = Brush.verticalGradient(listOf(ink.coverLit, ink.cover))
        drawRoundRect(leather, cornerRadius = radius)
        val spine = page * PageTurnDimens.CoverMarginXFraction
        drawRect(
            Brush.horizontalGradient(
                listOf(ink.cover, ink.coverSpine, ink.cover),
                startX = size.width / 2f - spine,
                endX = size.width / 2f + spine,
            ),
            topLeft = Offset(size.width / 2f - spine, 0f),
            size = Size(spine * 2f, size.height),
        )
        val ruleInset =
            page * PageTurnDimens.CoverMarginXFraction * PageTurnDimens.CoverRuleInsetFraction
        drawRoundRect(
            ink.coverRule.copy(alpha = 0.7f),
            topLeft = Offset(ruleInset, ruleInset),
            size = Size(size.width - ruleInset * 2f, size.height - ruleInset * 2f),
            cornerRadius = radius,
            style = Stroke(PageTurnDimens.CoverRuleWidth.toPx()),
        )
    }

    /**
     * The paper under each page, showing past its outer edge as fine lines of page edges, darker
     * towards the boards; [left] and [right] say how thick each side is, as shares of [thickest].
     */
    private fun DrawScope.drawStacks(thickest: Float, left: Float, right: Float) {
        drawStack(x = 0f, width = thickest * left, outwards = -1f)
        drawStack(x = size.width, width = thickest * right, outwards = 1f)
    }

    private fun DrawScope.drawStack(x: Float, width: Float, outwards: Float) {
        if (width <= 0f) return
        // Reaches a little under the page, so no gap shows where the page's curve lifts its edge.
        val under = width * 0.5f
        val near = x - outwards * under
        val far = x + outwards * width
        val left = minOf(near, far)
        val right = maxOf(near, far)
        val corner = CornerRadius(width * 0.6f)
        stackClip.rewind()
        stackClip.addRoundRect(
            RoundRect(
                left = left,
                top = 0f,
                right = right,
                bottom = size.height,
                topLeftCornerRadius = if (outwards < 0f) corner else CornerRadius.Zero,
                topRightCornerRadius = if (outwards > 0f) corner else CornerRadius.Zero,
                bottomRightCornerRadius = if (outwards > 0f) corner else CornerRadius.Zero,
                bottomLeftCornerRadius = if (outwards < 0f) corner else CornerRadius.Zero,
            )
        )
        clipPath(stackClip) {
            drawRect(
                ink.pageEdge,
                topLeft = Offset(left, 0f),
                size = Size(right - left, size.height),
            )
            val spacing = PageTurnDimens.StackLineSpacing.toPx()
            var line = 0
            var at = x + outwards * spacing * 0.5f
            while ((at - x) * outwards < width) {
                // Edges catch the light unevenly; a fixed pattern keeps it still from frame to
                // frame.
                val alpha = StackLineAlphas[line % StackLineAlphas.size]
                drawLine(
                    ink.pageEdgeLine.copy(alpha = alpha),
                    start = Offset(at, 0f),
                    end = Offset(at, size.height),
                    strokeWidth = 1f,
                )
                at += outwards * spacing
                line++
            }
            drawRect(
                Brush.horizontalGradient(
                    listOf(ink.shade.copy(alpha = 0f), ink.shade.copy(alpha = 0.35f)),
                    startX = x,
                    endX = far,
                ),
                topLeft = Offset(left, 0f),
                size = Size(right - left, size.height),
            )
        }
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
        // The seam reaches over the next strip out on the front and back over the last one on the
        // back, so it is the front's outer strip and the back's spine strip that have none.
        val seam = if ((facing && outer) || (!facing && index == 0)) 0f else PageTurnDimens.SeamPx
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
                        left = left,
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
                // The rest shade follows its curve across the strip in several stops: one straight
                // ramp per strip kinks at every edge, and the eye finds the kinks on a clear sky.
                val u0 = index.toFloat() / PageTurnDimens.Strips
                val du = 1f / PageTurnDimens.Strips
                if (
                    gutterRestShade(u0, frame.lift) > 0f ||
                        gutterRestShade(u0 + du, frame.lift) > 0f
                ) {
                    val stops =
                        Array(RestShadeStops) { k ->
                            val f = k.toFloat() / (RestShadeStops - 1)
                            f to ink.gutter.copy(alpha = gutterRestShade(u0 + f * du, frame.lift))
                        }
                    drawRect(
                        Brush.horizontalGradient(*stops, startX = 0f, endX = stripWidth),
                        topLeft = Offset(left, 0f),
                        size = Size(right - left, size.height),
                    )
                }
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
