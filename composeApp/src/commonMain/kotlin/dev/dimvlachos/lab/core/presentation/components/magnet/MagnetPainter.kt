package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import kotlin.math.min

/**
 * Where a magnet's [count] photos go when they fan out: a grid of up to three columns, centred on a
 * [width] × [height] table, its last row centred too. The cards grow to at most FanScale times
 * their size, less if that wouldn't fit, and keep [gap] apart and from the edges.
 */
internal class FanGrid(
    private val count: Int,
    private val width: Float,
    private val height: Float,
    private val cardWidth: Float,
    private val cardHeight: Float,
    private val gap: Float,
) {
    private val columns = count.coerceIn(1, MagnetDimens.FanColumns)
    private val rows = ((count + columns - 1) / columns).coerceAtLeast(1)

    val scale: Float =
        min(
                MagnetDimens.FanScale,
                min(
                    (width - gap * (columns + 1)) / (columns * cardWidth),
                    (height - gap * (rows + 1)) / (rows * cardHeight),
                ),
            )
            .coerceAtLeast(0.1f)

    /** The middle of the [index]th card's cell. */
    fun cell(index: Int): Offset {
        val w = cardWidth * scale
        val h = cardHeight * scale
        val row = index / columns
        val column = index % columns
        val inRow = if (row == rows - 1) count - row * columns else columns
        val rowWidth = inRow * w + (inRow - 1) * gap
        val gridHeight = rows * h + (rows - 1) * gap
        return Offset(
            (width - rowWidth) / 2f + w / 2f + column * (w + gap),
            (height - gridHeight) / 2f + h / 2f + row * (h + gap),
        )
    }
}
