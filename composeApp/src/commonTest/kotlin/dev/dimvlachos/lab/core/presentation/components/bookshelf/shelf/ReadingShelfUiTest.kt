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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
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

    private class Held(val shelf: ShelfState, val scroll: ScrollState)

    // Two planks below a band of list, scrolling, as the demo lays them out.
    private fun ComposeUiTest.planks(): Held {
        lateinit var held: Held
        setContent {
            val shelf = rememberShelfState()
            val scroll = rememberScrollState()
            held = Held(shelf, scroll)
            val content = rememberShelfContent(books, readingBookId = null)
            // As a preview draws it: no endless frame loop for the leaves, so the test can idle.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ReadingShelf(
                    content,
                    shelf,
                    chapterFor = { null },
                    Modifier.size(412.dp, 700.dp),
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
}
