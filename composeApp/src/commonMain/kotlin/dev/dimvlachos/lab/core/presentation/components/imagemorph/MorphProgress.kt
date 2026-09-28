package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

// The container's width as a fraction of its final width: a card's worth on the grid, 1 at rest as
// a detail.
internal fun morphChromeAlpha(widthFraction: Float): Float {
    val start = MorphDimens.ChromeStartWidth
    return ((widthFraction - start) / (1f - start)).coerceIn(0f, 1f)
}

// Progress runs 0 -> 1 on an open and 1 -> 0 on a close.
internal fun morphBackdropAlpha(progress: Float, closing: Boolean): Float =
    if (closing) {
        val exit = MorphDimens.BackdropExitFraction
        ((progress - (1f - exit)) / exit).coerceIn(0f, 1f)
    } else {
        progress.coerceIn(0f, 1f)
    }

/**
 * Publishes this node's width as a fraction of its final width into [fraction], from geometry, not
 * from a clock. The lookahead pass records the final width once; every approach pass
 * (RemeasureToBounds lays the node out at the animated bounds) writes the ratio. Readers take it in
 * layer blocks, so a write here invalidates draw and nothing else. Whatever the bounds do, this is
 * in step with them, including the frame the overlay hands back to the layout.
 */
internal fun Modifier.morphWidthFraction(fraction: MutableFloatState): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        if (isLookingAhead) {
            finalWidths[fraction] = placeable.width.toFloat()
        } else {
            val final = finalWidths[fraction] ?: 0f
            fraction.floatValue = if (final > 0f) placeable.width / final else 1f
        }
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

// One final width per published fraction; the lookahead pass is the only writer.
private val finalWidths = HashMap<MutableFloatState, Float>()

/**
 * The bounds' progress for what sits outside the node, rebuilt on the same transition with the same
 * tween and curve. Reading the transition here in composition is what makes this a scope of its
 * own: it re-runs when the transition changes state, and [content] is skipped.
 */
@Composable
internal fun MorphProgress(
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable (progress: State<Float>, closing: () -> Boolean) -> Unit,
) {
    val transition = animatedVisibilityScope.transition
    val progress =
        transition.animateFloat(
            transitionSpec = {
                val opening = targetState == EnterExitState.Visible
                tween(
                    if (opening) MorphDimens.OpenMs else MorphDimens.CloseMs,
                    easing = MorphDimens.MorphEasing,
                )
            },
            label = "morphProgress",
        ) { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }
    val closing = remember(transition) { { transition.targetState == EnterExitState.PostExit } }
    content(progress, closing)
}
