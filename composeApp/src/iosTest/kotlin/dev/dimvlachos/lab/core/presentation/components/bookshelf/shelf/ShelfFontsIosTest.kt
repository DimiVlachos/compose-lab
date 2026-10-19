package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.withContext
import platform.Foundation.NSThread

class ShelfFontsIosTest {
    // Compose keeps one cache of text styles for the whole app, unguarded for threads, and every
    // text laid out touches it, so the shelf sets its text on the thread that lays out the app's.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theShelfSetsItsTextOnTheThreadThatLaysOutTheApp() = runComposeUiTest {
        var app: NSThread? = null
        var shelf: NSThread? = null
        setContent {
            app = NSThread.currentThread
            LaunchedEffect(Unit) {
                shelf = withContext(ShelfTextBakers) { NSThread.currentThread }
            }
        }
        waitUntil { shelf != null }

        assertEquals(assertNotNull(app), shelf)
    }
}
