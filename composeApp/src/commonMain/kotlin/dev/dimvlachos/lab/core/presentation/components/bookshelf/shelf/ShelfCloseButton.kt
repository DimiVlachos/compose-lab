package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.BookshelfArt
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.bookshelf_close_book
import org.jetbrains.compose.resources.stringResource

/**
 * The open book's close button: a paper cross on a disc of the book's own ink, over the dimmed
 * shelf, fading in with the book. It puts the book back, as Back does.
 */
@Composable
internal fun ShelfCloseButton(
    alpha: () -> Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.bookshelf_close_book)
    Box(
        modifier
            .padding(12.dp)
            .size(48.dp)
            .graphicsLayer { this.alpha = alpha() }
            .semantics { contentDescription = label }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClose,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(40.dp)) {
            val r = size.minDimension / 2f
            drawCircle(BookshelfArt.CloseDisc, r)
            drawCircle(BookshelfArt.CloseRim, r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
            val arm = 6.5.dp.toPx()
            val c = center
            val stroke = 2.dp.toPx()
            drawLine(
                BookshelfArt.ClosePaper,
                c + Offset(-arm, -arm),
                c + Offset(arm, arm),
                stroke,
                StrokeCap.Round,
            )
            drawLine(
                BookshelfArt.ClosePaper,
                c + Offset(-arm, arm),
                c + Offset(arm, -arm),
                stroke,
                StrokeCap.Round,
            )
        }
    }
}
