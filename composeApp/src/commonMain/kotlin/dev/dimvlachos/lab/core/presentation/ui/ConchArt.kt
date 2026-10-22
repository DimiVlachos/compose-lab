package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The chat round the conch, as the prototype styles it (its `--a-*` tokens): warm sand by day and a
 * dark plum grey by night. [chip] is the conch button's well, [mine] the sent bubbles, [rec] the
 * recording dot and the armed cancel line, [accent] the focus ring.
 */
internal class ConchScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val mine: Color,
    val mineInk: Color,
    val rec: Color,
    val accent: Color,
) {
    internal companion object {
        val Light =
            ConchScreenColors(
                background = Color(0xFFF4EFE9),
                card = Color(0xFFFFFDFA),
                ink = Color(0xFF24252B),
                muted = Color(0xFF7A726D),
                line = Color(0xFFE0D8CF),
                chip = Color(40, 30, 20, (.07f * 255).toInt()),
                mine = Color(0xFF2D6AD3),
                mineInk = Color.White,
                rec = Color(0xFFD8453F),
                accent = Color(0xFFB9475A),
            )
        val Dark =
            ConchScreenColors(
                background = Color(0xFF18161A),
                card = Color(0xFF232126),
                ink = Color(0xFFEEEAE4),
                muted = Color(0xFFA9A097),
                line = Color(0xFF36323A),
                chip = Color(255, 255, 255, (.08f * 255).toInt()),
                mine = Color(0xFF3A77E0),
                mineInk = Color.White,
                rec = Color(0xFFFF6B62),
                accent = Color(0xFFF08B9A),
            )
    }
}
