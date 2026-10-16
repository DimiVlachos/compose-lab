package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The snow globe's paints. The globe is an object lit by its own lamp, so these don't follow the
 * light or dark theme.
 */
internal object SnowGlobeArt {
    // Skies, top to horizon, one per scene.
    val sunnySkyTop = Color(0xFF5FA8E8)
    val sunnySkyLow = Color(0xFFBFE0F7)
    val windySkyTop = Color(0xFF7D9DB8)
    val windySkyLow = Color(0xFFC9D8E4)
    val showerSkyTop = Color(0xFF5E7186)
    val showerSkyLow = Color(0xFFA9B7C4)
    val showerCloud = Color(0xFFD7DEE6)
    val cloud = Color.White
    val rain = Color(0x88D6E4F0)
    val rainbow =
        listOf(
            Color(0x55E05A5A),
            Color(0x55E8A94F),
            Color(0x55E8DA5B),
            Color(0x5563B86A),
            Color(0x55588BD6),
            Color(0x557A5BC2),
        )

    // The figurine: dune, lighthouse, cottages and weathervane.
    val duneTop = Color(0xFFE9D7AE)
    val duneLow = Color(0xFFC9AE7C)
    val towerLit = Color(0xFFF7F4EE)
    val towerShade = Color(0xFFCFC8BC)
    val ironwork = Color(0xFF2B2F36)
    val lanternDay = Color(0xFFFFE7A3)
    val lanternLit = Color(0xFFFFD36B)
    val lanternBeam = Color(0x66FFE08A)
    val shingle = Color(0xFF9AA0A6)
    val roof = Color(0xFF5B5550)
    val window = Color(0xFFFFF3C8)
    val vanePost = Color(0xFF8C5A2B)
    val vane = Color(0xFFB87333)

    // Glitter.
    val gold = Color(0xFFE9C46A)
    val goldBright = Color(0xFFFFF4CF)

    // Glass: the drawn fallback's rim and window, and the rim line and bubble on both paths.
    val glassRimShade = Color(0x22101826)
    val glassFresnel = Color(0x55E8F0FF)
    val glassWindow = Color(0x8CFFFFFF)
    val glassRimLine = Color(0x40FFFFFF)
    val bubbleEdge = Color(0x30FFFFFF)
    val bubbleBody = Color(0x70FFFFFF)

    // The walnut base and the table under it.
    val walnutDark = Color(0xFF3B2416)
    val walnutLight = Color(0xFF7A4B2C)
    val brassBand = Color(0xFFC9A15A)
    val tableShadow = Color(0x55000000)
    val caustic = Color(0x66FFE9B0)
}
