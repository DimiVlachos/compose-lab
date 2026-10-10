package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// The old caption and the new one dissolve into each other, in place, over this long.
private const val FadeMs = 450

/** A caption that dissolves softly into the next when [index] changes, in place. */
@Composable
internal fun FadingCaption(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    AnimatedContent(
        targetState = index,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(FadeMs)) togetherWith fadeOut(tween(FadeMs))).using(
                SizeTransform(clip = false)
            )
        },
        contentAlignment = Alignment.TopStart,
        label = "caption",
    ) { shown ->
        content(shown)
    }
}
