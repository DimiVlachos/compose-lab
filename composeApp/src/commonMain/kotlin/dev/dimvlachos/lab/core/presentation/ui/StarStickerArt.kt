package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The "Rate your order" screen round the star stickers, as the prototype styles it: warm paper by
 * day and a dark roast by night, with a burnt orange accent once a rating is set. [glow] is the
 * window light over its top left corner.
 */
internal class StarStickerScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val cardShadow: Color,
) {
    internal companion object {
        val Light =
            StarStickerScreenColors(
                background = Color(0xFFF7F3EC),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF211D18),
                muted = Color(0xFF756D63),
                line = Color(0xFFE6DFD4),
                accent = Color(0xFFC4581E),
                cardShadow = Color(0x593C280A),
            )
        val Dark =
            StarStickerScreenColors(
                background = Color(0xFF16130F),
                card = Color(0xFF211D18),
                ink = Color(0xFFF1ECE4),
                muted = Color(0xFFA69C8F),
                line = Color(0xFF353027),
                accent = Color(0xFFF08A4B),
                cardShadow = Color(0x593C280A),
            )
    }
}

/** The prototype's fixed inks on the screen: the glow, the toast, and the taco truck's avatar. */
internal object StarStickerArt {
    val glow = Color(0x38FFD696)
    val onAccent = Color(0xFFFFFFFF)
    val toast = Color(0xEB1C160E)
    val toastInk = Color(0xFFF6EFE2)
    val toastCheck = Color(0xFFF3C45E)
    val avatarTop = Color(0xFFF6A04D)
    val avatarBottom = Color(0xFFD9482B)
    val avatarEdge = Color(0x33FFFFFF)
    val tortilla = Color(0xFFFFD77A)
    val tortillaEdge = Color(0xFFB9781F)
    val lettuce = Color(0xFF5FAE4A)
    val tomato = Color(0xFFE2452F)
    val onion = Color(0xFFFFF4D6)
    val shine = Color(0xB3FFFFFF)
}
