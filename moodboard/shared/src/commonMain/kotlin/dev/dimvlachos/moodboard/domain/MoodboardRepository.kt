package dev.dimvlachos.moodboard.domain

import kotlinx.coroutines.flow.StateFlow

/** Every action on an id that doesn't exist is a no-op. */
interface MoodboardRepository {
    val photos: StateFlow<List<Photo>>
    val boards: StateFlow<List<Board>>

    fun toggleFavorite(id: String)

    /** Also removes the photo from every board. */
    fun deletePhoto(id: String)

    /** Returns the new board's id. */
    fun createBoard(name: String, initialPhotoId: String? = null): String

    fun renameBoard(id: String, name: String)

    fun deleteBoard(id: String, deletePhotos: Boolean)

    fun setMembership(boardId: String, photoId: String, member: Boolean)
}
