package dev.dimvlachos.moodboard.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dimvlachos.moodboard.MoodboardGraph
import dev.dimvlachos.moodboard.domain.Board
import dev.dimvlachos.moodboard.domain.MoodboardRepository
import dev.dimvlachos.moodboard.domain.Photo
import dev.dimvlachos.moodboard.domain.PhotoFilter
import dev.dimvlachos.moodboard.domain.SortOrder
import dev.dimvlachos.moodboard.domain.boardName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GalleryState(
    val photos: List<Photo>,
    val boards: List<Board>,
    val filter: PhotoFilter,
    val allTags: List<String>,
)

sealed interface GalleryAction {
    data class ToggleFavorite(val id: String) : GalleryAction

    data class AddToBoard(val photoId: String, val boardId: String) : GalleryAction

    data class RemoveFromBoard(val photoId: String, val boardId: String) : GalleryAction

    data class CreateBoardWith(val photoId: String, val name: String) : GalleryAction

    data class Delete(val id: String) : GalleryAction

    data class SetFilter(val filter: PhotoFilter) : GalleryAction
}

class GalleryViewModel(private val repository: MoodboardRepository = MoodboardGraph.repository) :
    ViewModel() {
    private val filter = MutableStateFlow(PhotoFilter())

    val state: StateFlow<GalleryState> =
        combine(repository.photos, repository.boards, filter, ::galleryState)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                galleryState(repository.photos.value, repository.boards.value, filter.value),
            )

    fun onAction(action: GalleryAction) {
        when (action) {
            is GalleryAction.ToggleFavorite -> repository.toggleFavorite(action.id)
            is GalleryAction.AddToBoard ->
                repository.setMembership(action.boardId, action.photoId, member = true)
            is GalleryAction.RemoveFromBoard ->
                repository.setMembership(action.boardId, action.photoId, member = false)
            is GalleryAction.CreateBoardWith ->
                boardName(action.name)?.let { repository.createBoard(it, action.photoId) }
            is GalleryAction.Delete -> repository.deletePhoto(action.id)
            is GalleryAction.SetFilter -> filter.value = action.filter
        }
    }
}

/**
 * A selected tag whose photos are all gone drops out of the filter: its chip disappears from the
 * sheet, so it could never be cleared, and the gallery would stay empty.
 */
private fun galleryState(
    photos: List<Photo>,
    boards: List<Board>,
    filter: PhotoFilter,
): GalleryState {
    val allTags = photos.flatMap { it.tags }.distinct().sorted()
    val effective = filter.copy(tags = filter.tags intersect allTags.toSet())
    return GalleryState(
        photos = photos.filtered(effective),
        boards = boards,
        filter = effective,
        allTags = allTags,
    )
}

/** Tags match any selected tag; no tags selected means no tag filter. */
private fun List<Photo>.filtered(filter: PhotoFilter): List<Photo> {
    val kept = filter { photo ->
        (!filter.favoritesOnly || photo.isFavorite) &&
            (filter.tags.isEmpty() || photo.tags.any { it in filter.tags })
    }
    return when (filter.sort) {
        SortOrder.Default -> kept
        SortOrder.Title -> kept.sortedBy { it.title }
    }
}
