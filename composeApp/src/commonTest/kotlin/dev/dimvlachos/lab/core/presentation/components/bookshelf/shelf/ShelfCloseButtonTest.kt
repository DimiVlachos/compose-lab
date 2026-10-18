package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ShelfCloseButtonTest {
    @Test
    fun theCloseButtonIsALabelledButtonThatPutsTheBookBack() = runComposeUiTest {
        var closed = 0
        setContent { ShelfCloseButton(alpha = { 1f }, onClose = { closed++ }) }

        val button = onNodeWithContentDescription("Close the book")
        button.assertHasClickAction()
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        button.performClick()

        assertEquals(1, closed)
    }
}
