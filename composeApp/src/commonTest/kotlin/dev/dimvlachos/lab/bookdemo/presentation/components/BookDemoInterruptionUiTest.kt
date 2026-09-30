package dev.dimvlachos.lab.bookdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.DragMove
import dev.dimvlachos.lab.core.demo.PageDrag
import dev.dimvlachos.lab.core.presentation.components.pageturn.PageTurnState
import dev.dimvlachos.lab.core.presentation.components.pageturn.rememberPageTurnState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class BookDemoInterruptionUiTest {
    @Test
    fun aScriptStoppedMidDragLetsGoOfThePage() = runComposeUiTest {
        mainClock.autoAdvance = false
        lateinit var book: PageTurnState
        lateinit var scope: CoroutineScope
        val touch = TouchDot()
        setContent {
            book = rememberPageTurnState(spreadCount = 2)
            scope = rememberCoroutineScope()
        }
        runOnUiThread { book.bookWidthPx = 1000f }
        val slowDrag = PageDrag(listOf(DragMove(-0.2f, 2.seconds)))
        var job: kotlinx.coroutines.Job? = null
        runOnUiThread { job = scope.launch { touch.drag(book, slowDrag) } }
        mainClock.advanceTimeBy(800)
        assertTrue(book.isTurning, "the page is up under the finger")

        runOnUiThread { job?.cancel() } // the demo's Stop
        mainClock.advanceTimeBy(3_000)

        assertFalse(book.isTurning, "the page came down")
        assertEquals(0, book.spread)
        runOnUiThread { book.next() }
        mainClock.advanceTimeBy(3_000)
        assertEquals(1, book.spread, "the book still turns afterwards")
    }

    @Test
    fun aSelectWhileAFlickedPageLandsDoesNotTurnOnePageTooFar() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        val spreads = mutableListOf<Int>()
        lateinit var scope: CoroutineScope
        setContent {
            LabTheme {
                scope = rememberCoroutineScope()
                Box(Modifier.size(800.dp, 450.dp)) {
                    BookDemo(state) { if (spreads.lastOrNull() != it) spreads += it }
                }
            }
        }
        mainClock.advanceTimeBy(500)
        val flick = PageDrag(listOf(DragMove(-0.12f, 140.milliseconds)), releaseSpeed = -2.4f)
        runOnUiThread {
            scope.launch {
                state.dragPage(flick) // returns on release, with the page still landing
                state.select(1)
            }
        }
        mainClock.advanceTimeBy(4_000)
        assertEquals(listOf(0, 1), spreads)
    }
}
