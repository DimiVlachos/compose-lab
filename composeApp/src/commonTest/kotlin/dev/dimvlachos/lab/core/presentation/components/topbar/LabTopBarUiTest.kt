package dev.dimvlachos.lab.core.presentation.components.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_arrow_back
import dev.dimvlachos.lab.resources.ic_search
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LabTopBarUiTest {
    @Test
    fun theBarHoldsItsButtonsAndTitleInARowAndItsBottomUnderThem() = runComposeUiTest {
        var backs = 0
        var searches = 0
        setContent {
            LabTheme {
                LabTopBar(
                    modifier = Modifier.testTag("bar"),
                    navigationIcon = {
                        LabIconButton(Res.drawable.ic_arrow_back, "Back", onClick = { backs++ })
                    },
                    actions = {
                        LabIconButton(Res.drawable.ic_search, "Search", onClick = { searches++ })
                    },
                    bottom = { Box(Modifier.fillMaxWidth().height(40.dp).testTag("bottom")) },
                ) {
                    LabTopBarTitle("Title")
                }
            }
        }
        onNodeWithContentDescription("Back").performClick()
        onNodeWithContentDescription("Search").performClick()
        assertEquals(1, backs)
        assertEquals(1, searches)
        val back = onNodeWithContentDescription("Back").getBoundsInRoot()
        val title = onNodeWithText("Title").getBoundsInRoot()
        val search = onNodeWithContentDescription("Search").getBoundsInRoot()
        val bottom = onNodeWithTag("bottom").getBoundsInRoot()
        val bar = onNodeWithTag("bar").getBoundsInRoot()
        assertTrue(back.right <= title.left && title.right <= search.left, "one row, in order")
        assertTrue(
            bottom.top >= back.bottom,
            "the bottom slot sits under the row",
        )
        assertTrue(bar.bottom - bar.top >= 56.dp + 40.dp, "a 56 dp row and the bottom slot")
    }
}
