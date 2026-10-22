package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The help screen round the paper fans, as the prototype styles it: warm paper by day and a dark
 * grey by night. [card] is the questions' card and [line] its hairlines; [chip] is the chat
 * button's fill.
 */
internal class PaperFanScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
) {
    internal companion object {
        val Light =
            PaperFanScreenColors(
                background = Color(0xFFF2EFE9),
                card = Color(0xFFFFFDF9),
                ink = Color(0xFF23252C),
                muted = Color(0xFF77706A),
                line = Color(0xFFE3DDD3),
                chip = Color(0x12281E14),
            )
        val Dark =
            PaperFanScreenColors(
                background = Color(0xFF16161A),
                card = Color(0xFF1F1F24),
                ink = Color(0xFFECE8E1),
                muted = Color(0xFFA8A096),
                line = Color(0xFF2F2E35),
                chip = Color(0x14FFFFFF),
            )
    }
}
