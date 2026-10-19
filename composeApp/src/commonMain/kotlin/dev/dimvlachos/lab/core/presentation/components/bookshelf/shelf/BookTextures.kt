package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.presentation.canvas2d.freeze
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookPageSetting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Binding
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookDimensions
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookPage
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Emblem
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.FaceTexture
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.mixRgb
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.BlendMode
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.BlurMaskFilter
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.LinearGradient
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Paint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Path
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Rect
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.RectF
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Shader
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.TextPaint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.createBitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.ellipsize
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.toDegrees
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withSave
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val PAGE_PAPER = 0xFBF6EB
internal const val PAGE_EDGE = 0xF6EFDE
internal const val RIBBON_BAND_SPAN = 0.16f

// The smallest a title is set, of its size, to keep every word on the cover or title page.
private const val MIN_TITLE_SCALE = 0.7f
internal const val PAGE_RIBBON_FOOT_X = 0.085f
internal const val PAGE_RIBBON_WIDTH = 0.026f
private const val PAGE_INK = 0x261E18
private const val PAPER_EDGE = 0xF0E6CC
private const val TAU = (2 * PI).toFloat()
private const val PAGE_CAPTION = 0x3C2E22
private const val PAGE_RIBBON_X = 0.055f
private const val GUTTER_SHADE = 0x2B2014
private const val GUTTER_SHADE_ALPHA = 0.26f
private const val GUTTER_SHADE_SPAN = 0.42f
private const val GUTTER_SHADE_STOPS = 9
private const val CREASE_SHADE = 0x4F412C
private const val CREASE_ALPHA = 0.1f
private const val CREASE_SPAN = 0.1f
private const val TURN_AWAY_ALPHA = 0.2f
private const val TURN_AWAY_SPAN = 0.16f
private const val PAGE_RIBBON_SHADE_BEFORE = 4f
private const val PAGE_RIBBON_SHADE_SPAN = 14f
private val PAGE_RIBBON_STOPS = floatArrayOf(0f, 0.45f, 1f)
private val PAGE_RIBBON_SHADES = floatArrayOf(0.7f, 1.25f, 0.8f)

internal object PageGeometry {
    const val MARGIN = 0.13f
    const val GUTTER_MARGIN = 0.1365f
    const val HEADING_BASELINE = 0.072f
    const val BODY_TOP = 0.1f
    const val BODY_HEIGHT = 0.78f
    const val FOLIO_BASELINE = 0.95f
    const val INDENT_EM = 1.4f
}

internal data class PageMetrics(
    val bodySize: Float,
    val lineHeight: Float,
    val captionSize: Float,
    val captionSpacingEm: Float,
)

internal class PageTypeset(
    val passage: PageContent.Passage,
    val pageWidth: Float,
    val heading: TextLayoutResult,
    val headingOrigin: Offset,
    val body: TextLayoutResult,
    val bodyOrigin: Offset,
)

internal sealed interface PageContent {
    data class Passage(val heading: String, val paragraphs: List<String>, val folio: String?) :
        PageContent

    data class TitlePage(val title: String, val author: String, val credit: String?) : PageContent

    data object Endpaper : PageContent

    data object Blank : PageContent

    data class Book(
        val chapterName: String,
        val folio: String?,
        val page: BookPage,
        val words: List<List<String>>,
        val highlights: Map<Int, Set<Int>>,
        val withBody: Boolean = true,
    ) : PageContent
}

internal data class OpenBookPages(
    val left: PageContent,
    val right: PageContent,
    val ribbon: Boolean,
    val liveLeft: Boolean = false,
    val pending: Boolean = false,
)

internal class BookShell(val bookId: Int, val cover: Bitmap?, val faces: Map<FaceTexture, Bitmap>)

internal fun BookShell?.isBakedWith(cover: Bitmap?): Boolean = this != null && this.cover === cover

