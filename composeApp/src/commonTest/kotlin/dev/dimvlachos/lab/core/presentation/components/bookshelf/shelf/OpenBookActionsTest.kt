package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OpenBookActionsTest {
    @Test
    fun screenReadersFindTheOpenBookAndCanTurnItsPagesAndCloseIt() = runComposeUiTest {
        val done = mutableListOf<String>()
        setContent {
            OpenBookActions(
                description = "Bayou Lanterns, page 2 of 9",
                onClose = { done += "close" },
                onNext = { done += "next" },
                onPrevious = { done += "previous" },
            )
        }

        val node = onRoot().fetchSemanticsNode().children.single()
        assertEquals(
            listOf("Bayou Lanterns, page 2 of 9"),
            node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() },
            "a description, so a screen reader can focus it",
        )
        val actions = node.config[SemanticsActions.CustomActions]
        runOnIdle { actions.forEach { it.action() } }

        assertEquals(
            listOf("Next page", "Previous page", "Close the book"),
            actions.map { it.label },
        )
        assertEquals(listOf("next", "previous", "close"), done)
    }

    @Test
    fun aBookAtItsFirstPageOffersNoPreviousPage() = runComposeUiTest {
        setContent {
            OpenBookActions("Bayou Lanterns", onClose = {}, onNext = null, onPrevious = null)
        }

        val actions =
            onRoot().fetchSemanticsNode().children.single().config[SemanticsActions.CustomActions]

        assertEquals(listOf("Close the book"), actions.map { it.label })
    }
}
