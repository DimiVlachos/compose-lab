package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Android lays text out safely from several threads, so faces bake four at a time.
internal actual fun shelfTextBakers(): CoroutineDispatcher =
    Dispatchers.Default.limitedParallelism(4)
