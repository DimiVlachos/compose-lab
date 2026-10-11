package dev.dimvlachos.lab.core.presentation.components.imagemorph

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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// The stock OverlayClip takes a fixed shape. This one asks for the radius on every frame, so the
// overlay's clip can follow the paired radius while the bounds animate. An [outset] widens it all
// round, by as much as a shadow on the element reaches, so the shadow travels with the morph
// instead of being cut off until it lands; the element's own clip still holds its content.
@OptIn(ExperimentalSharedTransitionApi::class)
internal class RoundedOverlayClip(
    private val outset: Dp = 0.dp,
    private val radius: () -> Dp,
) : SharedTransitionScope.OverlayClip {
    private val path = Path()

    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path = clipPath(bounds, density)

    internal fun clipPath(bounds: Rect, density: Density): Path {
        path.reset()
        val out = with(density) { outset.toPx() }
        val corner = with(density) { radius().toPx() } + out
        path.addRoundRect(RoundRect(bounds.inflate(out), CornerRadius(corner)))
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
    outset: Dp = 0.dp,
): SharedTransitionScope.OverlayClip {
    val current = rememberUpdatedState(radius)
    return remember(outset) { RoundedOverlayClip(outset) { current.value.value } }
}
