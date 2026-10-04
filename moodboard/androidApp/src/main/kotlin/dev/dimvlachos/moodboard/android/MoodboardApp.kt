package dev.dimvlachos.moodboard.android

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.dimvlachos.moodboard.android.boards.BoardDetailScreen
import dev.dimvlachos.moodboard.android.boards.BoardEditorScreen
import dev.dimvlachos.moodboard.android.boards.BoardsScreen
import dev.dimvlachos.moodboard.android.detail.PhotoDetailScreen
import dev.dimvlachos.moodboard.android.gallery.GalleryScreen
import dev.dimvlachos.moodboard.android.nav.BoardDetail
import dev.dimvlachos.moodboard.android.nav.BoardEditor
import dev.dimvlachos.moodboard.android.nav.Boards
import dev.dimvlachos.moodboard.android.nav.Gallery
import dev.dimvlachos.moodboard.android.nav.ParallaxPush
import dev.dimvlachos.moodboard.android.nav.PhotoDetail
import dev.dimvlachos.moodboard.android.nav.Search
import dev.dimvlachos.moodboard.android.nav.TopLevelBackStack
import dev.dimvlachos.moodboard.android.search.SearchScreen
import dev.dimvlachos.moodboard.android.ui.LocalMorphViewports
import dev.dimvlachos.moodboard.android.ui.LocalSharedTransitionScope
import dev.dimvlachos.moodboard.android.ui.MoodboardNavigationBar
import dev.dimvlachos.moodboard.morph.MorphBackdropIn
import dev.dimvlachos.moodboard.morph.MorphBackdropOut
import dev.dimvlachos.moodboard.morph.MorphViewports
import dev.dimvlachos.moodboard.morph.rememberMorphGate
import dev.dimvlachos.moodboard.nav.LiveIdsViewModel
import dev.dimvlachos.moodboard.ui.theme.MoodboardTheme

// Tab switches cross-fade: the screen on top fades over the other, which stays fully drawn
// underneath. Fading both at once (the old one out in 90 ms) dipped to the bare background in
// between, which read as a cut. Inner screens bring their own transitions (morph, parallax).
private const val TabFadeMs = 200

// KeepUntilTransitionsFinished is only reachable inside a transition scope, hence a function.
private fun AnimatedContentTransitionScope<*>.tabFadeIn(): ContentTransform =
    fadeIn(tween(TabFadeMs, easing = LinearOutSlowInEasing)) togetherWith
        ExitTransition.KeepUntilTransitionsFinished

// On a pop the leaving screen is the one on top, so it fades off the one returning.
private val TabFadeOut: ContentTransform =
    EnterTransition.None togetherWith fadeOut(tween(TabFadeMs, easing = LinearOutSlowInEasing))

private val Tabs: List<NavKey> = listOf(Gallery, Boards, Search)

