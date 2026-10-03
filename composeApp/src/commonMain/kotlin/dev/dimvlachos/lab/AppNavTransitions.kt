package dev.dimvlachos.lab

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import dev.dimvlachos.lab.core.presentation.ui.Motion

// The screen being covered drifts back this share of the way as the new one slides over it.
private const val PUSH_PARALLAX_DIVISOR = 3

/**
 * A deeper screen pushes in from the right over the one it opens from, which drifts a third of the
 * way left behind it; back, it slides off to the right again, over the one coming back.
 */
internal fun AnimatedContentTransitionScope<AppScreen>.appTransition(
    motion: Motion
): ContentTransform {
    val spec = tween<IntOffset>(motion.slowMs, easing = motion.ease)
    return if (targetState.pushesOver(initialState)) {
        slideInHorizontally(spec) { it } togetherWith
            slideOutHorizontally(spec) { -it / PUSH_PARALLAX_DIVISOR }
    } else {
        (slideInHorizontally(spec) { -it / PUSH_PARALLAX_DIVISOR } togetherWith
                slideOutHorizontally(spec) { it })
            .apply { targetContentZIndex = -1f }
    }
}
