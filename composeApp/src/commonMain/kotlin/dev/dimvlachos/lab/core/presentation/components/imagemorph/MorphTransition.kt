package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.ContentScale

// Both ends must build their key here, so they can never drift apart.
internal fun morphKey(index: Int): String = "morph_photo_$index"

// Both ends must pass the same mode. The card and the full-bleed detail differ in ratio, and
// scaleToBounds snaps the visible crop to the destination's on the first frame; RemeasureToBounds
// re-lays out the image every frame instead, so the crop glides between the two.
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun morphResizeMode(remeasure: Boolean): SharedTransitionScope.ResizeMode =
    if (remeasure) {
        SharedTransitionScope.ResizeMode.RemeasureToBounds
    } else {
        SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop)
    }

// Opening takes the longer tween, closing the shorter one, as Material's container transform does.
@OptIn(ExperimentalSharedTransitionApi::class)
internal val MorphBoundsTransform = BoundsTransform { initialBounds, targetBounds ->
    val opening = targetBounds.width > initialBounds.width
    tween(
        if (opening) MorphDimens.OpenMs else MorphDimens.CloseMs,
        easing = MorphDimens.MorphEasing,
    )
}

// For an element that keeps its size (an icon), the width cannot tell an open from a close, so the
// caller says which one it is.
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun morphBoundsTransform(opening: () -> Boolean): BoundsTransform =
    BoundsTransform { _, _ ->
        tween(
            if (opening()) MorphDimens.OpenMs else MorphDimens.CloseMs,
            easing = MorphDimens.MorphEasing,
        )
    }
