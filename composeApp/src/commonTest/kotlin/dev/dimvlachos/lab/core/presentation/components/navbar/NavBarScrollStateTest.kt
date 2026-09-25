package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.test.Test
import kotlin.test.assertEquals

class NavBarScrollStateTest {
    private fun state() = NavBarScrollState(collapseDistancePx = 100f)

    @Test
    fun scrollingTowardTheEndCollapsesProportionally() {
        val state = state()
        state.onScroll(-25f)
        assertEquals(0.25f, state.collapse, 0.0001f)
    }

    @Test
    fun scrollingBackExpands() {
        val state = state()
        state.onScroll(-100f)
        state.onScroll(40f)
        assertEquals(0.6f, state.collapse, 0.0001f)
    }

    @Test
    fun hugeDeltasAreClampedInBothDirections() {
        val state = state()
        state.onScroll(-100_000f)
        assertEquals(1f, state.collapse)
        state.onScroll(-5f)
        assertEquals(1f, state.collapse)
        state.onScroll(100_000f)
        assertEquals(0f, state.collapse)
        state.onScroll(5f)
        assertEquals(0f, state.collapse)
    }

    @Test
    fun nestedScrollObservesWithoutConsuming() {
        val state = state()
        val consumed =
            state.nestedScrollConnection.onPreScroll(Offset(0f, -50f), NestedScrollSource.UserInput)
        assertEquals(Offset.Zero, consumed)
        assertEquals(0.5f, state.collapse, 0.0001f)
    }
}
