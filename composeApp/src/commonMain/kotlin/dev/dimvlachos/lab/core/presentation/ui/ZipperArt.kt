package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The contacts screen round the zipper, as the prototype styles it: an off-white page by day and a
 * near-black by night, with white (or dark grey) cards, a hairline [line], a faint [chip] behind
 * the search field and a blue [accent] for the add button and the current tab.
 */
internal class ZipperScreenColors(
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
            ZipperScreenColors(
                background = Color(0xFFF6F5F2),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF22252C),
                muted = Color(0xFF767B84),
                line = Color(0xFFE4E2DD),
                chip = Color(30, 30, 40, (.07f * 255).toInt()),
                accent = Color(0xFF2F5FB3),
            )
        val Dark =
            ZipperScreenColors(
                background = Color(0xFF16171B),
                card = Color(0xFF1E2025),
                ink = Color(0xFFECEAE6),
                muted = Color(0xFFA09D98),
                line = Color(0xFF2C2E34),
                chip = Color(255, 255, 255, (.08f * 255).toInt()),
                accent = Color(0xFF8FB0EF),
            )
    }
}
