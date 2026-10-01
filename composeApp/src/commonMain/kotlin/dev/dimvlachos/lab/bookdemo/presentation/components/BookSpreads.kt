package dev.dimvlachos.lab.bookdemo.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.book_spread_1
import dev.dimvlachos.lab.resources.book_spread_2
import dev.dimvlachos.lab.resources.book_spread_3
import dev.dimvlachos.lab.resources.book_spread_4
import dev.dimvlachos.lab.resources.book_spread_5
import dev.dimvlachos.lab.resources.book_spread_6
import dev.dimvlachos.lab.resources.book_spread_7
import dev.dimvlachos.lab.resources.book_spread_8
import org.jetbrains.compose.resources.imageResource

/**
 * The book: the first sixteen Sunday pages of Winsor McCay's "Little Nemo in Slumberland"
 * (1905-1906, public domain), Nemo's journey to Slumberland, two pages to a spread. Each comic page
 * sits on a square paper page; a spread is 2016 x 1008, so a page cuts into 18 whole-pixel strips.
 * Null until every one has loaded: resources arrive as a 1 x 1 stand-in first.
 */
@Composable
internal fun rememberBookSpreads(): List<ImageBitmap>? {
    val spreads =
        listOf(
            imageResource(Res.drawable.book_spread_1),
            imageResource(Res.drawable.book_spread_2),
            imageResource(Res.drawable.book_spread_3),
            imageResource(Res.drawable.book_spread_4),
            imageResource(Res.drawable.book_spread_5),
            imageResource(Res.drawable.book_spread_6),
            imageResource(Res.drawable.book_spread_7),
            imageResource(Res.drawable.book_spread_8),
        )
    return spreads.takeIf { images -> images.all { it.width > 1 } }
}
