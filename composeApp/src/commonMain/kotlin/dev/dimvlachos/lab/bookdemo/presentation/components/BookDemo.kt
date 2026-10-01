package dev.dimvlachos.lab.bookdemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.pageturn.PageTurnBook
import dev.dimvlachos.lab.core.presentation.components.pageturn.rememberPageTurnState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val PhotoSpreads = 4

// Blank leaves before and after the photos: the book is opened at its first photo spread with this
// many leaves already turned, and as many left to turn after the last.
private const val BlankLeaves = 8
private const val SpreadCount = PhotoSpreads + 2 * BlankLeaves

// Room around the book for its shadow on the table; the book takes the largest 2:1 that fits.
private val TableMarginX = 40.dp
private val TableMarginY = 28.dp

/**
 * The page-turn book on a table: a book of blank paper leaves with four photo spreads in the
 * middle, opened at the first. The script's select(i) taps its way to photo spread i a page at a
 * time, and [onSpreadChange] reports which photo spread is open; its drags are played by a drawn
 * fingertip through the book's own drag calls.
 */
@Composable
internal fun BookDemo(state: DemoState, onSpreadChange: (Int) -> Unit = {}) {
    val photos = rememberBookSpreads()
    val blank = remember { List(BlankLeaves) { null } }
    val book = rememberPageTurnState(SpreadCount, initialSpread = BlankLeaves)
    val touch = remember { TouchDot() }
    val currentOnSpreadChange by rememberUpdatedState(onSpreadChange)
    LaunchedEffect(book) {
        snapshotFlow { book.spread }.collect { currentOnSpreadChange(it - BlankLeaves) }
    }
    LaunchedEffect(book, state.selectedIndex) {
        val target = BlankLeaves + state.selectedIndex.coerceIn(0, PhotoSpreads - 1)
        while (true) {
            // One tap per landed page: a page still up (a flick settling, a drag) finishes first,
            // so the book's spread is where the next tap really starts from.
            snapshotFlow { book.isTurning }.first { !it }
            if (book.spread == target) break
            val forward = target > book.spread
            launch { touch.tap(forward) }
            if (forward) book.next() else book.previous()
        }
    }
    DisposableEffect(state, book) {
        state.setPageDragHandler { drag -> touch.drag(book, drag) }
        onDispose { state.setPageDragHandler(null) }
    }
    val colors = LabTheme.colors
    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(colors.tableLit, colors.table)))
            .padding(horizontal = TableMarginX, vertical = TableMarginY),
        contentAlignment = Alignment.Center,
    ) {
        if (photos == null) return@Box
        PageTurnBook(
            spreads = blank + photos + blank,
            book,
            with(touch) { Modifier.drawTouch(colors.touch) },
        )
    }
}
