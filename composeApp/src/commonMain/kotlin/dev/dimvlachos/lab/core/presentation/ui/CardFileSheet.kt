package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The frosted sheet under the card file, as the prototype styles it: warm paper by day and a dark
 * walnut glass by night, with a burnt orange accent for Call. The desk above it is always night.
 */
internal class CardFileSheetColors(
    val sheet: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
) {
    internal companion object {
        val Light =
            CardFileSheetColors(
                sheet = Color(0xDBFAF6EE),
                ink = Color(0xFF1F1B1A),
                muted = Color(0xFF6D6560),
                line = Color(0xFFDDD5CE),
                accent = Color(0xFFB4532A),
            )
        val Dark =
            CardFileSheetColors(
                sheet = Color(0xD61E1816),
                ink = Color(0xFFF2ECE7),
                muted = Color(0xFFA9A09A),
                line = Color(0xFF3A3336),
                accent = Color(0xFFE88A55),
            )

        /** The hint pill floating over the desk, and its hairline edge. */
        val HintPill = Color(0x73140E0A)
        val HintEdge = Color(0x24FFFFFF)
    }
}
