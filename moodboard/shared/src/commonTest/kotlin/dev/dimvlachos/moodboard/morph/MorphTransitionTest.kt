@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class MorphTransitionTest {
    @Test
    fun bothEndsBuildTheSameKeyForOnePhoto() {
        assertEquals(morphKey("naxos"), morphKey("naxos"))
        assertNotEquals(morphKey("naxos"), morphKey("milos"))
    }

    @Test
    fun remeasuredEndsReLayOutToTheirBounds() {
        assertEquals(
            SharedTransitionScope.ResizeMode.RemeasureToBounds,
            morphResizeMode(remeasure = true),
        )
    }

    @Test
    fun naiveEndsScaleInsteadOfRemeasuring() {
        assertNotEquals(
            SharedTransitionScope.ResizeMode.RemeasureToBounds,
            morphResizeMode(remeasure = false),
        )
    }

    @Test
    fun openingBoundsTakeTheLongEmphasizedTween() {
        val spec =
            MorphBoundsTransform.createAnimationSpec(
                Rect(0f, 0f, 10f, 10f),
                Rect.Zero.copy(right = 100f, bottom = 100f),
            )
        val tween = assertIs<TweenSpec<Rect>>(spec)
        assertEquals(MorphDimens.OpenMs, tween.durationMillis)
        assertEquals(MorphDimens.MorphEasing, tween.easing)
    }

    @Test
    fun closingBoundsTakeTheShorterEmphasizedTween() {
        val spec =
            MorphBoundsTransform.createAnimationSpec(
                Rect(0f, 0f, 100f, 100f),
                Rect(0f, 0f, 10f, 10f),
            )
        val tween = assertIs<TweenSpec<Rect>>(spec)
        assertEquals(MorphDimens.CloseMs, tween.durationMillis)
        assertEquals(MorphDimens.MorphEasing, tween.easing)
    }

    @Test
    fun directionalTransformFollowsTheOpeningFlag() {
        var opening = true
        val transform = morphBoundsTransform { opening }
        val same = Rect(0f, 0f, 10f, 10f)
        assertEquals(
            MorphDimens.OpenMs,
            assertIs<TweenSpec<Rect>>(transform.createAnimationSpec(same, same)).durationMillis,
        )
        opening = false
        assertEquals(
            MorphDimens.CloseMs,
            assertIs<TweenSpec<Rect>>(transform.createAnimationSpec(same, same)).durationMillis,
        )
    }
}
