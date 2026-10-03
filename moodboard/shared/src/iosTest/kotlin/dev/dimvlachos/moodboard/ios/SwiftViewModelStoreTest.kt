package dev.dimvlachos.moodboard.ios

import dev.dimvlachos.moodboard.MoodboardGraph
import dev.dimvlachos.moodboard.presentation.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class SwiftViewModelStoreTest : MainDispatcherTest() {

    @Test
    fun sameScreenGetsTheSameViewModel() {
        val store = SwiftViewModelStore()
        assertSame(store.gallery(), store.gallery())
        assertSame(store.photoDetail("naxos"), store.photoDetail("naxos"))
    }

    @Test
    fun differentIdsGetDifferentViewModels() {
        val store = SwiftViewModelStore()
        assertNotSame(store.photoDetail("naxos"), store.photoDetail("milos"))
        assertNotSame(store.boardDetail("blue"), store.boardDetail("islands"))
    }

    @Test
    fun clearStopsTheViewModelFollowingTheRepository() {
        val store = SwiftViewModelStore()
        val detail = store.photoDetail("hydra")
        val before = detail.state.value.photo?.isFavorite
        store.clear()
        MoodboardGraph.repository.toggleFavorite("hydra")
        assertEquals(before, detail.state.value.photo?.isFavorite)
        MoodboardGraph.repository.toggleFavorite("hydra")
    }
}

class ImageDataTest {
    @Test
    fun bytesBecomeNSDataOfTheSameLength() {
        val data = byteArrayOf(1, 2, 3, 4).toNSData()
        assertEquals(4uL, data.length)
    }

    @Test
    fun emptyBytesBecomeEmptyNSData() {
        assertEquals(0uL, ByteArray(0).toNSData().length)
    }
}
