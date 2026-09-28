package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.runtime.Immutable

/**
 * Each layer is one fix from the morph's history, switchable so the catalog can show the defect it
 * cures. [stagedChrome] without [remeasure] is a near no-op: the chrome then sits inside the shared
 * node and rides the naive scale anyway.
 */
@Immutable
data class MorphLayers(
    val remeasure: Boolean = false,
    val pairedCorners: Boolean = false,
    val stagedChrome: Boolean = false,
) {
    companion object {
        val All = MorphLayers(remeasure = true, pairedCorners = true, stagedChrome = true)
    }
}
