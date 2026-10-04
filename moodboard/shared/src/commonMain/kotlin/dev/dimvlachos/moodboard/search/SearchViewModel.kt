package dev.dimvlachos.moodboard.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dimvlachos.moodboard.MoodboardGraph
import dev.dimvlachos.moodboard.domain.MoodboardRepository
import dev.dimvlachos.moodboard.domain.Photo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * [recent] holds the latest searches, newest first, for an empty field to offer again; [tagCounts]
 * how many photos carry each tag, for browsing by tag.
 */
data class SearchState(
    val query: String,
    val suggestions: List<String>,
    val results: List<Photo>,
    val recent: List<String> = emptyList(),
    val tagCounts: Map<String, Int> = emptyMap(),
)

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction

    data class TagTapped(val tag: String) : SearchAction

    /** The keyboard's search key: the query is remembered as a recent search. */
    data object Submitted : SearchAction

    /** A result was opened, which also remembers the query that found it. */
    data object ResultOpened : SearchAction

    data class RemoveRecent(val query: String) : SearchAction
}

class SearchViewModel(private val repository: MoodboardRepository = MoodboardGraph.repository) :
    ViewModel() {
    private val query = MutableStateFlow("")
    private val recent = MutableStateFlow(emptyList<String>())

    val state: StateFlow<SearchState> =
        combine(repository.photos, query, recent, ::searchState)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                searchState(repository.photos.value, query.value, recent.value),
            )

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> query.value = action.query
            is SearchAction.TagTapped -> {
                query.value = action.tag
                remember(action.tag)
            }
            SearchAction.Submitted,
            SearchAction.ResultOpened -> remember(query.value)
            is SearchAction.RemoveRecent -> recent.value -= action.query
        }
    }

    // Newest first, once each (ignoring case, keeping the latest spelling), at most MaxRecent.
    private fun remember(search: String) {
        val trimmed = search.trim()
        if (trimmed.isEmpty()) return
        recent.value =
            (listOf(trimmed) + recent.value.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(MaxRecent)
    }
}

private const val MaxRecent = 5

/** A blank query shows everything; otherwise titles and tags match case-insensitively. */
private fun searchState(photos: List<Photo>, query: String, recent: List<String>): SearchState {
    val needle = query.trim()
    val tags = photos.flatMap { it.tags }.distinct().sorted()
    return SearchState(
        query = query,
        suggestions = tags.filter { it.contains(needle, ignoreCase = true) },
        results =
            photos.filter { photo ->
                photo.title.contains(needle, ignoreCase = true) ||
                    photo.tags.any { it.contains(needle, ignoreCase = true) }
            },
        recent = recent,
        tagCounts = tags.associateWith { tag -> photos.count { tag in it.tags } },
    )
}
