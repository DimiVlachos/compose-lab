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
            is BoardEditorAction.Rename -> repository.renameBoard(boardId, action.name)
            is BoardEditorAction.Toggle ->
                repository.setMembership(
                    boardId,
                    action.photoId,
                    member = action.photoId !in state.value.selected,
                )
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
