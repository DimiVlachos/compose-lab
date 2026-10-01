package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
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

/** The colours the book is drawn in. */
internal class BookInk(
    val shade: Color,
    val glare: Color,
    val gutter: Color,
    val thread: Color,
    val threadTwist: Color,
    val crease: Color,
    val pageEdge: Color,
    val pageEdgeLine: Color,
)

/**
 * The book's size against its pages: a page is square (a spread is 2:1), the leaves under each side
 * fan out past the outer edges and a little room is left all round for the pages' arch. Everything
 * is a share of a page's width, so the whole book is this wide for its height.
 */
internal val BookAspect: Float =
    (2f + 2f * (PageTurnDimens.EdgeRoomFraction + PageTurnDimens.StackFraction)) /
        (1f + 2f * PageTurnDimens.EdgeRoomFraction)

/**
 * Draws the open book from its leaves. Each of [spreads] is one image across both pages, or blank
 * paper (null); leaf i carries the right half of spread i on its front and the left half of spread
 * i + 1 on its back. The leaves already turned lie on the left, the rest on the right, each one a
 * chain of strips in the open book's curve, flatter the deeper it lies, so their edges fan out.
 * Mid-turn the leaf in the air crosses between the stacks: the one it leaves rises to take its
 * place, and the one it lands on settles under it.
 */
internal class BookPainter(private val ink: BookInk) {
    private val matrix = Matrix()
    private val edgeClip = Path()
    private val sheetPath = Path()
    private val paper = paperPage(ink.pageEdge)

    fun DrawScope.drawBook(spreads: List<ImageBitmap?>, state: PageTurnState) {
        val page =
            size.width /
                (2f + 2f * (PageTurnDimens.EdgeRoomFraction + PageTurnDimens.StackFraction))
        val side = page * (PageTurnDimens.EdgeRoomFraction + PageTurnDimens.StackFraction)
        val top = page * PageTurnDimens.EdgeRoomFraction
        inset(left = side, top = top, right = side, bottom = top) {
            drawOpenBook(spreads, state, page)
        }
    }

    private fun DrawScope.drawOpenBook(
        spreads: List<ImageBitmap?>,
        state: PageTurnState,
        page: Float,
    ) {
        val spineX = size.width / 2f
        val geometry =
            PageGeometry(
                spineX = spineX,
                originY = size.height * PageTurnDimens.OriginYFraction,
                perspectivePx = PageTurnDimens.Perspective.toPx(),
                corner = CornerRadius(PageTurnDimens.CornerFraction * size.height),
                page = page,
            )
        fun image(spread: Int): ImageBitmap = spreads.getOrNull(spread) ?: paper
        val leaves = spreads.size - 1
        val flights = state.flights
        val flying = flights.mapTo(HashSet()) { it.leaf }
        // The leaves lying on each side, the ones in the air aside: the left holds those before the
        // open spread, the right the rest. The top of each is the page you see (-1 or [leaves]:
        // none, the page is the book's endpaper).
        val open = state.spread
        val leftTop = (open - 1 downTo 0).firstOrNull { it !in flying } ?: -1
        val rightTop = (open until leaves).firstOrNull { it !in flying } ?: leaves
        // How far each stack has sunk under the leaves above it: the one they leave rises as they
        // go, the one they land on settles as they come down.
        val leftSink = flights.fold(0f) { sum, it -> sum + stackLand(it.t) }
        val rightSink = flights.fold(0f) { sum, it -> sum + 1f - stackRise(it.t) }

        // The sheets under the top pages, deepest first, each showing past the one above it.
        for (d in leftTop downTo 1) {
            drawSheet(geometry, right = false, depth = d + leftSink, image(leftTop - d + 1))
        }
        for (d in (leaves - 1 - rightTop) downTo 1) {
            drawSheet(geometry, right = true, depth = d + rightSink, image(rightTop + d))
        }
        // The top pages: the back of the left top leaf, the front of the right one.
        drawLeaf(geometry, restFrame(geometry, right = false, depth = leftSink), image(leftTop + 1))
        drawLeaf(geometry, restFrame(geometry, right = true, depth = rightSink), image(rightTop))
        // The fold at the book's middle, with its binding thread, lies under any leaf in the air;
        // the crease, drawn last, darkens over the thread, so it sits down in the fold.
        if (stitchesShow(spreads.size / 2, leftTop, rightTop, flying)) drawStitches(spineX)
        if (flights.isEmpty()) {
            drawCrease(spineX)
            return
        }
        val frames = flights.map {
            it to turnFrame(it.t, leafWidth = page, bendDirection = it.bend)
        }
        drawGutterShades(spineX, page, frames.maxOf { (_, frame) -> frame.lift * frame.lift })
        // Leaves in the air keep the order they have in the book: nearer the left, a later leaf
        // lies over an earlier one; nearer the right, an earlier one over a later.
        frames
            .sortedBy { (flight, _) -> if (flight.t >= 0.5f) flight.leaf else -flight.leaf }
            .forEach { (flight, frame) ->
                drawLeaf(geometry, frame, front = image(flight.leaf), back = image(flight.leaf + 1))
            }
        drawCrease(spineX)
    }

