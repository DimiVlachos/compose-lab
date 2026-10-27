package dev.dimvlachos.lab.coinsorterdemo

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinItem
import dev.dimvlachos.lab.core.presentation.components.coinsorter.Metal

/** What a result's icon shows. */
internal enum class Glyph {
    Lantern,
    Oil,
    String,
    Head,
    Puck,
}

/**
 * A search result: its [name], price in [cents], [rating] and [reviews], its icon's [glyph] in
 * [ink] on [tint], and the [metal] of its coin.
 */
internal class Lantern(
    val name: String,
    val cents: Int,
    val rating: Double,
    val reviews: String,
    val glyph: Glyph,
    val tint: Color,
    val ink: Color,
    val metal: Metal,
) {
    val coin = CoinItem(cents, rating, metal)
}

/**
 * The prototype's eight results for "lantern" at an invented outfitter, in its best-match order.
 */
internal val LanternResults =
    listOf(
        Lantern(
            "Trailhead 400 LED Lantern",
            3499,
            4.7,
            "2,140",
            Glyph.Lantern,
            Color(0xFFE6F0E1),
            Color(0xFF3F6B3A),
            Metal.Nickel,
        ),
        Lantern(
            "Copper Creek Hurricane Lamp",
            5800,
            4.8,
            "612",
            Glyph.Oil,
            Color(0xFFF6E6D8),
            Color(0xFF9A5426),
            Metal.Copper,
        ),
        Lantern(
            "Pocket Glow Mini, 2-pack",
            1250,
            4.4,
            "3,305",
            Glyph.Lantern,
            Color(0xFFFDF0C8),
            Color(0xFFA07612),
            Metal.Brass,
        ),
        Lantern(
            "Basecamp 1000 Rechargeable",
            7995,
            4.6,
            "988",
            Glyph.Lantern,
            Color(0xFFE2EBF5),
            Color(0xFF2F5785),
            Metal.Nickel,
        ),
        Lantern(
            "Firefly String Lights, 20 ft",
            2199,
            4.5,
            "1,472",
            Glyph.String,
            Color(0xFFFBECDC),
            Color(0xFFB0641C),
            Metal.Brass,
        ),
        Lantern(
            "Old Mill Oil Lantern",
            4400,
            4.9,
            "356",
            Glyph.Oil,
            Color(0xFFECE5DD),
            Color(0xFF5A4636),
            Metal.Copper,
        ),
        Lantern(
            "Solar Puck Lantern",
            2749,
            4.2,
            "801",
            Glyph.Puck,
            Color(0xFFE2F2EE),
            Color(0xFF2D7266),
            Metal.Nickel,
        ),
        Lantern(
            "Summit Pro Headlamp Kit",
            9600,
            4.3,
            "214",
            Glyph.Head,
            Color(0xFFF1E3ED),
            Color(0xFF8A3B6E),
            Metal.Brass,
        ),
    )
