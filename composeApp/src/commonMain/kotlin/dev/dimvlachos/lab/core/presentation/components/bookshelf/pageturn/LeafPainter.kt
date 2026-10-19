package dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.ui.BookshelfArt
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

private val LeafShade = BookshelfArt.LeafShade

private val LeafGlare = BookshelfArt.LeafGlare

private val LeafGutter = BookshelfArt.LeafGutter

private val Unlit = Color.White

private const val MaxCachedImages = 16
private const val SPINE_SAMPLE_PX = 1
private const val SPINE_FILL_PX = 3f
private const val CAST_BLUR_FLOOR = 0.4f

internal class RestingBook(
    val left: LeafFaces?,
    val right: LeafFaces?,
    val leftSheets: Int,
    val rightSheets: Int,
    val leftSink: Float,
    val rightSink: Float,
    val arch: Float,
)

internal class LeafFaces(
    val front: ImageBitmap,
    val back: ImageBitmap,
    val band: ImageBitmap?,
    val bandAlpha: Float,
)

internal class LeafPainter {
    private val matrix = Matrix()
    private val wedgePath = Path()
    private var tint: ColorFilter? = null
    private var tintLight = Unlit
    private val imagePath = Path()
    private val imageMatrix = Matrix()
    private val pageMatrix = Matrix()
    private val brushes = HashMap<ImageBitmap, Brush>()
    private val shadowPaint = Paint()
    private val pixel = IntArray(1)

    fun DrawScope.drawFlights(
        layout: BookLayout,
        flights: List<Flight>,
        faces: (Int) -> LeafFaces?,
        light: Color,
        beforeLeaf: DrawScope.(Flight, TurnFrame) -> Unit = { _, _ -> },
    ) {
        if (flights.isEmpty()) return
        val geometry = layout.geometry
        val frames = flights.map {
            it to
                turnFrame(
                    it.t,
                    leafWidth = geometry.page,
                    bendDirection = it.bend,
                    height = geometry.height,
                    tilt = it.tilt,
                    grip = it.grip,
                    restLift = geometry.restLift,
                )
        }
        translate(layout.left, layout.top) {
            drawGutterShades(geometry, frames.maxOf { (_, frame) -> frame.lift * frame.lift })
            frames
                .sortedBy { (flight, _) -> if (flight.t >= 0.5f) flight.leaf else -flight.leaf }
                .forEach { (flight, frame) ->
                    val leaf = faces(flight.leaf) ?: return@forEach
                    beforeLeaf(flight, frame)
                    drawCastShadow(frame, geometry)
                    lit(light) {
                        for (i in 0 until frame.wedges) {
                            if (frame.tilt == 0f) drawStrip(frame, i, leaf, geometry, shaded = true)
                            else drawWedge(frame, i, leaf, geometry)
                        }
                    }
                }
        }
    }

    fun DrawScope.drawStacks(
        layout: BookLayout,
        book: RestingBook,
        light: Color,
        leftAlpha: Float,
        rightAlpha: Float,
    ) {
        val geometry = layout.geometry
        translate(layout.left, layout.top) {
            book.left?.let {
                withAlpha(leftAlpha) {
                    drawStack(
                        geometry,
                        right = false,
                        it,
                        book.leftSheets,
                        book.leftSink,
                        book.arch,
                        light,
                    )
                }
            }
            book.right?.let {
                withAlpha(rightAlpha) {
                    drawStack(
                        geometry,
                        right = true,
                        it,
                        book.rightSheets,
                        book.rightSink,
                        book.arch,
                        light,
                    )
                }
            }
        }
    }

    fun DrawScope.drawRestingBook(layout: BookLayout, book: RestingBook, light: Color) {
        val geometry = layout.geometry
        translate(layout.left, layout.top) {
            if (book.left != null) {
                book.right?.let {
                    drawSpineFill(
                        geometry,
                        restFrame(geometry, true, book.arch, book.rightSink),
                        it,
                        light,
                    )
                }
            }
            book.left?.let {
                val frame = restFrame(geometry, right = false, book.arch, book.leftSink)
                drawLeaf(geometry, frame, it, light)
            }
            book.right?.let {
                val frame = restFrame(geometry, right = true, book.arch, book.rightSink)
                drawLeaf(geometry, frame, it, light)
            }
        }
    }

