package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

// The old caption folds away in this long, then the new one unfolds in this long.
private const val FoldMs = 260
private const val UnfoldMs = 380

// How far a caption turns, in degrees, at the moment it is folded away; and how far off the eye
// is, in multiples of the density, so the turn has a little perspective without distorting.
private const val FoldDegrees = 80f
private const val Eye = 14f

/**
 * A caption that turns over like a leaf when [index] changes: the old one folds away on its top
 * edge, as a page is turned, and the new one unfolds into its place. Going forward the old caption
 * tips back away from the reader; going back it tips the other way, as the book's pages do.
 */
@Composable
internal fun TurningCaption(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    // Which way the tour last moved: read by both the leaving and the arriving caption.
    val last = remember { IntArray(1) { index } }
    val way = remember(index) { (if (index >= last[0]) 1f else -1f).also { last[0] = index } }
    AnimatedContent(
        targetState = index,
        modifier = modifier,
        transitionSpec = {
            // Both captions keep their full opacity here; the turn itself, below, hides them.
            (EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished).using(
                SizeTransform(clip = false)
            )
        },
        contentAlignment = Alignment.TopStart,
        label = "caption",
    ) { shown ->
        // -1 folded away behind, 0 lying open, 1 still folded, about to unfold.
        val turn by
            transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) {
                        tween(UnfoldMs, delayMillis = FoldMs, easing = LinearOutSlowInEasing)
                    } else {
                        tween(FoldMs, easing = FastOutLinearInEasing)
                    }
                },
                label = "turn",
            ) { state ->
                when (state) {
                    EnterExitState.PreEnter -> 1f
                    EnterExitState.Visible -> 0f
                    EnterExitState.PostExit -> -1f
                }
            }
        Box(
            Modifier.graphicsLayer {
                rotationX = -turn * way * FoldDegrees
                transformOrigin = TransformOrigin(0.5f, 0f)
                cameraDistance = Eye * density
                alpha = 1f - abs(turn)
            }
        ) {
            content(shown)
        }
    }
}
