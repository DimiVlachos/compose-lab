package dev.dimvlachos.moodboard.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.moodboard.boards.BoardEditorAction
import dev.dimvlachos.moodboard.boards.BoardEditorContent
import dev.dimvlachos.moodboard.boards.BoardEditorState
import dev.dimvlachos.moodboard.detail.PhotoDetailContent
import dev.dimvlachos.moodboard.detail.PhotoDetailState
import dev.dimvlachos.moodboard.domain.Board
import dev.dimvlachos.moodboard.domain.Photo
import dev.dimvlachos.moodboard.domain.PhotoFilter
import dev.dimvlachos.moodboard.domain.SortOrder
import dev.dimvlachos.moodboard.filter.FilterSheetContent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SharedContentTest {
    private val santorini =
        Photo("santorini", "Santorini", "files/photos/photo_santorini.jpg", setOf("sea"), true)

    @Test
    fun filterSheetTogglesFavoritesTagsAndSort() = runComposeUiTest {
        var filter by mutableStateOf(PhotoFilter())
        setContent {
            FilterSheetContent(
                filter,
                allTags = listOf("comic", "sea"),
                onFilterChange = { filter = it },
            )
        }
        onNodeWithTag("favoritesOnly").performClick()
        onNodeWithText("comic").performClick()
        onNodeWithText("Title").performClick()
        assertEquals(PhotoFilter(true, setOf("comic"), SortOrder.Title), filter)
        onNodeWithTag("favoritesOnly").assertIsOn()
        onNodeWithText("comic").performClick()
        assertEquals(emptySet(), filter.tags)
    }

    @Test
    fun photoDetailShowsTitleTagsAndBoards() = runComposeUiTest {
        val boards =
            listOf(Board("blue", "Blue", listOf("santorini")), Board("x", "Other", emptyList()))
        setContent { PhotoDetailContent(PhotoDetailState(santorini, boards, isDeleted = false)) }
        onNodeWithText("Santorini").assertExists()
        onNodeWithText("sea").assertExists()
        onNodeWithText("In Blue").assertExists()
        onNodeWithText("In Other").assertDoesNotExist()
    }

    @Test
    fun boardEditorRenamesAndToggles() = runComposeUiTest {
        val actions = mutableListOf<BoardEditorAction>()
        setContent {
            BoardEditorContent(
                BoardEditorState("Blue", listOf(santorini), selected = emptySet()),
                onAction = { actions += it },
            )
        }
        onNodeWithTag("boardName").performTextReplacement("Aegean")
        // The state never echoes the rename here; the field must still show what was typed.
        onNodeWithTag("boardName").assertTextContains("Aegean")
        onNodeWithTag("photo-santorini").performClick()
        assertEquals(
            listOf(BoardEditorAction.Rename("Aegean"), BoardEditorAction.Toggle("santorini")),
            actions,
        )
    }

    @Test
    fun photoDetailKeepsTheLastPhotoWhileItLeaves() = runComposeUiTest {
        var state by mutableStateOf(PhotoDetailState(santorini, emptyList(), isDeleted = false))
        setContent { PhotoDetailContent(state) }
        state = PhotoDetailState(photo = null, boards = emptyList(), isDeleted = true)
        onNodeWithText("Santorini").assertExists()
    }
}
