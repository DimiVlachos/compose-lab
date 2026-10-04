package dev.dimvlachos.moodboard.presentation

import dev.dimvlachos.moodboard.data.InMemoryMoodboardRepository
import dev.dimvlachos.moodboard.search.SearchAction
import dev.dimvlachos.moodboard.search.SearchViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchViewModelTest : MainDispatcherTest() {
    private val viewModel by lazy { SearchViewModel(InMemoryMoodboardRepository()) }
    private val state
        get() = viewModel.state.value

    @Test
    fun `blank query shows every photo and every tag`() {
        assertEquals(20, state.results.size)
        assertEquals(7, state.suggestions.size)
    }

    @Test
    fun `query matches titles case-insensitively`() {
        viewModel.onAction(SearchAction.QueryChanged("SANTO"))
        assertEquals(listOf("santorini"), state.results.map { it.id })
    }

    @Test
    fun `query matches tags and narrows suggestions`() {
        viewModel.onAction(SearchAction.QueryChanged("moun"))
        assertEquals(4, state.results.size)
        assertEquals(listOf("mountains"), state.suggestions)
    }

    @Test
    fun `tapping a tag searches for it`() {
        viewModel.onAction(SearchAction.TagTapped("mainland"))
        assertEquals("mainland", state.query)
        assertEquals(8, state.results.size)
    }
}
