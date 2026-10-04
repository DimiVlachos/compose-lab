package dev.dimvlachos.moodboard.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Dialogs ease in from a little smaller on a soft spring, over the platform's fade, instead of only
 * fading in at full size. Read in the layer block, so the animation never recomposes.
 */
@Composable
fun Modifier.dialogEntrance(): Modifier {
    val progress = rememberEntrance()
    return graphicsLayer {
        val scale = EntranceScale + (1f - EntranceScale) * progress.value
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A sheet's content fades in and drifts up a little while the sheet itself slides up, instead of
 * riding in fully drawn: the sheet's own window costs a frame or two as it opens, and content that
 * is still arriving hides that hitch.
 */
@Composable
fun Modifier.sheetContentEntrance(): Modifier {
    val progress = rememberEntrance()
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * SheetContentRise.toPx()
    }
}

@Composable
private fun rememberEntrance(): Animatable<Float, *> {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // A dialog or sheet opens in its own window, whose first frames are slow; starting after
        // them keeps the entrance from being spent before anything is on screen.
        repeat(SettleFrames) { withFrameNanos {} }
        progress.animateTo(1f, EntranceSpring)
    }
    return progress
}

private const val SettleFrames = 2
private const val EntranceScale = 0.92f
private val SheetContentRise = 24.dp
private val EntranceSpring =
    spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
