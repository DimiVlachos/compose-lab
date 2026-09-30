package dev.dimvlachos.lab.bookdemo.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.book_spread_1
import dev.dimvlachos.lab.resources.book_spread_2
import dev.dimvlachos.lab.resources.book_spread_3
import dev.dimvlachos.lab.resources.book_spread_4
import org.jetbrains.compose.resources.imageResource

/**
 * The book's four spreads, a little journey: a castle on a hill, cottages on a canal, a harbour
 * town and balloons over it all. Each photo is 2592 x 1296, so a page splits into 18 whole-pixel
 * strips. Null until every one has loaded: resources arrive as a 1 x 1 stand-in first.
 */
@Composable
internal fun rememberBookSpreads(): List<ImageBitmap>? {
    val spreads =
        listOf(
            imageResource(Res.drawable.book_spread_1),
            imageResource(Res.drawable.book_spread_2),
            imageResource(Res.drawable.book_spread_3),
            imageResource(Res.drawable.book_spread_4),
        )
    return spreads.takeIf { images -> images.all { it.width > 1 } }
}
