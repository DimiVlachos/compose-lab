package dev.dimvlachos.moodboard.android.boards

import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.android.ui.PhotoGrid
import dev.dimvlachos.moodboard.boards.BoardDetailAction
import dev.dimvlachos.moodboard.boards.BoardDetailViewModel
import dev.dimvlachos.moodboard.ui.components.EmptyState

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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.board?.name.orEmpty(),
                        Modifier.semantics { heading() },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) { MoodboardIcon(R.drawable.ic_arrow_back, "Back") }
                },
                actions = {
                    IconButton(onClick = onEdit) { MoodboardIcon(R.drawable.ic_edit, "Edit") }
                },
            )
        },
    ) { padding ->
        PhotoGrid(
            photos = state.photos,
            morphScope = "Boards",
            onOpen = { onOpenPhoto(it.id) },
            contentPadding = padding,
            empty = {
                EmptyState(
                    title = "No photos yet",
                    message = "Pick the photos this board should hold.",
                    icon = { MoodboardIcon(R.drawable.ic_boards, null) },
                    action = { Button(onClick = onEdit) { Text("Add photos") } },
                )
            },
        ) { photo, dismiss ->
            DropdownMenuItem(
                text = { Text("Remove from board") },
                leadingIcon = { MoodboardIcon(R.drawable.ic_remove, null) },
                onClick = {
                    dismiss()
                    viewModel.onAction(BoardDetailAction.Remove(photo.id))
                },
            )
        }
    }
}
