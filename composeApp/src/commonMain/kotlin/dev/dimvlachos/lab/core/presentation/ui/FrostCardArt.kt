package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The bank's card screen round the frost card, as the prototype's `--a-*` colours: a pale stone
 * screen by day and a near-black one by night. [card] is the Freeze card row's face, [track] the
 * switch's off track, [accent] the bank's green (Cards, an income, the active tab), and [ice] the
 * switch's on track and the frozen row's tint. The card itself is the same in both.
 */
internal class FrostCardScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val track: Color,
    val accent: Color,
    val ice: Color,
) {
    internal companion object {
        /** The frozen row's snowflake. */
        val IceInk = Color(0xFF3F7FB0)

        /** The frosted switch knob, white into ice blue, and the snowflake on it. */
        val KnobWhite = Color(0xFFFFFFFF)
        val KnobFrost = Color(0xFFE6F1FB)
        val KnobEdge = Color(0xFFC4DBEF)
        val KnobFlake = Color(0xFF86AED1)

        /** The recent purchases' logo colours, as the prototype's. */
        val Larkspur = Color(0xFF8A5A3B)
        val GreenAcres = Color(0xFF4F7F3A)
        val Cascade = Color(0xFFB8862C)
        val Brightline = Color(0xFF3B6A8F)
        val Northside = Color(0xFF9A4646)

        val Light =
            FrostCardScreenColors(
                background = Color(0xFFF4F5F2),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF1D2226),
                muted = Color(0xFF6F767C),
                line = Color(0xFFE2E4E0),
                track = Color(0xFFD5D9DC),
                accent = Color(0xFF2C6B57),
                ice = Color(0xFFA9CBE6),
            )
        val Dark =
            FrostCardScreenColors(
                background = Color(0xFF121517),
                card = Color(0xFF1C2023),
                ink = Color(0xFFE9ECEE),
                muted = Color(0xFF98A0A6),
                line = Color(0xFF2C3236),
                track = Color(0xFF3A4146),
                accent = Color(0xFF85D1B4),
                ice = Color(0xFF6F9CC2),
            )
    }
}
