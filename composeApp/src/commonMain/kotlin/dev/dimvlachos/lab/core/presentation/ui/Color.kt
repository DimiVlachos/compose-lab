package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
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
    val bookThread: Color = Color(0xFFEFE6D4),
    val bookCrease: Color = Color(0xFF4F412C),
    val bookShadow: Color = Color(0xFF000000),
    // The hardcover: burgundy leather boards with a tooled gold line, a darker spine, and the paper
    // edges of the page stacks.
    val cover: Color = Color(0xFF5E1F1C),
    val coverLit: Color = Color(0xFF74302A),
    val coverRule: Color = Color(0xFFC9A55C),
    val coverSpine: Color = Color(0xFF3A1311),
    val pageEdge: Color = Color(0xFFEDE3CB),
    val pageEdgeLine: Color = Color(0xFFA8926A),
    // The table the book demo lies on, lit from above its centre, and the scripted finger.
    val table: Color = Color(0xFF2B1F17),
    val tableLit: Color = Color(0xFF4A3727),
    val touch: Color = Color(0xFFFFFFFF),
)
