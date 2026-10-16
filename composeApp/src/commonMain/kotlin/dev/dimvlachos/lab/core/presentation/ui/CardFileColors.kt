package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The chrome card file on its walnut desk at night, which looks the same whatever the theme: the
 * scene is always lit by the one warm lamp.
 */
internal object CardFileColors {
    // The desk, dark at the top and warmer toward the viewer, and the lamp's pool on it.
    val night = Color(0xFF120D0A)
    val woodMid = Color(0xFF2A1B12)
    val woodNear = Color(0xFF3B2618)
    val lampPool = Color(0x66FFD9A0)
    val lampPoolEdge = Color(0x1AFFC070)

    // The chrome rod, knobs and feet, and the base the feet stand on.
    val chromeBright = Color(0xFFF2F4F7)
    val chromeShade = Color(0xFF8C939C)
    val chromeSheen = Color(0xFFDDE1E6)
    val chromeDeep = Color(0xFF5E646C)
    val chromeFoot = Color(0xFFB9BFC6)
    val base = Color(0xFF2B2420)
    val knurl = Color(0x55000000)
    val knurlLight = Color(0x66FFFFFF)
    val baseTop = Color(0xFF4A403A)
    val deskShadow = Color(0x66000000)

    // The cards: cream stock, buff dividers with brown letters, and the notches for the rail.
    val paper = Color(0xFFF4ECD8)
    val dividerPaper = Color(0xFFE3D3AE)
    val dividerInk = Color(0xFF6B4F2A)
    val cardEdge = Color(0x33000000)
    val notch = Color(0xFF1C140F)
    /** A card's plain back, seen once it has fallen forward over the top. */
    val cardBack = Color(0xFFCFC3A6)

    /** Dims the desk behind a pulled-out card. */
    val scrim = Color(0xE0000000)
}
