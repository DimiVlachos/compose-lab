package dev.dimvlachos.moodboard.ios

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import dev.dimvlachos.moodboard.boards.BoardEditorContent
import dev.dimvlachos.moodboard.boards.BoardEditorViewModel
import dev.dimvlachos.moodboard.detail.PhotoDetailContent
import dev.dimvlachos.moodboard.detail.PhotoDetailViewModel
import dev.dimvlachos.moodboard.filter.FilterSheetContent
import dev.dimvlachos.moodboard.gallery.GalleryAction
import dev.dimvlachos.moodboard.gallery.GalleryViewModel
import dev.dimvlachos.moodboard.ui.theme.MoodboardTheme
import platform.UIKit.UIViewController

// Each factory hosts one shared Compose screen inside SwiftUI. The SwiftUI screen passes in the
// ViewModel it already observes, so its native bars and the Compose content share one state.
// The views are transparent: SwiftUI paints the background, and the glass samples what's below.

fun PhotoDetailViewController(viewModel: PhotoDetailViewModel): UIViewController = hosted {
    val state by viewModel.state.collectAsState()
    PhotoDetailContent(state)
}

fun FilterSheetViewController(viewModel: GalleryViewModel): UIViewController = hosted {
    val state by viewModel.state.collectAsState()
    FilterSheetContent(
        filter = state.filter,
        allTags = state.allTags,
        onFilterChange = { viewModel.onAction(GalleryAction.SetFilter(it)) },
    )
}

fun BoardEditorViewController(viewModel: BoardEditorViewModel): UIViewController = hosted {
    val state by viewModel.state.collectAsState()
    BoardEditorContent(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalComposeUiApi::class)
private fun hosted(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController(configure = { opaque = false }) {
        MoodboardTheme {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                content = content,
            )
        }
    }
