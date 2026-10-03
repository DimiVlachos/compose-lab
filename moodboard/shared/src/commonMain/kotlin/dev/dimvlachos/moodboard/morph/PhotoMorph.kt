package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * One end of the photo container transform (Android only; iOS uses the system zoom). Both ends pass
 * the same [photoId] and [scope]. The scope (a tab) keeps a photo shown in two tabs' grids from
 * pairing across them on a tab switch. Only the detail end passes an [overlayZIndex], so on a close
 * the card composing underneath never covers the shrinking detail. The ends rest at different radii
 * and each travels to the other's, so their corners agree on every frame.
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
                clipInOverlayDuringTransition = rememberMorphOverlayClip(radius),
            )
            // Read in the layer block so the per-frame radius never recomposes this end.
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(radius.value)
            }
    }
