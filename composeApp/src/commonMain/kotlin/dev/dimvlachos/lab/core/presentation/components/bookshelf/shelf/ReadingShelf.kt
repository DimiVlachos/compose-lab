package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookPageSetting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.ChapterPaginator
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.WordRef
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageTurnState
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.rememberPageTurnState

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
    val pages =
        remember(ready, activeId, chapter) {
            if (ready == null || activeId == null || chapter == null) null
            else paginator.paginate(ready, activeId, chapter.words)
        }
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
    LaunchedEffect(turns, activeId, spread) {
        // A page turned in an open book; the jump to its bookmark as it opens is not one.
        val cue = PageFlipCue()
        snapshotFlow { (state.mode == ShelfMode.Open) to turns.page }
            .collect { (open, page) ->
                if (cue.next(open, page)) sounds.play(Sound.PageFlip, volume = 0.7f)
            }
    }
    LaunchedEffect(source, startLeaf) { reader.show(source, startLeaf) { 0 } }

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
        // Swiped down, or right from its first page, or Back, it goes back; screen readers have
        // the same as an action on the open book.
        chrome = { ShelfCloseAction(onClose = { state.putBack() }) },
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
