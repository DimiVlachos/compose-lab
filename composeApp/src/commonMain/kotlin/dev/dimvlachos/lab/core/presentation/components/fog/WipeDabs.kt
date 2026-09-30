package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.ui.geometry.Offset

/**
 * Where the soft brush lands along a stroke of pixel positions: one dab every [spacing] px of path.
 * The distance carries from one point to the next, so a slow drag, with its many close pointer
 * events, erases no faster than a quick one.
 */
internal fun wipeDabs(stroke: List<Offset>, spacing: Float): List<Offset> {
    if (stroke.isEmpty()) return emptyList()
    val dabs = mutableListOf(stroke.first())
    // How far along the current segment the next dab is due.
    var due = spacing
    for (i in 1 until stroke.size) {
        val from = stroke[i - 1]
        val segment = stroke[i] - from
        val length = segment.getDistance()
        if (length == 0f) continue
        while (due <= length) {
            dabs += from + segment * (due / length)
            due += spacing
        }
        due -= length
    }
    return dabs
}
