package dev.dimvlachos.lab.catalog.presentation.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBar
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CatalogSearchBarUiTest {
    private var open by mutableStateOf(false)
    private var text by mutableStateOf("")
    private var opened = 0
    private var closed = 0

    private fun ComposeUiTest.showBar() {
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                LabTopBar(Modifier.width(BarWidth), titlePadding = 0.dp) {
                    CatalogSearchBar(
                        searchOpen = open,
                        text = text,
                        onTextChange = { text = it },
                        focusSearch = false,
                        onSearchFocused = {},
                        themeMode = ThemeMode.System,
                        onCycleTheme = {},
                        soundOn = false,
                        onToggleSound = {},
                        onOpenSearch = { opened++ },
                        onCloseSearch = { closed++ },
                    )
                }
            }
        }
        mainClock.advanceTimeBy(500)
    }

    private fun ComposeUiTest.set(value: Boolean, thenMs: Long) {
        runOnUiThread { open = value }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(thenMs)
    }

    private fun ComposeUiTest.pillWidth(): Dp =
        onNodeWithTag(CatalogSearchPillTag).getBoundsInRoot().let { it.right - it.left }

    // The field inside the pill is laid out at the pill's size on every frame of the morph, where
    // the pill's slot holds its final size throughout.
    private fun ComposeUiTest.fieldWidth(): Dp =
        onNode(hasSetTextAction()).getBoundsInRoot().let { it.right - it.left }

    @Test
    fun theButtonGrowsIntoThePillAndShrinksBack() = runComposeUiTest {
        showBar()
        onNodeWithContentDescription("Search").performClick()
        assertEquals(1, opened)
        onNodeWithText("compose-lab").assertExists()
        set(true, thenMs = 16)
        val start = fieldWidth()
        mainClock.advanceTimeBy(84)
        val midway = fieldWidth()
        assertTrue(midway > start, "growing, from $start to $midway")
        assertTrue(midway < OpenFieldWidth - 20.dp, "but not there yet, got $midway")
        mainClock.advanceTimeBy(1_500)
        assertEquals(OpenPillWidth.value, pillWidth().value, 1f)
        assertEquals(OpenFieldWidth.value, fieldWidth().value, 1f)
        onNodeWithText("compose-lab").assertDoesNotExist()
        onNodeWithContentDescription("Search").assertDoesNotExist()
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, closed)
        set(false, thenMs = 100)
        val shrinking = fieldWidth()
        assertTrue(shrinking < OpenFieldWidth - 20.dp, "shrinking, got $shrinking")
        mainClock.advanceTimeBy(1_500)
        onNodeWithTag(CatalogSearchPillTag).assertDoesNotExist()
        onNodeWithContentDescription("Search").assertExists()
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun theClearButtonShowsOnlyWhileThereIsText() = runComposeUiTest {
        showBar()
        set(true, thenMs = 1_500)
        onNodeWithContentDescription("Clear search").assertDoesNotExist()
        runOnUiThread { text = "bath" }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(100)
        onNodeWithContentDescription("Clear search").performClick()
        assertEquals("", text)
    }

    private companion object {
        val BarWidth = 412.dp
        // The bar's 8 dp side padding, the row's 4 dp insets, and the 40 dp back arrow.
        val OpenPillWidth = BarWidth - 8.dp * 2 - 4.dp * 2 - 40.dp
        // Inside the pill: its 16 dp padding, and the 20 dp magnifier with the 12 dp gap after it.
        // The clear button takes no room while the field is empty.
        val OpenFieldWidth = OpenPillWidth - 16.dp * 2 - 20.dp - 12.dp
    }
}
