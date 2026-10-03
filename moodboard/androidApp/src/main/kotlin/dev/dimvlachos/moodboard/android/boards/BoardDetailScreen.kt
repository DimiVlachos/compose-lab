package dev.dimvlachos.moodboard.android.boards

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.android.ui.PhotoGrid
import dev.dimvlachos.moodboard.boards.BoardDetailAction
import dev.dimvlachos.moodboard.boards.BoardDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(
    boardId: String,
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onEdit: () -> Unit,
) {
    val viewModel = viewModel { BoardDetailViewModel(boardId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.board?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { MoodboardIcon(R.drawable.ic_arrow_back, "Back") }
                },
                actions = {
                    IconButton(onClick = onEdit) { MoodboardIcon(R.drawable.ic_edit, "Edit") }
                },
            )
        }
    ) { padding ->
        PhotoGrid(
            photos = state.photos,
            onOpen = { onOpenPhoto(it.id) },
            contentPadding = padding,
        ) { photo, dismiss ->
            DropdownMenuItem(
                text = { Text("Remove from board") },
                onClick = {
                    dismiss()
                    viewModel.onAction(BoardDetailAction.Remove(photo.id))
                },
            )
        }
    }
}
