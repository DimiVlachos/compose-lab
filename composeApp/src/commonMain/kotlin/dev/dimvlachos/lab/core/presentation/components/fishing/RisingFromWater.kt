package dev.dimvlachos.lab.core.presentation.components.fishing

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * Lets this item of a [FishingRefresh]'s list rise out of the water when it is part of a catch. Put
 * it on every item, with [index] its place in the list from the top: when a refresh lands as
 * [FishingOutcome.Caught] with a count of n, the top n items are the catch. They stay under the
 * water while the line is reeled in, then rise one after another, each growing from nothing to its
 * full height, foot first, as it comes up, so the list moves down to make room for it. Every other
 * item, and every item once its catch has risen, is laid out as it is.
 *
 * So that a catch rises at the top of the list, put its items there as you set the status to
 * [FishingStatus.Landed], and keep the list at the top as they come in: with a `LazyListState`,
 * `requestScrollToItem(0)`, or the list keeps its old first item in view and the catch lands out of
 * sight above it.
 *
 * The rise is read while laying out and drawing: the items are laid out again as they rise, and
 * nothing recomposes.
 */
public fun Modifier.risingFromWater(state: FishingRefreshState, index: Int): Modifier =
// Clipped to its place only while it rises, so a risen item draws as it would without this.
drawWithContent {
    if (state.riseFor(index) < 1f) clipRect { this@drawWithContent.drawContent() }
    else drawContent()
}
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val grow = state.riseFor(index)
        if (grow >= 1f) {
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            // Never quite nothing: a list keeps its first item in place as items change, and
            // an item with no height can't be its first, so the catch would rise above it, out
            // of sight.
            val height = (placeable.height * grow).roundToInt().coerceIn(1, placeable.height)
            layout(placeable.width, height) {
                // Its foot first, sliding out from under the water's edge, or the item above it,
                // as its place opens: no gap opens over it. It clears as it surfaces.
                placeable.placeWithLayer(0, height - placeable.height) {
                    alpha = SunkAlpha + (1f - SunkAlpha) * grow
                }
            }
        }
    }

// Under the water, an item is this clear.
private const val SunkAlpha = 0.35f
