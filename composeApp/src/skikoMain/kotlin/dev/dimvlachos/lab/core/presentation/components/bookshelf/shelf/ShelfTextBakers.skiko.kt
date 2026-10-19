package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Skia's font cache is not guarded for threads, so the shelf's text is set one bake at a time, all
// of it on this one thread, with a resolver (and so a font cache) the main thread never touches.
internal actual fun shelfTextBakers(): CoroutineDispatcher =
    Dispatchers.Default.limitedParallelism(1)

@Composable
internal actual fun rememberShelfFontResolver(): FontFamily.Resolver = remember {
    createFontFamilyResolver()
}
