package dev.dimvlachos.moodboard.nav

import dev.dimvlachos.moodboard.android.nav.BoardDetail
import dev.dimvlachos.moodboard.android.nav.BoardEditor
import dev.dimvlachos.moodboard.android.nav.Boards
import dev.dimvlachos.moodboard.android.nav.Gallery
import dev.dimvlachos.moodboard.android.nav.NavStep
import dev.dimvlachos.moodboard.android.nav.Search
import dev.dimvlachos.moodboard.android.nav.navStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavStepTest {
    @Test
    fun `opening a board is one screen pushed`() {
        assertEquals(
            NavStep.Push,
            navStep(listOf(Gallery, Boards), listOf(Gallery, Boards, BoardDetail("b"))),
        )
    }

    @Test
    fun `leaving the editor is one screen popped`() {
        assertEquals(
            NavStep.Pop,
            navStep(
                listOf(Gallery, Boards, BoardDetail("b"), BoardEditor("b")),
                listOf(Gallery, Boards, BoardDetail("b")),
            ),
        )
    }

    @Test
    fun `switching away from a tab with a board open is not a pop`() {
        assertNull(navStep(listOf(Gallery, Boards, BoardDetail("b")), listOf(Gallery)))
    }

    @Test
    fun `switching to a tab with a board open is not a push`() {
        assertNull(navStep(listOf(Gallery), listOf(Gallery, Boards, BoardDetail("b"))))
    }

    @Test
    fun `switching between two side tabs is neither`() {
        assertNull(navStep(listOf(Gallery, Boards, BoardDetail("b")), listOf(Gallery, Search)))
    }
}
