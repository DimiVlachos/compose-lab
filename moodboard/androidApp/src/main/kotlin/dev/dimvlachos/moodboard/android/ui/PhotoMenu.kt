package dev.dimvlachos.moodboard.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.domain.Board
import dev.dimvlachos.moodboard.domain.Photo

/** Share, favorite, add to board ▸ and delete: the same actions as the iOS context menu. */
@Composable
fun ColumnScope.PhotoMenuItems(
    photo: Photo,
    boards: List<Board>,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetMembership: (boardId: String, member: Boolean) -> Unit,
    onNewBoard: () -> Unit,
    onDelete: () -> Unit,
    dismiss: () -> Unit,
    showShareAndFavorite: Boolean = true,
) {
    var boardsOpen by remember { mutableStateOf(false) }
    if (showShareAndFavorite) {
        ShareAndFavoriteItems(photo, onShare, onToggleFavorite, dismiss)
    }
    Box {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_add_to_board)) },
            leadingIcon = { MoodboardIcon(R.drawable.ic_boards, null) },
            trailingIcon = { MoodboardIcon(R.drawable.ic_chevron_right, null) },
            onClick = { boardsOpen = true },
        )
        DropdownMenu(expanded = boardsOpen, onDismissRequest = { boardsOpen = false }) {
            boards.forEach { board ->
                val member = photo.id in board.photoIds
                DropdownMenuItem(
                    text = { Text(board.name) },
                    trailingIcon = { if (member) MoodboardIcon(R.drawable.ic_check, null) },
                    // Announced as a checkbox that is on or off, not only by a check icon.
                    modifier =
                        Modifier.semantics {
                            role = Role.Checkbox
                            toggleableState = ToggleableState(member)
                        },
                    onClick = {
                        boardsOpen = false
                        dismiss()
                        onSetMembership(board.id, !member)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_new_board_ellipsis)) },
                leadingIcon = { MoodboardIcon(R.drawable.ic_add, null) },
                onClick = {
                    boardsOpen = false
                    dismiss()
                    onNewBoard()
                },
            )
        }
    }
    HorizontalDivider()
    DropdownMenuItem(
        text = { Text(stringResource(R.string.action_delete)) },
        colors =
            MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.error,
                leadingIconColor = MaterialTheme.colorScheme.error,
            ),
        leadingIcon = { MoodboardIcon(R.drawable.ic_delete, null) },
        onClick = {
            dismiss()
            onDelete()
        },
    )
}

@Composable
private fun ShareAndFavoriteItems(
    photo: Photo,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    dismiss: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.action_share)) },
        leadingIcon = { MoodboardIcon(R.drawable.ic_share, null) },
        onClick = {
            dismiss()
            onShare()
        },
    )
    DropdownMenuItem(
        text = {
            Text(
                stringResource(
                    if (photo.isFavorite) R.string.action_unfavorite else R.string.action_favorite
                )
            )
        },
        leadingIcon = {
            MoodboardIcon(
                if (photo.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                null,
            )
        },
        onClick = {
            dismiss()
            onToggleFavorite()
        },
    )
}
