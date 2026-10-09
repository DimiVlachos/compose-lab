package dev.dimvlachos.lab.core.presentation.components.fishing

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * Lets this item of a [FishingRefresh]'s list rise out of the water when it is part of a catch. Put
 * it on every item, with [index] its place in the list from the top: when a refresh lands as
 * [FishingOutcome.Caught] with a count of n, the top n items are the catch. They stay under the
 * water while the line is reeled in, then rise one after another, each growing from nothing to its
 * full height as it comes up, so the list moves down to make room for it. Every other item, and
 * every item once its catch has risen, is laid out as it is.
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
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val grow = state.riseFor(index)
        // Never quite nothing: a list keeps its first item in place as items change, and an item
        // with no height can't be its first, so the catch would rise above it, out of sight.
        val height =
            if (grow >= 1f) placeable.height
            else (placeable.height * grow).roundToInt().coerceIn(1, placeable.height)
        layout(placeable.width, height) {
            if (grow >= 1f) {
                placeable.place(0, 0)
            } else {
                // It comes up into its place from no deeper than the water reaches over the top of
                // the list, so the room it leaves above it while it rises is under the water, not
                // a gap; and it clears as it surfaces.
                val deep = (FishingDimens.Band - FishingDimens.WaterLevel).toPx()
                placeable.placeWithLayer(0, 0) {
                    translationY = (1f - grow) * minOf(deep, placeable.height.toFloat())
                    alpha = SunkAlpha + (1f - SunkAlpha) * grow
                }
            }
        }
    }

// Under the water, an item is this clear.
private const val SunkAlpha = 0.35f
