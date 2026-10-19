package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Android lays text out safely from several threads, so faces bake four at a time.
internal actual fun shelfTextBakers(): CoroutineDispatcher =
    Dispatchers.Default.limitedParallelism(4)

@Composable
internal actual fun rememberShelfFontResolver(): FontFamily.Resolver =
    LocalFontFamilyResolver.current
