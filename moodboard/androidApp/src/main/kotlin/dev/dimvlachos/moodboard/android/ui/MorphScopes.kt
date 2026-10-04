package dev.dimvlachos.moodboard.android.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.dimvlachos.moodboard.morph.MorphEnd
import dev.dimvlachos.moodboard.morph.MorphViewports
import dev.dimvlachos.moodboard.morph.photoMorph

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope =
    staticCompositionLocalOf<SharedTransitionScope> { error("No SharedTransitionLayout") }

/** Each tab grid's visible area, which a morph to or from it stays inside. */
val LocalMorphViewports = staticCompositionLocalOf { MorphViewports() }

/**
 * One end of the grid-to-detail container transform; [scope] is the tab the grid belongs to. Both
 * ends clip to that grid's viewport, so the photo slides under its bars instead of over them.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.photoMorphEnd(
    photoId: String,
    scope: String,
    end: MorphEnd,
    overlayZIndex: Float = 0f,
): Modifier =
    photoMorph(
        sharedTransitionScope = LocalSharedTransitionScope.current,
        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
        photoId = photoId,
        scope = scope,
        end = end,
        overlayZIndex = overlayZIndex,
        viewport = LocalMorphViewports.current.of(scope),
    )
