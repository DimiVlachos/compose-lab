package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The neighborhood chat round the decoder ring, as the prototype styles it: warm paper by day and a
 * soft charcoal by night, with a pine green accent. [mine] is your own bubble, [chip] the day's.
 */
internal class DecoderRingScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    val mine: Color,
) {
    internal companion object {
        val Light =
            DecoderRingScreenColors(
                background = Color(0xFFF3F1EC),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF22242A),
                muted = Color(0xFF767069),
                line = Color(0xFFE3DDD2),
                chip = Color(0x0F281E14),
                accent = Color(0xFF2F6B4F),
                mine = Color(0xFF2F6B4F),
            )
        val Dark =
            DecoderRingScreenColors(
                background = Color(0xFF17181A),
                card = Color(0xFF232428),
                ink = Color(0xFFECE9E3),
                muted = Color(0xFFA29C93),
                line = Color(0xFF34343A),
                chip = Color(0x12FFFFFF),
                accent = Color(0xFF7CC4A0),
                mine = Color(0xFF2C5D46),
            )

        /** The group's avatar: amber, lighter toward its top left. */
        val GroupFrom = Color(0xFFE9B65D)
        val GroupTo = Color(0xFFC7782F)

        /** The neighbors' avatars: Dave, Lucía and Marcy. */
        val Dave = Color(0xFF4D7AB8)
        val Lucia = Color(0xFFB8654D)
        val Marcy = Color(0xFF7A5BB0)
    }
}
