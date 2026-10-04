package dev.dimvlachos.moodboard.presentation

import dev.dimvlachos.moodboard.data.InMemoryMoodboardRepository
import dev.dimvlachos.moodboard.nav.LiveIdsViewModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveIdsViewModelTest : MainDispatcherTest() {
    private val repository = InMemoryMoodboardRepository()
    private val viewModel by lazy { LiveIdsViewModel(repository) }

    @Test
    fun `tracks which photos and boards exist`() {
        assertTrue("naxos" in viewModel.state.value.photoIds)
        assertTrue("blue" in viewModel.state.value.boardIds)
        repository.deletePhoto("naxos")
        repository.deleteBoard("blue", deletePhotos = false)
        assertFalse("naxos" in viewModel.state.value.photoIds)
        assertFalse("blue" in viewModel.state.value.boardIds)
    }
}
