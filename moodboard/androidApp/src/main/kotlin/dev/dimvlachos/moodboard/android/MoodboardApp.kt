package dev.dimvlachos.moodboard.android

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
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
import dev.dimvlachos.moodboard.android.nav.PhotoDetail
import dev.dimvlachos.moodboard.android.nav.Search
import dev.dimvlachos.moodboard.android.search.SearchScreen
import dev.dimvlachos.moodboard.android.ui.LocalSharedTransitionScope
import dev.dimvlachos.moodboard.android.ui.MoodboardNavigationBar
import dev.dimvlachos.moodboard.morph.rememberMorphGate
import dev.dimvlachos.moodboard.ui.theme.MoodboardTheme

// Material's fade-through: the container transform carries the photo, the rest cross-fades.
private const val FadeInMs = 220
private const val FadeOutMs = 90

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoodboardApp() {
    MoodboardTheme {
        val backStack = rememberNavBackStack(Gallery)
        SharedTransitionLayout {
            // Opens and closes queue behind a morph in flight instead of reversing it.
            val gate = rememberMorphGate()
            val push: (NavKey) -> Unit = { key -> gate { backStack.add(key) } }
            val pop: () -> Unit = {
                gate { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
            }
            val selectTab: (NavKey) -> Unit = { tab ->
                backStack.clear()
                backStack.add(tab)
            }
            val bottomBar: @Composable () -> Unit = {
                MoodboardNavigationBar(current = backStack.first(), onSelect = selectTab)
            }
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavDisplay(
                    backStack = backStack,
                    onBack = pop,
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
                                GalleryScreen(onOpenPhoto = { push(PhotoDetail(it)) }, bottomBar)
                            }
                            entry<Boards> {
                                BoardsScreen(onOpenBoard = { push(BoardDetail(it)) }, bottomBar)
                            }
                            entry<Search> {
                                SearchScreen(onOpenPhoto = { push(PhotoDetail(it)) }, bottomBar)
                            }
                            entry<PhotoDetail> { PhotoDetailScreen(it.photoId, onBack = pop) }
                            entry<BoardDetail> {
                                BoardDetailScreen(
                                    boardId = it.boardId,
                                    onBack = pop,
                                    onOpenPhoto = { id -> push(PhotoDetail(id)) },
                                    onEdit = { push(BoardEditor(it.boardId)) },
                                )
                            }
                            entry<BoardEditor> { BoardEditorScreen(it.boardId, onBack = pop) }
                        },
                )
            }
        }
    }
}