internal class BookPages(
    val bookId: Int,
    val pages: OpenBookPages,
    val left: Bitmap,
    val right: Bitmap,
    val rightBlank: Bitmap?,
    val leftBlank: Bitmap? = null,
)

internal fun createBookShell(
    book: ShelfBookVisual,
    dims: BookDimensions,
    pxPerUnit: Float,
    spine: Bitmap,
    cover: Bitmap?,
    kit: ShelfKit,
): BookShell {
    val (t, h, d) = dims
    val edgeSeed = ShelfRandom(book.id.toLong() * 31L + 7L)
    return BookShell(
        book.id,
        cover,
        mapOf(
            FaceTexture.Spine to spine,
            FaceTexture.Cover to face(d, h, pxPerUnit) { drawCover(book, cover, d, h, kit) },
            FaceTexture.Back to face(d, h, pxPerUnit * 0.5f) { drawBackBoard(book, d, h, kit) },
            FaceTexture.Head to
                face(t, d, pxPerUnit) {
                    drawEdges(book, t, d, alongWidth = true, headband = true, edgeSeed)
                },
            FaceTexture.Fore to
                face(t, h, pxPerUnit) {
                    drawEdges(book, t, h, alongWidth = false, headband = false, edgeSeed)
                },
        ),
    )
}

internal fun createBookPages(
    book: ShelfBookVisual,
    dims: BookDimensions,
    pxPerUnit: Float,
    pages: OpenBookPages,
    metrics: PageMetrics,
    typeset: PageTypeset?,
    bookSetting: BookPageSetting?,
    kit: ShelfKit,
): BookPages {
    val (_, h, d) = dims
    val right = pages.right
    return BookPages(
        book.id,
        pages,
        left =
            face(d, h, pxPerUnit) {
                drawPage(
                    book,
                    pages.left,
                    rightSide = false,
                    ribbon = false,
                    d,
                    h,
                    metrics,
                    kit,
                    bookSetting = bookSetting,
                )
            },
        right =
            face(d, h, pxPerUnit) {
                val measured = typeset?.takeIf { it.passage == pages.right }
                drawPage(
                    book,
                    if (measured != null) PageContent.Blank else pages.right,
                    rightSide = true,
                    ribbon = pages.ribbon,
                    d,
                    h,
                    metrics,
                    kit,
                    measured,
                    bookSetting,
                )
            },
        rightBlank =
            when (right) {
                is PageContent.Passage,
                is PageContent.Book ->
                    face(d, h, pxPerUnit) {
                        drawPage(
                            book,
                            if (right is PageContent.Book) right.copy(withBody = false)
                            else PageContent.Blank,
                            rightSide = true,
                            ribbon = pages.ribbon,
                            d,
                            h,
                            metrics,
                            kit,
                            bookSetting = bookSetting,
                        )
                    }
                else -> null
            },
        leftBlank =
            (pages.left as? PageContent.Book)
                ?.takeIf { pages.liveLeft }
                ?.let { left ->
                    face(d, h, pxPerUnit) {
                        drawPage(
                            book,
                            left.copy(withBody = false),
                            rightSide = false,
                            ribbon = false,
                            d,
                            h,
                            metrics,
                            kit,
                            bookSetting = bookSetting,
                        )
                    }
                },
    )
}

internal fun createSpineBitmap(
    book: ShelfBookVisual,
    t: Float,
    h: Float,
    pxPerUnit: Float,
    kit: ShelfKit,
): Bitmap = face(t, h, pxPerUnit) { drawSpine(book, t, h, kit) }

internal fun createPageFace(
    book: ShelfBookVisual,
    dims: BookDimensions,
    pxPerUnit: Float,
    content: PageContent,
    rightSide: Boolean,
    metrics: PageMetrics,
    bookSetting: BookPageSetting?,
    kit: ShelfKit,
): Bitmap {
    val (_, h, d) = dims
    return face(d, h, pxPerUnit) {
        drawPage(
            book,
            content,
            rightSide = rightSide,
            ribbon = false,
            d,
            h,
            metrics,
            kit,
            bookSetting = bookSetting,
        )
    }
}