    private fun DrawScope.drawSpineFill(
        geometry: PageGeometry,
        frame: TurnFrame,
        right: LeafFaces,
        light: Color,
    ) {
        val texture = right.front
        texture.readPixels(
            pixel,
            startX = SPINE_SAMPLE_PX,
            startY = texture.height / 2,
            width = 1,
            height = 1,
        )
        val paper = Color(pixel[0])
        drawLine(
            Color(paper.red * light.red, paper.green * light.green, paper.blue * light.blue),
            start = frame.seenEdge(0, 0f, geometry),
            end = frame.seenEdge(0, frame.height, geometry),
            strokeWidth = SPINE_FILL_PX,
        )
    }

    private inline fun DrawScope.withAlpha(alpha: Float, block: DrawScope.() -> Unit) {
        if (alpha <= 0f) return
        if (alpha >= 1f) return block()
        drawIntoCanvas {
            it.saveLayer(Rect(Offset.Zero, size), Paint().apply { this.alpha = alpha })
        }
        block()
        drawIntoCanvas { it.restore() }
    }

    fun restFrame(geometry: PageGeometry, right: Boolean, arch: Float, depth: Float = 0f) =
        turnFrame(
            if (right) 0f else 1f,
            leafWidth = geometry.page,
            height = geometry.height,
            restLift = arch * sheetLift(depth),
        )

    @Suppress("LongParameterList")
    private fun DrawScope.drawStack(
        geometry: PageGeometry,
        right: Boolean,
        faces: LeafFaces,
        sheets: Int,
        sink: Float,
        arch: Float,
        light: Color,
    ) {
        val paper = LeafFaces(faces.front, faces.back, null, 0f)
        val fan = PageTurnDimens.SheetFan.toPx() * if (right) 1f else -1f
        for (d in sheets downTo 1) {
            val depth = d + sink
            val frame = restFrame(geometry, right, arch, depth)
            val from = frame.wedges - PageTurnDimens.EdgeStrips
            translate(left = fan * depth) {
                for (i in from until frame.wedges) drawStrip(
                    frame,
                    i,
                    paper,
                    geometry,
                    shaded = false,
                )
                sheetOutline(frame, geometry, from)
                drawPath(wedgePath, light, blendMode = BlendMode.Multiply)
                val shade =
                    (PageTurnDimens.SheetShadePerDepth * depth).coerceAtMost(
                        PageTurnDimens.SheetShadeMax
                    )
                drawPath(wedgePath, LeafShade.copy(alpha = shade))
                drawPath(
                    wedgePath,
                    LeafShade.copy(alpha = PageTurnDimens.SheetEdgeAlpha),
                    style = Stroke(PageTurnDimens.SheetEdgeWidth.toPx()),
                )
            }
        }
    }

    private fun DrawScope.drawLeaf(
        geometry: PageGeometry,
        frame: TurnFrame,
        faces: LeafFaces,
        light: Color,
    ) {
        for (i in 0 until frame.wedges) drawStrip(frame, i, faces, geometry, shaded = false)
        sheetOutline(frame, geometry, 0)
        drawPath(wedgePath, light, blendMode = BlendMode.Multiply)
    }

    private fun sheetOutline(frame: TurnFrame, geometry: PageGeometry, from: Int) {
        wedgePath.rewind()
        for (i in from..frame.wedges) {
            val at = frame.seenEdge(i, 0f, geometry)
            if (i == from) wedgePath.moveTo(at.x, at.y) else wedgePath.lineTo(at.x, at.y)
        }
        for (i in frame.wedges downTo from) {
            val at = frame.seenEdge(i, frame.height, geometry)
            wedgePath.lineTo(at.x, at.y)
        }
        wedgePath.close()
    }

    // Faces arrive as ImageBitmaps already; only their brushes are kept.
    private fun image(bitmap: ImageBitmap): ImageBitmap {
        if (brushes.size > MaxCachedImages) brushes.clear()
        return bitmap
    }

    private fun brush(bitmap: ImageBitmap): Brush =
        brushes.getOrPut(bitmap) { ShaderBrush(ImageShader(image(bitmap))) }

    private fun bandBrush(bitmap: ImageBitmap): Brush =
        brushes.getOrPut(bitmap) {
            ShaderBrush(ImageShader(image(bitmap), TileMode.Decal, TileMode.Decal))
        }

