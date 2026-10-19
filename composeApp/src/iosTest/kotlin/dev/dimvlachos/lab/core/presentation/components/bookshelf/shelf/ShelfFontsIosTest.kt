package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import kotlin.test.Test
import kotlin.test.assertNotSame

class ShelfFontsIosTest {
    // Skia's font cache is not guarded for threads, and the shelf sets its text on a baker thread
    // while the app sets its own on the main one, so the shelf's text has a resolver of its own.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theShelfSetsItsTextWithAResolverTheAppDoesNotShare() = runComposeUiTest {
        lateinit var app: FontFamily.Resolver
        lateinit var shelf: FontFamily.Resolver
        setContent {
            app = LocalFontFamilyResolver.current
            shelf = rememberShelfFontResolver()
        }
        waitForIdle()

        assertNotSame(app, shelf)
    }
}
