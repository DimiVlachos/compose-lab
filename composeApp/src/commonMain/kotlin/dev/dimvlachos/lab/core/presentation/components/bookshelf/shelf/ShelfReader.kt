package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookChapterPages
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.LeafSide
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.leafCount
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.leafOfPage
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.leafSide
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageTurnDimens
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageTurnState
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.Sheets
import kotlin.math.abs

private const val CONTIGUOUS_RIFFLE = 3
private const val FIRST_BRIDGE_KEY = -1_000

internal class ChapterSource(
    val book: BookChapterPages,
    val chapterName: String,
    val highlights: Map<Int, Set<Int>> = emptyMap(),
    val spread: Boolean = false,
) {
    val key: Int
        get() = book.key

    val count: Int
        get() = leafCount(book.pages.size, spread)

    fun leafOf(page: Int): Int = leafOfPage(page, spread)

    fun leaf(index: Int, right: Boolean): PageContent? =
        when (val side = leafSide(index, right, book.pages.size, spread)) {
            null -> null
            LeafSide.Endpaper -> PageContent.Endpaper
            LeafSide.Blank -> PageContent.Blank
            is LeafSide.Page ->
                PageContent.Book(
                    chapterName,
                    (side.index + 1).toString(),
                    book.pages[side.index],
                    book.words,
                    highlights,
                )
        }
}

internal class PageBridge(val key: Int, private val leaves: List<Pair<PageContent, PageContent>>) {
    val count: Int
        get() = leaves.size - 1

    fun leaf(index: Int, right: Boolean): PageContent? =
        leaves.getOrNull(index + 1)?.let { if (right) it.second else it.first }
}

internal fun bridgePages(
    earlier: (Int, Boolean) -> PageContent?,
    at: Int,
    later: (Int, Boolean) -> PageContent?,
    to: Int,
): List<Pair<PageContent, PageContent>> =
    (at - 1..at + 1).mapNotNull { bridged(earlier, it) } +
        (to - 2..to).filter { it >= -1 }.mapNotNull { bridged(later, it) }

private fun bridged(leaf: (Int, Boolean) -> PageContent?, index: Int) =
    leaf(index, true)?.let { front -> (leaf(index, false) ?: PageContent.Blank) to front }

@Stable
internal class ShelfReader(private val turns: PageTurnState) {
    var shown by mutableStateOf<ChapterSource?>(null)
        private set

    var bridge by mutableStateOf<PageBridge?>(null)
        private set

    private var pending: Pair<ChapterSource, Int?>? = null
    private var order: (Int) -> Int? = { null }
    private var nextKey = FIRST_BRIDGE_KEY

    init {
        turns.onJump = ::jump
    }

    val book: BookChapterPages?
        get() = shown?.book?.takeIf { bridge == null }

    val hasPages: Boolean
        get() = bridge != null || shown != null

    val spread: Boolean
        get() = shown?.spread == true

    fun leaf(index: Int, right: Boolean): PageContent? =
        bridge?.let { it.leaf(index, right) } ?: shown?.leaf(index, right)

    fun clear() {
        shown = null
        bridge = null
        turns.bridgeSheets = null
        pending = null
        turns.reset()
    }

    fun show(target: ChapterSource?, live: Int?, order: (Int) -> Int?) {
        this.order = order
        if (target == null) return
        if (bridge != null || turns.isRiffling) {
            pending = target to live
            return
        }
        val current = shown
        val from = current?.let { order(it.key) }
        val to = order(target.key)
        val crossing =
            current != null &&
                current.key != target.key &&
                current.book.setting == target.book.setting &&
                from != to
        if (current == null || !crossing) {
            shown = target
            turns.sync(target.key, target.count, live)
            return
        }
        val forward = from == null || to == null || to > from
        cross(current, turns.destination, target, live ?: 0, forward)
    }

    private fun jump(from: Int, to: Int) {
        val current = shown ?: return
        if (bridge != null) return
        if (abs(to - from) <= CONTIGUOUS_RIFFLE) {
            turns.riffle(current.key, current.count, from, to) { land(current, to) }
        } else {
            cross(current, from, current, to, forward = to > from)
        }
    }

    private fun cross(
        source: ChapterSource,
        at: Int,
        target: ChapterSource,
        to: Int,
        forward: Boolean,
    ) {
        val pages =
            if (forward) bridgePages(source::leaf, at, target::leaf, to)
            else bridgePages(target::leaf, to, source::leaf, at)
        val next = PageBridge(nextKey--, pages)
        bridge = next
        val first = 0
        val last = next.count - 1
        val from = sheetsAt(at, source.count)
        val onto = sheetsAt(to, target.count)
        turns.bridgeSheets = if (forward) from to onto else onto to from
        turns.riffle(
            next.key,
            next.count,
            from = if (forward) first else last,
            to = if (forward) last else first,
        ) {
            land(target, to)
        }
    }

    private fun land(source: ChapterSource, at: Int) {
        bridge = null
        turns.bridgeSheets = null
        shown = source
        turns.adopt(source.key, source.count, at, at)
        pending?.let { (target, live) ->
            pending = null
            show(target, live, order)
        }
    }
}

@Composable
internal fun rememberShelfReader(turns: PageTurnState): ShelfReader =
    remember(turns) { ShelfReader(turns) }

private fun sheetsAt(page: Int, count: Int): Sheets =
    Sheets(
        left = (page - 1).coerceIn(0, PageTurnDimens.MaxSheets),
        right = (count - 1 - page).coerceIn(0, PageTurnDimens.MaxSheets),
    )
