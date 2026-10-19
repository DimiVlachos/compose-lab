package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookChapterPages
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookPageSetting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.ChapterPaginator
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.WordRef
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageTurnState
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.rememberPageTurnState
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.bookshelf_open_book_page
import kotlin.time.TimeSource
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/** A chapter to read: its [title], and its paragraphs split into [words]. */
internal class ShelfChapter(val title: String, val words: List<List<String>>)

/**
 * A shelf whose books are read: a book with a chapter opens at its bookmark and its pages turn; one
 * without opens at its title page.
 */
@Composable
internal fun ReadingShelf(
    content: ShelfContent,
    state: ShelfState,
    chapterFor: (Int) -> ShelfChapter?,
    modifier: Modifier = Modifier,
    style: ShelfStyle = ShelfStyle.Planks,
    spread: Boolean = false,
    turns: PageTurnState = rememberPageTurnState(),
    body: @Composable (hero: @Composable (Modifier) -> Unit) -> Unit,
) {
    val paginator = remember { ChapterPaginator() }
    val reader = rememberShelfReader(turns)
    var setting by remember { mutableStateOf<BookPageSetting?>(null) }
    val activeId = state.activeBookId
    val chapter = remember(activeId) { activeId?.let(chapterFor) }
    val ready = setting
    // Set into pages by the bakers, the only thread the shelf's text is set on.
    // Another book's or layout's pages are dropped at once, not kept until these are set.
    val pages =
        produceState<BookChapterPages?>(null, ready, activeId, chapter, spread) {
                value = null
                if (ready != null && activeId != null && chapter != null) {
                    // Each layout's pages are pages of their own: the reader moves to its place
                    // in them at once, rather than turning page by page as if reading on.
                    val key = 31 * (31 * activeId + spread.hashCode()) + ready.hashCode()
                    value =
                        withContext(ShelfTextBakers) {
                            paginator.paginate(ready, key, chapter.words)
                        }
                }
            }
            .value
    val source =
        remember(pages, chapter, spread) {
            if (pages == null || chapter == null) null
            else ChapterSource(pages, chapter.title, spread = spread)
        }
    val readFraction = activeId?.let { content.entries[it]?.readFraction }
    // The page it opens at: its bookmark, or the page being read when only the layout changed.
    val place = remember { ReadingPlace() }
    val startLeaf =
        remember(source, readFraction) {
            source?.let {
                val shown = Snapshot.withoutReadObservation { turns.page }
                val page =
                    place.pageFor(
                        activeId,
                        spread,
                        it.book.pages,
                        shown,
                        bookmarkPage(it.book.pages.size, readFraction),
                    ) { paragraph ->
                        it.book.pageAt(WordRef(paragraph, 0))
                    }
                it.leafOf(page)
            }
        }

    LaunchedEffect(activeId, spread) { reader.clear() }
    LaunchedEffect(activeId) { if (activeId == null) place.forget() }
    val sounds = LocalShelfSounds.current
    LaunchedEffect(state) {
        var mode = state.mode
        snapshotFlow { state.mode }
            .collect { next ->
                shelfSoundFor(mode, next)?.let { sounds.play(it) }
                mode = next
            }
    }
    LaunchedEffect(source, startLeaf) { reader.show(source, startLeaf) { 0 } }
    // Heard from where the leaves are, not from the page they land on: a turn's spring settles
    // long after its leaf looks flat. A new layout moves to its page without turning a leaf.
    LaunchedEffect(turns) {
        val cue = LeafSoundCue()
        val start = TimeSource.Monotonic.markNow()
        snapshotFlow { (state.mode == ShelfMode.Open) to turns.flights.map { it.leaf to it.t } }
            .collect { (open, flights) ->
                cue.next(open, flights, start.elapsedNow().inWholeMilliseconds).forEach {
                    when (it) {
                        LeafSound.Turn -> sounds.play(Sound.PageTurn, volume = 0.6f)
                        LeafSound.Land -> sounds.play(Sound.PageLand, volume = 0.5f)
                    }
                }
            }
    }

    PhysicalShelfHost(
        content = content,
        state = state,
        pagesFor = { id, _ ->
            readingPages(
                content.entries[id],
                reader.takeIf { id == activeId },
                turns.page,
                chapterFor(id) != null,
            )
        },
        modifier = modifier,
        style = style,
        turns = turns,
        leafFor = { id, _, key -> reader.leaf(key.page, key.right).takeIf { id == activeId } },
        onBookSetting = { setting = it },
        spread = spread,
        // Nothing reads the page aloud here, so its text stays in the page itself.
        liveText = false,
        // Swiped down, or right from its first page, or Back, it goes back, and swiped across its
        // pages turn; screen readers have the same as actions on the open book.
        chrome = {
            val title = activeId?.let { content.entries[it]?.visual?.title }.orEmpty()
            val pages = source?.book?.pages?.size?.takeIf { reader.hasPages }
            val leaf = turns.page
            OpenBookActions(
                description =
                    if (pages == null) title
                    else
                        stringResource(
                            Res.string.bookshelf_open_book_page,
                            title,
                            pageOfLeaf(leaf, spread) + 1,
                            pages,
                        ),
                onClose = { state.putBack() },
                onNext =
                    { turns.tap(forward = true) }.takeIf { pages != null && leaf < turns.lastPage },
                onPrevious = { turns.tap(forward = false) }.takeIf { pages != null && leaf > 0 },
            )
        },
        body = body,
    )
}

/** The page a bookmark [readFraction] of the way through a chapter of [pages] pages lies on. */
internal fun bookmarkPage(pages: Int, readFraction: Float?): Int =
    ((pages - 1) * (readFraction ?: 0f)).toInt().coerceIn(0, (pages - 1).coerceAtLeast(0))

/**
 * What an open book shows: its chapter's pages once [reader] has them, else its title page, kept
 * [pending] while a chapter it has is still being set.
 */
internal fun readingPages(
    entry: ShelfEntry?,
    reader: ShelfReader?,
    page: Int,
    hasChapter: Boolean,
): OpenBookPages? {
    entry ?: return null
    if (reader?.hasPages == true) {
        return OpenBookPages(
            left = reader.leaf(page - 1, right = false) ?: PageContent.Endpaper,
            right = reader.leaf(page, right = true) ?: PageContent.Endpaper,
            ribbon = false,
            liveLeft = reader.spread,
        )
    }
    return OpenBookPages(
        left = PageContent.Endpaper,
        right = PageContent.TitlePage(entry.visual.title, entry.visual.author, null),
        ribbon = entry.readFraction != null,
        pending = hasChapter,
    )
}
