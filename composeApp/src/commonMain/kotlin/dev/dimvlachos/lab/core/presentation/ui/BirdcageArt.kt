package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The group info screen round the birdcage, as the prototype styles it: warm paper by day and a
 * dark plum grey by night, with a brass accent. [chip] and [quietBadge] are the "silenced" badge's
 * fill and ink, [line] the card's hairline edge.
 */
internal class BirdcageScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    val quietBadge: Color,
) {
    internal companion object {
        val Light =
            BirdcageScreenColors(
                background = Color(0xFFF4F0E8),
                card = Color(0xFFFFFDF8),
                ink = Color(0xFF24252B),
                muted = Color(0xFF7A736A),
                line = Color(0xFFE0D9CC),
                chip = Color(0x12281E14),
                accent = Color(0xFF94650F),
                quietBadge = Color(0xFF8A8278),
            )
        val Dark =
            BirdcageScreenColors(
                background = Color(0xFF18171A),
                card = Color(0xFF211F23),
                ink = Color(0xFFECE8E1),
                muted = Color(0xFFA8A096),
                line = Color(0xFF34313A),
                chip = Color(0x14FFFFFF),
                accent = Color(0xFFE8BB62),
                quietBadge = Color(0xFF8F877D),
            )

        /** The group's avatar: a green disc, lighter toward its top left. */
        val AvatarFrom = Color(0xFF3C8D6E)
        val AvatarTo = Color(0xFF23614B)
    }
}
