package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

internal data class BookPage(
    val chapter: Int,
    val firstParagraph: Int,
    val lastParagraph: Int,
    val firstLine: Int,
    val lineCount: Int,
    val opensChapter: Boolean,
)

internal data class ParagraphLines(val firstLine: Int, val lineCount: Int)

internal fun paragraphLines(lineCounts: List<Int>): List<ParagraphLines> {
    var line = 0
    return lineCounts.map { count -> ParagraphLines(line, count).also { line += count } }
}

internal fun paginateChapter(
    chapter: Int,
    lineCounts: List<Int>,
    linesPerPage: Int,
    openingLines: Int,
): List<BookPage> {
    require(linesPerPage > 0 && openingLines > 0)
    val paragraphs = paragraphLines(lineCounts)
    val total = lineCounts.sum()
    if (total == 0) return listOf(BookPage(chapter, 0, -1, 0, 0, opensChapter = true))
    val pages = mutableListOf<BookPage>()
    var line = 0
    while (line < total) {
        val capacity = if (pages.isEmpty()) openingLines else linesPerPage
        val end = minOf(line + capacity, total)
        val first = paragraphs.indexOfLast { it.firstLine <= line }
        val last = paragraphs.indexOfLast { it.firstLine < end }
        pages +=
            BookPage(
                chapter = chapter,
                firstParagraph = first,
                lastParagraph = last,
                firstLine = line - paragraphs[first].firstLine,
                lineCount = end - line,
                opensChapter = pages.isEmpty(),
            )
        line = end
    }
    return pages
}

internal fun List<BookPage>.indexAtLine(line: Int, lines: List<ParagraphLines>): Int =
    indexOfLast { page ->
        (lines.getOrNull(page.firstParagraph)?.firstLine ?: 0) + page.firstLine <= line
    }
    .coerceAtLeast(0)
