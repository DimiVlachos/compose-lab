package dev.dimvlachos.lab.core.presentation.components.bookshelf.book

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookPage
import dev.dimvlachos.lab.core.presentation.ui.BookshelfArt
import kotlin.math.floor
import kotlin.math.roundToInt

private const val OPENING_TITLE_BASELINE = 0.2f
private const val OPENING_BODY_TOP = 0.3f
private const val OPENING_TITLE_SCALE = 1.6f

/** Where a page's text sits, as fractions of the page. */
internal object PageGeometry {
    const val MARGIN = 0.13f
    const val GUTTER_MARGIN = 0.1365f
    const val HEADING_BASELINE = 0.072f
    const val BODY_TOP = 0.1f
    const val BODY_HEIGHT = 0.78f
    const val FOLIO_BASELINE = 0.95f
    const val INDENT_EM = 1.4f
}

/**
 * How a page of [pageWidth] by [pageHeight] px is set: justified [serif] at [textSize] px on
 * [lineHeight] px lines, every paragraph but a chapter's first indented, and a spaced caption
 * heading the page. One setting both counts the lines a chapter takes and draws its pages, so the
 * two always agree.
 */
internal data class BookPageSetting(
    val pageWidth: Float,
    val pageHeight: Float,
    val textSize: Float,
    val lineHeight: Float,
    val captionSize: Float,
    val captionSpacingEm: Float,
    val serif: FontFamily,
    val measurer: TextMeasurer,
    val density: Density,
) {
    val textLeft: Int = (pageWidth * PageGeometry.GUTTER_MARGIN).roundToInt()
    val textWidth: Int = (pageWidth * (1f - PageGeometry.MARGIN * 2f)).roundToInt()
    val indent: Int = (textSize * PageGeometry.INDENT_EM).roundToInt()
    val bodyTop: Int = (pageHeight * PageGeometry.BODY_TOP).roundToInt()
    val openingBodyTop: Int = (pageHeight * OPENING_BODY_TOP).roundToInt()
    private val bodyBottom = pageHeight * (PageGeometry.BODY_TOP + PageGeometry.BODY_HEIGHT)
    val linesPerPage: Int = floor((bodyBottom - bodyTop) / lineHeight).toInt().coerceAtLeast(1)
    val openingLines: Int =
        floor((bodyBottom - openingBodyTop) / lineHeight).toInt().coerceAtLeast(1)

    private fun px(value: Float): TextUnit = with(density) { value.toSp() }

    // The source's StaticLayout: justified between words, no hyphens, the best line breaks, and
    // each line exactly lineHeight tall.
    val bodyStyle: TextStyle =
        TextStyle(
            color = BookshelfArt.BookInk,
            fontFamily = serif,
            fontSize = px(textSize),
            lineHeight = px(lineHeight),
            lineHeightStyle =
                LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            textAlign = TextAlign.Justify,
            hyphens = Hyphens.None,
            lineBreak = LineBreak.Paragraph,
        )

    private val indented = ParagraphStyle(textIndent = TextIndent(firstLine = px(indent.toFloat())))

    fun layout(text: AnnotatedString): TextLayoutResult =
        measurer.measure(
            text,
            bodyStyle,
            constraints = Constraints(maxWidth = textWidth),
            density = density,
        )

    fun lineCounts(paragraphs: List<String>): List<Int> =
        paragraphs.mapIndexed { index, paragraph ->
            layout(styled(paragraph, indented = index > 0)).lineCount
        }

    fun pageText(page: BookPage, words: List<List<String>>): Pair<PageText, AnnotatedString> =
        PageText(words.subList(page.firstParagraph, page.lastParagraph + 1), page.firstParagraph)
            .let { text -> text to annotated(text, page) }

    // Highlights and the narrated word are left out: this book is read, not listened to. Each
    // paragraph is its own ParagraphStyle, which breaks the line between them as the source's '\n'.
    fun annotated(text: PageText, page: BookPage): AnnotatedString = buildAnnotatedString {
        text.paragraphStarts.forEachIndexed { i, start ->
            val end = text.paragraphStarts.getOrNull(i + 1)?.minus(1) ?: text.text.length
            val paragraph = text.text.substring(start, end)
            if (page.firstParagraph + i > 0) withParagraph(indented, paragraph)
            else withParagraph(ParagraphStyle(), paragraph)
        }
    }

    fun bodyOrigin(page: BookPage, layout: TextLayoutResult): Pair<Int, Int> {
        val top = if (page.opensChapter) openingBodyTop else bodyTop
        return top to top - layout.getLineTop(page.firstLine).roundToInt()
    }

    fun DrawScope.drawPage(
        page: BookPage,
        layout: TextLayoutResult,
        chapterName: String,
        folio: String?,
        includeBody: Boolean = true,
    ) {
        val caption =
            TextStyle(
                color = BookshelfArt.BookCaptionInk,
                fontFamily = serif,
                fontSize = px(captionSize),
                letterSpacing = captionSpacingEm.em,
                textAlign = TextAlign.Center,
            )
        if (page.opensChapter) {
            val title =
                caption.copy(
                    color = BookshelfArt.BookInk,
                    fontSize = px(textSize * OPENING_TITLE_SCALE),
                    letterSpacing = 0.sp,
                )
            drawCentredLine(chapterName, title, pageHeight * OPENING_TITLE_BASELINE)
        } else {
            drawCentredLine(
                chapterName.uppercase(),
                caption,
                pageHeight * PageGeometry.HEADING_BASELINE,
            )
        }
        folio?.let { drawCentredLine(it, caption, pageHeight * PageGeometry.FOLIO_BASELINE) }
        if (!includeBody) return
        val (top, offset) = bodyOrigin(page, layout)
        clipRect(
            left = textLeft.toFloat(),
            top = top.toFloat(),
            right = (textLeft + textWidth).toFloat(),
            bottom = top + page.lineCount * lineHeight,
        ) {
            translate(textLeft.toFloat(), offset.toFloat()) { drawText(layout) }
        }
    }

    // One line, cut short with an ellipsis, centred over the text with its baseline at [baseline],
    // as Canvas.drawText with a centred Paint draws it.
    private fun DrawScope.drawCentredLine(text: String, style: TextStyle, baseline: Float) {
        val line =
            measurer.measure(
                text,
                style,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints.fixedWidth(textWidth),
                density = this@BookPageSetting.density,
            )
        drawText(line, topLeft = Offset(textLeft.toFloat(), baseline - line.firstBaseline))
    }

    private fun styled(paragraph: String, indented: Boolean): AnnotatedString =
        buildAnnotatedString {
            withParagraph(
                if (indented) this@BookPageSetting.indented else ParagraphStyle(),
                paragraph,
            )
        }
}

private fun AnnotatedString.Builder.withParagraph(style: ParagraphStyle, text: String) {
    val at = pushStyle(style)
    append(text)
    pop(at)
}
