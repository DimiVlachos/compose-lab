package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlin.test.Test
import kotlin.test.assertEquals

class StackAlphasTest {
    @Test
    fun anOpenBookAtRestShowsBothStacks() {
        assertEquals(1f to 1f, stackAlphas(open = 1f, push = 0f))
    }

    @Test
    fun aPushedBookLeavesNoStackBehind() {
        // Once a push has flattened the pages the book moves in 3D; its flat stacks must be gone.
        assertEquals(0f to 0f, stackAlphas(open = 1f, push = 14f))
        assertEquals(0f to 0f, stackAlphas(open = 1f, push = 90f))
    }

    @Test
    fun aClosingCoverFadesTheLeftStackFirst() {
        val (left, right) = stackAlphas(open = 0.5f, push = 0f)
        assertEquals(0f, left)
        assertEquals(1f, right)
    }
}
