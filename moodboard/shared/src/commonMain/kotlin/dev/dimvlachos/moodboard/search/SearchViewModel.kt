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

data class SearchState(val query: String, val suggestions: List<String>, val results: List<Photo>)

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction

    data class TagTapped(val tag: String) : SearchAction
}

class SearchViewModel(private val repository: MoodboardRepository = MoodboardGraph.repository) :
    ViewModel() {
    private val query = MutableStateFlow("")

    val state: StateFlow<SearchState> =
        combine(repository.photos, query, ::searchState)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                searchState(repository.photos.value, query.value),
            )

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> query.value = action.query
            is SearchAction.TagTapped -> query.value = action.tag
        }
    }
}

/** A blank query shows everything; otherwise titles and tags match case-insensitively. */
private fun searchState(photos: List<Photo>, query: String): SearchState {
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
    )
}
