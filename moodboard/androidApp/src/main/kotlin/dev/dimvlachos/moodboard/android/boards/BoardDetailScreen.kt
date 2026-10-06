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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.nav.Boards
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
                    IconButton(onClick = onBack) {
                        MoodboardIcon(R.drawable.ic_arrow_back, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        MoodboardIcon(R.drawable.ic_edit, stringResource(R.string.cd_edit))
                    }
                },
            )
        },
    ) { padding ->
        PhotoGrid(
            photos = state.photos,
            morphScope = Boards.toString(),
            // No bottom bar here, only the transparent gesture bar.
            opaqueBottomBar = false,
            onOpen = { onOpenPhoto(it.id) },
            contentPadding = padding,
            empty = {
                EmptyState(
                    title = stringResource(R.string.empty_board_no_photos_title),
                    message = stringResource(R.string.empty_board_no_photos_message),
                    icon = { MoodboardIcon(R.drawable.ic_boards, null) },
                    action = {
                        Button(onClick = onEdit) {
                            Text(stringResource(R.string.action_add_photos))
                        }
                    },
                )
            },
        ) { photo, dismiss ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_remove_from_board)) },
                leadingIcon = { MoodboardIcon(R.drawable.ic_remove, null) },
                onClick = {
                    dismiss()
                    viewModel.onAction(BoardDetailAction.Remove(photo.id))
                },
            )
        }
    }
}