    private fun DrawScope.drawStrip(
        frame: TurnFrame,
        index: Int,
        faces: LeafFaces,
        geometry: PageGeometry,
        shaded: Boolean,
    ) {
        val facing = frame.facesReader(index)
        val image = image(if (facing) faces.front else faces.back)
        val stripWidth = frame.stripWidth
        val height = geometry.height
        val seam = if (index == frame.wedges - 1) 0f else PageTurnDimens.SeamPx
        val span = stripWidth + seam
        val perPx = image.width / frame.pageWidth
        val start =
            if (facing) (index * stripWidth - frame.textureShift) * perPx
            else image.width - (index * stripWidth + span - frame.textureShift) * perPx
        val srcLeft = floor(start).toInt().coerceIn(0, image.width)
        val srcRight = ceil(start + span * perPx).toInt().coerceIn(0, image.width)
        val srcWidth = srcRight - srcLeft
        if (srcWidth <= 0) return
        val right = stripWidth + seam
        frame.matrix(index, geometry, matrix)
        withTransform({
            transform(matrix)
            translate(left = index * stripWidth)
        }) {
            withTransform({
                if (!facing) scale(-1f, 1f, pivot = Offset(span / 2f, 0f))
                scale(span / srcWidth, height / image.height, pivot = Offset.Zero)
            }) {
                drawImage(
                    image,
                    srcOffset = IntOffset(srcLeft, 0),
                    srcSize = IntSize(srcWidth, image.height),
                    dstSize = IntSize(srcWidth, image.height),
                    filterQuality = FilterQuality.Medium,
                    colorFilter = tint,
                )
                val band = faces.band?.takeIf { facing && faces.bandAlpha > 0f }?.let(::image)
                val bandWidth = band?.let { minOf(srcWidth, it.width - srcLeft) } ?: 0
                if (band != null && bandWidth > 0) {
                    drawImage(
                        band,
                        srcOffset = IntOffset(srcLeft, 0),
                        srcSize = IntSize(bandWidth, band.height),
                        dstSize = IntSize(bandWidth, band.height),
                        alpha = faces.bandAlpha,
                        filterQuality = FilterQuality.Medium,
                        colorFilter = tint,
                    )
                }
            }
            if (!shaded) return@withTransform
            drawLight(
                frame,
                index,
                from = Offset.Zero,
                to = Offset(stripWidth, 0f),
                area = Rect(0f, 0f, right, height),
            )
        }
    }

    private fun DrawScope.drawWedge(
        frame: TurnFrame,
        index: Int,
        faces: LeafFaces,
        geometry: PageGeometry,
    ) {
        val facing = frame.facesReader(index)
        val bitmap = if (facing) faces.front else faces.back
        val width = frame.width
        val height = frame.height
        val last = index == frame.wedges - 1
        val after = if (last) 0f else PageTurnDimens.SeamPx
        if (!frame.wedgeOutline(index, 0f, after, wedgePath)) return
        val middle = height / 2f
        val start = Offset(frame.rulingX(index, middle), middle)
        val leanX = frame.rulingX(index, height) - frame.rulingX(index, 0f)
        val length = sqrt(leanX * leanX + height * height)
        val across = Offset(height / length, -leanX / length)
        val span = (frame.rulingX(index + 1, middle) - start.x) * across.x
        val sx = bitmap.width / frame.pageWidth
        imageMatrix.reset()
        imageMatrix[0, 0] = if (facing) sx else -sx
        imageMatrix[1, 1] = bitmap.height / height
        imageMatrix[3, 0] =
            if (facing) -frame.textureShift * sx else bitmap.width + frame.textureShift * sx
        imagePath.rewind()
        imagePath.addPath(wedgePath)
        imagePath.transform(imageMatrix)
        pageMatrix.reset()
        pageMatrix[0, 0] = if (facing) 1f / sx else -1f / sx
        pageMatrix[1, 1] = height / bitmap.height
        pageMatrix[3, 0] =
            if (facing) frame.textureShift else bitmap.width / sx + frame.textureShift
        frame.matrix(index, geometry, matrix)
        withTransform({
            transform(matrix)
            transform(pageMatrix)
        }) {
            drawPath(imagePath, brush(bitmap), colorFilter = tint)
            val band = faces.band?.takeIf { facing && faces.bandAlpha > 0f }
            if (band != null) {
                drawPath(imagePath, bandBrush(band), alpha = faces.bandAlpha, colorFilter = tint)
            }
        }
        withTransform({ transform(matrix) }) {
            drawLight(
                frame,
                index,
                from = start,
                to = start + across * span,
                area = Rect(0f, 0f, width, height),
                shape = wedgePath,
            )
        }
    }

    @Suppress("LongParameterList")
    private fun DrawScope.drawLight(
        frame: TurnFrame,
        index: Int,
        from: Offset,
        to: Offset,
        area: Rect,
        shape: Path? = null,
    ) {
        val (shadeFrom, shadeTo) = stripShadeAlphas(frame, index)
        fill(
            Brush.linearGradient(
                listOf(
                    lighted(LeafShade).copy(alpha = shadeFrom),
                    lighted(LeafShade).copy(alpha = shadeTo),
                ),
                start = from,
                end = to,
            ),
            area,
            shape,
        )
        val glare = stripGlareAlpha(frame, index)
        if (glare > 0f) fill(SolidColor(lighted(LeafGlare).copy(alpha = glare)), area, shape)
    }

