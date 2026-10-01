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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

// Colour stops per strip for the rest shade.
private const val RestShadeStops = 5

/** The colours the book is drawn in. */
internal class BookInk(
    val shade: Color,
    val glare: Color,
    val gutter: Color,
    val thread: Color,
    val crease: Color,
    val pageEdge: Color,
    val pageEdgeLine: Color,
)

/**
 * The book's size against its pages: a page is square (a spread is 2:1), the sheets fan out past
 * the outer edges and a little room is left all round for the pages' arch. Everything is a share of
 * a page's width, so the whole book is this wide for its height.
 */
internal val BookAspect: Float =
    (2f + 2f * (PageTurnDimens.EdgeRoomFraction + PageTurnDimens.StackFraction)) /
        (1f + 2f * PageTurnDimens.EdgeRoomFraction)

/**
 * Draws the open book: the sheets of paper under each side and the open spread on top, each of
 * [spreads] one image across both pages. Every page is a chain of strips: at rest the two lie in
 * the open book's curve, and mid-turn the pages either side of the leaf lie under it while the leaf
 * crosses between them, the right half of one spread on its front and the left half of the next on
 * its back.
 */
internal class BookPainter(private val ink: BookInk) {
    private val matrix = Matrix()
    private val edgeClip = Path()
    private val sheetPath = Path()
    private var restSize = Size.Unspecified
    private lateinit var restRight: TurnFrame
    private lateinit var restLeft: TurnFrame
    private lateinit var rightOutline: PageOutline
    private lateinit var leftOutline: PageOutline

    fun DrawScope.drawBook(spreads: List<ImageBitmap>, state: PageTurnState) {
        val page =
            size.width /
                (2f + 2f * (PageTurnDimens.EdgeRoomFraction + PageTurnDimens.StackFraction))
        val side = page * (PageTurnDimens.EdgeRoomFraction + PageTurnDimens.StackFraction)
        val top = page * PageTurnDimens.EdgeRoomFraction
        val pair = state.pair
        val t = state.leafProgress.coerceIn(0f, 1f)
        val position = if (pair != null) pair.leaf + t else state.spread.toFloat()
        val (leftShare, rightShare) = stackShares(position, spreads.size)
        inset(left = side, top = top, right = side, bottom = top) {
            restPages(page)
            drawSheets(leftOutline, leftShare, outwards = -1f, page)
            drawSheets(rightOutline, rightShare, outwards = 1f, page)
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

    // The two resting pages, and where their edges land on screen: fixed for a size, so worked out
    // once.
    private fun DrawScope.restPages(page: Float) {
        if (size == restSize) return
        val spineX = size.width / 2f
        val originY = size.height * PageTurnDimens.OriginYFraction
        val perspectivePx = PageTurnDimens.Perspective.toPx()
        restRight = turnFrame(0f, leafWidth = page)
        restLeft = turnFrame(1f, leafWidth = page)
        rightOutline = pageOutline(restRight, spineX, originY, perspectivePx, size.height)
        leftOutline = pageOutline(restLeft, spineX, originY, perspectivePx, size.height)
        restSize = size
    }

    /**
     * The sheets under a resting page: copies of its outline, each fanned a little further out from
     * the spine than the one above it, as the paper of an open book spreads. [share] of the most
     * sheets show; the deeper ones are darker, and each has its edge drawn so they read as layers.
     */
    private fun DrawScope.drawSheets(
        outline: PageOutline,
        share: Float,
        outwards: Float,
        page: Float,
    ) {
        val sheets = (PageTurnDimens.MaxSheets * share).roundToInt().coerceAtLeast(1)
        val step = page * PageTurnDimens.StackFraction / PageTurnDimens.MaxSheets
        val corner = PageTurnDimens.CornerFraction * size.height
        val edge = PageTurnDimens.SheetEdgeWidth.toPx()
        for (k in sheets downTo 1) {
            outline.fannedPath(sheetPath, fan = k * step * outwards, corner = corner, outwards)
            val depth = k.toFloat() / PageTurnDimens.MaxSheets
            drawPath(sheetPath, lerp(ink.pageEdge, ink.pageEdgeLine, depth * 0.45f))
            drawPath(
                sheetPath,
                ink.pageEdgeLine.copy(alpha = 0.55f + 0.3f * depth),
                style = Stroke(edge),
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

/**
 * A resting page's edges as drawn: its top and bottom edge, one point per strip boundary from the
 * spine out, through the strips' own projections, so they carry the page's curve and perspective.
 */
internal class PageOutline(private val top: Array<Offset>, private val bottom: Array<Offset>) {
    /**
     * Writes into [path] this outline fanned out by [fan] at the outer edge, nothing at the spine,
     * with its outer corners rounded by [corner]; [outwards] is the side the outer edge is on.
     */
    fun fannedPath(path: Path, fan: Float, corner: Float, outwards: Float) {
        val last = top.lastIndex
        fun fanned(point: Offset, index: Int) = Offset(point.x + fan * index / last, point.y)
        path.rewind()
        val start = fanned(top[0], 0)
        path.moveTo(start.x, start.y)
        for (i in 1 until last) fanned(top[i], i).let { path.lineTo(it.x, it.y) }
        val topCorner = fanned(top[last], last)
        val bottomCorner = fanned(bottom[last], last)
        path.lineTo(topCorner.x - outwards * corner, topCorner.y)
        path.quadraticTo(topCorner.x, topCorner.y, topCorner.x, topCorner.y + corner)
        path.lineTo(bottomCorner.x, bottomCorner.y - corner)
        path.quadraticTo(
            bottomCorner.x,
            bottomCorner.y,
            bottomCorner.x - outwards * corner,
            bottomCorner.y,
        )
        for (i in last - 1 downTo 0) fanned(bottom[i], i).let { path.lineTo(it.x, it.y) }
        path.close()
    }
}

internal fun pageOutline(
    frame: TurnFrame,
    spineX: Float,
    originY: Float,
    perspectivePx: Float,
    height: Float,
): PageOutline {
    val matrix = Matrix()
    val strips = frame.poses.size
    val top = Array(strips + 1) { Offset.Zero }
    val bottom = Array(strips + 1) { Offset.Zero }
    for (i in 0 until strips) {
        stripMatrix(frame.poses[i], spineX, originY, perspectivePx, matrix)
        top[i] = matrix.map(Offset(0f, 0f))
        bottom[i] = matrix.map(Offset(0f, height))
        if (i == strips - 1) {
            top[strips] = matrix.map(Offset(frame.stripWidth, 0f))
            bottom[strips] = matrix.map(Offset(frame.stripWidth, height))
        }
    }
    return PageOutline(top, bottom)
}
