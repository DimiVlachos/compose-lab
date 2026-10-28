package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The camping store's results screen round the coin sorter, as the prototype styles it (`--a-*`): a
 * warm off-white by day and a green-black by night. [chip] is the search field's and the cart's
 * fill and a lit option's, [line] the cards' border and the empty lines' outline, [star] the
 * rating's star, [accent] the cart badge, the menu's check and the focus ring.
 */
internal class CoinSorterScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    val star: Color,
    val dark: Boolean,
) {
    internal companion object {
        val Light =
            CoinSorterScreenColors(
                background = Color(0xFFF3F2EE),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF1F2522),
                muted = Color(0xFF6D736F),
                line = Color(0xFFE1E1DB),
                chip = Color(30, 40, 30).copy(alpha = .06f),
                accent = Color(0xFF2F6B4F),
                star = Color(0xFFC98A12),
                dark = false,
            )
        val Dark =
            CoinSorterScreenColors(
                background = Color(0xFF161817),
                card = Color(0xFF212422),
                ink = Color(0xFFE9ECE9),
                muted = Color(0xFF9CA39E),
                line = Color(0xFF323733),
                chip = Color.White.copy(alpha = .07f),
                accent = Color(0xFF8FD1AD),
                star = Color(0xFFE6B04A),
                dark = true,
            )
    }
}

/** Each result's icon tile and glyph ink, as the prototype's, in its best-match order. */
internal val CoinSorterTints =
    listOf(
        Color(0xFFE6F0E1) to Color(0xFF3F6B3A),
        Color(0xFFF6E6D8) to Color(0xFF9A5426),
        Color(0xFFFDF0C8) to Color(0xFFA07612),
        Color(0xFFE2EBF5) to Color(0xFF2F5785),
        Color(0xFFFBECDC) to Color(0xFFB0641C),
        Color(0xFFECE5DD) to Color(0xFF5A4636),
        Color(0xFFE2F2EE) to Color(0xFF2D7266),
        Color(0xFFF1E3ED) to Color(0xFF8A3B6E),
    )