    // A resting leaf [depth] down the right or left stack.
    private fun restFrame(geometry: PageGeometry, right: Boolean, depth: Float): TurnFrame =
        turnFrame(if (right) 0f else 1f, leafWidth = geometry.page, restLift = sheetLift(depth))

    private fun DrawScope.drawLeaf(
        geometry: PageGeometry,
        frame: TurnFrame,
        front: ImageBitmap,
        back: ImageBitmap = front,
        strips: IntRange = 0 until PageTurnDimens.Strips,
    ) {
        for (i in strips) drawStrip(frame, i, front, back, geometry)
    }

    /**
     * A leaf [depth] down a stack: only its outer strips, all that shows past the sheet above it,
     * shaded by how deep it lies and with its edge drawn, so each sheet reads as its own.
     */
    private fun DrawScope.drawSheet(
        geometry: PageGeometry,
        right: Boolean,
        depth: Float,
        image: ImageBitmap,
    ) {
        val frame = restFrame(geometry, right, depth)
        drawLeaf(
            geometry,
            frame,
            image,
            strips = PageTurnDimens.Strips - PageTurnDimens.EdgeStrips until PageTurnDimens.Strips,
        )
        pageOutline(frame, geometry.spineX, geometry.originY, geometry.perspectivePx, size.height)
            .fannedPath(
                sheetPath,
                fan = 0f,
                corner = geometry.corner.x,
                outwards = if (right) 1f else -1f,
            )
        // Each sheet lies a little more in the shadow of the ones above it.
        drawPath(
            sheetPath,
            ink.shade.copy(
                alpha =
                    (PageTurnDimens.SheetShadePerDepth * depth).coerceAtMost(
                        PageTurnDimens.SheetShadeMax
                    )
            ),
        )
        drawPath(
            sheetPath,
            ink.shade.copy(alpha = PageTurnDimens.SheetEdgeAlpha),
            style = Stroke(PageTurnDimens.SheetEdgeWidth.toPx()),
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
        geometry: PageGeometry,
    ) {
        val spineX = geometry.spineX
        val originY = geometry.originY
        val perspectivePx = geometry.perspectivePx
        val corner = geometry.corner
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

    // The binding thread down the fold, where it shows inside: between the first two holes and the
    // last two. It lies in a soft shadow of the fold, its twist marked along it, and enters the
    // paper through a small dark hole at each end.
    private fun DrawScope.drawStitches(spineX: Float) {
        val holes = PageTurnDimens.StitchHoles
        val width = PageTurnDimens.ThreadWidth.toPx()
        val twist = PageTurnDimens.ThreadTwist.toPx()
        for (run in holes.indices step 2) {
            val top = size.height * holes[run]
            val bottom = size.height * holes[run + 1]
            drawLine(
                ink.shade.copy(alpha = PageTurnDimens.ThreadBedAlpha),
                start = Offset(spineX, top),
                end = Offset(spineX, bottom),
                strokeWidth = PageTurnDimens.ThreadBedWidth.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                ink.thread,
                start = Offset(spineX, top),
                end = Offset(spineX, bottom),
                strokeWidth = width,
            )
            var y = top + twist
            while (y < bottom - twist / 2f) {
                drawLine(
                    ink.threadTwist.copy(alpha = PageTurnDimens.ThreadTwistAlpha),
                    start = Offset(spineX - width / 2f, y + twist / 3f),
                    end = Offset(spineX + width / 2f, y - twist / 3f),
                    strokeWidth = 1f,
                )
                y += twist
            }
            for (hole in floatArrayOf(top, bottom)) {
                drawCircle(
                    ink.shade.copy(alpha = PageTurnDimens.StitchHoleAlpha),
                    radius = PageTurnDimens.StitchHoleWidth.toPx() / 2f,
                    center = Offset(spineX, hole),
                )
            }
        }
    }
}

/** Where the pages are drawn from: the spine, the eye, and a page's width and corners. */
internal class PageGeometry(
    val spineX: Float,
    val originY: Float,
    val perspectivePx: Float,
    val corner: CornerRadius,
    val page: Float,
)

// A blank leaf's paper, as a small image: a page cuts it into strips like any other, and it scales.
private fun paperPage(color: Color): ImageBitmap {
    val image = ImageBitmap(PaperPageWidth, PaperPageWidth / 2)
    Canvas(image)
        .drawRect(
            Rect(0f, 0f, PaperPageWidth.toFloat(), PaperPageWidth / 2f),
            Paint().apply { this.color = color },
        )
    return image
}

private const val PaperPageWidth = 144

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
