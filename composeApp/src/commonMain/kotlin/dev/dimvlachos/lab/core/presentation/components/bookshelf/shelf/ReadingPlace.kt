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

// How near flat a leaf lies when it looks down, though its spring has yet to settle.
private const val LeafDown = 0.02f

// Leaves riffled over together whoosh as one, not once a leaf.
private const val TurnGapMillis = 120L

/** The sounds a leaf makes: air as it flips over the spine, a pat as it lies down. */
internal enum class LeafSound {
    Turn,
    Land,
}

/**
 * The sounds of the leaves in flight, from where each one is: a leaf whooshes as it crosses the
 * spine, half way over, and pats down the frame it looks flat. A leaf let fall back before it
 * crosses makes none, and nothing sounds while the book is not open.
 */
internal class LeafSoundCue {
    private class Leaf(var over: Boolean, var crossed: Boolean = false)

    private val leaves = HashMap<Int, Leaf>()
    private var lastTurn: Long? = null

    /** The sounds of the leaves in [flights], as (leaf, turn from 0 to 1), seen [atMillis]. */
    fun next(open: Boolean, flights: List<Pair<Int, Float>>, atMillis: Long): List<LeafSound> {
        if (!open) {
            leaves.clear()
            return emptyList()
        }
        val sounds = mutableListOf<LeafSound>()
        val gone = leaves.keys - flights.mapTo(HashSet()) { it.first }
        gone.forEach { if (leaves.remove(it)?.crossed == true) sounds += LeafSound.Land }
        for ((id, t) in flights) {
            val over = t >= 0.5f
            val leaf = leaves.getOrPut(id) { Leaf(over) }
            if (over != leaf.over) {
                leaf.over = over
                leaf.crossed = true
                if (lastTurn.let { it == null || atMillis - it >= TurnGapMillis }) {
                    lastTurn = atMillis
                    sounds += LeafSound.Turn
                }
            }
            val down = if (over) t >= 1f - LeafDown else t <= LeafDown
            if (leaf.crossed && down) {
                leaf.crossed = false
                sounds += LeafSound.Land
            }
        }
        return sounds
    }
}
