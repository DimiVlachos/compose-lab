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

    @Test
    fun `every tag carries how many photos have it`() {
        assertEquals(20, state.tagCounts["greece"])
        assertEquals(12, state.tagCounts["island"])
        assertEquals(2, state.tagCounts["ruins"])
    }

    @Test
    fun `starts with no recent searches`() {
        assertEquals(emptyList(), state.recent)
    }

    @Test
    fun `a submitted search is remembered newest first and once`() {
        submit("Meteora")
        submit("sea")
        submit("meteora ")
        assertEquals(listOf("meteora", "sea"), state.recent)
    }

    @Test
    fun `a blank search is not remembered`() {
        submit("   ")
        assertEquals(emptyList(), state.recent)
    }

    @Test
    fun `only the five latest searches are kept`() {
        listOf("a", "b", "c", "d", "e", "f").forEach(::submit)
        assertEquals(listOf("f", "e", "d", "c", "b"), state.recent)
    }

    @Test
    fun `tapping a tag and opening a result remember the search`() {
        viewModel.onAction(SearchAction.TagTapped("ruins"))
        viewModel.onAction(SearchAction.QueryChanged("Delphi"))
        viewModel.onAction(SearchAction.ResultOpened)
        assertEquals(listOf("Delphi", "ruins"), state.recent)
    }

    @Test
    fun `a recent search can be removed`() {
        submit("sea")
        submit("town")
        viewModel.onAction(SearchAction.RemoveRecent("sea"))
        assertEquals(listOf("town"), state.recent)
    }

    private fun submit(query: String) {
        viewModel.onAction(SearchAction.QueryChanged(query))
        viewModel.onAction(SearchAction.Submitted)
    }
}
