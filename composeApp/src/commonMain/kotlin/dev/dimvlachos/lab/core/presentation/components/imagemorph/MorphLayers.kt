package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.runtime.Immutable

/**
 * Each layer is one fix from the morph's history, switchable so the catalog can show the defect it
 * cures. [stagedChrome] without [remeasure] is a near no-op: the chrome then sits inside the shared
 * node and rides the naive scale anyway.
 *
 * @property remeasure The photo is laid out again every frame, so its crop glides between the
 *   card's and the detail's instead of snapping to the detail's on the first frame.
 * @property pairedCorners Each end's corners travel to the other's, so the card's 16 dp and the
 *   detail's 0 dp meet halfway.
 * @property stagedChrome The title and caption ride above the photo and fade in as it grows.
 */
@Immutable
public data class MorphLayers(
    val remeasure: Boolean = false,
    val pairedCorners: Boolean = false,
    val stagedChrome: Boolean = false,
) {
    public companion object {
        /** Every fix on. */
        public val All: MorphLayers =
            MorphLayers(remeasure = true, pairedCorners = true, stagedChrome = true)
    }
}
