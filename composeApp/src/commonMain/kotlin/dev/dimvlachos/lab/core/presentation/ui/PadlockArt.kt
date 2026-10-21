package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The sign-up screen round the padlock, as the prototype styles it: warm paper by day and a dark
 * plum grey by night, with a brass accent for the focused field and green for what's met. [chip] is
 * the step pill, [line] the fields' hairline edge.
 */
internal class PadlockScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    val ok: Color,
) {
    internal companion object {
        val Light =
            PadlockScreenColors(
                background = Color(0xFFF3EFE8),
                card = Color(0xFFFFFDF9),
                ink = Color(0xFF23252C),
                muted = Color(0xFF77706A),
                line = Color(0xFFDDD6CB),
                chip = Color(0x12281E14),
                accent = Color(0xFF8A5D0C),
                ok = Color(0xFF2F7A4A),
            )
        val Dark =
            PadlockScreenColors(
                background = Color(0xFF17161A),
                card = Color(0xFF201F24),
                ink = Color(0xFFECE8E1),
                muted = Color(0xFFA8A096),
                line = Color(0xFF34313A),
                chip = Color(0x14FFFFFF),
                accent = Color(0xFFE8BB62),
                ok = Color(0xFF6CC28E),
            )
    }
}
