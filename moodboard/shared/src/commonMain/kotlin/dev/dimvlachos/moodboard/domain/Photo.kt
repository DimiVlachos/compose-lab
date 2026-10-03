package dev.dimvlachos.moodboard.domain

data class Photo(
    val id: String,
    val title: String,
    val path: String,
    val tags: Set<String>,
    val isFavorite: Boolean,
)

data class Board(val id: String, val name: String, val photoIds: List<String>)

data class PhotoFilter(
    val favoritesOnly: Boolean = false,
    val tags: Set<String> = emptySet(),
    val sort: SortOrder = SortOrder.Default,
)

enum class SortOrder {
    Default,
    Title,
}
