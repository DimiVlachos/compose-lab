package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

/**
 * What the shelf takes from its app's theme: day or dusk, the colour it fades into at its edges,
 * the pace of its chrome, and the size of a book's type.
 */
internal object ShelfTheme {
    val isDark: Boolean
        @Composable get() = LabTheme.isDark

    val colors: ShelfColors
        @Composable get() = ShelfColors(LabTheme.colors.background)

    val motion: ShelfMotionTokens
        get() = ShelfMotionTokens

    val typography: ShelfType
        get() = ShelfType
}

internal class ShelfColors(val background: Color)

internal object ShelfMotionTokens {
    val ease: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    const val fastMs = 200
    const val mediumMs = 300
    const val slowMs = 400
}

/** A book's type: its sizes; the face itself is the shelf's Instrument Serif. */
internal object ShelfType {
    val bookPage = TextStyle(fontSize = 16.sp, lineHeight = 23.sp)
    val bookPageCaption = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp)
}
