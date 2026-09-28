package dev.dimvlachos.lab.core.presentation.components.gallery

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.presentation.components.imagemorph.LocalMorphCompositionProbe
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphEnd
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarSourceTag
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarTargetTag
import dev.dimvlachos.lab.core.presentation.components.profile.DialogScrimTag
import dev.dimvlachos.lab.core.presentation.components.profile.FabDialogTag
import dev.dimvlachos.lab.core.presentation.components.profile.FabSourceTag
import dev.dimvlachos.lab.core.presentation.components.profile.ScrimTag
import dev.dimvlachos.lab.core.presentation.components.profile.SearchPillTag
import dev.dimvlachos.lab.core.presentation.components.profile.SearchSourceTag
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.portrait
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProfileGalleryUiTest {
    private var scene by mutableStateOf(GalleryScene())
    private val reported = mutableListOf<GalleryScene>()

    private val backInput = DirectNavigationEventInput()

    private fun ComposeUiTest.showGallery(
        probe: ((MorphEnd) -> Unit)? = null,
        scroll: ScrollState = ScrollState(0),
        query: String = "xos",
    ) {
        mainClock.autoAdvance = false
        setContent {
            val owner = rememberNavigationEventDispatcherOwner(parent = null)
            DisposableEffect(owner) {
                owner.navigationEventDispatcher.addInput(backInput)
                onDispose { owner.navigationEventDispatcher.removeInput(backInput) }
            }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                LabTheme {
                    CompositionLocalProvider(LocalMorphCompositionProbe provides probe) {
                        ProfileGallery(
                            name = "Alex Morgan",
                            portrait = Res.drawable.portrait,
                            photos = testIslands(),
                            searchQuery = query,
                            recentSearches = listOf("Santorini", "Milos", "Hydra"),
                            scene = scene,
                            onSceneChange = { reported += it },
                            scrollState = scroll,
                        )
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
    }

    private fun ComposeUiTest.go(to: GalleryScene, thenMs: Long = 1_500) {
        runOnUiThread { scene = to }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(thenMs)
    }

    @Test
    fun eachTriggerReportsTheNextScene() = runComposeUiTest {
        showGallery()
        // Apart, as a person taps: taps inside one morph's window are serialised by the gate.
        onNodeWithText("Paxos").performClick()
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag(AvatarSourceTag).performClick()
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag(SearchSourceTag).performClick()
        assertEquals(
            listOf(
                GalleryScene(photo = 2),
                GalleryScene(avatar = true),
                GalleryScene(search = true),
            ),
            reported,
        )
    }

    @Test
    fun photoCardAndDetailOverlapWhileMorphing() = runComposeUiTest {
        showGallery()
        go(GalleryScene(photo = 3), thenMs = 100)
        onAllNodesWithText("Santorini").assertCountEquals(2)
    }

    @Test
    fun theFabOnlyExistsOverTheZoomedAvatarAndOpensTheDialog() = runComposeUiTest {
        showGallery()
        onNodeWithTag(FabSourceTag).assertDoesNotExist()
        go(GalleryScene(avatar = true))
        onNodeWithTag(FabSourceTag).performClick()
        assertEquals(GalleryScene(avatar = true, dialog = true), reported.last())
        go(GalleryScene(avatar = true, dialog = true))
        onNodeWithTag(FabDialogTag).assertExists()
        onNodeWithTag(FabSourceTag).assertDoesNotExist()
        onNodeWithTag(DialogScrimTag).performClick()
        assertEquals(GalleryScene(avatar = true), reported.last(), "only the dialog closes")
        go(GalleryScene(avatar = true))
        onNodeWithTag(FabSourceTag).assertExists()
        onNodeWithTag(AvatarTargetTag).assertExists()
    }

    @Test
    fun theAvatarScrimClosesWithoutReachingTheScreen() = runComposeUiTest {
        showGallery()
        go(GalleryScene(avatar = true))
        onNodeWithTag(ScrimTag).performClick()
        assertEquals(listOf(GalleryScene()), reported)
    }

    // A title in the search page's own grid, as opposed to the home grid under it.
    private fun ComposeUiTest.inSearch(title: String) =
        onAllNodes(hasText(title) and hasAnyAncestor(hasTestTag(SearchGridTag)))

    @Test
    fun searchShowsItsOwnFilteredGridOverAnUntouchedHome() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 2_500)
        inSearch("Paxos").assertCountEquals(1)
        inSearch("Naxos").assertCountEquals(1)
        for (gone in listOf("Corfu", "Santorini", "Milos", "Hydra")) {
            inSearch(gone).assertCountEquals(0)
        }
        onNodeWithTag(ProfileNameTag).assertExists() // home is still there, under the page
        go(GalleryScene(), thenMs = 2_000)
        onNodeWithTag(SearchGridTag).assertDoesNotExist()
        for (title in IslandTitles) onAllNodesWithText(title).assertCountEquals(1)
    }

    @Test
    fun aPhotoOpensFromTheSearchResultsAndReturnsToThem() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 2_500)
        inSearch("Naxos").onFirst().performClick()
        assertEquals(GalleryScene(photo = 5, search = true), reported.last())
        go(GalleryScene(photo = 5, search = true))
        onNodeWithText("Caption Naxos").assertExists()
        go(GalleryScene(search = true))
        onNodeWithText("Caption Naxos").assertDoesNotExist()
        inSearch("Naxos").assertCountEquals(1)
        inSearch("Corfu").assertCountEquals(0)
    }

    @Test
    fun anIllegalSceneShowsTheNearestLegalOne() = runComposeUiTest {
        scene = GalleryScene(photo = 9, dialog = true)
        showGallery()
        for (title in IslandTitles) onAllNodesWithText(title).assertCountEquals(1)
        onNodeWithTag(AvatarTargetTag).assertDoesNotExist()
        onNodeWithTag(FabDialogTag).assertDoesNotExist()
    }

    @Test
    fun nothingRecomposesDuringThePhotoAndAvatarMorphs() = runComposeUiTest {
        var sources = 0
        var targets = 0
        showGallery(probe = { end -> if (end == MorphEnd.Card) sources++ else targets++ })
        for (open in listOf(GalleryScene(photo = 2), GalleryScene(avatar = true))) {
            go(open) // warm-up: first decode and first overlay
            go(GalleryScene())
            waitForIdle()
            go(open, thenMs = 32)
            val afterOpen = sources to targets
            mainClock.advanceTimeBy(1_500)
            assertEquals(afterOpen, sources to targets, "$open recomposed while animating")
            go(GalleryScene())
        }
    }

    @Test
    fun theAvatarScrimClosesFromAnywhereAroundTheCircle() = runComposeUiTest {
        showGallery()
        val spots: List<TouchInjectionScope.() -> Offset> =
            listOf({ topCenter }, { centerRight }, { bottomCenter }, { bottomLeft }, { centerLeft })
        for (spot in spots) {
            reported.clear()
            go(GalleryScene(avatar = true))
            onNodeWithTag(ScrimTag).performTouchInput { click(spot()) }
            assertEquals(
                listOf(GalleryScene()),
                reported,
                "a tap at ${onNodeWithTag(ScrimTag).fetchSemanticsNode().size} failed",
            )
            go(GalleryScene())
        }
    }

    @Test
    fun theHeaderAndGridScrollUnderTheBar() = runComposeUiTest {
        showGallery()
        val before = onNodeWithTag(ProfileNameTag).getBoundsInRoot().top
        // A scroll animates, so the clock must run on its own for performScrollTo to return.
        mainClock.autoAdvance = true
        onNodeWithText("Hydra").performScrollTo()
        waitForIdle()
        onNodeWithText("Hydra").assertIsDisplayed()
        assertTrue(
            onNodeWithTag(ProfileNameTag).getBoundsInRoot().top < before,
            "the header scrolled",
        )
    }

    @Test
    fun theFabSitsThirtySixDpFromTheBottomAndEndEdges() = runComposeUiTest {
        showGallery()
        go(GalleryScene(avatar = true))
        val fab = onNodeWithTag(FabSourceTag).getBoundsInRoot()
        val root = onRoot().getBoundsInRoot()
        assertEquals(36.dp, root.right - fab.right)
        assertEquals(36.dp, root.bottom - fab.bottom)
    }

    @Test
    fun theGridEndsSixteenDpAboveTheBottomWhenScrolledToTheEnd() = runComposeUiTest {
        showGallery()
        mainClock.autoAdvance = true
        onNodeWithTag(GalleryScrollTag).performTouchInput { repeat(4) { swipeUp() } }
        waitForIdle()
        val grid = onNodeWithTag(GalleryGridTag).getBoundsInRoot()
        assertEquals(16.dp, onRoot().getBoundsInRoot().bottom - grid.bottom)
    }

    @Test
    fun theDetailCaptionEndsSixteenDpAboveTheBottom() = runComposeUiTest {
        showGallery()
        go(GalleryScene(photo = 3), thenMs = 2_000)
        val caption = onNodeWithText("Caption Santorini").getBoundsInRoot()
        assertEquals(16.dp, onRoot().getBoundsInRoot().bottom - caption.bottom)
    }

    @Test
    fun reopeningAPhotoNeverLeavesAnOldEndBehind() = runComposeUiTest {
        showGallery()
        repeat(3) { round ->
            go(GalleryScene(photo = 3), thenMs = 100)
            onAllNodesWithText("Caption Santorini").assertCountEquals(1)
            onAllNodesWithText("Santorini").assertCountEquals(2) // the card and the detail
            mainClock.advanceTimeBy(1_500)
            go(GalleryScene(), thenMs = 100)
            onAllNodesWithText("Santorini").assertCountEquals(2)
            mainClock.advanceTimeBy(1_500)
            onAllNodesWithText("Santorini").assertCountEquals(1)
            onAllNodesWithText("Caption Santorini").assertCountEquals(0)
        }
    }

    @Test
    fun closingMidOpenLeavesNoOldEndBehind() = runComposeUiTest {
        showGallery()
        repeat(2) {
            go(GalleryScene(photo = 3), thenMs = 200) // still growing
            go(GalleryScene(), thenMs = 2_000)
            onAllNodesWithText("Caption Santorini").assertCountEquals(0)
            onAllNodesWithText("Santorini").assertCountEquals(1)
        }
        go(GalleryScene(photo = 3), thenMs = 100)
        onAllNodesWithText("Caption Santorini").assertCountEquals(1)
        onAllNodesWithText("Santorini").assertCountEquals(2)
    }

    @Test
    fun aCloseDuringTheOpenWaitsForTheMorphToLand() = runComposeUiTest {
        mainClock.autoAdvance = false
        var live by mutableStateOf(GalleryScene())
        setContent {
            LabTheme {
                ProfileGallery(
                    name = "Alex Morgan",
                    portrait = Res.drawable.portrait,
                    photos = testIslands(),
                    searchQuery = "xos",
                    scene = live,
                    onSceneChange = { live = it },
                )
            }
        }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Paxos").performClick()
        mainClock.advanceTimeBy(100) // still growing
        onNodeWithContentDescription("Close").performClick()
        mainClock.advanceTimeBy(50)
        assertEquals(GalleryScene(photo = 2), live, "the close must not cut into the open")
        mainClock.advanceTimeBy(1_500)
        assertEquals(GalleryScene(), live, "and happens once the photo has landed")
    }

    @Test
    fun theAvatarShrinksToTheSearchButtonAsTheGridScrolls() = runComposeUiTest {
        val scroll = ScrollState(0)
        showGallery(scroll = scroll)
        assertEquals(
            88.dp,
            onNodeWithTag(AvatarSourceTag).getBoundsInRoot().let { it.right - it.left },
        )
        runOnUiThread { scroll.dispatchRawDelta(1_000f) }
        mainClock.advanceTimeBy(100)
        assertEquals(
            40.dp,
            onNodeWithTag(AvatarSourceTag).getBoundsInRoot().let { it.right - it.left },
        )
        assertEquals(
            40.dp,
            onNodeWithTag(SearchSourceTag).getBoundsInRoot().let { it.right - it.left },
        )
        runOnUiThread { scroll.dispatchRawDelta(-1_000f) }
        mainClock.advanceTimeBy(100)
        assertEquals(
            88.dp,
            onNodeWithTag(AvatarSourceTag).getBoundsInRoot().let { it.right - it.left },
        )
    }

    @Test
    fun theResultsStartSixteenDpBelowTheSearchBar() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 2_500)
        val pill = onNodeWithTag(SearchPillTag).getBoundsInRoot()
        val grid = onNodeWithTag(SearchGridTag).getBoundsInRoot()
        assertEquals(16.dp, grid.top - pill.bottom)
    }

    @Test
    fun scrollingTheGridRecomposesNoMorphEnd() = runComposeUiTest {
        var compositions = 0
        val scroll = ScrollState(0)
        showGallery(probe = { compositions++ }, scroll = scroll)
        val before = compositions
        repeat(20) {
            runOnUiThread { scroll.dispatchRawDelta(12f) }
            mainClock.advanceTimeByFrame()
        }
        assertEquals(before, compositions)
    }

    @Test
    fun onceCollapsedTheGridStartsEightDpBelowTheBar() = runComposeUiTest {
        val scroll = ScrollState(0)
        showGallery(scroll = scroll)
        val range = with(density) { (HeaderExpandedHeight - HeaderCollapsedHeight).toPx() }
        runOnUiThread { scroll.dispatchRawDelta(range) }
        mainClock.advanceTimeBy(100)
        val bar = onNodeWithTag(GalleryHeaderTag).getBoundsInRoot()
        val grid = onNodeWithTag(GalleryGridTag).getBoundsInRoot()
        assertEquals(64.dp, bar.bottom - bar.top, "fully collapsed")
        assertEquals(8.dp, grid.top - bar.bottom) // plus the 8 dp shadow: 16 dp to the eye
    }

    @Test
    fun searchNeverMovesTheHomeGrid() = runComposeUiTest {
        val scroll = ScrollState(0)
        showGallery(scroll = scroll)
        runOnUiThread { scroll.dispatchRawDelta(400f) }
        mainClock.advanceTimeBy(100)
        go(GalleryScene(search = true), thenMs = 2_500)
        assertEquals(400, scroll.value)
        go(GalleryScene(), thenMs = 2_500)
        assertEquals(400, scroll.value)
    }

    @Test
    fun searchShowsRecentSearchesUntilTheQueryTypesIn() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 300) // the bar is still landing
        onNodeWithText("Recent searches").assertExists()
        onNodeWithTag(SearchGridTag).assertDoesNotExist()
        mainClock.advanceTimeBy(2_200)
        onNodeWithText("Recent searches").assertDoesNotExist()
        inSearch("Paxos").assertCountEquals(1)
    }

    @Test
    fun aQueryWithNoMatchesShowsTheEmptyState() = runComposeUiTest {
        showGallery(query = "zzz")
        go(GalleryScene(search = true), thenMs = 2_500)
        onNodeWithText("No photos match \u201Czzz\u201D").assertExists()
        onNodeWithTag(SearchGridTag).assertDoesNotExist()
    }

    @Test
    fun tappingARecentSearchFillsTheQuery() = runComposeUiTest {
        showGallery(query = "")
        go(GalleryScene(search = true), thenMs = 2_500)
        onNode(hasText("Milos") and hasAnyAncestor(hasTestTag(RecentSearchesTag))).performClick()
        mainClock.advanceTimeBy(1_000)
        inSearch("Milos").assertCountEquals(1)
        inSearch("Corfu").assertCountEquals(0)
    }

    @Test
    fun theResultsStayWhileThePageFadesOut() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 2_500)
        go(GalleryScene(), thenMs = 100)
        inSearch("Paxos").assertCountEquals(1) // not swapped for the recent searches mid-close
        onNodeWithText("Recent searches").assertDoesNotExist()
    }

    @Test
    fun expandedThePhotosStayTwentyDpUnderTheAvatar() = runComposeUiTest {
        showGallery()
        val avatar = onNodeWithTag(AvatarSourceTag).getBoundsInRoot()
        val grid = onNodeWithTag(GalleryGridTag).getBoundsInRoot()
        assertEquals(20.dp, grid.top - avatar.bottom)
    }

    @Test
    fun tapsOnTheCollapsedBarNeverReachThePhotosUnderIt() = runComposeUiTest {
        val scroll = ScrollState(0)
        showGallery(scroll = scroll)
        val range = with(density) { (HeaderExpandedHeight - HeaderCollapsedHeight).toPx() }
        runOnUiThread { scroll.dispatchRawDelta(range + 200f) } // the first row is under the bar
        mainClock.advanceTimeBy(100)
        onNodeWithTag(ProfileNameTag).performClick()
        val bar = onNodeWithTag(GalleryHeaderTag).getBoundsInRoot()
        val band = with(density) { (bar.bottom + GridTopGap / 2).toPx() }
        onNodeWithTag(GalleryScrollTag).performTouchInput { click(Offset(centerX, band)) }
        mainClock.advanceTimeBy(1_000)
        assertEquals(emptyList(), reported)
    }

    @Test
    fun aSecondSearchStartsFromTheRecentSearches() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 2_500)
        go(GalleryScene(), thenMs = 2_500)
        go(GalleryScene(search = true), thenMs = 50)
        onNodeWithTag(SearchGridTag).assertDoesNotExist() // no flash of the last results
        onNodeWithText("Recent searches").assertExists()
    }

    @Test
    fun aRecentSearchPickedWhileTheBarLandsIsKept() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 100)
        onNode(hasText("Milos") and hasAnyAncestor(hasTestTag(RecentSearchesTag))).performClick()
        mainClock.advanceTimeBy(2_500)
        inSearch("Milos").assertCountEquals(1)
        inSearch("Paxos").assertCountEquals(0) // the scripted "xos" did not type over it
    }

    @Test
    fun theResultsGridIsOnlyAsTallAsItsMatches() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 2_500)
        // Its own size, not its bounds in the root, which the scroll viewport clips.
        val grid = onNodeWithTag(SearchGridTag).fetchSemanticsNode().size
        // Two matches: one row of 4:5 cards at half the width, not six rows of empty slots.
        assertTrue(grid.height < grid.width, "one row, got $grid")
    }

    private fun ComposeUiTest.pressBack() {
        runOnUiThread { backInput.backCompleted() }
        mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun systemBackClosesWhateverIsOpenOneLayerAtATime() = runComposeUiTest {
        showGallery()
        go(GalleryScene(avatar = true, dialog = true))
        pressBack()
        go(GalleryScene(photo = 2, search = true))
        pressBack()
        go(GalleryScene(search = true))
        pressBack()
        assertEquals(
            listOf(
                GalleryScene(avatar = true),
                GalleryScene(search = true),
                GalleryScene(),
            ),
            reported,
        )
    }

    @Test
    fun systemBackOnTheBareScreenIsLeftToTheApp() = runComposeUiTest {
        var appBacks = 0
        mainClock.autoAdvance = false
        setContent {
            val owner = rememberNavigationEventDispatcherOwner(parent = null)
            DisposableEffect(owner) {
                owner.navigationEventDispatcher.addInput(backInput)
                onDispose { owner.navigationEventDispatcher.removeInput(backInput) }
            }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                NavigationBackHandler(
                    state = rememberNavigationEventState(NavigationEventInfo.None),
                    onBackCompleted = { appBacks++ },
                )
                LabTheme {
                    ProfileGallery(
                        name = "Alex Morgan",
                        portrait = Res.drawable.portrait,
                        photos = testIslands(),
                        searchQuery = "xos",
                        scene = scene,
                        onSceneChange = { reported += it },
                    )
                }
            }
        }
        mainClock.advanceTimeBy(500)
        pressBack()
        assertEquals(1, appBacks)
        assertEquals(emptyList(), reported)
    }

    @Test
    fun tapsOnAnOpenPhotoNeverReachTheGridUnderIt() = runComposeUiTest {
        showGallery()
        go(GalleryScene(photo = 3))
        onRoot().performTouchInput { click(Offset(centerX / 2, centerY)) } // a card lies below
        mainClock.advanceTimeBy(1_000)
        assertEquals(emptyList(), reported)
    }
}