internal fun pageRibbonFootShades(pageWidth: Float, count: Int): FloatArray =
    FloatArray(count) { i ->
        val across = i / (count - 1f) - 0.5f
        val x = pageWidth * (PAGE_RIBBON_FOOT_X + PAGE_RIBBON_WIDTH * across)
        val start = pageWidth * PAGE_RIBBON_X - PAGE_RIBBON_SHADE_BEFORE
        val g = ((x - start) / PAGE_RIBBON_SHADE_SPAN).coerceIn(0f, 1f)
        val k = if (g <= PAGE_RIBBON_STOPS[1]) 0 else 1
        val f = (g - PAGE_RIBBON_STOPS[k]) / (PAGE_RIBBON_STOPS[k + 1] - PAGE_RIBBON_STOPS[k])
        PAGE_RIBBON_SHADES[k] + (PAGE_RIBBON_SHADES[k + 1] - PAGE_RIBBON_SHADES[k]) * f
    }

internal fun createRibbonBand(dims: BookDimensions, pxPerUnit: Float): Bitmap {
    val (_, h, d) = dims
    val pageWidth = (d * pxPerUnit).roundToInt().coerceAtLeast(1)
    val pageHeight = (h * pxPerUnit).roundToInt().coerceAtLeast(1)
    val bitmap = createBitmap((pageWidth * RIBBON_BAND_SPAN).roundToInt(), pageHeight)
    Canvas(bitmap).apply {
        scale(pageWidth / d, pageHeight / h)
        drawPageRibbon(d, h)
    }
    bitmap.freeze()
    return bitmap
}

private fun face(w: Float, h: Float, pxPerUnit: Float, draw: Canvas.() -> Unit): Bitmap {
    val bitmap =
        createBitmap(
            (w * pxPerUnit).roundToInt().coerceAtLeast(1),
            (h * pxPerUnit).roundToInt().coerceAtLeast(1),
        )
    Canvas(bitmap).apply {
        scale(bitmap.width / w, bitmap.height / h)
        draw()
    }
    // Drawn every frame from now on, so frozen: see freeze.
    bitmap.freeze()
    return bitmap
}

