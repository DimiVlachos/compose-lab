package dev.dimvlachos.moodboard.nav

import dev.dimvlachos.moodboard.android.nav.BoardDetail
import dev.dimvlachos.moodboard.android.nav.Boards
import dev.dimvlachos.moodboard.android.nav.Gallery
import dev.dimvlachos.moodboard.android.nav.PhotoDetail
import dev.dimvlachos.moodboard.android.nav.Search
import dev.dimvlachos.moodboard.android.nav.TopLevelBackStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelBackStackTest {
    private val stack = TopLevelBackStack(Gallery)

    @Test
    fun `starts on the start tab`() {
        assertEquals(listOf(Gallery), stack.backStack.toList())
        assertEquals(Gallery, stack.currentTab)
    }

    @Test
    fun `pushing the key already on top is ignored`() {
        stack.push(PhotoDetail("naxos", "Gallery"))
        stack.push(PhotoDetail("naxos", "Gallery"))
        assertEquals(listOf(Gallery, PhotoDetail("naxos", "Gallery")), stack.backStack.toList())
    }

    @Test
    fun `nav display sees the start tab then the current tab`() {
        stack.push(PhotoDetail("naxos", "Gallery"))
        stack.selectTab(Search)
        assertEquals(
            listOf(Gallery, PhotoDetail("naxos", "Gallery"), Search),
            stack.backStack.toList(),
        )
        stack.selectTab(Boards)
        assertEquals(
            listOf(Gallery, PhotoDetail("naxos", "Gallery"), Boards),
            stack.backStack.toList(),
        )
    }

    @Test
    fun `returning to the start tab shows only its stack`() {
        stack.push(PhotoDetail("naxos", "Gallery"))
        stack.selectTab(Search)
        stack.selectTab(Gallery)
        assertEquals(listOf(Gallery, PhotoDetail("naxos", "Gallery")), stack.backStack.toList())
    }

    @Test
    fun `a tab keeps its position across switches`() {
        stack.selectTab(Boards)
        stack.push(BoardDetail("blue"))
        stack.selectTab(Gallery)
        stack.selectTab(Boards)
        assertEquals(BoardDetail("blue"), stack.backStack.last())
    }

    @Test
    fun `back pops within the current tab`() {
        stack.selectTab(Boards)
        stack.push(BoardDetail("blue"))
        assertTrue(stack.pop())
        assertEquals(Boards, stack.backStack.last())
    }

    @Test
    fun `back from another tab's root returns to the start tab`() {
        stack.selectTab(Search)
        assertTrue(stack.pop())
        assertEquals(Gallery, stack.currentTab)
        assertEquals(listOf(Gallery), stack.backStack.toList())
    }

    @Test
    fun `back from the start tab's root is not handled`() {
        assertFalse(stack.pop())
        assertEquals(listOf(Gallery), stack.backStack.toList())
    }

    @Test
    fun `the same photo opened from two tabs gets two distinct entries`() {
        stack.push(PhotoDetail("naxos", "Gallery"))
        stack.selectTab(Search)
        stack.push(PhotoDetail("naxos", "Search"))
        assertEquals(stack.backStack.size, stack.backStack.toSet().size)
    }
}
