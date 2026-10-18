package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageTurnState
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.rememberPageTurnState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReadingShelfUiTest {
    private val books =
        listOf(
            listOf(
                ShelfBook(11, "Cotton and Iron", "Lydia Marsh", "#24343A", 9.0),
                ShelfBook(12, "The Prairie Schoolhouse", "Amos Reed", "#E7DCC4", 6.5),
                ShelfBook(13, "Bayou Lanterns", "Celeste Fontaine", "#2E5E6B", 8.0),
            ),
            listOf(ShelfBook(21, "Harbor Lights of Mystic", "Thomas Avery", "#60263E", 8.5)),
        )

    private val spine = "Bayou Lanterns by Celeste Fontaine"

    private class Held(val shelf: ShelfState, val scroll: ScrollState, val turns: PageTurnState)

    // Bayou Lanterns has a chapter of a dozen paragraphs: several pages to turn.
    private val chapter =
        ShelfChapter(
            "Chapter One",
            List(12) {
                "The river ran slow and brown past the dock.".split(' ').let { w ->
                    List(6) { w }.flatten()
                }
            },
        )

    // Two planks below a band of list, scrolling, as the demo lays them out.
    private fun ComposeUiTest.planks(withChapter: Boolean = false): Held {
        lateinit var held: Held
        setContent {
            val shelf = rememberShelfState()
            val scroll = rememberScrollState()
            val turns = rememberPageTurnState()
            held = Held(shelf, scroll, turns)
            val content = rememberShelfContent(books, readingBookId = null)
            // As a preview draws it: no endless frame loop for the leaves, so the test can idle.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ReadingShelf(
                    content,
                    shelf,
                    chapterFor = { id -> chapter.takeIf { withChapter && id == 13 } },
                    Modifier.size(412.dp, 700.dp),
                    turns = turns,
                ) { hero ->
                    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                        Spacer(Modifier.height(200.dp))
                        hero(Modifier.size(412.dp, 640.dp))
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 20_000) {
            onAllNodes(hasContentDescription(spine)).fetchSemanticsNodes().isNotEmpty()
        }
        return held
    }

    @Test
    fun aPullDownOnASpineTipsTheBookOut() = runComposeUiTest {
        val held = planks()

        onNodeWithContentDescription(spine).performTouchInput {
            // By the top of the spine, down and a little out: the way a book is tipped.
            val from = Offset(centerX, top + height * 0.15f)
            swipe(from, from + Offset(0f, height * 0.9f), durationMillis = 250)
        }
        waitForIdle()

        assertTrue(held.shelf.activeBookId == 13, "taken: ${held.shelf.activeBookId}")
        assertEquals(0, held.scroll.value, "the list did not scroll")
    }

    @Test
    fun aDragUpOnASpineScrollsTheListInstead() = runComposeUiTest {
        val held = planks()

        onNodeWithContentDescription(spine).performTouchInput {
            val from = Offset(centerX, bottom - height * 0.1f)
            swipe(from, from - Offset(0f, height * 0.8f), durationMillis = 250)
        }
        waitForIdle()

        assertTrue(held.scroll.value > 0, "scrolled ${held.scroll.value}")
        assertEquals(null, held.shelf.activeBookId)
    }

    @Test
    fun aScreenReaderTurnsTheOpenBooksPagesAndPutsItBack() = runComposeUiTest {
        val held = planks(withChapter = true)
        runOnIdle { held.shelf.openDirect(13) }
        fun actions() =
            onAllNodes(hasCloseAction).fetchSemanticsNodes().singleOrNull()?.let {
                it.config[SemanticsActions.CustomActions]
            }
        fun act(label: String) = runOnIdle { actions()!!.single { it.label == label }.action() }
        waitUntil(timeoutMillis = 20_000) {
            held.shelf.mode == ShelfMode.Open && actions()?.any { it.label == "Next page" } == true
        }
        val described = onAllNodes(hasCloseAction).fetchSemanticsNodes().single()
        assertTrue(
            described.config[SemanticsProperties.ContentDescription]
                .single()
                .startsWith("Bayou Lanterns, page 1 of"),
            "the open book says what it is and where",
        )

        act("Next page")
        waitUntil(timeoutMillis = 10_000) { held.turns.page == 1 }
        act("Close the book")
        waitUntil(timeoutMillis = 10_000) { held.shelf.mode == ShelfMode.Shelf }
    }

    private val hasCloseAction =
        SemanticsMatcher("has the close action") { node ->
            node.config
                .getOrElse(SemanticsActions.CustomActions) { emptyList() }
                .any {
                    it.label == "Close the book"
                }
        }
}
