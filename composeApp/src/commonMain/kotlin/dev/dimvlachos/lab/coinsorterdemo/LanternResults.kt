package dev.dimvlachos.lab.coinsorterdemo

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinItem
import dev.dimvlachos.lab.core.presentation.components.coinsorter.Metal
import dev.dimvlachos.lab.core.presentation.ui.CoinSorterTints

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
            CoinSorterTints[0].first,
            CoinSorterTints[0].second,
            Metal.Nickel,
        ),
        Lantern(
            "Copper Creek Hurricane Lamp",
            5800,
            4.8,
            "612",
            Glyph.Oil,
            CoinSorterTints[1].first,
            CoinSorterTints[1].second,
            Metal.Copper,
        ),
        Lantern(
            "Pocket Glow Mini, 2-pack",
            1250,
            4.4,
            "3,305",
            Glyph.Lantern,
            CoinSorterTints[2].first,
            CoinSorterTints[2].second,
            Metal.Brass,
        ),
        Lantern(
            "Basecamp 1000 Rechargeable",
            7995,
            4.6,
            "988",
            Glyph.Lantern,
            CoinSorterTints[3].first,
            CoinSorterTints[3].second,
            Metal.Nickel,
        ),
        Lantern(
            "Firefly String Lights, 20 ft",
            2199,
            4.5,
            "1,472",
            Glyph.String,
            CoinSorterTints[4].first,
            CoinSorterTints[4].second,
            Metal.Brass,
        ),
        Lantern(
            "Old Mill Oil Lantern",
            4400,
            4.9,
            "356",
            Glyph.Oil,
            CoinSorterTints[5].first,
            CoinSorterTints[5].second,
            Metal.Copper,
        ),
        Lantern(
            "Solar Puck Lantern",
            2749,
            4.2,
            "801",
            Glyph.Puck,
            CoinSorterTints[6].first,
            CoinSorterTints[6].second,
            Metal.Nickel,
        ),
        Lantern(
            "Summit Pro Headlamp Kit",
            9600,
            4.3,
            "214",
            Glyph.Head,
            CoinSorterTints[7].first,
            CoinSorterTints[7].second,
            Metal.Brass,
        ),
    )
