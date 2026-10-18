package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The bookshelf's art colours, the same in either theme: the shelf has its own day and dusk light
 * instead.
 */
internal object BookshelfArt {
    /** The shade on a turning leaf as it faces away. */
    val LeafShade = Color(0xFF3A2B14)

    /** The glare on a turning leaf as it faces the light. */
    val LeafGlare = Color(0xFFFFFAF0)

    /** The shadow in the gutter where a page dips into the binding. */
    val LeafGutter = Color(0xFF342614)

    /** A page's body text, a warm near-black. */
    val BookInk = Color(0xEB261E18)

    /** A page's running head and folio, lighter than its text. */
    val BookCaptionInk = Color(0xCC3C2E22)

    /** The open book's close button: a disc of the books' warm ink, over the dimmed shelf. */
    val CloseDisc = Color(0xC7261E18)

    /** A hairline of gilt round the close button's disc, as on a cloth spine. */
    val CloseRim = Color(0x66F1D590)

    /** The close button's cross, in the pages' paper. */
    val ClosePaper = Color(0xFFFBF6EB)
}
