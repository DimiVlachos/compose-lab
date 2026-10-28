package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The bakery's to-do list round the grave, as the prototype's `--app-*` colours: warm paper by day
 * and a plum-black list by night. [row] is a task's face, [line] the hairlines between rows,
 * [line2] an unticked check's ring, [accent] the bakery's orange (a ticked check, the dot, the
 * empty list's button), and [soil] what shows under a dragged row. The grave itself is the same
 * dusk graveyard in both.
 */
internal class GraveUndoScreenColors(
    val background: Color,
    val row: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val line2: Color,
    val accent: Color,
    val soil: Color,
    val soilInk: Color,
) {
    internal companion object {
        val Light =
            GraveUndoScreenColors(
                background = Color(0xFFF5F1EA),
                row = Color(0xFFFFFDF8),
                ink = Color(0xFF221D1A),
                muted = Color(0xFF736B64),
                line = Color(0xFFE7DFD4),
                line2 = Color(0xFFC9BFB3),
                accent = Color(0xFFD46F1F),
                soil = Color(0xFF4B3527),
                soilInk = Color(0xFFF4E9DC),
            )
        val Dark =
            GraveUndoScreenColors(
                background = Color(0xFF121014),
                row = Color(0xFF1B191E),
                ink = Color(0xFFEFE9F1),
                muted = Color(0xFF9F98A6),
                line = Color(0xFF2B2730),
                line2 = Color(0xFF4A4450),
                accent = Color(0xFFF0913F),
                soil = Color(0xFF3A281E),
                soilInk = Color(0xFFF0E2D2),
            )
    }
}
