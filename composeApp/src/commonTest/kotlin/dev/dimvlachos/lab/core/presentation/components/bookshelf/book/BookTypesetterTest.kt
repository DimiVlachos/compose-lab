package dev.dimvlachos.lab.core.presentation.components.bookshelf.book

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BookTypesetterTest {
    private val sentence =
        "The tide came up the channel past the cotton sheds and the ferry landing, slow and brown."

    private fun chapter(paragraphs: Int, sentences: Int = 5): List<List<String>> =
        List(paragraphs) { List(sentences) { sentence }.joinToString(" ").split(" ") }

    private fun withSetting(block: (BookPageSetting) -> Unit) = runComposeUiTest {
        lateinit var measurer: TextMeasurer
        setContent { measurer = rememberTextMeasurer() }
        runOnIdle {
            // A phone's page: 300 by 460 dp at 3x, the source's body and caption sizes.
            val density = Density(3f)
            block(
                BookPageSetting(
                    pageWidth = 900f,
                    pageHeight = 1380f,
                    textSize = 48f,
                    lineHeight = 69f,
                    captionSize = 33f,
                    captionSpacingEm = 1.6f / 11f,
                    serif = FontFamily.Serif,
                    measurer = measurer,
                    density = density,
                )
            )
        }
    }

    @Test
    fun everyLineOfTheChapterIsOnExactlyOnePage() = withSetting { setting ->
        val words = chapter(12)
        val pages = ChapterPaginator().paginate(setting, key = 1, words).pages
        val counts = setting.lineCounts(words.map { it.joinToString(" ") })
        // A page's first line counts from the start of its first paragraph.
        val paragraphStart = counts.runningFold(0) { at, n -> at + n }
        fun start(page: BookPage) = paragraphStart[page.firstParagraph] + page.firstLine

        assertEquals(counts.sum(), pages.sumOf { it.lineCount })
        pages.zipWithNext { a, b -> assertEquals(start(a) + a.lineCount, start(b)) }
    }

    @Test
    fun noPageHoldsMoreLinesThanItsBodyHas() = withSetting { setting ->
        val pages = ChapterPaginator().paginate(setting, key = 1, chapter(12)).pages

        assertTrue(pages.size > 2)
        pages.forEachIndexed { i, page ->
            val room = if (page.opensChapter) setting.openingLines else setting.linesPerPage
            assertTrue(page.lineCount <= room, "page $i: ${page.lineCount} of $room")
        }
    }

    @Test
    fun theFirstPageOpensTheChapterLowerDown() = withSetting { setting ->
        val pages = ChapterPaginator().paginate(setting, key = 1, chapter(6)).pages

        assertTrue(pages.first().opensChapter)
        assertTrue(pages.drop(1).none { it.opensChapter })
        assertTrue(setting.openingLines < setting.linesPerPage)
    }

    @Test
    fun aShortChapterIsOnePage() = withSetting { setting ->
        val pages = ChapterPaginator().paginate(setting, key = 1, chapter(1, sentences = 1)).pages

        assertEquals(1, pages.size)
    }

    @Test
    fun anIndentedParagraphCanTakeAnExtraLine() = withSetting { setting ->
        val words = List(60) { "word" }.joinToString(" ")
        val (first, indented) = setting.lineCounts(listOf(words, words))

        assertTrue(indented >= first)
    }

    @Test
    fun theWordTheBookmarkIsOnIsFoundOnItsPage() = withSetting { setting ->
        val chapterPages = ChapterPaginator().paginate(setting, key = 1, chapter(12))
        val last = chapterPages.pages.lastIndex

        assertEquals(0, chapterPages.pageAt(WordRef(0, 0)))
        assertEquals(last, chapterPages.pageAt(WordRef(11, 80)))
    }
}
