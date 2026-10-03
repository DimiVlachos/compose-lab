package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp

enum class MorphEnd {
    Card,
    Detail,
}

internal fun morphRestRadius(end: MorphEnd): Dp =
    when (end) {
        MorphEnd.Card -> MorphDimens.CardRadius
        MorphEnd.Detail -> MorphDimens.DetailRadius
    }

// sharedBounds interpolates bounds but never a clip, so two ends resting at different radii
// disagree at the corners for the whole transition. Paired, each end travels to the other's rest.
internal fun morphCounterpartRadius(end: MorphEnd, paired: Boolean): Dp =
    when {
        !paired -> morphRestRadius(end)
        end == MorphEnd.Card -> morphRestRadius(MorphEnd.Detail)
        else -> morphRestRadius(MorphEnd.Card)
    }

// Both ends pick the spec from the same travel direction, which keeps them in agreement at every
// instant: a rising radius takes the fast ramp, a falling one (only ever on an open) rides the
// opening bounds.
internal fun morphRadiusSpec(
    entering: Boolean,
    restRadius: Dp,
    counterpartRadius: Dp,
): FiniteAnimationSpec<Dp> {
    val toward = if (entering) restRadius else counterpartRadius
    val from = if (entering) counterpartRadius else restRadius
    return if (toward > from) {
        tween(MorphDimens.RiseMs, easing = LinearOutSlowInEasing)
    } else {
        tween(MorphDimens.OpenMs, easing = MorphDimens.MorphEasing)
    }
}

// isMatchFound is false for the whole of an arriving end's first composition (it flips in the
// layout pass), so an end whose rest differs from its counterpart would draw one frame at the wrong
// radius over the still-square card beneath it; preMorphRadius is what it shows until the match
// lands. A settled end with no match is simply at rest.
internal fun resolveMorphRadius(
    isMatchFound: Boolean,
    currentState: EnterExitState,
    animated: Dp,
    restRadius: Dp,
    preMorphRadius: Dp,
): Dp =
    when {
        isMatchFound -> animated
        currentState == EnterExitState.PreEnter -> preMorphRadius
        else -> restRadius
    }

/**
 * The radius one end draws with, as a [State] so callers read it in a layer block or a clip path
 * and never in composition: the value changes every frame of the morph, and this component's bar is
 * that nothing recomposes while it animates.
 *
 * isMatchFound gates only the returned value, never the animateDp lambda: the animation latches its
 * initial value on first composition, before a match can be found, so a match-dependent target
 * would pin the arriving end at [restRadius] for the whole morph.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberMorphCornerRadius(
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedContentState: SharedTransitionScope.SharedContentState,
    restRadius: Dp,
    counterpartRadius: Dp,
    preMorphRadius: Dp = restRadius,
): State<Dp> {
    val transition = animatedVisibilityScope.transition
    val animated =
        transition.animateDp(
            transitionSpec = {
                morphRadiusSpec(
                    entering = targetState == EnterExitState.Visible,
                    restRadius = restRadius,
                    counterpartRadius = counterpartRadius,
                )
            },
            label = "morphRadius",
        ) { state ->
            if (state == EnterExitState.Visible) restRadius else counterpartRadius
        }
    return remember(animated, sharedContentState, restRadius, preMorphRadius) {
        derivedStateOf {
            resolveMorphRadius(
                isMatchFound = sharedContentState.isMatchFound,
                currentState = transition.currentState,
                animated = animated.value,
                restRadius = restRadius,
                preMorphRadius = preMorphRadius,
            )
        }
    }
}
