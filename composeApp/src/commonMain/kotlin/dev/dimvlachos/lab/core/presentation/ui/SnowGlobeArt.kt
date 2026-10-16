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

/**
 * The weather screen round the snow globe, as the prototype styles it: its own warm paper by day
 * and plum by night, with an amber accent for a fresh forecast. [card] is the day strip, [chip] the
 * hint, [selected] the chosen day, and [cardLine] their hairline edge.
 */
internal class SnowGlobeScreenColors(
    val background: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val card: Color,
    val cardLine: Color,
    val selected: Color,
    val chip: Color,
) {
    internal companion object {
        val Light =
            SnowGlobeScreenColors(
                background = Color(0xFFF4E6D6),
                ink = Color(0xFF231D1A),
                muted = Color(0xFF6F655E),
                accent = Color(0xFFA8641C),
                card = Color(0x9EFFFCF7),
                cardLine = Color(0x143C2814),
                selected = Color(0x143C2814),
                chip = Color(0x94FFFCF7),
            )
        val Dark =
            SnowGlobeScreenColors(
                background = Color(0xFF1C1720),
                ink = Color(0xFFF3EEE9),
                muted = Color(0xFFA9A0A6),
                accent = Color(0xFFF0B064),
                card = Color(0x99241E26),
                cardLine = Color(0x14FFF0DC),
                selected = Color(0x1AFFF0DC),
                chip = Color(0x8C241E26),
            )
    }
}

/** The day cards' weather icons, in the prototype's colours. */
internal object SnowGlobeIconColors {
    val sun = Color(0xFFF2A53A)
    val wind = Color(0xFF5B8FC7)
    val cloud = Color(0xFFA8B5C7)
    val rain = Color(0xFF4F8FD8)
}

/**
 * The room the globe stands in for one weather: the wall's gradient from the top down to the
 * horizon ([wall0] to [wall1]) and the table's from there to the bottom ([floor0] to [floor1]). The
 * prototype's ROOM, light and dark. The glass reflects [wall1] above and [floor1] below.
 */
internal class SnowGlobeRoom(
    val wall0: Color,
    val wall1: Color,
    val floor0: Color,
    val floor1: Color,
) {
    internal companion object {
        val LightSunny =
            SnowGlobeRoom(
                Color(0xFFEFD6BB),
                Color(0xFFF6E6D4),
                Color(0xFFF1DFCA),
                Color(0xFFE2C7AA),
            )
        val LightWindy =
            SnowGlobeRoom(
                Color(0xFFD3DFEA),
                Color(0xFFE9EFF5),
                Color(0xFFE5EBF1),
                Color(0xFFCCD6E1),
            )
        val LightShower =
            SnowGlobeRoom(
                Color(0xFFC9D0D9),
                Color(0xFFE1E4E9),
                Color(0xFFDDE0E5),
                Color(0xFFC4C9D1),
            )
        val DarkSunny =
            SnowGlobeRoom(
                Color(0xFF16121B),
                Color(0xFF261D26),
                Color(0xFF2C2229),
                Color(0xFF18131A),
            )
        val DarkWindy =
            SnowGlobeRoom(
                Color(0xFF0F151F),
                Color(0xFF18212D),
                Color(0xFF1D2531),
                Color(0xFF11161E),
            )
        val DarkShower =
            SnowGlobeRoom(
                Color(0xFF111419),
                Color(0xFF1B1F27),
                Color(0xFF20232B),
                Color(0xFF121419),
            )
    }
}
