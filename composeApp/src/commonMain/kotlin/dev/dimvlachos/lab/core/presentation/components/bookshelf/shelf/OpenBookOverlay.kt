package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookPageSetting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.RectF
import dev.dimvlachos.lab.core.presentation.ui.BookshelfArt
import kotlin.math.roundToInt

// The open page's narration overlay is left out; what the shelf needs to set its pages stays.

private val UntrimmedLines =
    LineHeightStyle(LineHeightStyle.Alignment.Proportional, LineHeightStyle.Trim.None)

internal data class OpenPageSlot(
    val bookId: Int,
    val left: Dp,
    val top: Dp,
    val width: Dp,
    val height: Dp,
    val showsText: Boolean = true,
    val bookSetting: BookPageSetting? = null,
    val facing: Boolean = false,
)

@Composable
internal fun rememberPageMetrics(): PageMetrics {
    val fontScale = LocalDensity.current.fontScale
    val body = ShelfTheme.typography.bookPage
    val caption = ShelfTheme.typography.bookPageCaption
    return remember(fontScale, body, caption) {
        PageMetrics(
            bodySize = body.fontSize.value * fontScale,
            lineHeight = body.lineHeight.value * fontScale,
            captionSize = caption.fontSize.value * fontScale,
            captionSpacingEm = caption.letterSpacing.value / caption.fontSize.value,
        )
    }
}

@Composable
internal fun rememberPageTypeset(passage: PageContent.Passage?, page: RectF?): PageTypeset? {
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val density = LocalDensity.current
    val headingStyle = pageHeadingStyle()
    val bodyStyle = pageBodyStyle()
    return remember(passage, page, density, headingStyle, bodyStyle) {
        if (passage == null || page == null) return@remember null
        with(density) {
            val width = page.width().dp
            val height = page.height().dp
            val textLeft = (width * PageGeometry.GUTTER_MARGIN).roundToPx()
            val textWidth = (width * (1f - PageGeometry.MARGIN * 2f)).roundToPx()
            val heading =
                measurer.measure(
                    AnnotatedString(passage.heading.uppercase()),
                    headingStyle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    constraints =
                        Constraints(
                            minWidth = textWidth,
                            maxWidth = textWidth,
                            maxHeight = height.roundToPx(),
                        ),
                )
            val body =
                measurer.measure(
                    pageBody(passage.paragraphs),
                    bodyStyle,
                    overflow = TextOverflow.Clip,
                    maxLines = bodyLines(height, bodyStyle),
                    constraints =
                        Constraints.fixed(
                            textWidth,
                            (height * PageGeometry.BODY_HEIGHT).roundToPx(),
                        ),
                )
            PageTypeset(
                passage = passage,
                pageWidth = width.toPx(),
                heading = heading,
                headingOrigin =
                    Offset(
                        textLeft.toFloat(),
                        ((height * PageGeometry.HEADING_BASELINE).toPx() -
                                heading.firstBaseline.roundToInt())
                            .roundToInt()
                            .toFloat(),
                    ),
                body = body,
                bodyOrigin =
                    Offset(
                        textLeft.toFloat(),
                        (height * PageGeometry.BODY_TOP).roundToPx().toFloat(),
                    ),
            )
        }
    }
}

@Composable
private fun pageHeadingStyle(): TextStyle =
    ShelfTheme.typography.bookPageCaption.copy(
        color = BookshelfArt.BookCaptionInk,
        textAlign = TextAlign.Center,
    )

@Composable
private fun pageBodyStyle(): TextStyle =
    ShelfTheme.typography.bookPage.copy(color = BookshelfArt.BookInk)

private fun pageBody(paragraphs: List<String>): AnnotatedString = buildAnnotatedString {
    paragraphs.forEachIndexed { index, paragraph ->
        val indent =
            if (index == 0) TextIndent.None else TextIndent(firstLine = PageGeometry.INDENT_EM.em)
        withStyle(
            ParagraphStyle(
                textAlign = TextAlign.Justify,
                textIndent = indent,
                lineHeightStyle = UntrimmedLines,
            )
        ) {
            append(paragraph)
        }
    }
}

private fun Density.bodyLines(pageHeight: Dp, style: TextStyle): Int =
    ((pageHeight * PageGeometry.BODY_HEIGHT).toPx() / style.lineHeight.toPx())
        .toInt()
        .coerceAtLeast(1)
