package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Compose on Skia keeps one cache of text styles for the whole app, not guarded for threads, and
// every text laid out, through any resolver, touches it. So the shelf's text is set where the
// bake was asked for, on the thread that lays out all the app's other text.
internal actual fun shelfTextBakers(): CoroutineDispatcher = Dispatchers.Unconfined

@Composable
internal actual fun rememberShelfFontResolver(): FontFamily.Resolver =
    LocalFontFamilyResolver.current
