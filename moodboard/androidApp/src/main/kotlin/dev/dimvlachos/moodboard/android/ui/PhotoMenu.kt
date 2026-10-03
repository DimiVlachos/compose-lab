package dev.dimvlachos.moodboard.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
) {
    var boardsOpen by remember { mutableStateOf(false) }
    DropdownMenuItem(
        text = { Text("Share") },
        leadingIcon = { MoodboardIcon(R.drawable.ic_share, null) },
        onClick = {
            dismiss()
            onShare()
        },
    )
    DropdownMenuItem(
        text = { Text(if (photo.isFavorite) "Unfavorite" else "Favorite") },
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
    Box {
        DropdownMenuItem(
            text = { Text("Add to board") },
            leadingIcon = { MoodboardIcon(R.drawable.ic_boards, null) },
            trailingIcon = { MoodboardIcon(R.drawable.ic_chevron_right, null) },
            onClick = { boardsOpen = true },
        )
        DropdownMenu(expanded = boardsOpen, onDismissRequest = { boardsOpen = false }) {
            boards.forEach { board ->
                val member = photo.id in board.photoIds
                DropdownMenuItem(
                    text = { Text(board.name) },
                    trailingIcon = { if (member) MoodboardIcon(R.drawable.ic_check, "In board") },
                    onClick = {
                        boardsOpen = false
                        dismiss()
                        onSetMembership(board.id, !member)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("New board…") },
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
        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
        leadingIcon = { MoodboardIcon(R.drawable.ic_delete, null) },
        onClick = {
            dismiss()
            onDelete()
        },
    )
}
