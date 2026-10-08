package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MagnetTableUiTest {
    private class Table(val state: MagnetState, val compositions: () -> Int)

    // The three test photos and tags of MagnetStateTest, on a 360 × 720 dp table.
    private fun ComposeUiTest.table(): Table {
        var state: MagnetState? = null
        var compositions = 0
        setContent {
            LabTheme {
                CompositionLocalProvider(LocalMagnetCompositionProbe provides { compositions++ }) {
                    val table = rememberMagnetState(TestPhotos, TestTags)
                    state = table
                    MagnetTable(table, Modifier.size(360.dp, 720.dp).testTag("table"))
                }
            }
        }
        waitForIdle()
        return Table(state!!) { compositions }
    }

    private fun stateIs(text: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun ComposeUiTest.runAction(node: SemanticsNodeInteraction, label: String) {
        val action =
            node.fetchSemanticsNode().config[SemanticsActions.CustomActions].first {
                it.label == label
            }
        runOnUiThread { action.action() }
        waitForIdle()
    }

    @Test
    fun draggingAMagnetOverAMatchingPhotoSticksItAndShowsTheCount() = runComposeUiTest {
        val table = table()
        val from = table.state.magnetPosition("a")!!
        val photo = table.state.photoPosition("strong")!!
        onNodeWithTag("table").performTouchInput {
            val to = photo + Offset(0f, 90.dp.toPx())
            down(from)
            for (i in 1..10) moveTo(lerp(from, to, i / 10f))
            up()
        }
        mainClock.advanceTimeBy(2_000)
        assertEquals(setOf("strong"), table.state.results["a"])
        onNodeWithContentDescription("Alpha filter").assertIsOn().assert(stateIs("1 photo"))
        onNodeWithContentDescription("Strong").assert(stateIs("On the Alpha magnet"))
    }

    @Test
    fun nothingRecomposesWhileThePhotosMove() = runComposeUiTest {
        mainClock.autoAdvance = false
        val table = table()
        // Only the weak photo matches Gamma, under the threshold: it leans towards the magnet
        // but never sticks, so nothing composition reads changes.
        runOnUiThread { table.state.apply("c") }
        repeat(3) { mainClock.advanceTimeByFrame() }
        val before = table.compositions()
        val start = table.state.photoPosition("weak")!!
        repeat(60) { mainClock.advanceTimeByFrame() }
        val moved = (table.state.photoPosition("weak")!! - start).getDistance()
        assertTrue(moved > 5f, "the weak photo moved only $moved px")
        assertEquals(before, table.compositions(), "moving photos must not recompose")
    }

    @Test
    fun theScreenReadersActionsApplyAndRemoveAFilter() = runComposeUiTest {
        val table = table()
        val magnet = onNodeWithContentDescription("Alpha filter")
        magnet.assertIsOff().assert(stateIs("Off"))
        runAction(magnet, "Apply Alpha filter")
        mainClock.advanceTimeBy(2_000)
        magnet.assertIsOn().assert(stateIs("1 photo"))
        assertEquals(setOf("strong"), table.state.results["a"])
        runAction(magnet, "Remove Alpha filter")
        magnet.assertIsOff()
        assertTrue(table.state.results.isEmpty())
    }

    @Test
    fun aTapOnAMagnetFansItsPhotosOutAndATapAnywhereFoldsThem() = runComposeUiTest {
        val table = table()
        runOnUiThread { table.state.apply("a") }
        mainClock.advanceTimeBy(2_000)
        val at = table.state.magnetPosition("a")!!
        onNodeWithTag("table").performTouchInput { click(at) }
        assertEquals("a", table.state.fannedOut)
        mainClock.advanceTimeBy(600)
        onNodeWithTag("table").performTouchInput { click(Offset(20f, 20f)) }
        assertNull(table.state.fannedOut)
        mainClock.advanceTimeBy(600)
        assertNull(table.state.fanShown)
        assertEquals(setOf("strong"), table.state.results["a"], "folding gives nothing up")
    }

    @Test
    fun flickedBackIntoTheStripAMagnetLetsGoOfItsPhotos() = runComposeUiTest {
        val table = table()
        runOnUiThread { table.state.apply("a") }
        mainClock.advanceTimeBy(2_000)
        val at = table.state.magnetPosition("a")!!
        onNodeWithTag("table").performTouchInput {
            swipe(at, Offset(at.x, at.y + 300.dp.toPx()), durationMillis = 120)
        }
        mainClock.advanceTimeBy(3_000)
        assertTrue(table.state.results.isEmpty())
        assertEquals(table.state.slotPosition("a"), table.state.magnetPosition("a"))
    }

    @Test
    fun aPhotoPulledOffByHandLeavesTheResults() = runComposeUiTest {
        val table = table()
        runOnUiThread { table.state.apply("a") }
        mainClock.advanceTimeBy(2_000)
        val magnet = table.state.magnetPosition("a")!!
        val card = table.state.photoPosition("strong")!!
        onNodeWithTag("table").performTouchInput {
            // Taken by its far side, clear of the magnet's own touch target.
            val away = card - magnet
            val grip = card + away / away.getDistance() * 20.dp.toPx()
            swipe(grip, grip + Offset(0f, -150.dp.toPx()), durationMillis = 300)
        }
        mainClock.advanceTimeBy(1_000)
        assertEquals(emptySet(), table.state.results["a"])
    }
}
