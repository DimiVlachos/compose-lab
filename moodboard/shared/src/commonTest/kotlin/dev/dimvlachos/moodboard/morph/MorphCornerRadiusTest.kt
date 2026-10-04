package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MorphCornerRadiusTest {
    @Test
    fun eachEndsCounterpartIsTheOtherEndsRest() {
        assertEquals(
            morphRestRadius(MorphEnd.Detail),
            morphCounterpartRadius(MorphEnd.Card, paired = true),
        )
        assertEquals(
            morphRestRadius(MorphEnd.Card),
            morphCounterpartRadius(MorphEnd.Detail, paired = true),
        )
    }

    @Test
    fun unpairedEndsMorphFromTheirOwnRest() {
        for (end in MorphEnd.entries) {
            assertEquals(morphRestRadius(end), morphCounterpartRadius(end, paired = false), "$end")
        }
    }

    @Test
    fun risingRadiusUsesTheFastRamp() {
        // The card is entering (0 -> 16): its radius must finish rounding inside the backdrop fade.
        val spec = morphRadiusSpec(entering = true, restRadius = 16.dp, counterpartRadius = 0.dp)
        val tween = assertIs<TweenSpec<Dp>>(spec)
        assertEquals(MorphDimens.RiseMs, tween.durationMillis)
        assertEquals(LinearOutSlowInEasing, tween.easing)
    }

    @Test
    fun fallingRadiusUsesTheBoundsCurve() {
        // The card is exiting (16 -> 0), which only happens on an open: the corners flatten on the
        // same curve and clock as the opening bounds.
        val spec = morphRadiusSpec(entering = false, restRadius = 16.dp, counterpartRadius = 0.dp)
        val tween = assertIs<TweenSpec<Dp>>(spec)
        assertEquals(MorphDimens.OpenMs, tween.durationMillis)
        assertEquals(MorphDimens.MorphEasing, tween.easing)
    }

    @Test
    fun bothEndsPickTheSameSpecForOneMorph() {
        // Opening: the card exits (16 -> 0) while the detail enters (16 -> 0), the same travel, so
        // both must pick the same spec.
        val card = morphRadiusSpec(entering = false, restRadius = 16.dp, counterpartRadius = 0.dp)
        val detail = morphRadiusSpec(entering = true, restRadius = 0.dp, counterpartRadius = 16.dp)
        assertEquals(
            assertIs<TweenSpec<Dp>>(card).durationMillis,
            assertIs<TweenSpec<Dp>>(detail).durationMillis,
        )
        assertEquals(assertIs<TweenSpec<Dp>>(card).easing, assertIs<TweenSpec<Dp>>(detail).easing)
    }

    @Test
    fun unmatchedPreEnterFrameShowsThePreMorphRadius() {
        val radius =
            resolveMorphRadius(
                isMatchFound = false,
                currentState = EnterExitState.PreEnter,
                animated = 7.dp,
                restRadius = 0.dp,
                preMorphRadius = 16.dp,
            )
        assertEquals(16.dp, radius)
    }

    @Test
    fun unmatchedSettledEndShowsItsRest() {
        val radius =
            resolveMorphRadius(
                isMatchFound = false,
                currentState = EnterExitState.Visible,
                animated = 7.dp,
                restRadius = 16.dp,
                preMorphRadius = 0.dp,
            )
        assertEquals(16.dp, radius)
    }

    @Test
    fun matchedEndFollowsTheAnimation() {
        val radius =
            resolveMorphRadius(
                isMatchFound = true,
                currentState = EnterExitState.PreEnter,
                animated = 7.dp,
                restRadius = 16.dp,
                preMorphRadius = 0.dp,
            )
        assertEquals(7.dp, radius)
    }
}
