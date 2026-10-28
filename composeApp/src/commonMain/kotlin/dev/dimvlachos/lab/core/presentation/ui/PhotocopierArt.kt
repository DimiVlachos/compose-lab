package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The household lists screen round the photocopier, as the prototype styles it (its `--a-*`): warm
 * paper by day, a dark warm grey by night. [back] is a card's reverse, [chip] the search field's
 * fill, [accent] the focus ring and [scrim] the dimming while the copier is up.
 */
internal class PhotocopierScreenColors(
    val background: Color,
    val card: Color,
    val back: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    val scrim: Color,
) {
    /** A warm copy's fill: `color-mix(in srgb, card 88%, #ffb575)`. */
    val warmCard: Color = lerp(card, Color(0xFFFFB575), .12f)

    /** The dashed outline a card leaves on the glass: `color-mix(muted 55%, transparent)`. */
    val outline: Color = muted.copy(alpha = .55f)

    internal companion object {
        val Avatar = Color(0xFF4F7A63)
        val WarmBorder = Color(230, 140, 64, (.6f * 255).toInt())
        val SnackFill = Color(0xFF2B2825)
        val SnackInk = Color(0xFFF4EFE6)
        val UndoInk = Color(0xFFFFC68A)

        val Light =
            PhotocopierScreenColors(
                background = Color(0xFFF3F1EC),
                card = Color(0xFFFFFEFB),
                back = Color(0xFFF4F1EA),
                ink = Color(0xFF23252C),
                muted = Color(0xFF7A7268),
                line = Color(0xFFE2DCD0),
                chip = Color(40, 30, 20, (.06f * 255).toInt()),
                accent = Color(0xFF2F7D4F),
                scrim = Color(38, 30, 22, (.24f * 255).toInt()),
            )
        val Dark =
            PhotocopierScreenColors(
                background = Color(0xFF18171B),
                card = Color(0xFF232227),
                back = Color(0xFF2A2930),
                ink = Color(0xFFECE8E1),
                muted = Color(0xFFA69F95),
                line = Color(0xFF35323B),
                chip = Color(255, 255, 255, (.07f * 255).toInt()),
                accent = Color(0xFF7FD3A0),
                scrim = Color(0, 0, 0, (.4f * 255).toInt()),
            )
    }
}
