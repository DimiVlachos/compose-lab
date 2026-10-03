package dev.dimvlachos.moodboard.detail

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

/** Both platforms pop the screen once [isDeleted] turns true. */
data class PhotoDetailState(val photo: Photo?, val boards: List<Board>, val isDeleted: Boolean)

sealed interface PhotoDetailAction {
    data object ToggleFavorite : PhotoDetailAction

    data object Delete : PhotoDetailAction

    data class SetMembership(val boardId: String, val member: Boolean) : PhotoDetailAction
}

class PhotoDetailViewModel(
    private val photoId: String,
    private val repository: MoodboardRepository = MoodboardGraph.repository,
) : ViewModel() {
    val state: StateFlow<PhotoDetailState> =
        combine(repository.photos, repository.boards, ::detailState)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                detailState(repository.photos.value, repository.boards.value),
            )

    fun onAction(action: PhotoDetailAction) {
        when (action) {
            PhotoDetailAction.ToggleFavorite -> repository.toggleFavorite(photoId)
            PhotoDetailAction.Delete -> repository.deletePhoto(photoId)
            is PhotoDetailAction.SetMembership ->
                repository.setMembership(action.boardId, photoId, action.member)
        }
    }

    private fun detailState(photos: List<Photo>, boards: List<Board>): PhotoDetailState {
        val photo = photos.firstOrNull { it.id == photoId }
        return PhotoDetailState(photo = photo, boards = boards, isDeleted = photo == null)
    }
}
