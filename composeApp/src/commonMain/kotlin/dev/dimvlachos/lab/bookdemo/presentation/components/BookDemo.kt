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

private const val SpreadCount = 4

// Room around the book for its shadow on the table; the book takes the largest 2:1 that fits.
private val TableMarginX = 40.dp
private val TableMarginY = 28.dp

/**
 * The page-turn book on a table. The script's select(i) taps its way to spread i a page at a time;
 * its drags are played by a drawn fingertip through the book's own drag calls.
 */
@Composable
internal fun BookDemo(state: DemoState, onSpreadChange: (Int) -> Unit = {}) {
    val spreads = rememberBookSpreads()
    val book = rememberPageTurnState(SpreadCount)
    val touch = remember { TouchDot() }
    val currentOnSpreadChange by rememberUpdatedState(onSpreadChange)
    LaunchedEffect(book) { snapshotFlow { book.spread }.collect { currentOnSpreadChange(it) } }
    LaunchedEffect(book, state.selectedIndex) {
        val target = state.selectedIndex.coerceIn(0, SpreadCount - 1)
        while (book.spread != target) {
            val forward = target > book.spread
            val landing = book.spread + if (forward) 1 else -1
            launch { touch.tap(forward) }
            if (forward) book.next() else book.previous()
            snapshotFlow { book.spread }.first { it == landing }
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
            spreads,
            book,
            with(touch) { Modifier.drawTouch(colors.touch) },
        )
    }
}
