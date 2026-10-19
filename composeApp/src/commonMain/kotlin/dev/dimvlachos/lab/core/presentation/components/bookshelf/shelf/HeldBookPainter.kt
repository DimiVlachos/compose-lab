package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookPose
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.FaceTexture
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Point2
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ProjectedFace
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.RIBBON_FAR
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.RIBBON_NEAR
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Rgb
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Vec3
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.bookShadow
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.buildBookFaces
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.mix
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ribbonAnchors
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.smoothStep
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.BlurMaskFilter
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Matrix
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Paint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Path
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Rect
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.RectF
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withSave
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SHADOW_INK = 0x140A04
private const val SHADOW_REACH = 60f
private const val SHADOW_FADE_IN = 4f

private const val OPEN_TAIL_FROM = 0.6f
private const val OPEN_TAIL_SHOWN = 0.8f
private const val CLOSED_RIBBON_GONE = 0.9f
private const val CLOSED_RIBBON_TURNED = 0.9f
private const val CLOSED_RIBBON_PINCHED = 0.02f
private const val CLOSED_RIBBON_FREE = 0.3f

internal class FaceOverlay(val bitmap: Bitmap, val alpha: Float)

internal enum class RibbonSide {
    Right,
    Left,
}

internal fun Canvas.drawHeldBook(
    pose: BookPose,
    faceBitmap: (FaceTexture) -> Bitmap?,
    readFraction: Float?,
    ribbonTail: RibbonSide?,
    lighting: ShelfLighting,
    scale: Float,
    neighbours: List<List<Point2>>,
    plainFace: (FaceTexture) -> Int,
    faceOverlay: (FaceTexture) -> FaceOverlay? = { null },
    tailUnderPages: Boolean = false,
    tailMotion: TailMotion? = null,
    shelfRibbonBookId: Int? = null,
    hidePages: Boolean = false,
    shade: Float = 1f,
    castShadow: Float = 1f,
    coverOnly: Boolean = false,
) {
    val faces = buildBookFaces(pose, readFraction, coverOnly)
    if (faces.isEmpty()) return
    if (castShadow > 0f) drawBookShadow(pose, readFraction, scale, castShadow, coverOnly)
    val onLeftPage = ribbonTail == RibbonSide.Left
    val openTail = ribbonTail != null && tailMotion != null
    if (shelfRibbonBookId != null) {
        drawClosedRibbon(
            shelfRibbonBookId,
            pose,
            scale,
            lighting,
            ribbonAnchors(pose, readFraction, onLeftPage, coverOnly),
            openTail,
        )
    }
    fun tail() {
        if (!openTail || tailMotion == null || pose.open <= OPEN_TAIL_FROM) return
        drawRibbonTail(
            ribbonAnchors(pose, readFraction, onLeftPage, coverOnly),
            smoothStep(OPEN_TAIL_FROM, OPEN_TAIL_SHOWN, pose.open),
            lighting,
            tailMotion,
        )
    }
    if (tailUnderPages) tail()

    val occluders = Path().apply { neighbours.forEach { addPath(polygon(it)) } }
    val shadePaint =
        if (shade < 1f) multiplyPaint((Rgb(255f, 255f, 255f) * shade).toArgb()) else null
    val held = smoothStep(0.25f, 1f, pose.travel)
    val matrix = Matrix()
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    for (face in faces) {
        if (hidePages && face.texture.isPage()) continue
        val bitmap = faceBitmap(face.texture)
        withSave {
            visibleRegion(face, occluders)?.let(::clipPath)
            if (bitmap != null) {
                drawFace(
                    face,
                    bitmap,
                    faceOverlay(face.texture),
                    matrix,
                    bitmapPaint,
                    lighting,
                    held,
                )
            } else drawPlainFace(face, plainFace(face.texture), lighting, held)
            shadePaint?.let { drawPath(polygon(face.corners), it) }
        }
    }

    if (!tailUnderPages) tail()
}

@Suppress("LongParameterList")
private fun Canvas.drawClosedRibbon(
    bookId: Int,
    pose: BookPose,
    k: Float,
    lighting: ShelfLighting,
    openAnchors: Pair<Point2, Point2>,
    handsOver: Boolean,
) {
    val alpha =
        if (handsOver) 1f - smoothStep(OPEN_TAIL_SHOWN, CLOSED_RIBBON_GONE, pose.open)
        else 1f - smoothStep(OPEN_TAIL_FROM, OPEN_TAIL_SHOWN, pose.open)
    if (alpha <= 0f) return
    val (t, _, d) = pose.dimensions
    val turned = smoothStep(0f, CLOSED_RIBBON_TURNED, pose.swing)
    val onShelf = Vec3(shelfRibbonOffset(bookId, t, k), 0f, 0f)
    val near = mix(onShelf, Vec3(t, 0f, -d * RIBBON_NEAR), turned)
    val far = mix(onShelf + Vec3(shelfRibbonWidth(k), 0f, 0f), Vec3(t, 0f, -d * RIBBON_FAR), turned)
    val opening = smoothStep(0f, OPEN_TAIL_FROM, pose.open)
    val a = lerp(pose.camera.project(pose.map(near)), openAnchors.first, opening)
    val b = lerp(pose.camera.project(pose.map(far)), openAnchors.second, opening)
    val width = abs(b.x - a.x)
    if (width <= 0f) return
    val notch = mix(shelfRibbonNotch(k), width * RIBBON_TAIL_NOTCH, turned)
    val drop = smoothStep(CLOSED_RIBBON_PINCHED, CLOSED_RIBBON_FREE, pose.travel)
    val length = mix(shelfRibbonTip(bookId, k), RIBBON_TAIL_LENGTH * k, drop)
    drawHangingRibbon(minOf(a.x, b.x), (a.y + b.y) / 2f, width, length, notch, k, lighting, alpha)
}