private fun Canvas.drawCover(
    book: ShelfBookVisual,
    cover: Bitmap?,
    w: Float,
    h: Float,
    kit: ShelfKit,
) {
    withSave {
        clipRect(0f, 0f, w, h)
        if (cover != null) {
            drawBitmap(
                cover,
                centreCrop(cover, w / h),
                RectF(0f, 0f, w, h),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
        } else {
            drawGeneratedCover(book, w, h, kit)
        }
        drawRect(
            0f,
            0f,
            w,
            h,
            Paint().apply {
                shader =
                    horizontalGradient(
                        0f,
                        w,
                        0f to black(0.28f),
                        0.04f to black(0.05f),
                        0.08f to white(0.06f),
                        0.2f to black(0f),
                        1f to black(0.12f),
                    )
            },
        )
    }
}

private fun Canvas.drawGeneratedCover(book: ShelfBookVisual, w: Float, h: Float, kit: ShelfKit) {
    drawRect(0f, 0f, w, h, fillPaint(argb(book.style.base)))
    val text = TextPaint(TEXT_FLAGS).apply { textAlign = Paint.Align.CENTER }
    if (book.style.binding == Binding.Cloth) {
        texture(kit.noise.weave, 5f, 0f, 0f, w, h, 0.55f)
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        frame.strokeWidth = 1.2f
        frame.color = black(0.28f)
        drawRect(7f, 7f, w - 7f, h - 7f, frame)
        frame.strokeWidth = 0.6f
        frame.color = argb(0xFFF0DC, 0.12f)
        drawRect(8.2f, 8.2f, w - 8.2f, h - 8.2f, frame)
        val size = minOf(w * 0.1f, 15f)
        text.typeface = kit.fonts.serif
        text.textSize = size
        text.letterSpacing = 0.1f
        val title = text.fitTitle(book.title.uppercase(), w * 0.74f, maxLines = 3, size)
        title.lines.forEachIndexed { i, line ->
            val y = h * 0.26f + i * title.size * 1.3f
            text.shader = null
            text.color = black(0.35f)
            drawText(line, w / 2f, y + 0.6f, text)
            text.shader = giltShader(0f, y - title.size, 0f, y)
            drawText(line, w / 2f, y, text)
        }
        text.letterSpacing = 0f
        drawEmblem(book.style.emblem, w / 2f, h * 0.56f, w * 0.17f)
        text.typeface = kit.fonts.serifItalic
        text.textSize = size * 0.8f
        text.shader = giltShader(0f, h * 0.84f - size, 0f, h * 0.84f)
        drawText(book.author, w / 2f, h * 0.86f, text)
    } else {
        texture(kit.noise.grain, 14f, 0f, 0f, w, h, 0.25f)
        val ink = argb(book.style.ink)
        drawRect(0f, 0f, w, h * 0.26f, fillPaint(ink))
        drawCircle(w * 0.5f, h * 0.52f, w * 0.16f, fillPaint(argb(book.style.accent)))
        text.typeface = kit.fonts.sansBold
        text.color = argb(book.style.base)
        val title = text.fitTitle(book.title.uppercase(), w * 0.84f, maxLines = 2, w * 0.1f)
        title.lines.forEachIndexed { i, line ->
            drawText(line, w / 2f, h * 0.12f + i * title.size * 1.1f, text)
        }
        text.typeface = kit.fonts.sans
        text.textSize = w * 0.065f
        text.color = ink
        drawText(book.author, w / 2f, h * 0.82f, text)
    }
}

private fun Canvas.drawEmblem(emblem: Emblem, x: Float, y: Float, r: Float) {
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = r * 0.09f
            strokeCap = Paint.Cap.ROUND
            shader = giltShader(x - r, y - r, x + r, y + r)
        }
    withSave {
        translate(x, y)
        when (emblem) {
            Emblem.Sun -> {
                drawCircle(0f, 0f, r * 0.36f, paint)
                for (i in 0 until 12) {
                    val a = i / 12f * TAU
                    val outer = if (i % 2 == 1) 0.7f else 0.86f
                    drawLine(
                        cos(a) * r * 0.52f,
                        sin(a) * r * 0.52f,
                        cos(a) * r * outer,
                        sin(a) * r * outer,
                        paint,
                    )
                }
                for (j in 0 until 2) {
                    drawPath(wavePath(r, r * (1.15f + j * 0.22f), 2f, 0f, r * 0.06f, 20), paint)
                }
            }
            Emblem.Arch -> {
                drawPath(
                    Path().apply {
                        moveTo(-r * 0.55f, r)
                        lineTo(-r * 0.55f, -r * 0.1f)
                        arcTo(-r * 0.55f, -r * 0.65f, r * 0.55f, r * 0.45f, 180f, 180f, false)
                        lineTo(r * 0.55f, r)
                    },
                    paint,
                )
                drawCircle(0f, -r * 0.1f, r * 0.2f, paint)
            }
            Emblem.Leaf -> {
                drawPath(
                    Path().apply {
                        moveTo(0f, r)
                        quadTo(0f, 0f, 0f, -r)
                    },
                    paint,
                )
                for (i in 0 until 4) {
                    val yy = r * 0.6f - i * r * 0.45f
                    val side = if (i % 2 == 1) 1f else -1f
                    withSave {
                        translate(side * r * 0.28f, yy)
                        rotate(toDegrees((side * -0.5f).toDouble()).toFloat())
                        drawOval(-r * 0.3f, -r * 0.1f, r * 0.3f, r * 0.1f, paint)
                    }
                }
            }
            Emblem.Wave -> {
                for (j in 0 until 3) {
                    drawPath(
                        wavePath(r, -r * 0.4f + j * r * 0.4f, 1.5f, j.toFloat(), r * 0.14f, 24),
                        paint,
                    )
                }
            }
        }
    }
}

