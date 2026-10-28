package dev.dimvlachos.lab.coinsorterdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.coinsorterdemo.CoinSorterDemos
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinSort
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinSorterState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class CoinSorterDemoUiTest {
    @Test
    fun eachRowReadsItsNamePriceAndRating() = runComposeUiTest {
        setContent { Phone { CoinSorterDemo(DemoState()) } }
        waitForIdle()
        val label =
            onNodeWithTag("row:2")
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertEquals(listOf("Pocket Glow Mini, 2-pack, $12.50, rated 4.4"), label)
        onNode(hasContentDescription("Sort by: Best match")).assertExists()
    }

    @Test
    fun tappingTheChipOpensTheMenuAndAnOptionSortsThroughTheTray() = runComposeUiTest {
        lateinit var sorter: CoinSorterState
        setContent {
            sorter = rememberCoinSorter()
            Phone { CoinSorterDemo(DemoState(), sorter, reducedMotion = false) }
        }
        waitForIdle()
        onNodeWithTag("chip").performTouchInput {
            down(center)
            up()
        }
        waitForIdle()
        assertTrue(sorter.menuOpen)
        onNodeWithTag("option:low").performTouchInput {
            down(center)
            up()
        }
        mainClock.advanceTimeBy(600)
        assertTrue(sorter.running)
        assertFalse(sorter.menuOpen)
        mainClock.advanceTimeBy(5_000)
        waitForIdle()
        assertFalse(sorter.running)
        assertEquals(CoinSort.Low, sorter.sort)
        assertEquals(2, sorter.order[0])
        onNode(hasContentDescription("Sorted by Price: low to high")).assertExists()
    }

    @Test
    fun aScreenReaderOpensTheMenuAndPicksAnOption() = runComposeUiTest {
        lateinit var sorter: CoinSorterState
        setContent {
            sorter = rememberCoinSorter()
            Phone { CoinSorterDemo(DemoState(), sorter, reducedMotion = true) }
        }
        waitForIdle()
        onNodeWithTag("chip").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNodeWithTag("option:high").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        assertEquals(CoinSort.High, sorter.sort)
        assertEquals(7, sorter.order[0])
    }

    @Test
    fun theScriptSortsThroughTheChipAndEndsAsItBegan() = runComposeUiTest {
        val state = DemoState()
        lateinit var sorter: CoinSorterState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            sorter = rememberCoinSorter()
            Phone { CoinSorterDemo(state, sorter, reducedMotion = false) }
        }
        waitForIdle()
        scope.launch { state.tapControl(CoinSorterDemos.SortLow) }
        mainClock.advanceTimeBy(1_500)
        assertTrue(sorter.menuOpen)
        mainClock.advanceTimeBy(5_000)
        assertEquals(CoinSort.Low, sorter.sort)
        scope.launch { state.tapControl(CoinSorterDemos.SortBest) }
        mainClock.advanceTimeBy(6_500)
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(sorter.asBegun)
    }

    // A phone's stage under the demo bar.
    @Composable
    private fun Phone(content: @Composable () -> Unit) {
        Box(Modifier.size(412.dp, 820.dp)) { content() }
    }
}
