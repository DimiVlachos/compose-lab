package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_add
import dev.dimvlachos.lab.resources.ic_edit
import dev.dimvlachos.lab.resources.ic_home
import dev.dimvlachos.lab.resources.ic_home_filled
import dev.dimvlachos.lab.resources.ic_profile
import dev.dimvlachos.lab.resources.ic_profile_filled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AnimatedNavBarUiTest {
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

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
        onNodeWithText("Home").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun actionButtonExistsWhenTheSelectedTabHasAnActionAndTheLayerIsOn() = runComposeUiTest {
        setContent {
            AnimatedNavBar(
                testItems,
                selectedIndex = 3,
                onSelect = {},
                layers = NavBarLayers(action = true),
            )
        }
        onNodeWithContentDescription("Edit profile").assertExists()
    }

    @Test
    fun actionButtonDoesNotExistOnATabWithoutAnAction() = runComposeUiTest {
        setContent {
            AnimatedNavBar(
                testItems,
                selectedIndex = 0,
                onSelect = {},
                layers = NavBarLayers(action = true),
            )
        }
        onNodeWithContentDescription("Edit profile").assertDoesNotExist()
    }

    @Test
    fun actionButtonDoesNotExistWhenTheLayerIsOff() = runComposeUiTest {
        setContent {
            AnimatedNavBar(testItems, selectedIndex = 3, onSelect = {}, layers = NavBarLayers())
        }
        onNodeWithContentDescription("Edit profile").assertDoesNotExist()
    }

    @Test
    fun clickingTheActionButtonReportsTheSelectedIndex() = runComposeUiTest {
        mainClock.autoAdvance = false
        var clicked = -1
        setContent {
            AnimatedNavBar(
                testItems,
                selectedIndex = 3,
                onSelect = {},
                onActionClick = { clicked = it },
                layers = NavBarLayers(action = true),
            )
        }
        mainClock.advanceTimeBy(1_000)
        onNodeWithContentDescription("Edit profile").performClick()
        assertEquals(3, clicked)
    }

    @Test
    fun actionButtonHasButtonRole() = runComposeUiTest {
        setContent {
            AnimatedNavBar(
                testItems,
                selectedIndex = 3,
                onSelect = {},
                layers = NavBarLayers(action = true),
            )
        }
        onNodeWithContentDescription("Edit profile").assert(isButton)
    }

    @Test
    fun actionButtonNeverOverlapsTheBarAcrossCollapse() = runComposeUiTest {
        mainClock.autoAdvance = false
        val scrollState = NavBarScrollState(collapseDistancePx = 100f)
        setContent {
            AnimatedNavBar(
                testItems,
                selectedIndex = 3,
                onSelect = {},
                layers = NavBarLayers(action = true, scrollAware = true),
                scrollState = scrollState,
            )
        }
        mainClock.advanceTimeBy(2_000)

        fun assertButtonAdjacentToBar() {
            mainClock.advanceTimeBy(32)
            val barBounds = onNodeWithTag(NavBarRowTestTag).getBoundsInRoot()
            val buttonBounds = onNodeWithContentDescription("Edit profile").getBoundsInRoot()
            assertTrue(
                buttonBounds.left >= barBounds.right + 8.dp - 1.dp,
                "collapse=${scrollState.collapse} barRight=${barBounds.right} " +
                    "buttonLeft=${buttonBounds.left}",
            )
            assertEquals(
                (barBounds.bottom - barBounds.top).value,
                (buttonBounds.bottom - buttonBounds.top).value,
                1f,
                "collapse=${scrollState.collapse}",
            )
        }

        assertButtonAdjacentToBar()

        runOnUiThread { scrollState.onScroll(-50f) }
        Snapshot.sendApplyNotifications()
        assertButtonAdjacentToBar()

        runOnUiThread { scrollState.onScroll(-50f) }
        Snapshot.sendApplyNotifications()
        assertEquals(1f, scrollState.collapse)
        assertButtonAdjacentToBar()
    }

    @Test
    fun revealedButtonNeverOverlapsTheBarAcrossCollapseWithAllLayers() = runComposeUiTest {
        mainClock.autoAdvance = false
        var compositions = 0
        val scrollState = NavBarScrollState(collapseDistancePx = 100f)
        setContent {
            CompositionLocalProvider(LocalNavBarCompositionProbe provides { compositions++ }) {
                AnimatedNavBar(
                    testItems,
                    selectedIndex = 3,
                    onSelect = {},
                    layers = NavBarLayers.All,
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
            val barBounds = onNodeWithTag(NavBarRowTestTag).getBoundsInRoot()
            val buttonBounds = onNodeWithContentDescription("Edit profile").getBoundsInRoot()
            assertTrue(
                buttonBounds.left >= barBounds.right,
                "collapse=${scrollState.collapse} barRight=${barBounds.right} " +
                    "buttonLeft=${buttonBounds.left}",
            )
        }
        val afterScroll = compositions

        assertEquals(1f, scrollState.collapse)
        assertEquals(
            beforeScroll,
            afterScroll,
            "collapsing with the button already revealed must not recompose",
        )
    }

    @Test
    fun interruptingAHideWithAShowSettlesFullyShown() = runComposeUiTest {
        mainClock.autoAdvance = false
        var selected by mutableIntStateOf(3)
        var clicked = -1
        setContent {
            AnimatedNavBar(
                testItems,
                selected,
                onSelect = {},
                onActionClick = { clicked = it },
                layers = NavBarLayers(action = true),
            )
        }
        mainClock.advanceTimeBy(1_000)

        runOnUiThread { selected = 0 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(60)
        runOnUiThread { selected = 3 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(3_000)

        onNodeWithContentDescription("Edit profile").assertExists()
        onNodeWithContentDescription("Edit profile").performClick()
        mainClock.advanceTimeBy(1_000)
        assertEquals(3, clicked)
    }

    @Test
    fun interruptingAShowWithAHideSettlesFullyHidden() = runComposeUiTest {
        mainClock.autoAdvance = false
        var selected by mutableIntStateOf(0)
        setContent {
            AnimatedNavBar(testItems, selected, onSelect = {}, layers = NavBarLayers(action = true))
        }
        mainClock.advanceTimeBy(1_000)

        runOnUiThread { selected = 3 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(60)
        runOnUiThread { selected = 0 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(3_000)

        onNodeWithContentDescription("Edit profile").assertDoesNotExist()
    }

    @Test
    fun actionHideDoesNotRecomposeAfterTheSelectionFrame() = runComposeUiTest {
        mainClock.autoAdvance = false
        var compositions = 0
        var selected by mutableIntStateOf(3)
        setContent {
            CompositionLocalProvider(LocalNavBarCompositionProbe provides { compositions++ }) {
                AnimatedNavBar(
                    testItems,
                    selected,
                    onSelect = {},
                    layers = NavBarLayers(action = true),
                )
            }
        }
        mainClock.advanceTimeBy(1_000)
        val beforeSelection = compositions

        runOnUiThread { selected = 0 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(32)
        val afterSelection = compositions
        mainClock.advanceTimeBy(2_000)

        onNodeWithContentDescription("Edit profile").assertDoesNotExist()
        assertTrue(afterSelection > beforeSelection, "the selection change itself must recompose")
        assertEquals(afterSelection, compositions, "no recomposition is allowed while it fades")
    }

    @Test
    fun actionSwapDoesNotRecomposeAfterTheSelectionFrame() = runComposeUiTest {
        mainClock.autoAdvance = false
        var compositions = 0
        val items =
            listOf(
                NavItem(
                    "Home",
                    Res.drawable.ic_home,
                    Res.drawable.ic_home_filled,
                    NavAction(Res.drawable.ic_add, "New post"),
                ),
                NavItem(
                    "Profile",
                    Res.drawable.ic_profile,
                    Res.drawable.ic_profile_filled,
                    NavAction(Res.drawable.ic_edit, "Edit profile"),
                ),
            )
        var selected by mutableIntStateOf(0)
        setContent {
            CompositionLocalProvider(LocalNavBarCompositionProbe provides { compositions++ }) {
                AnimatedNavBar(items, selected, onSelect = {}, layers = NavBarLayers(action = true))
            }
        }
        mainClock.advanceTimeBy(1_000)
        val beforeSelection = compositions

        runOnUiThread { selected = 1 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(32)
        val afterSelection = compositions
        mainClock.advanceTimeBy(2_000)

        onNodeWithContentDescription("New post").assertDoesNotExist()
        onNodeWithContentDescription("Edit profile").assertExists()
        assertTrue(afterSelection > beforeSelection, "the selection change itself must recompose")
        assertEquals(
            afterSelection,
            compositions,
            "no recomposition is allowed while the icon swaps",
        )
    }

    @Test
    fun revealingAnActionOnTheAlreadySelectedTabShowsTheButtonAndInsetsTheBar() = runComposeUiTest {
        mainClock.autoAdvance = false
        val home = NavItem("Home", Res.drawable.ic_home, Res.drawable.ic_home_filled)
        val homeWithAction = home.copy(action = NavAction(Res.drawable.ic_add, "New post"))
        val profile =
            NavItem(
                "Profile",
                Res.drawable.ic_profile,
                Res.drawable.ic_profile_filled,
                NavAction(Res.drawable.ic_edit, "Edit profile"),
            )
        var items by mutableStateOf(listOf(home, profile))
        setContent {
            AnimatedNavBar(
                items,
                selectedIndex = 0,
                onSelect = {},
                layers = NavBarLayers(action = true),
            )
        }
        mainClock.advanceTimeBy(1_000)
        onNodeWithContentDescription("New post").assertDoesNotExist()
        val barRightBefore = onNodeWithTag(NavBarRowTestTag).getBoundsInRoot().right

        runOnUiThread { items = listOf(homeWithAction, profile) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_000)

        onNodeWithContentDescription("New post").assertExists()
        val barRightAfter = onNodeWithTag(NavBarRowTestTag).getBoundsInRoot().right
        assertTrue(
            barRightAfter < barRightBefore,
            "the bar must inset once the selected tab's action appears",
        )

        runOnUiThread { items = listOf(home, profile) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_000)

        onNodeWithContentDescription("New post").assertDoesNotExist()
        val barRightFinal = onNodeWithTag(NavBarRowTestTag).getBoundsInRoot().right
        assertEquals(barRightBefore.value, barRightFinal.value, 0.5f)
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

        onNodeWithContentDescription("Edit profile").assertExists()
        assertTrue(afterSelection > beforeSelection, "the selection change itself must recompose")
        assertEquals(afterSelection, compositions, "no recomposition is allowed while animating")
    }

    @Test
    fun barHeightDoesNotRecomposeWhileScrollingWithoutTheCutoutLayer() = runComposeUiTest {
        mainClock.autoAdvance = false
        var compositions = 0
        val scrollState = NavBarScrollState(collapseDistancePx = 100f)
        setContent {
            CompositionLocalProvider(LocalNavBarCompositionProbe provides { compositions++ }) {
                AnimatedNavBar(
                    testItems,
                    selectedIndex = 0,
                    onSelect = {},
                    layers = NavBarLayers(scrollAware = true),
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
        assertEquals(
            beforeScroll,
            afterScroll,
            "the bar height shrinking on scroll must not recompose",
        )
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
