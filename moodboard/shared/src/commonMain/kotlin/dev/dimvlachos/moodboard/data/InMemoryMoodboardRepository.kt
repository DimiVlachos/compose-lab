package dev.dimvlachos.moodboard.data

import dev.dimvlachos.moodboard.domain.Board
import dev.dimvlachos.moodboard.domain.MoodboardRepository
import dev.dimvlachos.moodboard.domain.Photo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Lives for the process: everything resets on relaunch, by design. */
class InMemoryMoodboardRepository : MoodboardRepository {
    private val _photos = MutableStateFlow(SeedData.photos)
    private val _boards = MutableStateFlow(SeedData.boards)
    private var nextBoardNumber = 1

    override val photos: StateFlow<List<Photo>> = _photos.asStateFlow()
    override val boards: StateFlow<List<Board>> = _boards.asStateFlow()

    override fun toggleFavorite(id: String) {
        _photos.update { photos ->
            photos.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }

    override fun deletePhoto(id: String) = deletePhotos(setOf(id))

    override fun createBoard(name: String, initialPhotoId: String?): String {
        val id = "board-${nextBoardNumber++}"
        val photoIds = listOfNotNull(initialPhotoId?.takeIf { photoExists(it) })
        _boards.update { it + Board(id, name, photoIds) }
        return id
    }

    override fun renameBoard(id: String, name: String) {
        _boards.update { boards -> boards.map { if (it.id == id) it.copy(name = name) else it } }
    }

    override fun deleteBoard(id: String, deletePhotos: Boolean) {
        val board = _boards.value.firstOrNull { it.id == id } ?: return
        _boards.update { boards -> boards.filterNot { it.id == id } }
        if (deletePhotos) deletePhotos(board.photoIds.toSet())
    }

    override fun setMembership(boardId: String, photoId: String, member: Boolean) {
        if (!photoExists(photoId)) return
        _boards.update { boards ->
            boards.map { board ->
                when {
                    board.id != boardId -> board
                    member && photoId !in board.photoIds ->
                        board.copy(photoIds = board.photoIds + photoId)
                    !member -> board.copy(photoIds = board.photoIds - photoId)
                    else -> board
                }
            }
        }
    }

    private fun photoExists(id: String) = _photos.value.any { it.id == id }

    private fun deletePhotos(ids: Set<String>) {
        _photos.update { photos -> photos.filterNot { it.id in ids } }
        _boards.update { boards -> boards.map { it.copy(photoIds = it.photoIds - ids) } }
    }
}
