package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The lab's colours: a dark scheme with a violet accent, plus the warm paper tones of the book and
 * the chat.
 *
 * @property background Behind every screen.
 * @property surface Cards and dialogs.
 * @property bar The nav bar and the search bar.
 * @property textPrimary Text and icons.
 * @property textMuted Secondary text and unselected icons.
 * @property accent The selected tab, buttons and your own chat bubbles.
 * @property accentSoft The accent, faint: the nav bar's indicator.
 * @property onAccent Text and icons on the accent.
 */
@Immutable
public data class AppColors(
    val background: Color = Color(0xFF0B0D12),
    val surface: Color = Color(0xFF171A22),
    val bar: Color = Color(0xFF232733),
    val textPrimary: Color = Color(0xFFE8EAF0),
    val textMuted: Color = Color(0xFF8A90A0),
    val accent: Color = Color(0xFF7C5CFF),
    val accentSoft: Color = Color(0x337C5CFF),
    val onAccent: Color = Color(0xFFFFFFFF),
    // The open book: the shade across a lifted leaf, the sheen on it, the shadow in the gutter,
    // the binding thread and the crease down the spine, all warm like paper under a lamp.
    val pageShade: Color = Color(0xFF2E2416),
    val pageGlare: Color = Color(0xFFFFF8EC),
    val bookGutter: Color = Color(0xFF2B2014),
    val bookCrease: Color = Color(0xFF4F412C),
    // The paper edges of the sheets under the pages.
    // The binding thread in the centre fold: unbleached linen, and the darker grain of its twist.
    val thread: Color = Color(0xFFE6DAC0),
    val threadTwist: Color = Color(0xFF9A8662),
    val pageEdge: Color = Color(0xFFEDE3CB),
    val pageEdgeLine: Color = Color(0xFFA8926A),
    // The table the book demo lies on, lit from above its centre, and the scripted finger.
    val table: Color = Color(0xFF2B1F17),
    val tableLit: Color = Color(0xFF4A3727),
    val touch: Color = Color(0xFFFFFFFF),
    // The chat: the bubbles of the messages that come in, and the writing paper its planes are
    // folded from.
    val bubbleTheirs: Color = Color(0xFF262A36),
    val paper: Color = Color(0xFFF3F1EC),
    // The pull-cord lamp: its green enamel shade, the brass of its rod, rim and bead, the cotton
    // cord, the bulb dark and lit, and the warm light it throws. It hangs over a light screen and
    // a dark one alike, so it reads on both.
    val lampShade: Color = Color(0xFF2E4A3E),
    val lampShadeSheen: Color = Color(0xFF5F8273),
    val lampBrass: Color = Color(0xFFB8945A),
    val lampCord: Color = Color(0xFF8E8676),
    val bulbOff: Color = Color(0xFFD9D3C4),
    val bulbLit: Color = Color(0xFFFFF6DC),
    val lampLight: Color = Color(0xFFFFD592),
)
