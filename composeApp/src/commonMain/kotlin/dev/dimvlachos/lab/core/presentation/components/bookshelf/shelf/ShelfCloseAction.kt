package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.bookshelf_close_book
import org.jetbrains.compose.resources.stringResource

/**
 * The open book's close, for screen readers: an action on the book, as a swipe down or Back is for
 * a finger. Nothing is drawn and no touch is taken.
 */
@Composable
internal fun ShelfCloseAction(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.bookshelf_close_book)
    Box(
        modifier.fillMaxSize().semantics {
            customActions =
                listOf(
                    CustomAccessibilityAction(label) {
                        onClose()
                        true
                    }
                )
        }
    )
}
