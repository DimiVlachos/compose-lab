package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.bookshelf_close_book
import dev.dimvlachos.lab.resources.bookshelf_next_page
import dev.dimvlachos.lab.resources.bookshelf_previous_page
import org.jetbrains.compose.resources.stringResource

/**
 * The open book for screen readers: what it is and where ([description]), so it can be focused, and
 * its page turns and close as actions, as a swipe across, a swipe down or Back is for a finger.
 * [onNext] and [onPrevious] are null where there is no page to turn to. Nothing is drawn and no
 * touch is taken.
 */
@Composable
internal fun OpenBookActions(
    description: String,
    onClose: () -> Unit,
    onNext: (() -> Unit)?,
    onPrevious: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val next = stringResource(Res.string.bookshelf_next_page)
    val previous = stringResource(Res.string.bookshelf_previous_page)
    val close = stringResource(Res.string.bookshelf_close_book)
    Box(
        modifier.fillMaxSize().semantics {
            contentDescription = description
            customActions =
                listOfNotNull(
                    onNext?.let { action(next, it) },
                    onPrevious?.let { action(previous, it) },
                    action(close, onClose),
                )
        }
    )
}

private fun action(label: String, run: () -> Unit) =
    CustomAccessibilityAction(label) {
        run()
        true
    }
