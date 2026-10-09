package dev.dimvlachos.lab.fishingdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingOutcome
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingRefreshState
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingStatus
import dev.dimvlachos.lab.core.presentation.components.fishing.LocalFishingCompositionProbe
import dev.dimvlachos.lab.core.presentation.components.fishing.rememberFishingRefreshState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.fishingdemo.FishingDemos
import dev.dimvlachos.lab.fishingdemo.IslandFeed
import dev.dimvlachos.lab.fishingdemo.rememberIslandFeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FishingDemoUiTest {
    @Test
    fun theScriptCatchesTwoFindsNothingSnapsThenCatchesOne() = runComposeUiTest {
        mainClock.autoAdvance = false
        val demo = FishingDemos.all.single()
        val state = DemoState(replay = true)
        var finished = false
        var compositions = 0
        var fishing: FishingRefreshState? = null
        var feed: IslandFeed? = null
        setContent {
            LabTheme {
                CompositionLocalProvider(LocalFishingCompositionProbe provides { compositions++ }) {
                    Box(Modifier.size(400.dp, 800.dp)) {
                        val f = rememberFishingRefreshState()
                        val islands = rememberIslandFeed()
                        fishing = f
                        feed = islands
                        FishingDemo(state, islands, f)
                    }
                }
                LaunchedEffect(Unit) {
                    demo.script.play(state)
                    finished = true
                }
            }
        }
        mainClock.advanceTimeBy(100)
        onNodeWithText("Mykonos").assertDoesNotExist()
        // The first pull catches two islands, which rise in at the top.
        mainClock.advanceTimeBy(6_200)
        onNodeWithText("Mykonos").assertExists()
        onNodeWithText("Crete").assertExists()
        assertEquals(11, feed!!.items.size)
        // The second finds nothing new, faster than the bobber's shortest float: it floats on
        // after the answer is in, and nothing recomposes while it does.
        mainClock.advanceTimeBy(2_200)
        assertEquals(FishingStatus.Landed(FishingOutcome.NothingNew), feed!!.status)
        val floating = compositions
        mainClock.advanceTimeBy(600)
        assertEquals(floating, compositions, "the floating bobber must not recompose the refresh")
        assertEquals(11, feed!!.items.size)
        // The third fails, and says so.
        mainClock.advanceTimeBy(5_900)
        onNode(hasContentDescription("Couldn’t refresh")).assertExists()
        // The fourth catches Folegandros, and by the end everything is at rest.
        mainClock.advanceTimeBy(demo.script.nominalDuration.inWholeMilliseconds - 15_000 + 100)
        assertTrue(finished)
        onNodeWithText("Folegandros").assertExists()
        assertEquals(12, feed!!.items.size)
        assertFalse(fishing!!.awake, "everything must be at rest by the end")
    }

    @Test
    fun aRealPullRefreshesTheFeed() = runComposeUiTest {
        var feed: IslandFeed? = null
        setContent {
            LabTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    val islands = rememberIslandFeed()
                    feed = islands
                    FishingDemo(DemoState(), islands)
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithTag("feed").performTouchInput {
            down(Offset(centerX, top + 20f))
            repeat(20) { moveBy(Offset(0f, FishingDemos.Pull.toPx() / 20f)) }
        }
        mainClock.advanceTimeBy(100)
        onNodeWithTag("feed").performTouchInput { up() }
        mainClock.advanceTimeBy(300)
        assertEquals(FishingStatus.Refreshing, feed!!.status)
        mainClock.advanceTimeBy(6_000)
        onNodeWithText("Mykonos").assertExists()
    }

    @Test
    fun theFeedReadsAsATitledListOfIslands() = runComposeUiTest {
        setContent { LabTheme { Box(Modifier.size(400.dp, 800.dp)) { FishingDemo(DemoState()) } } }
        onNodeWithText("Islands").assertExists()
        onNodeWithText("Pull down to fish for new islands").assertExists()
        onNodeWithText("Corfu").assertExists()
    }
}
