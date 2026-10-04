package dev.dimvlachos.moodboard.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavKey
import dev.dimvlachos.moodboard.android.nav.BoardDetail
import dev.dimvlachos.moodboard.android.nav.BoardEditor
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
    private val gallery = mutableStateListOf<NavKey>(Gallery)
    private val boards = mutableStateListOf<NavKey>(Boards)
    private val search = mutableStateListOf<NavKey>(Search)
    private val stack =
        TopLevelBackStack(
            start = Gallery,
            stacks = mapOf(Gallery to gallery, Boards to boards, Search to search),
            current = mutableStateOf(Gallery),
        )

    @Test
    fun `starts on the start tab`() {
        assertEquals(listOf(Gallery), stack.visible)
        assertEquals(Gallery, stack.currentTab)
    }

    @Test
    fun `pushing the key already on top is ignored`() {
        stack.push(PhotoDetail("naxos", "Gallery"))
        stack.push(PhotoDetail("naxos", "Gallery"))
        assertEquals(listOf(Gallery, PhotoDetail("naxos", "Gallery")), gallery.toList())
    }

    @Test
    fun `pushes land on the current tab's stack`() {
        stack.selectTab(Boards)
        stack.push(BoardDetail("blue"))
        assertEquals(listOf(Boards, BoardDetail("blue")), boards.toList())
        assertEquals(listOf(Gallery), gallery.toList())
    }

    @Test
    fun `visible keys are the start tab then the current tab`() {
        stack.push(PhotoDetail("naxos", "Gallery"))
        stack.selectTab(Search)
        assertEquals(listOf(Gallery, PhotoDetail("naxos", "Gallery"), Search), stack.visible)
        stack.selectTab(Gallery)
        assertEquals(listOf(Gallery, PhotoDetail("naxos", "Gallery")), stack.visible)
    }

    @Test
    fun `a tab keeps its stack across switches`() {
        stack.selectTab(Boards)
        stack.push(BoardDetail("blue"))
        stack.selectTab(Gallery)
        stack.selectTab(Boards)
        assertEquals(BoardDetail("blue"), stack.visible.last())
    }

    @Test
    fun `back pops within the current tab`() {
        stack.selectTab(Boards)
        stack.push(BoardDetail("blue"))
        assertTrue(stack.pop())
        assertEquals(listOf(Boards), boards.toList())
    }

    @Test
    fun `back from another tab's root returns to the start tab`() {
        stack.selectTab(Search)
        assertTrue(stack.pop())
        assertEquals(Gallery, stack.currentTab)
        assertEquals(listOf(Search), search.toList())
    }

    @Test
    fun `back from the start tab's root is not handled`() {
        assertFalse(stack.pop())
        assertEquals(listOf(Gallery), gallery.toList())
    }

    @Test
    fun `screens for deleted photos and boards leave every tab`() {
        stack.push(PhotoDetail("naxos", "Gallery"))
        stack.selectTab(Boards)
        stack.push(BoardDetail("blue"))
        stack.push(BoardEditor("blue"))
        stack.selectTab(Search)
        stack.push(PhotoDetail("milos", "Search"))
        stack.prune(photoIds = setOf("milos"), boardIds = setOf("islands"))
        assertEquals(listOf(Gallery), gallery.toList())
        assertEquals(listOf(Boards), boards.toList())
        assertEquals(listOf(Search, PhotoDetail("milos", "Search")), search.toList())
    }

    @Test
    fun `tab roots are never pruned`() {
        stack.prune(photoIds = emptySet(), boardIds = emptySet())
        assertEquals(listOf(Gallery), gallery.toList())
        assertEquals(listOf(Boards), boards.toList())
    }

    @Test
    fun `back from a screen pops only while that screen is on top`() {
        stack.selectTab(Boards)
        stack.push(BoardDetail("blue"))
        stack.push(PhotoDetail("naxos", tab = "Boards"))
        // A double tap on the photo's Back arrow: the second tap belongs to a screen already gone.
        assertTrue(stack.popFrom(PhotoDetail("naxos", tab = "Boards")))
        assertFalse(stack.popFrom(PhotoDetail("naxos", tab = "Boards")))
        assertEquals(listOf(Boards, BoardDetail("blue")), boards.toList())
        assertEquals(Boards, stack.currentTab)
    }
}
