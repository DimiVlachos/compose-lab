package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * Fernwood Market's screen round the paper bag, as the prototype styles it: warm paper by day and a
 * dark roast by night, with a grocer's green for Add. [tile] is the square under each product's
 * art, [chip] the category chips and the Done button.
 */
internal class PaperBagColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    val accentInk: Color,
    val tile: Color,
) {
    internal companion object {
        val Light =
            PaperBagColors(
                background = Color(0xFFFBF8F2),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF1F1C19),
                muted = Color(0xFF73695D),
                line = Color(0xFFECE4D6),
                chip = Color(0x0F3C2814),
                accent = Color(0xFF2F6B3A),
                accentInk = Color(0xFFFFFFFF),
                tile = Color(0xFFF3EDE2),
            )
        val Dark =
            PaperBagColors(
                background = Color(0xFF161412),
                card = Color(0xFF201D1A),
                ink = Color(0xFFF1ECE6),
                muted = Color(0xFFA69D92),
                line = Color(0xFF2E2A26),
                chip = Color(0x12FFFFFF),
                accent = Color(0xFF6FBF7C),
                accentInk = Color(0xFF0D1F10),
                tile = Color(0xFF26221E),
            )
    }
}
