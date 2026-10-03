package dev.dimvlachos.moodboard.android.gallery

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.share.rememberShareLauncher
import dev.dimvlachos.moodboard.android.ui.BoardNameDialog
import dev.dimvlachos.moodboard.android.ui.DeletePhotoDialog
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.android.ui.PhotoGrid
import dev.dimvlachos.moodboard.android.ui.PhotoMenuItems
import dev.dimvlachos.moodboard.filter.FilterSheetContent
import dev.dimvlachos.moodboard.gallery.GalleryAction
import dev.dimvlachos.moodboard.gallery.GalleryViewModel
import dev.dimvlachos.moodboard.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GalleryScreen(onOpenPhoto: (String) -> Unit, bottomBar: @Composable () -> Unit) {
    val viewModel = viewModel { GalleryViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val share = rememberShareLauncher()
    // Ids, saved: an open dialog or sheet survives rotation.
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    var newBoardForId by rememberSaveable { mutableStateOf<String?>(null) }
    val deleting = state.photos.firstOrNull { it.id == deletingId }
    val newBoardFor = state.photos.firstOrNull { it.id == newBoardForId }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Gallery", Modifier.semantics { heading() }) },
                actions = {
                    IconButton(onClick = { filterOpen = true }) {
                        MoodboardIcon(R.drawable.ic_filter, "Filter")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        PhotoGrid(
            photos = state.photos,
            onOpen = { onOpenPhoto(it.id) },
            morphScope = "Gallery",
            contentPadding = padding,
            empty = {
                val filtered = state.filter.favoritesOnly || state.filter.tags.isNotEmpty()
                if (!filtered) {
                    EmptyState(
                        title = "No photos",
                        message = "Deleted photos come back the next time the app starts.",
                        icon = { MoodboardIcon(R.drawable.ic_gallery, null) },
                    )
                } else {
                    EmptyState(
                        title = "No photos match",
                        message = "Try other tags, or turn off Favorites only.",
                        icon = { MoodboardIcon(R.drawable.ic_filter, null) },
                        action = {
                            TextButton(
                                onClick = {
                                    // Sorting stays as it was: only what hides photos is cleared.
                                    val cleared =
                                        state.filter.copy(favoritesOnly = false, tags = emptySet())
                                    viewModel.onAction(GalleryAction.SetFilter(cleared))
                                }
                            ) {
                                Text("Clear filter")
                            }
                        },
                    )
                }
            },
        ) { photo, dismiss ->
            PhotoMenuItems(
                photo = photo,
                boards = state.boards,
                onShare = { share.launch(context, photo) },
                onToggleFavorite = { viewModel.onAction(GalleryAction.ToggleFavorite(photo.id)) },
                onSetMembership = { boardId, member ->
                    viewModel.onAction(
                        if (member) GalleryAction.AddToBoard(photo.id, boardId)
                        else GalleryAction.RemoveFromBoard(photo.id, boardId)
                    )
                },
                onNewBoard = { newBoardForId = photo.id },
                onDelete = { deletingId = photo.id },
                dismiss = dismiss,
            )
        }
    }

    if (filterOpen) {
        // Short enough to show whole: a half-open sheet cut the sort row off at the screen's edge.
        ModalBottomSheet(
            onDismissRequest = { filterOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            FilterSheetContent(
                filter = state.filter,
                allTags = state.allTags,
                onFilterChange = { viewModel.onAction(GalleryAction.SetFilter(it)) },
            )
        }
    }
    deleting?.let { photo ->
        DeletePhotoDialog(
            title = photo.title,
            onConfirm = {
                viewModel.onAction(GalleryAction.Delete(photo.id))
                deletingId = null
            },
            onDismiss = { deletingId = null },
        )
    }
    newBoardFor?.let { photo ->
        BoardNameDialog(
            title = "New board",
            confirmLabel = "Create",
            onConfirm = {
                viewModel.onAction(GalleryAction.CreateBoardWith(photo.id, it))
                newBoardForId = null
            },
            onDismiss = { newBoardForId = null },
        )
    }
}
