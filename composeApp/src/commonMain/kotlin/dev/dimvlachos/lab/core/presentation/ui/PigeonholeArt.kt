package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The mail screen round the pigeonhole rack, as the prototype's `--a-*` styles it: warm paper by
 * day and a dark warm brown by night. [card] and [line] are a row's face and border, [chip] the
 * search field and [accent] the keyboard focus ring.
 */
internal class PigeonholeScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
) {
    internal companion object {
        val Light =
            PigeonholeScreenColors(
                background = Color(0xFFF3EFE8),
                card = Color(0xFFFFFDF9),
                ink = Color(0xFF25221F),
                muted = Color(0xFF7B7268),
                line = Color(0xFFE3DACD),
                chip = Color(0x0F281E14),
                accent = Color(0xFF9A4A1C),
            )
        val Dark =
            PigeonholeScreenColors(
                background = Color(0xFF181614),
                card = Color(0xFF23201D),
                ink = Color(0xFFEEE8E0),
                muted = Color(0xFFA89E93),
                line = Color(0xFF36312C),
                chip = Color(0x12FFFFFF),
                accent = Color(0xFFF0A46E),
            )
    }
}

/**
 * The rack's own text, the same by day and night as the oak is: its cream title, the Done button's
 * warmer cream, the dark shadow both are pressed into the wood with, and the cubbies' focus ring.
 */
internal object PigeonholeRackInks {
    val title = Color(0xFFFFF3E1)
    val done = Color(0xFFFFE3B8)
    val shadow = Color(0x8C3C1C08)
    val ring = Color(0xFFFFD58A)
}
