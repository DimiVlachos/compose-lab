package dev.dimvlachos.moodboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryMoodboardRepositoryTest {

    private val repository = InMemoryMoodboardRepository()

    private fun photo(id: String) = repository.photos.value.single { it.id == id }

    private fun board(name: String) = repository.boards.value.single { it.name == name }

    @Test
    fun `seed has twelve islands and eight mainland places`() {
        val photos = repository.photos.value
        assertEquals(20, photos.size)
        assertEquals(12, photos.count { "island" in it.tags })
        assertEquals(8, photos.count { "mainland" in it.tags })
        assertEquals("files/photos/photo_santorini.jpg", photo("santorini").path)
        assertEquals("Kalogeriko Bridge", photo("kalogeriko").title)
        assertEquals("files/photos/photo_kalogeriko.jpg", photo("kalogeriko").path)
    }

    @Test
    fun `seed favorites are Santorini and Meteora`() {
        assertEquals(
            setOf("santorini", "meteora"),
            repository.photos.value.filter { it.isFavorite }.map { it.id }.toSet(),
        )
    }

    @Test
    fun `seed boards are Islands then Mainland then Blue`() {
        assertEquals(
            listOf("Islands", "Mainland", "Blue"),
            repository.boards.value.map { it.name },
        )
        assertEquals(12, board("Islands").photoIds.size)
        assertEquals(8, board("Mainland").photoIds.size)
        assertEquals(listOf("santorini", "mykonos", "milos"), board("Blue").photoIds)
    }

    @Test
    fun `toggleFavorite flips the flag`() {
        repository.toggleFavorite("naxos")
        assertTrue(photo("naxos").isFavorite)
        repository.toggleFavorite("naxos")
        assertFalse(photo("naxos").isFavorite)
    }

    @Test
    fun `deletePhoto removes it from photos and every board`() {
        repository.deletePhoto("santorini")
        assertTrue(repository.photos.value.none { it.id == "santorini" })
        assertTrue(repository.boards.value.none { "santorini" in it.photoIds })
    }

    @Test
    fun `createBoard appends a board holding the initial photo`() {
        val id = repository.createBoard("Sunsets", initialPhotoId = "hydra")
        val created = repository.boards.value.last()
        assertEquals(id, created.id)
        assertEquals("Sunsets", created.name)
        assertEquals(listOf("hydra"), created.photoIds)
    }

    @Test
    fun `createBoard gives each board a distinct id`() {
        val first = repository.createBoard("A")
        val second = repository.createBoard("B")
        assertTrue(first != second)
    }

    @Test
    fun `renameBoard changes only the name`() {
        val blue = board("Blue")
        repository.renameBoard(blue.id, "Aegean")
        assertEquals(
            blue.copy(name = "Aegean"),
            repository.boards.value.single { it.id == blue.id },
        )
    }

    @Test
    fun `deleteBoard without photos keeps the photos`() {
        repository.deleteBoard(board("Blue").id, deletePhotos = false)
        assertTrue(repository.boards.value.none { it.name == "Blue" })
        assertEquals(20, repository.photos.value.size)
    }

    @Test
    fun `deleteBoard with photos removes them everywhere`() {
        repository.deleteBoard(board("Blue").id, deletePhotos = true)
        assertEquals(17, repository.photos.value.size)
        assertEquals(9, board("Islands").photoIds.size)
    }

    @Test
    fun `setMembership adds once and removes`() {
        val blue = board("Blue").id
        repository.setMembership(blue, "naxos", member = true)
        repository.setMembership(blue, "naxos", member = true)
        assertEquals(listOf("santorini", "mykonos", "milos", "naxos"), board("Blue").photoIds)
        repository.setMembership(blue, "santorini", member = false)
        assertEquals(listOf("mykonos", "milos", "naxos"), board("Blue").photoIds)
    }

    @Test
    fun `actions on missing ids change nothing`() {
        val photos = repository.photos.value
        val boards = repository.boards.value
        repository.toggleFavorite("nope")
        repository.deletePhoto("nope")
        repository.renameBoard("nope", "x")
        repository.deleteBoard("nope", deletePhotos = true)
        repository.setMembership("nope", "naxos", member = true)
        repository.setMembership(boards.first().id, "nope", member = true)
        assertEquals(photos, repository.photos.value)
        assertEquals(boards, repository.boards.value)
    }
}
