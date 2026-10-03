package dev.dimvlachos.moodboard.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dimvlachos.moodboard.MoodboardGraph
import dev.dimvlachos.moodboard.domain.Board
import dev.dimvlachos.moodboard.domain.MoodboardRepository
import dev.dimvlachos.moodboard.domain.Photo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class BoardEditorState(val name: String, val photos: List<Photo>, val selected: Set<String>)

/** Edits apply to the repository as they happen; there is no save step. */
sealed interface BoardEditorAction {
    data class Rename(val name: String) : BoardEditorAction

    data class Toggle(val photoId: String) : BoardEditorAction
}

class BoardEditorViewModel(
    private val boardId: String,
    private val repository: MoodboardRepository = MoodboardGraph.repository,
) : ViewModel() {
    val state: StateFlow<BoardEditorState> =
        combine(repository.photos, repository.boards, ::editorState)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                editorState(repository.photos.value, repository.boards.value),
            )

    fun onAction(action: BoardEditorAction) {
        when (action) {
            // A board always keeps a name; an emptied field leaves the last one in place.
            is BoardEditorAction.Rename ->
                if (action.name.isNotBlank()) repository.renameBoard(boardId, action.name.trim())
            is BoardEditorAction.Toggle -> {
                val members = repository.boards.value.firstOrNull { it.id == boardId }?.photoIds
                repository.setMembership(
                    boardId,
                    action.photoId,
                    member = members != null && action.photoId !in members,
                )
            }
        }
    }

    private fun editorState(photos: List<Photo>, boards: List<Board>): BoardEditorState {
        val board = boards.firstOrNull { it.id == boardId }
        return BoardEditorState(
            name = board?.name.orEmpty(),
            photos = photos,
            selected = board?.photoIds.orEmpty().toSet(),
        )
    }
}
