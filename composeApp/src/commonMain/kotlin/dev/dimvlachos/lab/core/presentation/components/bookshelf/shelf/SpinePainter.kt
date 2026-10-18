package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Binding
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.PaperDesign
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfBookStyle
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Paint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Path
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.TextPaint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Typeface
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.ellipsize
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withSave

private const val MIN_SPINE_TEXT = 6.5f
private const val CLOTH_SPACING = 0.12f

internal data class ShelfBookVisual(
    val id: Int,
    val title: String,
    val author: String,
    val style: ShelfBookStyle,
)

internal fun Canvas.drawSpine(book: ShelfBookVisual, t: Float, h: Float, kit: ShelfKit) {
    withSave {
        clipRect(0f, 0f, t, h)
        drawRect(0f, 0f, t, h, fillPaint(argb(book.style.base)))
        when (book.style.binding) {
            Binding.Cloth -> drawClothSpine(book, t, h, kit)
            Binding.Paper -> drawPaperSpine(book, t, h, kit)
        }
    }
}

private fun Canvas.drawClothSpine(book: ShelfBookVisual, t: Float, h: Float, kit: ShelfKit) {
    texture(kit.noise.weave, 5f, 0f, 0f, t, h, 0.55f)
    val bands = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = giltShader(0f, 0f, t, 0f) }
    for (y in floatArrayOf(9f, 12.5f, h - 13.5f, h - 10f)) drawRect(2f, y, t - 2f, y + 0.9f, bands)

    val length = h - 62f
    val titlePaint =
        textPaint(kit.fonts.serif, letterSpacing = CLOTH_SPACING).apply { isFakeBoldText = true }
    val authorPaint = textPaint(kit.fonts.serifItalic)
    val line =
        clothLine(
            book.title.uppercase(),
            book.author,
            minOf(t * 0.38f, 13f),
            length,
            titlePaint,
            authorPaint,
        )
    withSave {
        translate(t / 2f, 24f)
        rotate(90f)
        drawGilt(line.title, 0f, titlePaint, line.size)
        if (line.author != null) {
            val x = titlePaint.measureText(line.title) + line.size * 1.3f
            drawGilt(line.author, x, authorPaint, line.size)
        }
    }

    val mark =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
            shader = giltShader(0f, h - 30f, 0f, h - 20f)
        }
    drawCircle(t / 2f, h - 25f, minOf(4.2f, t * 0.14f), mark)
    drawPath(
        Path().apply {
            moveTo(t / 2f - 2.6f, h - 24.4f)
            quadTo(t / 2f, h - 27f, t / 2f + 2.6f, h - 24.4f)
        },
        mark,
    )
    drawRect(
        0f,
        0f,
        t,
        h,
        Paint().apply {
            shader =
                horizontalGradient(
                    0f,
                    t,
                    0f to black(0.42f),
                    0.1f to black(0.12f),
                    0.3f to argb(0xFFF0DC, 0.16f),
                    0.55f to black(0f),
                    0.88f to black(0.16f),
                    1f to black(0.45f),
                )
        },
    )
    drawRect(
        0f,
        0f,
        t,
        h,
        Paint().apply {
            shader =
                verticalGradient(
                    0f,
                    h,
                    0f to argb(0xFFF0D7, 0.22f),
                    0.015f to black(0.18f),
                    0.035f to black(0f),
                    0.965f to black(0f),
                    0.985f to black(0.2f),
                    1f to argb(0xFFF0D7, 0.2f),
                )
        },
    )
    drawRect(0.5f, 0f, t - 0.5f, 0.6f, fillPaint(argb(0xFFECCD, 0.35f)))
}

private fun Canvas.drawPaperSpine(book: ShelfBookVisual, t: Float, h: Float, kit: ShelfKit) {
    texture(kit.noise.grain, 14f, 0f, 0f, t, h, 0.25f)
    val ink = argb(book.style.ink)
    val accent = argb(book.style.accent)
    when (book.style.paperDesign) {
        PaperDesign.Block -> {
            drawRect(0f, h * 0.2f, t, h * 0.8f, fillPaint(ink))
            drawCircle(t / 2f, h * 0.1f, t * 0.2f, fillPaint(accent))
            drawRotatedFitted(
                book.title.uppercase(),
                kit.fonts.sansBold,
                t * 0.44f,
                h * 0.54f,
                t / 2f,
                h * 0.23f,
                argb(book.style.base),
            )
            drawRotatedAuthor(
                book.author,
                upper = true,
                kit.fonts.sans,
                t * 0.3f,
                h * 0.15f,
                t / 2f,
                h * 0.835f,
                ink,
            )
        }
        PaperDesign.Rule -> {
            val rule = fillPaint(argb(0x1E140A, 0.85f))
            drawRect(2f, 7f, t - 2f, 7.8f, rule)
            drawRect(2f, h - 8f, t - 2f, h - 7.2f, rule)
            val used =
                drawRotatedFitted(
                    book.title,
                    kit.fonts.serifItalic,
                    t * 0.6f,
                    h * 0.62f,
                    t / 2f,
                    18f,
                    ink,
                )
            drawRotatedAuthor(
                book.author,
                upper = true,
                kit.fonts.sans,
                t * 0.28f,
                h - 30f - used - 12f,
                t / 2f,
                18f + used + 12f,
                ink,
            )
        }
        PaperDesign.Bands -> {
            drawRect(0f, 0f, t, h * 0.17f, fillPaint(ink))
            drawRect(0f, h * 0.87f, t, h, fillPaint(ink))
            drawCircle(t / 2f, h * 0.085f, t * 0.22f, fillPaint(accent))
            val used =
                drawRotatedFitted(
                    book.title.uppercase(),
                    kit.fonts.sansBold,
                    t * 0.42f,
                    h * 0.5f,
                    t / 2f,
                    h * 0.21f,
                    ink,
                )
            drawRotatedAuthor(
                book.author,
                upper = false,
                kit.fonts.sans,
                t * 0.3f,
                h * 0.62f - used - 10f,
                t / 2f,
                h * 0.21f + used + 10f,
                argb(0x282832, 0.7f),
            )
        }
    }
    val crease =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = white(0.4f)
        }
    for (i in 0 until 3) {
        val x = t * (0.3f + i * 0.18f)
        val y0 = h * (0.28f + i * 0.07f)
        drawPath(
            Path().apply {
                moveTo(x, y0)
                cubicTo(x + 1f, y0 + 30f, x - 1f, y0 + 60f, x + 0.5f, y0 + 90f)
            },
            crease,
        )
    }
    drawRect(
        0f,
        0f,
        t,
        h,
        Paint().apply {
            shader =
                horizontalGradient(
                    0f,
                    t,
                    0f to black(0.3f),
                    0.08f to black(0.06f),
                    0.32f to white(0.14f),
                    0.5f to white(0f),
                    0.92f to black(0.1f),
                    1f to black(0.32f),
                )
        },
    )
}

