package dev.dimvlachos.moodboard.android.boards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.ui.BoardNameDialog
import dev.dimvlachos.moodboard.android.ui.DeleteBoardSheet
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.boards.BoardSummary
import dev.dimvlachos.moodboard.boards.BoardsAction
import dev.dimvlachos.moodboard.boards.BoardsViewModel
import dev.dimvlachos.moodboard.ui.components.PhotoImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoardsScreen(
    viewModel: BoardsViewModel,
    listState: LazyListState,
    onOpenBoard: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<BoardSummary?>(null) }
    var deleting by remember { mutableStateOf<BoardSummary?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(title = { Text("Boards") }, scrollBehavior = scrollBehavior)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { MoodboardIcon(R.drawable.ic_add, null) },
                text = { Text("New board") },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(state = listState, contentPadding = padding) {
            items(state.boards, key = { it.id }) { board ->
                BoardRow(
                    board = board,
                    onOpen = { onOpenBoard(board.id) },
                    onRename = { renaming = board },
                    onDelete = { deleting = board },
                )
            }
        }
    }

    if (creating) {
        BoardNameDialog(
            title = "New board",
            confirmLabel = "Create",
            onConfirm = {
                viewModel.onAction(BoardsAction.Create(it))
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    renaming?.let { board ->
        BoardNameDialog(
            title = "Rename board",
            confirmLabel = "Rename",
            initialName = board.name,
            onConfirm = {
                viewModel.onAction(BoardsAction.Rename(board.id, it))
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { board ->
        DeleteBoardSheet(
            boardName = board.name,
            onDelete = { deletePhotos ->
                viewModel.onAction(BoardsAction.Delete(board.id, deletePhotos))
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardRow(
    board: BoardSummary,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box {
        ListItem(
            headlineContent = { Text(board.name) },
            supportingContent = { Text("${board.count} photos") },
            leadingContent = {
                val cover = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                board.coverPath?.let { PhotoImage(it, null, cover) } ?: Box(cover)
            },
            modifier =
                Modifier.combinedClickable(
                    onClick = onOpen,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                ),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { MoodboardIcon(R.drawable.ic_edit, null) },
                onClick = {
                    menuOpen = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { MoodboardIcon(R.drawable.ic_delete, null) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}
