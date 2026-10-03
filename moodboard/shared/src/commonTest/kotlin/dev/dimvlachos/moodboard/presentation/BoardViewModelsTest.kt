package dev.dimvlachos.moodboard.presentation

import dev.dimvlachos.moodboard.boards.BoardDetailAction
import dev.dimvlachos.moodboard.boards.BoardDetailViewModel
import dev.dimvlachos.moodboard.boards.BoardEditorAction
import dev.dimvlachos.moodboard.boards.BoardEditorViewModel
import dev.dimvlachos.moodboard.boards.BoardsAction
import dev.dimvlachos.moodboard.boards.BoardsViewModel
import dev.dimvlachos.moodboard.data.InMemoryMoodboardRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardViewModelsTest : MainDispatcherTest() {
    private val repository = InMemoryMoodboardRepository()

    @Test
    fun `boards list summarises count and cover`() {
        val viewModel = BoardsViewModel(repository)
        val blue = viewModel.state.value.boards.single { it.id == "blue" }
        assertEquals("Blue", blue.name)
        assertEquals(3, blue.count)
        assertEquals("files/photos/photo_santorini.jpg", blue.coverPath)
    }

    @Test
    fun `empty board has no cover`() {
        val viewModel = BoardsViewModel(repository)
        viewModel.onAction(BoardsAction.Create("Empty"))
        assertEquals(null, viewModel.state.value.boards.last().coverPath)
    }

    @Test
    fun `boards list renames and deletes`() {
        val viewModel = BoardsViewModel(repository)
        viewModel.onAction(BoardsAction.Rename("blue", "Aegean"))
        assertEquals("Aegean", viewModel.state.value.boards.single { it.id == "blue" }.name)
        viewModel.onAction(BoardsAction.Delete("blue", deletePhotos = true))
        assertTrue(viewModel.state.value.boards.none { it.id == "blue" })
        assertEquals(17, repository.photos.value.size)
    }

    @Test
    fun `board detail lists members in board order and removes`() {
        val viewModel = BoardDetailViewModel("blue", repository)
        assertEquals(
            listOf("santorini", "mykonos", "milos"),
            viewModel.state.value.photos.map { it.id },
        )
        viewModel.onAction(BoardDetailAction.Remove("mykonos"))
        assertEquals(listOf("santorini", "milos"), viewModel.state.value.photos.map { it.id })
    }

    @Test
    fun `board detail is deleted when its board goes`() {
        val viewModel = BoardDetailViewModel("blue", repository)
        repository.deleteBoard("blue", deletePhotos = false)
        assertTrue(viewModel.state.value.isDeleted)
    }

    @Test
    fun `board editor renames and toggles membership live`() {
        val viewModel = BoardEditorViewModel("blue", repository)
        assertEquals("Blue", viewModel.state.value.name)
        assertEquals(20, viewModel.state.value.photos.size)
        assertEquals(setOf("santorini", "mykonos", "milos"), viewModel.state.value.selected)
        viewModel.onAction(BoardEditorAction.Rename("Aegean"))
        viewModel.onAction(BoardEditorAction.Toggle("naxos"))
        viewModel.onAction(BoardEditorAction.Toggle("santorini"))
        assertEquals("Aegean", viewModel.state.value.name)
        assertEquals(setOf("mykonos", "milos", "naxos"), viewModel.state.value.selected)
        assertEquals("Aegean", repository.boards.value.single { it.id == "blue" }.name)
    }
}
