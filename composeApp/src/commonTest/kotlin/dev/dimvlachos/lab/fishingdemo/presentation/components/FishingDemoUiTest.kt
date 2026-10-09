package dev.dimvlachos.lab.fishingdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
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
    // Steps the clock a frame at a time until [done], failing after [most] ms with [what].
    private fun ComposeUiTest.advanceUntil(what: String, most: Long = 8_000, done: () -> Boolean) {
        var waited = 0L
        while (!done()) {
            assertTrue(waited < most, "waited $most ms for $what")
            mainClock.advanceTimeByFrame()
            waited += 16
        }
    }

    @Test
    fun theScriptCatchesTwoFindsNothingSnapsThenCatchesOneAndPlaysTheSameAgain() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val demo = FishingDemos.all.single()
            val state = DemoState(replay = true)
            var runs = 0
            var compositions = 0
            var fishing: FishingRefreshState? = null
            var feed: IslandFeed? = null
            setContent {
                LabTheme {
                    CompositionLocalProvider(
                        LocalFishingCompositionProbe provides { compositions++ }
                    ) {
                        Box(Modifier.size(400.dp, 800.dp)) {
                            val f = rememberFishingRefreshState()
                            val islands = rememberIslandFeed()
                            fishing = f
                            feed = islands
                            FishingDemo(state, islands, f)
                        }
                    }
                    // Twice in one composition, as the recorder plays it and a Replay loops it.
                    LaunchedEffect(Unit) {
                        repeat(2) {
                            demo.script.play(state)
                            runs++
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(100)
            val islands = feed!!
            repeat(2) { run ->
                assertEquals(9, islands.items.size, "run ${run + 1} must start from the same feed")
                onNodeWithText("Mykonos").assertDoesNotExist()
                // The first pull catches two islands, which rise in at the top.
                advanceUntil("the first catch") { islands.items.size == 11 }
                advanceUntil("the first catch to rise") { !fishing!!.busy }
                onNodeWithText("Mykonos").assertExists()
                onNodeWithText("Crete").assertExists()
                // The second finds nothing new, faster than the bobber's shortest float: it floats
                // on after the answer is in, and nothing recomposes while it does.
                advanceUntil("nothing new") {
                    islands.status == FishingStatus.Landed(FishingOutcome.NothingNew)
                }
                val floating = compositions
                mainClock.advanceTimeBy(400)
                assertEquals(floating, compositions, "the floating bobber must not recompose")
                assertEquals(11, islands.items.size)
                // The third fails, and says so.
                advanceUntil("the failure") { islands.status.isFailure() }
                advanceUntil("the snap to be announced") {
                    onAllNodes(hasContentDescription("Couldn’t refresh"))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                // The fourth catches Folegandros.
                advanceUntil("the last catch") { islands.items.size == 12 }
                onNodeWithText("Folegandros").assertExists()
                // By the end everything is at rest and the feed is back as it began.
                advanceUntil("the run's end", most = 12_000) { runs == run + 1 }
                assertFalse(fishing!!.awake, "everything must be at rest by the end")
            }
        }

    private fun FishingStatus.isFailure(): Boolean =
        this is FishingStatus.Landed && outcome is FishingOutcome.Failed

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
