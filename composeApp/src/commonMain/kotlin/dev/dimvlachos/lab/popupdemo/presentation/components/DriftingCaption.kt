package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// The old caption fades out over this long; the new one starts a little later and takes longer,
// so the two overlap softly.
private const val OutMs = 240
private const val InDelayMs = 120
private const val InMs = 420

// How far the old caption drifts up as it goes, and how far below its place the new one starts.
private val DriftOut = 10.dp
private val DriftIn = 16.dp

/**
 * A caption that changes softly when [index] changes: the old one fades out drifting gently up, and
 * the new one fades in rising into its place from just below.
 */
@Composable
internal fun DriftingCaption(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    val density = LocalDensity.current
    val out = with(density) { DriftOut.roundToPx() }
    val rise = with(density) { DriftIn.roundToPx() }
    AnimatedContent(
        targetState = index,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(InMs, InDelayMs, LinearOutSlowInEasing)) +
                    slideInVertically(tween(InMs, InDelayMs, LinearOutSlowInEasing)) { rise })
                .togetherWith(
                    fadeOut(tween(OutMs, easing = FastOutLinearInEasing)) +
                        slideOutVertically(tween(OutMs, easing = FastOutLinearInEasing)) { -out }
                )
                .using(SizeTransform(clip = false))
        },
        contentAlignment = Alignment.TopStart,
        label = "caption",
    ) { shown ->
        content(shown)
    }
}