private fun wavePath(
    r: Float,
    y: Float,
    cycles: Float,
    phase: Float,
    amplitude: Float,
    steps: Int,
) =
    Path().apply {
        for (i in 0..steps) {
            val x = -r + i / steps.toFloat() * 2f * r
            val yy = y + sin(i / steps.toFloat() * TAU * cycles + phase) * amplitude
            if (i == 0) moveTo(x, yy) else lineTo(x, yy)
        }
    }

private fun Canvas.drawBackBoard(book: ShelfBookVisual, w: Float, h: Float, kit: ShelfKit) {
    drawRect(0f, 0f, w, h, fillPaint(argb(book.style.base)))
    if (book.style.binding == Binding.Cloth) {
        texture(kit.noise.weave, 5f, 0f, 0f, w, h, 0.5f)
    } else {
        texture(kit.noise.grain, 14f, 0f, 0f, w, h, 0.5f)
    }
}

private fun Canvas.drawEdges(
    book: ShelfBookVisual,
    w: Float,
    h: Float,
    alongWidth: Boolean,
    headband: Boolean,
    random: ShelfRandom,
) {
    withSave {
        clipRect(0f, 0f, w, h)
        val cloth = book.style.binding == Binding.Cloth
        drawRect(0f, 0f, w, h, fillPaint(argb(if (cloth) PAGE_EDGE else PAPER_EDGE)))
        val line =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 0.25f
            }
        val count = ((if (alongWidth) w else h) / 0.7f).roundToInt()
        for (i in 0 until count) {
            val p = (i + random.next() * 0.5f) * 0.7f
            line.color = argb(0x786446, 0.18f * (0.4f + random.next() * 0.6f))
            if (alongWidth) drawLine(p, 0f, p, h, line) else drawLine(0f, p, w, p, line)
        }
        val board = if (cloth) 1.5f else 0.45f
        val boardPaint = fillPaint(shade(book.style.base, 0.8f))
        if (alongWidth) {
            drawRect(0f, 0f, board, h, boardPaint)
            drawRect(w - board, 0f, w, h, boardPaint)
        } else {
            drawRect(0f, 0f, w, board, boardPaint)
            drawRect(0f, h - board, w, h, boardPaint)
        }
        if (headband && cloth) {
            var x = 0f
            var stripe = 0
            while (x < w) {
                val color = if (stripe % 2 == 0) argb(book.style.accent) else argb(0xFAF0DC)
                drawRect(x, 0f, x + 0.9f, 2.2f, fillPaint(color))
                x += 1.6f
                stripe++
            }
        }
        drawRect(
            0f,
            0f,
            w,
            h,
            Paint().apply {
                shader =
                    verticalGradient(0f, h, 0f to argb(0x5A4628, 0.12f), 1f to argb(0x5A4628, 0f))
            },
        )
    }
}

