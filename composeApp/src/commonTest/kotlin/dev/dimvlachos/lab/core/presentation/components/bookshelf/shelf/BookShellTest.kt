package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookShellTest {

    @Test
    fun aMissingShellIsNeverBakedEvenWhenThereIsNoCover() {
        val missing: BookShell? = null

        assertFalse(missing.isBakedWith(null))
    }

    @Test
    fun aShellBakedWithoutACoverMatchesARequestWithoutOne() {
        assertTrue(BookShell(bookId = 1, cover = null, faces = emptyMap()).isBakedWith(null))
    }
}