private fun FaceTexture.isPage() = this == FaceTexture.LeftPage || this == FaceTexture.RightPage

private fun lerp(a: Point2, b: Point2, t: Float) =
    Point2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

@Suppress("LongParameterList")
internal fun Canvas.drawFace(
    face: ProjectedFace,
    bitmap: Bitmap,
    overlay: FaceOverlay?,
    matrix: Matrix,
    bitmapPaint: Paint,
    lighting: ShelfLighting,
    held: Float,
) {
    val uv = face.uv
    val src =
        Rect(
            (uv.u0 * bitmap.width).roundToInt(),
            (uv.v0 * bitmap.height).roundToInt(),
            (uv.u1 * bitmap.width).roundToInt(),
            (uv.v1 * bitmap.height).roundToInt(),
        )
    if (src.isEmpty) return
    val c = face.corners
    val source =
        floatArrayOf(
            src.left.toFloat(),
            src.top.toFloat(),
            src.right.toFloat(),
            src.top.toFloat(),
            src.right.toFloat(),
            src.bottom.toFloat(),
            src.left.toFloat(),
            src.bottom.toFloat(),
        )
    val target = floatArrayOf(c[0].x, c[0].y, c[1].x, c[1].y, c[2].x, c[2].y, c[3].x, c[3].y)
    if (!matrix.setPolyToPoly(source, 0, target, 0, 4)) return
    withSave {
        concat(matrix)
        drawBitmap(bitmap, src, RectF(src), bitmapPaint)
        if (overlay != null && overlay.alpha > 0f) {
            drawBitmap(
                overlay.bitmap,
                0f,
                0f,
                Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = (overlay.alpha * 255f).roundToInt()
                },
            )
        }
    }
    val outline =
        Path().apply {
            moveTo(c[0].x, c[0].y)
            lineTo(c[1].x, c[1].y)
            lineTo(c[2].x, c[2].y)
            lineTo(c[3].x, c[3].y)
            close()
        }
    drawPath(outline, multiplyPaint(lighting.faceLight(face.normal, held).toArgb()))
}

internal fun Canvas.drawPlainFace(
    face: ProjectedFace,
    color: Int,
    lighting: ShelfLighting,
    held: Float,
) {
    val outline = polygon(face.corners)
    drawPath(outline, fillPaint(color))
    drawPath(outline, multiplyPaint(lighting.faceLight(face.normal, held).toArgb()))
}

private fun visibleRegion(face: ProjectedFace, occluders: Path): Path? {
    if (face.insideRow.isEmpty() || occluders.isEmpty) return null
    val hidden = polygon(face.insideRow).apply { op(occluders, Path.Op.INTERSECT) }
    return polygon(face.corners)
        .apply { op(hidden, Path.Op.DIFFERENCE) }
        .also {
            if (face.inFront.size >= 3) it.op(polygon(face.inFront), Path.Op.UNION)
        }
}

internal fun polygon(points: List<Point2>) =
    Path().apply {
        points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        close()
    }

private fun Canvas.drawBookShadow(
    pose: BookPose,
    readFraction: Float?,
    k: Float,
    alpha: Float,
    coverOnly: Boolean,
) {
    val depth = pose.dimensions.depth
    val reach = SHADOW_REACH * k
    val shape =
        bookShadow(
            pose,
            readFraction,
            Point2(SUN_SHADOW_DX * k / depth, SUN_SHADOW_DY * k / depth),
            reach,
            coverOnly,
        )
    val fade = smoothStep(0f, SHADOW_FADE_IN * k, shape.height) * alpha
    if (fade <= 0f || shape.outline.size < 3) return
    val lifted = shape.height / reach
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = argb(SHADOW_INK, (0.58f - 0.16f * lifted) * fade)
            maskFilter =
                BlurMaskFilter(2f * k + 0.5f * shape.height * lifted, BlurMaskFilter.Blur.NORMAL)
        }
    drawPath(polygon(shape.outline), paint)
}
