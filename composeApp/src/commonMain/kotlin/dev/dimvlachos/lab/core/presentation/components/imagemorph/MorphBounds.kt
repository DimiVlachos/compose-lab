package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * One end of the morph: the shared bounds plus the clip that end draws with. Both ends call this
 * with the same [key] and [layers], and only the detail end ever passes an [overlayZIndex]: both
 * ends draw during a transition and tie at the default z, where insertion order wins, so on a close
 * the freshly composed card would cover the shrinking detail.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.morphBounds(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    key: String,
    end: MorphEnd,
    layers: MorphLayers,
    preMorphRadius: Dp = morphRestRadius(end),
    overlayZIndex: Float = 0f,
): Modifier =
    with(sharedTransitionScope) {
        val sharedContentState = rememberSharedContentState(key)
        val restRadius = morphRestRadius(end)
        val radius =
            rememberMorphCornerRadius(
                animatedVisibilityScope = animatedVisibilityScope,
                sharedContentState = sharedContentState,
                restRadius = restRadius,
                counterpartRadius = morphCounterpartRadius(end, layers.pairedCorners),
                preMorphRadius = preMorphRadius,
            )
        // pairedCorners is constant for the lifetime of a call site, so the branch never flips.
        val overlayClip =
            if (layers.pairedCorners) rememberMorphOverlayClip(radius)
            else remember(restRadius) { OverlayClip(RoundedCornerShape(restRadius)) }
        sharedBounds(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = animatedVisibilityScope,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                boundsTransform = MorphBoundsTransform,
                resizeMode = morphResizeMode(layers.remeasure),
                zIndexInOverlay = overlayZIndex,
                clipInOverlayDuringTransition = overlayClip,
            )
            // The element's own clip, read in the layer block: Modifier.clip(shape) takes a value,
            // so a radius read there would recompose this end on every frame of the morph.
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(radius.value)
            }
    }

/**
 * The shared node itself, in a composition scope of its own. sharedBounds and the paired radius
 * both read the transition during composition (through a composed modifier and animateDp), and that
 * invalidates the nearest non-inline scope: Box is inline, so applied straight to a card's Box the
 * whole card would recompose each time the transition changes state. Here only this small function
 * re-runs, and [content] is skipped.
 */
@Composable
internal fun MorphNode(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    key: String,
    end: MorphEnd,
    layers: MorphLayers,
    modifier: Modifier = Modifier,
    preMorphRadius: Dp = morphRestRadius(end),
    overlayZIndex: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.morphBounds(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            key = key,
            end = end,
            layers = layers,
            preMorphRadius = preMorphRadius,
            overlayZIndex = overlayZIndex,
        ),
        content = content,
    )
}
