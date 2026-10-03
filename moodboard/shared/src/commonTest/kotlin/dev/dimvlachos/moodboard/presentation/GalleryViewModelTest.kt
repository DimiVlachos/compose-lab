package dev.dimvlachos.moodboard.presentation

import dev.dimvlachos.moodboard.data.InMemoryMoodboardRepository
import dev.dimvlachos.moodboard.domain.PhotoFilter
import dev.dimvlachos.moodboard.domain.SortOrder
import dev.dimvlachos.moodboard.gallery.GalleryAction
import dev.dimvlachos.moodboard.gallery.GalleryViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GalleryViewModelTest : MainDispatcherTest() {
    private val repository = InMemoryMoodboardRepository()
    private val viewModel by lazy { GalleryViewModel(repository) }
    private val state
        get() = viewModel.state.value

    @Test
    fun `starts with every photo board and tag`() {
        assertEquals(20, state.photos.size)
        assertEquals(3, state.boards.size)
        assertEquals(listOf("comic", "greece", "island", "little nemo", "sea"), state.allTags)
    }

    @Test
    fun `favorites-only filter keeps favorites`() {
        viewModel.onAction(GalleryAction.SetFilter(PhotoFilter(favoritesOnly = true)))
        assertEquals(listOf("santorini", "book_spread_1"), state.photos.map { it.id })
    }

    @Test
    fun `tag filter keeps photos with any selected tag`() {
        viewModel.onAction(GalleryAction.SetFilter(PhotoFilter(tags = setOf("comic"))))
        assertEquals(8, state.photos.size)
        viewModel.onAction(GalleryAction.SetFilter(PhotoFilter(tags = setOf("comic", "sea"))))
        assertEquals(20, state.photos.size)
    }

    @Test
    fun `title sort orders photos alphabetically`() {
        viewModel.onAction(GalleryAction.SetFilter(PhotoFilter(sort = SortOrder.Title)))
        assertEquals("Corfu", state.photos.first().title)
        assertEquals("Zakynthos", state.photos.last().title)
    }

    @Test
    fun `toggle favorite reaches the repository`() {
        viewModel.onAction(GalleryAction.ToggleFavorite("naxos"))
        assertTrue(state.photos.single { it.id == "naxos" }.isFavorite)
    }

    @Test
    fun `add to board and create board with photo`() {
        viewModel.onAction(GalleryAction.AddToBoard("naxos", "blue"))
        assertTrue("naxos" in state.boards.single { it.id == "blue" }.photoIds)
        viewModel.onAction(GalleryAction.CreateBoardWith("hydra", "Sunsets"))
        assertEquals(listOf("hydra"), state.boards.single { it.name == "Sunsets" }.photoIds)
    }

    @Test
    fun `remove from board takes the photo out`() {
        viewModel.onAction(GalleryAction.RemoveFromBoard("santorini", "blue"))
        assertTrue("santorini" !in state.boards.single { it.id == "blue" }.photoIds)
    }

    @Test
    fun `delete removes the photo`() {
        viewModel.onAction(GalleryAction.Delete("naxos"))
        assertTrue(state.photos.none { it.id == "naxos" })
    }
}
