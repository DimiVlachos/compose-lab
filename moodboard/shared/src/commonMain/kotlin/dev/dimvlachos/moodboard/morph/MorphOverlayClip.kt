package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

// The stock OverlayClip takes a fixed shape. This one asks for the radius on every frame, so the
// overlay's clip can follow the paired radius while the bounds animate, and cuts the photo to
// [viewport] (the grid's visible area) so it never paints over the bars.
@OptIn(ExperimentalSharedTransitionApi::class)
internal class RoundedOverlayClip(
    private val radius: () -> Dp,
    private val viewport: (bounds: Rect) -> Rect? = { null },
) : SharedTransitionScope.OverlayClip {
    private val path = Path()
    private val cut = Path()

    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path = clipPath(bounds, density)

    internal fun clipPath(bounds: Rect, density: Density): Path {
        path.reset()
        path.addRoundRect(RoundRect(bounds, CornerRadius(with(density) { radius().toPx() })))
        viewport(bounds)?.let {
            cut.reset()
            cut.addRect(it)
            path.op(path, cut, PathOperation.Intersect)
        }
        return path
    }
}

// The clip is remembered once and read through rememberUpdatedState, never through a lambda that
// closes over a local: remember does not re-run its calculation, so a captured local would pin the
// clip to its first frame and it could never animate.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberMorphOverlayClip(
    radius: State<Dp>,
    viewport: MorphViewport? = null,
    viewportFraction: State<Float>? = null,
): SharedTransitionScope.OverlayClip {
    val current = rememberUpdatedState(radius)
    val currentViewport = rememberUpdatedState(viewport)
    val currentFraction = rememberUpdatedState(viewportFraction)
    return remember {
        RoundedOverlayClip({ current.value.value }) { bounds ->
            val area = currentViewport.value ?: return@RoundedOverlayClip null
            val fraction = currentFraction.value?.value ?: 0f
            viewportClipRect(bounds, area.top, area.bottom, fraction)
        }
    }
}
