package dev.dimvlachos.moodboard.android.boards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.ui.BoardNameDialog
import dev.dimvlachos.moodboard.android.ui.DeleteBoardSheet
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.boards.BoardSummary
import dev.dimvlachos.moodboard.boards.BoardsAction
import dev.dimvlachos.moodboard.boards.BoardsViewModel
import dev.dimvlachos.moodboard.ui.components.EmptyState
import dev.dimvlachos.moodboard.ui.components.PhotoImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoardsScreen(onOpenBoard: (String) -> Unit, bottomBar: @Composable () -> Unit) {
    val viewModel = viewModel { BoardsViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Ids, saved: an open dialog or sheet survives rotation.
    var creating by rememberSaveable { mutableStateOf(false) }
    var renamingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val renaming = state.boards.firstOrNull { it.id == renamingId }
    val deleting = state.boards.firstOrNull { it.id == deletingId }
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
        if (state.boards.isEmpty()) {
            EmptyState(
                title = "No boards",
                message = "Make a board to group photos you want together.",
                modifier = Modifier.padding(padding),
                icon = { MoodboardIcon(R.drawable.ic_boards, null) },
            )
            return@Scaffold
        }
        // Room under the last row, so the floating button never covers it.
        val listPadding =
            PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + FabClearance,
            )
        LazyColumn(contentPadding = listPadding) {
            items(state.boards, key = { it.id }) { board ->
                BoardRow(
                    board = board,
                    onOpen = { onOpenBoard(board.id) },
                    onRename = { renamingId = board.id },
                    onDelete = { deletingId = board.id },
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
                renamingId = null
            },
            onDismiss = { renamingId = null },
        )
    }
    deleting?.let { board ->
        DeleteBoardSheet(
            boardName = board.name,
            onDelete = { deletePhotos ->
                viewModel.onAction(BoardsAction.Delete(board.id, deletePhotos))
                deletingId = null
            },
            onDismiss = { deletingId = null },
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
    ListItem(
        headlineContent = { Text(board.name) },
        supportingContent = { Text(if (board.count == 1) "1 photo" else "${board.count} photos") },
        leadingContent = {
            val cover =
                Modifier.size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            board.coverPath?.let { PhotoImage(it, null, cover) }
                ?: Box(cover, contentAlignment = Alignment.Center) {
                    MoodboardIcon(
                        R.drawable.ic_boards,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        },
        // The menu drops from the overflow button, whether opened by it or by a long press.
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    MoodboardIcon(R.drawable.ic_more_vert, "More options for ${board.name}")
                }
                BoardMenu(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    onRename = onRename,
                    onDelete = onDelete,
                )
            }
        },
        modifier =
            Modifier.combinedClickable(
                onClick = onOpen,
                onLongClickLabel = "More options",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuOpen = true
                },
            ),
    )
}

@Composable
private fun BoardMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { MoodboardIcon(R.drawable.ic_edit, null) },
            onClick = {
                onDismiss()
                onRename()
            },
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { MoodboardIcon(R.drawable.ic_delete, null) },
            onClick = {
                onDismiss()
                onDelete()
            },
        )
    }
}

private val FabClearance = 88.dp
