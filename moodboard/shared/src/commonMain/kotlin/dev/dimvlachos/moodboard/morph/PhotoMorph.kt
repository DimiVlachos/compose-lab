package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * One end of the photo container transform (Android only; iOS uses the system zoom). Both ends pass
 * the same [photoId] and [scope]. The scope (a tab) keeps a photo shown in two tabs' grids from
 * pairing across them on a tab switch. Only the detail end passes an [overlayZIndex], so on a close
 * the card composing underneath never covers the shrinking detail. The ends rest at different radii
 * and each travels to the other's, so their corners agree on every frame. With a [viewport], the
 * morph is cut to the grid's visible area on the grid side, so it slides under the bars instead of
 * over them.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.photoMorph(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    photoId: String,
    scope: String,
    end: MorphEnd,
    overlayZIndex: Float = 0f,
    viewport: MorphViewport? = null,
): Modifier =
    with(sharedTransitionScope) {
        val sharedContentState = rememberSharedContentState(morphKey("$scope/$photoId"))
        val radius =
            rememberMorphCornerRadius(
                animatedVisibilityScope = animatedVisibilityScope,
                sharedContentState = sharedContentState,
                restRadius = morphRestRadius(end),
                counterpartRadius = morphCounterpartRadius(end, paired = true),
            )
        sharedBounds(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = animatedVisibilityScope,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                boundsTransform = MorphBoundsTransform,
                resizeMode = morphResizeMode(remeasure = true),
                zIndexInOverlay = overlayZIndex,
                clipInOverlayDuringTransition =
                    rememberMorphOverlayClip(
                        radius = radius,
                        viewport = viewport,
                        viewportFraction =
                            rememberMorphViewportFraction(
                                animatedVisibilityScope,
                                sharedContentState,
                                end,
                            ),
                    ),
            )
            // Read in the layer block so the per-frame radius never recomposes this end.
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(radius.value)
            }
    }

/**
 * How far the overlay clip is cut to the grid's viewport, paired across the ends like the radius:
 * the card rests cut (it lives in the grid), the detail rests uncut and travels to cut, so on every
 * frame both ends agree. Gated on the match outside the animation, as the radius is.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun rememberMorphViewportFraction(
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedContentState: SharedTransitionScope.SharedContentState,
    end: MorphEnd,
): State<Float> {
    val rest = if (end == MorphEnd.Card) 1f else 0f
    val transition = animatedVisibilityScope.transition
    val animated =
        transition.animateFloat(
            transitionSpec = {
                tween(
                    morphViewportMs(end, targetVisible = targetState == EnterExitState.Visible),
                    easing = MorphDimens.MorphEasing,
                )
            },
            label = "morphViewport",
        ) { state ->
            if (state == EnterExitState.Visible) rest else 1f - rest
        }
    return remember(animated, sharedContentState, rest) {
        derivedStateOf { if (sharedContentState.isMatchFound) animated.value else rest }
    }
}

// The bounds' timing (MorphBoundsTransform): an open is the detail arriving and the card leaving.
internal fun morphViewportMs(end: MorphEnd, targetVisible: Boolean): Int =
    if ((end == MorphEnd.Detail) == targetVisible) MorphDimens.OpenMs else MorphDimens.CloseMs
