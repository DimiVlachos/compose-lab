package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The neighborhood app round the mailbox, as the prototype styles it: warm paper by day and a warm
 * dark by night, with a brick-red accent. [card] and [line] are the feed's cards and hairlines,
 * [chip] the pill buttons and an opened message's ground.
 */
internal class MailboxScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
) {
    internal companion object {
        val Light =
            MailboxScreenColors(
                background = Color(0xFFFAF7F2),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF1F1C19),
                muted = Color(0xFF71695F),
                line = Color(0xFFECE5DA),
                chip = Color(0x0F281E14),
                accent = Color(0xFFC2412D),
            )
        val Dark =
            MailboxScreenColors(
                background = Color(0xFF161412),
                card = Color(0xFF201D1A),
                ink = Color(0xFFF1ECE6),
                muted = Color(0xFFA69D92),
                line = Color(0xFF2E2A26),
                chip = Color(0x12FFFFFF),
                accent = Color(0xFFEF6A52),
            )
    }
}

/** The people in the neighborhood feed: the user's avatar gradient and the neighbors' faces. */
internal object MailboxFaces {
    val avatarTop = Color(0xFFE9A46B)
    val avatarBottom = Color(0xFFC2573A)
    val initials = Color(0xFFFFFFFF)
    val neighbors =
        listOf(Color(0xFFC2573A), Color(0xFF4F7A5A), Color(0xFF5A6FA3), Color(0xFFA3825A))
}
