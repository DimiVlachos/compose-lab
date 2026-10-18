package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Skia's font collection is not guarded for threads, so text here is set one bake at a time.
internal actual fun shelfTextBakers(): CoroutineDispatcher =
    Dispatchers.Default.limitedParallelism(1)
