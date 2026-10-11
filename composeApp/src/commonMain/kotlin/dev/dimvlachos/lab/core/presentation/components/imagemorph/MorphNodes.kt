package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Both ends share one shape (a circle, a pill), so there is no radius to pair. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.morphShapedBounds(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    key: String,
    shape: Shape,
    overlayZIndex: Float = 0f,
): Modifier =
    with(sharedTransitionScope) {
        val sharedContentState = rememberSharedContentState(key)
        val overlayClip = remember(shape) { OverlayClip(shape) }
        sharedBounds(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = animatedVisibilityScope,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                boundsTransform = MorphBoundsTransform,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                zIndexInOverlay = overlayZIndex,
                clipInOverlayDuringTransition = overlayClip,
            )
            .clip(shape)
    }

/** The ends rest at different radii, paired so each travels to the other's. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.morphRadiusBounds(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    key: String,
    restRadius: Dp,
    counterpartRadius: Dp,
    preMorphRadius: Dp = restRadius,
    remeasure: Boolean = true,
    paired: Boolean = true,
    overlayZIndex: Float = 0f,
    shadowElevation: Dp = 0.dp,
): Modifier =
    with(sharedTransitionScope) {
        val sharedContentState = rememberSharedContentState(key)
        val radius =
            rememberMorphCornerRadius(
                animatedVisibilityScope = animatedVisibilityScope,
                sharedContentState = sharedContentState,
                restRadius = restRadius,
                counterpartRadius = if (paired) counterpartRadius else restRadius,
                preMorphRadius = preMorphRadius,
            )
        // paired is constant for the lifetime of a call site, so the branch never flips.
        val overlayClip =
            if (paired) rememberMorphOverlayClip(radius, shadowReach(shadowElevation))
            else remember(restRadius) { OverlayClip(RoundedCornerShape(restRadius)) }
        sharedBounds(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = animatedVisibilityScope,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                boundsTransform = MorphBoundsTransform,
                resizeMode = morphResizeMode(remeasure),
                zIndexInOverlay = overlayZIndex,
                clipInOverlayDuringTransition = overlayClip,
            )
            // The element's own clip, read in the layer block: Modifier.clip(shape) takes a value,
            // so a radius read there would recompose this end on every frame of the morph. The
            // shadow follows the same shape, and travels with it: the overlay's clip leaves room
            // for it. Only the end that is staying casts it while the two are drawn over each
            // other, so it never doubles up; the leaving end hands its shadow over on the first
            // frame, at the same bounds, and the shadow carries on unbroken.
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(radius.value)
                if (shadowElevation > 0.dp) {
                    val leaving =
                        animatedVisibilityScope.transition.targetState == EnterExitState.PostExit
                    this.shadowElevation = morphShadowElevation(shadowElevation, leaving).toPx()
                }
            }
    }

// The node wrappers keep the transition's composition reads in a scope of their own; see
// MorphNode. The fill is drawn inside the node, after its clip: a background in the caller's
// modifier would sit outside the node and paint the slot left behind, not the morphing shape.
@Composable
internal fun ShapedMorphNode(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    key: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    overlayZIndex: Float = 0f,
    color: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .morphShapedBounds(
                sharedTransitionScope,
                animatedVisibilityScope,
                key,
                shape,
                overlayZIndex,
            )
            .background(color),
        content = content,
    )
}

@Composable
internal fun RadiusMorphNode(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    key: String,
    restRadius: Dp,
    counterpartRadius: Dp,
    modifier: Modifier = Modifier,
    preMorphRadius: Dp = restRadius,
    overlayZIndex: Float = 0f,
    color: Color = Color.Transparent,
    shadowElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .morphRadiusBounds(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                key = key,
                restRadius = restRadius,
                counterpartRadius = counterpartRadius,
                preMorphRadius = preMorphRadius,
                overlayZIndex = overlayZIndex,
                shadowElevation = shadowElevation,
            )
            .background(color),
        content = content,
    )
}

/** The shadow a morphing end casts: none while it is leaving, its own [elevation] otherwise. */
internal fun morphShadowElevation(elevation: Dp, leaving: Boolean): Dp =
    if (leaving) 0.dp else elevation

// How far past the element a shadow of [elevation] reaches, blur and drop together.
internal fun shadowReach(elevation: Dp): Dp = elevation * 3
