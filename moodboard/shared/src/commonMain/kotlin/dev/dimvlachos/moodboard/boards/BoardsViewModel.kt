package dev.dimvlachos.moodboard.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dimvlachos.moodboard.MoodboardGraph
import dev.dimvlachos.moodboard.domain.Board
import dev.dimvlachos.moodboard.domain.MoodboardRepository
import dev.dimvlachos.moodboard.domain.Photo
import dev.dimvlachos.moodboard.domain.boardName
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class BoardSummary(val id: String, val name: String, val count: Int, val coverPath: String?)

data class BoardsState(val boards: List<BoardSummary>)

sealed interface BoardsAction {
    data class Create(val name: String) : BoardsAction

    data class Rename(val id: String, val name: String) : BoardsAction

    data class Delete(val id: String, val deletePhotos: Boolean) : BoardsAction
}

class BoardsViewModel(private val repository: MoodboardRepository = MoodboardGraph.repository) :
    ViewModel() {
    val state: StateFlow<BoardsState> =
        combine(repository.photos, repository.boards, ::boardsState)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                boardsState(repository.photos.value, repository.boards.value),
            )

    fun onAction(action: BoardsAction) {
        when (action) {
            is BoardsAction.Create -> boardName(action.name)?.let { repository.createBoard(it) }
            is BoardsAction.Rename ->
                boardName(action.name)?.let { repository.renameBoard(action.id, it) }
            is BoardsAction.Delete -> repository.deleteBoard(action.id, action.deletePhotos)
        }
    }
}

private fun boardsState(photos: List<Photo>, boards: List<Board>): BoardsState {
    val paths = photos.associate { it.id to it.path }
    return BoardsState(
        boards.map { board ->
            BoardSummary(
                id = board.id,
                name = board.name,
                count = board.photoIds.size,
                coverPath = board.photoIds.firstNotNullOfOrNull { paths[it] },
            )
        }
    )
}
