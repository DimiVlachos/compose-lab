package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookPage

/**
 * Where an open book is shown: at its bookmark when it opens, and where it was being read when only
 * its layout changes, as when the screen turns and a single page becomes a spread. A new layout
 * breaks the chapter into new pages, so what is kept is the paragraph the shown page began in.
 */
internal class ReadingPlace {
    private var id: Int? = null
    private var spread = false
    private var pages: List<BookPage> = emptyList()
    private var kept: Int? = null

    /**
     * The page to show book [id] at, its chapter broken into [pages] and laid out as a [spread] or
     * not, now showing [shownLeaf]; [pageOf] finds the page a paragraph begins on in [pages].
     */
    fun pageFor(
        id: Int?,
        spread: Boolean,
        pages: List<BookPage>,
        shownLeaf: Int,
        bookmarkPage: Int,
        pageOf: (paragraph: Int) -> Int,
    ): Int {
        when {
            id != this.id -> kept = null
            id != null && spread != this.spread ->
                kept = this.pages.getOrNull(pageOfLeaf(shownLeaf, this.spread))?.firstParagraph
        }
        this.id = id
        this.spread = spread
        this.pages = pages
        return kept?.let(pageOf) ?: bookmarkPage
    }

    /** The book was put back: taken out again, even the same one opens at its bookmark. */
    fun forget() {
        id = null
        kept = null
    }
}

// The first page a leaf shows: a spread's leaf n shows pages 2n and 2n + 1.
internal fun pageOfLeaf(leaf: Int, spread: Boolean): Int = if (spread) leaf * 2 else leaf

/** Whether a page turned: the page changed while the book stayed open, not as it opened. */
internal class PageFlipCue {
    private var openAt: Int? = null

    fun next(open: Boolean, page: Int): Boolean {
        val before = openAt
        openAt = if (open) page else null
        return before != null && open && page != before
    }
}