    private fun DrawScope.fill(brush: Brush, area: Rect, shape: Path?) {
        if (shape != null) drawPath(shape, brush)
        else drawRect(brush, topLeft = area.topLeft, size = area.size)
    }

    private inline fun lit(light: Color, block: () -> Unit) {
        tint = ColorFilter.tint(light, BlendMode.Modulate)
        tintLight = light
        try {
            block()
        } finally {
            tint = null
            tintLight = Unlit
        }
    }

    private fun lighted(color: Color): Color =
        color.copy(
            red = color.red * tintLight.red,
            green = color.green * tintLight.green,
            blue = color.blue * tintLight.blue,
        )

    private fun DrawScope.drawCastShadow(frame: TurnFrame, geometry: PageGeometry) {
        val amount = frame.lift
        if (amount <= 0f) return
        sheetOutline(frame, geometry, 0)
        // A shadow layer of radius r, drawn as the outline itself blurred by Skia's sigma for r.
        val radius = PageTurnDimens.CastShadowBlur.toPx() * (CAST_BLUR_FLOOR + amount)
        shadowPaint.color = LeafShade.copy(alpha = PageTurnDimens.CastShadowAlpha * amount)
        shadowPaint.setBlur(0.57735f * radius + 0.5f)
        clipRect(
            left = geometry.spineX - geometry.page,
            top = 0f,
            right = geometry.spineX + geometry.page,
            bottom = geometry.height,
        ) {
            translate(top = PageTurnDimens.CastShadowDrop.toPx() * amount) {
                drawIntoCanvas { it.drawPath(wedgePath, shadowPaint) }
            }
        }
    }

    private fun DrawScope.drawGutterShades(geometry: PageGeometry, amount: Float) {
        if (amount <= 0f) return
        val spineX = geometry.spineX
        val width = geometry.page * PageTurnDimens.GutterWidthFraction
        val clear = LeafGutter.copy(alpha = 0f)
        drawRect(
            Brush.horizontalGradient(
                listOf(LeafGutter.copy(alpha = PageTurnDimens.GutterLeftAlpha * amount), clear),
                startX = spineX,
                endX = spineX - width,
            ),
            topLeft = Offset(spineX - width, 0f),
            size = Size(width, geometry.height),
        )
        drawRect(
            Brush.horizontalGradient(
                listOf(LeafGutter.copy(alpha = PageTurnDimens.GutterRightAlpha * amount), clear),
                startX = spineX,
                endX = spineX + width,
            ),
            topLeft = Offset(spineX, 0f),
            size = Size(width, geometry.height),
        )
    }
}

private fun TurnFrame.seenEdge(boundary: Int, y: Float, geometry: PageGeometry): Offset {
    val x = boundary * stripWidth
    val wedge = (boundary - 1).coerceAtLeast(0).coerceAtMost(wedges - 1)
    return projectOn(wedge, x, y, geometry)
}

private fun TurnFrame.wedgeOutline(index: Int, before: Float, after: Float, path: Path): Boolean {
    val last = index == wedges - 1
    val beyond = width + PageTurnDimens.SeamPx
    val corners =
        clippedToPage(
            floatArrayOf(
                rulingX(index, 0f) - before,
                0f,
                if (last) beyond else rulingX(index + 1, 0f) + after,
                0f,
                if (last) beyond else rulingX(index + 1, height) + after,
                height,
                rulingX(index, height) - before,
                height,
            ),
            width,
        ) ?: return false
    path.rewind()
    for (i in corners.indices step 2) {
        if (i == 0) path.moveTo(corners[0], corners[1]) else path.lineTo(corners[i], corners[i + 1])
    }
    path.close()
    return true
}

private fun clippedToPage(points: FloatArray, width: Float): FloatArray? {
    val out = ArrayList<Float>(12)
    val n = points.size / 2
    for (i in 0 until n) {
        val x0 = points[i * 2]
        val y0 = points[i * 2 + 1]
        val x1 = points[(i + 1) % n * 2]
        val y1 = points[(i + 1) % n * 2 + 1]
        if (x0 <= width) {
            out += x0
            out += y0
        }
        if ((x0 <= width) != (x1 <= width)) {
            val f = (width - x0) / (x1 - x0)
            out += width
            out += y0 + (y1 - y0) * f
        }
    }
    return if (out.size < 6) null else out.toFloatArray()
}
