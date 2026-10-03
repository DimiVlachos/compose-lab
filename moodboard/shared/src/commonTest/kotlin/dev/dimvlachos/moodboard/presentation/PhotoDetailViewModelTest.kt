package dev.dimvlachos.moodboard.presentation

import dev.dimvlachos.moodboard.data.InMemoryMoodboardRepository
import dev.dimvlachos.moodboard.detail.PhotoDetailAction
import dev.dimvlachos.moodboard.detail.PhotoDetailViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotoDetailViewModelTest : MainDispatcherTest() {
    private val repository = InMemoryMoodboardRepository()
    private val viewModel by lazy { PhotoDetailViewModel("santorini", repository) }
    private val state
        get() = viewModel.state.value

    @Test
    fun `shows the photo and all boards`() {
        assertEquals("Santorini", state.photo?.title)
        assertEquals(3, state.boards.size)
        assertFalse(state.isDeleted)
    }

    @Test
    fun `toggle favorite and membership`() {
        viewModel.onAction(PhotoDetailAction.ToggleFavorite)
        assertEquals(false, state.photo?.isFavorite)
        viewModel.onAction(PhotoDetailAction.SetMembership("blue", member = false))
        assertFalse("santorini" in state.boards.single { it.id == "blue" }.photoIds)
    }

    @Test
    fun `create board holds this photo`() {
        viewModel.onAction(PhotoDetailAction.CreateBoard("Sunsets"))
        assertEquals(listOf("santorini"), state.boards.single { it.name == "Sunsets" }.photoIds)
    }

    @Test
    fun `delete marks the screen deleted`() {
        viewModel.onAction(PhotoDetailAction.Delete)
        assertTrue(state.isDeleted)
        assertEquals(null, state.photo)
    }

    @Test
    fun `deleting elsewhere also marks it deleted`() {
        repository.deletePhoto("santorini")
        assertTrue(state.isDeleted)
    }
}
