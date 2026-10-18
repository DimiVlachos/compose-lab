package dev.dimvlachos.lab.core.presentation.components.bookshelf.book

import androidx.compose.ui.text.TextLayoutResult
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookPage
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ParagraphLines
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.indexAtLine
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.paginateChapter
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.paragraphLines

internal class BookChapterPages(
    val key: Int,
    val setting: BookPageSetting,
    val words: List<List<String>>,
    val pages: List<BookPage>,
    private val lines: List<ParagraphLines>,
) {
    private var measured: Pair<Int, TextLayoutResult>? = null

    fun pageAt(ref: WordRef?): Int {
        val paragraph = ref?.paragraph?.takeIf { it in words.indices } ?: return 0
        val text = PageText(listOf(words[paragraph]), paragraph)
        val offset = text.rangeOf(ref)?.first ?: 0
        // A paragraph's first word is on its first line: nothing to lay out, so this can be asked
        // from any thread.
        if (offset == 0) return pages.indexAtLine(lines[paragraph].firstLine, lines)
        val layout =
            measured?.takeIf { it.first == paragraph }?.second
                ?: setting
                    .layout(
                        setting.annotated(
                            text,
                            BookPage(0, paragraph, paragraph, 0, 0, opensChapter = false),
                        )
                    )
                    .also { measured = paragraph to it }
        return pages.indexAtLine(
            lines[paragraph].firstLine + layout.getLineForOffset(offset),
            lines,
        )
    }
}

internal class ChapterPaginator {
    private var cached: BookChapterPages? = null

    fun paginate(setting: BookPageSetting, key: Int, words: List<List<String>>): BookChapterPages {
        cached
            ?.takeIf { it.key == key && it.setting == setting }
            ?.let {
                return it
            }
        val counts = setting.lineCounts(words.map { it.joinToString(" ") })
        return BookChapterPages(
                key = key,
                setting = setting,
                words = words,
                pages = paginateChapter(0, counts, setting.linesPerPage, setting.openingLines),
                lines = paragraphLines(counts),
            )
            .also { cached = it }
    }
}
