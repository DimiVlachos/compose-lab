package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ShelfCloseActionTest {
    @Test
    fun screenReadersCanCloseTheOpenBook() = runComposeUiTest {
        var closed = 0
        setContent { ShelfCloseAction(onClose = { closed++ }) }

        val actions =
            onRoot().fetchSemanticsNode().children.flatMap {
                it.config.getOrElse(SemanticsActions.CustomActions) { emptyList() }
            }
        val close = actions.single { it.label == "Close the book" }
        runOnIdle { close.action() }

        assertEquals(1, closed)
    }
}
