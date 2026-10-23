package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The notes screen round the pencil cup, as the prototype styles it: warm paper by day and a dark
 * grey by night, cobalt for the accent. [chip] is the search field, [line] the cards' hairline,
 * [scrim] what dims the list while the fan or a sheet is out, [accentInk] the lit label's text.
 */
internal class PencilCupScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    val accentInk: Color,
    val scrim: Color,
) {
    internal companion object {
        /** The avatar's terracotta, the same in both themes. */
        val Avatar = Color(0xFFB4683E)

        val Light =
            PencilCupScreenColors(
                background = Color(0xFFF4F0E8),
                card = Color(0xFFFFFDF9),
                ink = Color(0xFF23252C),
                muted = Color(0xFF7A7268),
                line = Color(0xFFE2DBCF),
                chip = Color(40, 30, 20, 15),
                accent = Color(0xFF2B55B8),
                accentInk = Color(0xFFFFFFFF),
                scrim = Color(38, 30, 22, 71),
            )
        val Dark =
            PencilCupScreenColors(
                background = Color(0xFF18171B),
                card = Color(0xFF222126),
                ink = Color(0xFFECE8E1),
                muted = Color(0xFFA69F95),
                line = Color(0xFF34313A),
                chip = Color(255, 255, 255, 18),
                accent = Color(0xFF9AB6FF),
                accentInk = Color(0xFF0B1430),
                scrim = Color(0, 0, 0, 107),
            )
    }
}
