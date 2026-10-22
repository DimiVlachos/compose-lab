package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The reminders screen round the sticky notes, as the prototype styles it: warm paper by day and a
 * dark warm grey by night. [chip] is the count's pill and [accent] the keyboard focus ring.
 */
internal class StickyNoteScreenColors(
    val background: Color,
    val ink: Color,
    val muted: Color,
    val chip: Color,
    val accent: Color,
) {
    internal companion object {
        val Light =
            StickyNoteScreenColors(
                background = Color(0xFFF3EFE8),
                ink = Color(0xFF2A2620),
                muted = Color(0xFF7A7166),
                chip = Color(0x123C280A),
                accent = Color(0xFFB8641C),
            )
        val Dark =
            StickyNoteScreenColors(
                background = Color(0xFF1A1917),
                ink = Color(0xFFECE6DC),
                muted = Color(0xFFA59C8F),
                chip = Color(0x14FFFFFF),
                accent = Color(0xFFF0A456),
            )
    }
}
