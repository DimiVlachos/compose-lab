package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlin.test.Test
import kotlin.test.assertEquals

class TextureCacheTest {
    @Test
    fun aCacheKeepsTheBooksInUseAndOneMore() {
        val cached = listOf(1, 2, 3, 4)

        val evicted = evictions(cached, keep = setOf(3))

        assertEquals(2, evicted.size, "of 1, 2 and 4, one is kept as a spare")
        assertEquals(emptyList(), evicted.filter { it == 3 })
    }

    @Test
    fun aCacheWithRoomEvictsNothing() {
        assertEquals(emptyList(), evictions(listOf(3, 7), keep = setOf(3)))
    }
}