private fun Canvas.drawPage(
    book: ShelfBookVisual,
    content: PageContent,
    rightSide: Boolean,
    ribbon: Boolean,
    w: Float,
    h: Float,
    metrics: PageMetrics,
    kit: ShelfKit,
    typeset: PageTypeset? = null,
    bookSetting: BookPageSetting? = null,
) {
    withSave {
        clipRect(0f, 0f, w, h)
        drawRect(0f, 0f, w, h, fillPaint(argb(PAGE_PAPER)))
        texture(kit.noise.grain, 10f, 0f, 0f, w, h, 0.18f, BlendMode.MULTIPLY)
        val margin = w * PageGeometry.MARGIN
        val left = if (rightSide) w * PageGeometry.GUTTER_MARGIN else margin
        val textWidth = w - margin * 2f
        when (content) {
            is PageContent.Passage -> drawPassage(content, left, textWidth, h, metrics, kit)
            is PageContent.TitlePage -> drawTitlePage(content, w, h, textWidth, kit)
            PageContent.Endpaper -> drawEndpaper(book, w, h, kit)
            PageContent.Blank -> Unit
            is PageContent.Book -> bookSetting?.let { drawBookPage(content, it, w) }
        }
        typeset?.let { drawTypeset(it, w) }
        if (ribbon) drawPageRibbon(w, h)
        val spine = if (rightSide) 0f else w
        val inward = if (rightSide) 1f else -1f
        drawRect(0f, 0f, w, h, Paint().apply { shader = gutterShader(spine, inward * w) })
        drawRect(0f, 0f, w, h, Paint().apply { shader = creaseShader(spine, inward * w) })
        if (!rightSide) {
            drawRect(0f, 0f, w, h, Paint().apply { shader = turnAwayShader(spine, inward * w) })
        }
        val foreEdge =
            if (rightSide) horizontalGradient(w, w * 0.94f, 0f to white(0.25f), 1f to white(0f))
            else horizontalGradient(0f, w * 0.06f, 0f to white(0.25f), 1f to white(0f))
        drawRect(0f, 0f, w, h, Paint().apply { shader = foreEdge })
    }
}

private fun Canvas.drawPassage(
    passage: PageContent.Passage,
    left: Float,
    textWidth: Float,
    h: Float,
    metrics: PageMetrics,
    kit: ShelfKit,
) {
    val header =
        TextPaint(TEXT_FLAGS).apply {
            typeface = kit.fonts.serif
            textSize = metrics.captionSize
            letterSpacing = metrics.captionSpacingEm
            color = argb(PAGE_CAPTION, 0.8f)
            textAlign = Paint.Align.CENTER
        }
    drawText(
        ellipsize(passage.heading.uppercase(), header, textWidth),
        left + textWidth / 2f,
        h * PageGeometry.HEADING_BASELINE,
        header,
    )
    passage.folio?.let {
        drawText(it, left + textWidth / 2f, h * PageGeometry.FOLIO_BASELINE, header)
    }
    val body =
        TextPaint(TEXT_FLAGS).apply {
            typeface = kit.fonts.serif
            color = argb(PAGE_INK, 0.92f)
            textSize = metrics.bodySize
        }
    val glyph = body.fontMetrics.let { it.descent - it.ascent }
    val topLeading = (metrics.lineHeight - glyph) * (-body.fontMetrics.ascent / glyph)
    val layout = passageLayout(passage.paragraphs, body, textWidth, metrics)
    // StaticLayout puts the first baseline an ascent below the top leading; Compose centres the
    // first line in its height, so the layout is moved to put its baseline there.
    val shift = topLeading - body.fontMetrics.ascent - layout.firstBaseline
    withSave {
        translate(left, h * PageGeometry.BODY_TOP)
        clipRect(0f, 0f, textWidth, h * PageGeometry.BODY_HEIGHT)
        translate(0f, shift)
        TextPainter.paint(compose, layout)
    }
}

private fun Canvas.drawBookPage(content: PageContent.Book, setting: BookPageSetting, w: Float) {
    val layout = setting.layout(setting.pageText(content.page, content.words).second)
    withSave {
        val scale = w / setting.pageWidth
        scale(scale, scale)
        // The page's setting draws in a DrawScope, here over this canvas.
        CanvasDrawScope().draw(
            setting.density,
            LayoutDirection.Ltr,
            compose,
            Size(setting.pageWidth, setting.pageHeight),
        ) {
            with(setting) {
                drawPage(content.page, layout, content.chapterName, content.folio, content.withBody)
            }
        }
    }
}

