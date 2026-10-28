package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The farm's "Tonight" screen round the jack-o'-lantern, as the prototype styles it (`.app.day` and
 * `.app.night`): warm paper by day, plum dark by candlelight. The hero card's landscape takes
 * [sky1] to [sky2], [hill1], [hill2], the [barn] and its [barnTrim], the sun or moon [orb] and
 * [stars] (how visible). [shadow] and [shadowBlur] are the cards' `box-shadow`.
 */
internal class JackOLanternScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val tile: Color,
    val tileInk: Color,
    val sky1: Color,
    val sky2: Color,
    val hill1: Color,
    val hill2: Color,
    val barn: Color,
    val barnTrim: Color,
    val orb: Color,
    val stars: Float,
    val tabBar: Color,
    val chip: Color,
    val shadow: Color,
    val shadowBlur: Float,
) {
    internal companion object {
        val Day =
            JackOLanternScreenColors(
                background = Color(0xFFF6F1E8),
                card = Color(0xFFFFFDF9),
                ink = Color(0xFF231C15),
                muted = Color(0xFF7A6E62),
                line = Color(0xFFE8DFD2),
                accent = Color(0xFFC4561A),
                onAccent = Color(0xFFFFFFFF),
                tile = Color(0xFFF4E7D5),
                tileInk = Color(0xFFA24C16),
                sky1 = Color(0xFFA9D4EE),
                sky2 = Color(0xFFF7E2BD),
                hill1 = Color(0xFFC7AE72),
                hill2 = Color(0xFF9F8248),
                barn = Color(0xFFB8452C),
                barnTrim = Color(0xFFFFF4E4),
                orb = Color(0xFFF8BD57),
                stars = 0f,
                tabBar = Color(255, 253, 249, (.94f * 255).toInt()),
                chip = Color(40, 25, 10, (.06f * 255).toInt()),
                shadow = Color(70, 40, 10, (.35f * 255).toInt()),
                shadowBlur = 16f,
            )
        val Night =
            JackOLanternScreenColors(
                background = Color(0xFF15111B),
                card = Color(0xFF1E1826),
                ink = Color(0xFFF2E9DD),
                muted = Color(0xFFA99B8D),
                line = Color(0xFF2D2536),
                accent = Color(0xFFF08A3C),
                onAccent = Color(0xFF1E0D03),
                tile = Color(0xFF2A1F2C),
                tileInk = Color(0xFFF6A65E),
                sky1 = Color(0xFF0C1230),
                sky2 = Color(0xFF3A2347),
                hill1 = Color(0xFF1C1626),
                hill2 = Color(0xFF110D19),
                barn = Color(0xFF2B1A24),
                barnTrim = Color(0xFFF3A14F),
                orb = Color(0xFFF4EAD2),
                stars = 1f,
                tabBar = Color(25, 20, 32, (.94f * 255).toInt()),
                chip = Color(255, 255, 255, (.07f * 255).toInt()),
                shadow = Color(0, 0, 0, (.7f * 255).toInt()),
                shadowBlur = 18f,
            )

        /** The little pumpkins in the hero's field, the same by day and night. */
        val FieldPumpkin = Color(0xFFE8781F)

        /** The stars, `#fff5dc`. */
        val Star = Color(0xFFFFF5DC)

        /** The hint chip: `rgba(34, 22, 12, .8)` with `#fff3e6` text. */
        val Hint = Color(34, 22, 12, (.8f * 255).toInt())
        val HintText = Color(0xFFFFF3E6)
    }
}
