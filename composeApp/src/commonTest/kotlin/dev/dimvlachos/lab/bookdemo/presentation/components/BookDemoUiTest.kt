package dev.dimvlachos.lab.bookdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.bookdemo.BookDemos
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BookDemoUiTest {
    @Test
    fun theScriptTurnsThroughTheBookAndBackWithTheDragsLandingAsPlanned() = runComposeUiTest {
        mainClock.autoAdvance = false
        val demo = BookDemos.all.single()
        val state = DemoState()
        val spreads = mutableListOf<Int>()
        var finished = false
        setContent {
            LabTheme {
                Box(Modifier.size(800.dp, 450.dp)) {
                    BookDemo(state) { if (spreads.lastOrNull() != it) spreads += it }
                }
                LaunchedEffect(Unit) {
                    demo.script.play(state)
                    finished = true
                }
            }
        }
        mainClock.advanceTimeBy(demo.script.nominalDuration.inWholeMilliseconds + 2_000)
        // The slow lift falls back and the reversed drag too: neither is a turn. The flick is; the
        // riffles turn a page per tap, three on and five back.
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0), spreads)
        assertEquals(true, finished)
    }
}
