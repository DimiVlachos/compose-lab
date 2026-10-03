package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The detail screen's backdrop rides the morph, as in the lab's container transform: the grid stays
 * put underneath while the backdrop fades in over the opening bounds, on the same curve and clock.
 * A cross-fade of whole screens would swap them in a frame or two, a visible jump.
 */
val MorphBackdropIn: EnterTransition =
    fadeIn(tween(MorphDimens.OpenMs, easing = MorphDimens.MorphEasing))

/** On a close the backdrop is gone by half way, so the grid is there when the photo lands. */
val MorphBackdropOut: ExitTransition =
    fadeOut(tween((MorphDimens.CloseMs * BackdropExitFraction).toInt(), easing = LinearEasing))

private const val BackdropExitFraction = 0.5f

/**
 * The detail's chrome (app bar, title, tags), staged as in the lab: it comes in slowly, outlasting
 * the open so it settles after the photo lands, and leaves fast on a close. The photo itself
 * carries the transition; chrome at full strength over it from the first frame reads as a flash.
 */
@Composable
fun Modifier.morphChrome(animatedVisibilityScope: AnimatedVisibilityScope): Modifier =
    with(animatedVisibilityScope) {
        animateEnterExit(
            enter = fadeIn(tween(MorphDimens.ChromeFadeInMs, easing = MorphDimens.MorphEasing)),
            exit = fadeOut(tween(MorphDimens.ChromeFadeOutMs, easing = LinearEasing)),
        )
    }
