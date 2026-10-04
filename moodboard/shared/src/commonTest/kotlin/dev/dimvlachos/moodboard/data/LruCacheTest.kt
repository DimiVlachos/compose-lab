package dev.dimvlachos.moodboard.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LruCacheTest {

    @Test
    fun `evicts the least recently used entry past capacity`() {
        val cache = LruCache<String, Int>(capacity = 2)
        cache["a"] = 1
        cache["b"] = 2
        cache["a"]
        cache["c"] = 3
        assertNull(cache["b"])
        assertEquals(1, cache["a"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun `getOrPut computes once`() {
        val cache = LruCache<String, Int>(capacity = 2)
        var calls = 0
        repeat(3) { cache.getOrPut("a") { ++calls } }
        assertEquals(1, calls)
    }
}
