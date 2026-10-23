package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The streaming app's home screen round the key rack, as the prototype styles it: warm plaster by
 * day and a dark plum grey by night, with a brass accent. [line] is the cards' hairline edge.
 */
internal class KeyHookScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
) {
    internal companion object {
        /** The progress bar along a show's thumbnail. */
        val Progress = Color(0xFFFFCF6A)

        /** An avatar's colour, given as the prototype's `#rrggbb`. */
        fun avatar(hex: String) = Color(0xFF000000 or hex.removePrefix("#").toLong(16))

        /** A thumbnail's gradient colour, given as `0xAARRGGBB`. */
        fun thumb(argb: Long) = Color(argb)

        val Light =
            KeyHookScreenColors(
                background = Color(0xFFF2EEE7),
                card = Color(0xFFFFFDF9),
                ink = Color(0xFF23252C),
                muted = Color(0xFF77706A),
                line = Color(0xFFDDD6CB),
                accent = Color(0xFF8A5D0C),
            )
        val Dark =
            KeyHookScreenColors(
                background = Color(0xFF17161A),
                card = Color(0xFF222126),
                ink = Color(0xFFECE8E1),
                muted = Color(0xFFA8A096),
                line = Color(0xFF34313A),
                accent = Color(0xFFE8BB62),
            )
    }
}
