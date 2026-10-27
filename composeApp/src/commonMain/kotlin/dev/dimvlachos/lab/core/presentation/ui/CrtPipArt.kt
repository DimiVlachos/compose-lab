package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The cooking-video watch page round the CRT picture-in-picture, as the prototype styles it: warm
 * cream by day and a warm near-black by night, a tomato accent (the set's focus ring). [chip] is
 * the chip fill, [line] the nav bar's hairline, [toast] the snackbar (inverse colours), [toastInk]
 * its text and [toastAccent] its Undo.
 */
internal class CrtPipScreenColors(
    val background: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    val toast: Color,
    val toastInk: Color,
    val toastAccent: Color,
) {
    internal companion object {
        /** The player's slot behind the picture, the same in both themes. */
        val Slot = Color(0xFF0B0B0C)

        /** The user's avatar in the bar, and the channel's. */
        val Avatar = Color(0xFF4F7A5A)
        val ChannelAvatar = Color(0xFF8A4B2A)

        /** The Skillet mark: a cast-iron pan of tomato sauce. */
        val LogoIron = Color(0xFF2B2724)
        val LogoSauce = Color(0xFFE8402A)

        /** The commenters' avatars, in the order they comment. */
        val CommentAvatars = listOf(Color(0xFF7A5BB0), Color(0xFF3D7F8F), Color(0xFFB0663B))

        /** The progress track's red. */
        val Progress = Color(0xFFE8402A)

        val Light =
            CrtPipScreenColors(
                background = Color(0xFFFAF7F2),
                ink = Color(0xFF1F1D1B),
                muted = Color(0xFF6F6860),
                line = Color(0xFFE7E0D6),
                chip = Color(40, 30, 20, 17),
                accent = Color(0xFFD2432C),
                toast = Color(0xFF2A2623),
                toastInk = Color(0xFFF6F1EA),
                toastAccent = Color(0xFFFF9A80),
            )
        val Dark =
            CrtPipScreenColors(
                background = Color(0xFF151413),
                ink = Color(0xFFEFE9E2),
                muted = Color(0xFFA59D93),
                line = Color(0xFF322F2B),
                chip = Color(255, 255, 255, 19),
                accent = Color(0xFFFF7A62),
                toast = Color(0xFFEFE9E2),
                toastInk = Color(0xFF1F1D1B),
                toastAccent = Color(0xFFB3341F),
            )
    }
}