// The photo detail opens over a grid that stays put, its backdrop fading with the morph; on the way
// back the grid is already underneath and the detail fades off it. Predictive back seeks the same.
private val ContainerTransform =
    NavDisplay.transitionSpec {
        MorphBackdropIn togetherWith ExitTransition.KeepUntilTransitionsFinished
    } +
        NavDisplay.popTransitionSpec { EnterTransition.None togetherWith MorphBackdropOut } +
        NavDisplay.predictivePopTransitionSpec {
            EnterTransition.None togetherWith MorphBackdropOut
        }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoodboardApp() {
    MoodboardTheme {
        // One saveable back stack per tab: each survives tab switches, rotation and process death.
        val stacks =
            mapOf(
                Gallery to rememberNavBackStack(Gallery),
                Boards to rememberNavBackStack(Boards),
                Search to rememberNavBackStack(Search),
            )
        val currentTab = rememberSaveable(saver = TabSaver) { mutableStateOf(Gallery) }
        val stack = remember(stacks) { TopLevelBackStack(Gallery, stacks, currentTab) }

        // Screens never pop themselves on a delete; their keys leave every tab here instead.
        val liveIds by viewModel { LiveIdsViewModel() }.state.collectAsStateWithLifecycle()
        LaunchedEffect(liveIds) { stack.prune(liveIds.photoIds, liveIds.boardIds) }

        val viewports = remember { MorphViewports() }
        SharedTransitionLayout(
            Modifier.onGloballyPositioned { viewports.layoutTopInWindow = it.positionInWindow().y }
        ) {
            // Taps and tab switches queue behind a morph in flight instead of cutting it off.
            // System back is gated too. Popping a photo whose open morph is still running made
            // NavDisplay rewind the push (its cancelled-gesture path): the grid wasn't drawn until
            // the photo landed, and the next open kept the grid on screen. Now Back waits for the
            // morph to land, at most its length, then closes it; with nothing in flight it pops at
            // once, predictive gesture included.
            val gate = rememberMorphGate()
            val push: (NavKey) -> Unit = { key -> gate.run { stack.push(key) } }
            val toolbarBack: () -> Unit = { gate.run { stack.pop() } }
            val openPhoto: (String) -> Unit = { id ->
                push(PhotoDetail(id, tab = stack.currentTab.toString()))
            }
            val bottomBar: @Composable () -> Unit = {
                MoodboardNavigationBar(
                    current = stack.currentTab,
                    onSelect = { tab -> gate.run { stack.selectTab(tab) } },
                )
            }
            val provider =
                entryProvider<NavKey> {
                    entry<Gallery> { GalleryScreen(openPhoto, bottomBar) }
                    entry<Boards> { BoardsScreen({ push(BoardDetail(it)) }, bottomBar) }
                    entry<Search> {
                        SearchScreen(
                            onOpenPhoto = openPhoto,
                            bottomBar = bottomBar,
                            isTopScreen =
                                stack.currentTab == Search &&
                                    stacks.getValue(Search).last() == Search,
                            // Back is handled here instead of by NavDisplay: drop queued taps.
                            onBackHandled = gate::cancelPending,
                        )
                    }
                    entry<PhotoDetail>(metadata = ContainerTransform) {
                        PhotoDetailScreen(it.photoId, it.tab, onBack = toolbarBack)
                    }
                    entry<BoardDetail>(metadata = ParallaxPush) {
                        BoardDetailScreen(
                            boardId = it.boardId,
                            onBack = toolbarBack,
                            onOpenPhoto = openPhoto,
                            onEdit = { push(BoardEditor(it.boardId)) },
                        )
                    }
                    entry<BoardEditor>(metadata = ParallaxPush) {
                        BoardEditorScreen(it.boardId, onBack = toolbarBack)
                    }
                }
            // Each tab decorates its own entries, so a hidden tab keeps its screens' ViewModels and
            // saved state (scroll, query, app bar) until they are really popped.
            val entries = Tabs.associateWith { tab -> tabEntries(stacks.getValue(tab), provider) }
            val shown =
                entries.getValue(Gallery) +
                    if (stack.currentTab == Gallery) emptyList()
                    else entries.getValue(stack.currentTab)

            CompositionLocalProvider(
                LocalSharedTransitionScope provides this,
                LocalMorphViewports provides viewports,
            ) {
                NavDisplay(
                    entries = shown,
                    onBack = { gate.run { stack.pop() } },
                    transitionSpec = { tabFadeIn() },
                    popTransitionSpec = { TabFadeOut },
                    predictivePopTransitionSpec = { TabFadeOut },
                )
            }
        }
    }
}

@Composable
private fun tabEntries(
    backStack: List<NavKey>,
    provider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> =
    rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider = provider,
    )

private val TabSaver =
    Saver<MutableState<NavKey>, Int>(
        save = { Tabs.indexOf(it.value) },
        restore = { mutableStateOf(Tabs[it]) },
    )
