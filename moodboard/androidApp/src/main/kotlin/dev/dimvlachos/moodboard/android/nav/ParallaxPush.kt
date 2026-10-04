package dev.dimvlachos.moodboard.android.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

// The push used in edda and Builder Brigade: the new screen slides in over the full width while
// the one underneath drifts a third of the way, the parallax. Back plays it in reverse.
private val Ease = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private const val SlideMs = 400
private const val ParallaxDivisor = 3

private val Push: ContentTransform =
    slideInHorizontally(tween(SlideMs, easing = Ease)) { it } togetherWith
        slideOutHorizontally(tween(SlideMs, easing = Ease)) { -it / ParallaxDivisor }

private val Pop: ContentTransform =
    slideInHorizontally(tween(SlideMs, easing = Ease)) { -it / ParallaxDivisor } togetherWith
        slideOutHorizontally(tween(SlideMs, easing = Ease)) { it }

/**
 * Metadata for the inner screens that don't morph (board detail, board editor). Only a real
 * one-screen push or pop slides; a tab switch that lands on or leaves one of these screens keeps
 * the app's fade (null falls back to NavDisplay's default).
 */
val ParallaxPush: Map<String, Any> =
    NavDisplay.transitionSpec { if (step() == NavStep.Push) Push else null } +
        NavDisplay.popTransitionSpec { if (step() == NavStep.Pop) Pop else null } +
        NavDisplay.predictivePopTransitionSpec { if (step() == NavStep.Pop) Pop else null }

private fun AnimatedContentTransitionScope<Scene<*>>.step(): NavStep? =
    navStep(initialState.stack(), targetState.stack())

// A single-pane scene holds only its top entry; the screens under it are its previousEntries.
private fun Scene<*>.stack(): List<Any> = (previousEntries + entries).map { it.contentKey }
