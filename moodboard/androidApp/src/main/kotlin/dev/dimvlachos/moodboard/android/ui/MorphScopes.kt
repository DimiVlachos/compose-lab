package dev.dimvlachos.moodboard.android.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.dimvlachos.moodboard.morph.MorphEnd
import dev.dimvlachos.moodboard.morph.photoMorph

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope =
    staticCompositionLocalOf<SharedTransitionScope> { error("No SharedTransitionLayout") }

/** One end of the grid-to-detail container transform, scoped to the current nav entry. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.photoMorphEnd(photoId: String, end: MorphEnd, overlayZIndex: Float = 0f): Modifier =
    photoMorph(
        sharedTransitionScope = LocalSharedTransitionScope.current,
        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
        photoId = photoId,
        end = end,
        overlayZIndex = overlayZIndex,
    )
