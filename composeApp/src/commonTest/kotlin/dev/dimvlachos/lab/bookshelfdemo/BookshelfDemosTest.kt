package dev.dimvlachos.lab.bookshelfdemo

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookPageSetting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.ChapterPaginator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BookshelfDemosTest {
    private val all = DemoBooks.reading + DemoBooks.upNext + DemoBooks.finished

    @Test
    fun everyBookHasItsOwnId() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test
    fun theRingHoldsMoreBooksThanFitAcrossTheScreen() {
        assertTrue(DemoBooks.reading.size >= 16)
        assertTrue(DemoBooks.reading.all { it.inProgress })
        assertTrue((DemoBooks.upNext + DemoBooks.finished).none { it.inProgress })
    }

    @Test
    fun theBookReadLastStandsNearTheMiddleOfTheRing() {
        val at = DemoBooks.reading.indexOfFirst { it.id == DemoBooks.SaltRoad }
        assertTrue(at in DemoBooks.reading.size / 3..DemoBooks.reading.size * 2 / 3, "at $at")
    }

    @Test
    fun threeBooksHaveTheirOwnChapterAndTheRestOfTheRingShareOne() {
        assertEquals(DemoChapters.CottonExchange, DemoChapters.forBook(DemoBooks.SaltRoad))
        assertEquals(DemoChapters.FirstSnow, DemoChapters.forBook(DemoBooks.MarshHollow))
        assertEquals(DemoChapters.LastCall, DemoChapters.forBook(DemoBooks.BluePlate))
        assertEquals(DemoChapters.Shared, DemoChapters.forBook(DemoBooks.reading.last().id))
        assertNull(DemoChapters.forBook(BookshelfDemos.PlankBook))
        assertNotNull(DemoBooks.upNext.firstOrNull { it.id == BookshelfDemos.PlankBook })
    }

    @Test
    fun noBookIsAFamousOne() {
        val famous =
            listOf(
                "Twain",
                "Hemingway",
                "Steinbeck",
                "Fitzgerald",
                "Austen",
                "Dickens",
                "Melville",
                "Faulkner",
                "Alcott",
                "Thoreau",
                "Morrison",
                "King",
                "Rowling",
            )
        assertTrue(all.none { book -> famous.any { it in book.author } })
    }

    @Test
    fun theThreeChaptersFillEightToTenPages() = runComposeUiTest {
        lateinit var measurer: TextMeasurer
        setContent { measurer = rememberTextMeasurer() }
        runOnIdle {
            // The open book's page on a phone, as the shelf sets it: about 300 by 450 dp at 3x.
            val setting =
                BookPageSetting(
                    900f,
                    1350f,
                    48f,
                    69f,
                    33f,
                    1.6f / 11f,
                    FontFamily.Serif,
                    measurer,
                    Density(3f),
                )
            for (chapter in
                listOf(
                    DemoChapters.CottonExchange,
                    DemoChapters.FirstSnow,
                    DemoChapters.LastCall,
                )) {
                val pages =
                    ChapterPaginator().paginate(setting, chapter.hashCode(), chapter.words).pages
                assertTrue(pages.size in 7..12, "${chapter.title}: ${pages.size} pages")
            }
        }
    }
}