private fun Canvas.drawTypeset(typeset: PageTypeset, w: Float) {
    val unitsPerPx = w / typeset.pageWidth
    withSave {
        scale(unitsPerPx, unitsPerPx)
        val canvas = compose
        for ((layout, origin) in
            listOf(
                typeset.heading to typeset.headingOrigin,
                typeset.body to typeset.bodyOrigin,
            )) {
            withSave {
                translate(origin.x, origin.y)
                TextPainter.paint(canvas, layout)
            }
        }
    }
}

// The source's justified StaticLayout, its lines metrics.lineHeight apart, each paragraph after the
// first indented.
private fun passageLayout(
    paragraphs: List<String>,
    paint: TextPaint,
    width: Float,
    metrics: PageMetrics,
): TextLayoutResult {
    val indent = (metrics.bodySize * PageGeometry.INDENT_EM).roundToInt().toFloat()
    val text = buildAnnotatedString {
        paragraphs.forEachIndexed { i, paragraph ->
            val style =
                ParagraphStyle(textIndent = TextIndent(firstLine = if (i == 0) 0.sp else indent.sp))
            val at = pushStyle(style)
            append(paragraph)
            pop(at)
        }
    }
    val face = requireNotNull(paint.typeface)
    return face.measurer.measure(
        text,
        TextStyle(
            color = Color(paint.color),
            fontFamily = face.family,
            fontSize = paint.textSize.sp,
            lineHeight = metrics.lineHeight.sp,
            lineHeightStyle =
                LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            textAlign = TextAlign.Justify,
        ),
        constraints = Constraints(maxWidth = width.roundToInt().coerceAtLeast(1)),
        density = Density(1f, 1f),
    )
}

private fun Canvas.drawTitlePage(
    page: PageContent.TitlePage,
    w: Float,
    h: Float,
    textWidth: Float,
    kit: ShelfKit,
) {
    val size = w * 0.085f
    val title =
        TextPaint(TEXT_FLAGS).apply {
            typeface = kit.fonts.serif
            textSize = size
            letterSpacing = 0.08f
            color = argb(PAGE_INK, 0.92f)
            textAlign = Paint.Align.CENTER
        }
    val fitted = title.fitTitle(page.title.uppercase(), textWidth, maxLines = 4, size)
    val step = fitted.size * 1.25f
    fitted.lines.forEachIndexed { i, line -> drawText(line, w / 2f, h * 0.3f + i * step, title) }
    val ruleY = h * 0.3f + fitted.lines.size * step
    drawLine(
        w * 0.42f,
        ruleY,
        w * 0.58f,
        ruleY,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 0.5f
            color = argb(0x3C2E22, 0.5f)
        },
    )
    title.typeface = kit.fonts.serifItalic
    title.letterSpacing = 0f
    title.textSize = size * 0.62f
    drawText(page.author, w / 2f, ruleY + size * 1.3f, title)
    page.credit?.let {
        title.typeface = kit.fonts.serif
        title.textSize = w * 0.034f
        title.letterSpacing = 0.1f
        title.color = argb(0x3C2E22, 0.75f)
        drawText(it.uppercase(), w / 2f, h * 0.9f, title)
    }
}

private fun Canvas.drawEndpaper(book: ShelfBookVisual, w: Float, h: Float, kit: ShelfKit) {
    drawRect(0f, 0f, w, h, fillPaint(argb(mixRgb(book.style.base, 0xF6EEDE, 0.78f))))
    texture(kit.noise.grain, 10f, 0f, 0f, w, h, 0.2f, BlendMode.MULTIPLY)
    val line =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.55f
            color = argb(mixRgb(book.style.base, 0xF6EEDE, 0.3f), 0.5f)
        }
    var y = 4f
    while (y < h) {
        val path = Path()
        var x = 0f
        while (x <= w) {
            val yy = y + sin(x / w * TAU * 3f + y * 0.37f) * 1.6f
            if (x == 0f) path.moveTo(x, yy) else path.lineTo(x, yy)
            x += 2f
        }
        drawPath(path, line)
        y += 6f
    }
}

