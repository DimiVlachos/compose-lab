package dev.dimvlachos.lab.core.presentation.components.fishing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.Recreation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Long enough for any outcome to play out, the band to close and the frame loop to sleep.
private const val SettleMs = 10_000L

@OptIn(ExperimentalTestApi::class)
class FishingRefreshUiTest {
    private class Ticks : HapticFeedback {
        val types = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            types += hapticFeedbackType
        }
    }

    // A caller as a ViewModel would be: a refresh asked for sets Refreshing; the test lands it.
    private class Host {
        var status: FishingStatus by mutableStateOf(FishingStatus.Idle)
        var refreshes = 0
        var compositions = 0
        val ticks = Ticks()
        lateinit var state: FishingRefreshState
    }

    // A fishing refresh over a list of 30 plain rows, 80 dp tall, on a 360 × 640 dp screen. With
    // [rising], each row rises out of the water when it is part of a catch.
    private fun ComposeUiTest.fishing(
        host: Host = Host(),
        rising: Boolean = false,
        setContent: (@Composable () -> Unit) -> Unit = {
            this.setContent(it)
        },
    ): Host {
        setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides host.ticks,
                LocalFishingCompositionProbe provides { host.compositions++ },
            ) {
                val state = rememberFishingRefreshState()
                host.state = state
                FishingRefresh(
                    status = host.status,
                    onRefresh = {
                        host.refreshes++
                        host.status = FishingStatus.Refreshing
                    },
                    modifier = Modifier.size(360.dp, 640.dp).testTag("fishing"),
                    state = state,
                ) {
                    LazyColumn(Modifier.fillMaxSize().testTag("list")) {
                        items(30) { i ->
                            Box(
                                Modifier.fillMaxWidth()
                                    .then(
                                        if (rising) Modifier.risingFromWater(state, i) else Modifier
                                    )
                                    .height(80.dp)
                                    .testTag("row$i")
                            )
                        }
                    }
                }
            }
        }
        waitForIdle()
        // The clock is the test's: a bobber floats for as long as a refresh takes, so a screen
        // mid-refresh is never idle, and an advancing clock would wait on it for ever.
        mainClock.autoAdvance = false
        return host
    }

    // A finger down near the top of the list, drawn [by] down in 20 even moves, held a moment and
    // lifted, as a person pulls to refresh.
    private fun ComposeUiTest.pullList(by: Dp) {
        onNodeWithTag("list").performTouchInput {
            down(Offset(centerX, top + 10f))
            repeat(20) { moveBy(Offset(0f, by.toPx() / 20f)) }
        }
        mainClock.advanceTimeBy(100)
        onNodeWithTag("list").performTouchInput { up() }
        // The release is played out on the next frames.
        mainClock.advanceTimeBy(300)
    }

    private fun ComposeUiTest.refreshAndLand(host: Host, outcome: FishingOutcome) {
        runOnIdle { host.status = FishingStatus.Refreshing }
        mainClock.advanceTimeBy(1_500)
        runOnIdle { host.status = FishingStatus.Landed(outcome) }
    }

    @Test
    fun aPullPastTheThresholdTicksAndAsksForARefresh() = runComposeUiTest {
        val host = fishing()
        pullList(by = 320.dp)
        assertEquals(1, host.refreshes)
        assertEquals(listOf(HapticFeedbackType.VirtualKey), host.ticks.types)
    }

    @Test
    fun aShortPullAsksForNothing() = runComposeUiTest {
        val host = fishing()
        pullList(by = 120.dp)
        assertEquals(0, host.refreshes)
        assertTrue(host.ticks.types.isEmpty())
    }

    @Test
    fun nothingRecomposesWhileTheLineAndTheWaterMove() = runComposeUiTest {
        val host = fishing()
        runOnIdle { host.status = FishingStatus.Refreshing }
        mainClock.advanceTimeBy(200)
        val casting = host.compositions
        mainClock.advanceTimeBy(3_000)
        assertEquals(FishingPhase.Waiting, host.state.rig.phase)
        assertEquals(casting, host.compositions, "the cast and the bobbing must not recompose")
        runOnIdle { host.status = FishingStatus.Landed(FishingOutcome.Caught(2)) }
        mainClock.advanceTimeBy(100)
        val landed = host.compositions
        mainClock.advanceTimeBy(1_700)
        assertTrue(host.state.rig.phase in setOf(FishingPhase.Reeling, FishingPhase.Rising))
        assertEquals(landed, host.compositions, "the bite and the reel must not recompose")
    }

    @Test
    fun aScreenReaderCanRefreshWithAButtonThatIsOffWhileBusy() = runComposeUiTest {
        val host = fishing()
        val button = onNode(hasContentDescription("Refresh") and hasClickAction())
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        button.assertIsEnabled()
        button.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, host.refreshes)
        mainClock.advanceTimeBy(300)
        assertTrue(host.state.rig.busy, "the line is cast")
        // Mid-refresh it can't ask again.
        onNode(hasContentDescription("Refresh")).assertIsNotEnabled()
    }

    @Test
    fun eachOutcomeIsAnnouncedPolitely() = runComposeUiTest {
        val host = fishing()
        val said =
            listOf(
                FishingOutcome.Caught(2) to "2 new items",
                FishingOutcome.NothingNew to "Nothing new",
                FishingOutcome.Failed(IllegalStateException("Network unreachable")) to
                    "Couldn’t refresh",
            )
        for ((outcome, words) in said) {
            refreshAndLand(host, outcome)
            mainClock.advanceTimeBy(1_000)
            onNode(hasContentDescription(words))
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.LiveRegion,
                        LiveRegionMode.Polite,
                    )
                )
            runOnIdle { host.status = FishingStatus.Idle }
            mainClock.advanceTimeBy(SettleMs)
        }
    }

    @Test
    fun afterAnOutcomeTheFramesStop() = runComposeUiTest {
        val host = fishing()
        refreshAndLand(host, FishingOutcome.NothingNew)
        mainClock.advanceTimeBy(500)
        assertTrue(host.state.awake)
        mainClock.advanceTimeBy(SettleMs)
        assertFalse(host.state.awake)
        assertTrue(host.state.rig.atRest)
    }

    @Test
    fun caughtItemsRiseFromZeroHeight() = runComposeUiTest {
        val host = fishing(rising = true)
        refreshAndLand(host, FishingOutcome.Caught(1))
        mainClock.advanceTimeBy(100)
        // Under the water the catch takes no room, all but a hair: the row after it starts at the
        // list's top.
        assertTrue(rowTop(1) <= 1.dp, "the catch took ${rowTop(1)} before it rose")
        mainClock.advanceTimeBy(SettleMs)
        assertEquals(80.dp, rowTop(1), "risen, it takes its full height")
        assertEquals(0.dp, rowTop(0))
    }

    @Test
    fun aCatchIsUnderWaterFromTheFirstFrameItLands() = runComposeUiTest {
        val host = fishing(rising = true)
        runOnIdle { host.status = FishingStatus.Refreshing }
        mainClock.advanceTimeBy(1_500)
        // The caller puts its catch on top as it lands: on the very frame it does, the catch must
        // already be under water, or it flashes up at full height before it sinks.
        runOnIdle { host.status = FishingStatus.Landed(FishingOutcome.Caught(1)) }
        mainClock.advanceTimeByFrame()
        assertTrue(rowTop(1) <= 1.dp, "the catch showed ${rowTop(1)} tall as it landed")
    }

    @Test
    fun theSameOutcomeTwiceIsAnnouncedTwice() = runComposeUiTest {
        val host = fishing()
        repeat(2) { round ->
            refreshAndLand(host, FishingOutcome.NothingNew)
            mainClock.advanceTimeBy(1_000)
            onNode(hasContentDescription("Nothing new")).assertExists()
            runOnIdle { host.status = FishingStatus.Idle }
            mainClock.advanceTimeBy(SettleMs)
            if (round == 0) {
                // Before the next outcome the old one is gone, so the next one is news again.
                runOnIdle { host.status = FishingStatus.Refreshing }
                mainClock.advanceTimeBy(300)
                onNode(hasContentDescription("Nothing new")).assertDoesNotExist()
                runOnIdle { host.status = FishingStatus.Idle }
                mainClock.advanceTimeBy(SettleMs)
            }
        }
    }

    @Test
    fun aRefreshAnsweredWithinAFrameStillPlaysOut() = runComposeUiTest {
        val host = fishing()
        // Refreshing and Landed in the same frame: composition only ever sees Landed.
        runOnIdle {
            host.status = FishingStatus.Refreshing
            host.status = FishingStatus.Landed(FishingOutcome.Caught(1))
        }
        mainClock.advanceTimeByFrame()
        assertEquals(FishingPhase.Casting, host.state.rig.phase, "the line must still be cast")
        mainClock.advanceTimeBy(3_000)
        onNode(hasContentDescription("1 new item")).assertExists()
    }

    @Test
    fun aRefreshAskedForWhileACatchPlaysIsCastOnceItHasRisen() = runComposeUiTest {
        val host = fishing(rising = true)
        refreshAndLand(host, FishingOutcome.Caught(1))
        mainClock.advanceTimeBy(1_000)
        assertTrue(host.state.rig.busy)
        // The caller starts another refresh while the catch is still playing out.
        runOnIdle { host.status = FishingStatus.Refreshing }
        var waited = 0
        while (host.state.rig.phase != FishingPhase.Waiting && waited < 6_000) {
            mainClock.advanceTimeBy(50)
            waited += 50
        }
        assertEquals(FishingPhase.Waiting, host.state.rig.phase, "the new refresh was never cast")
        runOnIdle { host.status = FishingStatus.Landed(FishingOutcome.NothingNew) }
        mainClock.advanceTimeBy(1_500)
        onNode(hasContentDescription("Nothing new")).assertExists()
    }

    @Test
    fun aCatchCalledOffBeforeItPlaysIsNotLeftUnderWater() = runComposeUiTest {
        val host = fishing(rising = true)
        refreshAndLand(host, FishingOutcome.Caught(1))
        runOnIdle { host.status = FishingStatus.Refreshing }
        mainClock.advanceTimeByFrame()
        runOnIdle { host.status = FishingStatus.Idle }
        mainClock.advanceTimeBy(SettleMs)
        assertEquals(80.dp, rowTop(1), "the catch was left under water")
    }

    @Test
    fun nothingRecomposesWhileACatchRises() = runComposeUiTest {
        var rows = 0
        val host = Host()
        setContent {
            CompositionLocalProvider(
                LocalFishingCompositionProbe provides { host.compositions++ }
            ) {
                val state = rememberFishingRefreshState()
                host.state = state
                FishingRefresh(
                    host.status,
                    onRefresh = {},
                    Modifier.size(360.dp, 640.dp),
                    state = state,
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(30) { i ->
                            SideEffect { rows++ }
                            Box(Modifier.fillMaxWidth().risingFromWater(state, i).height(80.dp))
                        }
                    }
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        refreshAndLand(host, FishingOutcome.Caught(2))
        var waited = 0
        while (host.state.rig.phase != FishingPhase.Reeling && waited < 5_000) {
            mainClock.advanceTimeBy(20)
            waited += 20
        }
        mainClock.advanceTimeByFrame()
        val refresh = host.compositions
        val items = rows
        mainClock.advanceTimeBy(900)
        assertEquals(FishingPhase.Reeling, host.state.rig.phase)
        assertEquals(refresh, host.compositions, "a rising catch must not recompose the refresh")
        assertEquals(items, rows, "a rising catch must not recompose the list's items")
    }

    @Test
    fun aCatchIsHauledUpFromTheDeepGrowingAsItComes() = runComposeUiTest {
        val host = Host()
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides host.ticks) {
                val state = rememberFishingRefreshState()
                host.state = state
                FishingRefresh(
                    host.status,
                    onRefresh = {},
                    Modifier.size(360.dp, 640.dp).testTag("fishing"),
                    state = state,
                ) {
                    LazyColumn(Modifier.fillMaxSize().testTag("list")) {
                        items(30) { i ->
                            // Only the catch is green, so where it is drawn can be told.
                            Box(
                                Modifier.testTag("row$i")
                                    .fillMaxWidth()
                                    .risingFromWater(state, i)
                                    .height(80.dp)
                                    .background(if (i == 0) Color.Green else Color.White)
                            )
                        }
                    }
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        refreshAndLand(host, FishingOutcome.Caught(1))
        var waited = 0
        while (host.state.rig.phase != FishingPhase.Reeling && waited < 5_000) {
            mainClock.advanceTimeBy(20)
            waited += 20
        }
        mainClock.advanceTimeBy(300)
        val early = green()
        mainClock.advanceTimeBy(700)
        val late = green()
        assertEquals(FishingPhase.Reeling, host.state.rig.phase)
        assertTrue(early.count > 40, "no catch drawn as it is hauled up (${early.count} px)")
        assertTrue(late.y < early.y - 20f, "it must come up: from ${early.y} to ${late.y} px")
        assertTrue(
            late.count > early.count,
            "it must grow as it comes: ${early.count}, ${late.count}",
        )
        // Once in, it is the list's own item in its place.
        mainClock.advanceTimeBy(SettleMs)
        assertEquals(80.dp, rowTop(1))
    }

    private class Green(val count: Int, val y: Float)

    // How many greenish pixels the screen shows, and how far down they are on average.
    private fun ComposeUiTest.green(): Green {
        val pixels = onNodeWithTag("fishing").captureToImage().toPixelMap()
        var count = 0
        var ys = 0f
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val c = pixels[x, y]
                // Green, however faint: clearly greener than it is red or blue.
                if (c.green - maxOf(c.red, c.blue) > 0.2f) {
                    count++
                    ys += y
                }
            }
        }
        return Green(count, if (count == 0) 0f else ys / count)
    }

    // How far row [i] starts below the list's own top.
    private fun ComposeUiTest.rowTop(i: Int): Dp =
        onNodeWithTag("row$i").getUnclippedBoundsInRoot().top -
            onNodeWithTag("list").getUnclippedBoundsInRoot().top

    @Test
    fun recreatedWhileRefreshingTheBobberFloatsAtOnce() = runComposeUiTest {
        val recreation = Recreation(this)
        val host = Host()
        fishing(host) { recreation.setContent(it) }
        runOnIdle { host.status = FishingStatus.Refreshing }
        mainClock.advanceTimeBy(2_000)
        val before = host.state
        recreation.saveAndRestore()
        assertTrue(host.state !== before, "the screen was made again")
        mainClock.advanceTimeByFrame()
        assertEquals(FishingPhase.Waiting, host.state.rig.phase, "it floats, with no second cast")
    }

    @Test
    fun recreatedWhileLandedNothingReplays() = runComposeUiTest {
        val recreation = Recreation(this)
        val host = Host()
        fishing(host) { recreation.setContent(it) }
        refreshAndLand(host, FishingOutcome.NothingNew)
        mainClock.advanceTimeBy(SettleMs)
        val before = host.state
        recreation.saveAndRestore()
        assertTrue(host.state !== before, "the screen was made again")
        mainClock.advanceTimeBy(1_000)
        assertEquals(FishingPhase.Idle, host.state.rig.phase)
        onNode(hasContentDescription("Nothing new")).assertDoesNotExist()
        assertFalse(host.state.awake)
    }

    @Test
    fun aPullDuringTheRiseIsIgnored() = runComposeUiTest {
        val host = fishing(rising = true)
        refreshAndLand(host, FishingOutcome.Caught(3))
        var waited = 0
        while (host.state.rig.phase != FishingPhase.Reeling && waited < 5_000) {
            mainClock.advanceTimeBy(20)
            waited += 20
        }
        assertTrue(host.state.rig.carrying)
        // Pulled and let go well inside the haul: it isn't taken.
        pullList(by = 320.dp)
        assertTrue(host.state.rig.carrying, "the pull outlasted the haul")
        mainClock.advanceTimeBy(SettleMs)
        assertEquals(0, host.refreshes)
        assertEquals(FishingPhase.Idle, host.state.rig.phase)
        assertTrue(host.ticks.types.none { it == HapticFeedbackType.VirtualKey }, "no threshold")
    }
}
