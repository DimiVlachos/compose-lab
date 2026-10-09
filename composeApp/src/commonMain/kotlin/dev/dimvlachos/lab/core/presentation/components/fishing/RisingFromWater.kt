package dev.dimvlachos.lab.core.presentation.components.fishing

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Lets this item of a [FishingRefresh]'s list be fished out of the water when it is part of a
 * catch. Put it on every item, with [index] its place in the list from the top: when a refresh
 * lands as [FishingOutcome.Caught] with a count of n, the top n items are the catch. They are
 * pulled up out of the sea on the line, small, hanging on the hook and swinging; at the top they
 * grow to their full size as they drop into their places, one after another, and the list moves
 * down to make room for them. Every other item, and every item once its catch is in, is laid out
 * and drawn as it is.
 *
 * So that a catch goes in at the top of the list, put its items there as you set the status to
 * [FishingStatus.Landed], and keep the list at the top as they come in: with a `LazyListState`,
 * `requestScrollToItem(0)`, or the list keeps its old first item in view and the catch lands out of
 * sight above it.
 *
 * While it is on the line, the item is drawn by the fishing refresh, from a recording of its own
 * drawing, so it can hang in the air above the list. Its place opens as it is laid out again, and
 * nothing recomposes.
 */
public fun Modifier.risingFromWater(state: FishingRefreshState, index: Int): Modifier =
    this then RisingFromWaterElement(state, index)

private data class RisingFromWaterElement(val state: FishingRefreshState, val index: Int) :
    ModifierNodeElement<RisingFromWaterNode>() {
    override fun create() = RisingFromWaterNode(state, index)

    override fun update(node: RisingFromWaterNode) {
        node.follow(state, index)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "risingFromWater"
        properties["index"] = index
    }
}

private class RisingFromWaterNode(private var state: FishingRefreshState, private var index: Int) :
    Modifier.Node(), LayoutModifierNode, DrawModifierNode, LayoutAwareModifierNode {
    private var card: CaughtCard? = null

    override fun onAttach() {
        val card = CaughtCard(requireGraphicsContext().createGraphicsLayer())
        this.card = card
        state.caught[index] = card
    }

    override fun onDetach() {
        val card = card ?: return
        if (state.caught[index] === card) state.caught.remove(index)
        requireGraphicsContext().releaseGraphicsLayer(card.layer)
        this.card = null
    }

    fun follow(state: FishingRefreshState, index: Int) {
        if (state === this.state && index == this.index) return
        val card = card
        if (card != null && this.state.caught[this.index] === card)
            this.state.caught.remove(this.index)
        this.state = state
        this.index = index
        if (card != null) state.caught[index] = card
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        card?.coordinates = coordinates
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        card?.size = IntSize(placeable.width, placeable.height)
        val grow = state.riseFor(index)
        // Its place opens as it comes in. Never quite nothing: a list keeps its first item in
        // place as items change, and an item with no height can't be its first, so the catch
        // would go in above it, out of sight.
        val height =
            if (grow >= 1f) placeable.height
            else (placeable.height * grow).roundToInt().coerceIn(1, placeable.height)
        return layout(placeable.width, height) { placeable.place(0, 0) }
    }

    override fun ContentDrawScope.draw() {
        val card = card
        if (card == null || !state.hooked(index)) {
            drawContent()
            return
        }
        // On the line: recorded at its full size, for the fishing refresh to draw on the hook,
        // and not drawn in its place, which opens under it as it comes in.
        card.layer.record(card.size) { this@draw.drawContent() }
    }
}

/** A list item on a catch's line: a recording of its drawing, where it is, and its full size. */
internal class CaughtCard(val layer: GraphicsLayer) {
    var coordinates: LayoutCoordinates? = null
    var size = IntSize.Zero
}
