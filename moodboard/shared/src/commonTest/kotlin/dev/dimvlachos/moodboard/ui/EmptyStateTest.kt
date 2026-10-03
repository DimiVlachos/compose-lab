package dev.dimvlachos.moodboard.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.moodboard.ui.components.EmptyState
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class EmptyStateTest {
    @Test
    fun showsTitleMessageAndAction() = runComposeUiTest {
        var added = false
        setContent {
            EmptyState(
                title = "No photos yet",
                message = "Add some from the gallery.",
                action = { TextButton(onClick = { added = true }) { Text("Add photos") } },
            )
        }
        onNodeWithText("No photos yet").assertExists()
        onNodeWithText("Add some from the gallery.").assertExists()
        onNodeWithText("Add photos").performClick()
        assertTrue(added)
    }
}
