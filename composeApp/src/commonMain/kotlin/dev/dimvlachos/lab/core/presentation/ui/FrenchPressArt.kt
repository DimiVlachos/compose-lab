package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The news story round the French press, as the prototype styles it: warm newsprint by day and a
 * dark warm brown by night. [card] and [amberLine] are the summary card's paper and edge, [amber]
 * its ink (and the press's label), [red] the kicker's.
 */
internal class FrenchPressScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val red: Color,
    val amber: Color,
    val amberLine: Color,
) {
    internal companion object {
        val Light =
            FrenchPressScreenColors(
                background = Color(0xFFF7F3EC),
                card = Color(0xFFFFFAF1),
                ink = Color(0xFF22201D),
                muted = Color(0xFF75695B),
                line = Color(0xFFE3D9C9),
                red = Color(0xFFB3261E),
                amber = Color(0xFFA8641F),
                amberLine = Color(0xFFEAD2AD),
            )
        val Dark =
            FrenchPressScreenColors(
                background = Color(0xFF191714),
                card = Color(0xFF24201B),
                ink = Color(0xFFECE6DC),
                muted = Color(0xFFA99D8D),
                line = Color(0xFF39332B),
                red = Color(0xFFFF8A7A),
                amber = Color(0xFFE7A75E),
                amberLine = Color(0xFF5A4428),
            )
    }
}
