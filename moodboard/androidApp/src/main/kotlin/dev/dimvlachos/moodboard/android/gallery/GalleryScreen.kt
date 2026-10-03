package dev.dimvlachos.moodboard.android.gallery

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.share.sharePhoto
import dev.dimvlachos.moodboard.android.ui.BoardNameDialog
import dev.dimvlachos.moodboard.android.ui.DeletePhotoDialog
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.android.ui.PhotoGrid
import dev.dimvlachos.moodboard.android.ui.PhotoMenuItems
import dev.dimvlachos.moodboard.domain.Photo
import dev.dimvlachos.moodboard.filter.FilterSheetContent
import dev.dimvlachos.moodboard.gallery.GalleryAction
import dev.dimvlachos.moodboard.gallery.GalleryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GalleryScreen(onOpenPhoto: (String) -> Unit, bottomBar: @Composable () -> Unit) {
    val viewModel = viewModel { GalleryViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var filterOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Photo?>(null) }
    var newBoardFor by remember { mutableStateOf<Photo?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Gallery") },
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
            contentPadding = padding,
        ) { photo, dismiss ->
            PhotoMenuItems(
                photo = photo,
                boards = state.boards,
                onShare = { scope.launch { sharePhoto(context, photo) } },
                onToggleFavorite = { viewModel.onAction(GalleryAction.ToggleFavorite(photo.id)) },
                onSetMembership = { boardId, member ->
                    viewModel.onAction(
                        if (member) GalleryAction.AddToBoard(photo.id, boardId)
                        else GalleryAction.RemoveFromBoard(photo.id, boardId)
                    )
                },
                onNewBoard = { newBoardFor = photo },
                onDelete = { deleting = photo },
                dismiss = dismiss,
            )
        }
    }

    if (filterOpen) {
        ModalBottomSheet(onDismissRequest = { filterOpen = false }) {
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
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
    newBoardFor?.let { photo ->
        BoardNameDialog(
            title = "New board",
            confirmLabel = "Create",
            onConfirm = {
                viewModel.onAction(GalleryAction.CreateBoardWith(photo.id, it))
                newBoardFor = null
            },
            onDismiss = { newBoardFor = null },
        )
    }
}
