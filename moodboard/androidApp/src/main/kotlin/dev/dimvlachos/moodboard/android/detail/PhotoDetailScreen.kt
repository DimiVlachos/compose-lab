package dev.dimvlachos.moodboard.android.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.share.rememberShareLauncher
import dev.dimvlachos.moodboard.android.ui.BoardNameDialog
import dev.dimvlachos.moodboard.android.ui.DeletePhotoDialog
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.android.ui.PhotoMenuItems
import dev.dimvlachos.moodboard.android.ui.photoMorphEnd
import dev.dimvlachos.moodboard.detail.PhotoDetailAction
import dev.dimvlachos.moodboard.detail.PhotoDetailContent
import dev.dimvlachos.moodboard.detail.PhotoDetailViewModel
import dev.dimvlachos.moodboard.domain.Photo
import dev.dimvlachos.moodboard.morph.MorphEnd
import dev.dimvlachos.moodboard.morph.morphChrome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(photoId: String, tab: String, onBack: () -> Unit) {
    val viewModel = viewModel { PhotoDetailViewModel(photoId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val share = rememberShareLauncher()
    val haptics = LocalHapticFeedback.current
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var newBoard by rememberSaveable { mutableStateOf(false) }

    // A delete removes this screen from the back stack (TopLevelBackStack.prune); until it has
    // left, the bars keep the last photo, as the content does.
    val last = remember { arrayOfNulls<Photo>(1) }
    state.photo?.let { last[0] = it }
    val photo = last[0] ?: return

    // The photo carries the morph; the bars and text are staged around it, as in the lab.
    val chrome = Modifier.morphChrome(LocalNavAnimatedContentScope.current)
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = chrome,
                title = { Text(photo.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { MoodboardIcon(R.drawable.ic_arrow_back, "Back") }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            viewModel.onAction(PhotoDetailAction.ToggleFavorite)
                        }
                    ) {
                        MoodboardIcon(
                            if (photo.isFavorite) R.drawable.ic_favorite
                            else R.drawable.ic_favorite_border,
                            if (photo.isFavorite) "Unfavorite" else "Favorite",
                        )
                    }
                    IconButton(onClick = { share.launch(context, photo) }) {
                        MoodboardIcon(R.drawable.ic_share, "Share")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            MoodboardIcon(R.drawable.ic_more_vert, "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            PhotoMenuItems(
                                photo = photo,
                                boards = state.boards,
                                onShare = { share.launch(context, photo) },
                                onToggleFavorite = {
                                    viewModel.onAction(PhotoDetailAction.ToggleFavorite)
                                },
                                onSetMembership = { boardId, member ->
                                    viewModel.onAction(
                                        PhotoDetailAction.SetMembership(boardId, member)
                                    )
                                },
                                onNewBoard = { newBoard = true },
                                onDelete = { deleting = true },
                                dismiss = { menuOpen = false },
                            )
                        }
                    }
                },
            )
        }
    ) { padding ->
        PhotoDetailContent(
            state = state,
            imageModifier = Modifier.photoMorphEnd(photo.id, tab, MorphEnd.Detail, 1f),
            chromeModifier = chrome,
            contentPadding = padding,
        )
    }

    if (deleting) {
        DeletePhotoDialog(
            title = photo.title,
            onConfirm = {
                deleting = false
                viewModel.onAction(PhotoDetailAction.Delete)
            },
            onDismiss = { deleting = false },
        )
    }
    if (newBoard) {
        BoardNameDialog(
            title = "New board",
            confirmLabel = "Create",
            onConfirm = {
                viewModel.onAction(PhotoDetailAction.CreateBoard(it))
                newBoard = false
            },
            onDismiss = { newBoard = false },
        )
    }
}