private fun Canvas.drawPageRibbon(w: Float, h: Float) {
    val x = w * PAGE_RIBBON_X
    fun ribbonPath() =
        Path().apply {
            moveTo(x, -2f)
            cubicTo(w * 0.07f, h * 0.35f, w * 0.1f, h * 0.62f, w * PAGE_RIBBON_FOOT_X, h + 2f)
        }
    val shadow =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.028f
            color = argb(0x28140A, 0.28f)
            maskFilter = BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL)
        }
    withSave {
        translate(1.2f, 1.4f)
        drawPath(ribbonPath(), shadow)
    }
    drawPath(
        ribbonPath(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * PAGE_RIBBON_WIDTH
            shader =
                horizontalGradient(
                    x - PAGE_RIBBON_SHADE_BEFORE,
                    x - PAGE_RIBBON_SHADE_BEFORE + PAGE_RIBBON_SHADE_SPAN,
                    PAGE_RIBBON_STOPS[0] to shade(RIBBON_RED, PAGE_RIBBON_SHADES[0]),
                    PAGE_RIBBON_STOPS[1] to shade(RIBBON_RED, PAGE_RIBBON_SHADES[1]),
                    PAGE_RIBBON_STOPS[2] to shade(RIBBON_RED, PAGE_RIBBON_SHADES[2]),
                )
        },
    )
}

// [title] set in this paint to fit, as small as [MIN_TITLE_SCALE] of [size]; the paint is left at
// the size it fitted at.
private fun Paint.fitTitle(title: String, maxWidth: Float, maxLines: Int, size: Float): CoverTitle {
    val fitted =
        fitCoverTitle(title, maxWidth, maxLines, size, size * MIN_TITLE_SCALE) { line, s ->
            textSize = s
            measureText(line)
        }
    textSize = fitted.size
    return fitted
}

private fun centreCrop(bitmap: Bitmap, aspect: Float): Rect {
    val source = bitmap.width.toFloat() / bitmap.height
    return if (source > aspect) {
        val w = (bitmap.height * aspect).roundToInt()
        val x = (bitmap.width - w) / 2
        Rect(x, 0, x + w, bitmap.height)
    } else {
        val h = (bitmap.width / aspect).roundToInt()
        val y = (bitmap.height - h) / 2
        Rect(0, y, bitmap.width, y + h)
    }
}

private fun gutterShader(spine: Float, across: Float): Shader {
    val stops = FloatArray(GUTTER_SHADE_STOPS) { it / (GUTTER_SHADE_STOPS - 1f) }
    return LinearGradient(
        spine,
        0f,
        spine + across * GUTTER_SHADE_SPAN,
        0f,
        IntArray(stops.size) {
            val near = 1f - stops[it]
            argb(GUTTER_SHADE, GUTTER_SHADE_ALPHA * near * near)
        },
        stops,
        Shader.TileMode.CLAMP,
    )
}

private fun creaseShader(spine: Float, across: Float): Shader =
    LinearGradient(
        spine,
        0f,
        spine + across * CREASE_SPAN,
        0f,
        argb(CREASE_SHADE, CREASE_ALPHA),
        argb(CREASE_SHADE, 0f),
        Shader.TileMode.CLAMP,
    )

private fun turnAwayShader(spine: Float, across: Float): Shader =
    LinearGradient(
        spine,
        0f,
        spine + across * TURN_AWAY_SPAN,
        0f,
        argb(GUTTER_SHADE, TURN_AWAY_ALPHA),
        argb(GUTTER_SHADE, 0f),
        Shader.TileMode.CLAMP,
    )
