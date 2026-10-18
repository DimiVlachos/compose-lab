package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.MAX_BOOKS_PER_PLANK
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfBookSpec
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.shelfBookStyle
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.spineThicknessFor
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.bookshelf_hours_minutes_left
import dev.dimvlachos.lab.resources.bookshelf_minutes_left
import dev.dimvlachos.lab.resources.bookshelf_shelf_book
import dev.dimvlachos.lab.resources.bookshelf_shelf_book_in_progress
import org.jetbrains.compose.resources.stringResource

private const val IN_PROGRESS_READ_FRACTION = 0.42f
private const val BASE_HEIGHT = 198f
private const val HEIGHT_SPREAD = 42
private const val DEPTH_RATIO = 0.66f
private const val MINUTES_PER_HOUR = 60

/**
 * A book on the shelf: its [title] and [author], its cloth or paper in [color] (`#RRGGBB`), how
 * long it is in [hours] (which sets its spine's thickness), and, if it is being read, the [chapter]
 * its bookmark is in and the [minutesLeft] of it.
 */
@Immutable
internal data class ShelfBook(
    val id: Int,
    val title: String,
    val author: String,
    val color: String,
    val hours: Double,
    val chapter: String? = null,
    val minutesLeft: Int? = null,
) {
    val inProgress: Boolean
        get() = chapter != null
}

/** The shelf's books plank by plank (or, on the ring, all of them), and the one being read. */
@Composable
internal fun rememberShelfContent(
    planks: List<List<ShelfBook>>,
    readingBookId: Int?,
    perPlank: Int = MAX_BOOKS_PER_PLANK,
): ShelfContent {
    val labels = planks.flatten().associate { book -> book.id to shelfBookLabel(book) }
    return remember(planks, readingBookId, labels, perPlank) {
        shelfContentOf(planks, readingBookId, perPlank) { labels[it.id].orEmpty() }
    }
}

@Composable
internal fun steadyShelfContent(content: ShelfContent, state: ShelfState): ShelfContent {
    val held = remember { HeldContent() }
    if (state.activeBookId == null) held.content = content
    return held.content ?: content
}

@Composable
private fun shelfBookLabel(book: ShelfBook): String {
    val chapter = book.chapter
    val minutesLeft = book.minutesLeft
    return if (chapter != null && minutesLeft != null) {
        stringResource(
            Res.string.bookshelf_shelf_book_in_progress,
            book.title,
            book.author,
            chapter,
            timeLeftText(minutesLeft),
        )
    } else {
        stringResource(Res.string.bookshelf_shelf_book, book.title, book.author)
    }
}

@Composable
private fun timeLeftText(minutes: Int): String =
    if (minutes < MINUTES_PER_HOUR) {
        stringResource(Res.string.bookshelf_minutes_left, minutes)
    } else {
        stringResource(
            Res.string.bookshelf_hours_minutes_left,
            minutes / MINUTES_PER_HOUR,
            minutes % MINUTES_PER_HOUR,
        )
    }

internal fun shelfContentOf(
    planks: List<List<ShelfBook>>,
    readingBookId: Int?,
    perPlank: Int = MAX_BOOKS_PER_PLANK,
    label: (ShelfBook) -> String,
): ShelfContent {
    fun entry(book: ShelfBook): ShelfEntry {
        val id = book.id
        val height = BASE_HEIGHT + (id * 53).mod(HEIGHT_SPREAD)
        return ShelfEntry(
            visual =
                ShelfBookVisual(
                    id = id,
                    title = book.title,
                    author = book.author,
                    style = shelfBookStyle(id, book.color),
                ),
            spec =
                ShelfBookSpec(
                    id = id,
                    thickness = spineThicknessFor(book.hours * 3600.0),
                    height = height,
                    depth = height * DEPTH_RATIO,
                ),
            readFraction = IN_PROGRESS_READ_FRACTION.takeIf { book.inProgress },
            accessibilityLabel = label(book),
        )
    }
    val laid = planks.map { plank -> plank.take(perPlank).map(::entry) }.filter { it.isNotEmpty() }
    return ShelfContent(laid, readingBookId)
}

private class HeldContent {
    var content: ShelfContent? = null
}
