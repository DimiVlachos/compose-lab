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

data class BoardDetailState(val board: Board?, val photos: List<Photo>, val isDeleted: Boolean)

sealed interface BoardDetailAction {
    data class Remove(val photoId: String) : BoardDetailAction
}

class BoardDetailViewModel(
    private val boardId: String,
    private val repository: MoodboardRepository = MoodboardGraph.repository,
) : ViewModel() {
    val state: StateFlow<BoardDetailState> =
        combine(repository.photos, repository.boards, ::detailState)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                detailState(repository.photos.value, repository.boards.value),
            )

    fun onAction(action: BoardDetailAction) {
        when (action) {
            is BoardDetailAction.Remove ->
                repository.setMembership(boardId, action.photoId, member = false)
        }
    }

    private fun detailState(photos: List<Photo>, boards: List<Board>): BoardDetailState {
        val board = boards.firstOrNull { it.id == boardId }
        val byId = photos.associateBy { it.id }
        return BoardDetailState(
            board = board,
            photos = board?.photoIds.orEmpty().mapNotNull { byId[it] },
            isDeleted = board == null,
        )
    }
}