private data class SpineLine(val title: String, val author: String?, val size: Float)

private fun clothLine(
    title: String,
    author: String,
    start: Float,
    length: Float,
    titlePaint: TextPaint,
    authorPaint: TextPaint,
): SpineLine {
    val both =
        fitSize(start, 0.95f) { s ->
            titlePaint.textSize = s
            authorPaint.textSize = s * 0.78f
            titlePaint.measureText(title) + authorPaint.measureText(author) + s * 1.3f <= length
        }
    if (both >= MIN_SPINE_TEXT) {
        titlePaint.textSize = both
        authorPaint.textSize = both * 0.78f
        return SpineLine(title, author, both)
    }
    val surname = author.substringAfterLast(' ')
    val withSurname =
        fitSize(start, 0.95f) { s ->
            titlePaint.textSize = s
            authorPaint.textSize = s * 0.78f
            titlePaint.measureText(title) + authorPaint.measureText(surname) + s * 1.3f <= length
        }
    if (withSurname >= MIN_SPINE_TEXT) {
        titlePaint.textSize = withSurname
        authorPaint.textSize = withSurname * 0.78f
        return SpineLine(title, surname, withSurname)
    }
    val alone =
        fitSize(start, 0.95f) { s ->
            titlePaint.textSize = s
            titlePaint.measureText(title) <= length
        }
    val size = alone.coerceAtLeast(MIN_SPINE_TEXT)
    titlePaint.textSize = size
    val shown = ellipsize(title, titlePaint, length)
    return SpineLine(shown, null, size)
}

internal fun fitSize(start: Float, shrink: Float, fits: (Float) -> Boolean): Float {
    var size = start
    var attempts = 0
    while (attempts < 30 && !fits(size)) {
        size *= shrink
        attempts++
    }
    return size
}

private fun Canvas.drawGilt(text: String, x: Float, paint: TextPaint, size: Float) {
    val baseline = -(paint.ascent() + paint.descent()) / 2f
    paint.shader = null
    paint.color = black(0.5f)
    drawText(text, x, baseline + 0.5f, paint)
    paint.shader = giltShader(0f, -size / 2f, 0f, size / 2f)
    drawText(text, x, baseline, paint)
    paint.shader = null
}

private fun Canvas.drawRotatedFitted(
    text: String,
    typeface: Typeface,
    maxSize: Float,
    maxLength: Float,
    cx: Float,
    startY: Float,
    color: Int,
): Float {
    if (maxLength <= 0f) return 0f
    val paint = textPaint(typeface)
    paint.color = color
    val size =
        fitSize(maxSize, 0.94f) { s ->
            paint.textSize = s
            paint.measureText(text) <= maxLength
        }
    paint.textSize = size.coerceAtLeast(MIN_SPINE_TEXT)
    val shown = ellipsize(text, paint, maxLength)
    withSave {
        translate(cx, startY)
        rotate(90f)
        drawText(shown, 0f, -(paint.ascent() + paint.descent()) / 2f, paint)
    }
    return paint.measureText(shown)
}

private fun Canvas.drawRotatedAuthor(
    author: String,
    upper: Boolean,
    typeface: Typeface,
    maxSize: Float,
    maxLength: Float,
    cx: Float,
    startY: Float,
    color: Int,
) {
    if (maxLength <= 0f) return
    val paint = textPaint(typeface).apply { this.color = color }
    val variants =
        listOf(author, author.substringAfterLast(' ')).distinct().map {
            if (upper) it.uppercase() else it
        }
    for (variant in variants) {
        val size =
            fitSize(maxSize, 0.94f) { s ->
                paint.textSize = s
                paint.measureText(variant) <= maxLength
            }
        if (size >= MIN_SPINE_TEXT) {
            paint.textSize = size
            withSave {
                translate(cx, startY)
                rotate(90f)
                drawText(variant, 0f, -(paint.ascent() + paint.descent()) / 2f, paint)
            }
            return
        }
    }
}

private fun textPaint(typeface: Typeface, letterSpacing: Float = 0f) =
    TextPaint(TEXT_FLAGS).apply {
        this.typeface = typeface
        this.letterSpacing = letterSpacing
    }
