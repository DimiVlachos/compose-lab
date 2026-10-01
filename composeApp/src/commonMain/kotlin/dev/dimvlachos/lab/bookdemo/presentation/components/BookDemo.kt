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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val SpreadCount = 8

// The book opens a few leaves in, so both sides lie on a stack; the script counts from here.
private const val OpenAt = 2

// Between taps when the script turns several pages: a quick hand, each page let go while the last
// is still in the air.
private const val RiffleMs = 170L

// Room around the book for its shadow on the table; the book takes the largest 2:1 that fits.
private val TableMarginX = 40.dp
private val TableMarginY = 28.dp

/**
 * The page-turn book on a table: Little Nemo's first sixteen pages, opened a couple of leaves in.
 * The script's select(i) taps its way to spread i (counted from where it opens), a page per tap and
 * taps in quick succession when there are several, and [onSpreadChange] reports the spread the same
 * way; its drags are played by a drawn fingertip through the book's own drag calls.
 */
@Composable
internal fun BookDemo(state: DemoState, onSpreadChange: (Int) -> Unit = {}) {
    val spreads = rememberBookSpreads()
    val book = rememberPageTurnState(SpreadCount, initialSpread = OpenAt)
    val touch = remember { TouchDot() }
    val currentOnSpreadChange by rememberUpdatedState(onSpreadChange)
    LaunchedEffect(book) {
        snapshotFlow { book.spread }.collect { currentOnSpreadChange(it - OpenAt) }
    }
    LaunchedEffect(book, state.selectedIndex) {
        val target = OpenAt + state.selectedIndex.coerceIn(0, SpreadCount - 1 - OpenAt)
        while (true) {
            // A page under a finger is let go first; one already landing counts as landed, so
            // the next tap turns from where the book is headed and taps can come quickly.
            snapshotFlow { book.isDragging }.first { !it }
            val at = book.destination
            if (at == target) break
            val forward = target > at
            launch { touch.tap(forward) }
            if (forward) book.next() else book.previous()
            delay(RiffleMs)
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
        if (spreads == null) return@Box
        PageTurnBook(
            spreads = spreads,
            book,
            with(touch) { Modifier.drawTouch(colors.touch) },
        )
    }
}
