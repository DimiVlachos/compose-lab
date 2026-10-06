package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A screen's own colours, for a screen with a light and a dark look of its own inside the lab's
 * dark one: the settings screen the pull-cord lamp hangs over, by [Day] and by [Night].
 *
 * @property background Behind the screen.
 * @property surface Its cards.
 * @property textPrimary Text and icons.
 * @property textMuted Secondary text: at least 4.5:1 against [surface], under the lamp's light too.
 * @property accent A switch that is on, and the avatar.
 * @property onAccent Text and the knob on the accent.
 * @property track A switch that is off: at least 3:1 against [surface], so it shows.
 * @property divider The lines between rows.
 */
@Immutable
internal data class ScreenPalette(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val track: Color,
    val divider: Color,
) {
    companion object {
        /** By day: warm paper white and ink, the lamp off. */
        val Day =
            ScreenPalette(
                background = Color(0xFFF4F0E8),
                surface = Color(0xFFFFFFFF),
                textPrimary = Color(0xFF1E1F24),
                textMuted = Color(0xFF6C6A66),
                accent = Color(0xFF2E6B57),
                onAccent = Color(0xFFFFFFFF),
                track = Color(0xFF8F8A81),
                divider = Color(0xFFE6E1D7),
            )

        /**
         * By lamplight: a deep blue-black room and soft text, the lamp's warm light falling on it.
         */
        val Night =
            ScreenPalette(
                background = Color(0xFF101318),
                surface = Color(0xFF1B1F27),
                textPrimary = Color(0xFFE9E5DC),
                textMuted = Color(0xFFA29E97),
                accent = Color(0xFFE0B46C),
                onAccent = Color(0xFF1B1F27),
                track = Color(0xFF6A7080),
                divider = Color(0xFF272C35),
            )
    }
}
