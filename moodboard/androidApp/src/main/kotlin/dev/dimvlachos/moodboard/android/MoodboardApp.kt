package dev.dimvlachos.moodboard.android

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
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
import dev.dimvlachos.moodboard.android.nav.NavigationViewModel
import dev.dimvlachos.moodboard.android.nav.PhotoDetail
import dev.dimvlachos.moodboard.android.nav.Search
import dev.dimvlachos.moodboard.android.search.SearchScreen
import dev.dimvlachos.moodboard.android.ui.LocalSharedTransitionScope
import dev.dimvlachos.moodboard.android.ui.MoodboardNavigationBar
import dev.dimvlachos.moodboard.boards.BoardsViewModel
import dev.dimvlachos.moodboard.gallery.GalleryViewModel
import dev.dimvlachos.moodboard.morph.rememberMorphGate
import dev.dimvlachos.moodboard.search.SearchViewModel
import dev.dimvlachos.moodboard.ui.theme.MoodboardTheme

// Material's fade-through: the container transform carries the photo, the rest cross-fades.
private const val FadeInMs = 220
private const val FadeOutMs = 90

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoodboardApp() {
    MoodboardTheme {
        val stack = viewModel { NavigationViewModel() }.stack
        // The tab roots live above NavDisplay, so a tab switch keeps their filter, query and
        // scroll position the way an iOS TabView does.
        val gallery = viewModel { GalleryViewModel() }
        val boards = viewModel { BoardsViewModel() }
        val search = viewModel { SearchViewModel() }
        val galleryGrid = rememberLazyGridState()
        val searchGrid = rememberLazyGridState()
        val boardsList = rememberLazyListState()

        SharedTransitionLayout {
            // Taps and tab switches queue behind a morph in flight instead of cutting it off.
            // System back is not
            // gated: a predictive-back gesture already owns its transition, so it pops at once.
            val gate = rememberMorphGate()
            val push: (NavKey) -> Unit = { key -> gate { stack.push(key) } }
            val toolbarBack: () -> Unit = { gate { stack.pop() } }
            val openPhoto: (String) -> Unit = { id ->
                push(PhotoDetail(id, tab = stack.currentTab.toString()))
            }
            val bottomBar: @Composable () -> Unit = {
                MoodboardNavigationBar(
                    current = stack.currentTab,
                    onSelect = { tab -> gate { stack.selectTab(tab) } },
                )
            }
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavDisplay(
                    backStack = stack.backStack,
                    onBack = { stack.pop() },
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                    transitionSpec = {
                        fadeIn(tween(FadeInMs)) togetherWith fadeOut(tween(FadeOutMs))
                    },
                    popTransitionSpec = {
                        fadeIn(tween(FadeInMs)) togetherWith fadeOut(tween(FadeOutMs))
                    },
                    predictivePopTransitionSpec = {
                        fadeIn(tween(FadeInMs)) togetherWith fadeOut(tween(FadeOutMs))
                    },
                    entryProvider =
                        entryProvider {
                            entry<Gallery> {
                                GalleryScreen(gallery, galleryGrid, openPhoto, bottomBar)
                            }
                            entry<Boards> {
                                BoardsScreen(
                                    boards,
                                    boardsList,
                                    { push(BoardDetail(it)) },
                                    bottomBar,
                                )
                            }
                            entry<Search> { SearchScreen(search, searchGrid, openPhoto, bottomBar) }
                            entry<PhotoDetail> {
                                PhotoDetailScreen(it.photoId, onBack = toolbarBack)
                            }
                            entry<BoardDetail> {
                                BoardDetailScreen(
                                    boardId = it.boardId,
                                    onBack = toolbarBack,
                                    onOpenPhoto = openPhoto,
                                    onEdit = { push(BoardEditor(it.boardId)) },
                                )
                            }
                            entry<BoardEditor> {
                                BoardEditorScreen(it.boardId, onBack = toolbarBack)
                            }
                        },
                )
            }
        }
    }
}
