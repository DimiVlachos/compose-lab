package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The browser's History screen round the sand mandala, as the prototype's `--a-*` colours: warm
 * paper by day, near black by night. [accent] is the browser's blue (Clear history, Clear now, the
 * restore link), [snack] the snackbar with its [snackInk] and [snackAccent] Undo, [chalk] the
 * outline drawn before the sand is poured. The grains' own shadows are [shadow1] to [shadow3],
 * ARGB, as one, two or three grains' shadows overlap.
 */
internal class SandMandalaScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val snack: Color,
    val snackInk: Color,
    val snackAccent: Color,
    val chalk: Color,
    val shadow1: Int,
    val shadow2: Int,
    val shadow3: Int,
) {
    internal companion object {
        /**
         * The nine sites in the history, each a favicon's colour and a pigment of the mandala:
         * Copperleaf Books, Bluebird Credit Union, Pine & Pantry, Mesa Weather, Lantern, Saguaro
         * Maps, Harbor Times, Trailhead Outfitters and Prairie Rail.
         */
        val Sites =
            intArrayOf(
                0xC2412D,
                0x2D5FB8,
                0x3F8F4A,
                0xE3A126,
                0x7A3F9A,
                0x1F8F8A,
                0x24345C,
                0xD9682A,
                0xB8325C,
            )

        /** A favicon's letter. */
        val FavInk = Color(0xF2FFFFFF)

        val Light =
            SandMandalaScreenColors(
                background = Color(0xFFF7F4EE),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF202227),
                muted = Color(0xFF74767E),
                line = Color(0xFFE5E1D9),
                accent = Color(0xFF2F5FB3),
                snack = Color(0xFF2A2B30),
                snackInk = Color(0xFFF2F0EC),
                snackAccent = Color(0xFFA9C4FF),
                chalk = Color(0x33503C28),
                shadow1 = 0x400E1A28,
                shadow2 = 0x5C0E1A28,
                shadow3 = 0x700E1A28,
            )
        val Dark =
            SandMandalaScreenColors(
                background = Color(0xFF16171A),
                card = Color(0xFF1F2024),
                ink = Color(0xFFECEAE6),
                muted = Color(0xFF9A9BA3),
                line = Color(0xFF2E2F35),
                accent = Color(0xFF9DBCFF),
                snack = Color(0xFFE9E6E0),
                snackInk = Color(0xFF1C1D21),
                snackAccent = Color(0xFF2F5FB3),
                chalk = Color(0x38FFFAEB),
                shadow1 = 0x66000000,
                shadow2 = 0x8A000000.toInt(),
                shadow3 = 0xA0000000.toInt(),
            )
    }
}
