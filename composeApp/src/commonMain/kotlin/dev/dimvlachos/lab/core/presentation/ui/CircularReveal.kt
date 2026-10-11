package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.hypot
import kotlin.math.max

/**
 * How far it is from [from] to the farthest corner of a box [size] big: how big a circle spreading
 * from there must grow to cover all of it.
 */
internal fun farthestCorner(from: Offset, size: Size): Float =
    max(
        max(hypot(from.x, from.y), hypot(size.width - from.x, from.y)),
        max(hypot(from.x, size.height - from.y), hypot(size.width - from.x, size.height - from.y)),
    )

/**
 * Draws [content] clipped to a circle spreading from [from], [progress] of the way to the farthest
 * corner: [inside] it, or outside it. [circle] is the path the circle is made in, kept by the
 * caller so nothing is made anew every frame.
 */
internal fun DrawScope.clipToReveal(
    circle: Path,
    from: Offset,
    progress: Float,
    inside: Boolean,
    content: DrawScope.() -> Unit,
) {
    val radius = farthestCorner(from, size) * progress
    circle.rewind()
    circle.addOval(Rect(from, radius))
    clipPath(circle, if (inside) ClipOp.Intersect else ClipOp.Difference) { content() }
}
