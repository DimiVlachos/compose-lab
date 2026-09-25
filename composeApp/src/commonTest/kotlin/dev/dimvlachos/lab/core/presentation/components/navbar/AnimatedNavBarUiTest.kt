package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AnimatedNavBarUiTest {
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    @Test
    fun clickingATabReportsItsIndex() = runComposeUiTest {
        var clicked = -1
        setContent { AnimatedNavBar(testItems, selectedIndex = 0, onSelect = { clicked = it }) }
        onNodeWithText("Saved").performClick()
        assertEquals(2, clicked)
    }

    @Test
    fun tabsExposeRoleAndSelection() = runComposeUiTest {
        setContent { AnimatedNavBar(testItems, selectedIndex = 1, onSelect = {}) }
        onAllNodes(isTab).assertCountEquals(4)
        onNodeWithText("Search").assertIsSelected()
        onNodeWithText("Home").assertIsNotSelected()
    }

    @Test
    fun emptyItemsRenderNothing() = runComposeUiTest {
        setContent { AnimatedNavBar(emptyList(), selectedIndex = 0, onSelect = {}) }
        onAllNodes(isTab).assertCountEquals(0)
    }

    @Test
    fun outOfRangeIndexIsClamped() = runComposeUiTest {
        setContent { AnimatedNavBar(testItems, selectedIndex = 9, onSelect = {}) }
        onNodeWithText("Profile").assertIsSelected()
    }

    @Test
    fun labelsStayAccessibleWhenCollapsed() = runComposeUiTest {
        val scrollState = NavBarScrollState(collapseDistancePx = 100f)
        scrollState.onScroll(-1_000f)
        setContent {
            AnimatedNavBar(testItems, selectedIndex = 0, onSelect = {}, scrollState = scrollState)
        }
        assertEquals(1f, scrollState.collapse)
        onNodeWithText("Home").assertExists()
        onNodeWithText("Profile").assertExists()
    }

    @Test
    fun barDoesNotRecomposeWhileAnimating() = runComposeUiTest {
        mainClock.autoAdvance = false
        var compositions = 0
        var selected by mutableIntStateOf(0)
        val scrollState = NavBarScrollState(collapseDistancePx = 100f)
        setContent {
            CompositionLocalProvider(LocalNavBarCompositionProbe provides { compositions++ }) {
                AnimatedNavBar(
                    testItems,
                    selected,
                    onSelect = {},
                    layers = NavBarLayers.All,
                    scrollState = scrollState,
                )
            }
        }
        mainClock.advanceTimeBy(1_000)
        val beforeSelection = compositions

        runOnUiThread { selected = 3 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(32)
        val afterSelection = compositions
        mainClock.advanceTimeBy(2_000)

        assertTrue(afterSelection > beforeSelection, "the selection change itself must recompose")
        assertEquals(afterSelection, compositions, "no recomposition is allowed while animating")
    }

    @Test
    fun barDoesNotRecomposeWhileTheScrollMorphAnimates() = runComposeUiTest {
        mainClock.autoAdvance = false
        var compositions = 0
        var selected by mutableIntStateOf(0)
        val scrollState = NavBarScrollState(collapseDistancePx = 100f)
        setContent {
            CompositionLocalProvider(LocalNavBarCompositionProbe provides { compositions++ }) {
                AnimatedNavBar(
                    testItems,
                    selected,
                    onSelect = {},
                    layers = NavBarLayers(indicator = true, cutout = true, scrollAware = true),
                    scrollState = scrollState,
                )
            }
        }
        mainClock.advanceTimeBy(1_000)
        val beforeScroll = compositions

        repeat(20) {
            runOnUiThread { scrollState.onScroll(-5f) }
            Snapshot.sendApplyNotifications()
            mainClock.advanceTimeBy(16)
        }
        val afterScroll = compositions
        assertEquals(1f, scrollState.collapse)
        assertEquals(beforeScroll, afterScroll, "scroll-driven morph frames must not recompose")

        runOnUiThread { selected = 3 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(32)
        val afterSelection = compositions
        mainClock.advanceTimeBy(2_000)

        assertTrue(afterSelection > afterScroll, "the selection change itself must recompose")
        assertEquals(afterSelection, compositions, "no recomposition is allowed while animating")
    }
}
