package dev.dimvlachos.moodboard.morph

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp
import kotlin.math.max

/**
 * Where a grid's photos can be seen: below its top bar and above its bottom bar, in the
 * SharedTransitionLayout's coordinates. The shared-element overlay draws above every bar, so a
 * photo morphing to or from a cell the bars half cover would paint over them until it settles; the
 * morph clips to this instead (Builder Brigade's top-bar clip, at both edges).
 */
@Stable
class MorphViewport {
    // The grid's frame, and its opaque bars' heights, kept apart: the frame only moves on layout,
    // while a large top bar's height changes as it collapses. The heights are read when the clip
    // is drawn, never stored, so they are always current without recomposing the grid.
    var frameTop by mutableFloatStateOf(Float.NaN)
    var frameBottom by mutableFloatStateOf(Float.NaN)
    var insetTop: () -> Float = { 0f }
    var insetBottom: () -> Float = { 0f }

    internal val top: Float
        get() = frameTop + insetTop()

    internal val bottom: Float
        get() = frameBottom - insetBottom()
}

/** One viewport per morph scope (a tab), so each detail clips to its own grid. */
class MorphViewports {
    /** The SharedTransitionLayout's top in the window, to turn window positions into its own. */
    var layoutTopInWindow by mutableFloatStateOf(0f)
    private val byScope = mutableMapOf<String, MorphViewport>()

    fun of(scope: String): MorphViewport = byScope.getOrPut(scope) { MorphViewport() }
}

/**
 * The part of [bounds] inside the viewport from [top] to [bottom], with the cut [fraction] of the
 * way from the photo's own edges: a full-screen end starts uncut instead of losing its strips over
 * the bars on the first frame. Null when nothing needs cutting.
 */
internal fun viewportClipRect(bounds: Rect, top: Float, bottom: Float, fraction: Float): Rect? {
    if (fraction <= 0f || top.isNaN() || bottom.isNaN()) return null
    val clipTop = if (top > bounds.top) lerp(bounds.top, top, fraction) else bounds.top
    val clipBottom =
        if (bottom < bounds.bottom) lerp(bounds.bottom, bottom, fraction) else bounds.bottom
    if (clipTop == bounds.top && clipBottom == bounds.bottom) return null
    return Rect(bounds.left, clipTop, bounds.right, max(clipTop, clipBottom))
}
